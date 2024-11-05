import java.awt.*
import javax.swing.*
import kotlin.math.roundToInt

class RasterizationApp : JFrame() {
    private val canvas: JPanel
    private val x0Field: JTextField
    private val y0Field: JTextField
    private val x1Field: JTextField
    private val y1Field: JTextField
    private val cellSizeField: JTextField
    private val stepAlgoButton: JRadioButton
    private val ddaAlgoButton: JRadioButton
    private val bresenhamAlgoButton: JRadioButton
    private val bresenhamCircleAlgoButton: JRadioButton
    private val logArea: JTextArea
    private var centerX = 0
    private var centerY = 0
    private var cellSize = 20

    init {
        title = "Rasterization"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        canvas = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawGrid(g)
                drawAxis(g)
                rasterizeLine(g)
            }
        }
        canvas.preferredSize = Dimension(400, 400)
        add(canvas, BorderLayout.CENTER)

        val controlPanel = JPanel().apply { layout = GridLayout(11, 2) }
        add(controlPanel, BorderLayout.EAST)

        val algoGroup = ButtonGroup()
        stepAlgoButton = JRadioButton("Step-by-Step Algorithm", true)
        ddaAlgoButton = JRadioButton("DDA Algorithm")
        bresenhamAlgoButton = JRadioButton("Bresenham Algorithm")
        bresenhamCircleAlgoButton = JRadioButton("Bresenham (Circle)")

        algoGroup.add(stepAlgoButton)
        algoGroup.add(ddaAlgoButton)
        algoGroup.add(bresenhamAlgoButton)
        algoGroup.add(bresenhamCircleAlgoButton)

        controlPanel.add(stepAlgoButton)
        controlPanel.add(ddaAlgoButton)
        controlPanel.add(bresenhamAlgoButton)
        controlPanel.add(bresenhamCircleAlgoButton)

        controlPanel.add(JLabel("x0:"))
        x0Field = JTextField("0")
        controlPanel.add(x0Field)

        controlPanel.add(JLabel("y0:"))
        y0Field = JTextField("0")
        controlPanel.add(y0Field)

        controlPanel.add(JLabel("x1:"))
        x1Field = JTextField("5")
        controlPanel.add(x1Field)

        controlPanel.add(JLabel("y1:"))
        y1Field = JTextField("4")
        controlPanel.add(y1Field)

        controlPanel.add(JLabel("Scale:"))
        cellSizeField = JTextField(cellSize.toString())
        controlPanel.add(cellSizeField)

        logArea = JTextArea().apply { isEditable = false }
        val scrollPane = JScrollPane(logArea).apply { preferredSize = Dimension(600, 100) }
        add(scrollPane, BorderLayout.SOUTH)

        val drawButton = JButton("Draw").apply {
            addActionListener {
                try {
                    cellSize = cellSizeField.text.toInt()
                    canvas.repaint()
                } catch (ex: NumberFormatException) {
                    logArea.text = "Invalid cell size!"
                }
            }
        }
        controlPanel.add(drawButton)
    }

    private fun drawGrid(g: Graphics) {
        g.color = Color.LIGHT_GRAY
        for (i in 0 until canvas.width step cellSize) {
            g.drawLine(i, 0, i, canvas.height)
        }
        for (i in 0 until canvas.height step cellSize) {
            g.drawLine(0, i, canvas.width, i)
        }
    }

    private fun drawAxis(g: Graphics) {
        g.color = Color.BLACK
        val width = canvas.width
        val height = canvas.height
        centerX = (width / 2 / cellSize) * cellSize
        centerY = (height / 2 / cellSize) * cellSize
        g.drawLine(centerX, 0, centerX, height)
        g.drawLine(0, centerY, width, centerY)
        g.drawString("X", width - 20, centerY - 10)
        g.drawString("Y", centerX + 10, 20)

        for (i in 0..width / 2 - 2 * cellSize step cellSize) {
            g.drawLine(centerX + i, centerY - 5, centerX + i, centerY + 5)
            if (i != 0) g.drawString((i / cellSize).toString(), centerX + i - 5, centerY + 20)
            g.drawLine(centerX - i, centerY - 5, centerX - i, centerY + 5)
            if (i != 0) g.drawString((-i / cellSize).toString(), centerX - i - 10, centerY + 20)
        }

        for (i in 0..height / 2 - 2 * cellSize step cellSize) {
            g.drawLine(centerX - 5, centerY - i, centerX + 5, centerY - i)
            if (i != 0) g.drawString((i / cellSize).toString(), centerX + 10, centerY - i + 5)
            g.drawLine(centerX - 5, centerY + i, centerX + 5, centerY + i)
            if (i != 0) g.drawString((-i / cellSize).toString(), centerX + 10, centerY + i + 5)
        }
    }

    private fun rasterizeLine(g: Graphics) {
        val x0 = x0Field.text.toInt()
        val y0 = y0Field.text.toInt()
        val x1 = x1Field.text.toInt()
        val y1 = y1Field.text.toInt()

        val startTime = System.nanoTime()
        when {
            stepAlgoButton.isSelected -> stepByStepAlgorithm(g, x0, y0, x1, y1)
            ddaAlgoButton.isSelected -> ddaAlgorithm(g, x0, y0, x1, y1)
            bresenhamAlgoButton.isSelected -> bresenhamAlgorithm(g, x0, y0, x1, y1)
            bresenhamCircleAlgoButton.isSelected -> {
                val r = kotlin.math.sqrt(((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)).toDouble()).toInt()
                bresenhamCircleAlgorithm(g, x0, y0, r)
            }
        }
        val endTime = System.nanoTime()
        logArea.append("Execution time: ${endTime - startTime} ns\n")
    }

    private fun stepByStepAlgorithm(g: Graphics, x0: Int, y0: Int, x1: Int, y1: Int) {
        var x0Var = x0
        var y0Var = y0
        var x1Var = x1
        var y1Var = y1

        if (x0Var > x1Var) x0Var = x1.also { x1Var = x0 }
        if (y0Var > y1Var) y0Var = y1.also { y1Var = y0 }

        logArea.append("Step-by-Step Algorithm:\n")
        g.color = Color.BLACK
        val dx = x1Var - x0Var
        val dy = y1Var - y0Var

        if (dx > dy) {
            val slope = dy.toDouble() / dx
            for (x in x0Var..x1Var) {
                val y = y0Var + slope * (x - x0Var)
                val yRounded = kotlin.math.floor(y).toInt()
                logArea.append("Drawing point ($x, $yRounded)\n")
                g.fillRect(x * cellSize + centerX, -yRounded * cellSize + centerY - cellSize, cellSize, cellSize)
            }
        } else {
            val slope = dx.toDouble() / dy
            for (y in y0Var..y1Var) {
                val x = x0Var + slope * (y - y0Var)
                val xRounded = kotlin.math.floor(x).toInt()
                logArea.append("Drawing point ($xRounded, $y)\n")
                g.fillRect(xRounded * cellSize + centerX, -y * cellSize + centerY - cellSize, cellSize, cellSize)
            }
        }
    }

    private fun ddaAlgorithm(g: Graphics, x0: Int, y0: Int, x1: Int, y1: Int) {
        logArea.append("DDA Algorithm:\n")
        g.color = Color.BLACK
        val dx = x1 - x0
        val dy = y1 - y0
        val steps = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
        val xInc = dx.toDouble() / steps
        val yInc = dy.toDouble() / steps
        var x = x0.toDouble()
        var y = y0.toDouble()

        for (i in 0..steps) {
            logArea.append("Drawing point (${x.roundToInt()}, ${y.roundToInt()})\n")
            g.fillRect(x.roundToInt() * cellSize + centerX, -y.roundToInt() * cellSize + centerY - cellSize, cellSize, cellSize)
            x += xInc
            y += yInc
        }
    }

    private fun bresenhamAlgorithm(g: Graphics, x0: Int, y0: Int, x1: Int, y1: Int) {
        logArea.append("Bresenham Algorithm:\n")
        g.color = Color.BLACK
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            logArea.append("Drawing point ($x, $y)\n")
            g.fillRect(x * cellSize + centerX, -y * cellSize + centerY - cellSize, cellSize, cellSize)
            if (x == x1 && y == y1) break
            val e2 = err * 2
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun bresenhamCircleAlgorithm(g: Graphics, x0: Int, y0: Int, radius: Int) {
        logArea.append("Bresenham Circle Algorithm:\n")
        g.color = Color.BLACK
        var x = 0
        var y = radius
        var d = 3 - 2 * radius

        drawCirclePoints(g, x0, y0, x, y)

        while (y >= x) {
            x++
            if (d > 0) {
                y--
                d += 4 * (x - y) + 10
            } else {
                d += 4 * x + 6
            }
            drawCirclePoints(g, x0, y0, x, y)
        }
    }

    private fun drawCirclePoints(g: Graphics, xc: Int, yc: Int, x: Int, y: Int) {
        val points = listOf(
            Pair(xc + x, yc + y), Pair(xc - x, yc + y),
            Pair(xc + x, yc - y), Pair(xc - x, yc - y),
            Pair(xc + y, yc + x), Pair(xc - y, yc + x),
            Pair(xc + y, yc - x), Pair(xc - y, yc - x)
        )
        for ((px, py) in points) {
            logArea.append("Drawing point ($px, $py)\n")
            g.fillRect(px * cellSize + centerX, -py * cellSize + centerY - cellSize, cellSize, cellSize)
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        RasterizationApp().isVisible = true
    }
}
