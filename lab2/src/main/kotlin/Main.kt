import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.exp

// Главный класс приложения, наследующий JFrame
class ImageProcessingApp : JFrame("Image Processing Application") {
    private val originalPanel = ImagePanel() // Панель для отображения оригинального изображения
    private val processedPanel = ImagePanel() // Панель для отображения обработанного изображения
    private var originalImage: BufferedImage? = null // Оригинальное изображение

    init {
        setupUI() // Инициализация пользовательского интерфейса
    }

    // Настройка пользовательского интерфейса
    private fun setupUI() {
        layout = BorderLayout() // Установка компоновки

        // Создание разделенного окна для отображения оригинального и обработанного изображения
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalPanel, processedPanel)
        splitPane.resizeWeight = 0.5
        add(splitPane, BorderLayout.CENTER)

        // Панель с кнопками для управления обработкой изображений
        val buttonPanel = JPanel(GridLayout(1, 5))
        val loadButton = JButton("Load Image") // Кнопка загрузки изображения
        val gaussianButton = JButton("Gaussian Blur") // Кнопка размытия по Гауссу
        val medianButton = JButton("Median Filter") // Кнопка медианного фильтра
        val erosionButton = JButton("Erosion") // Кнопка эрозии
        val dilationButton = JButton("Dilation") // Кнопка дилатации

        // Добавление кнопок на панель
        buttonPanel.add(loadButton)
        buttonPanel.add(gaussianButton)
        buttonPanel.add(medianButton)
        buttonPanel.add(erosionButton)
        buttonPanel.add(dilationButton)

        add(buttonPanel, BorderLayout.SOUTH) // Добавление панели кнопок в нижнюю часть окна

        // Настройка действий кнопок
        loadButton.addActionListener { loadImage() }
        gaussianButton.addActionListener { applyGaussianBlur() }
        medianButton.addActionListener { applyMedianFilter() }
        erosionButton.addActionListener { applyErosion() }
        dilationButton.addActionListener { applyDilation() }

        defaultCloseOperation = EXIT_ON_CLOSE // Завершение работы приложения при закрытии окна
        setSize(1000, 600) // Установка размера окна
        isVisible = true // Отображение окна
    }

    // Загрузка изображения
    private fun loadImage() {
        val fileChooser = JFileChooser() // Создание диалогового окна выбора файла
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            originalImage = ImageIO.read(file) // Чтение изображения из файла
            originalPanel.setImage(originalImage) // Установка оригинального изображения
            processedPanel.setImage(originalImage) // Изначально устанавливаем обработанное изображение таким же
        }
    }

    // Применение фильтров и морфологических операций
    private fun applyGaussianBlur() {
        originalImage?.let { image ->
            val blurredImage = gaussianBlur(image, 5) // Применение размытия по Гауссу
            processedPanel.setImage(blurredImage) // Отображение обработанного изображения
        }
    }

    private fun applyMedianFilter() {
        originalImage?.let { image ->
            val filteredImage = medianFilter(image, 3) // Применение медианного фильтра
            processedPanel.setImage(filteredImage) // Отображение обработанного изображения
        }
    }

    private fun applyErosion() {
        originalImage?.let { image ->
            val erodedImage = morphologicalErosion(image, 1) // Применение эрозии
            processedPanel.setImage(erodedImage) // Отображение обработанного изображения
        }
    }

    private fun applyDilation() {
        originalImage?.let { image ->
            val dilatedImage = morphologicalDilation(image, 1) // Применение дилатации
            processedPanel.setImage(dilatedImage) // Отображение обработанного изображения
        }
    }

    // Размытие по Гауссу
    private fun gaussianBlur(image: BufferedImage, radius: Int): BufferedImage {
        val kernel = generateGaussianKernel(radius) // Генерация ядра Гаусса
        val output = BufferedImage(image.width, image.height, image.type) // Создание выходного изображения
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val pixel = image.getRGB(x + kx, y + ky) // Получение цвета пикселя
                        val weight = kernel[ky + radius][kx + radius] // Получение веса из ядра
                        rSum += ((pixel shr 16) and 0xFF) * weight // Суммирование красного канала
                        gSum += ((pixel shr 8) and 0xFF) * weight // Суммирование зеленого канала
                        bSum += (pixel and 0xFF) * weight // Суммирование синего канала
                    }
                }
                // Установка нового цвета пикселя
                val r = rSum.toInt().coerceIn(0, 255)
                val g = gSum.toInt().coerceIn(0, 255)
                val b = bSum.toInt().coerceIn(0, 255)
                output.setRGB(x, y, Color(r, g, b).rgb)
            }
        }
        return output // Возврат обработанного изображения
    }

    // Генерация ядра Гаусса
    private fun generateGaussianKernel(radius: Int): Array<DoubleArray> {
        val size = 2 * radius + 1
        val kernel = Array(size) { DoubleArray(size) }
        val sigma = radius / 3.0
        var sum = 0.0
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val value = (1 / (2 * Math.PI * sigma * sigma)) * exp(-(x * x + y * y) / (2 * sigma * sigma))
                kernel[y + radius][x + radius] = value // Заполнение ядра
                sum += value // Сумма всех значений для нормализации
            }
        }
        // Нормализация ядра
        for (y in 0..<size) {
            for (x in 0..<size) {
                kernel[y][x] /= sum
            }
        }
        return kernel // Возврат нормализованного ядра
    }

    // Медианный фильтр
    private fun medianFilter(image: BufferedImage, radius: Int): BufferedImage {
        val output = BufferedImage(image.width, image.height, image.type)
        for (y in radius..<image.height - radius) {
            for (x in radius..<image.width - radius) {
                val neighbors = mutableListOf<Int>() // Список для хранения соседних пикселей
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        neighbors.add(image.getRGB(x + kx, y + ky)) // Добавление цвета пикселя
                    }
                }
                neighbors.sort() // Сортировка цветов
                output.setRGB(x, y, neighbors[neighbors.size / 2]) // Установка медианного значения
            }
        }
        return output // Возврат обработанного изображения
    }

    // Эрозия
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
                        minR = min(minR, r) // Находим минимальные значения для каждого канала
                        minG = min(minG, g)
                        minB = min(minB, b)
                    }
                }
                output.setRGB(x, y, Color(minR, minG, minB).rgb) // Установка минимального цвета пикселя
            }
        }
        return output // Возврат обработанного изображения
    }

    // Дилатация
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
                        maxR = max(maxR, r) // Находим максимальные значения для каждого канала
                        maxG = max(maxG, g)
                        maxB = max(maxB, b)
                    }
                }
                output.setRGB(x, y, Color(maxR, maxG, maxB).rgb) // Установка максимального цвета пикселя
            }
        }
        return output // Возврат обработанного изображения
    }
}

// Класс для панели, отображающей изображение
class ImagePanel : JPanel() {
    private var image: BufferedImage? = null // Изображение для отображения

    // Метод для установки изображения
    fun setImage(img: BufferedImage?) {
        image = img
        repaint() // Перерисовка панели
    }

    // Метод для рисования изображения на панели
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        image?.let {
            val imgWidth = it.width
            val imgHeight = it.height

            val aspectRatio = imgWidth.toDouble() / imgHeight // Соотношение сторон

            // Вычисление размеров для отображения
            var drawWidth = width
            var drawHeight = (width / aspectRatio).toInt()

            if (drawHeight > height) {
                drawHeight = height
                drawWidth = (height * aspectRatio).toInt()
            }

            val x = (width - drawWidth) / 2 // Центрирование изображения по горизонтали
            val y = (height - drawHeight) / 2 // Центрирование изображения по вертикали

            g.drawImage(it, x, y, drawWidth, drawHeight, this) // Отрисовка изображения
        }
    }
}

// Главная функция, запускающая приложение
fun main() {
    SwingUtilities.invokeLater {
        ImageProcessingApp() // Создание и отображение приложения
    }
}