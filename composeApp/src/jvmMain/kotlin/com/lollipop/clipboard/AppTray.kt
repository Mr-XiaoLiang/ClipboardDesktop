package com.lollipop.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.rememberTrayState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

@Composable
fun AppTray(
    icon: Painter,
    tooltip: String? = null,
    state: TrayState = rememberTrayState(),
    onTrayClick: (TrayClickEvent) -> Unit = {},
    onTrayAction: () -> Unit = {},
) {

    val awtIcon = remember(icon) {
        icon.toAwtImage(GlobalDensity, GlobalLayoutDirection, iconSize)
    }
    val coroutineScopeR = rememberCoroutineScope()
    val trayIcon = remember {
        TrayIcon(awtIcon).apply {
            isImageAutoSize = true
            //给托盘图标添加鼠标监听
            addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    val pointer = MouseInfo.getPointerInfo().location
                    onTrayClick(
                        TrayClickEvent(
                            e.x,
                            e.y,
                            pointer.x,
                            pointer.y,
                            ButtonType.createButtonType(e.button),
                            e.isPopupTrigger,
                            e
                        )
                    )
                }
            })
            addActionListener {
                onTrayAction()
            }
        }
    }.apply {
        if (toolTip != tooltip) {
            toolTip = tooltip
        }
    }

    DisposableEffect(Unit) {
        // 将托盘图标添加到系统的托盘实例中
        SystemTray.getSystemTray().add(trayIcon)

        state.notificationFlow
            .onEach(trayIcon::displayMessage)
            .launchIn(coroutineScopeR)

        onDispose {
//            menuWindow.dispose()
            SystemTray.getSystemTray().remove(trayIcon)
        }
    }
}

fun TrayIcon.displayMessage(notification: Notification) {
    val messageType = when (notification.type) {
        Notification.Type.None -> TrayIcon.MessageType.NONE
        Notification.Type.Info -> TrayIcon.MessageType.INFO
        Notification.Type.Warning -> TrayIcon.MessageType.WARNING
        Notification.Type.Error -> TrayIcon.MessageType.ERROR
    }
    displayMessage(notification.title, notification.message, messageType)
}

/**
 * 点击事件
 */
data class TrayClickEvent(
    val x: Int,
    val y: Int,
    val mouseX: Int,
    val mouseY: Int,
    val buttonType: ButtonType,
    val isPopupTrigger: Boolean,
    val awtEvent: MouseEvent,
)

/**
 * 按钮类型
 */
enum class ButtonType {
    LEFT,
    RIGHT,
    UNDEFINED;

    companion object {
        fun createButtonType(button: Int): ButtonType {
            return when (button) {
                MouseEvent.BUTTON1 -> LEFT
                MouseEvent.BUTTON3 -> RIGHT
                else -> UNDEFINED
            }
        }
    }
}
