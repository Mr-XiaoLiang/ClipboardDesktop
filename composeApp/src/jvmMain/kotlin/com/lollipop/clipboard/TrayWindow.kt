package com.lollipop.clipboard

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.rememberTrayState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.*
import javax.swing.border.Border

/**
 * 托盘窗口
 * 使用 JDialog 作为 JPopupMenu 载体。实现托盘菜单。
 * 允许在里面设置复杂菜单项，并解决了中文乱码问题。
 * 使用方式与 Tray() 接近
 *
 * @param icon 图标
 * @param tooltip 提示
 * @param state 控制托盘和显示通知的状态
 * @param onClick 菜单被鼠标单击时触发，无论是左键还是右键
 * @param onAction 菜单被双击时触发
 * @param onVisible 菜单显示时触发
 * @param onInvisible 菜单隐藏时触发
 * @param isSort 是否对菜单进行排序，默认为 false
 * @param setLookAndFeel 设置Swing 的皮肤。如果使用系统的皮肤请使用 UIManager.getSystemLookAndFeelClassName() 获取值
 * @param content 菜单内容
 */
@Composable
fun TrayWindow(
    icon: Painter,
    tooltip: String? = null,
    state: TrayState = rememberTrayState(),
    onClick: (TrayClickEvent) -> Unit = {},
    isSort: Boolean = false,
    onAction: () -> Unit = {},
    onVisible: () -> Unit = {},
    onInvisible: () -> Unit = {},
    style: ComponentStyle = ComponentStyle(),
    setLookAndFeel: String? = null,
    content: @Composable MenuScope.() -> Unit = {},
) {
    setLookAndFeel?.let { UIManager.setLookAndFeel(it) }


    val awtIcon = remember(icon) {
        icon.toAwtImage(GlobalDensity, GlobalLayoutDirection, iconSize)
    }

    val menuWindow = remember { JDialog() }.apply {
        isUndecorated = true
        //作为菜单载体不需要存在可以视的窗体
        setSize(0, 0)
    }

    val coroutineScopeR = rememberCoroutineScope()
    val onClickR by rememberUpdatedState(onClick)
    val onActionR by rememberUpdatedState(onAction)
    val contentR by rememberUpdatedState(content)
    val onVisibleR by rememberUpdatedState(onVisible)
    val onInvisibleR by rememberUpdatedState(onInvisible)

    //创建JPopupMenu
    val menu: JPopupMenu = remember {
        TrayMenu(
            onVisible = {
                menuWindow.isVisible = true
                onVisibleR()
            },
            onInvisible = {
                menuWindow.isVisible = false
                onInvisibleR()
            }
        )
    }.apply {
        style.setStyle2(this)
    }
    val menuScopeR by rememberUpdatedState(MenuScope(menu, isSort = isSort))

    //重绘菜单
    menu.removeAll()
    contentR(menuScopeR)
    val menuSizeR = calculationMenuSize(menu)


    val trayIcon = remember {
        TrayIcon(awtIcon).apply {
            isImageAutoSize = true
            //给托盘图标添加鼠标监听
            addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    val pointer = MouseInfo.getPointerInfo().location
                    onClickR(
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
                    if (e.button == 3 && e.isPopupTrigger) {
                        openMenu(pointer, menuWindow, menu, menuSizeR)
                    }
                }
            })

            addActionListener {
                onActionR()
            }
        }
    }.apply {
        if (toolTip != tooltip) toolTip = tooltip
    }

    DisposableEffect(Unit) {
        // 将托盘图标添加到系统的托盘实例中
        SystemTray.getSystemTray().add(trayIcon)

        state.notificationFlow
            .onEach(trayIcon::displayMessage)
            .launchIn(coroutineScopeR)

        onDispose {
            menuWindow.dispose()
            SystemTray.getSystemTray().remove(trayIcon)
        }
    }
}


/**
 * 弹出菜单
 * @param menuWindow 菜单绑定的容器
 * @param menu 菜单
 */
private fun openMenu(pointer: Point, menuWindow: JDialog, menu: JPopupMenu, menuSize: Dimension) {
    val x = pointer.x
    val y = pointer.y
    //右键点击弹出JPopupMenu绑定的载体以及JPopupMenu
    menuWindow.setLocation(x, y)
    menuWindow.isVisible = true
    menu.show(menuWindow, 3, 0 - (menuSize.height + 3))
}

/**
 * 计算菜单的尺寸
 */
fun calculationMenuSize(menu: JPopupMenu): Dimension {
    var menuHeight = 0
    var menuWidth = 0
    for (component in menu.components) {
        if (component is JMenuItem && component.isVisible) {
            val size = component.getPreferredSize()
            menuHeight += size.height
            menuWidth += size.width
        }
    }

    return Dimension(menuWidth, menuHeight)
}

/**
 * 菜单域，用于添加控件
 */
class MenuScope(val menu: JPopupMenu, val menuItem: JMenu? = null, var isSort: Boolean = false) {
    private fun Painter.toAwtImageIcon(): ImageIcon {
        return ImageIcon(toAwtImage(GlobalDensity, GlobalLayoutDirection))
    }


    companion object {
        private val orderMap = HashMap<Int, Int>()
        private val COM = HashMap<Int, HashSet<Order>>()
    }

    data class Order(
        val key: UUID,
        var order: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Order) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }


    fun getItemCount(): Int {
        return menuItem?.itemCount ?: menu.componentCount
    }

    private fun getOrderKey(): Int {
        return menuItem?.hashCode() ?: menu.hashCode()
    }

    @Composable
    private fun rememberOrder(): Int {
        if (!isSort) return -1
        val orderKey = getOrderKey()
        val key by remember { mutableStateOf(UUID.randomUUID()) }

        val list = COM.getOrPut(orderKey) {
            hashSetOf()
        }

        var order = list.lastOrNull { it.key == key }
        if (order == null) {
            order = Order(key, list.size)
            if (order.order <= getItemCount()) list.add(order)
            else order.order -= 1
        }

//        println("${if (menuItem != null) "menuItem" else "menu"} : $order itemCount: ${getItemCount()}   key: $key")
        return order.order
    }

    private fun removeOrder(order: Int) {
        if (order == -1) return
        val orderKey = getOrderKey()
        val list = COM[orderKey] ?: return
        if (list.isEmpty()) return
        list.removeIf {
            it.order == order
        }
        val result = list.filter {
            it.order >= order
        }.map {
            Order(it.key, it.order - 1)
        }
        result.forEach { rus ->
            list.removeIf {
                it.key == rus.key
            }
        }
        list.addAll(result)
    }

    /**
     * 通用菜单项
     *
     * @param text 菜单项文本内容，默认为 null
     * @param icon 菜单项图标，默认为 null
     * @param enabled 是否启用，默认为 true
     * @param mnemonic 快捷键字符，默认为 null
     * @param style 组件样式，默认为 [ComponentStyle]
     * @param orderIndex 菜单项排序索引，默认为 -1
     * @param onClick 点击菜单项时的回调函数
     */
    @Composable
    fun Item(
        text: String? = null,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        style: ComponentStyle = ComponentStyle(),
        orderIndex: Int = -1,
        onClick: () -> Unit = {},
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }

        fun createItem() = JMenuItem(text, icon?.toAwtImageIcon()).apply {
            addActionListener {
                if (isEnabled) onClick()
            }
            if (mnemonic != null) this.accelerator = KeyStroke.getKeyStroke(mnemonic.uppercaseChar())
            isEnabled = enabled
            style.setStyle(this)
//            println("text: $text  order: $order sort:$isSort")
            menuItem?.add(this, order) ?: menu.add(this, order)
        }

        var item by remember { mutableStateOf(createItem()) }

        LaunchedEffect(icon, text, enabled, onClick, style.id(), mnemonic, order) {
            menuItem?.remove(item) ?: menu.remove(item)
            item = createItem()
        }

        if (menuItem != null) {
            menuItem.remove(item)
            menuItem.add(item, order)
        }


        DisposableEffect(Unit) {
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }


    /**
     * 文字标签
     *
     * @param text 标签文本内容
     * @param enabled 是否启用，默认为 true
     * @param mnemonic 快捷键字符，默认为 null
     * @param style 组件样式，默认为 [ComponentStyle]
     * @param orderIndex 标签排序索引，默认为 -1
     * @param onClick 点击标签时的回调函数
     */
    @Composable
    fun Label(
        text: String,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        style: ComponentStyle = ComponentStyle(),
        orderIndex: Int = -1,
        onClick: () -> Unit = {},
    ) {
        Item(text, enabled = enabled, mnemonic = mnemonic, style = style, onClick = onClick, orderIndex = orderIndex)
    }

    /**
     * 分割线
     * @param orderIndex 排序序号，-1表示默认排序
     */
    @Composable
    fun Separator(orderIndex: Int = -1) {
        check(menuItem == null) { "Separator only support menu" }
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }
        val jSeparator = remember {
            JSeparator(SwingConstants.HORIZONTAL).apply {
                menu.add(this, order)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                menu.remove(jSeparator)
                removeOrder(order)
            }
        }
    }

    /**
     * 垂直分割线
     * @param orderIndex 排序序号，-1表示默认排序
     */
    @Composable
    fun VerticalSeparator(orderIndex: Int = -1) {
        check(menuItem == null) { "VerticalSeparator only support menu" }
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }
        val jSeparator = remember {
            JSeparator(SwingConstants.VERTICAL).apply {
                menu.add(this, order)
                removeOrder(order)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                menu.remove(jSeparator)
                removeOrder(order)
            }
        }
    }

    /**
     * 复选框菜单项
     *
     * @param text 菜单项文本内容，默认为 null
     * @param icon 菜单项图标，默认为 null
     * @param selected 是否选中，默认为 false
     * @param enabled 是否启用，默认为 true
     * @param mnemonic 快捷键字符，默认为 null
     * @param style 组件样式，默认为 [ComponentStyle]
     * @param orderIndex 菜单项排序索引，默认为 -1
     * @param onCheckedChange 复选框状态变化时的回调函数
     */
    @Composable
    fun CheckboxItem(
        text: String? = null,
        icon: Painter? = null,
        selected: Boolean = false,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        style: ComponentStyle = ComponentStyle(),
        orderIndex: Int = -1,
        onCheckedChange: (Boolean) -> Unit = {},
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }

        fun createItem() = JCheckBoxMenuItem(text, icon?.toAwtImageIcon(), selected).apply {
            addActionListener {
                onCheckedChange(isSelected)
            }
            if (mnemonic != null) this.accelerator = KeyStroke.getKeyStroke(mnemonic.uppercaseChar())
            isEnabled = enabled
            style.setStyle(this)
            menuItem?.add(this, order) ?: menu.add(this, order)
        }

        var item by remember { mutableStateOf(createItem()) }

        LaunchedEffect(icon, text, enabled, selected, style.id(), mnemonic, onCheckedChange, orderIndex) {
            menuItem?.remove(item) ?: menu.remove(item)
            item = createItem()
        }

        DisposableEffect(Unit) {
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }

    /**
     * 单选按钮菜单项
     *
     * @param text 菜单项文本内容，默认为 null
     * @param icon 菜单项图标，默认为 null
     * @param selected 是否选中，默认为 false
     * @param enabled 是否启用，默认为 true
     * @param style 组件样式，默认为 [ComponentStyle]
     * @param orderIndex 菜单项排序索引，默认为 -1
     * @param onCheckedChange 单选按钮状态变化时的回调函数
     *
     */
    @Composable
    fun RadioButtonItem(
        text: String? = null,
        icon: Painter? = null,
        selected: Boolean = false,
        enabled: Boolean = true,
        style: ComponentStyle = ComponentStyle(),
        orderIndex: Int = -1,
        onCheckedChange: (Boolean) -> Unit = {},
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }

        fun createItem() = JRadioButton(text, icon?.toAwtImageIcon(), selected).apply {
            addActionListener {
                onCheckedChange(isSelected)
            }
            isEnabled = enabled
            style.setStyle(this)
            menuItem?.add(this, order) ?: menu.add(this, order)
        }

        var item by remember {
            mutableStateOf(createItem())
        }

        LaunchedEffect(icon, text, enabled, selected, style.id(), onCheckedChange, orderIndex) {
            menuItem?.remove(item) ?: menu.remove(item)
            item = createItem()
        }

        DisposableEffect(Unit) {
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }

    /**
     * 子菜单
     *
     * @param text 子菜单名称
     * @param visible 是否可见，默认为 true
     * @param enabled 是否启用，默认为 true
     * @param mnemonic 快捷键字符，默认为 null
     * @param style 组件样式，默认为 [ComponentStyle]
     * @param orderIndex 菜单项排序索引，默认为 -1
     * @param content 菜单内容的组合构建器
     *
     */
    @Composable
    fun Menu(
        text: String = "子菜单",
        visible: Boolean = true,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        style: ComponentStyle = ComponentStyle(),
        orderIndex: Int = -1,
        content: @Composable MenuScope.() -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }

        fun createItem() = JMenu(text).apply {
            isVisible = visible
            isEnabled = enabled

            if (mnemonic != null) this.accelerator = KeyStroke.getKeyStroke(mnemonic.uppercaseChar())
            style.setStyle(this)
            menuItem?.add(this, order) ?: menu.add(this, order)
        }

        var item by remember {
            mutableStateOf(createItem())
        }


        MenuScope(menu, item, isSort = isSort).apply {
            content(this)
        }

        LaunchedEffect(text, enabled, visible, style.id(), content, mnemonic, orderIndex) {
            menuItem?.remove(item) ?: menu.remove(item)
            item = createItem()
        }

        DisposableEffect(Unit) {
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }

    @Deprecated("可能存在bug")
    @Composable
    fun Component(
        orderIndex: Int = -1,
        content: @Composable MenuScope.() -> Component,
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }
        val item by rememberUpdatedState(content())

        DisposableEffect(order, content) {
            menuItem?.add(item, order) ?: menu.add(item, order)
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }

    @Deprecated("可能存在bug")
    @Composable
    fun Component(
        orderIndex: Int = -1,
        component: Component,
    ) {
        val scope = rememberCoroutineScope()
        val order = if (orderIndex >= 0) {
            scope.launch { withContext(Dispatchers.IO) { initCustomSorting() } }
            if (isSort) orderIndex else -1
        } else {
            rememberOrder()
        }

        val item = remember { component }
        menuItem?.add(item, order) ?: menu.add(item, order)

        DisposableEffect(orderIndex, component) {
            onDispose {
                menuItem?.remove(item) ?: menu.remove(item)
                removeOrder(order)
            }
        }
    }


    /**
     * 初始化菜单排序
     */
    private fun initCustomSorting() {
        if (!isSort) return
        if (menu.components.count { !it.isVisible } <= 9) {
            for (i in 0..10) {
                menu.add(JMenuItem("Null").apply {
                    isVisible = false
                })
            }
        }

        if (menuItem != null) {
            var count = 0
            var composeCount = 0
            for (i in 0 until menuItem.itemCount) {
                if (!menuItem.getItem(i).isVisible) {
                    count++
                } else {
                    composeCount++
                }
            }
            if (count <= 9) {
                for (i in 0..10) {
                    menuItem.add(JMenuItem("Null").apply {
                        isVisible = false
                    })
                }
            }
        }
    }

}

/**
 * 菜单主体
 */
internal class TrayMenu(
    val onInvisible: () -> Unit = {},
    val onVisible: () -> Unit = {},
) : JPopupMenu() {
    init {
        setSize(100, 30)
    }

    override fun firePopupMenuWillBecomeInvisible() {
        onInvisible()
    }

    override fun firePopupMenuWillBecomeVisible() {
        super.firePopupMenuWillBecomeVisible()
        onVisible()
    }
}

/**
 * 组件样式
 */
data class ComponentStyle(
    /**
     * 组件字体
     */
    val font: Font? = null,
    /**
     * 组件背景色
     */
    val background: androidx.compose.ui.graphics.Color? = null,
    /**
     * 组件文字颜色
     */
    val foreground: androidx.compose.ui.graphics.Color? = null,
    /**
     * 组件边框
     */
    val border: Border? = null,
    /**
     * 组件边距
     */
    val margin: Insets? = null,
    /**
     * 组件位置
     */
    val bounds: Rectangle? = null,
    /**
     * 组件位置
     */
    val location: Point? = null,
    /**
     * 组件大小
     */
    val size: Dimension? = null,
) {
    private var color: Color? = background?.toAwtColor()

    /**
     * 鼠标进入事件
     */
    val onMouseEnter: (MouseEvent) -> Unit = {
        color = it.component.background
        it.component.background = color
    }

    /**
     * 鼠标离开事件
     */
    val onMouseExit: (MouseEvent) -> Unit = {
        it.component.background = color ?: Color.white
    }

    /**
     * 鼠标点击事件
     */
    val onMouseClick: (MouseEvent) -> Unit = {
    }

    /**
     * 鼠标按下事件
     */
    val onMousePressed: (MouseEvent) -> Unit = {
    }

    /**
     * 鼠标释放事件
     */
    val onMouseReleased: (MouseEvent) -> Unit = {
    }

    /**
     * 计算组件样式的唯一标识，注意部分样式未能计算到
     */
    fun id(): Int {
        val s = font?.hashCode().toString() +
                background?.toArgb().toString() +
                foreground?.toArgb().toString() +
                margin?.top.toString() + margin?.left?.toString() + margin?.bottom?.toString() + margin?.right?.toString() +
                bounds?.x?.toString() + bounds?.y.toString() + bounds?.height.toString() + bounds?.width.toString() +
                location?.x.toString() + location?.y.toString() +
                size?.height.toString() + size?.width.toString()

        return s.hashCode()
    }

    fun setStyle(component: AbstractButton) {
        val style = this
        if (font != null) component.font = font
        if (foreground != null) component.foreground = foreground.toAwtColor()
        if (background != null) component.background = background.toAwtColor()
        if (border != null) component.border = border
        if (size != null) component.size = this.size
        if (location != null) component.location = this.location
        if (margin != null) component.margin = margin
        if (bounds != null) component.bounds = bounds
        component.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                style.onMouseClick(e)
            }

            override fun mousePressed(e: MouseEvent) {
                style.onMousePressed(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                style.onMouseReleased(e)
            }

            override fun mouseEntered(e: MouseEvent) {
                style.onMouseEnter(e)
            }

            override fun mouseExited(e: MouseEvent) {
                style.onMouseExit(e)
            }
        })
    }

    fun setStyle2(component: JComponent) {
        val style = this
        if (font != null) component.font = font
        if (foreground != null) component.foreground = foreground.toAwtColor()
        if (background != null) component.background = background.toAwtColor()
        if (border != null) component.border = border
        if (size != null) component.size = this.size
        if (location != null) component.location = this.location
        if (bounds != null) component.bounds = bounds
        component.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                style.onMouseClick(e)
            }

            override fun mousePressed(e: MouseEvent) {
                style.onMousePressed(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                style.onMouseReleased(e)
            }

            override fun mouseEntered(e: MouseEvent) {
                style.onMouseEnter(e)
            }

            override fun mouseExited(e: MouseEvent) {
                style.onMouseExit(e)
            }
        })
    }
}


// 辅助函数
// 来自于 Compose 内部的函数，不确定是否会引发问题
internal val GlobalDensity
    get() = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
        .density
private val GraphicsConfiguration.density: Density
    get() = Density(
        defaultTransform.scaleX.toFloat(),
        fontScale = 1f
    )

internal val GlobalLayoutDirection get() = Locale.getDefault().layoutDirection
internal val Locale.layoutDirection: LayoutDirection
    get() = ComponentOrientation.getOrientation(this).layoutDirection
internal val ComponentOrientation.layoutDirection: LayoutDirection
    get() = when {
        isLeftToRight -> LayoutDirection.Ltr
        isHorizontal -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

internal val iconSize = when (DesktopPlatform.Current) {
    // https://doc.qt.io/qt-5/qtwidgets-desktop-systray-example.html (search 22x22)
    DesktopPlatform.Linux -> Size(22f, 22f)
    // https://doc.qt.io/qt-5/qtwidgets-desktop-systray-example.html (search 16x16)
    DesktopPlatform.Windows -> Size(16f, 16f)
    // https://medium.com/@acwrightdesign/creating-a-macos-menu-bar-application-using-swiftui-54572a5d5f87
    DesktopPlatform.MacOS -> Size(22f, 22f)
    DesktopPlatform.Unknown -> Size(32f, 32f)
}

enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        /**
         * Identify OS on which the application is currently running.
         */
        val Current: DesktopPlatform by lazy {
            val name = System.getProperty("os.name")
            when {
                name?.startsWith("Linux") == true -> Linux
                name?.startsWith("Win") == true -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toAwtColor(): Color = Color(this.red, this.green, this.blue, this.alpha)

