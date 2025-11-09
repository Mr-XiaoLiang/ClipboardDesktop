package com.lollipop.clipboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppWindow(
    title: String,
    onCloseRequest: () -> Unit,
    visible: Boolean,
    undecorated: Boolean,
    menuList: List<AppMenu>,
    content: @Composable (ComposeWindow) -> Unit
) {
    if (undecorated) {
        UndecoratedWindow(
            title = title,
            onCloseRequest = onCloseRequest,
            visible = visible,
            menuList = menuList,
            content = content
        )
    } else {
        DecoratedWindow(
            title = title,
            onCloseRequest = onCloseRequest,
            visible = visible,
            menuList = menuList,
            content = content
        )
    }
}

@Composable
fun UndecoratedWindow(
    title: String,
    onCloseRequest: () -> Unit,
    visible: Boolean,
    menuList: List<AppMenu>,
    content: @Composable (ComposeWindow) -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = title,
        visible = visible,
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            AppMenuBar(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                horizontalArrangement = Arrangement.End,
                                menuList = menuList,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
    menuList: List<AppMenu>,
    content: @Composable (ComposeWindow) -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = title,
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
                        menuList = menuList,
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
    iconPadding: Dp = 8.dp,
    iconSize: Dp = 24.dp,
    iconShapeRadius: Dp = 7.dp,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    menuList: List<AppMenu>
) {
    val iconButtonSize by remember { mutableStateOf(iconSize + (iconPadding * 2)) }
    LazyRow(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement
    ) {
        items(menuList, key = { item -> item.tooltip }) { item ->
            TooltipArea(
                tooltip = {
                    Card {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            text = item.tooltip
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = item.tooltip,
                    modifier = Modifier.size(iconButtonSize)
                        .clip(RoundedCornerShape(iconShapeRadius))
                        .clickable(onClick = item.onClickCallback)
                        .padding(iconPadding),
                    tint = iconColor
                )
            }
        }
    }

}

class AppMenu(
    val icon: DrawableResource,
    val tooltip: String,
    val onClickCallback: () -> Unit,
)

