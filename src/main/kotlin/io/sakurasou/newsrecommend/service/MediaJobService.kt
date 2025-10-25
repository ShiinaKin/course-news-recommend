package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.MediaJobDAO
import io.sakurasou.newsrecommend.dto.StructuredArticleDTO
import io.sakurasou.newsrecommend.model.MediaJob
import io.sakurasou.newsrecommend.model.MediaJobStatus
import io.sakurasou.newsrecommend.model.MediaJobType
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

@Service
class MediaJobService(
    private val mediaJobDao: MediaJobDAO,
    private val sttService: STTService,
    private val ocrService: OCRService,
    private val articleService: ArticleService,
    private val structureTextService: StructureTextService,
    private val taskExecutor: TaskExecutor,
) {

    private val logger = LoggerFactory.getLogger(MediaJobService::class.java)
    private val metrics = MediaJobMetrics()
    private val processing = ConcurrentHashMap<Long, Boolean>()

    @Transactional
    fun submitJob(userId: Long, type: MediaJobType, filePath: Path): MediaJob {
        val job = MediaJob(
            userId = userId,
            type = type,
            filePath = filePath.toAbsolutePath().toString(),
            status = MediaJobStatus.PENDING,
        )
        mediaJobDao.insert(job)
        val jobId = job.id ?: throw IllegalStateException("媒体任务创建失败")
        logger.info(
            "Submit media job id={} type={} size={} bytes",
            jobId,
            type,
            Files.size(filePath),
        )
        dispatch(jobId, type)
        return mediaJobDao.findById(jobId) ?: job.copy(id = jobId)
    }

    fun findJob(jobId: Long): MediaJob? = mediaJobDao.findById(jobId)

    private fun dispatch(jobId: Long, type: MediaJobType) {
        if (processing.putIfAbsent(jobId, true) != null) {
            return
        }
        taskExecutor.execute {
            processJob(jobId, type)
            processing.remove(jobId)
        }
    }

    private fun processJob(jobId: Long, type: MediaJobType) {
        mediaJobDao.updateRunning(jobId, MediaJobStatus.RUNNING)
        val start = System.nanoTime()
        try {
            val job = mediaJobDao.findById(jobId) ?: return
            val path = Path.of(job.filePath)
            val rawOutput = when (type) {
                MediaJobType.STT -> sttService.transcribe(jobId, path)
                MediaJobType.OCR -> ocrService.recognize(path)
            }
            val normalized = rawOutput.trim()
            val structured = if (normalized.isNotBlank()) {
                try {
                    structureTextService.structureFromText(rawOutput)
                } catch (structureError: Exception) {
                    logger.warn(
                        "Structuring text for media job {} failed: {}",
                        jobId,
                        structureError.message,
                    )
                    logger.debug("Structure failure for job $jobId", structureError)
                    null
                }
            } else {
                null
            }
            val payload = buildArticlePayload(type, jobId, normalized, structured)
            val article = articleService.createGeneratedArticle(
                userId = job.userId,
                type = type.name,
                title = payload.title,
                content = payload.content,
                source = "用户上传",
                publishTime = payload.publishTime,
            )
            val durationMs = (System.nanoTime() - start) / 1_000_000
            val (p50, p95) = metrics.record(durationMs)
            mediaJobDao.updateStatus(jobId, MediaJobStatus.DONE, payload.jobResultText)
            logger.info(
                "Media job {} completed in {} ms (p50={}, p95={}), articleId={}, structured={}",
                jobId,
                durationMs,
                p50,
                p95,
                article.id,
                structured != null,
            )
        } catch (ex: Exception) {
            val durationMs = (System.nanoTime() - start) / 1_000_000
            mediaJobDao.updateStatus(jobId, MediaJobStatus.FAILED, ex.message)
            logger.error("Media job {} failed after {} ms", jobId, durationMs, ex)
        }
    }

    private fun buildArticlePayload(
        type: MediaJobType,
        jobId: Long,
        normalizedText: String,
        structured: StructuredArticleDTO?,
    ): ArticlePayload {
        val fallbackTitle = defaultTitleFor(type, jobId)
        val fallbackContent = if (normalizedText.isBlank()) NO_TEXT_MESSAGE else normalizedText
        if (structured == null) {
            val jobText = if (fallbackContent == NO_TEXT_MESSAGE) {
                fallbackContent
            } else {
                "$fallbackTitle\n\n$fallbackContent"
            }
            return ArticlePayload(
                title = fallbackTitle,
                content = fallbackContent,
                publishTime = null,
                jobResultText = jobText.trim(),
            )
        }
        val resolvedTitle = structured.title.trim().takeIf { it.isNotEmpty() } ?: fallbackTitle
        val resolvedContent = structured.content.trim().takeIf { it.isNotEmpty() } ?: fallbackContent
        val publishTime = parsePublishTime(structured.publishTime)
        val jobResultText = buildResultSummary(resolvedTitle, structured, resolvedContent)
        return ArticlePayload(
            title = resolvedTitle,
            content = resolvedContent,
            publishTime = publishTime,
            jobResultText = jobResultText,
        )
    }

    private fun defaultTitleFor(type: MediaJobType, jobId: Long): String = when (type) {
        MediaJobType.STT -> "音频转写 $jobId"
        MediaJobType.OCR -> "图片识别 $jobId"
    }

    private fun buildResultSummary(
        resolvedTitle: String,
        structured: StructuredArticleDTO,
        content: String,
    ): String {
        val headerLines = buildList {
            add(resolvedTitle)
            structured.author?.trim()?.takeIf { it.isNotEmpty() }?.let { add("作者: $it") }
            structured.publishTime?.trim()?.takeIf { it.isNotEmpty() }?.let { add("发布时间: $it") }
        }
        val header = headerLines.joinToString(separator = "\n")
        val body = content.trim()
        return if (body.isEmpty()) {
            header
        } else {
            "$header\n\n$body".trim()
        }
    }

    private fun parsePublishTime(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) {
            return null
        }
        val trimmed = value.trim()
        return try {
            OffsetDateTime.parse(trimmed).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                ZonedDateTime.parse(trimmed).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                try {
                    Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } catch (_: DateTimeParseException) {
                    try {
                        LocalDateTime.parse(trimmed)
                    } catch (_: DateTimeParseException) {
                        null
                    }
                }
            }
        }
    }

    private data class ArticlePayload(
        val title: String,
        val content: String,
        val publishTime: LocalDateTime?,
        val jobResultText: String,
    )

    private companion object {
        private const val NO_TEXT_MESSAGE = "未识别到有效文本"
    }
}

private class MediaJobMetrics {
    private val durations = ArrayDeque<Long>()
    private val maxSamples = 50

    @Synchronized
    fun record(durationMs: Long): Pair<Double, Double> {
        if (durations.size >= maxSamples) {
            durations.removeFirst()
        }
        durations.addLast(durationMs)
        if (durations.isEmpty()) {
            return 0.0 to 0.0
        }
        val sorted = durations.sorted()
        val p50 = percentile(sorted, 0.5)
        val p95 = percentile(sorted, 0.95)
        return p50 to p95
    }

    private fun percentile(values: List<Long>, percentile: Double): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val index = (percentile * (values.size - 1)).coerceIn(0.0, (values.size - 1).toDouble())
        val lower = values[index.toInt()]
        val upper = values[ceil(index).toInt()]
        val fraction = index - index.toInt()
        return lower + (upper - lower) * fraction
    }
}
