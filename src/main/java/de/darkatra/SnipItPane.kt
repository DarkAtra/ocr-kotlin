package de.darkatra

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.util.function.Consumer
import javax.swing.JPanel
import javax.swing.SwingUtilities

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
