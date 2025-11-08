package com.lollipop.clipboard

import androidx.compose.runtime.snapshots.SnapshotStateList
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


object DataManager {

    val dataList = SnapshotStateList<Data>()

    fun put(value: String) {
        val data = dataList.find { it.value == value }
        if (data != null) {
            return
        } else {
            dataList.add(0, Data(value))
        }
    }

    fun copy(data: Data) {
        data.copyCountUp()
        // 获取系统剪贴板
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        // 封装字符串
        val selection = StringSelection(data.value)
        // 设置剪贴板内容
        clipboard.setContents(selection, null)
    }

    fun sort() {
        val newList = dataList.sortedByDescending { it.createTime }.sortedByDescending { it.copyCount.value }
        dataList.clear()
        dataList.addAll(newList)
    }

    fun remove(data: Data) {
        dataList.remove(data)
    }

}