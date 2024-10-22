import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.exp

class ImageProcessingApp : JFrame("Image Processing Application") {
    private val originalPanel = ImagePanel()
    private val processedPanel = ImagePanel()
    private var originalImage: BufferedImage? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalPanel, processedPanel)
        splitPane.resizeWeight = 0.5
        add(splitPane, BorderLayout.CENTER)

        val buttonPanel = JPanel(GridLayout(1, 5))
        val loadButton = JButton("Load Image")
        val gaussianButton = JButton("Gaussian Blur")
        val medianButton = JButton("Median Filter")
        val erosionButton = JButton("Erosion")
        val dilationButton = JButton("Dilation")

        buttonPanel.add(loadButton)
        buttonPanel.add(gaussianButton)
        buttonPanel.add(medianButton)
        buttonPanel.add(erosionButton)
        buttonPanel.add(dilationButton)

        add(buttonPanel, BorderLayout.SOUTH)

        loadButton.addActionListener { loadImage() }
        gaussianButton.addActionListener { applyGaussianBlur() }
        medianButton.addActionListener { applyMedianFilter() }
        erosionButton.addActionListener { applyErosion() }
        dilationButton.addActionListener { applyDilation() }

        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1000, 600)
        isVisible = true
    }

    private fun loadImage() {
        val fileChooser = JFileChooser()
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            originalImage = ImageIO.read(file)
            originalPanel.setImage(originalImage)
            processedPanel.setImage(originalImage) // Initially set the same image for processed
        }
    }

    private fun applyGaussianBlur() {
        originalImage?.let { image ->
            val blurredImage = gaussianBlur(image, 5)
            processedPanel.setImage(blurredImage)
        }
    }

    private fun applyMedianFilter() {
        originalImage?.let { image ->
            val filteredImage = medianFilter(image, 3)
            processedPanel.setImage(filteredImage)
        }
    }

    private fun applyErosion() {
        originalImage?.let { image ->
            val erodedImage = morphologicalErosion(image, 1)
            processedPanel.setImage(erodedImage)
        }
    }

    private fun applyDilation() {
        originalImage?.let { image ->
            val dilatedImage = morphologicalDilation(image, 1)
            processedPanel.setImage(dilatedImage)
        }
    }

    private fun gaussianBlur(image: BufferedImage, radius: Int): BufferedImage {
        val kernel = generateGaussianKernel(radius)
        val output = BufferedImage(image.width, image.height, image.type)
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val pixel = image.getRGB(x + kx, y + ky)
                        val weight = kernel[ky + radius][kx + radius]
                        rSum += ((pixel shr 16) and 0xFF) * weight
                        gSum += ((pixel shr 8) and 0xFF) * weight
                        bSum += (pixel and 0xFF) * weight
                    }
                }
                val r = rSum.toInt().coerceIn(0, 255)
                val g = gSum.toInt().coerceIn(0, 255)
                val b = bSum.toInt().coerceIn(0, 255)
                output.setRGB(x, y, Color(r, g, b).rgb)
            }
        }
        return output
    }

    private fun generateGaussianKernel(radius: Int): Array<DoubleArray> {
        val size = 2 * radius + 1
        val kernel = Array(size) { DoubleArray(size) }
        val sigma = radius / 3.0
        var sum = 0.0
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val value = (1 / (2 * Math.PI * sigma * sigma)) * exp(-(x * x + y * y) / (2 * sigma * sigma))
                kernel[y + radius][x + radius] = value
                sum += value
            }
        }
        for (y in 0..<size) {
            for (x in 0..<size) {
                kernel[y][x] /= sum
            }
        }
        return kernel
    }

    private fun medianFilter(image: BufferedImage, radius: Int): BufferedImage {
        val output = BufferedImage(image.width, image.height, image.type)
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                val neighbors = mutableListOf<Int>()
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        neighbors.add(image.getRGB(x + kx, y + ky))
                    }
                }
                neighbors.sort()
                output.setRGB(x, y, neighbors[neighbors.size / 2])
            }
        }
        return output
    }

    private fun morphologicalErosion(image: BufferedImage, radius: Int): BufferedImage {
        val output = BufferedImage(image.width, image.height, image.type)
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                var minR = 255
                var minG = 255
                var minB = 255
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val pixel = image.getRGB(x + kx, y + ky)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        minR = min(minR, r)
                        minG = min(minG, g)
                        minB = min(minB, b)
                    }
                }
                output.setRGB(x, y, Color(minR, minG, minB).rgb)
            }
        }
        return output
    }

    private fun morphologicalDilation(image: BufferedImage, radius: Int): BufferedImage {
        val output = BufferedImage(image.width, image.height, image.type)
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                var maxR = 0
                var maxG = 0
                var maxB = 0
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val pixel = image.getRGB(x + kx, y + ky)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        maxR = max(maxR, r)
                        maxG = max(maxG, g)
                        maxB = max(maxB, b)
                    }
                }
                output.setRGB(x, y, Color(maxR, maxG, maxB).rgb)
            }
        }
        return output
    }
}

class ImagePanel : JPanel() {
    private var image: BufferedImage? = null

    fun setImage(img: BufferedImage?) {
        image = img
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        image?.let {
            val imgWidth = it.width
            val imgHeight = it.height

            val aspectRatio = imgWidth.toDouble() / imgHeight

            var drawWidth = width
            var drawHeight = (width / aspectRatio).toInt()

            if (drawHeight > height) {
                drawHeight = height
                drawWidth = (height * aspectRatio).toInt()
            }

            val x = (width - drawWidth) / 2
            val y = (height - drawHeight) / 2

            g.drawImage(it, x, y, drawWidth, drawHeight, this)
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        ImageProcessingApp()
    }
}
