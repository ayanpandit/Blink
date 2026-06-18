package com.ayanpandey.blink.core.file.resolver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentMetadata
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import android.os.ParcelFileDescriptor


@Suppress("TooGenericExceptionCaught")
class FileResolverImpl(
    private val context: Context,
    private val logger: BlinkLogger,
) : FileResolver {
    private val supportedExtensions =
        setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
        )

    private val supportedMimeTypes =
        setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
        )

    private fun getActiveContext(): Context {
        val activity = com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity
        logger.d(TAG, "getActiveContext: Holder returned activity? ${activity != null}")
        return activity ?: context
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override suspend fun resolveMetadata(uriString: String): Result<DocumentMetadata> =
        withContext(Dispatchers.IO) {
            logger.d(TAG, "Resolving metadata for URI: $uriString")
            try {
                val uri =
                    Uri.parse(uriString)
                        ?: return@withContext Result.failure(AppErrorException(AppError.FileError.InvalidUri))

                val scheme = uri.scheme
                if (scheme != "content" && scheme != "file") {
                    logger.e(TAG, "Invalid URI scheme: $scheme")
                    return@withContext Result.failure(AppErrorException(AppError.FileError.InvalidUri))
                }

                val activeContext = getActiveContext()
                val resolver = activeContext.contentResolver

                // Try to persist read permission as early as possible
                if (scheme == "content") {
                    try {
                        logger.d(TAG, "Attempting to take persistable read permission for $uri")
                        resolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        logger.i(TAG, "Persistable read permission taken successfully for $uri")
                    } catch (e: SecurityException) {
                        logger.w(TAG, "Failed to take persistable permission for $uri: ${e.message}", e)
                    }
                }

                val authority = uri.authority ?: "N/A"
                val permissionState =
                    activeContext.checkUriPermission(
                        uri,
                        android.os.Process.myPid(),
                        android.os.Process.myUid(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                val permissionStateStr =
                    if (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        "GRANTED"
                    } else {
                        "DENIED"
                    }

                var mimeType = ""
                if (scheme == "content") {
                    try {
                        logger.d(TAG, "Querying MIME type via getType() for $uri")
                        mimeType = resolver.getType(uri) ?: ""
                        logger.d(TAG, "getType() returned MIME type: $mimeType")
                    } catch (e: SecurityException) {
                        logger.e(TAG, "SecurityException in getType() for $uri", e)
                        throw e
                    }
                }

                logger.d(
                    TAG,
                    "URI Details - URI: $uriString | Scheme: $scheme | MIME: $mimeType | " +
                        "Authority: $authority | Permission: $permissionStateStr",
                )

                val resolvedData =
                    if (scheme == "content") {
                        logger.d(TAG, "Resolving content metadata for $uri")
                        resolveContentMetadata(activeContext, uri)
                    } else {
                        val fileResult = resolveFileMetadata(uri)
                        if (fileResult.isFailure) {
                            return@withContext Result.failure(fileResult.exceptionOrNull()!!)
                        }
                        fileResult.getOrThrow()
                    }

                val extension = extractExtension(resolvedData.fileName)
                if (mimeType.isEmpty() && extension.isNotEmpty()) {
                    mimeType = getMimeTypeFallback(extension)
                }

                val isSupported = supportedMimeTypes.contains(mimeType) || supportedExtensions.contains(extension)
                if (!isSupported) {
                    logger.w(TAG, "Unsupported file format. Mime: $mimeType, Ext: $extension")
                    return@withContext Result.failure(AppErrorException(AppError.FileError.UnsupportedType))
                }

                val metadata =
                    DocumentMetadata(
                        uri = uriString,
                        fileName = resolvedData.fileName.ifEmpty { "Unknown Document" },
                        mimeType = mimeType.ifEmpty { "application/octet-stream" },
                        fileSize = resolvedData.fileSize,
                        lastModified = resolvedData.lastModified,
                        extension = extension,
                    )

                logger.i(TAG, "Successfully resolved metadata: $metadata")
                Result.success(metadata)
            } catch (e: SecurityException) {
                println("TEST EXCEPTION SECURITY resolveMetadata: $e")
                e.printStackTrace()
                logger.e(TAG, "SecurityException while reading URI: $uriString", e)
                val stackTrace = e.stackTraceToString()
                Result.failure(
                    AppErrorException(
                        AppError.FileError.PermissionDenied(
                            causeMessage = e.message,
                            stackTrace = stackTrace
                        )
                    )
                )
            } catch (e: Exception) {
                println("TEST EXCEPTION GENERIC resolveMetadata: $e")
                e.printStackTrace()
                logger.e(TAG, "Exception while reading URI: $uriString", e)
                Result.failure(AppErrorException(AppError.FileError.CorruptedUri))
            }
        }

    override suspend fun openInputStream(uriString: String): Result<InputStream> =
        withContext(Dispatchers.IO) {
            logger.d(TAG, "Opening InputStream for URI: $uriString")
            try {
                val uri =
                    Uri.parse(uriString)
                        ?: return@withContext Result.failure(AppErrorException(AppError.FileError.InvalidUri))

                val activeContext = getActiveContext()
                val resolver = activeContext.contentResolver

                // Try to persist read permission
                if (uri.scheme == "content") {
                    try {
                        logger.d(TAG, "Attempting to take persistable read permission inside openInputStream for $uri")
                        resolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        logger.i(TAG, "Persistable read permission taken successfully inside openInputStream for $uri")
                    } catch (e: SecurityException) {
                        logger.w(TAG, "Failed to take persistable permission during stream open: ${e.message}", e)
                    }
                }

                logger.d(TAG, "Opening input stream for content URI: $uri")
                val inputStream =
                    resolver.openInputStream(uri)
                        ?: return@withContext Result.failure(AppErrorException(AppError.FileError.FileNotFound))
                logger.i(TAG, "InputStream opened successfully for $uri")
                Result.success(inputStream)
            } catch (e: SecurityException) {
                println("TEST EXCEPTION SECURITY openInputStream: $e")
                e.printStackTrace()
                logger.e(TAG, "SecurityException opening stream: $uriString", e)
                val stackTrace = e.stackTraceToString()
                Result.failure(
                    AppErrorException(
                        AppError.FileError.PermissionDenied(
                            causeMessage = e.message,
                            stackTrace = stackTrace
                        )
                    )
                )
            } catch (e: Exception) {
                println("TEST EXCEPTION GENERIC openInputStream: $e")
                e.printStackTrace()
                logger.e(TAG, "Error opening stream: $uriString", e)
                Result.failure(AppErrorException(AppError.FileError.CorruptedUri))
            }
        }

    override suspend fun openFileDescriptor(uriString: String): Result<ParcelFileDescriptor> =
        withContext(Dispatchers.IO) {
            logger.d(TAG, "Opening ParcelFileDescriptor for URI: $uriString")
            try {
                val uri = Uri.parse(uriString)
                    ?: return@withContext Result.failure(AppErrorException(AppError.FileError.InvalidUri))

                val activeContext = getActiveContext()
                val resolver = activeContext.contentResolver

                // Try to persist read permission
                if (uri.scheme == "content") {
                    try {
                        logger.d(TAG, "Attempting to take persistable read permission inside openFileDescriptor for $uri")
                        resolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        logger.i(TAG, "Persistable read permission taken successfully inside openFileDescriptor for $uri")
                    } catch (e: SecurityException) {
                        logger.w(TAG, "Failed to take persistable permission during pfd open: ${e.message}", e)
                    }
                }

                logger.d(TAG, "Opening PFD for content URI: $uri")
                val pfd = resolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(AppErrorException(AppError.FileError.FileNotFound))
                logger.i(TAG, "ParcelFileDescriptor opened successfully for $uri")
                Result.success(pfd)
            } catch (e: SecurityException) {
                logger.e(TAG, "SecurityException opening PFD: $uriString", e)
                val stackTrace = e.stackTraceToString()
                Result.failure(
                    AppErrorException(
                        AppError.FileError.PermissionDenied(
                            causeMessage = e.message,
                            stackTrace = stackTrace
                        )
                    )
                )
            } catch (e: Exception) {
                logger.e(TAG, "Error opening PFD: $uriString", e)
                Result.failure(AppErrorException(AppError.FileError.CorruptedUri))
            }
        }


    @Suppress("NestedBlockDepth")
    private fun resolveContentMetadata(
        activeContext: Context,
        uri: Uri,
    ): ResolvedData {
        var fileName = ""
        var fileSize = -1L
        var lastModified = 0L
        try {
            logger.d(TAG, "Querying ContentResolver for metadata of $uri")
            activeContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: ""
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                    val modifiedIndex = cursor.getColumnIndex("last_modified")
                    if (modifiedIndex != -1) {
                        lastModified = cursor.getLong(modifiedIndex)
                    }
                }
            }
            logger.d(TAG, "Content metadata resolved: name=$fileName, size=$fileSize, modified=$lastModified")
        } catch (e: SecurityException) {
            logger.e(TAG, "SecurityException querying content resolver for metadata of $uri", e)
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "Error querying content resolver for metadata of $uri", e)
        }
        return ResolvedData(fileName, fileSize, lastModified)
    }

    private fun resolveFileMetadata(uri: Uri): Result<ResolvedData> {
        val file = File(uri.path ?: "")
        if (!file.exists()) {
            return Result.failure(AppErrorException(AppError.FileError.FileNotFound))
        }
        return Result.success(
            ResolvedData(
                fileName = file.name,
                fileSize = file.length(),
                lastModified = file.lastModified(),
            ),
        )
    }

    private fun extractExtension(fileName: String): String {
        if (fileName.isEmpty()) return ""
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex != -1 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
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
            else -> ""
        }
    }

    private data class ResolvedData(
        val fileName: String,
        val fileSize: Long,
        val lastModified: Long,
    )

    companion object {
        private const val TAG = "FileResolverImpl"
    }
}
