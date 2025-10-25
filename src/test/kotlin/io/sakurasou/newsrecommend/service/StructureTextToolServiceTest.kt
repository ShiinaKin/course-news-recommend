package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dto.StructuredArticleDTO
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

@SpringBootTest
class StructureTextToolServiceTest {

    @Autowired
    private lateinit var structureTextService: StructureTextService

    @Test
    fun `structure text service returns structured article`() {
        val apiKey = System.getenv("OPENAI_API_KEY")
        Assumptions.assumeTrue(!apiKey.isNullOrBlank(), "Skipping test because no OPENAI_API_KEY is configured")

        val transcriptPath = Path.of("src/test/resources/structure_text/sample_transcript.txt")
        Assumptions.assumeTrue(Files.exists(transcriptPath), "Sample transcript missing at $transcriptPath")

        val result: StructuredArticleDTO = structureTextService.structureFromFile(transcriptPath)

        println(
            buildString {
                appendLine("=== StructureTextService result ===")
                appendLine(result.toString())
                appendLine("=== End of result ===")
            },
        )

        assertTrue(result.title.isNotBlank(), "title should not be blank")
        assertTrue(result.content.isNotBlank(), "content should not be blank")
        result.author?.let { author ->
            assertTrue(author.isNotBlank(), "author should not be blank when provided")
        }
        result.publishTime?.let { publishTime ->
            assertTrue(publishTime.isNotBlank(), "publishTime should not be blank when provided")
        }
    }
}