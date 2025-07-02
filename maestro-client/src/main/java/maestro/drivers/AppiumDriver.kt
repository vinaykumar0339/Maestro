package maestro.drivers

import com.getvymo.appium.MaestroAppiumDriver
import maestro.Capability
import maestro.DeviceInfo
import maestro.Driver
import maestro.KeyCode
import maestro.Platform
import maestro.Point
import maestro.ScreenRecording
import maestro.SwipeDirection
import maestro.TreeNode
import maestro.ViewHierarchy
import maestro.utils.Metrics
import maestro.utils.MetricsProvider
import maestro.utils.ScreenshotUtils
import okio.Sink
import okio.buffer
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.time.Duration

class AppiumDriver(
    private val maestroAppiumDriver: MaestroAppiumDriver = MaestroAppiumDriver(),
    private val deviceId: String,
    private var capabilities: Map<String, Any>,
    private val metricsProvider: Metrics = MetricsProvider.getInstance(),
): Driver {

    private val metrics = metricsProvider.withPrefix("maestro.driver").withTags(mapOf("platform" to "<add_platform>", "deviceId" to deviceId))

    override fun name(): String {
        return "AppiumDriver"
    }

    override fun open() {
        metrics.measured("open") {
            // start the appium server.
            // think is this required for the multi session devices etc.
            maestroAppiumDriver.startServer()
            maestroAppiumDriver.createDriver(capabilities)
        }
    }

    override fun close() {
        metrics.measured("close") {
            // stop the appium server.
            maestroAppiumDriver.quit()
            maestroAppiumDriver.stopServer()
        }
    }

    override fun deviceInfo(): DeviceInfo {
        return metrics.measured("deviceInfo") {
            // Get device information using Appium
            val windowRect = maestroAppiumDriver.getWindowRect()
            return@measured DeviceInfo(
                platform = if (maestroAppiumDriver.isAndroid()) Platform.ANDROID else Platform.IOS,
                widthPixels = windowRect.size.width,
                heightPixels = windowRect.size.height,
                widthGrid = windowRect.size.width,
                heightGrid = windowRect.size.height,
            )
        }
    }

    override fun launchApp(appId: String, launchArguments: Map<String, Any>) {
        metrics.measured("operation", mapOf("command" to "launchApp", "appId" to appId)) {
            // Launch the app using Appium
            maestroAppiumDriver.launchApp(appId, launchArguments, capabilities)
        }
    }

    override fun stopApp(appId: String) {
        metrics.measured("operation", mapOf("command" to "stopApp", "appId" to appId)) {
            maestroAppiumDriver.terminateApp(appId)
        }
    }

    override fun killApp(appId: String) {
        metrics.measured("operation", mapOf("command" to "killApp", "appId" to appId)) {
            // Kill the app using Appium
            stopApp(appId)
        }
    }

    override fun clearAppState(appId: String) {
        metrics.measured("operation", mapOf("command" to "clearAppState", "appId" to appId)) {
            // Update the capability by setting noReset = false
            val updatedCapabilities = capabilities.toMutableMap()
            updatedCapabilities["noReset"] = false
            capabilities = updatedCapabilities
        }
    }

    override fun clearKeychain() {
        metrics.measured("operation", mapOf("command" to "clearKeychain")) {
            // Clear keychain is not directly supported by Appium, so this is a no-op
            maestroAppiumDriver.clearKeyChain()
        }
    }

    override fun tap(point: Point) {
        metrics.measured("operation", mapOf("command" to "tap")) {
            maestroAppiumDriver.tap(point.x, point.y)
        }
    }

    override fun longPress(point: Point) {
        metrics.measured("operation", mapOf("command" to "longPress")) {
            maestroAppiumDriver.longPress(point.x, point.y, Duration.ofMillis(3000)) // Default duration of 3000 ms
        }
    }

    override fun pressKey(code: KeyCode) {
        TODO("Not yet implemented")
    }

    private fun mapHierarchy(node: Node): TreeNode {
        val attributes = if (node is Element) {
            val attributesBuilder = mutableMapOf<String, String>()

            // text attribute
            if (node.hasAttribute("text")) {
                val text = node.getAttribute("text")
                attributesBuilder["text"] = text
            }

            if (node.hasAttribute("value")) {
                val value = node.getAttribute("value")
                if (value.isNotEmpty()) {
                    attributesBuilder["text"] = value
                }
            }

            // accessibilityText attribute
            if (node.hasAttribute("content-desc")) {
                attributesBuilder["accessibilityText"] = node.getAttribute("content-desc")
            } else if (node.hasAttribute("label")) {
                attributesBuilder["accessibilityText"] = node.getAttribute("label")
            }

            // hintText attribute
            if (node.hasAttribute("hintText")) {
                attributesBuilder["hintText"] = node.getAttribute("hintText")
            } else if (node.hasAttribute("placeholderValue")) {
                attributesBuilder["hintText"] = node.getAttribute("placeholderValue")
            }

            if (node.hasAttribute("class") && node.getAttribute("class") == "android.widget.Toast") {
                attributesBuilder["ignoreBoundsFiltering"] = true.toString()
            } else {
                attributesBuilder["ignoreBoundsFiltering"] = false.toString()
            }

            // id attribute
            if (node.hasAttribute("resource-id")) {
                attributesBuilder["resource-id"] = node.getAttribute("resource-id")
            } else if (node.hasAttribute("name") || node.hasAttribute("identifier")) {
                attributesBuilder["resource-id"] = node.getAttribute("name") ?: node.getAttribute("identifier")
            }

            if (node.hasAttribute("clickable")) {
                attributesBuilder["clickable"] = node.getAttribute("clickable")
            }

            if (node.hasAttribute("bounds")) {
                attributesBuilder["bounds"] = node.getAttribute("bounds")
            } else {
                // from appium we get x, y, width, height attributes for ios, so we need to construct bounds from them
                val x = node.hasAttribute("x") && node.getAttribute("x").isNotEmpty()
                val y = node.hasAttribute("y") && node.getAttribute("y").isNotEmpty()
                val width = node.hasAttribute("width") && node.getAttribute("width").isNotEmpty()
                val height = node.hasAttribute("height") && node.getAttribute("height").isNotEmpty()

                if (x && y && width && height) {
                    val xInt = node.getAttribute("x").toIntOrNull() ?: 0
                    val yInt = node.getAttribute("y").toIntOrNull() ?: 0
                    val widthInt = node.getAttribute("width").toIntOrNull() ?: 0
                    val heightInt = node.getAttribute("height").toIntOrNull() ?: 0
                    attributesBuilder["bounds"] = "[${xInt},${yInt},${xInt + widthInt},${yInt + heightInt}]"
                }
            }

            if (node.hasAttribute("enabled")) {
                attributesBuilder["enabled"] = node.getAttribute("enabled")
            }

            if (node.hasAttribute("focused")) {
                attributesBuilder["focused"] = node.getAttribute("focused")
            }

            if (node.hasAttribute("checked")) {
                attributesBuilder["checked"] = node.getAttribute("checked")
            }

            if (node.hasAttribute("scrollable")) {
                attributesBuilder["scrollable"] = node.getAttribute("scrollable")
            }

            if (node.hasAttribute("selected")) {
                attributesBuilder["selected"] = node.getAttribute("selected")
            }

            if (node.hasAttribute("class")) {
                attributesBuilder["class"] = node.getAttribute("class")
            }

            attributesBuilder
        } else {
            emptyMap()
        }

        val children = mutableListOf<TreeNode>()
        val childNodes = node.childNodes
        (0 until childNodes.length).forEach { i ->
            children += mapHierarchy(childNodes.item(i))
        }

        return TreeNode(
            attributes = attributes.toMutableMap(),
            children = children,
            clickable = node.getBoolean("clickable"),
            enabled = node.getBoolean("enabled"),
            focused = node.getBoolean("focused"),
            checked = node.getBoolean("checked"),
            selected = node.getBoolean("selected"),
        )
    }

    private fun Node.getBoolean(name: String): Boolean? {
        return (this as? Element)
            ?.getAttribute(name)
            ?.let { it == "true" }
    }

    override fun contentDescriptor(excludeKeyboardElements: Boolean): TreeNode {
        return metrics.measured("operation", mapOf("command" to "contentDescriptor")) {
            val document = maestroAppiumDriver.getDocumentViewHierarchy()

            val baseTree = mapHierarchy(document)

            return@measured baseTree
        }
    }

    override fun scrollVertical() {
        metrics.measured(
            "operation",
            mapOf("command" to "scrollVertical")
        ) {
            swipe(SwipeDirection.UP, 400)
        }
    }

    override fun isKeyboardVisible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun swipe(start: Point, end: Point, durationMs: Long) {
       return metrics.measured(
           "operation",
           mapOf("command" to "swipe", "start" to start.toString(), "end" to end.toString(), "durationMs" to durationMs.toString())
       ) {
           maestroAppiumDriver.scroll(
               startX = start.x,
               startY = start.y,
               endX = end.x,
               endY = end.y,
               duration = Duration.ofMillis(durationMs)
           )
       }
    }

    override fun swipe(swipeDirection: SwipeDirection, durationMs: Long) {
        metrics.measured(
            "operation",
            mapOf("command" to "swipeWithDirection", "direction" to swipeDirection.name, "durationMs" to durationMs.toString())
        ) {
            val deviceInfo = deviceInfo()
            when (swipeDirection) {
                SwipeDirection.UP -> {
                    val startX = (deviceInfo.widthGrid * 0.5f).toInt()
                    val startY = (deviceInfo.heightGrid * 0.5f).toInt()
                    val endX = (deviceInfo.widthGrid * 0.5f).toInt()
                    val endY = (deviceInfo.heightGrid * 0.1f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.DOWN -> {
                    val startX = (deviceInfo.widthGrid * 0.5f).toInt()
                    val startY = (deviceInfo.heightGrid * 0.2f).toInt()
                    val endX = (deviceInfo.widthGrid * 0.5f).toInt()
                    val endY = (deviceInfo.heightGrid * 0.9f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.RIGHT -> {
                    val startX = (deviceInfo.widthGrid * 0.1f).toInt()
                    val startY = (deviceInfo.heightGrid * 0.5f).toInt()
                    val endX = (deviceInfo.widthGrid * 0.9f).toInt()
                    val endY = (deviceInfo.heightGrid * 0.5f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.LEFT -> {
                    val startX = (deviceInfo.widthGrid * 0.9f).toInt()
                    val startY = (deviceInfo.heightGrid * 0.5f).toInt()
                    val endX = (deviceInfo.widthGrid * 0.1f).toInt()
                    val endY = (deviceInfo.heightGrid * 0.5f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }
            }
        }
    }

    override fun swipe(elementPoint: Point, direction: SwipeDirection, durationMs: Long) {
        metrics.measured("operation", mapOf("command" to "swipeWithElementPoint", "direction" to direction.toString())) {
            val deviceInfo = deviceInfo()
            when (direction) {
                SwipeDirection.UP -> {
                    val endY = (deviceInfo.heightGrid * 0.1f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = elementPoint.x,
                        startY = elementPoint.y,
                        endX = elementPoint.x,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.DOWN -> {
                    val endY = (deviceInfo.heightGrid * 0.9f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = elementPoint.x,
                        startY = elementPoint.y,
                        endX = elementPoint.x,
                        endY = endY,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.RIGHT -> {
                    val endX = (deviceInfo.widthGrid * 0.9f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = elementPoint.x,
                        startY = elementPoint.y,
                        endX = endX,
                        endY = elementPoint.y,
                        duration = Duration.ofMillis(durationMs)
                    )
                }

                SwipeDirection.LEFT -> {
                    val endX = (deviceInfo.widthGrid * 0.1f).toInt()
                    maestroAppiumDriver.scroll(
                        startX = elementPoint.x,
                        startY = elementPoint.y,
                        endX = endX,
                        endY = elementPoint.y,
                        duration = Duration.ofMillis(durationMs)
                    )
                }
            }
        }
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }

    override fun inputText(text: String) {
        metrics.measured("operation", mapOf("command" to "inputText")) {
            maestroAppiumDriver.inputText(text)
        }
    }

    override fun openLink(link: String, appId: String?, autoVerify: Boolean, browser: Boolean) {
        TODO("Not yet implemented")
    }

    override fun hideKeyboard() {
        maestroAppiumDriver.hideKeyboard()
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        metrics.measured("operation", mapOf("command" to "takeScreenshot")) {
            // Take a screenshot using Appium
            val screenshot = maestroAppiumDriver.takeScreenshot()
            out.buffer().use {
                it.write(screenshot)
            }
        }
    }

    override fun startScreenRecording(out: Sink): ScreenRecording {
        TODO("Not yet implemented")
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        TODO("Not yet implemented")
    }

    override fun eraseText(charactersToErase: Int) {
        // send backspace key for the number of characters to erase
        metrics.measured("operation", mapOf("command" to "eraseText", "charactersToErase" to charactersToErase.toString())) {
            maestroAppiumDriver.clearText(charactersToErase)
        }
    }

    override fun setProxy(host: String, port: Int) {
        TODO("Not yet implemented")
    }

    override fun resetProxy() {
        TODO("Not yet implemented")
    }

    override fun isShutdown(): Boolean {
        return metrics.measured("isShutdown") {
            // Check if the Appium driver is shutdown
            return@measured false
        }
    }

    override fun isUnicodeInputSupported(): Boolean {
        return !maestroAppiumDriver.isAndroid()
    }

    override fun waitUntilScreenIsStatic(timeoutMs: Long): Boolean {
        return metrics.measured("operation", mapOf("command" to "waitUntilScreenIsStatic", "timeoutMs" to timeoutMs.toString())) {
            ScreenshotUtils.waitUntilScreenIsStatic(timeoutMs, SCREENSHOT_DIFF_THRESHOLD, this)
        }
    }

    override fun waitForAppToSettle(initialHierarchy: ViewHierarchy?, appId: String?, timeoutMs: Int?): ViewHierarchy? {
        return ScreenshotUtils.waitForAppToSettle(initialHierarchy, this, timeoutMs)
    }

    override fun capabilities(): List<Capability> {
        return maestroAppiumDriver.isAndroid()
            .let { if (it) listOf(Capability.FAST_HIERARCHY) else emptyList() }
    }

    override fun setPermissions(appId: String, permissions: Map<String, String>) {

    }

    override fun addMedia(mediaFiles: List<File>) {
        TODO("Not yet implemented")
    }

    override fun isAirplaneModeEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setAirplaneMode(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val SCREENSHOT_DIFF_THRESHOLD = 0.005
    }

}