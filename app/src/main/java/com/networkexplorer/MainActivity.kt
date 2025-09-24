package com.networkexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.networkexplorer.adapter.SmbDeviceAdapter
import com.networkexplorer.databinding.ActivityMainBinding
import com.networkexplorer.model.SmbDevice
import com.networkexplorer.utils.SmbNetworkUtils
import kotlinx.coroutines.launch

/**
 * Main activity for the Network Explorer app
 * 
 * This activity handles:
 * - Requesting necessary permissions
 * - Scanning for SMB devices on the network
 * - Displaying discovered devices
 * - Launching file browser when device is selected
 * 
 * TODO: Add support for manual device entry
 * TODO: Add device favorites/bookmarks
 * TODO: Add network scanning options (custom IP ranges)
 * TODO: Add refresh/auto-refresh functionality
 * TODO: Improve error handling and user feedback
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: SmbDeviceAdapter
    private lateinit var smbNetworkUtils: SmbNetworkUtils
    private var deviceList = mutableListOf<SmbDevice>()

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            startScan()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        checkPermissionsAndScan()
    }

    private fun initializeComponents() {
        smbNetworkUtils = SmbNetworkUtils(this)
        
        deviceAdapter = SmbDeviceAdapter(deviceList) { device ->
            openDeviceBrowser(device)
        }
    }

    private fun setupUI() {
        // Setup RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        // Setup scan button
        binding.btnScanDevices.setOnClickListener {
            if (checkPermissions()) {
                startScan()
            } else {
                requestPermissions()
            }
        }

        // Initial UI state
        updateUI(isScanning = false, showEmptyMessage = true)
    }

    private fun checkPermissionsAndScan() {
        if (checkPermissions()) {
            // Automatically start scanning when app opens
            startScan()
        }
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun requestPermissions() {
        // Show explanation dialog first
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage(getString(R.string.permission_explanation))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                requestPermissionLauncher.launch(getRequiredPermissions())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Denied")
            .setMessage("Network Explorer needs these permissions to scan for SMB devices and download files. You can grant them in the app settings.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startScan() {
        updateUI(isScanning = true, showEmptyMessage = false)
        
        lifecycleScope.launch {
            try {
                binding.tvStatus.text = getString(R.string.scanning)
                
                val devices = smbNetworkUtils.scanForDevices()
                
                deviceList.clear()
                deviceList.addAll(devices)
                deviceAdapter.updateDevices(deviceList)
                
                if (devices.isEmpty()) {
                    updateUI(isScanning = false, showEmptyMessage = true)
                    binding.tvStatus.text = getString(R.string.no_devices_found)
                } else {
                    updateUI(isScanning = false, showEmptyMessage = false)
                    binding.tvStatus.text = "Found ${devices.size} SMB device(s)"
                }
                
            } catch (e: Exception) {
                updateUI(isScanning = false, showEmptyMessage = true)
                binding.tvStatus.text = getString(R.string.error_network)
                
                Toast.makeText(
                    this@MainActivity,
                    "Scan failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI(isScanning: Boolean, showEmptyMessage: Boolean) {
        binding.progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
        binding.tvStatus.visibility = if (isScanning || showEmptyMessage) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (!isScanning && !showEmptyMessage) View.VISIBLE else View.GONE
        binding.tvEmptyMessage.visibility = if (!isScanning && showEmptyMessage && deviceList.isEmpty()) View.VISIBLE else View.GONE
        
        // Disable scan button while scanning
        binding.btnScanDevices.isEnabled = !isScanning
        binding.btnScanDevices.text = if (isScanning) getString(R.string.scanning) else getString(R.string.scan_devices)
    }

    private fun openDeviceBrowser(device: SmbDevice) {
        val intent = Intent(this, FileBrowserActivity::class.java).apply {
            putExtra("device_name", device.name)
            putExtra("device_address", device.address)
            putExtra("device_url", device.url)
        }
        startActivity(intent)
    }
}