package com.lollipop.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import clipboard.composeapp.generated.resources.Res
import clipboard.composeapp.generated.resources.ic_content_copy
import clipboard.composeapp.generated.resources.ic_delete_forever
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppContent() {
    val dataList = remember { DataManager.dataList }
    val iconPadding = 8.dp
    val iconSize = 24.dp
    val iconButtonSize = iconSize + (iconPadding * 2)
    val itemShapeRadius = 10.dp
    val iconShapeRadius = 7.dp
    LazyColumn(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
    ) {
        items(dataList, key = { it.value }) { data ->
            val copyCount by remember { data.copyCount }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(itemShapeRadius)
                    ).padding(all = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (copyCount > 0) {
                    Text(
                        text = copyCount.toString(),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(iconShapeRadius)
                            )
                            .padding(all = iconPadding),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    painter = painterResource(Res.drawable.ic_delete_forever),
                    contentDescription = "Delete",
                    modifier = Modifier.size(iconButtonSize)
                        .clip(RoundedCornerShape(iconShapeRadius))
                        .clickable(onClick = {
                            DataManager.remove(data)
                        })
                        .padding(iconPadding),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = data.value,
                    modifier = Modifier.weight(1f).padding(all = iconPadding),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_content_copy),
                    contentDescription = "Copy",
                    modifier = Modifier.size(iconButtonSize)
                        .clip(RoundedCornerShape(iconShapeRadius))
                        .clickable(onClick = {
                            DataManager.copy(data)
                        })
                        .padding(iconPadding),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}