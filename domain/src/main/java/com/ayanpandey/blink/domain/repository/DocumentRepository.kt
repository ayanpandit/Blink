package com.ayanpandey.blink.domain.repository

import com.ayanpandey.blink.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    // Recent Documents
    fun getRecentDocuments(): Flow<List<Document>>
    suspend fun addRecentDocument(document: Document)
    suspend fun getContinueReadingPosition(uri: String): Int
    suspend fun saveContinueReadingPosition(uri: String, position: Int)
    
    // Manual Scan / Document Discovery Index
    fun getScannedDocuments(): Flow<List<Document>>
    suspend fun scanStorage(customFolderUris: List<String>): Result<Int>
    suspend fun getScannedFolders(): List<String>
    suspend fun addScannedFolder(uri: String)
    suspend fun removeScannedFolder(uri: String)
    
    // Local Search
    suspend fun searchDocuments(query: String, category: String?, extension: String?): List<Document>
    
    // Recent Searches
    suspend fun getRecentSearches(): List<String>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
}
