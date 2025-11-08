package com.lollipop.clipboard

import kotlinx.coroutines.Runnable
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit


object DeamonObserver {

    private var isRunning = false

    private var peekDelay = 1000L

    private val peekLooper by lazy {
        PeekLooper()
    }

    fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        peekLooper.start()
        PeekTask.delay(0).run()
    }

    private fun postNext() {
        peekLooper.post(PeekTask.delay(peekDelay))
    }

    private fun peedClipboard() {
        println("DeamonObserver.peedClipboard")
        // 获取系统剪贴板
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        // 获取剪贴板内容
        val contents = clipboard.getContents(null) ?: return

        if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                // 获取文本内容
                val text = contents.getTransferData(DataFlavor.stringFlavor)
                if (text is String) {
                    onNewContent(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val text = DataFlavor.stringFlavor.getReaderForText(contents).readText()
                onNewContent(text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onNewContent(value: String) {
        DataManager.put(value)
    }

    private class PeekLooper : Thread() {

        private val delayQueue = DelayQueue<PeekTask>()

        fun post(task: PeekTask) {
            delayQueue.put(task)
        }

        override fun run() {
            while (true) {
                val peekTask = delayQueue.take()
                try {
                    peekTask.run()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

    }

    private class PeekTask(
        private val targetTime: Long
    ) : Runnable, Delayed {

        companion object {
            fun delay(delay: Long): PeekTask {
                return PeekTask(System.currentTimeMillis() + delay)
            }
        }

        override fun run() {
            try {
                peedClipboard()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                DeamonObserver.postNext()
            }
        }

        override fun getDelay(unit: TimeUnit): Long {
            return unit.convert(targetTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        override fun compareTo(other: Delayed?): Int {
            if (other == null) {
                return 1
            }
            if (other is PeekTask) {
                return this.targetTime.compareTo(other.targetTime)
            }
            return this.getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))
        }

    }

}