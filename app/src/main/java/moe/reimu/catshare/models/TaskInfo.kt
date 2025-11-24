package moe.reimu.catshare.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskInfo(val id: Int, val device: DiscoveredDevice, val files: List<FileInfo>, val force5Ghz: Boolean = false) : Parcelable
