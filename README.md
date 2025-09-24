# Network Explorer Android App

A Kotlin-based Android application for scanning and exploring SMB (Server Message Block) network devices. This app allows users to discover SMB devices on their local network, browse shared folders, and download files to local storage.

## Features

- 🌐 **SMB Device Scanning**: Automatically discover SMB devices on the local network
- 📱 **Material Design UI**: Modern, intuitive user interface with Material Design components
- 📁 **Folder Navigation**: Browse SMB shared folders with breadcrumb navigation
- 💾 **File Downloads**: Download files from SMB shares to local device storage
- 🔒 **Permission Handling**: Proper runtime permission requests for network and storage access
- 🛡️ **Error Handling**: Comprehensive error handling with user-friendly messages
- 📊 **File Information**: Display file sizes, modification dates, and types

## Technical Architecture

### Core Components

1. **MainActivity**: Main screen with device scanning and discovery
2. **FileBrowserActivity**: SMB share browsing and file management
3. **SmbNetworkUtils**: Network operations using jcifs-ng library
4. **Data Models**: `SmbDevice` and `SmbFile` for representing network resources
5. **RecyclerView Adapters**: For displaying device and file lists

### Key Libraries

- **jcifs-ng**: SMB/CIFS protocol implementation for Java/Kotlin
- **AndroidX**: Core Android Jetpack libraries
- **Material Components**: Google's Material Design components
- **Kotlin Coroutines**: Asynchronous programming support
- **View Binding**: Type-safe view references

## Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK API 33
- Kotlin 1.8.10 or newer
- Gradle 7.4.2
- Target Android devices running API 24 (Android 7.0) or higher

## Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/The3211/NetworkExplorer-Android.git
   cd NetworkExplorer-Android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click "Open an Existing Project"
   - Navigate to the cloned repository folder
   - Select the project and click "OK"

3. **Sync Project**
   - Android Studio will automatically sync the project
   - If not, click "Sync Project with Gradle Files" in the toolbar

4. **Build the Project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run on Device/Emulator**
   - Connect an Android device or start an emulator
   - Click the "Run" button or use `./gradlew installDebug`

## Permissions

The app requires the following permissions:

### Network Permissions
- `INTERNET`: Required for SMB network communication
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `ACCESS_WIFI_STATE`: Access WiFi network information

### Storage Permissions
- `READ_EXTERNAL_STORAGE`: Read files from external storage
- `WRITE_EXTERNAL_STORAGE`: Download files to device storage (API ≤ 28)
- `READ_MEDIA_*`: Media access permissions for Android 13+ (API 33+)

## Usage

### Scanning for SMB Devices
1. Open the app and grant necessary permissions
2. Tap "Scan for SMB Devices" button
3. The app will scan the local network (typically 192.168.x.x range)
4. Discovered devices will appear in the list

### Browsing SMB Shares
1. Tap on any discovered SMB device
2. Navigate through shared folders by tapping on directory items
3. Use the back button or navigation to move up in the directory structure
4. View file information including size, type, and modification date

### Downloading Files
1. In the file browser, locate the file you want to download
2. Tap the download button next to the file
3. Monitor download progress in the dialog
4. Files are saved to `Downloads/NetworkExplorer/` folder

## Configuration

### SMB Settings
The app uses the following default SMB configurations (configurable in `SmbNetworkUtils.kt`):

```kotlin
// SMB Protocol versions
setProperty("jcifs.smb.client.minVersion", "SMB202")
setProperty("jcifs.smb.client.maxVersion", "SMB311")

// Timeouts
setProperty("jcifs.smb.client.responseTimeout", "5000")
setProperty("jcifs.smb.client.soTimeout", "5000")
```

### Network Scanning
- Default port: 445 (SMB)
- Scan timeout: 3 seconds per device
- IP range: Automatically detected from local IP (assumes /24 subnet)

## Extensibility & TODOs

The codebase includes numerous TODO comments for future enhancements:

### Authentication Support
```kotlin
// TODO: Add support for authentication (username/password)
// TODO: Add support for different SMB versions (SMB1, SMB2, SMB3)
```

### Network Discovery
```kotlin
// TODO: Implement more sophisticated network discovery
// TODO: Add support for mDNS/Bonjour discovery
// TODO: Add support for NetBIOS name resolution
```

### File Operations
```kotlin
// TODO: Add support for file upload to SMB shares
// TODO: Add file operations (delete, rename, create folder)
// TODO: Add multi-select for batch downloads
// TODO: Add resume capability for interrupted downloads
```

### UI Improvements
```kotlin
// TODO: Add search functionality within shares
// TODO: Add file preview capabilities
// TODO: Add download queue management
// TODO: Add device favorites/bookmarks
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/networkexplorer/
│   │   ├── MainActivity.kt                 # Main activity with device scanning
│   │   ├── FileBrowserActivity.kt          # SMB share browser
│   │   ├── adapter/
│   │   │   ├── SmbDeviceAdapter.kt         # Device list adapter
│   │   │   └── SmbFileAdapter.kt           # File list adapter
│   │   ├── model/
│   │   │   └── SmbModels.kt                # Data models
│   │   └── utils/
│   │       └── SmbNetworkUtils.kt          # SMB network operations
│   ├── res/
│   │   ├── layout/                         # UI layouts
│   │   ├── values/                         # Colors, strings, themes
│   │   └── drawable/                       # Icons and drawables
│   └── AndroidManifest.xml                # App permissions and components
├── build.gradle                           # Module build configuration
└── proguard-rules.pro                     # ProGuard configuration
```

## Troubleshooting

### Common Issues

1. **No devices found during scan**
   - Ensure you're on the same network as SMB devices
   - Check that SMB devices have file sharing enabled
   - Verify firewall settings on target devices

2. **Connection failed**
   - SMB device may require authentication
   - Network may block SMB traffic (port 445)
   - Device may not support the SMB version being used

3. **Download failures**
   - Check storage permissions
   - Ensure sufficient device storage space
   - Verify network connectivity stability

4. **Build errors**
   - Ensure all dependencies are synced
   - Check Android SDK installation
   - Verify internet connection for dependency downloads

### Network Requirements

- Local network access (WiFi or Ethernet)
- SMB/CIFS enabled on target devices
- Port 445 accessible between devices
- No network firewalls blocking SMB traffic

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Security Considerations

- The app currently supports guest access only
- Future versions should implement proper authentication
- Network scanning may be detected by security software
- SMB traffic is not encrypted by default (consider SMBv3)

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or contributions, please use the GitHub issues page.

---

**Note**: This is a starter implementation designed for educational and development purposes. Production use should include additional security measures, authentication, and error handling.
