package de.darkatra

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.util.LoadLibs
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyAdapter
import org.jnativehook.keyboard.NativeKeyEvent
import java.awt.Color
import java.awt.Image
import java.awt.Rectangle
import java.awt.Robot
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.StringSelection
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.system.exitProcess

class OCR {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			OCR()
		}
	}

	init {

		if (!SystemTray.isSupported()) {
			println("SystemTray is not supported.")
			exitProcess(1)
		}

		val logger: Logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
		logger.level = Level.WARNING
		logger.useParentHandlers = false

		try {
			GlobalScreen.registerNativeHook()
		} catch (ex: NativeHookException) {
			println("There was a problem registering the native hook.")
			println(ex.message)
			exitProcess(2)
		}

		val clipboard = Toolkit.getDefaultToolkit().systemClipboard
		val tesseract = Tesseract().also { tesseract ->
			tesseract.setDatapath(LoadLibs.extractTessResources("tessdata").absolutePath)
		}

		GlobalScreen.addNativeKeyListener(object : NativeKeyAdapter() {
			override fun nativeKeyPressed(keyEvent: NativeKeyEvent) {
				if (keyEvent.keyCode == NativeKeyEvent.VC_F12) {
					val frame = JFrame()
					frame.isAlwaysOnTop = true
					frame.isUndecorated = true
					frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
					frame.bounds = Rectangle(Toolkit.getDefaultToolkit().screenSize)
					frame.background = Color(0, 0, 0, 1)

					frame.add(SnipItPane { rectangle ->
						val image = Robot().createScreenCapture(rectangle)
						val text = tesseract.doOCR(image)
						clipboard.setContents(StringSelection(text), null)
					})

					frame.isVisible = true
				}
			}
		})

		val tracIcon = TrayIcon(ImageIO.read(javaClass.getResource("/icon_radius_small.png")).getScaledInstance(16, 16, Image.SCALE_DEFAULT))
		tracIcon.addActionListener {
			GlobalScreen.unregisterNativeHook()
			exitProcess(0)
		}

		val tray = SystemTray.getSystemTray()
		tray.add(tracIcon)
	}
}
