package com.ayanpandey.blink.core.file.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

@Suppress("TooGenericExceptionCaught", "LargeClass", "ComplexMethod", "NestedBlockDepth")
class DocumentRepositoryImpl(
    private val context: Context,
    private val logger: BlinkLogger
) : DocumentRepository {

    private val supportedExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"
    )

    private val recentsFile = File(context.filesDir, "blink_recents.json")
    private val scannedFile = File(context.filesDir, "blink_scanned.json")
    private val foldersFile = File(context.filesDir, "blink_folders.json")
    private val bookmarksFile = File(context.filesDir, "blink_bookmarks.json")
    private val searchesFile = File(context.filesDir, "blink_searches.json")

    private val _recentDocuments = MutableStateFlow<List<Document>>(emptyList())
    private val _scannedDocuments = MutableStateFlow<List<Document>>(emptyList())

    init {
        loadRecents()
        loadScanned()
    }

    override fun getRecentDocuments(): Flow<List<Document>> = _recentDocuments.asStateFlow()

    override fun getScannedDocuments(): Flow<List<Document>> = _scannedDocuments.asStateFlow()

    override suspend fun addRecentDocument(document: Document): Unit = withContext(Dispatchers.IO) {
        logger.d(TAG, "Adding document to recents: ${document.displayName}")
        val current = _recentDocuments.value.toMutableList()
        current.removeAll { it.uri == document.uri }
        current.add(0, document) // Add to the top
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        _recentDocuments.value = current
        saveRecents(current)
    }

    override suspend fun getContinueReadingPosition(uri: String): Int = withContext(Dispatchers.IO) {
        try {
            if (!bookmarksFile.exists()) return@withContext 0
            val jsonStr = bookmarksFile.readText()
            val obj = JSONObject(jsonStr)
            return@withContext obj.optInt(uri, 0)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get continue reading position for $uri", e)
            0
        }
    }

    override suspend fun saveContinueReadingPosition(uri: String, position: Int): Unit = withContext(Dispatchers.IO) {
        try {
            val obj = if (bookmarksFile.exists()) {
                JSONObject(bookmarksFile.readText())
            } else {
                JSONObject()
            }
            obj.put(uri, position)
            bookmarksFile.writeText(obj.toString())
            logger.d(TAG, "Saved continue reading position for $uri at $position")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save continue reading position for $uri", e)
        }
    }

    override suspend fun getScannedFolders(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (!foldersFile.exists()) return@withContext emptyList()
            val jsonStr = foldersFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load scanned folders", e)
            emptyList()
        }
    }

    override suspend fun addScannedFolder(uri: String): Unit = withContext(Dispatchers.IO) {
        val current = getScannedFolders().toMutableList()
        if (!current.contains(uri)) {
            current.add(uri)
            saveScannedFolders(current)
        }
    }

    override suspend fun removeScannedFolder(uri: String): Unit = withContext(Dispatchers.IO) {
        val current = getScannedFolders().toMutableList()
        if (current.remove(uri)) {
            saveScannedFolders(current)
        }
    }

    override suspend fun scanStorage(customFolderUris: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        logger.i(TAG, "Manual scan started. Custom folders to scan: ${customFolderUris.size}")
        try {
            val foundDocuments = mutableListOf<Document>()

            // 1. Scan Public Directories (Downloads & Documents)
            val publicFolders = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            )
            for (folder in publicFolders) {
                if (folder.exists() && folder.isDirectory) {
                    logger.d(TAG, "Scanning public folder: ${folder.absolutePath}")
                    scanFile(folder, foundDocuments)
                }
            }

            // 2. Scan WhatsApp and Telegram folders if standard permission allows
            val chatFolders = listOf(
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Documents/"),
                File(Environment.getExternalStorageDirectory(), "Android/media/org.telegram.messenger/Telegram/Telegram Documents/"),
                File(Environment.getExternalStorageDirectory(), "Telegram/Telegram Documents/")
            )
            for (folder in chatFolders) {
                if (folder.exists() && folder.isDirectory) {
                    logger.d(TAG, "Scanning chat attachment folder: ${folder.absolutePath}")
                    scanFile(folder, foundDocuments)
                }
            }

            // 3. Scan Custom/User Selected SAF folders
            for (uriStr in customFolderUris) {
                try {
                    val treeUri = Uri.parse(uriStr)
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val dirFile = DocumentFile.fromTreeUri(context, treeUri)
                    if (dirFile != null && dirFile.isDirectory) {
                        logger.d(TAG, "Scanning custom SAF folder: $uriStr")
                        scanDocumentFile(dirFile, foundDocuments)
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to scan custom folder: $uriStr", e)
                }
            }

            // Remove duplicates by URI
            val distinctDocs = foundDocuments.distinctBy { it.uri }
            _scannedDocuments.value = distinctDocs
            saveScanned(distinctDocs)

            logger.i(TAG, "Manual scan finished. Found ${distinctDocs.size} documents.")
            Result.success(distinctDocs.size)
        } catch (e: Exception) {
            logger.e(TAG, "Exception during manual scan", e)
            Result.failure(e)
        }
    }

    override suspend fun searchDocuments(query: String, category: String?, extension: String?): List<Document> = withContext(Dispatchers.Default) {
        val start = System.currentTimeMillis()
        val docs = _scannedDocuments.value
        val lowerQuery = query.lowercase(Locale.ROOT)
        
        val filtered = docs.filter { doc ->
            val matchesQuery = if (query.isEmpty()) true else doc.displayName.lowercase(Locale.ROOT).contains(lowerQuery)
            val matchesCategory = if (category.isNullOrEmpty()) true else getCategoryName(doc.documentType) == category
            val matchesExtension = if (extension.isNullOrEmpty()) true else doc.extension == extension.lowercase(Locale.ROOT)
            matchesQuery && matchesCategory && matchesExtension
        }
        val duration = System.currentTimeMillis() - start
        logger.d(TAG, "Search completed in ${duration}ms. Query='$query', Category='$category', ResultsCount=${filtered.size}")
        filtered
    }

    override suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (!searchesFile.exists()) return@withContext emptyList()
            val jsonStr = searchesFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load recent searches", e)
            emptyList()
        }
    }

    override suspend fun addRecentSearch(query: String): Unit = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext
        val current = getRecentSearches().toMutableList()
        current.remove(query)
        current.add(0, query)
        if (current.size > 10) {
            current.removeAt(current.size - 1)
        }
        try {
            val arr = JSONArray(current)
            searchesFile.writeText(arr.toString())
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save recent searches", e)
        }
    }

    override suspend fun clearRecentSearches(): Unit = withContext(Dispatchers.IO) {
        try {
            if (searchesFile.exists()) {
                searchesFile.delete()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to clear searches", e)
        }
    }

    // ----- Helper Methods -----

    private fun getCategoryName(type: DocumentType): String {
        return when (type) {
            DocumentType.PDF -> "PDF"
            DocumentType.DOC, DocumentType.DOCX -> "Word"
            DocumentType.XLS, DocumentType.XLSX -> "Excel"
            DocumentType.PPT, DocumentType.PPTX -> "PowerPoint"
            DocumentType.TXT -> "Text"
            DocumentType.CSV -> "CSV"
            else -> "Unknown"
        }
    }

    private fun scanFile(file: File, resultList: MutableList<Document>) {
        if (file.isDirectory) {
            val list = file.listFiles()
            if (list != null) {
                for (child in list) {
                    scanFile(child, resultList)
                }
            }
        } else {
            val name = file.name
            val ext = extractExtension(name)
            if (supportedExtensions.contains(ext)) {
                val doc = createDocumentFromFile(file)
                resultList.add(doc)
            }
        }
    }

    private fun scanDocumentFile(dir: DocumentFile, resultList: MutableList<Document>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDocumentFile(file, resultList)
            } else {
                val name = file.name ?: continue
                val ext = extractExtension(name)
                if (supportedExtensions.contains(ext)) {
                    val doc = createDocumentFromDocumentFile(file)
                    resultList.add(doc)
                }
            }
        }
    }

    private fun createDocumentFromFile(file: File): Document {
        val uriStr = Uri.fromFile(file).toString()
        val ext = extractExtension(file.name)
        val mime = getMimeTypeFallback(ext)
        return Document(
            id = file.name + "_" + file.length(),
            uri = uriStr,
            displayName = file.name,
            mimeType = mime,
            extension = ext,
            size = file.length(),
            lastModified = file.lastModified(),
            documentType = getDocumentType(ext)
        )
    }

    private fun createDocumentFromDocumentFile(file: DocumentFile): Document {
        val uriStr = file.uri.toString()
        val name = file.name ?: "Unnamed"
        val ext = extractExtension(name)
        val mime = file.type ?: getMimeTypeFallback(ext)
        return Document(
            id = name + "_" + file.length(),
            uri = uriStr,
            displayName = name,
            mimeType = mime,
            extension = ext,
            size = file.length(),
            lastModified = file.lastModified(),
            documentType = getDocumentType(ext)
        )
    }

    private fun extractExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex != -1 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    private fun getDocumentType(extension: String): DocumentType {
        return when (extension) {
            "pdf" -> DocumentType.PDF
            "doc", "docx" -> DocumentType.DOCX // standardizing type resolution
            "xls", "xlsx" -> DocumentType.XLSX
            "ppt", "pptx" -> DocumentType.PPTX
            "txt" -> DocumentType.TXT
            "csv" -> DocumentType.CSV
            else -> DocumentType.UNKNOWN
        }
    }

    private fun getMimeTypeFallback(extension: String): String {
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            else -> "application/octet-stream"
        }
    }

    // ----- JSON Persistence Load/Save -----

    private fun loadRecents() {
        try {
            if (!recentsFile.exists()) return
            val jsonStr = recentsFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Document>()
            for (i in 0 until arr.length()) {
                list.add(jsonObjectToDocument(arr.getJSONObject(i)))
            }
            _recentDocuments.value = list
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load recents", e)
        }
    }

    private fun saveRecents(list: List<Document>) {
        try {
            val arr = JSONArray()
            for (doc in list) {
                arr.put(documentToJSONObject(doc))
            }
            recentsFile.writeText(arr.toString())
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save recents", e)
        }
    }

    private fun loadScanned() {
        try {
            if (!scannedFile.exists()) return
            val jsonStr = scannedFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Document>()
            for (i in 0 until arr.length()) {
                list.add(jsonObjectToDocument(arr.getJSONObject(i)))
            }
            _scannedDocuments.value = list
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load scanned index", e)
        }
    }

    private fun saveScanned(list: List<Document>) {
        try {
            val arr = JSONArray()
            for (doc in list) {
                arr.put(documentToJSONObject(doc))
            }
            scannedFile.writeText(arr.toString())
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save scanned index", e)
        }
    }

    private fun saveScannedFolders(list: List<String>) {
        try {
            val arr = JSONArray(list)
            foldersFile.writeText(arr.toString())
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save scanned folders", e)
        }
    }

    private fun documentToJSONObject(doc: Document): JSONObject {
        val obj = JSONObject()
        obj.put("id", doc.id)
        obj.put("uri", doc.uri)
        obj.put("displayName", doc.displayName)
        obj.put("mimeType", doc.mimeType)
        obj.put("extension", doc.extension)
        obj.put("size", doc.size)
        obj.put("lastModified", doc.lastModified)
        obj.put("documentType", doc.documentType.name)
        return obj
    }

    private fun jsonObjectToDocument(obj: JSONObject): Document {
        val typeName = obj.optString("documentType", "UNKNOWN")
        val docType = try {
            DocumentType.valueOf(typeName)
        } catch (e: Exception) {
            DocumentType.UNKNOWN
        }
        return Document(
            id = obj.getString("id"),
            uri = obj.getString("uri"),
            displayName = obj.getString("displayName"),
            mimeType = obj.getString("mimeType"),
            extension = obj.getString("extension"),
            size = obj.getLong("size"),
            lastModified = obj.getLong("lastModified"),
            documentType = docType
        )
    }

    companion object {
        private const val TAG = "DocumentRepositoryImpl"
    }
}
