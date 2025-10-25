package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.config.AppProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["app.stt.mode=real"])
class STTServiceTest {

    @Autowired
    private lateinit var sttService: STTService

    @Autowired
    private lateinit var appProperties: AppProperties

    @Test
    fun `real mode executes whisper-cli and returns non-empty transcript`() {
        val modelPath = Path.of(appProperties.stt.modelPath)
        assertTrue(Files.exists(modelPath), "Configured Whisper model not found at $modelPath")

        val audioPath = Path.of(
            requireNotNull(javaClass.getResource("/stt/test.wav")).toURI(),
        )

        val transcript = sttService.transcribe(jobId = 1001L, filePath = audioPath)

        println(
            buildString {
                appendLine("=== Whisper transcript artifact ===")
                appendLine(transcript)
                appendLine("=== End of artifact ===")
            },
        )

        assertTrue(transcript.isNotBlank(), "Transcript should not be blank in real mode")
        assertFalse(transcript.contains("模拟转写"), "Real mode should not return mock transcript")
    }
}
