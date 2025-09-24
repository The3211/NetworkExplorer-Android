package com.networkexplorer.model

/**
 * Data class representing an SMB device discovered on the network
 * 
 * @property name Display name of the device
 * @property address IP address or hostname of the device
 * @property url Full SMB URL for connecting to the device
 */
data class SmbDevice(
    val name: String,
    val address: String,
    val url: String
) {
    override fun toString(): String {
        return "$name ($address)"
    }
}

/**
 * Data class representing a file or folder in an SMB share
 * 
 * @property name Name of the file/folder
 * @property path Full path to the file/folder
 * @property isDirectory Whether this item is a directory
 * @property size Size of the file in bytes (0 for directories)
 * @property lastModified Last modification timestamp
 */
data class SmbFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
) {
    
    /**
     * Format file size in human readable format
     */
    fun getFormattedSize(): String {
        if (isDirectory) return ""
        
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            size >= gb -> String.format("%.1f GB", size.toDouble() / gb)
            size >= mb -> String.format("%.1f MB", size.toDouble() / mb)
            size >= kb -> String.format("%.1f KB", size.toDouble() / kb)
            else -> "$size B"
        }
    }
    
    /**
     * Get file extension
     */
    fun getExtension(): String {
        return if (isDirectory) "" else name.substringAfterLast('.', "")
    }
}