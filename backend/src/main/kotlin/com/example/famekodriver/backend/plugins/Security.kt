package com.example.famekodriver.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import com.example.famekodriver.backend.db.DatabaseInitializer

@Serializable
data class AdminSession(val username: String, val role: String, val region: String?, val canViewAnalytics: Boolean = true)
data class AdminPrincipal(val username: String, val role: String, val region: String?, val canViewAnalytics: Boolean = true) : Principal

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<AdminSession>("ADMIN_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 * 24 // 24 hours
        }
    }

    install(Authentication) {
        session<AdminSession>("admin-auth") {
            validate { session ->
                AdminPrincipal(session.username, session.role, session.region, session.canViewAnalytics)
            }
            challenge {
                val accept = call.request.header(HttpHeaders.Accept) ?: ""
                if (accept.contains("application/json") || call.request.contentType().match(ContentType.Application.Json)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Session expired"))
                } else {
                    call.respondRedirect("/admin/login")
                }
            }
        }

        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "SELECT role, region, can_view_analytics FROM admins WHERE username = ? AND password = ? AND is_active = true"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, credentials.name)
                    stmt.setString(2, credentials.password)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        AdminPrincipal(
                            credentials.name,
                            rs.getString("role"),
                            rs.getString("region"),
                            rs.getBoolean("can_view_analytics")
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
}
