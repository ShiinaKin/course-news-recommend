package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.config.AppProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class OCRServiceTest {

    @Autowired
    private lateinit var ocrService: OCRService

    @Autowired
    private lateinit var appProperties: AppProperties

    @Test
    fun `vision OCR recognizes text from fixture image`() {
        val binaryPath = Path.of(appProperties.ocr.binPath)
        assertTrue(Files.exists(binaryPath), "Configured OCR binary not found at $binaryPath")
        assertTrue(Files.isExecutable(binaryPath), "Configured OCR binary is not executable: $binaryPath")

        val imagePath = Path.of(
            requireNotNull(javaClass.getResource("/ocr/test.png")).toURI(),
        )

        val result = ocrService.recognize(imagePath)

        println(
            buildString {
                appendLine("=== OCR transcript artifact ===")
                appendLine(result)
                appendLine("=== End of artifact ===")
            },
        )

        assertTrue(result.isNotBlank(), "OCR output should not be blank")
        assertFalse(result.contains("示例第一行"), "OCR output should originate from actual CLI, not mock data")
    }
}
