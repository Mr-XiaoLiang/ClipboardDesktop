package com.lollipop.clipboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window

@Composable
fun AppWindow(
    title: String,
    onCloseRequest: () -> Unit,
    visible: Boolean,
    undecorated: Boolean,
    alwaysOnTop: Boolean,
    windowMenu: @Composable RowScope.() -> Unit = {},
    content: @Composable (ComposeWindow) -> Unit
) {
    if (undecorated) {
        UndecoratedWindow(
            title = title,
            onCloseRequest = onCloseRequest,
            visible = visible,
            windowMenu = windowMenu,
            alwaysOnTop = alwaysOnTop,
            content = content
        )
    } else {
        DecoratedWindow(
            title = title,
            onCloseRequest = onCloseRequest,
            visible = visible,
            windowMenu = windowMenu,
            alwaysOnTop = alwaysOnTop,
            content = content
        )
    }
}

@Composable
fun UndecoratedWindow(
    title: String,
    onCloseRequest: () -> Unit,
    visible: Boolean,
    alwaysOnTop: Boolean,
    titleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    windowMenu: @Composable RowScope.() -> Unit = {},
    content: @Composable (ComposeWindow) -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = title,
        visible = visible,
        alwaysOnTop = alwaysOnTop,
        transparent = true,
        // 透明窗口必须无边框
        undecorated = true,
    ) {
        MaterialTheme {
            Surface(
                modifier = Modifier.clip(shape = MaterialTheme.shapes.large)
                    .background(color = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    WindowDraggableArea {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = titleColor,
                            )
                            AppMenuBar(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                horizontalArrangement = Arrangement.End,
                                content = windowMenu,
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(window)
                    }
                }
            }
        }
    }
}

@Composable
fun DecoratedWindow(
    title: String,
    onCloseRequest: () -> Unit,
    visible: Boolean,
    alwaysOnTop: Boolean,
    windowMenu: @Composable RowScope.() -> Unit = {},
    content: @Composable (ComposeWindow) -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = title,
        alwaysOnTop = alwaysOnTop,
        visible = visible,
    ) {
        MaterialTheme {
            Surface(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppMenuBar(
                        modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        content = windowMenu,
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(window)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppMenuBar(
    modifier: Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    content: @Composable RowScope.() -> Unit
) {

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppMenu(
    icon: Painter,
    tooltip: String,
    iconPadding: Dp = 8.dp,
    iconSize: Dp = 24.dp,
    iconShapeRadius: Dp = 7.dp,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClickCallback: () -> Unit,
) {
    val iconButtonSize by remember { mutableStateOf(iconSize + (iconPadding * 2)) }
    TooltipArea(
        tooltip = {
            Card {
                Text(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    text = tooltip
                )
            }
        }
    ) {
        Icon(
            painter = icon,
            contentDescription = tooltip,
            modifier = Modifier.size(iconButtonSize)
                .clip(RoundedCornerShape(iconShapeRadius))
                .clickable(onClick = onClickCallback)
                .padding(iconPadding),
            tint = iconColor
        )
    }
}

