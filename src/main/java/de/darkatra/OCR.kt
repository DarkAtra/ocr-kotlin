package de.darkatra

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.util.LoadLibs
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyAdapter
import org.jnativehook.keyboard.NativeKeyEvent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.system.exitProcess


class SelectionPane : JPanel() {

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val g2d = g.create() as Graphics2D
		val dash1 = floatArrayOf(10.0f)
		val dashed = BasicStroke(3.0f,
			BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER,
			10.0f, dash1, 0.0f)
		g2d.color = Color.BLACK
		g2d.stroke = dashed
		g2d.drawRect(0, 0, width - 3, height - 3)
		g2d.dispose()
	}

	init {
		isOpaque = false
	}
}

class SnipItPane(
	val listener: Consumer<Rectangle>
) : JPanel() {

	private var mouseAnchor: Point? = null
	private var dragPoint: Point? = null
	private val selectionPane: SelectionPane

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val g2d = g.create() as Graphics2D
		val bounds = Rectangle(0, 0, width, height)
		val area = Area(bounds)
		area.subtract(Area(selectionPane.bounds))
		g2d.color = Color(192, 192, 192, 64)
		g2d.fill(area)
	}

	init {
		isOpaque = false
		layout = null
		selectionPane = SelectionPane()
		add(selectionPane)
		val adapter: MouseAdapter = object : MouseAdapter() {
			override fun mousePressed(e: MouseEvent) {
				mouseAnchor = e.point
				dragPoint = null
				selectionPane.location = mouseAnchor!!
				selectionPane.setSize(0, 0)
			}

			override fun mouseDragged(e: MouseEvent) {
				dragPoint = e.point
				var width: Int = dragPoint!!.x - mouseAnchor!!.x
				var height: Int = dragPoint!!.y - mouseAnchor!!.y
				var x: Int = mouseAnchor!!.x
				var y: Int = mouseAnchor!!.y
				if (width < 0) {
					x = dragPoint!!.x
					width *= -1
				}
				if (height < 0) {
					y = dragPoint!!.y
					height *= -1
				}
				selectionPane.setBounds(x, y, width, height)
				selectionPane.revalidate()
				repaint()
			}

			override fun mouseReleased(e: MouseEvent?) {
				val snipArea = Rectangle(selectionPane.x, selectionPane.y, selectionPane.width, selectionPane.height)
				SwingUtilities.getWindowAncestor(selectionPane).dispose()
				if (snipArea.width + snipArea.height > 0) {
					listener.accept(snipArea)
				}
			}
		}
		addMouseListener(adapter)
		addMouseMotionListener(adapter)
	}
}

class OCR {
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

fun main() {
	OCR()
}
