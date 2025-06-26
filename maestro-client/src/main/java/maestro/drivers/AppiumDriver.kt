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
import okio.Sink
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.time.Duration

class AppiumDriver(
    private val maestroAppiumDriver: MaestroAppiumDriver = MaestroAppiumDriver(),
    private val deviceId: String,
    private val capabilities: Map<String, Any>,
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
//            maestroAppiumDriver.getDriver().quit()
//            maestroAppiumDriver.stopServer()
        }
    }

    override fun deviceInfo(): DeviceInfo {
        return metrics.measured("deviceInfo") {
            // Get device information using Appium
//            val windowRect = maestroAppiumDriver.getWindowRect()
//            return@measured DeviceInfo(
//                platform = if (maestroAppiumDriver.isAndroid()) Platform.ANDROID else Platform.IOS,
//                widthPixels = windowRect.size.width,
//                heightPixels = windowRect.size.height,
//                widthGrid = windowRect.size.width,
//                heightGrid = windowRect.size.height,
//            )
            return@measured DeviceInfo(
                platform = Platform.ANDROID,
                widthPixels = 1080,
                heightPixels = 1920,
                widthGrid = 1080,
                heightGrid = 1920,
            )
        }
    }

    override fun launchApp(appId: String, launchArguments: Map<String, Any>) {
        metrics.measured("operation", mapOf("command" to "launchApp", "appId" to appId)) {
            // Launch the app using Appium
            maestroAppiumDriver.launchApp(appId)
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

    }

    override fun clearKeychain() {
        TODO("Not yet implemented")
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

            if (node.hasAttribute("text")) {
                val text = node.getAttribute("text")
                attributesBuilder["text"] = text
            }

            if (node.hasAttribute("content-desc")) {
                attributesBuilder["accessibilityText"] = node.getAttribute("content-desc")
            }

            if (node.hasAttribute("hintText")) {
                attributesBuilder["hintText"] = node.getAttribute("hintText")
            }

            if (node.hasAttribute("class") && node.getAttribute("class") == "android.widget.Toast") {
                attributesBuilder["ignoreBoundsFiltering"] = true.toString()
            } else {
                attributesBuilder["ignoreBoundsFiltering"] = false.toString()
            }

            if (node.hasAttribute("resource-id")) {
                attributesBuilder["resource-id"] = node.getAttribute("resource-id")
            }

            if (node.hasAttribute("clickable")) {
                attributesBuilder["clickable"] = node.getAttribute("clickable")
            }

            if (node.hasAttribute("bounds")) {
                attributesBuilder["bounds"] = node.getAttribute("bounds")
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
        TODO("Not yet implemented")
    }

    override fun isKeyboardVisible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun swipe(start: Point, end: Point, durationMs: Long) {
        TODO("Not yet implemented")
    }

    override fun swipe(swipeDirection: SwipeDirection, durationMs: Long) {
        TODO("Not yet implemented")
    }

    override fun swipe(elementPoint: Point, direction: SwipeDirection, durationMs: Long) {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }

    override fun inputText(text: String) {
        metrics.measured("operation", mapOf("command" to "inputText")) {
            maestroAppiumDriver
        }
    }

    override fun openLink(link: String, appId: String?, autoVerify: Boolean, browser: Boolean) {
        TODO("Not yet implemented")
    }

    override fun hideKeyboard() {
        TODO("Not yet implemented")
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        TODO("Not yet implemented")
    }

    override fun startScreenRecording(out: Sink): ScreenRecording {
        TODO("Not yet implemented")
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        TODO("Not yet implemented")
    }

    override fun eraseText(charactersToErase: Int) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun waitUntilScreenIsStatic(timeoutMs: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun waitForAppToSettle(initialHierarchy: ViewHierarchy?, appId: String?, timeoutMs: Int?): ViewHierarchy? {
        TODO("Not yet implemented")
    }

    override fun capabilities(): List<Capability> {
        TODO("Not yet implemented")
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

}