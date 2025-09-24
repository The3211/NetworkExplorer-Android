package com.networkexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.networkexplorer.R
import com.networkexplorer.model.SmbDevice

/**
 * Adapter for displaying SMB devices in a RecyclerView
 */
class SmbDeviceAdapter(
    private var devices: List<SmbDevice>,
    private val onDeviceClick: (SmbDevice) -> Unit
) : RecyclerView.Adapter<SmbDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.tvDeviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_smb_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        
        holder.deviceName.text = device.name
        holder.deviceAddress.text = device.address
        
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    /**
     * Update the device list and refresh the adapter
     */
    fun updateDevices(newDevices: List<SmbDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}