package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.dao.UserTagDAO
import io.sakurasou.newsrecommend.dto.TagResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserTagService(
    private val userTagDao: UserTagDAO,
    private val tagDao: TagDAO,
    private val knowledgeGraphService: KnowledgeGraphService,
) {

    fun listAllTags(): List<TagResponse> = tagDao.findAll()
        .mapNotNull { tag ->
            val id = tag.id ?: return@mapNotNull null
            TagResponse(id = id, name = tag.name)
        }

    @Transactional
    fun saveUserTags(userId: Long, tagIds: List<Long>) {
        userTagDao.deleteByUserId(userId)
        tagIds.distinct().forEach { tagId ->
            userTagDao.insertUserTag(userId, tagId, 0.5)
        }
        val views = userTagDao.findViewsByUserId(userId)
        knowledgeGraphService.syncUserTagInterests(userId, views)
    }

    @Transactional
    fun incrementWeights(userId: Long, increments: Map<Long, Double>, maxWeight: Double = 2.0) {
        increments.forEach { (tagId, delta) ->
            val updated = userTagDao.incrementUserTag(userId, tagId, delta, maxWeight)
            if (updated == 0) {
                val initial = delta.coerceAtMost(maxWeight)
                userTagDao.insertUserTag(userId, tagId, initial)
            }
        }
        val views = userTagDao.findViewsByUserId(userId)
        knowledgeGraphService.syncUserTagInterests(userId, views)
    }
}
