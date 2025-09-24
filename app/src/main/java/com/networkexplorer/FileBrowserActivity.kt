package com.networkexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.networkexplorer.adapter.SmbFileAdapter
import com.networkexplorer.databinding.ActivityFileBrowserBinding
import com.networkexplorer.model.SmbDevice
import com.networkexplorer.model.SmbFile
import com.networkexplorer.utils.SmbNetworkUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity for browsing SMB shares and downloading files
 * 
 * This activity handles:
 * - Browsing SMB shared folders
 * - Navigating through directory structure
 * - Downloading files to local storage
 * - Managing download progress
 * 
 * TODO: Add support for file upload to SMB shares
 * TODO: Add file operations (delete, rename, create folder)
 * TODO: Add multi-select for batch downloads
 * TODO: Add download queue management
 * TODO: Add file preview capabilities
 * TODO: Add search functionality within shares
 */
class FileBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileBrowserBinding
    private lateinit var fileAdapter: SmbFileAdapter
    private lateinit var smbNetworkUtils: SmbNetworkUtils
    private lateinit var currentDevice: SmbDevice
    
    private var fileList = mutableListOf<SmbFile>()
    private var currentPath = ""
    private val pathHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFromIntent()
        initializeComponents()
        setupUI()
        loadFiles()
    }

    private fun initializeFromIntent() {
        val deviceName = intent.getStringExtra("device_name") ?: "Unknown Device"
        val deviceAddress = intent.getStringExtra("device_address") ?: ""
        val deviceUrl = intent.getStringExtra("device_url") ?: ""

        if (deviceAddress.isEmpty() || deviceUrl.isEmpty()) {
            Toast.makeText(this, "Invalid device information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentDevice = SmbDevice(deviceName, deviceAddress, deviceUrl)
    }

    private fun initializeComponents() {
        smbNetworkUtils = SmbNetworkUtils(this)
        
        fileAdapter = SmbFileAdapter(
            files = fileList,
            onItemClick = { file -> onFileItemClick(file) },
            onDownloadClick = { file -> onDownloadClick(file) }
        )
    }

    private fun setupUI() {
        // Setup toolbar
        binding.toolbar.title = currentDevice.name
        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }

        // Setup RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FileBrowserActivity)
            adapter = fileAdapter
        }

        // Update path display
        updatePathDisplay()
    }

    private fun loadFiles() {
        updateUI(isLoading = true)
        
        lifecycleScope.launch {
            try {
                val files = smbNetworkUtils.listFiles(currentDevice, currentPath)
                
                fileList.clear()
                fileList.addAll(files)
                fileAdapter.updateFiles(fileList)
                
                updateUI(isLoading = false)
                
            } catch (e: Exception) {
                updateUI(isLoading = false)
                
                MaterialAlertDialogBuilder(this@FileBrowserActivity)
                    .setTitle("Connection Error")
                    .setMessage("Could not connect to ${currentDevice.name}:\\n\\n${e.message}")
                    .setPositiveButton("Retry") { _, _ -> loadFiles() }
                    .setNegativeButton("Back") { _, _ -> finish() }
                    .show()
            }
        }
    }

    private fun onFileItemClick(file: SmbFile) {
        if (file.isDirectory) {
            // Navigate into directory
            pathHistory.add(currentPath)
            currentPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
            updatePathDisplay()
            loadFiles()
        } else {
            // Show file info dialog
            showFileInfoDialog(file)
        }
    }

    private fun onDownloadClick(file: SmbFile) {
        if (!checkStoragePermission()) {
            requestStoragePermission {
                downloadFile(file)
            }
            return
        }
        
        downloadFile(file)
    }

    private fun downloadFile(file: SmbFile) {
        // Create download directory
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "NetworkExplorer"
        )
        
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        val localFile = File(downloadDir, file.name)
        
        // Show download dialog
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Downloading")
            .setMessage("Downloading ${file.name}...")
            .setNegativeButton("Cancel", null)
            .create()
        
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val success = smbNetworkUtils.downloadFile(file, localFile) { progress ->
                    runOnUiThread {
                        progressDialog.setMessage("Downloading ${file.name}... $progress%")
                    }
                }
                
                progressDialog.dismiss()
                
                if (success) {
                    Toast.makeText(
                        this@FileBrowserActivity,
                        "Downloaded to ${localFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@FileBrowserActivity,
                        "Download failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                
                MaterialAlertDialogBuilder(this@FileBrowserActivity)
                    .setTitle("Download Error")
                    .setMessage("Could not download file:\\n\\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showFileInfoDialog(file: SmbFile) {
        val message = buildString {
            append("Name: ${file.name}\\n")
            append("Size: ${file.getFormattedSize()}\\n")
            if (file.lastModified > 0) {
                append("Modified: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified))}\\n")
            }
            append("Path: ${file.path}")
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("File Information")
            .setMessage(message)
            .setPositiveButton("Download") { _, _ -> onDownloadClick(file) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun navigateBack() {
        if (pathHistory.isNotEmpty()) {
            currentPath = pathHistory.removeLastOrNull() ?: ""
            updatePathDisplay()
            loadFiles()
        } else {
            finish()
        }
    }

    private fun updatePathDisplay() {
        val displayPath = if (currentPath.isEmpty()) {
            "//${currentDevice.address}/"
        } else {
            "//${currentDevice.address}/$currentPath/"
        }
        binding.tvCurrentPath.text = displayPath
    }

    private fun updateUI(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        if (isLoading) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.GONE
        } else {
            if (fileList.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.tvEmptyMessage.visibility = View.GONE
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(onGranted: () -> Unit) {
        // TODO: Implement proper permission request for API 33+
        // For now, assume permission is granted on newer Android versions
        onGranted()
    }

    override fun onBackPressed() {
        navigateBack()
    }
}