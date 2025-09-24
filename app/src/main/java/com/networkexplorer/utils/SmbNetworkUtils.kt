package com.networkexplorer.utils

import android.content.Context
import android.util.Log
import com.networkexplorer.model.SmbDevice
import com.networkexplorer.model.SmbFile
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.SmbException
import jcifs.smb.SmbFile as JcifsSmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.*
import java.util.*

/**
 * Utility class for SMB network operations using jcifs-ng library
 * 
 * TODO: Add support for authentication (username/password)
 * TODO: Add support for different SMB versions (SMB1, SMB2, SMB3)
 * TODO: Add caching mechanism for discovered devices
 * TODO: Add more robust error handling for different network conditions
 */
class SmbNetworkUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "SmbNetworkUtils"
        private const val SMB_PORT = 445
        private const val SCAN_TIMEOUT = 3000 // 3 seconds timeout for each device
        private const val CONNECTION_TIMEOUT = 5000 // 5 seconds connection timeout
    }
    
    private val cifsContext: CIFSContext
    
    init {
        // Initialize jcifs configuration
        val props = Properties().apply {
            // TODO: Configure SMB properties for better compatibility
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.dfs.disabled", "true")
            setProperty("jcifs.smb.client.responseTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.soTimeout", CONNECTION_TIMEOUT.toString())
            // Disable guest access by default - TODO: Make this configurable
            setProperty("jcifs.smb.client.disablePlainTextPasswords", "false")
        }
        
        cifsContext = BaseContext(PropertyConfiguration(props))
    }
    
    /**
     * Scan the local network for SMB devices
     * This is a simple implementation that scans common IP ranges
     * 
     * TODO: Implement more sophisticated network discovery
     * TODO: Add support for mDNS/Bonjour discovery
     * TODO: Add support for NetBIOS name resolution
     */
    suspend fun scanForDevices(): List<SmbDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SmbDevice>()
        
        try {
            val localIp = getLocalIpAddress()
            if (localIp == null) {
                Log.e(TAG, "Could not determine local IP address")
                return@withContext devices
            }
            
            Log.d(TAG, "Starting SMB scan from local IP: $localIp")
            val subnet = getSubnet(localIp)
            
            // Scan common IP range (assumes /24 subnet)
            for (i in 1..254) {
                val targetIp = "$subnet.$i"
                if (targetIp == localIp) continue // Skip self
                
                try {
                    if (isPortOpen(targetIp, SMB_PORT)) {
                        val device = createSmbDevice(targetIp)
                        if (device != null) {
                            devices.add(device)
                            Log.d(TAG, "Found SMB device: ${device.name} at ${device.address}")
                        }
                    }
                } catch (e: Exception) {
                    // Ignore individual device scan failures
                    Log.v(TAG, "Could not scan $targetIp: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during device scan", e)
        }
        
        Log.d(TAG, "SMB scan complete. Found ${devices.size} devices")
        devices
    }
    
    /**
     * List files and directories in an SMB share
     * 
     * @param device The SMB device to browse
     * @param path Path within the share (empty for root)
     * @return List of files and directories
     * 
     * TODO: Add support for authentication
     * TODO: Add support for hidden files option
     * TODO: Add file type filtering
     */
    suspend fun listFiles(device: SmbDevice, path: String = ""): List<SmbFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SmbFile>()
        
        try {
            val url = if (path.isEmpty()) device.url else "${device.url}$path"
            Log.d(TAG, "Listing files for URL: $url")
            
            val smbFile = JcifsSmbFile(url, cifsContext)
            
            if (smbFile.exists() && smbFile.isDirectory) {
                smbFile.listFiles()?.forEach { file ->
                    try {
                        val fileName = file.name.removeSuffix("/")
                        if (fileName.isNotEmpty() && !fileName.startsWith(".")) {
                            files.add(
                                SmbFile(
                                    name = fileName,
                                    path = file.path,
                                    isDirectory = file.isDirectory,
                                    size = if (file.isDirectory) 0 else file.length(),
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error reading file info for ${file.name}: ${e.message}")
                    }
                }
                
                // Sort: directories first, then files, alphabetically
                files.sortWith(compareBy<SmbFile> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }
            
        } catch (e: SmbException) {
            Log.e(TAG, "SMB error listing files: ${e.message}")
            throw IOException("Could not access SMB share: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            throw IOException("Error accessing files: ${e.message}")
        }
        
        Log.d(TAG, "Listed ${files.size} files/folders")
        files
    }
    
    /**
     * Download a file from SMB share to local storage
     * 
     * @param smbFile The SMB file to download
     * @param localFile The local file to save to
     * @param progressCallback Optional callback for download progress (0-100)
     * 
     * TODO: Add resume capability for interrupted downloads
     * TODO: Add download speed optimization
     * TODO: Add file integrity verification
     */
    suspend fun downloadFile(
        smbFile: SmbFile,
        localFile: File,
        progressCallback: ((Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Downloading ${smbFile.name} to ${localFile.absolutePath}")
            
            val remoteSmbFile = JcifsSmbFile(smbFile.path, cifsContext)
            
            if (!remoteSmbFile.exists() || remoteSmbFile.isDirectory) {
                throw IOException("File does not exist or is a directory")
            }
            
            // Create parent directories if they don't exist
            localFile.parentFile?.mkdirs()
            
            remoteSmbFile.inputStream.use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    val totalSize = smbFile.size
                    var totalBytesRead = 0L
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // Report progress
                        progressCallback?.let { callback ->
                            if (totalSize > 0) {
                                val progress = (totalBytesRead * 100 / totalSize).toInt()
                                callback(progress)
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Download completed: ${localFile.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: ${e.message}", e)
            // Clean up partial download
            if (localFile.exists()) {
                localFile.delete()
            }
            false
        }
    }
    
    /**
     * Get the local IP address of the device
     */
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        return null
    }
    
    /**
     * Extract subnet from IP address (assumes /24)
     */
    private fun getSubnet(ipAddress: String): String {
        val parts = ipAddress.split(".")
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }
    
    /**
     * Check if a specific port is open on a host
     */
    private fun isPortOpen(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), SCAN_TIMEOUT)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create SMB device object from IP address
     */
    private fun createSmbDevice(ipAddress: String): SmbDevice? {
        return try {
            // Try to get hostname, fall back to IP if not available
            val hostName = try {
                InetAddress.getByName(ipAddress).hostName
            } catch (e: Exception) {
                ipAddress
            }
            
            // Create SMB URL - TODO: Make protocol version configurable
            val url = "smb://$ipAddress/"
            
            SmbDevice(
                name = if (hostName != ipAddress) hostName else "SMB Device",
                address = ipAddress,
                url = url
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not create SMB device for $ipAddress: ${e.message}")
            null
        }
    }
}