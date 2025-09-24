package com.networkexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.networkexplorer.R
import com.networkexplorer.model.SmbFile
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying SMB files and folders in a RecyclerView
 */
class SmbFileAdapter(
    private var files: List<SmbFile>,
    private val onItemClick: (SmbFile) -> Unit,
    private val onDownloadClick: (SmbFile) -> Unit
) : RecyclerView.Adapter<SmbFileAdapter.FileViewHolder>() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val fileName: TextView = itemView.findViewById(R.id.tvFileName)
        val fileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        val fileDate: TextView = itemView.findViewById(R.id.tvFileDate)
        val downloadButton: MaterialButton = itemView.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        
        holder.fileName.text = file.name
        holder.fileSize.text = file.getFormattedSize()
        
        // Format date
        if (file.lastModified > 0) {
            holder.fileDate.text = dateFormatter.format(Date(file.lastModified))
        } else {
            holder.fileDate.text = ""
        }
        
        // Set appropriate icon based on file type
        if (file.isDirectory) {
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
            holder.downloadButton.visibility = View.GONE
        } else {
            holder.fileIcon.setImageResource(getFileIcon(file.getExtension()))
            holder.downloadButton.visibility = View.VISIBLE
            holder.downloadButton.setOnClickListener {
                onDownloadClick(file)
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = files.size

    /**
     * Update the file list and refresh the adapter
     */
    fun updateFiles(newFiles: List<SmbFile>) {
        files = newFiles
        notifyDataSetChanged()
    }
    
    /**
     * Get appropriate icon resource based on file extension
     * TODO: Add more file type icons and better categorization
     */
    private fun getFileIcon(extension: String): Int {
        return when (extension.lowercase()) {
            "pdf" -> android.R.drawable.ic_menu_edit
            "doc", "docx" -> android.R.drawable.ic_menu_edit
            "xls", "xlsx" -> android.R.drawable.ic_menu_edit
            "ppt", "pptx" -> android.R.drawable.ic_menu_edit
            "txt" -> android.R.drawable.ic_menu_edit
            "jpg", "jpeg", "png", "gif", "bmp" -> android.R.drawable.ic_menu_camera
            "mp3", "wav", "flac", "aac" -> android.R.drawable.ic_media_play
            "mp4", "avi", "mkv", "mov" -> android.R.drawable.ic_media_play
            "zip", "rar", "7z", "tar", "gz" -> android.R.drawable.ic_menu_save
            else -> android.R.drawable.ic_menu_agenda
        }
    }
}