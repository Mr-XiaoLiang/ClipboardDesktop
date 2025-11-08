package com.lollipop.clipboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import clipboard.composeapp.generated.resources.Res
import clipboard.composeapp.generated.resources.tray_icon_v
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    var isAppRunning by remember { mutableStateOf(true) }
    if (isAppRunning) {
        Window(
            onCloseRequest = {
                isAppRunning = false
            },
            title = "clipboard",
        ) {
            App()
        }
    }
    Tray(
        icon = painterResource(Res.drawable.tray_icon_v),
        menu = {
            Item("Open", onClick = { isAppRunning = true })
            Item("Exit", onClick = ::exitApplication)
        },
        tooltip = "Clipboard",
        onAction = {
            isAppRunning = true
        }
    )
}