package com.getvymo.appium

import io.appium.java_client.AppiumDriver
import io.appium.java_client.HidesKeyboard
import io.appium.java_client.InteractsWithApps
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.nativekey.KeyEvent
import io.appium.java_client.appmanagement.ApplicationState
import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.remote.options.BaseOptions
import io.appium.java_client.service.local.AppiumDriverLocalService
import io.appium.java_client.service.local.AppiumServerHasNotBeenStartedLocallyException
import io.appium.java_client.service.local.AppiumServiceBuilder
import io.appium.java_client.service.local.flags.GeneralServerFlag
import org.openqa.selenium.Platform
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.w3c.dom.Document
import java.io.File
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory

class MaestroAppiumDriver {
    private var appiumDriver: AppiumDriver? = null
    private var appiumService: AppiumDriverLocalService? = null
    private val documentBuilderFactory = DocumentBuilderFactory.newInstance()

    fun startServer() {
        // Logic to start the Appium server
        // This is a placeholder; actual implementation will depend on your setup
        println("Starting Appium server...")
        val appiumServiceBuilder = AppiumServiceBuilder()
        appiumServiceBuilder
            .usingAnyFreePort()
            .withArgument(
                GeneralServerFlag.LOG_LEVEL,
                "error" // Set the log level to error to reduce noise in logs
            )
        appiumService = AppiumDriverLocalService.buildService(appiumServiceBuilder)

        try {
            appiumService?.start()
        } catch (e: AppiumServerHasNotBeenStartedLocallyException) {
            println("Failed to start Appium server: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("An error occurred while starting the Appium server: ${e.message}")
            throw e
        }

    }

    @Suppress("SdCardPath")
    fun addMediaToDevice(mediaFile: File) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            androidHandler = {
                it.pushFile("/sdcard/Download/${mediaFile.name}", mediaFile)
            },
            iosHandler = {
                it.pushFile("/var/mobile/Media/Downloads/${mediaFile.name}", mediaFile)
            }
        )
    }

    private inline fun <reified T : AppiumDriver?, reified I : Any?, R> handleDriverCommand(
        driver: T?,
        noinline androidHandler: ((AndroidDriver) -> R)? = null,
        noinline iosHandler: ((IOSDriver) -> R)? = null,
        noinline interfaceHandler: ((I) -> R)? = null,
        noinline handler: ((T) -> R)? = null
    ): R {
        if (driver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        return when {
            driver is AndroidDriver && androidHandler != null -> androidHandler(driver)
            driver is IOSDriver && iosHandler != null -> iosHandler(driver)
            handler != null -> handler(driver)
            driver is I && interfaceHandler != null -> interfaceHandler(driver)
            else -> throw IllegalStateException("Unsupported driver type: ${driver.javaClass.name}")
        }
    }



    private fun isAppForeground(appId: String): Boolean {
        return handleDriverCommand<AppiumDriver, InteractsWithApps, Boolean>(
            driver = appiumDriver,
            interfaceHandler = {
                val appState = it.queryAppState(appId)
                appState == ApplicationState.RUNNING_IN_FOREGROUND
            }
        )
    }

    private fun executeScript(script: String, options: Map<String, Any>) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            handler = {
                it.executeScript(script, options)
            }
        )
    }

    private fun launchAndroidApp(appId: String, launchArguments: Map<String, Any> = emptyMap(), capabilities: Map<String, Any> = emptyMap()) {
        val options = mutableMapOf<String, Any>()
        options["wait"] = true
        val appActivity = capabilities["appActivity"] ?: capabilities["appium:appActivity"] as? String
            ?: throw IllegalArgumentException("appActivity is required for Android app to launch Please check the launchApp Arguments.")

        options["component"] = "$appId/$appActivity"

        val extras = launchArguments.map { (key, value) ->
            when (value) {
                is String -> listOf("s", key, value).toTypedArray()
                is Boolean -> listOf("z", key, value.toString()).toTypedArray()
                is Int -> listOf("i", key, value.toString()).toTypedArray()
                is Long -> listOf("l", key, value.toString()).toTypedArray()
                is Float -> listOf("f", key, value.toString()).toTypedArray()
                else -> throw IllegalArgumentException("Unsupported type for intent extra: ${value.javaClass.name}")
            }
        }.toTypedArray()

        options["extras"] = extras

        return executeScript(
            script = "mobile: startActivity",
            options = options
        )

    }

    private fun launchIOSApp(appId: String, launchArguments: Map<String, Any> = emptyMap()) {
        val options = mutableMapOf<String, Any>()
        options["bundleId"] = appId

        val arguments = launchArguments.flatMap {
            listOf(
                "-${it.key}",
                it.value.toString()
            )
        }
        if (arguments.isNotEmpty()) {
            options["arguments"] = arguments
        }
        return executeScript(
            script = "mobile: launchApp",
            options = options
        )
    }

    fun launchApp(appId: String, launchArguments: Map<String, Any> = emptyMap(), capabilities: Map<String, Any> = emptyMap()) {
        // no need to do anything create Driver should automatically open the app.
        // Make sure stopApp is false from the yaml files.
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            androidHandler = {
                launchAndroidApp(appId, launchArguments, capabilities)
            },
            iosHandler = {
                launchIOSApp(appId, launchArguments)
            }
        )
    }

    fun terminateApp(appId: String) {
        return handleDriverCommand<AppiumDriver, InteractsWithApps, Unit>(
            driver = appiumDriver,
            interfaceHandler = {
                if (isAppForeground(appId)) {
                    it.terminateApp(appId)
                }
            }
        )
    }

    fun takeScreenshot(): ByteArray {
        return handleDriverCommand<AppiumDriver, Any, ByteArray>(
            driver = appiumDriver,
            handler = { it.getScreenshotAs(org.openqa.selenium.OutputType.BYTES) }
        )
    }

    private fun getDriver(): AppiumDriver {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }
        return appiumDriver!!
    }

    private fun resetDriver() {
        println("Resetting Appium driver...")
        appiumDriver = null
    }

    fun quit() {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            handler = { it.quit() }
        ).also {
            resetDriver()
        }
    }

    fun hideKeyboard() {
        return handleDriverCommand<AppiumDriver, HidesKeyboard, Unit>(
            driver = appiumDriver,
            interfaceHandler = {
                it.hideKeyboard()
            }
        )
    }

    fun createDriver(
        capabilities: Map<String, Any>,
        autoLaunch: Boolean = false,
    ): AppiumDriver {
        if (appiumService == null || !appiumService!!.isRunning) {
            throw IllegalStateException("Appium server is not running. Please start the server first.")
        }

        val nonAutoLaunchCapabilities = capabilities.toMutableMap().apply {
            set("autoLaunch", autoLaunch)
            set("appium:autoLaunch", autoLaunch)
        }
        val options = getCapabilitiesOptions(nonAutoLaunchCapabilities)
        appiumDriver = when (options.platformName) {
            Platform.ANDROID -> {
                AndroidDriver(appiumService!!.url, options)
            }

            Platform.IOS -> {
                IOSDriver(appiumService!!.url, options)
            }

            else -> {
                throw IllegalArgumentException("Unsupported platform: ${options.platformName}")
            }
        }

        return appiumDriver!!
    }

    private fun getPageSource() : String? {
        return handleDriverCommand<AppiumDriver, Any, String?>(
            driver = appiumDriver,
            handler = { it.pageSource }
        )
    }

    fun clearKeyChain() {
        if (isAndroid()) {
            // Android does not have a keychain concept like iOS, so this is a no-op
        } else {
            // TODO: need to implement the code Will check later.
        }
    }

    fun tap(
        x: Int,
        y: Int,
    ) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            appiumDriver,
            handler = {
                val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
                val tap = Sequence(pointerInput, 1)

                tap.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                tap.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                tap.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                it.perform(listOf(tap))
            }
        )

    }

    fun longPress(
        x: Int,
        y: Int,
        duration: Duration = Duration.ofMillis(1000)
    ) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            appiumDriver,
            handler = {
                val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
                val tap = Sequence(pointerInput, 1)

                tap.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                tap.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                tap.addAction(Pause(pointerInput, duration))
                tap.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                it.perform(listOf(tap))
            }
        )
    }

    fun pressAndroidKey(keyEvent: KeyEvent) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            androidHandler = {
                it.pressKey(keyEvent)
            },
        )
    }

    fun pressIosButtonPress(name: String) {
        return executeScript(
            "mobile: pressButton",
            mutableMapOf<String, Any>().apply {
                set("name", name)
            }
        )
    }

    fun getDocumentViewHierarchy(): Document {
        val pageSource = getPageSource()
        if (pageSource.isNullOrEmpty()) {
            throw IllegalStateException("Page source is empty. Ensure the app is running and the driver is connected.")
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(pageSource.byteInputStream())

        return document
    }

    fun inputText(text: String) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            handler = { it.switchTo().activeElement().sendKeys(text) }
        )
    }

    fun clearText(charactersToErase: Int) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            handler = {
                val activeElement = it.switchTo().activeElement()
                val currentText = activeElement.text
                if (currentText.isNotEmpty()) {
                    val newText = currentText.dropLast(charactersToErase)
                    activeElement.clear()
                    activeElement.sendKeys(newText)
                }
            }
        )
    }

    fun scroll(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Duration = Duration.ofMillis(500)
    ) {
        return handleDriverCommand<AppiumDriver, Any, Unit>(
            driver = appiumDriver,
            handler = {
                val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
                val scroll = Sequence(pointerInput, 1)

                scroll.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
                scroll.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                scroll.addAction(pointerInput.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
                scroll.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                it.perform(listOf(scroll))
            }
        )
    }

    fun stopServer() {
        // Logic to stop the Appium server
        println("Stopping Appium server...")
        appiumService?.stop()
        appiumService = null
    }

    fun getWindowRect(): WebDriver.Window {
        return handleDriverCommand<AppiumDriver, Any, WebDriver.Window>(
            driver = appiumDriver,
            handler = { it.manage().window() }
        )
    }

    fun isAndroid(): Boolean {
        return handleDriverCommand<AppiumDriver, Any, Boolean> (
            driver = appiumDriver,
            handler = { it is AndroidDriver }
        )
    }

    private fun getCapabilitiesOptions(
        capabilities: Map<String, Any>,
    ): BaseOptions<out BaseOptions<*>> {
        val baseOptions = BaseOptions()

        capabilities.forEach {
            baseOptions.setCapability(it.key, it.value)
        }

        return baseOptions
    }
}