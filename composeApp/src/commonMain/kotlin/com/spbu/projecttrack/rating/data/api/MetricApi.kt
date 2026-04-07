package com.spbu.projecttrack.rating.data.api

import com.spbu.projecttrack.core.network.MetricApiConfig
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectInList
import com.spbu.projecttrack.rating.data.model.MetricRankingItem
import com.spbu.projecttrack.rating.data.model.RatingSyncProject
import com.spbu.projecttrack.rating.data.model.RatingSyncRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class MetricApi(
    private val client: HttpClient
) {
    suspend fun getProjects(): Result<List<MetricProjectInList>> {
        return try {
            val response = client.get("${MetricApiConfig.baseUrl}/project")
            if (!response.status.isSuccess()) {
                return Result.failure(RuntimeException("HTTP ${response.status}"))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectRatings(): Result<List<MetricRankingItem>> {
        return getProjects().map { projects ->
            projects
                .map { project ->
                    MetricRankingItem(
                        id = project.id,
                        name = project.name,
                        score = project.grade.toScoreOrNull()
                    )
                }
                .sortedByDescending { it.score ?: Double.NEGATIVE_INFINITY }
        }
    }

    suspend fun getStudentRatings(): Result<List<MetricRankingItem>> {
        return try {
            val response = client.get("${MetricApiConfig.baseUrl}/rating/students")
            if (!response.status.isSuccess()) {
                return Result.failure(RuntimeException("HTTP ${response.status}"))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectDetail(id: String): Result<MetricProjectDetail> {
        return try {
            val response = client.get("${MetricApiConfig.baseUrl}/project/$id")
            if (!response.status.isSuccess()) {
                return Result.failure(RuntimeException("HTTP ${response.status}"))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncProjects(projects: List<RatingSyncProject>): Result<Unit> {
        if (projects.isEmpty()) return Result.success(Unit)

        return try {
            val response = client.post("${MetricApiConfig.baseUrl}/rating/sync") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(RatingSyncRequest(projects = projects))
            }
            if (!response.status.isSuccess()) {
                return Result.failure(RuntimeException("HTTP ${response.status}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String?.toScoreOrNull(): Double? {
        val value = this?.trim()
        if (value.isNullOrEmpty()) return null
        if (value.equals("N/A", ignoreCase = true)) return null
        return value.toDoubleOrNull()
    }
}
