# Development Guide - Network Explorer Android App

## Overview
This guide provides detailed information for developers who want to extend or modify the Network Explorer Android app.

## Code Architecture

### MVVM Pattern Implementation
The app follows a simplified MVVM (Model-View-ViewModel) pattern:

- **Models**: Located in `com.networkexplorer.model` package
- **Views**: Activities and their corresponding layout files
- **Business Logic**: Contained in utility classes and activities

### Key Classes

#### 1. SmbNetworkUtils.kt
Core networking class that handles all SMB operations.

**Key Methods:**
- `scanForDevices()`: Discovers SMB devices on the local network
- `listFiles()`: Lists files and directories in an SMB share
- `downloadFile()`: Downloads files from SMB shares

**Extension Points:**
```kotlin
// Add authentication support
fun connectWithCredentials(device: SmbDevice, username: String, password: String): CIFSContext

// Add upload functionality
suspend fun uploadFile(localFile: File, remotePath: String, progressCallback: ((Int) -> Unit)? = null): Boolean

// Add file operations
suspend fun createDirectory(parentPath: String, dirName: String): Boolean
suspend fun deleteFile(filePath: String): Boolean
suspend fun renameFile(oldPath: String, newPath: String): Boolean
```

#### 2. MainActivity.kt
Main entry point that handles device scanning and display.

**Extension Points:**
```kotlin
// Add manual device entry
private fun showAddDeviceDialog()

// Add device bookmarks
private fun saveDeviceToBookmarks(device: SmbDevice)
private fun loadBookmarkedDevices(): List<SmbDevice>

// Add advanced scanning options
private fun showScanOptionsDialog()
```

#### 3. FileBrowserActivity.kt
Handles SMB share browsing and file operations.

**Extension Points:**
```kotlin
// Add file upload
private fun showUploadDialog()
private fun uploadFiles(files: List<File>)

// Add file operations context menu
private fun showFileContextMenu(file: SmbFile)

// Add search functionality
private fun searchFiles(query: String)
```

## Adding New Features

### 1. Authentication Support

To add username/password authentication:

1. **Update SmbModels.kt**:
```kotlin
data class SmbCredentials(
    val username: String,
    val password: String,
    val domain: String = ""
)

data class SmbDevice(
    val name: String,
    val address: String,
    val url: String,
    var credentials: SmbCredentials? = null
)
```

2. **Update SmbNetworkUtils.kt**:
```kotlin
private fun createAuthenticatedContext(credentials: SmbCredentials): CIFSContext {
    val auth = NtlmPasswordAuthentication(
        credentials.domain,
        credentials.username,
        credentials.password
    )
    return cifsContext.withCredentials(auth)
}
```

3. **Add login dialog in MainActivity**:
```kotlin
private fun showLoginDialog(device: SmbDevice) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null)
    // Implement login dialog logic
}
```

### 2. File Upload Functionality

1. **Create upload method in SmbNetworkUtils.kt**:
```kotlin
suspend fun uploadFile(
    localFile: File,
    remotePath: String,
    progressCallback: ((Int) -> Unit)? = null
): Boolean = withContext(Dispatchers.IO) {
    try {
        val remoteSmbFile = JcifsSmbFile(remotePath, cifsContext)
        
        FileInputStream(localFile).use { inputStream ->
            remoteSmbFile.outputStream.use { outputStream ->
                val buffer = ByteArray(8192)
                val totalSize = localFile.length()
                var totalBytesUploaded = 0L
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesUploaded += bytesRead
                    
                    progressCallback?.let { callback ->
                        val progress = (totalBytesUploaded * 100 / totalSize).toInt()
                        callback(progress)
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading file", e)
        false
    }
}
```

2. **Add file picker in FileBrowserActivity.kt**:
```kotlin
private val filePickerLauncher = registerForActivityResult(
    ActivityResultContracts.GetMultipleContents()
) { uris ->
    if (uris.isNotEmpty()) {
        uploadFiles(uris)
    }
}

private fun uploadFiles(uris: List<Uri>) {
    lifecycleScope.launch {
        uris.forEach { uri ->
            // Convert URI to File and upload
            val localFile = createTempFileFromUri(uri)
            val remotePath = "${currentDevice.url}$currentPath/${getFileName(uri)}"
            
            val success = smbNetworkUtils.uploadFile(localFile, remotePath) { progress ->
                runOnUiThread {
                    // Update progress UI
                }
            }
            
            if (success) {
                // Refresh file list
                loadFiles()
            }
        }
    }
}
```

### 3. Advanced Network Discovery

1. **Add mDNS/Bonjour support**:
```kotlin
// Add to build.gradle dependencies
implementation 'javax.jmdns:jmdns:3.5.8'

// In SmbNetworkUtils.kt
private fun discoverDevicesViaMdns(): List<SmbDevice> {
    val jmDNS = JmDNS.create()
    val devices = mutableListOf<SmbDevice>()
    
    val serviceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            // Handle service discovery
        }
        
        override fun serviceResolved(event: ServiceEvent) {
            val info = event.info
            if (info.type.contains("smb")) {
                val device = SmbDevice(
                    name = info.name,
                    address = info.inet4Addresses.firstOrNull()?.hostAddress ?: "",
                    url = "smb://${info.inet4Addresses.firstOrNull()?.hostAddress}/"
                )
                devices.add(device)
            }
        }
        
        override fun serviceRemoved(event: ServiceEvent) {
            // Handle service removal
        }
    }
    
    jmDNS.addServiceListener("_smb._tcp.local.", serviceListener)
    Thread.sleep(5000) // Wait for discovery
    jmDNS.removeServiceListener("_smb._tcp.local.", serviceListener)
    
    return devices
}
```

### 4. Database Integration

For persistent storage of bookmarks and settings:

1. **Add Room database dependencies**:
```kotlin
// build.gradle
implementation "androidx.room:room-runtime:2.5.0"
implementation "androidx.room:room-ktx:2.5.0"
kapt "androidx.room:room-compiler:2.5.0"
```

2. **Create database entities**:
```kotlin
@Entity(tableName = "bookmarked_devices")
data class BookmarkedDevice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val url: String,
    val lastAccessed: Long = System.currentTimeMillis()
)
```

### 5. Improved Error Handling

Create a centralized error handling system:

```kotlin
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val exception: Exception) : NetworkResult<T>()
    data class Loading<T>(val message: String) : NetworkResult<T>()
}

class NetworkErrorHandler {
    companion object {
        fun handleSmbException(exception: SmbException): String {
            return when (exception.ntStatus) {
                NtStatus.NT_STATUS_ACCESS_DENIED -> "Access denied. Authentication required."
                NtStatus.NT_STATUS_BAD_NETWORK_NAME -> "Network path not found."
                NtStatus.NT_STATUS_NETWORK_UNREACHABLE -> "Network unreachable."
                else -> "Connection failed: ${exception.message}"
            }
        }
    }
}
```

## UI Customization

### Material Design Theming

1. **Update colors.xml**:
```xml
<resources>
    <color name="primary_color">#1976D2</color>
    <color name="primary_variant_color">#1565C0</color>
    <color name="secondary_color">#03DAC6</color>
    <color name="surface_color">#FFFFFF</color>
    <color name="background_color">#F5F5F5</color>
    <color name="error_color">#B00020</color>
</resources>
```

2. **Update themes.xml**:
```xml
<style name="Theme.NetworkExplorer" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/primary_color</item>
    <item name="colorPrimaryVariant">@color/primary_variant_color</item>
    <item name="colorSecondary">@color/secondary_color</item>
    <item name="android:colorBackground">@color/background_color</item>
    <item name="colorSurface">@color/surface_color</item>
    <item name="colorError">@color/error_color</item>
</style>
```

### Custom File Type Icons

Create a comprehensive icon mapping system:

```kotlin
class FileIconResolver {
    companion object {
        private val iconMap = mapOf(
            // Documents
            "pdf" to R.drawable.ic_file_pdf,
            "doc" to R.drawable.ic_file_word,
            "docx" to R.drawable.ic_file_word,
            "xls" to R.drawable.ic_file_excel,
            "xlsx" to R.drawable.ic_file_excel,
            "ppt" to R.drawable.ic_file_powerpoint,
            "pptx" to R.drawable.ic_file_powerpoint,
            
            // Images
            "jpg" to R.drawable.ic_file_image,
            "jpeg" to R.drawable.ic_file_image,
            "png" to R.drawable.ic_file_image,
            "gif" to R.drawable.ic_file_image,
            "bmp" to R.drawable.ic_file_image,
            "svg" to R.drawable.ic_file_image,
            
            // Audio
            "mp3" to R.drawable.ic_file_audio,
            "wav" to R.drawable.ic_file_audio,
            "flac" to R.drawable.ic_file_audio,
            "aac" to R.drawable.ic_file_audio,
            
            // Video
            "mp4" to R.drawable.ic_file_video,
            "avi" to R.drawable.ic_file_video,
            "mkv" to R.drawable.ic_file_video,
            "mov" to R.drawable.ic_file_video,
            
            // Archives
            "zip" to R.drawable.ic_file_archive,
            "rar" to R.drawable.ic_file_archive,
            "7z" to R.drawable.ic_file_archive,
            "tar" to R.drawable.ic_file_archive,
            "gz" to R.drawable.ic_file_archive
        )
        
        fun getIconForFile(extension: String): Int {
            return iconMap[extension.lowercase()] ?: R.drawable.ic_file_default
        }
    }
}
```

## Testing

### Unit Tests
Create comprehensive unit tests for core functionality:

```kotlin
@RunWith(MockitoJUnitRunner::class)
class SmbNetworkUtilsTest {
    
    @Mock
    private lateinit var context: Context
    
    private lateinit var smbNetworkUtils: SmbNetworkUtils
    
    @Before
    fun setup() {
        smbNetworkUtils = SmbNetworkUtils(context)
    }
    
    @Test
    fun `scanForDevices should return list of devices`() = runBlocking {
        // Test implementation
    }
    
    @Test
    fun `downloadFile should handle network errors gracefully`() = runBlocking {
        // Test implementation
    }
}
```

### UI Tests
Create Espresso tests for UI interactions:

```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun scanButtonClickStartsScan() {
        onView(withId(R.id.btnScanDevices))
            .perform(click())
            
        onView(withId(R.id.progressBar))
            .check(matches(isDisplayed()))
    }
}
```

## Performance Optimization

### Network Operations
- Use connection pooling for SMB connections
- Implement caching for frequently accessed directories
- Use background threads for all network operations

### UI Performance
- Use RecyclerView with proper ViewHolder patterns
- Implement image caching for file thumbnails
- Use lazy loading for large directory listings

### Memory Management
- Implement proper lifecycle management
- Use weak references for callbacks
- Close SMB connections properly

## Security Considerations

### Data Protection
- Store credentials securely using Android Keystore
- Implement certificate pinning for HTTPS connections
- Validate all user inputs

### Network Security
- Support only secure SMB versions (SMBv2/3)
- Implement timeout mechanisms
- Add network security configuration

This guide provides a foundation for extending the Network Explorer app. Follow Android development best practices and Material Design guidelines when implementing new features.