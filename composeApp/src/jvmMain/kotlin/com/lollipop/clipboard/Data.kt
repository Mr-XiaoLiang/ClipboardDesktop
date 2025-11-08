package com.lollipop.clipboard

import androidx.compose.runtime.mutableIntStateOf

class Data(val value: String) {

    val copyCount = mutableIntStateOf(0)
    val createTime = System.currentTimeMillis()

    fun copyCountUp() {
        copyCount.value++
    }

    override fun toString(): String {
        return value
    }
}