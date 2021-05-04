package de.darkatra

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

class SelectionPane : JPanel() {

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val g2d = g.create() as Graphics2D
		val dashed = BasicStroke(
			1.0f,
			BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER,
			5.0f,
			floatArrayOf(5.0f),
			0.0f
		)
		g2d.color = Color.WHITE
		g2d.stroke = dashed
		g2d.drawRect(0, 0, width - 1, height - 1)
		g2d.dispose()
	}

	init {
		isOpaque = false
	}
}
