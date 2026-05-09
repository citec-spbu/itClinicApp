package com.spbu.projecttrack.projects.data.api

import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.network.ApiConfig
import com.spbu.projecttrack.projects.data.model.Member
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
private data class EditMemberRoleRequest(
    val member: EditMemberBody,
)

@Serializable
private data class EditMemberBody(
    val id: Int,
    val name: String,
    val roles: List<String>,
    val isAdministrator: Boolean? = null,
    val team: Int? = null,
    val user: Int? = null,
)

class MemberApi(private val client: HttpClient) {

    private val logTag = "MemberApi"

    suspend fun editMemberRoles(member: Member, roles: List<String>): Result<Unit> {
        return try {
            val url = "${ApiConfig.baseUrl}${ApiConfig.AuthRequired.MEMBER_EDIT}"
            AppLog.d(logTag, "PUT $url (memberId=${member.id}, roles=$roles)")

            val response = client.put(url) {
                accept(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(
                    EditMemberRoleRequest(
                        member = EditMemberBody(
                            id = member.id,
                            name = member.name,
                            roles = roles.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                            isAdministrator = member.isAdministrator,
                            team = member.team,
                            user = member.user,
                        ),
                    )
                )
            }

            val bodyText = response.bodyAsText()
            AppLog.d(logTag, "Status: ${response.status.value} ${response.status.description}")
            AppLog.d(logTag, "Response body: ${bodyText.take(500)}")

            if (!response.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("HTTP ${response.status.value} ${response.status.description}: $bodyText")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLog.e(logTag, "Failed to edit member role", e)
            Result.failure(e)
        }
    }
}
