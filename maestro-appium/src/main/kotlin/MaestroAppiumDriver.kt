package com.getvymo.appium

import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.remote.options.BaseOptions
import io.appium.java_client.service.local.AppiumDriverLocalService
import io.appium.java_client.service.local.AppiumServerHasNotBeenStartedLocallyException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.w3c.dom.Document
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
        appiumService = AppiumDriverLocalService.buildDefaultService()

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

    fun launchApp(appId: String) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        when (appiumDriver) {
            is AndroidDriver -> {
                val androidDriver = appiumDriver as AndroidDriver
                androidDriver.activateApp(appId)
            }

            is IOSDriver -> {
                val iosDriver = appiumDriver as IOSDriver
                iosDriver.activateApp(appId)
            }

            else -> {
                throw IllegalStateException("Unsupported driver type: ${appiumDriver?.javaClass?.name}")
            }
        }
    }

    fun terminateApp(appId: String) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        when (appiumDriver) {
            is AndroidDriver -> {
                val androidDriver = appiumDriver as AndroidDriver
                androidDriver.terminateApp(appId)
            }

            is IOSDriver -> {
                val iosDriver = appiumDriver as IOSDriver
                iosDriver.terminateApp(appId)
            }

            else -> {
                throw IllegalStateException("Unsupported driver type: ${appiumDriver?.javaClass?.name}")
            }
        }
    }

    fun getDriver(): AppiumDriver {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }
        return appiumDriver!!
    }

    fun createDriver(
        capabilities: Map<String, Any>,
    ): AppiumDriver {
        if (appiumService == null || !appiumService!!.isRunning) {
            throw IllegalStateException("Appium server is not running. Please start the server first.")
        }

        val options = getCapabilitiesOptions(capabilities)
        appiumDriver = AppiumDriver(appiumService!!.url, options)

        return appiumDriver!!
    }

    // TODO: need to convert this xml string into a proper
    fun getPageSource() : String? {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        return appiumDriver!!.pageSource
    }

    fun tap(
        x: Int,
        y: Int,
    ) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        // Logic to perform tap action
        println("Performing tap action...")
        // Example: appiumDriver!!.tap(1, 100, 200, 500)
        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val tap = Sequence(pointerInput, 1)

        tap.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
        tap.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        tap.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver!!.perform(listOf(tap))

    }

    fun longPress(
        x: Int,
        y: Int,
        duration: Duration = Duration.ofMillis(1000)
    ) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        // Logic to perform tap action
        println("Performing tap action...")
        // Example: appiumDriver!!.tap(1, 100, 200, 500)
        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val tap = Sequence(pointerInput, 1)

        tap.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
        tap.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        tap.addAction(Pause(pointerInput, duration))
        tap.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver!!.perform(listOf(tap))
    }

    fun getDocumentViewHierarchy(): Document {
        // Logic to get view hierarchy
        println("Retrieving view hierarchy...")
        val pageSource = getPageSource()
        if (pageSource.isNullOrEmpty()) {
            throw IllegalStateException("Page source is empty. Ensure the app is running and the driver is connected.")
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(pageSource.byteInputStream())

        return document
    }

    fun inputText(text: String) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        // Logic to input text
        println("Inputting text: $text")
        val activeElement = appiumDriver!!.switchTo().activeElement()
        activeElement.sendKeys(text)
    }

    fun scroll(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Duration = Duration.ofMillis(500)
    ) {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        // Logic to perform scroll action
        println("Performing scroll action...")
        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val scroll = Sequence(pointerInput, 1)

        scroll.addAction(pointerInput.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
        scroll.addAction(pointerInput.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        scroll.addAction(pointerInput.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
        scroll.addAction(pointerInput.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver!!.perform(listOf(scroll))
    }

    fun stopServer() {
        // Logic to stop the Appium server
        println("Stopping Appium server...")
        appiumService?.stop()
    }

    fun getWindowRect(): WebDriver.Window {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        // Logic to get window rectangle
        println("Retrieving window rectangle...")
        val windowRect = appiumDriver!!.manage().window()
        return windowRect
    }

    fun isAndroid(): Boolean {
        if (appiumDriver == null) {
            throw IllegalStateException("Appium driver is not initialized. Please create the driver first.")
        }

        return appiumDriver is AndroidDriver
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