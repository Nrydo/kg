import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.abs
import kotlin.math.roundToInt

class ColorConverter {
    fun rgbToCmyk(r: Int, g: Int, b: Int): List<Double> {
        if (listOf(r, g, b).all { it == 0 }) {
            return listOf(0.0, 0.0, 0.0, 1.0)
        }
        val rNorm = r / 255.0
        val gNorm = g / 255.0
        val bNorm = b / 255.0

        val k = 1 - maxOf(rNorm, gNorm, bNorm)
        val c = (1 - rNorm - k) / (1 - k)
        val m = (1 - gNorm - k) / (1 - k)
        val y = (1 - bNorm - k) / (1 - k)

        return listOf(c, m, y, k)
    }

    fun cmykToRgb(c: Double, m: Double, y: Double, k: Double): List<Int> {
        val r = (255 * (1 - c) * (1 - k)).roundToInt()
        val g = (255 * (1 - m) * (1 - k)).roundToInt()
        val b = (255 * (1 - y) * (1 - k)).roundToInt()

        return listOf(r, g, b)
    }

    fun rgbToHls(r: Int, g: Int, b: Int): List<Double> {
        val rNorm = r / 255.0
        val gNorm = g / 255.0
        val bNorm = b / 255.0

        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        if (max == min) {
            return listOf(0.0, 0.0, max)
        }
        val delta = max - min

        val l = (max + min) / 2
        val s = delta / (1 - abs(2 * l - 1))
        var h = when (max) {
            rNorm -> (gNorm - bNorm) / delta
            gNorm -> ((bNorm - rNorm) / delta) + 2
            else -> ((rNorm - gNorm) / delta) + 4
        }
        h = 60 * ((h + 6) % 6)

        return listOf(h, l, s)
    }

    fun hlsToRgb(h: Double, l: Double, s: Double): List<Int> {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2

        val (r1, g1, b1) = when {
            h < 60 -> listOf(c, x, 0.0)
            h < 120 -> listOf(x, c, 0.0)
            h < 180 -> listOf(0.0, c, x)
            h < 240 -> listOf(0.0, x, c)
            h < 300 -> listOf(x, 0.0, c)
            else -> listOf(c, 0.0, x)
        }

        val r = ((r1 + m) * 255).roundToInt()
        val g = ((g1 + m) * 255).roundToInt()
        val b = ((b1 + m) * 255).roundToInt()

        return listOf(r, g, b)
    }
}

enum class Changed {
    RGB,
    CMYK,
    HLS
}

class ColorConverterApp : JFrame("Color Converter") {
    private val colorConverter = ColorConverter()
    private var isUpdating = false

    private var rgbColor = listOf(0, 0, 0)
    private var cmykColor = listOf(0.0, 0.0, 0.0, 1.0)
    private var hlsColor = listOf(0.0, 0.0, 0.0)

    private val rgbFields = rgbColor.map { JTextField(5) }
    private val cmykFields = cmykColor.map { JTextField(5) }
    private val hlsFields = hlsColor.map { JTextField(5) }

    private val rgbSliders = List(3) { createSlider(0, 255) }
    private val cmykSliders = List(4) { createSlider(0, 100) }
    private val hlsSliders = listOf(createSlider(0, 360), createSlider(0, 100), createSlider(0, 100))

    private val colorDisplayPanel = JPanel().apply {
        preferredSize = Dimension(200, 200)
        background = Color.BLACK
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(10, 10, 10, 10)
        }

        gbc.gridx = 0
        gbc.gridy = 0
        add(createColorPanel("RGB:", rgbFields, rgbSliders), gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        add(createColorPanel("CMYK:", cmykFields, cmykSliders), gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        add(createColorPanel("HLS:", hlsFields, hlsSliders), gbc)

        gbc.gridx = 0
        gbc.gridy = 3
        add(colorDisplayPanel, gbc)

        val colorChooserButton = JButton("Open palette").apply {
            addActionListener { chooseColor() }
        }
        gbc.gridx = 0
        gbc.gridy = 4
        add(colorChooserButton, gbc)

        setupListeners()

        updateFields()
        updateSliders()
        updateColorDisplay()
        pack()
        isVisible = true
    }

    private fun createColorPanel(title: String, fields: List<JTextField>, sliders: List<JSlider>): JPanel {
        val panel = JPanel().apply {
            layout = GridLayout(1, 0)
            border = BorderFactory.createTitledBorder(title)
        }
        fields.forEach { panel.add(it) }
        sliders.forEach { panel.add(it) }
        return panel
    }

    private fun setupListeners() {
        rgbFields.forEach { addDocumentListener(it, Changed.RGB) }
        cmykFields.forEach { addDocumentListener(it, Changed.CMYK) }
        hlsFields.forEach { addDocumentListener(it, Changed.HLS) }

        rgbSliders.forEachIndexed { i, slider -> addSliderListener(slider, rgbFields[i], Changed.RGB) }
        cmykSliders.forEachIndexed { i, slider -> addSliderListener(slider, cmykFields[i], Changed.CMYK) }
        hlsSliders.forEachIndexed { i, slider -> addSliderListener(slider, hlsFields[i], Changed.HLS) }
    }

    private fun chooseColor() {
        val color = JColorChooser.showDialog(this, "Choose color", null)
        if (color != null) {
            rgbColor = listOf(color.red, color.green, color.blue)
            updateFromRgb()
            updateFields()
            updateSliders()
            updateColorDisplay()
        }
    }

    private fun addDocumentListener(field: JTextField, changed: Changed) {
        field.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateColors(changed)
            override fun removeUpdate(e: DocumentEvent?) = updateColors(changed)
            override fun changedUpdate(e: DocumentEvent?) = updateColors(changed)
        })
    }

    private fun addSliderListener(slider: JSlider, field: JTextField, changed: Changed) {
        slider.addChangeListener {
            if (!isUpdating) {
                field.text = slider.value.toString()
            }
        }
    }

    private fun createSlider(min: Int, max: Int): JSlider {
        return JSlider(min, max).apply {
            majorTickSpacing = (max - min) / 5
            paintTicks = true
            paintLabels = true
            preferredSize = Dimension(100, 50)
        }
    }

    private fun updateColors(changed: Changed) {
        if (isUpdating) return
        SwingUtilities.invokeLater {
            isUpdating = true;
            try {
                when (changed) {
                    Changed.RGB -> {
                        if (rgbFields.any { it.text.isEmpty() }) {
                            return@invokeLater
                        }
                        val (r, g, b) = rgbFields.map { it.text.toInt() }
                        rgbColor = listOf(r, g, b)
                        updateFromRgb()
                        updateCMYKField()
                        updateHLSField()
                    }

                    Changed.CMYK -> {
                        if (cmykFields.any { it.text.isEmpty() }) {
                            return@invokeLater
                        }
                        val (c, m, y, k) = cmykFields.map { it.text.toDouble() / 100 }
                        val (r, g, b) = colorConverter.cmykToRgb(c, m, y, k)
                        rgbColor = listOf(r, g, b)
                        cmykColor = listOf(c, m, y, k)
                        hlsColor = colorConverter.rgbToHls(r, g, b)
                        updateRGBField()
                        updateHLSField()
                    }

                    Changed.HLS -> {
                        if (hlsFields.any { it.text.isEmpty() }) {
                            return@invokeLater
                        }
                        var (h, l, s) = hlsFields.map { it.text.toDouble() }
                        l /= 100
                        s /= 100
                        val (r, g, b) = colorConverter.hlsToRgb(h, l, s)
                        rgbColor = listOf(r, g, b)
                        cmykColor = colorConverter.rgbToCmyk(r, g, b)
                        hlsColor = listOf(h, l, s)
                        updateRGBField()
                        updateCMYKField()
                    }
                }
                updateSliders()
                updateColorDisplay()
            } catch (e: NumberFormatException) {
                println(e.toString())
            } finally {
                isUpdating = false;
            }
        }
    }

    private fun updateRGBField() {
        updateField(rgbFields, rgbColor)
    }

    private fun updateCMYKField() {
        updateField(cmykFields, cmykColor.map { it * 100 })
    }

    private fun updateHLSField() {
        val (h, l, s) = hlsColor
        updateField(hlsFields, listOf(h, l * 100, s * 100))
    }

    private fun updateFields() {
        updateRGBField()
        updateCMYKField()
        updateHLSField()
    }

    private fun updateField(fields: List<JTextField>, values: List<Number>) {
        fields.forEachIndexed { i, field ->
            field.text = String.format(if (values[i] is Double) "%.2f" else "%d", values[i])
        }
    }

    private fun updateColorDisplay() {
        val (r, g, b) = rgbColor
        colorDisplayPanel.background = Color(r, g, b)
        colorDisplayPanel.repaint()
    }

    private fun updateSliders() {
        val (h, l, s) = hlsColor
        mapOf(
            rgbSliders to rgbColor,
            cmykSliders to cmykColor.map { it * 100 },
            hlsSliders to listOf(h, l * 100, s * 100)
        ).forEach { (sliders, values) ->
            sliders.forEachIndexed { i, slider ->
                slider.value = values[i].toInt()
            }
        }
    }

    private fun updateFromRgb() {
        val (r, g, b) = rgbColor
        cmykColor = colorConverter.rgbToCmyk(r, g, b)
        hlsColor = colorConverter.rgbToHls(r, g, b)
    }
}


fun main() {
    Locale.setDefault(Locale.ENGLISH)
    SwingUtilities.invokeLater {
        ColorConverterApp()
    }
}