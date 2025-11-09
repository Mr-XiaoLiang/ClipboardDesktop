package com.lollipop.clipboard

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import clipboard.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

fun main() {
    // 启动剪切板的监听守护线程，监听剪切板
    DeamonObserver.start()
    // 启动Compose
    application {

        val trayState = rememberTrayState()

        var isAppVisible by remember { mutableStateOf(true) }
        var isMenuExpand by remember { mutableStateOf(false) }
        val title = "Clipboard"
        var windowController by remember { mutableStateOf<ComposeWindow?>(null) }
        val menuListUndecoratedExpand = remember {
            mutableStateListOf<AppMenu>(
                AppMenu(
                    icon = Res.drawable.ic_arrow_right,
                    tooltip = "折叠",
                    onClickCallback = {
                        isMenuExpand = false
                    }
                ),
                AppMenu(
                    icon = Res.drawable.ic_exit_to_app,
                    tooltip = "退出",
                    onClickCallback = {
                        exitApplication()
                    }
                ),
                AppMenu(
                    icon = Res.drawable.ic_close,
                    tooltip = "关闭",
                    onClickCallback = {
                        isAppVisible = false
                    }
                ),
            )
        }
        val menuListUndecoratedFold = remember {
            mutableStateListOf<AppMenu>(
                AppMenu(
                    icon = Res.drawable.ic_arrow_left,
                    tooltip = "展开",
                    onClickCallback = {
                        isMenuExpand = true
                    }
                ),
                AppMenu(
                    icon = Res.drawable.ic_close,
                    tooltip = "关闭",
                    onClickCallback = {
                        isAppVisible = false
                    }
                ),
            )
        }

        UndecoratedWindow(
            title = title,
            onCloseRequest = {
                isAppVisible = false
            },
            menuList = if (isMenuExpand) {
                menuListUndecoratedExpand
            } else {
                menuListUndecoratedFold
            },
            visible = isAppVisible,
        ) { window ->
            windowController = window
            AppContent()
        }

        AppTray(
            state = trayState,
            icon = painterResource(Res.drawable.tray_icon_v),
            onTrayClick = { event ->
                if (!isAppVisible) {
                    isAppVisible = true
                } else {
                    windowController?.toFront()
                }
                DataManager.sort()
            },
        )
    }
}
