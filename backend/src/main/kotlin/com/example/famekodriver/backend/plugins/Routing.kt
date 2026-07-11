package com.example.famekodriver.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.sessions.*
import io.ktor.server.http.content.*
import io.ktor.http.content.*
import com.example.famekodriver.backend.services.H3Helper
import com.example.famekodriver.backend.services.RedisManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.backend.services.IntelligenceService
import com.example.famekodriver.backend.services.SimulationParams
import kotlinx.coroutines.runBlocking
import com.example.famekodriver.backend.db.DatabaseInitializer
import kotlinx.coroutines.flow.MutableSharedFlow
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.LocalDate
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.max
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

val sosFlow = MutableSharedFlow<SOSAlert>(extraBufferCapacity = 64)

val PAYSTACK_SECRET = System.getenv("PAYSTACK_SECRET") ?: ""

fun Application.configureRouting() {
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        exception<Throwable> { call, cause ->
            println("GLOBAL EXCEPTION: ${cause.message}")
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, AuthResponse(false, "Server Error: ${cause.localizedMessage}", null, null))
        }
    }
    routing {
        staticResources("/static", "static")

        get("/") { call.respondRedirect("/admin/dashboard") }

        get("/admin/login") { call.respond(ThymeleafContent("login", emptyMap())) }

        post("/admin/api-login") {
            try {
                val req = call.receive<LoginRequest>()
                val email = req.email ?: ""
                val password = req.password ?: ""
                val admin = loginAdminInDb(email, password)
                if (admin != null) {
                    val session = AdminSession(
                        admin["username"].toString(),
                        admin["role"].toString(),
                        null,
                        true
                    )
                    call.sessions.set(session)
                    call.respond(AuthResponse(true, "Success", admin["id"].toString(), admin["username"].toString(), user_role = admin["role"].toString()))
                } else {
                    call.respond(AuthResponse(false, "Invalid credentials", null, null))
                }
            } catch (e: Exception) {
                call.respond(AuthResponse(false, e.message ?: "Login error", null, null))
            }
        }

        authenticate("auth-form") {
            post("/admin/login") {
                val principal = call.principal<AdminPrincipal>()!!
                call.sessions.set(AdminSession(principal.username, principal.role, principal.region, principal.canViewAnalytics))
                call.respondRedirect("/admin/dashboard")
            }
        }

        get("/admin/logout") {
            call.sessions.clear<AdminSession>()
            call.respondRedirect("/admin/login")
        }

        get("/payment-success") {
            call.respondText(
                "<html><body style='text-align:center;padding-top:50px;font-family:sans-serif;'>" +
                "<h1>Payment Successful!</h1>" +
                "<p>Your transaction has been processed. You can now return to the app.</p>" +
                "<a href='fameko://rental-payment' style='display:inline-block;padding:15px 30px;background:#2ecc71;color:white;text-decoration:none;border-radius:5px;'>Return to Fameko App</a>" +
                "<script>setTimeout(function(){ window.location.href = 'fameko://rental-payment'; }, 3000);</script>" +
                "</body></html>",
                ContentType.Text.Html
            )
        }

        // --- ADMIN PANEL ---
        authenticate("admin-auth") {
            route("/admin") {
                get("/dashboard") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val region = if (principal.role == "REGIONAL_ADMIN") principal.region else null
                    
                    val drivers = getAllDriversFromDb(region)
                    val fleetOwners = getAllFleetOwnersFromDb(region)
                    val stats = getPlatformStatsFromDb()
                    val rentals = getAllRentalsFromDb(region)
                    val liveDeliveries = getActiveDeliveriesFromDb()
                    val sosCount = RedisManager.getActiveSOSCount()

                    call.respond(ThymeleafContent("admin_dashboard", mapOf(
                        "drivers" to drivers.take(10),
                        "totalDrivers" to drivers.size,
                        "totalFleetOwners" to fleetOwners.size,
                        "pendingCount" to drivers.count { it["status"] == "PENDING" || it["status"] == "PENDING_DOCS" },
                        "pendingFleetOwners" to fleetOwners.count { it["status"] == "PENDING" },
                        "onlineCount" to drivers.count { it["is_online"] == true },
                        "totalRevenue" to stats["totalRevenue"],
                        "totalDebt" to stats["totalDebt"],
                        "totalDeliveries" to liveDeliveries.size,
                        "pendingRentals" to rentals.count { it["status"] == "PENDING" },
                        "activeSOSCount" to sosCount,
                        "deliveries" to liveDeliveries.take(10),
                        "activePage" to "dashboard",
                        "admin" to principal
                    )))
                }

                get("/drivers") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val statusFilter = call.parameters["status"]
                    val regionFilter = call.parameters["region"]
                    val principalRegion = if (principal.role == "REGIONAL_ADMIN") principal.region else null
                    
                    val allDrivers = getAllDriversFromDb(principalRegion)
                    val filteredDrivers = allDrivers.filter { driver ->
                        (statusFilter.isNullOrBlank() || driver["status"].toString().equals(statusFilter, ignoreCase = true)) &&
                        (regionFilter.isNullOrBlank() || driver["region"].toString().equals(regionFilter, ignoreCase = true))
                    }

                    // Group by region for better arrangement if requested
                    val groupedDrivers = filteredDrivers.groupBy { it["region"].toString() }

                    call.respond(ThymeleafContent("admin_drivers", mapOf(
                        "drivers" to filteredDrivers,
                        "groupedDrivers" to groupedDrivers,
                        "activePage" to "drivers",
                        "admin" to principal,
                        "selectedStatus" to (statusFilter ?: ""),
                        "selectedRegion" to (regionFilter ?: "")
                    )))
                }

                get("/driver/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val driver = getDriverDetailsFromDb(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(ThymeleafContent("admin_driver_details", mapOf("driver" to driver, "activePage" to "drivers", "admin" to call.principal<AdminPrincipal>()!!)))
                }

                post("/driver/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val p = call.receiveParameters()
                    val category = p["category"] ?: "Economy"
                    
                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        conn.prepareStatement("UPDATE drivers SET status = 'APPROVED', vehicle_category = ? WHERE id = ?").apply {
                            setString(1, category)
                            setInt(2, id)
                            executeUpdate()
                        }
                    }
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateDriverStatusInDb(id, "REJECTED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/suspend/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateDriverStatusInDb(id, "SUSPENDED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/release/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateDriverStatusInDb(id, "APPROVED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/terminate/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    terminateDriverAccount(id)
                    call.respondRedirect("/admin/drivers")
                }

                post("/customer/terminate/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    terminateCustomerAccount(id)
                    call.respondRedirect("/admin/customers")
                }

                // --- FLEET OWNERS ---
                get("/fleet-owners") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val statusFilter = call.parameters["status"]
                    val regionFilter = call.parameters["region"]
                    val principalRegion = if (principal.role == "REGIONAL_ADMIN") principal.region else null

                    val allOwners = getAllFleetOwnersFromDb(principalRegion)
                    val filteredOwners = allOwners.filter { owner ->
                        (statusFilter.isNullOrBlank() || owner["status"].toString().equals(statusFilter, ignoreCase = true)) &&
                        (regionFilter.isNullOrBlank() || owner["region"].toString().equals(regionFilter, ignoreCase = true))
                    }

                    val groupedOwners = filteredOwners.groupBy { it["region"].toString() }

                    call.respond(ThymeleafContent("admin_fleet_owners", mapOf(
                        "owners" to filteredOwners,
                        "groupedOwners" to groupedOwners,
                        "activePage" to "fleet-owners",
                        "admin" to principal,
                        "selectedStatus" to (statusFilter ?: ""),
                        "selectedRegion" to (regionFilter ?: "")
                    )))
                }

                get("/fleet-owner/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val owner = getFleetOwnerDetailsFromDb(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(ThymeleafContent("admin_fleet_owner_details", mapOf(
                        "owner" to owner,
                        "activePage" to "fleet-owners",
                        "admin" to call.principal<AdminPrincipal>()!!
                    )))
                }

                post("/fleet-owner/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateFleetOwnerStatusInDb(id, "APPROVED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateFleetOwnerStatusInDb(id, "REJECTED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/suspend/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateFleetOwnerStatusInDb(id, "SUSPENDED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/release/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateFleetOwnerStatusInDb(id, "APPROVED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }


                get("/map") {
                    val principal = call.principal<AdminPrincipal>()!!
                    call.respond(ThymeleafContent("admin_map", mapOf("activePage" to "map", "admin" to principal)))
                }

                get("/live-locations") {
                    call.respond(getLiveLocationsFromDb())
                }

                get("/active-sos") {
                    call.respond(RedisManager.getAllSOS())
                }

                post("/resolve-sos/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    RedisManager.resolveSOS(id)
                    call.respond(mapOf("success" to true))
                }

                get("/customers") {
                    val principal = call.principal<AdminPrincipal>()!!
                    call.respond(ThymeleafContent("admin_customers", mapOf(
                        "customers" to getAllCustomersFromDb(),
                        "activePage" to "customers",
                        "admin" to principal
                    )))
                }

                get("/deliveries") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val filterType = call.parameters["type"] ?: "RIDE_HAILING"
                    
                    val allDeliveries = getActiveDeliveriesFromDb()
                    val filtered = allDeliveries.filter { 
                        if (filterType == "PACKAGE_DELIVERY") {
                            it["service_type"] == "PACKAGE_DELIVERY"
                        } else {
                            it["service_type"] == "RIDE_HAILING" || it["service_type"] == "BOTH" || it["service_type"] == "car"
                        }
                    }

                    call.respond(ThymeleafContent("admin_deliveries", mapOf(
                        "deliveries" to filtered,
                        "activePage" to "deliveries",
                        "admin" to principal,
                        "selectedType" to filterType
                    )))
                }

                get("/daily-payments") {
                    val principal = call.principal<AdminPrincipal>()!!
                    call.respond(ThymeleafContent("admin_daily_payments", mapOf(
                        "payments" to getPendingPaymentsFromDb(),
                        "activePage" to "daily-payments",
                        "admin" to principal
                    )))
                }

                post("/daily-payments/approve") {
                    val p = call.receiveParameters()
                    val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    approvePaymentInDb(id)
                    call.respondRedirect("/admin/daily-payments")
                }

                post("/daily-payments/reject") {
                    val p = call.receiveParameters()
                    val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    rejectPaymentInDb(id)
                    call.respondRedirect("/admin/daily-payments")
                }

                get("/rentals") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val region = if (principal.role == "REGIONAL_ADMIN") principal.region else null
                    call.respond(ThymeleafContent("admin_rentals", mapOf(
                        "rentals" to getAllRentalsFromDb(region),
                        "activePage" to "rentals",
                        "admin" to principal
                    )))
                }

                post("/rentals/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateRentalStatusInDb(id, "ACTIVE")
                    call.respondRedirect("/admin/rentals")
                }

                post("/rentals/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateRentalStatusInDb(id, "REJECTED")
                    call.respondRedirect("/admin/rentals")
                }

                get("/pricing") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    val config = getPricingConfigFromDb()
                    call.respond(ThymeleafContent("admin_pricing", mapOf(
                        "pricing" to config,
                        "rentalRates" to getRentalRatesFromDb(),
                        "activePage" to "pricing",
                        "admin" to principal,
                        "showSuccess" to (call.parameters["success"] == "true")
                    )))
                }

                post("/pricing/update") {
                    val p = call.receiveParameters()
                    updatePricingConfigInDb(
                        baseFare = p["baseFare"]?.toDoubleOrNull(),
                        perKmRate = p["perKmRate"]?.toDoubleOrNull(),
                        perMinuteRate = p["perMinuteRate"]?.toDoubleOrNull(),
                        minFare = p["minFare"]?.toDoubleOrNull(),
                        milestoneInterval = p["milestoneInterval"]?.toIntOrNull(),
                        milestoneDiscount = p["milestoneDiscount"]?.toIntOrNull(),
                        driverCommission = p["driverCommission"]?.toDoubleOrNull(),
                        peakMultiplier = p["peakMultiplier"]?.toDoubleOrNull(),
                        dailyServiceFee = p["dailyServiceFee"]?.toDoubleOrNull(),
                        rentalOwnerComm = p["rentalOwnerCommission"]?.toDoubleOrNull(),
                        rentalGuestFee = p["rentalCustomerFee"]?.toDoubleOrNull()
                    )
                    call.respondRedirect("/admin/pricing?success=true")
                }

                post("/pricing/update-rentals") {
                    val p = call.receiveParameters()
                    updatePricingConfigInDb(
                        baseFare = null, perKmRate = null, perMinuteRate = null, minFare = null,
                        milestoneInterval = null, milestoneDiscount = null, driverCommission = null,
                        peakMultiplier = null, dailyServiceFee = null,
                        rentalOwnerComm = p["rentalOwnerCommission"]?.toDoubleOrNull(),
                        rentalGuestFee = p["rentalCustomerFee"]?.toDoubleOrNull()
                    )
                    
                    // Update individual vehicle rates
                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        p.entries().forEach { (key, values) ->
                            if (key.startsWith("rate_")) {
                                val type = key.removePrefix("rate_")
                                val rate = values.firstOrNull()?.toDoubleOrNull() ?: 0.0
                                conn.prepareStatement("UPDATE rental_rates SET daily_rate = ?, updated_at = CURRENT_TIMESTAMP WHERE vehicle_type = ?").apply {
                                    setDouble(1, rate)
                                    setString(2, type)
                                    executeUpdate()
                                }
                            }
                        }
                    }
                    call.respondRedirect("/admin/pricing?success=true")
                }

                get("/financials") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val month = call.parameters["month"]
                    val date = call.parameters["date"]
                    
                    val stats = getFinancialStatsFromDb(month, date)

                    call.respond(ThymeleafContent("admin_financials", mapOf(
                        "activePage" to "financials",
                        "admin" to principal,
                        "selectedMonth" to (month ?: ""),
                        "selectedDate" to (date ?: ""),
                        "stats" to stats
                    )))
                }

                get("/settings") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    val settings = getSystemSettingsFromDb()
                    call.respond(ThymeleafContent("admin_settings", mapOf(
                        "activePage" to "settings",
                        "admin" to principal,
                        "settings" to settings,
                        "showSuccess" to (call.parameters["success"] == "true")
                    )))
                }

                post("/settings/update") {
                    val p = call.receiveParameters()
                    updateSystemSettingsInDb(
                        phone = p["supportPhone"],
                        email = p["supportEmail"],
                        whatsapp = p["supportWhatsApp"],
                        verCustomer = p["versionCustomer"],
                        verDriver = p["versionDriver"],
                        maintenance = p["maintenanceMode"] != null,
                        psMode = p["paystackMode"],
                        psTestSec = p["paystackTestSecret"],
                        psLiveSec = p["paystackLiveSecret"],
                        psTestPub = p["paystackTestPublic"],
                        psLivePub = p["paystackLivePublic"]
                    )
                    call.respondRedirect("/admin/settings?success=true")
                }

                get("/admins") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    call.respond(ThymeleafContent("admin_manage", mapOf("admins" to getAllAdminsFromDb(), "activePage" to "admins", "admin" to principal)))
                }

                post("/admins/create") {
                    val p = call.receiveParameters()
                    createAdminInDb(p["username"]!!, p["email"]!!, p["password"]!!, p["role"]!!, p["region"])
                    call.respondRedirect("/admin/admins")
                }

                get("/fleet") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val vehicles = getAllFleetVehiclesFromDb()
                    call.respond(ThymeleafContent("admin_fleet", mapOf(
                        "vehicles" to vehicles,
                        "activePage" to "fleet",
                        "admin" to principal,
                        "showSuccess" to (call.parameters["success"] == "true")
                    )))
                }

                post("/fleet/update") {
                    val p = call.receiveParameters()
                    val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    updateFleetVehicleInDb(
                        id = id,
                        name = p["name"] ?: "",
                        model = p["model"] ?: "",
                        type = p["vehicle_type"] ?: "",
                        number = p["vehicle_number"] ?: "",
                        rate = p["daily_rate"]?.toDoubleOrNull() ?: 0.0,
                        description = p["description"],
                        features = p["features"],
                        imageUrls = p["image_urls"],
                        status = p["status"],
                        seats = p["seats"]?.toIntOrNull(),
                        transmission = p["transmission"],
                        fuelType = p["fuel_type"]
                    )
                    call.respondRedirect("/admin/fleet?success=true")
                }

                post("/fleet/delete/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    deleteFleetVehicleFromDb(id)
                    call.respondRedirect("/admin/fleet")
                }

                get("/intelligence") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (!principal.canViewAnalytics) return@get call.respondRedirect("/admin/dashboard")
                    
                    val report = IntelligenceService.getAnalyticsReport()
                    call.respond(ThymeleafContent("admin_intelligence", mapOf(
                        "activePage" to "intelligence",
                        "admin" to principal,
                        "report" to report
                    )))
                }

                post("/intelligence/simulate") {
                    val p = call.receiveParameters()
                    val params = SimulationParams(
                        driverCount = p["driverCount"]?.toIntOrNull() ?: 10,
                        customerCount = p["customerCount"]?.toIntOrNull() ?: 50,
                        weather = p["weather"] ?: "CLEAR",
                        timeOfDay = p["timeOfDay"] ?: "AFTERNOON"
                    )
                    val result = IntelligenceService.runSimulation(params)
                    call.respond(result)
                }

                get("/intelligence/movement") {
                    val history = IntelligenceService.getMovementHistory()
                    call.respond(history)
                }

                get("/intelligence/export") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (!principal.canViewAnalytics) return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val logs = IntelligenceService.getMovementHistory(5000)
                    val csv = StringBuilder("ID,EntityID,Type,Lat,Lng,Timestamp\n")
                    logs.forEach { log ->
                        csv.append("${log["id"]},${log["entity_id"]},${log["entity_type"]},${log["lat"]},${log["lng"]},${log["created_at"]}\n")
                    }
                    
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "fameko_movement_data.csv").toString()
                    )
                    call.respondText(csv.toString(), contentType = ContentType.Text.CSV)
                }

                get("/database") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    
                    val tables = getAllTablesFromDb()
                    call.respond(ThymeleafContent("admin_db_explorer", mapOf(
                        "activePage" to "database",
                        "admin" to principal,
                        "tables" to tables,
                        "selectedTable" to null
                    )))
                }

                get("/database/{tableName}") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    
                    val tableName = call.parameters["tableName"] ?: return@get call.respondRedirect("/admin/database")
                    val tables = getAllTablesFromDb()
                    
                    if (tableName !in tables) return@get call.respondRedirect("/admin/database")
                    
                    val (columns, rows) = getTableDataFromDb(tableName)
                    
                    call.respond(ThymeleafContent("admin_db_explorer", mapOf(
                        "activePage" to "database",
                        "admin" to principal,
                        "tables" to tables,
                        "selectedTable" to tableName,
                        "columns" to columns,
                        "rows" to rows
                    )))
                }

                get("/api/database/{tableName}") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val tableName = call.parameters["tableName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val tables = getAllTablesFromDb()
                    if (tableName !in tables) return@get call.respond(HttpStatusCode.NotFound)
                    
                    val (columns, rows) = getTableDataFromDb(tableName)
                    call.respond(mapOf(
                        "columns" to columns,
                        "rows" to rows
                    ))
                }

                webSocket("/ws-sos") {
                    val principal = call.principal<AdminPrincipal>()
                    if (principal == null || principal.role != "SUPERADMIN") {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                        return@webSocket
                    }
                    
                    println("Admin SOS WS: Connected")
                    try {
                        sosFlow.collect { alert ->
                            send(Frame.Text(com.google.gson.Gson().toJson(alert)))
                        }
                    } catch (e: Exception) {
                        println("Admin SOS WS Error: ${e.message}")
                    } finally {
                        println("Admin SOS WS: Disconnected")
                    }
                }
            }
        }

        // --- API ROUTES ---

        route("/api/admin") {
            get("/live-locations") {
                call.respond(getLiveLocationsFromDb())
            }

            get("/active-sos") {
                call.respond(RedisManager.getAllSOS())
            }

            get("/pending-drivers") {
                val drivers = getAllDriversFromDb(null).filter { it["status"] == "PENDING" || it["status"] == "PENDING_DOCS" }
                call.respond(drivers)
            }

            get("/stats") {
                val drivers = getAllDriversFromDb(null)
                val stats = getPlatformStatsFromDb()
                val liveDeliveries = getActiveDeliveriesFromDb()
                
                call.respond(mapOf(
                    "totalDrivers" to drivers.size,
                    "pendingDrivers" to drivers.count { it["status"] == "PENDING" || it["status"] == "PENDING_DOCS" },
                    "onlineDrivers" to drivers.count { it["is_online"] == true },
                    "liveDeliveries" to liveDeliveries.size,
                    "totalRevenue" to stats["totalRevenue"],
                    "totalDebt" to stats["totalDebt"],
                    "activeSOS" to RedisManager.getActiveSOSCount()
                ))
            }

            get("/daily-payments") {
                call.respond(getPendingPaymentsFromDb())
            }

            get("/customers") {
                call.respond(getAllCustomersFromDb())
            }

            get("/deliveries") {
                call.respond(getActiveDeliveriesFromDb())
            }

            get("/driver/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val driver = getDriverDetailsFromDb(id)
                if (driver != null) call.respond(driver) else call.respond(HttpStatusCode.NotFound)
            }

            get("/financials") {
                call.respond(getFinancialStatsFromDb(null, null))
            }
        }

        get("/api/weather") {
            val lat = call.parameters["lat"]?.toDoubleOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing lat")
            val lon = call.parameters["lon"]?.toDoubleOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing lon")
            
            try {
                val apiKey = System.getenv("OPENWEATHER_API_KEY") ?: "b03dab643348ea7b4c96676da9190314"
                val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric")
                println("Backend: Fetching weather from OpenWeather for $lat, $lon")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    println("Backend: Weather fetch successful: $response")
                    call.respondText(response, ContentType.Application.Json)
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    println("Backend: Weather fetch failed with code ${conn.responseCode}: $error")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to error))
                }
            } catch (e: Exception) {
                println("Backend: Weather fetch exception: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.localizedMessage))
            }
        }

        route("/api/admin") {
            get("/rentals") {
                val principal = call.principal<AdminPrincipal>()
                val region = if (principal?.role == "REGIONAL_ADMIN") principal.region else null
                call.respond(getAllRentalsFromDb(region))
            }

            post("/payments/approve/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                approvePaymentInDb(id)
                call.respond(mapOf("success" to true))
            }

            post("/payments/reject/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                rejectPaymentInDb(id)
                call.respond(mapOf("success" to true))
            }

            post("/driver/approve/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    conn.prepareStatement("UPDATE drivers SET status = 'APPROVED' WHERE id = ?").apply {
                        setInt(1, id)
                        executeUpdate()
                    }
                }
                call.respond(mapOf("success" to true))
            }

            post("/driver/reject/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                updateDriverStatusInDb(id, "REJECTED")
                call.respond(mapOf("success" to true))
            }
        }

        post("/customer/login") {
            val req = call.receive<LoginRequest>()
            val phone = req.phone
            if (phone.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Phone number is required", null, null))
                return@post
            }
            val user = loginCustomerInDbByPhone(phone)
            if (user != null) call.respond(AuthResponse(true, "Success", user["id"].toString(), user["name"].toString()))
            else call.respond(AuthResponse(false, "User not found with this phone number", null, null))
        }

        post("/customer/request-otp") {
            val req = call.receive<OtpRequest>()
            val phone = req.phone
            val otp = (100000..999999).random().toString()
            
            RedisManager.storeLoginOtp(phone, otp)
            
            // Simulate sending WhatsApp message
            println("WHATSAPP_SIMULATOR: Sending OTP $otp to $phone via WhatsApp")
            
            call.respond(mapOf("success" to true, "message" to "OTP sent to WhatsApp"))
        }

        post("/customer/verify-otp") {
            val req = call.receive<OtpVerifyRequest>()
            if (RedisManager.verifyLoginOtp(req.phone, req.otp)) {
                val user = loginCustomerInDbByPhone(req.phone)
                if (user != null) {
                    call.respond(AuthResponse(true, "Success", user["id"].toString(), user["name"].toString()))
                } else {
                    call.respond(AuthResponse(false, "User not found", null, null))
                }
            } else {
                call.respond(AuthResponse(false, "Invalid or expired OTP", null, null))
            }
        }

        post("/customer/google-login") {
            val req = call.receive<LoginRequest>()
            val idToken = req.googleToken
            if (idToken.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Google token is required", null, null))
                return@post
            }

            val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: "989048143840-chmqrl6lr2s0kdtep3gbbp0t5kse2gf6.apps.googleusercontent.com"
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(listOf(googleClientId))
                .build()

            try {
                val token = verifier.verify(idToken)
                if (token != null) {
                    val payload = token.payload
                    val email = payload.email
                    val uid = payload.subject // Google unique ID
                    
                    val user = syncCustomerWithFirebase(email, uid)
                    if (user != null) {
                        call.respond(AuthResponse(true, "Success", user["id"].toString(), user["name"].toString()))
                    } else {
                        call.respond(AuthResponse(false, "USER_NOT_FOUND", null, null))
                    }
                } else {
                    call.respond(AuthResponse(false, "Invalid Google token", null, null))
                }
            } catch (e: Exception) {
                call.respond(AuthResponse(false, "Google verification failed: ${e.message}", null, null))
             }
        }

        post("/customer/register") {
            try {
                val req = call.receive<CustomerRegisterRequest>()
                println("Backend: Registering customer ${req.email}")
                val userId = registerCustomerInDb(req)
                if (userId != null) {
                    println("Backend: Registration successful for ID $userId")
                    call.respond(AuthResponse(true, "Registration successful", userId.toString(), req.name))
                } else {
                    println("Backend: Registration failed in DB helper")
                    call.respond(AuthResponse(false, "Registration failed", null, null))
                }
            } catch (e: Exception) {
                println("Backend: Customer registration error: ${e.message}")
                e.printStackTrace()
                call.respond(AuthResponse(false, e.message ?: "Server Error", null, null))
            }
        }

        get("/customer/profile/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "SELECT id, name, email, phone, region, profile_picture FROM customers WHERE id = ?"
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    call.respond(mapOf(
                        "success" to true,
                        "id" to rs.getInt("id"),
                        "name" to rs.getString("name"),
                        "email" to rs.getString("email"),
                        "phone" to rs.getString("phone"),
                        "region" to rs.getString("region"),
                        "profile_picture" to rs.getString("profile_picture")
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Customer not found"))
                }
            }
        }

        get("/customer/saved-places/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val list = mutableListOf<SavedPlace>()
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "SELECT * FROM saved_places WHERE customer_id = ?"
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, customerId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(SavedPlace(
                        id = rs.getString("id"),
                        customerId = rs.getInt("customer_id").toString(),
                        label = rs.getString("label"),
                        address = rs.getString("address"),
                        latitude = rs.getDouble("latitude"),
                        longitude = rs.getDouble("longitude")
                    ))
                }
            }
            call.respond(list)
        }

        get("/customer/trips/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val list = mutableListOf<Map<String, Any>>()
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = """
                    SELECT o.id, o.pickup_location, o.dropoff_location, o.total_amount as fare, 
                           o.status, o.created_at, d.pickup_lat, d.pickup_lng, d.dropoff_lat, d.dropoff_lng
                    FROM orders o
                    LEFT JOIN deliveries d ON o.id = d.order_id
                    WHERE o.customer_id = ?
                    ORDER BY o.id DESC
                """.trimIndent()
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, customerId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(mapOf(
                        "id" to rs.getInt("id"),
                        "pickup" to (rs.getString("pickup_location") ?: ""),
                        "dropoff" to (rs.getString("dropoff_location") ?: ""),
                        "fare" to rs.getDouble("fare"),
                        "status" to (rs.getString("status") ?: "UNKNOWN"),
                        "created_at" to rs.getTimestamp("created_at").toString(),
                        "dropoff_lat" to rs.getDouble("dropoff_lat"),
                        "dropoff_lng" to rs.getDouble("dropoff_lng")
                    ))
                }
            }
            call.respond(list)
        }

        post("/customer/saved-places") {
            try {
                val req = call.receive<SavedPlace>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "INSERT INTO saved_places (id, customer_id, label, address, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, req.id)
                    stmt.setInt(2, req.customerId.toInt())
                    stmt.setString(3, req.label)
                    stmt.setString(4, req.address)
                    stmt.setDouble(5, req.latitude)
                    stmt.setDouble(6, req.longitude)
                    stmt.executeUpdate()
                }
                call.respond(mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
            }
        }

        put("/customer/saved-places/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<SavedPlace>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "UPDATE saved_places SET label = ?, address = ?, latitude = ?, longitude = ? WHERE id = ?"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, req.label)
                    stmt.setString(2, req.address)
                    stmt.setDouble(3, req.latitude)
                    stmt.setDouble(4, req.longitude)
                    stmt.setString(5, id)
                    val rows = stmt.executeUpdate()
                    call.respond(mapOf<String, Any>("success" to (rows > 0)))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
            }
        }

        delete("/customer/saved-places/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "DELETE FROM saved_places WHERE id = ?"
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, id)
                val rows = stmt.executeUpdate()
                call.respond(mapOf<String, Any>("success" to (rows > 0)))
            }
        }

        get("/driver/profile/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "SELECT id, full_name, email, phone, region, profile_picture, vehicle_type, vehicle_number, vehicle_model, status FROM drivers WHERE id = ?"
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    call.respond(mapOf(
                        "success" to true,
                        "id" to rs.getInt("id"),
                        "name" to rs.getString("full_name"),
                        "email" to rs.getString("email"),
                        "phone" to rs.getString("phone"),
                        "region" to rs.getString("region"),
                        "profile_picture" to rs.getString("profile_picture"),
                        "vehicle_type" to rs.getString("vehicle_type"),
                        "vehicle_number" to rs.getString("vehicle_number"),
                        "vehicle_model" to rs.getString("vehicle_model"),
                        "status" to rs.getString("status")
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Driver not found"))
                }
            }
        }

        post("/driver/login") {
            val req = call.receive<LoginRequest>()
            val phone = req.phone
            if (phone.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Phone number is required", null, null))
                return@post
            }
            val driver = loginDriverInDbByPhone(phone)
            if (driver != null) call.respond(AuthResponse(
                success = true, 
                message = "Success", 
                user_id = driver.id.toString(), 
                name = driver.fullName, 
                status = driver.status, 
                profile_picture = driver.profilePicture,
                user_role = driver.userRole,
                company_name = driver.companyName
            ))
            else call.respond(AuthResponse(false, "Driver not found with this phone number", null, null))
        }

        post("/driver/request-otp") {
            val req = call.receive<OtpRequest>()
            val phone = req.phone
            val otp = (100000..999999).random().toString()
            
            RedisManager.storeLoginOtp(phone, otp)
            
            println("WHATSAPP_SIMULATOR: Sending OTP $otp to $phone via WhatsApp (Driver)")
            
            call.respond(mapOf("success" to true, "message" to "OTP sent to WhatsApp"))
        }

        post("/driver/verify-otp") {
            val req = call.receive<OtpVerifyRequest>()
            if (RedisManager.verifyLoginOtp(req.phone, req.otp)) {
                val driver = loginDriverInDbByPhone(req.phone)
                if (driver != null) {
                    call.respond(AuthResponse(
                        success = true, 
                        message = "Success", 
                        user_id = driver.id.toString(), 
                        name = driver.fullName, 
                        status = driver.status, 
                        profile_picture = driver.profilePicture,
                        user_role = driver.userRole,
                        company_name = driver.companyName
                    ))
                } else {
                    call.respond(AuthResponse(false, "Driver not found", null, null))
                }
            } else {
                call.respond(AuthResponse(false, "Invalid or expired OTP", null, null))
            }
        }

        post("/driver/google-login") {
            val req = call.receive<LoginRequest>()
            val idToken = req.googleToken
            if (idToken.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Google token is required", null, null))
                return@post
            }

            val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: "989048143840-chmqrl6lr2s0kdtep3gbbp0t5kse2gf6.apps.googleusercontent.com"
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(listOf(googleClientId))
                .build()

            try {
                val token = verifier.verify(idToken)
                if (token != null) {
                    val payload = token.payload
                    val email = payload.email
                    val name = payload["name"] as? String ?: "Google Driver"
                    val uid = payload.subject
                    
                    val user = syncDriverWithFirebase(email, uid)
                    if (user != null) {
                        call.respond(AuthResponse(
                            success = true, 
                            message = "Success", 
                            user_id = user.id.toString(), 
                            name = user.fullName,
                            status = user.status,
                            user_role = user.userRole
                        ))
                    } else {
                        call.respond(AuthResponse(false, "USER_NOT_FOUND", null, null))
                    }
                } else {
                    call.respond(AuthResponse(false, "Invalid Google token", null, null))
                }
            } catch (e: Exception) {
                call.respond(AuthResponse(false, "Google verification failed: ${e.message}", null, null))
            }
        }

        post("/driver/register") {
            try {
                val multipart = call.receiveMultipart()
                val data = mutableMapOf<String, String>()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            data[part.name ?: ""] = part.value
                        }
                        else -> {}
                    }
                    part.dispose()
                }
                val driverId = registerDriverInDb(data)
                if (driverId != null) call.respond(AuthResponse(
                    success = true, 
                    message = "Success", 
                    user_id = driverId.toString(), 
                    name = data["full_name"]
                ))
                else call.respond(AuthResponse(false, "Registration failed", null, null))
            } catch (e: Exception) {
                call.respond(AuthResponse(false, e.message ?: "Error", null, null))
            }
        }

        // --- PASSWORD RESET REMOVED ---

        get("/pricing/config") { call.respond(getPricingConfigFromDb()) }

        route("/api/v1") {
            get("/products") {
                call.respond(getAllProductsFromDb())
            }
            get("/products/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val product = getProductFromDb(id)
                if (product != null) {
                    call.respond(product)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Product not found")
                }
            }
        }

        post("/orders/estimates") {
            val p = call.receiveParameters()
            val lat = p["lat"]?.toDoubleOrNull() ?: 0.0
            val lng = p["lng"]?.toDoubleOrNull() ?: 0.0
            val dist = p["dist"]?.toDoubleOrNull() ?: 0.0
            val dur = p["dur"]?.toDoubleOrNull() ?: 0.0

            val config = getPricingConfigFromDb()
            val base = config.baseFare
            val perKm = config.perKmRate
            val perMin = config.perMinuteRate
            val peak = config.peakMultiplier
            val minFare = config.minFare

            val estimates = mutableListOf<RideEstimateResponse>()
            val types = listOf(
                Pair("Economy", "car"), 
                Pair("Comfort", "car"), 
                Pair("Pragya", "pragya"), 
                Pair("Okada", "okada"), 
                Pair("Aboboyaa", "aboboyaa"), 
                Pair("Truck", "truck"), 
                Pair("Bicycle", "bicycle")
            )

            types.forEach { (name, internal) ->
                val desc = when(name) {
                    "Economy" -> "Quick and affordable rides"
                    "Comfort" -> "Newer cars, extra legroom"
                    "Pragya" -> "Local tricycle trips"
                    "Okada" -> "Quickest bike trips"
                    "Aboboyaa" -> "Heavy load/cargo transport"
                    "Truck" -> "Large moving and hauling"
                    "Bicycle" -> "Short distance eco deliveries"
                    else -> ""
                }
                val multiplier = if (name == "Comfort") 1.3 else if (name == "Pragya" || name == "Okada") 0.7 else if (name == "Aboboyaa") 0.8 else if (name == "Truck") 2.0 else if (name == "Bicycle") 0.4 else 1.0
                val finalMinFare = if (name == "Pragya") minFare * 0.7 else if (name == "Okada") minFare * 0.6 else if (name == "Aboboyaa") minFare * 0.8 else if (name == "Truck") minFare * 2.0 else if (name == "Bicycle") minFare * 0.4 else minFare

                estimates.add(RideEstimateResponse(
                    serviceId = name,
                    name = name,
                    description = desc,
                    fare = max(finalMinFare, (base * multiplier + (dist * perKm * multiplier) + (dur * perMin)) * peak),
                    pickupEtaMin = 5,
                    icon = internal,
                    isAvailableInRegion = true,
                    availabilityStatus = "AVAILABLE"
                ))
            }
            
            call.respond(estimates)
        }

        route("/orders") {
            post("/create") {
                try {
                    val req = call.receive<OrderCreateRequest>()
                    println("DEBUG: New Order Request from Customer ${req.customerId}: Pickup=(${req.pickupLat}, ${req.pickupLng}), Type=${req.requestedVehicleType}")
                    val pin = (1000..9999).random().toString()
                    
                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        conn.autoCommit = false
                        try {
                            val isScheduled = !req.scheduledTime.isNullOrBlank()
                            val initialStatus = if (isScheduled) "SCHEDULED" else "PENDING"
                            
                            // 1. Create Order
                            val orderSql = "INSERT INTO orders (customer_id, total_amount, status, pickup_location, dropoff_location, verification_pin, scheduled_time) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id"
                            val orderStmt = conn.prepareStatement(orderSql)
                            orderStmt.setInt(1, req.customerId.toInt())
                            orderStmt.setDouble(2, req.estimatedFare)
                            orderStmt.setString(3, initialStatus)
                            orderStmt.setString(4, req.pickupLocation)
                            orderStmt.setString(5, req.dropOffLocation)
                            orderStmt.setString(6, pin)
                            
                            if (isScheduled) {
                                try {
                                    val formatted = req.scheduledTime!!.replace("T", " ")
                                    val finalTime = if (formatted.length == 16) "$formatted:00" else formatted
                                    orderStmt.setTimestamp(7, java.sql.Timestamp.valueOf(finalTime))
                                } catch (e: Exception) {
                                    orderStmt.setNull(7, java.sql.Types.TIMESTAMP)
                                }
                            } else {
                                orderStmt.setNull(7, java.sql.Types.TIMESTAMP)
                            }
                            
                            val rs = orderStmt.executeQuery()
                            if (rs.next()) {
                                val orderId = rs.getInt(1)
                                
                                // 2. Create Delivery
                                val config = getPricingConfigFromDb()
                                val commissionPercent = config.driverCommissionPercent
                                val driverEarnings = req.estimatedFare * (1.0 - (commissionPercent / 100.0))

                                val deliverySql = "INSERT INTO deliveries (order_id, pickup_location, dropoff_location, status, service_type, estimated_earnings, pickup_lat, pickup_lng, dropoff_lat, dropoff_lng, distance_km) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                                val delStmt = conn.prepareStatement(deliverySql)
                                delStmt.setInt(1, orderId)
                                delStmt.setString(2, req.pickupLocation)
                                delStmt.setString(3, req.dropOffLocation)
                                delStmt.setString(4, initialStatus)
                                delStmt.setString(5, req.requestedVehicleType ?: req.serviceType.name)
                                delStmt.setDouble(6, driverEarnings) 
                                delStmt.setDouble(7, req.pickupLat)
                                delStmt.setDouble(8, req.pickupLng)
                                delStmt.setDouble(9, req.dropOffLat)
                                delStmt.setDouble(10, req.dropOffLng)
                                delStmt.setDouble(11, req.distanceKm)
                                
                                val rsDel = delStmt.executeQuery()
                                if (rsDel.next()) {
                                    val deliveryId = rsDel.getInt(1)
                                    conn.commit()

                                    // 3. Notify Nearby Drivers (Only if NOT scheduled)
                                    if (!isScheduled) {
                                        application.launch {
                                            var attempt = 1
                                            var currentRadius = 3.0 // Start small
                                            val maxAttempts = 3
                                            
                                            while (attempt <= maxAttempts) {
                                                println("DEBUG: Dispatch attempt $attempt for order $orderId (radius: ${currentRadius}km)")
                                                val nearby = getNearbyDriversFromDb(req.pickupLat, req.pickupLng, currentRadius, req.requestedVehicleType)
                                                
                                                val deliveryObj = Delivery(
                                                    id = deliveryId.toString(),
                                                    orderId = orderId,
                                                    driverId = null,
                                                    pickupLocation = req.pickupLocation,
                                                    dropOffLocation = req.dropOffLocation,
                                                    pickupLat = req.pickupLat,
                                                    pickupLng = req.pickupLng,
                                                    dropOffLat = req.dropOffLat,
                                                    dropOffLng = req.dropOffLng,
                                                    status = DeliveryStatus.PENDING,
                                                    distanceKm = req.distanceKm,
                                                    estimatedEarnings = driverEarnings,
                                                    pickupEtaMin = 5.0,
                                                    customerName = getCustomerName(req.customerId.toInt()) ?: "Customer",
                                                    customerPhone = getCustomerPhone(req.customerId.toInt()) ?: "",
                                                    totalFare = req.estimatedFare
                                                )

                                                // Try to lock and offer to drivers
                                                var offerSent = false
                                                for (driver in nearby) {
                                                    if (RedisManager.tryLockDriver(driver.id)) {
                                                        println("DEBUG: Offering order $orderId to DRIVER_${driver.id}")
                                                        sendToUser("DRIVER_${driver.id}", "NEW_DELIVERY", deliveryObj)
                                                        offerSent = true
                                                        // In a real system, we'd wait for acceptance or rejection here
                                                        // For this simulation, we send to the first available and wait
                                                        break 
                                                    }
                                                }

                                                if (offerSent) {
                                                    // Wait for driver response (15s timeout as in image)
                                                    delay(15000)
                                                    
                                                    // Check if order is still PENDING
                                                    val status = getOrderStatus(orderId)
                                                    if (status != "PENDING") {
                                                        println("DEBUG: Order $orderId no longer pending ($status). Dispatch finished.")
                                                        break
                                                    } else {
                                                        println("DEBUG: Order $orderId still pending after 15s. Retrying...")
                                                    }
                                                }

                                                attempt++
                                                currentRadius += 5.0 // Expand search
                                                sendToUser("CUSTOMER_${req.customerId}", "ORDER_RETRY", mapOf(
                                                    "type" to "ORDER_RETRY",
                                                    "attempt" to attempt - 1,
                                                    "maxAttempts" to maxAttempts,
                                                    "radius" to currentRadius,
                                                    "message" to "Expanding search area..."
                                                ))
                                            }
                                            
                                            if (getOrderStatus(orderId) == "PENDING" && attempt > maxAttempts) {
                                                println("DEBUG: Order $orderId timed out after $maxAttempts attempts.")
                                                sendToUser("CUSTOMER_${req.customerId}", "ORDER_TIMEOUT", mapOf("orderId" to orderId))
                                            }
                                        }
                                    }

                                    call.respond(mapOf("success" to true, "orderId" to orderId, "status" to initialStatus))
                                } else {
                                    conn.rollback()
                                    call.respond(mapOf("success" to false, "message" to "Failed to create delivery record"))
                                }
                            } else {
                                conn.rollback()
                                call.respond(mapOf("success" to false, "message" to "Failed to create order record"))
                            }
                        } catch (e: Exception) {
                            conn.rollback()
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    application.log.error("Order creation failed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
                }
            }

            get("/status/{orderId}") {
                val orderId = call.parameters["orderId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = """
                        SELECT o.status, o.verification_pin, o.total_amount, d.id as delivery_id, d.driver_id, dr.full_name, dr.phone, dr.vehicle_type, 
                               dr.vehicle_model, dr.vehicle_number, dr.profile_picture, dr.rating, 
                               ds.latitude, ds.longitude, ds.bearing
                        FROM orders o
                        LEFT JOIN deliveries d ON o.id = d.order_id
                        LEFT JOIN drivers dr ON d.driver_id = dr.id
                        LEFT JOIN driver_stats ds ON dr.id = ds.driver_id
                        WHERE o.id = ?
                    """.trimIndent()
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, orderId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        call.respond(OrderStatusResponse(
                            success = true,
                            status = rs.getString("status"),
                            driverId = rs.getString("driver_id"),
                            driverName = rs.getString("full_name"),
                            driverPhone = rs.getString("phone"),
                            driverVehicle = rs.getString("vehicle_type"),
                            driverVehicleModel = rs.getString("vehicle_model"),
                            driverVehicleNumber = rs.getString("vehicle_number"),
                            driverProfilePic = rs.getString("profile_picture"),
                            driverRating = rs.getDouble("rating"),
                            driverLat = rs.getDouble("latitude"),
                            driverLng = rs.getDouble("longitude"),
                            driverBearing = rs.getFloat("bearing"),
                            verificationPin = rs.getString("verification_pin"),
                            deliveryId = rs.getString("delivery_id"),
                            fare = rs.getDouble("total_amount")
                        ))
                    } else {
                        call.respond(mapOf("success" to false, "message" to "Order not found"))
                    }
                }
            }

            post("/cancel/{orderId}") {
                val orderId = call.parameters["orderId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val driverId = getDriverIdForOrder(orderId)
                
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?").apply { setInt(1, orderId); executeUpdate() }
                    conn.prepareStatement("UPDATE deliveries SET status = 'CANCELLED' WHERE order_id = ?").apply { setInt(1, orderId); executeUpdate() }
                }
                
                if (driverId != null) {
                    println("DEBUG: Notifying DRIVER_$driverId that Order $orderId was cancelled")
                    sendToUser("DRIVER_$driverId", "ORDER_CANCELLED", mapOf("orderId" to orderId))
                }

                call.respond(mapOf("success" to true))
            }

            post("/verify-pin") {
                val p = call.receiveParameters()
                val orderId = p["orderId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val pin = p["pin"] ?: ""
                
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT verification_pin FROM orders WHERE id = ?").apply { setInt(1, orderId) }.executeQuery()
                    if (rs.next() && rs.getString(1) == pin) {
                        conn.prepareStatement("UPDATE orders SET status = 'IN_PROGRESS' WHERE id = ?").apply { setInt(1, orderId); executeUpdate() }
                        conn.prepareStatement("UPDATE deliveries SET status = 'IN_PROGRESS' WHERE order_id = ?").apply { setInt(1, orderId); executeUpdate() }
                        call.respond(mapOf("success" to true))
                    } else {
                        call.respond(mapOf("success" to false, "message" to "Invalid PIN"))
                    }
                }
            }
        }

        post("/paystack-webhook") {
            // Deprecated - using /paystack/webhook instead
            call.respond(HttpStatusCode.MovedPermanently)
        }

        route("/fleet") {
            get("/vehicles") {
                val ownerId = call.parameters["owner_id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val vehicles = getFleetVehiclesFromDb(ownerId)
                call.respond(vehicles)
            }
            post("/add-vehicle") {
                val p = call.receiveParameters()
                val ownerId = p["owner_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                addVehicleToFleetInDb(
                    ownerId = ownerId,
                    name = p["name"] ?: "",
                    model = p["model"] ?: "",
                    type = p["type"] ?: "",
                    number = p["number"] ?: "",
                    rate = p["rate"]?.toDoubleOrNull() ?: 0.0,
                    description = p["description"],
                    features = p["features"],
                    imageUrls = p["image_urls"],
                    location = p["location"],
                    lat = p["lat"]?.toDoubleOrNull(),
                    lng = p["lng"]?.toDoubleOrNull(),
                    seats = p["seats"]?.toIntOrNull(),
                    transmission = p["transmission"],
                    fuelType = p["fuel_type"]
                )
                call.respond(mapOf("success" to true))
            }
            post("/update-vehicle") {
                val p = call.receiveParameters()
                val vehicleId = p["vehicle_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                updateFleetVehicleInDb(
                    id = vehicleId,
                    name = p["name"] ?: "",
                    model = p["model"] ?: "",
                    type = p["type"] ?: "",
                    number = p["number"] ?: "",
                    rate = p["rate"]?.toDoubleOrNull() ?: 0.0,
                    description = p["description"],
                    features = p["features"],
                    imageUrls = p["image_urls"],
                    status = p["status"],
                    seats = p["seats"]?.toIntOrNull(),
                    transmission = p["transmission"],
                    fuelType = p["fuel_type"]
                )
                call.respond(mapOf("success" to true))
            }
        }

        route("/rentals") {
            get("/vehicles") {
                println("API: Fetching all fleet vehicles for catalog...")
                val vehicles = getAllFleetVehiclesFromDb()
                call.respond(vehicles)
            }
            get("/rates") {
                call.respond(getRentalRatesFromDb())
            }
            post("/book") {
                try {
                    val req = call.receive<RentalBookRequest>()
                    application.log.info("Processing rental booking: customer=${req.customerId}, vehicle=${req.vehicleId}, total=${req.totalPrice}")
                    
                    val bookingCode = (1000..9999).random().toString()
                    
                    val config = getPricingConfigFromDb()
                    val guestFeePercent = config.rentalCustomerServiceFeePercent
                    val ownerCommPercent = config.rentalOwnerCommissionPercent
                    
                    val days = max(1, req.durationHours / 24)
                    // totalPrice = (days * dailyRate) + (dailyRate * guestFeePercent / 100)
                    // totalPrice = dailyRate * (days + guestFeePercent / 100)
                    val dailyRate = req.totalPrice / (days + (guestFeePercent / 100.0))
                    
                    val basePrice = days * dailyRate
                    val guestFee = req.totalPrice - basePrice
                    val ownerCommAmount = basePrice * (ownerCommPercent / 100.0)
                    val ownerEarnings = basePrice - ownerCommAmount

                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        val sql = """
                            INSERT INTO rentals (
                                customer_id, vehicle_id, pickup_location, pickup_lat, pickup_lng, 
                                duration_hours, total_price, base_price, customer_service_fee, 
                                owner_commission_amount, owner_earnings, start_time, trip_notes, 
                                stops, booking_code, is_self_drive, status, payment_status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 'PENDING') RETURNING id
                        """.trimIndent()
                        
                        val stmt = conn.prepareStatement(sql)
                        stmt.setInt(1, req.customerId)
                        stmt.setInt(2, req.vehicleId)
                        stmt.setString(3, req.pickupLocation)
                        stmt.setDouble(4, req.pickupLat)
                        stmt.setDouble(5, req.pickupLng)
                        stmt.setInt(6, req.durationHours)
                        stmt.setDouble(7, req.totalPrice)
                        stmt.setDouble(8, basePrice)
                        stmt.setDouble(9, guestFee)
                        stmt.setDouble(10, ownerCommAmount)
                        stmt.setDouble(11, ownerEarnings)
                        
                        // Safe Timestamp Parsing
                        val startTimeStr = req.startTime
                        if (!startTimeStr.isNullOrBlank() && startTimeStr != "Select Date") {
                            try {
                                val formatted = if (startTimeStr.length == 10) "$startTimeStr 00:00:00" else startTimeStr.replace("T", " ")
                                stmt.setTimestamp(12, java.sql.Timestamp.valueOf(formatted))
                            } catch (e: Exception) {
                                application.log.warn("Invalid start_time format: $startTimeStr")
                                stmt.setNull(12, java.sql.Types.TIMESTAMP)
                            }
                        } else {
                            stmt.setNull(12, java.sql.Types.TIMESTAMP)
                        }
                        
                        stmt.setString(13, req.tripNotes ?: "")
                        stmt.setString(14, req.stops ?: "")
                        stmt.setString(15, bookingCode)
                        stmt.setBoolean(16, req.isSelfDrive)
                        
                        val rs = stmt.executeQuery()
                        if (rs.next()) {
                            val rentalId = rs.getInt(1)

                            // Notify Owner
                            try {
                                val ownerRs = conn.prepareStatement("SELECT owner_id, name FROM rental_vehicles WHERE id = ?").apply { setInt(1, req.vehicleId) }.executeQuery()
                                if (ownerRs.next()) {
                                    val ownerId = ownerRs.getInt(1)
                                    val vName = ownerRs.getString(2)
                                    val token = getUserFcmToken(ownerId, "driver")
                                    if (token != null) PushNotificationHelper.sendNotification(token, "New Rental Booking", "Your vehicle $vName has been booked.")
                                }
                            } catch (_: Exception) {}

                            val customerEmail = getCustomerEmail(req.customerId) ?: "customer_${req.customerId}@example.com"
                            val reference = "RENTAL_${rentalId}_${System.currentTimeMillis()}"
                            
                            val paystackData = try { 
                                initializePaystackPayment(customerEmail, req.totalPrice, reference)
                            } catch (e: Exception) {
                                application.log.error("Paystack initialization failed", e)
                                null
                            }
                            
                            call.respond(mapOf(
                                "success" to true, 
                                "rentalId" to rentalId, 
                                "bookingCode" to bookingCode,
                                "checkoutUrl" to paystackData?.first,
                                "accessCode" to paystackData?.second
                            ))
                        } else {
                            call.respond(mapOf("success" to false, "message" to "No ID returned from database"))
                        }
                    }
                } catch (e: Throwable) {
                    application.log.error("CRITICAL: Rental booking failed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "success" to false, 
                        "message" to "Server error: ${e.localizedMessage ?: "Unknown error"}"
                    ))
                }
            }
            get("/active/{customerId}") {
                val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "SELECT * FROM rentals WHERE customer_id = ? AND status IN ('PENDING', 'ASSIGNED', 'ACTIVE', 'IN_PROGRESS') ORDER BY id DESC LIMIT 1"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        call.respond(mapOf(
                            "success" to true,
                            "rental" to mapOf(
                                "id" to rs.getInt("id"),
                                "pickup_location" to rs.getString("pickup_location"),
                                "destination_location" to (rs.getString("destination_location") ?: ""),
                                "pickup_lat" to rs.getDouble("pickup_lat"),
                                "pickup_lng" to rs.getDouble("pickup_lng"),
                                "destination_lat" to rs.getDouble("destination_lat"),
                                "destination_lng" to rs.getDouble("destination_lng"),
                                "stops" to (rs.getString("stops") ?: ""),
                                "booking_code" to rs.getString("booking_code"),
                                "status" to rs.getString("status"),
                                "is_unlocked" to rs.getBoolean("is_unlocked"),
                                "is_self_drive" to rs.getBoolean("is_self_drive"),
                                "vehicle_id" to rs.getInt("vehicle_id")
                            )
                        ))
                    } else {
                        call.respond(mapOf("success" to false))
                    }
                }
            }
            post("/cancel/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                updateRentalStatusInDb(id, "CANCELLED")
                call.respond(mapOf("success" to true))
            }
            post("/end/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                updateRentalStatusInDb(id, "COMPLETED")
                notifyRentalEnded(id, isManual = true)
                call.respond(mapOf("success" to true))
            }
            post("/update-location") {
                val p = call.receiveParameters()
                val vehicleId = p["vehicleId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val lat = p["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = p["lng"]?.toDoubleOrNull() ?: 0.0
                updateRentalVehicleLocationInDb(vehicleId, lat, lng)
                call.respond(mapOf("success" to true))
            }
            post("/update-status") {
                val p = call.receiveParameters()
                val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val status = p["status"] ?: ""
                updateRentalStatusInDb(id, status)
                call.respond(mapOf("success" to true))
            }
            post("/update-destination") {
                val p = call.receiveParameters()
                val rentalId = p["rentalId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val location = p["location"] ?: ""
                val lat = p["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = p["lng"]?.toDoubleOrNull() ?: 0.0
                val stops = p["stops"]
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "UPDATE rentals SET destination_location = ?, destination_lat = ?, destination_lng = ?, stops = ? WHERE id = ?"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, location)
                    stmt.setDouble(2, lat)
                    stmt.setDouble(3, lng)
                    stmt.setString(4, stops)
                    stmt.setInt(5, rentalId)
                    stmt.executeUpdate()
                }
                call.respond(mapOf("success" to true))
            }

            get("/history/{customerId}") {
                val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val list = mutableListOf<Map<String, Any>>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "SELECT r.*, v.name as vehicle_name FROM rentals r JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE r.customer_id = ? ORDER BY r.id DESC"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, customerId)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list.add(mapOf(
                            "id" to rs.getInt("id"),
                            "vehicle_name" to rs.getString("vehicle_name"),
                            "status" to rs.getString("status"),
                            "total_price" to rs.getDouble("total_price"),
                            "booking_code" to (rs.getString("booking_code") ?: ""),
                            "pickup_location" to rs.getString("pickup_location"),
                            "destination_location" to (rs.getString("destination_location") ?: ""),
                            "pickup_lat" to rs.getDouble("pickup_lat"),
                            "pickup_lng" to rs.getDouble("pickup_lng"),
                            "destination_lat" to rs.getDouble("destination_lat"),
                            "destination_lng" to rs.getDouble("destination_lng"),
                            "stops" to (rs.getString("stops") ?: ""),
                            "trip_notes" to (rs.getString("trip_notes") ?: ""),
                            "start_time" to (rs.getString("start_time") ?: ""),
                            "created_at" to (rs.getString("created_at") ?: ""),
                            "is_self_drive" to rs.getBoolean("is_self_drive"),
                            "vehicle_id" to rs.getInt("vehicle_id")
                        ))
                    }
                }
                call.respond(list)
            }
        }

        get("/customer/rentals/{customerId}") {
            val customerId = call.parameters["customerId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val list = mutableListOf<Map<String, Any>>()
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "SELECT r.*, v.name as vehicle_name FROM rentals r JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE r.customer_id = ? ORDER BY r.id DESC"
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, customerId.toInt())
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(mapOf(
                        "id" to rs.getInt("id"),
                        "vehicle_name" to rs.getString("vehicle_name"),
                        "status" to rs.getString("status"),
                        "total_price" to rs.getDouble("total_price"),
                        "booking_code" to (rs.getString("booking_code") ?: ""),
                        "pickup_location" to rs.getString("pickup_location"),
                        "destination_location" to (rs.getString("destination_location") ?: ""),
                        "pickup_lat" to rs.getDouble("pickup_lat"),
                        "pickup_lng" to rs.getDouble("pickup_lng"),
                        "destination_lat" to rs.getDouble("destination_lat"),
                        "destination_lng" to rs.getDouble("destination_lng"),
                        "stops" to (rs.getString("stops") ?: ""),
                        "trip_notes" to (rs.getString("trip_notes") ?: ""),
                        "start_time" to (rs.getString("start_time") ?: ""),
                        "is_self_drive" to rs.getBoolean("is_self_drive"),
                        "vehicle_id" to rs.getInt("vehicle_id"),
                        "created_at" to (rs.getString("created_at") ?: "")
                    ))
                }
            }
            call.respond(list)
        }

        route("/driver") {
            post("/update-vehicle") {
                val p = call.receiveParameters()
                val id = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                updateDriverVehicleInDb(
                    id = id,
                    type = p["vehicle_type"] ?: "",
                    number = p["vehicle_number"] ?: "",
                    model = p["vehicle_model"] ?: "",
                    service = p["service_types"] ?: "BOTH"
                )
                call.respond(mapOf("success" to true))
            }

            post("/submit-rating") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val _orderId = p["order_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val rating = p["rating"]?.toFloatOrNull() ?: 5.0f
                val _comment = p["comment"] ?: ""

                DatabaseInitializer.getDataSource().connection.use { conn ->
                    // 1. Update driver's average rating
                    val sql = "UPDATE drivers SET rating = (rating + ?) / 2.0 WHERE id = ?"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setDouble(1, rating.toDouble())
                    stmt.setInt(2, driverId)
                    stmt.executeUpdate()
                    
                    // 2. We could also store it in a ratings table for history
                    // println("Rating for order $_orderId: $rating - $_comment")
                }
                call.respond(AuthResponse(true, "Rating submitted", null, null))
            }

            get("/nearby") {
                val lat = call.parameters["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = call.parameters["lng"]?.toDoubleOrNull() ?: 0.0
                call.respond(getNearbyDriversFromDb(lat, lng, call.parameters["radius"]?.toDoubleOrNull() ?: 5.0))
            }
            post("/update-online-status") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: ""
                val online = p["is_online"]?.toBoolean() ?: false
                
                val error = updateDriverOnlineStatusInDb(driverId, online)
                if (error != null) {
                    call.respond(mapOf("success" to false, "message" to error))
                } else {
                    call.respond(mapOf("success" to true))
                }
            }
            post("/update-location") {
                val p = call.receiveParameters()
                val driverIdStr = p["driver_id"] ?: ""
                val lat = p["latitude"]?.toDoubleOrNull() ?: 0.0
                val lng = p["longitude"]?.toDoubleOrNull() ?: 0.0
                val bearing = p["bearing"]?.toFloatOrNull() ?: 0f
                
                if (lat != 0.0) {
                    println("DEBUG: Received location update for Driver $driverIdStr: $lat, $lng")
                }
                updateDriverLocationInDb(driverIdStr, lat, lng, bearing)
                
                // Notify active customer if any
                val driverId = driverIdStr.toIntOrNull()
                if (driverId != null) {
                    val customerId = getActiveCustomerIdForDriver(driverId)
                    if (customerId != null) {
                        sendToUser("CUSTOMER_$customerId", "DRIVER_LOCATION_UPDATE", mapOf(
                            "driverId" to driverId,
                            "lat" to lat,
                            "lng" to lng,
                            "bearing" to bearing
                        ))
                    }
                }

                call.respond(mapOf("success" to true))
            }
            get("/available-deliveries") {
                val lat = call.parameters["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = call.parameters["lng"]?.toDoubleOrNull() ?: 0.0
                val vehicleType = call.parameters["vehicle_type"] ?: "car"
                val vehicleCategory = call.parameters["vehicle_category"]
                
                // Tiered expansion search logic (Industry standard)
                var radius = 3.0 // Start with 3km
                var deliveries = getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                
                if (deliveries.isEmpty()) {
                    radius = 7.0 // Expand to 7km
                    deliveries = getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }
                
                if (deliveries.isEmpty()) {
                    radius = 15.0 // Expand to 15km
                    deliveries = getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }

                if (deliveries.isEmpty()) {
                    radius = 50.0 // Max expansion to 50km for remote areas
                    deliveries = getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }
                
                call.respond(deliveries)
            }
            post("/pay-daily-fee") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                
                val config = getPricingConfigFromDb()
                val amount = config.dailyServiceFee
                
                val settings = getSystemSettingsFromDb()
                val mode = settings["paystack_mode"] as? String ?: "TEST"

                if (mode == "TEST") {
                    println("DEBUG: Auto-approving daily fee in TEST mode for driver $driverId")
                    val reference = "DAILY_FEE_TEST_${driverId}_${System.currentTimeMillis()}"
                    processDailyFeePayment(driverId, amount, reference)
                    call.respond(mapOf(
                        "success" to true,
                        "message" to "Approved automatically (Test Mode)",
                        "autoApproved" to true
                    ))
                } else {
                    val email = getDriverEmail(driverId) ?: "driver_$driverId@example.com"
                    val reference = "DAILY_FEE_${driverId}_${System.currentTimeMillis()}"
                    val paystackData = initializePaystackPayment(email, amount, reference)
                    
                    if (paystackData != null) {
                        call.respond(mapOf(
                            "success" to true,
                            "checkoutUrl" to paystackData.first,
                            "reference" to reference
                        ))
                    } else {
                        call.respond(mapOf("success" to false, "message" to "Could not initialize payment"))
                    }
                }
            }
            get("/status/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val status = getDriverStatusFromDb(id)
                if (status != null) call.respond(status) else call.respond(HttpStatusCode.NotFound)
            }
            get("/stats/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val stats = getDriverStatsFromDb(id)
                if (stats != null) call.respond(stats) else call.respond(HttpStatusCode.NotFound)
            }
            post("/upload-document") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val docType = p["doc_type"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val fileUrl = p["file_url"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                updateDriverDocumentInDb(driverId, docType, fileUrl)
                call.respond(AuthResponse(true, "Document updated successfully", null, null))
            }
            post("/update-emergency-contacts") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val c1 = p["contact1"] ?: ""
                val c2 = p["contact2"] ?: ""
                
                updateEmergencyContactsInDb(driverId, c1, c2)
                call.respond(AuthResponse(true, "Contacts updated successfully", null, null))
            }

            post("/accept-delivery") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val deliveryId = p["delivery_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    conn.autoCommit = false
                    try {
                        // 1. Check if delivery is still available
                        val rs = conn.prepareStatement("SELECT status, order_id FROM deliveries WHERE id = ? FOR UPDATE").apply { setInt(1, deliveryId) }.executeQuery()
                        if (rs.next() && rs.getString("status") == "PENDING") {
                            val orderId = rs.getInt("order_id")
                            
                            // 2. Assign Driver
                            conn.prepareStatement("UPDATE deliveries SET driver_id = ?, status = 'ASSIGNED' WHERE id = ?").apply {
                                setInt(1, driverId)
                                setInt(2, deliveryId)
                                executeUpdate()
                            }
                            
                            // 3. Update Order Status
                            conn.prepareStatement("UPDATE orders SET status = 'ASSIGNED' WHERE id = ?").apply {
                                setInt(1, orderId)
                                executeUpdate()
                            }
                            
                            conn.commit()
                            
                            // 4. Notify Customer
                            val customerId = getCustomerIdForOrder(orderId)
                            if (customerId != null) {
                                sendToUser("CUSTOMER_$customerId", "ORDER_ACCEPTED", mapOf("orderId" to orderId, "deliveryId" to deliveryId))
                            }
                            
                            call.respond(AuthResponse(true, "Delivery accepted successfully", null, null))
                        } else {
                            conn.rollback()
                            call.respond(AuthResponse(false, "Delivery no longer available", null, null))
                        }
                    } catch (e: Exception) {
                        conn.rollback()
                        call.respond(AuthResponse(false, "Error: ${e.localizedMessage}", null, null))
                    }
                }
            }

            get("/my-deliveries/{id}") {
                val driverId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val list = mutableListOf<Delivery>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = """
                        SELECT d.*, c.name as customer_name, c.phone as customer_phone, o.total_amount as total_fare
                        FROM deliveries d
                        JOIN orders o ON d.order_id = o.id
                        JOIN customers c ON o.customer_id = c.id
                        WHERE d.driver_id = ? AND d.status NOT IN ('DELIVERED', 'CANCELLED')
                    """.trimIndent()
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, driverId)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list.add(Delivery(
                            id = rs.getInt("id").toString(),
                            orderId = rs.getInt("order_id"),
                            driverId = rs.getInt("driver_id").toString(),
                            pickupLocation = rs.getString("pickup_location"),
                            dropOffLocation = rs.getString("dropoff_location"),
                            pickupLat = rs.getDouble("pickup_lat"),
                            pickupLng = rs.getDouble("pickup_lng"),
                            dropOffLat = rs.getDouble("dropoff_lat"),
                            dropOffLng = rs.getDouble("dropoff_lng"),
                            status = DeliveryStatus.valueOf(rs.getString("status").uppercase()),
                            distanceKm = rs.getDouble("distance_km"),
                            estimatedEarnings = rs.getDouble("estimated_earnings"),
                            pickupEtaMin = 5.0,
                            customerName = rs.getString("customer_name"),
                            customerPhone = rs.getString("customer_phone"),
                            totalFare = rs.getDouble("total_fare")
                        ))
                    }
                }
                call.respond(list)
            }

            route("/rentals") {
                get("/{driverId}") {
                    val driverId = call.parameters["driverId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val list = mutableListOf<Map<String, Any>>()
                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        val sql = "SELECT r.*, c.name as customer_name, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE r.driver_id = ? ORDER BY r.id DESC"
                        val stmt = conn.prepareStatement(sql)
                        stmt.setInt(1, driverId)
                        val rs = stmt.executeQuery()
                        while (rs.next()) {
                            list.add(mapOf(
                                "id" to rs.getInt("id"),
                                "customer_name" to rs.getString("customer_name"),
                                "vehicle_name" to rs.getString("vehicle_name"),
                                "status" to rs.getString("status"),
                                "total_price" to rs.getDouble("total_price"),
                                "booking_code" to (rs.getString("booking_code") ?: ""),
                                "pickup_location" to rs.getString("pickup_location"),
                                "start_time" to (rs.getString("start_time") ?: "")
                            ))
                        }
                    }
                    call.respond(list)
                }

                post("/verify") {
                    val p = call.receiveParameters()
                    val rentalId = p["rentalId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val code = p["code"] ?: ""
                    
                    DatabaseInitializer.getDataSource().connection.use { conn ->
                        val rs = conn.prepareStatement("SELECT booking_code FROM rentals WHERE id = ?").apply { setInt(1, rentalId) }.executeQuery()
                        if (rs.next() && rs.getString(1) == code) {
                            updateRentalStatusInDb(rentalId, "ACTIVE")
                            call.respond(mapOf("success" to true))
                        } else {
                            call.respond(mapOf("success" to false, "message" to "Invalid code"))
                        }
                    }
                }
            }
        }

        route("/delivery") {
            post("/update-status") {
                val p = call.receiveParameters()
                val deliveryId = p["delivery_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val status = p["status"] ?: ""
                
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT order_id, driver_id, estimated_earnings FROM deliveries WHERE id = ?").apply { setInt(1, deliveryId) }.executeQuery()
                    if (rs.next()) {
                        val orderId = rs.getInt(1)
                        val driverId = rs.getInt(2)
                        val earnings = rs.getDouble(3)
                        
                        conn.prepareStatement("UPDATE deliveries SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, deliveryId); executeUpdate() }
                        conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, orderId); executeUpdate() }
                        
                        if (status == "DELIVERED" && driverId != 0) {
                            // Update driver stats with daily reset logic for earnings_today
                            val statsSql = """
                                INSERT INTO driver_stats (driver_id, earnings_today, total_earnings, completed_today, total_deliveries)
                                VALUES (?, ?, ?, 1, 1)
                                ON CONFLICT (driver_id) DO UPDATE SET
                                    earnings_today = CASE 
                                        WHEN driver_stats.updated_at::date = CURRENT_DATE THEN driver_stats.earnings_today + EXCLUDED.earnings_today 
                                        ELSE EXCLUDED.earnings_today 
                                    END,
                                    total_earnings = driver_stats.total_earnings + EXCLUDED.total_earnings,
                                    completed_today = CASE 
                                        WHEN driver_stats.updated_at::date = CURRENT_DATE THEN driver_stats.completed_today + 1 
                                        ELSE 1 
                                    END,
                                    total_deliveries = driver_stats.total_deliveries + 1,
                                    updated_at = CURRENT_TIMESTAMP
                            """.trimIndent()
                            conn.prepareStatement(statsSql).apply {
                                setInt(1, driverId)
                                setDouble(2, earnings)
                                setDouble(3, earnings)
                                executeUpdate()
                            }
                        }
                        
                        // Notify Customer
                        val customerId = getCustomerIdForOrder(orderId)
                        if (customerId != null) {
                            sendToUser("CUSTOMER_$customerId", "ORDER_STATUS_UPDATE", mapOf("orderId" to orderId, "status" to status))
                        }
                    }
                }
                call.respond(AuthResponse(true, "Status updated", null, null))
            }
        }

        // --- WEBHOOKS ---
        post("/paystack/webhook") {
            try {
                val body = call.receiveText()
                val signature = call.request.headers["x-paystack-signature"]
                
                if (signature == null || !verifyPaystackSignature(body, signature)) {
                    application.log.warn("Invalid Paystack Signature received")
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid signature")
                }

                val json = JsonParser.parseString(body).asJsonObject
                val event = json.get("event").asString
                val data = json.getAsJsonObject("data")
                
                if (event == "charge.success") {
                    val reference = data.get("reference")?.asString ?: ""
                    val amount = data.get("amount").asDouble / 100.0
                    
                    if (reference.startsWith("RENTAL_")) {
                        val rentalId = reference.split("_")[1].toInt()
                        println("DEBUG: Rental Payment Success for ID $rentalId")
                        updateRentalStatusInDb(rentalId, "BOOKED")
                        
                        // Record Payment
                        recordPaymentInDb(
                            userId = getCustomerIdForRental(rentalId) ?: 0,
                            userType = "CUSTOMER",
                            amount = amount,
                            type = "RENTAL_BOOKING",
                            reference = reference
                        )
                    } else if (reference.startsWith("DAILY_FEE_")) {
                        val driverId = reference.split("_")[2].toInt()
                        processDailyFeePayment(driverId, amount, reference)
                    } else {
                        // Check metadata for driver daily fee (legacy/momo)
                        val metadata = data.getAsJsonObject("metadata")
                        val driverId = metadata?.get("driver_id")?.asInt
                        
                        if (driverId != null) {
                            processDailyFeePayment(driverId, amount, reference)
                        }
                    }
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                println("Webhook Error: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        route("/wallet") {
            get("/balance/{driverId}") {
                val driverId = call.parameters["driverId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT total_earnings FROM driver_stats WHERE driver_id = ?").apply { setInt(1, driverId.toInt()) }.executeQuery()
                    if (rs.next()) call.respond(mapOf("balance" to rs.getDouble(1)))
                    else call.respond(mapOf("balance" to 0.0))
                }
            }

            get("/transactions/{driverId}") {
                val driverId = call.parameters["driverId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val list = mutableListOf<Map<String, Any>>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT * FROM payments WHERE user_id = ? AND user_type = 'DRIVER' ORDER BY created_at DESC").apply { setInt(1, driverId.toInt()) }.executeQuery()
                    while (rs.next()) {
                        list.add(mapOf(
                            "amount" to rs.getDouble("amount"),
                            "type" to rs.getString("payment_type"),
                            "reference" to rs.getString("reference"),
                            "status" to rs.getString("status"),
                            "created_at" to rs.getTimestamp("created_at").toString()
                        ))
                    }
                }
                call.respond(list)
            }

            post("/request-topup") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val amount = p["amount"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reference = p["reference"] ?: ""
                val type = if (reference.startsWith("DF_")) "DAILY_FEE" else "TOPUP"

                var topupId: Int? = null
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "INSERT INTO wallet_topups (driver_id, amount, reference_code, payment_type, status) VALUES (?, ?, ?, ?, 'PENDING') RETURNING id"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, driverId)
                    stmt.setDouble(2, amount.toDouble())
                    stmt.setString(3, reference)
                    stmt.setString(4, type)
                    val rs = stmt.executeQuery()
                    if (rs.next()) topupId = rs.getInt(1)
                }
                
                val settings = getSystemSettingsFromDb()
                val mode = settings["paystack_mode"] as? String ?: "TEST"
                
                if (mode == "TEST" && type == "DAILY_FEE" && topupId != null) {
                    approvePaymentInDb(topupId)
                    call.respond(AuthResponse(true, "Approved automatically (Test Mode)", null, null))
                } else {
                    call.respond(AuthResponse(true, "Request submitted for approval", null, null))
                }
            }

            get("/topup-history/{driverId}") {
                val driverId = call.parameters["driverId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val list = mutableListOf<Map<String, Any>>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT * FROM wallet_topups WHERE driver_id = ? ORDER BY created_at DESC").apply { setInt(1, driverId.toInt()) }.executeQuery()
                    while (rs.next()) {
                        list.add(mapOf(
                            "id" to rs.getInt("id"),
                            "amount" to rs.getDouble("amount"),
                            "reference" to rs.getString("reference_code"),
                            "status" to rs.getString("status"),
                            "payment_type" to (rs.getString("payment_type") ?: "TOPUP"),
                            "created_at" to rs.getTimestamp("created_at").toString()
                        ))
                    }
                }
                call.respond(list)
            }
        }

        route("/safety") {
            post("/sos") {
                val req = call.receive<SOSRequest>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val rs = conn.prepareStatement("SELECT full_name, phone, vehicle_number, vehicle_model FROM drivers WHERE id = ?").apply { setInt(1, req.driverId.toInt()) }.executeQuery()
                    if (rs.next()) {
                        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                        val alert = SOSAlert(
                            id = id,
                            driverId = req.driverId,
                            driverName = rs.getString(1),
                            driverPhone = rs.getString(2),
                            lat = req.latitude,
                            lng = req.longitude,
                            time = System.currentTimeMillis(),
                            type = req.type,
                            plateNumber = rs.getString(3),
                            vehicleModel = rs.getString(4)
                        )
                        RedisManager.addSOS(alert)
                        sosFlow.tryEmit(alert)
                        call.respond(AuthResponse(true, "SOS Triggered", null, null))
                    } else {
                        call.respond(AuthResponse(false, "Driver not found", null, null))
                    }
                }
            }

            get("/share-trip/{driverId}/{deliveryId}") {
                val driverId = call.parameters["driverId"] ?: "0"
                val deliveryId = call.parameters["deliveryId"] ?: "0"
                val shareUrl = "https://fameko-backend-production.up.railway.app/track/$driverId/$deliveryId"
                call.respond(ShareTripResponse(shareUrl, deliveryId, System.currentTimeMillis() + 3600000))
            }
        }

        get("/track/{driverId}/{deliveryId}") {
            val driverId = call.parameters["driverId"] ?: "0"
            val deliveryId = call.parameters["deliveryId"] ?: "0"
            
            // Clean up numeric IDs (e.g., 8.0 -> 8)
            val cleanDriverId = driverId.toDoubleOrNull()?.toInt()?.toString() ?: driverId
            val cleanDeliveryId = deliveryId.toDoubleOrNull()?.toInt()?.toString() ?: deliveryId
            
            call.respond(ThymeleafContent("track", mapOf(
                "driverId" to cleanDriverId,
                "deliveryId" to cleanDeliveryId
            )))
        }

        post("/update-fcm-token") {
            val p = call.receiveParameters()
            val userId = p["userId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val token = p["token"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val type = p["type"] ?: "driver"
            val table = if (type == "customer") "customers" else "drivers"
            DatabaseInitializer.getDataSource().connection.use { conn ->
                conn.prepareStatement("UPDATE $table SET fcm_token = ? WHERE id = ?").apply {
                    setString(1, token); setInt(2, userId); executeUpdate()
                }
            }
            call.respond(mapOf("success" to true))
        }

        route("/chat") {
            post("/send") {
                val msg = call.receive<Message>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "INSERT INTO chat_messages (conversation_id, sender_type, sender_id, body) VALUES (?, ?, ?, ?) RETURNING id, created_at"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, msg.conversationId)
                    stmt.setString(2, msg.senderType)
                    stmt.setInt(3, msg.senderId)
                    stmt.setString(4, msg.body)
                    
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val savedMsg = msg.copy(
                            id = rs.getInt(1),
                            createdAt = rs.getTimestamp(2).toInstant().toString()
                        )
                        
                        // 1. Find recipient
                        val recipientId = if (savedMsg.senderType == "customer") {
                            getDriverIdForOrder(savedMsg.conversationId)?.let { "DRIVER_$it" }
                        } else {
                            getCustomerIdForOrder(savedMsg.conversationId)?.let { "CUSTOMER_$it" }
                        }
                        
                        // 2. Broadcast via WS and Push
                        if (recipientId != null) {
                            sendToUser(recipientId, "NEW_MESSAGE", savedMsg)
                        }
                        
                        call.respond(savedMsg)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }

            get("/history/{convId}") {
                val convId = call.parameters["convId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val list = mutableListOf<Message>()
                DatabaseInitializer.getDataSource().connection.use { conn ->
                    val sql = "SELECT * FROM chat_messages WHERE conversation_id = ? ORDER BY created_at ASC"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setInt(1, convId)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list.add(Message(
                            id = rs.getInt("id"),
                            conversationId = rs.getInt("conversation_id"),
                            senderType = rs.getString("sender_type"),
                            senderId = rs.getInt("sender_id"),
                            body = rs.getString("body"),
                            createdAt = rs.getTimestamp("created_at").toInstant().toString(),
                            read = rs.getBoolean("is_read")
                        ))
                    }
                }
                call.respond(list)
            }
        }
    }
}

// --- DATABASE HELPERS ---

private suspend fun processDailyFeePayment(driverId: Int, amount: Double, reference: String) {
    println("DEBUG: Daily Fee Payment Success for Driver $driverId")
    
    val settings = getSystemSettingsFromDb()
    val mode = settings["paystack_mode"] as? String ?: "TEST"
    
    if (mode == "TEST") {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE drivers SET daily_fee_paid_at = CURRENT_DATE, daily_fee_expires_at = CURRENT_TIMESTAMP + interval '24 hours' WHERE id = ?"
            conn.prepareStatement(sql).apply {
                setInt(1, driverId)
                executeUpdate()
            }
        }

        // Automatically set driver to ONLINE
        updateDriverOnlineStatusInDb(driverId.toString(), true)

        // Record Payment
        recordPaymentInDb(
            userId = driverId,
            userType = "DRIVER",
            amount = amount,
            type = "DAILY_FEE",
            reference = reference
        )

        // Notify driver via WebSocket
        sendToUser("DRIVER_$driverId", "NOTIFICATION_RECEIVED", mapOf(
            "title" to "Payment Successful",
            "message" to "Daily fee received. You are now ONLINE automatically.",
            "type" to "DAILY_FEE_PAID",
            "isOnline" to true,
            "createdAt" to java.time.LocalDateTime.now().toString()
        ))
    } else {
        // LIVE Mode: Queue for Admin Approval
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO wallet_topups (driver_id, amount, reference_code, payment_type, status) VALUES (?, ?, ?, 'DAILY_FEE', 'PENDING')"
            conn.prepareStatement(sql).apply {
                setInt(1, driverId)
                setDouble(2, amount)
                setString(3, reference)
                executeUpdate()
            }
        }
        
        // Notify driver
        sendToUser("DRIVER_$driverId", "NOTIFICATION_RECEIVED", mapOf(
            "title" to "Payment Received",
            "message" to "Your daily fee payment of ₵$amount has been received and is awaiting admin approval.",
            "type" to "DAILY_FEE_PENDING",
            "createdAt" to java.time.LocalDateTime.now().toString()
        ))
    }
}

private fun loginCustomerInDbByPhone(phone: String): Map<String, Any>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "SELECT id, name FROM customers WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, phone.trim())
        // Handle cases where phone might be stored with or without + prefix
        val altPhone = if (phone.startsWith("+")) phone.substring(1) else "+$phone"
        stmt.setString(2, altPhone)
        val rs = stmt.executeQuery()
        if (rs.next()) return mapOf("id" to rs.getInt("id"), "name" to rs.getString("name"))
    }
    return null
}

private fun syncCustomerWithFirebase(email: String, firebaseUid: String): Map<String, Any>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val selectSql = "SELECT id, name, firebase_uid FROM customers WHERE email = ?"
        val selectStmt = conn.prepareStatement(selectSql)
        selectStmt.setString(1, email.lowercase())
        val rs = selectStmt.executeQuery()
        if (rs.next()) {
            val dbUid = rs.getString("firebase_uid")
            if (dbUid == null) {
                // Link existing account to this Firebase UID
                conn.prepareStatement("UPDATE customers SET firebase_uid = ? WHERE email = ?").apply {
                    setString(1, firebaseUid)
                    setString(2, email.lowercase())
                    executeUpdate()
                }
            }
            return mapOf("id" to rs.getInt("id"), "name" to rs.getString("name"))
        }
    }
    return null
}

private fun registerCustomerInDb(req: CustomerRegisterRequest): Int? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "INSERT INTO customers (name, email, phone, default_address, password, region, profile_picture, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, req.name.trim())
        stmt.setString(2, req.email.trim().lowercase())
        stmt.setString(3, req.phone.trim())
        stmt.setString(4, req.address.trim())
        stmt.setString(5, req.password.trim())
        stmt.setString(6, req.region ?: "")
        stmt.setString(7, req.profilePicture ?: "")
        stmt.setString(8, req.firebaseUid)
        val rs = stmt.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }
    return null
}

private fun getCustomerEmail(id: Int): String? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT email FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return rs.getString(1)
    }
    return null
}

private fun getCustomerIdForRental(rentalId: Int): Int? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT customer_id FROM rentals WHERE id = ?").apply { setInt(1, rentalId) }.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }
    return null
}

private fun recordPaymentInDb(userId: Int, userType: String, amount: Double, type: String, reference: String) {
    try {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO payments (user_id, user_type, amount, payment_type, reference, status) VALUES (?, ?, ?, ?, ?, 'SUCCESS') ON CONFLICT (reference) DO NOTHING"
            conn.prepareStatement(sql).apply {
                setInt(1, userId)
                setString(2, userType)
                setDouble(3, amount)
                setString(4, type)
                setString(5, reference)
                executeUpdate()
            }
        }
    } catch (e: Exception) {
        println("Error recording payment: ${e.message}")
    }
}

private fun getFinancialStatsFromDb(month: String?, date: String?): Map<String, Any> {
    val stats = mutableMapOf<String, Any>(
        "totalRevenue" to 0.0,
        "dailyFees" to 0.0,
        "rentalIncome" to 0.0,
        "totalDebt" to 0.0,
        "pendingPaymentsValue" to 0.0,
        "recentTransactions" to emptyList<Map<String, Any>>(),
        "topDebtors" to emptyList<Map<String, Any>>()
    )

    try {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            var filter = ""
            if (!date.isNullOrBlank()) {
                filter = " AND DATE(created_at) = '$date'"
            } else if (!month.isNullOrBlank()) {
                filter = " AND TO_CHAR(created_at, 'YYYY-MM') = '$month'"
            }

            // 1. Daily Fees
            val rsFees = conn.createStatement().executeQuery("SELECT SUM(amount) FROM payments WHERE payment_type = 'DAILY_FEE' $filter")
            if (rsFees.next()) stats["dailyFees"] = rsFees.getDouble(1)

            // 2. Rental Income (Platform Guest Fees)
            val rsRentals = conn.createStatement().executeQuery("SELECT SUM(customer_service_fee) FROM rentals WHERE payment_status = 'PAID' $filter")
            if (rsRentals.next()) stats["rentalIncome"] = rsRentals.getDouble(1)

            // 3. Ride Revenue
            val rsRides = conn.createStatement().executeQuery("SELECT SUM(total_amount) FROM orders WHERE status = 'DELIVERED' $filter")
            if (rsRides.next()) stats["totalRevenue"] = rsRides.getDouble(1)

            // 4. Total Outstanding Debt (Sum of pending payments)
            val rsDebt = conn.createStatement().executeQuery("SELECT SUM(amount) FROM wallet_topups WHERE status = 'PENDING'")
            if (rsDebt.next()) stats["pendingPaymentsValue"] = rsDebt.getDouble(1)
            stats["totalDebt"] = stats["pendingPaymentsValue"] as Double

            // 5. Recent Transactions
            val transactions = mutableListOf<Map<String, Any>>()
            val rsTrans = conn.createStatement().executeQuery("SELECT p.*, d.full_name as driver_name, c.name as customer_name FROM payments p LEFT JOIN drivers d ON (p.user_id = d.id AND p.user_type = 'DRIVER') LEFT JOIN customers c ON (p.user_id = c.id AND p.user_type = 'CUSTOMER') ORDER BY p.created_at DESC LIMIT 15")
            while (rsTrans.next()) {
                transactions.add(mapOf(
                    "driver" to (rsTrans.getString("driver_name") ?: rsTrans.getString("customer_name") ?: "System"),
                    "type" to "CREDIT",
                    "amount" to rsTrans.getDouble("amount"),
                    "desc" to (rsTrans.getString("payment_type") ?: "Payment"),
                    "date" to rsTrans.getString("created_at")
                ))
            }
            stats["recentTransactions"] = transactions

            // 6. Top Debtors (Drivers with low earnings/balance if we implemented debt)
            // For now, empty list or drivers with pending topups
            stats["topDebtors"] = emptyList<Map<String, Any>>()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return stats
}

private fun getDriverEmail(id: Int): String? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT email FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return rs.getString(1)
    }
    return null
}

private fun initializePaystackPayment(email: String, amountGHS: Double, reference: String): Pair<String?, String?>? {
    try {
        val settings = getSystemSettingsFromDb()
        val mode = settings["paystack_mode"] as? String ?: "TEST"
        val secret = if (mode == "LIVE") settings["paystack_live_secret"] as? String else settings["paystack_test_secret"] as? String
        
        if (secret.isNullOrBlank()) {
            println("Paystack Error: No secret key found for $mode mode")
            return null
        }

        val url = URL("https://api.paystack.co/transaction/initialize")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $secret")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val body = JsonObject().apply {
            addProperty("email", email)
            addProperty("amount", (amountGHS * 100).toInt()) // Pesewas
            addProperty("reference", reference)
            addProperty("callback_url", "https://fameko-backend-production.up.railway.app/payment-success")
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        
        if (conn.responseCode == 200) {
            val response = JsonParser.parseString(conn.inputStream.bufferedReader().readText()).asJsonObject
            val data = response.getAsJsonObject("data")
            return Pair(data.get("authorization_url").asString, data.get("access_code").asString)
        } else {
            println("Paystack Init Error: ${conn.responseCode} ${conn.errorStream?.bufferedReader()?.readText()}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private fun syncDriverWithFirebase(email: String, firebaseUid: String): Driver? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val selectSql = "SELECT * FROM drivers WHERE email = ?"
        val selectStmt = conn.prepareStatement(selectSql)
        selectStmt.setString(1, email.lowercase())
        val rs = selectStmt.executeQuery()
        if (rs.next()) {
            val dbUid = rs.getString("firebase_uid")
            if (dbUid == null) {
                conn.prepareStatement("UPDATE drivers SET firebase_uid = ? WHERE email = ?").apply {
                    setString(1, firebaseUid)
                    setString(2, email.lowercase())
                    executeUpdate()
                }
            }
            return Driver(
                id = rs.getInt("id"), fullName = rs.getString("full_name") ?: "", email = rs.getString("email") ?: "",
                phone = rs.getString("phone") ?: "", region = rs.getString("region") ?: "", licenseNumber = rs.getString("license_number") ?: "",
                vehicleType = rs.getString("vehicle_type"), vehicleNumber = rs.getString("vehicle_number"), status = rs.getString("status") ?: "PENDING",
                isOnline = rs.getBoolean("is_online"), rating = rs.getDouble("rating"),
                profilePicture = rs.getString("profile_picture"),
                userRole = rs.getString("user_role") ?: "DRIVER",
                companyName = rs.getString("company_name"),
                registrationNumber = rs.getString("registration_number")
            )
        }
    }
    return null
}

private fun loginDriverInDbByPhone(phone: String): Driver? {
    val cleanPhone = phone.trim()
    val altPhone = if (cleanPhone.startsWith("+")) cleanPhone.substring(1) else "+$cleanPhone"
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // 1. Try drivers table first
        val sql = "SELECT * FROM drivers WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, cleanPhone)
        stmt.setString(2, altPhone)
        val rs = stmt.executeQuery()
        if (rs.next()) return Driver(
            id = rs.getInt("id"), fullName = rs.getString("full_name") ?: "", email = rs.getString("email") ?: "",
            phone = rs.getString("phone") ?: "", region = rs.getString("region") ?: "", licenseNumber = rs.getString("license_number") ?: "",
            vehicleType = rs.getString("vehicle_type"), vehicleNumber = rs.getString("vehicle_number"), status = rs.getString("status") ?: "PENDING",
            isOnline = rs.getBoolean("is_online"), rating = rs.getDouble("rating"),
            serviceType = try { ServiceType.valueOf(rs.getString("service_types").uppercase()) } catch(_: Exception) { ServiceType.BOTH },
            profilePicture = rs.getString("profile_picture"),
            userRole = rs.getString("user_role") ?: "DRIVER",
            companyName = rs.getString("company_name"),
            registrationNumber = rs.getString("registration_number")
        )

        // 2. If not found in drivers, try fleet_owners table
        val sqlOwner = "SELECT * FROM fleet_owners WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
        val stmtOwner = conn.prepareStatement(sqlOwner)
        stmtOwner.setString(1, cleanPhone)
        stmtOwner.setString(2, altPhone)
        val rsOwner = stmtOwner.executeQuery()
        if (rsOwner.next()) return Driver(
            id = rsOwner.getInt("id"),
            fullName = rsOwner.getString("full_name") ?: "",
            email = rsOwner.getString("email") ?: "",
            phone = rsOwner.getString("phone") ?: "",
            region = rsOwner.getString("region") ?: "",
            licenseNumber = "N/A",
            vehicleType = "Fleet",
            vehicleNumber = "OWNER",
            status = rsOwner.getString("status") ?: "PENDING",
            isOnline = false,
            rating = 5.0,
            serviceType = ServiceType.BOTH,
            profilePicture = rsOwner.getString("profile_picture"),
            userRole = "OWNER",
            companyName = rsOwner.getString("company_name"),
            registrationNumber = rsOwner.getString("registration_number")
        )
    }
    return null
}

private fun registerDriverInDb(data: Map<String, String>): Int? {
    val role = data["user_role"]?.trim()?.uppercase() ?: "DRIVER"
    val email = data["email"]?.trim()?.lowercase() ?: ""

    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.autoCommit = false
        try {
            var primaryId: Int? = null
            var fleetOwnerId: Int? = null

            // 1. Save to fleet_owners table if OWNER or BOTH
            if (role == "OWNER" || role == "BOTH") {
                val sql = "INSERT INTO fleet_owners (full_name, email, phone, password, company_name, registration_number, region, status, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, data["full_name"]?.trim() ?: "")
                stmt.setString(2, email)
                stmt.setString(3, data["phone"]?.trim() ?: "")
                stmt.setString(4, data["password"]?.trim() ?: "")
                stmt.setString(5, data["company_name"]?.trim() ?: "My Fleet")
                stmt.setString(6, data["registration_number"]?.trim())
                stmt.setString(7, data["region"]?.trim() ?: "")
                stmt.setString(8, "PENDING")
                stmt.setString(9, data["firebase_uid"])
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    fleetOwnerId = rs.getInt(1)
                    if (role == "OWNER") primaryId = fleetOwnerId
                }
            }

            // 2. Save to drivers table if DRIVER or BOTH
            if (role == "DRIVER" || role == "BOTH") {
                val sql = "INSERT INTO drivers (full_name, email, phone, region, password, license_number, vehicle_type, vehicle_number, service_types, user_role, company_name, registration_number, emergency_contact_1, emergency_contact_2, fleet_owner_id, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, data["full_name"]?.trim() ?: "")
                stmt.setString(2, email)
                stmt.setString(3, data["phone"]?.trim() ?: "")
                stmt.setString(4, data["region"]?.trim() ?: "")
                stmt.setString(5, data["password"]?.trim() ?: "")
                stmt.setString(6, data["license_number"]?.trim() ?: "")
                stmt.setString(7, data["vehicle_type"]?.trim() ?: "")
                stmt.setString(8, data["vehicle_number"]?.trim() ?: "")
                stmt.setString(9, data["service_type"]?.trim()?.uppercase() ?: "BOTH")
                stmt.setString(10, role)
                stmt.setString(11, data["company_name"]?.trim())
                stmt.setString(12, data["registration_number"]?.trim())
                stmt.setString(13, data["emergency_contact_1"]?.trim())
                stmt.setString(14, data["emergency_contact_2"]?.trim())
                if (fleetOwnerId != null) stmt.setInt(15, fleetOwnerId) else stmt.setNull(15, java.sql.Types.INTEGER)
                stmt.setString(16, data["firebase_uid"])

                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val driverId = rs.getInt(1)
                    primaryId = driverId
                    // Initialize stats row
                    conn.prepareStatement("INSERT INTO driver_stats (driver_id) VALUES (?)").apply {
                        setInt(1, driverId)
                        executeUpdate()
                    }
                }
            }

            if (primaryId != null) {
                conn.commit()
                return primaryId
            }
            conn.rollback()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }
    return null
}

private fun getAllDriversFromDb(region: String?): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = if (region == null) "SELECT * FROM drivers ORDER BY region ASC, status ASC, id DESC" else "SELECT * FROM drivers WHERE region = ? ORDER BY status ASC, id DESC"
        val stmt = conn.prepareStatement(sql); if (region != null) stmt.setString(1, region)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"), "name" to rs.getString("full_name"), "email" to rs.getString("email"),
                "status" to rs.getString("status"), "phone" to rs.getString("phone"), "region" to (rs.getString("region") ?: "Unknown"),
                "vehicle" to (rs.getString("vehicle_model") ?: rs.getString("vehicle_type") ?: "Car"), "is_online" to rs.getBoolean("is_online")
            ))
        }
    }
    return list
}

private fun getAllFleetOwnersFromDb(region: String?): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = if (region == null) "SELECT * FROM fleet_owners ORDER BY region ASC, status ASC, id DESC" else "SELECT * FROM fleet_owners WHERE region = ? ORDER BY status ASC, id DESC"
        val stmt = conn.prepareStatement(sql); if (region != null) stmt.setString(1, region)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"),
                "name" to rs.getString("full_name"),
                "email" to rs.getString("email"),
                "phone" to rs.getString("phone"),
                "company_name" to rs.getString("company_name"),
                "registration_number" to (rs.getString("registration_number") ?: "N/A"),
                "status" to rs.getString("status"),
                "region" to (rs.getString("region") ?: "Unknown"),
                "date_joined" to rs.getTimestamp("date_joined").toString()
            ))
        }
    }
    return list
}

private fun getFleetOwnerDetailsFromDb(id: Int): Map<String, Any>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT * FROM fleet_owners WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return mapOf(
            "id" to rs.getInt("id"), "full_name" to rs.getString("full_name"), "email" to rs.getString("email"), "phone" to rs.getString("phone"),
            "status" to rs.getString("status"), "region" to rs.getString("region"), "company_name" to rs.getString("company_name"),
            "registration_number" to rs.getString("registration_number"),
            "profile_picture" to rs.getString("profile_picture"), "id_front_image" to rs.getString("id_front_image"),
            "id_back_image" to rs.getString("id_back_image"), "business_certificate" to rs.getString("business_certificate")
        )
    }
    return null
}

private fun updateFleetOwnerStatusInDb(id: Int, status: String) {
    DatabaseInitializer.getDataSource().connection.use { it.prepareStatement("UPDATE fleet_owners SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, id); executeUpdate() } }
}

private fun userExistsInAnyTable(email: String): Boolean {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Check Drivers
        val s1 = conn.prepareStatement("SELECT 1 FROM drivers WHERE LOWER(TRIM(email)) = ?")
        s1.setString(1, email)
        if (s1.executeQuery().next()) return true

        // Check Customers
        val s2 = conn.prepareStatement("SELECT 1 FROM customers WHERE LOWER(TRIM(email)) = ?")
        s2.setString(1, email)
        if (s2.executeQuery().next()) return true

        // Check Fleet Owners
        val s3 = conn.prepareStatement("SELECT 1 FROM fleet_owners WHERE LOWER(TRIM(email)) = ?")
        s3.setString(1, email)
        if (s3.executeQuery().next()) return true
    }
    return false
}

private fun updateUserPasswordInDb(email: String, newPass: String) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Update all tables where this email might exist
        val tables = listOf("drivers", "customers", "fleet_owners")
        tables.forEach { table ->
            val sql = "UPDATE $table SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE LOWER(TRIM(email)) = ?"
            conn.prepareStatement(sql).apply {
                setString(1, newPass)
                setString(2, email)
                executeUpdate()
            }
        }
    }
}



private fun getProductFromDb(id: Int): Map<String, Any?>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT * FROM products WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) {
            return mapOf(
                "id" to rs.getInt("id"),
                "name" to rs.getString("name"),
                "description" to rs.getString("description"),
                "price" to rs.getDouble("price"),
                "category" to rs.getString("category"),
                "location" to rs.getString("location"),
                "images" to (rs.getString("images")?.split(",")?.map { it.replace("\\", "/") } ?: emptyList<String>()),
                "seller_id" to rs.getInt("seller_id")
            )
        }
    }
    return null
}

private fun getDriverDetailsFromDb(id: Int): Map<String, Any>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT * FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return mapOf(
            "id" to rs.getInt("id"), "full_name" to rs.getString("full_name"), "email" to rs.getString("email"), "phone" to rs.getString("phone"),
            "status" to rs.getString("status"), "region" to rs.getString("region"), "license_number" to rs.getString("license_number"),
            "vehicle_type" to rs.getString("vehicle_type"), "vehicle_number" to rs.getString("vehicle_number"),
            "vehicle_category" to (rs.getString("vehicle_category") ?: "Economy"),
            "profile_picture" to rs.getString("profile_picture"), "license_image" to rs.getString("license_image"),
            "id_front_image" to rs.getString("id_front_image"), "id_back_image" to rs.getString("id_back_image"), 
            "vehicle_image" to rs.getString("vehicle_image"), "insurance_cert" to rs.getString("insurance_cert"),
            "roadworthy_cert" to rs.getString("roadworthy_cert")
        )
    }
    return null
}

private fun updateDriverStatusInDb(id: Int, status: String) {
    DatabaseInitializer.getDataSource().connection.use { it.prepareStatement("UPDATE drivers SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, id); executeUpdate() } }
}

private fun getDriverStatsFromDb(id: Int): DriverStats? {
    try {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            // Use LEFT JOIN to ensure we get a row even if driver_stats hasn't been created yet
            // Use COALESCE to provide default values if driver_stats row is missing
            val sql = """
                SELECT 
                    COALESCE(ds.is_online, d.is_online) as is_online,
                    COALESCE(ds.total_deliveries, 0) as total_deliveries,
                    COALESCE(ds.total_earnings, 0.0) as total_earnings,
                    COALESCE(ds.earnings_today, 0.0) as earnings_today,
                    COALESCE(ds.completed_today, 0) as completed_today,
                    ds.updated_at,
                    d.rating as avg_rating, 
                    d.rating_count 
                FROM drivers d
                LEFT JOIN driver_stats ds ON d.id = ds.driver_id 
                WHERE d.id = ?
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val updatedAt = rs.getTimestamp("updated_at")
                val isToday = updatedAt != null && updatedAt.toLocalDateTime().toLocalDate() == java.time.LocalDate.now()
                
                return DriverStats(
                    isOnline = rs.getBoolean("is_online"),
                    activeDeliveries = 0,
                    completedToday = if (isToday) rs.getInt("completed_today") else 0,
                    earningsToday = if (isToday) rs.getDouble("earnings_today") else 0.0,
                    rating = rs.getDouble("avg_rating"),
                    ratingCount = rs.getInt("rating_count"),
                    totalDeliveries = rs.getInt("total_deliveries"),
                    completionRate = 100, 
                    totalEarnings = rs.getDouble("total_earnings")
                )
            }
        }
    } catch (e: Exception) {
        println("ERROR in getDriverStatsFromDb: ${e.message}")
        e.printStackTrace()
    }
    return DriverStats(rating = 5.0)
}

private fun getDriverStatusFromDb(id: Int): DriverStatusResponse? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT status, profile_picture, license_image, insurance_cert, roadworthy_cert, id_front_image, emergency_contact_1, emergency_contact_2, daily_fee_paid_at, daily_fee_expires_at, vehicle_category, vehicle_number, vehicle_model, is_online FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) {
            val missing = mutableListOf<String>()
            
            fun isMissing(col: String): Boolean {
                val value = rs.getString(col) ?: return true
                val clean = value.trim()
                if (clean.isEmpty() || clean.lowercase() == "null" || clean.lowercase() == "undefined") return true
                if (clean.contains("placeholder", ignoreCase = true) || clean.contains("default", ignoreCase = true)) return true
                if (clean.length < 5) return true // Too short for a valid URL
                return false
            }
            
            if (isMissing("profile_picture")) missing.add("profile_pic")
            if (isMissing("license_image")) missing.add("drivers_license")
            if (isMissing("insurance_cert")) missing.add("insurance_cert")
            if (isMissing("roadworthy_cert")) missing.add("roadworthy_cert")
            if (isMissing("id_front_image")) missing.add("ghana_card")
            
            val isPaid = isDailyFeePaid(id)
            val expiryTime = rs.getString("daily_fee_expires_at")
            val config = getPricingConfigFromDb()

            return DriverStatusResponse(
                success = true, 
                status = rs.getString("status") ?: "PENDING", 
                missingDocs = missing,
                emergencyContact1 = rs.getString("emergency_contact_1"),
                emergencyContact2 = rs.getString("emergency_contact_2"),
                profilePicture = rs.getString("profile_picture"),
                isOnline = rs.getBoolean("is_online"),
                isDailyFeePaid = isPaid,
                dailyFeeAmount = config.dailyServiceFee,
                dailyFeeExpiryTime = expiryTime,
                vehicleCategory = rs.getString("vehicle_category") ?: "Economy",
                plateNumber = rs.getString(11),
                vehicleModel = rs.getString(12)
            )
        }
    }
    return null
}

private fun getAllRentalsFromDb(region: String?): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = if (region == null) {
            "SELECT r.*, c.name as customer_name, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id ORDER BY r.id DESC"
        } else {
            "SELECT r.*, c.name as customer_name, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE c.region = ? ORDER BY r.id DESC"
        }
        val stmt = conn.prepareStatement(sql)
        if (region != null) stmt.setString(1, region)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"), 
                "customer_name" to rs.getString("customer_name"), 
                "vehicle_name" to rs.getString("vehicle_name"), 
                "status" to rs.getString("status"), 
                "total_price" to rs.getDouble("total_price"),
                "booking_code" to (rs.getString("booking_code") ?: ""),
                "trip_notes" to (rs.getString("trip_notes") ?: ""),
                "start_time" to (rs.getString("start_time") ?: ""),
                "created_at" to (rs.getString("created_at") ?: "")
            ))
        }
    }
    return list
}

private fun getPricingConfigFromDb(): PricingConfig {
    // 1. Try Redis Cache first
    val cached = RedisManager.get("config:pricing")
    if (cached != null) {
        try {
            return com.google.gson.Gson().fromJson(cached, PricingConfig::class.java)
        } catch (e: Exception) { /* ignore and fallback */ }
    }

    // 2. Fallback to Database
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM pricing_config LIMIT 1")
        if (rs.next()) {
            val config = PricingConfig(
                baseFare = rs.getDouble("base_fare"), perKmRate = rs.getDouble("per_km_rate"), perMinuteRate = rs.getDouble("per_minute_rate"),
                minFare = rs.getDouble("min_fare"), milestoneInterval = rs.getInt("milestone_interval"), milestoneDiscountPercent = rs.getInt("milestone_discount_percent"),
                peakMultiplier = rs.getDouble("peak_multiplier"), driverCommissionPercent = rs.getDouble("driver_commission_percent"),
                dailyServiceFee = rs.getDouble("daily_service_fee"), rentalDailyRate = rs.getDouble("rental_daily_rate"), 
                rentalCommissionPercent = rs.getDouble("rental_commission_percent"),
                rentalOwnerCommissionPercent = rs.getDouble("rental_owner_commission_percent"),
                rentalCustomerServiceFeePercent = rs.getDouble("rental_customer_service_fee_percent")
            )
            // 3. Update Redis Cache (TTL: 1 hour)
            RedisManager.set("config:pricing", com.google.gson.Gson().toJson(config), 3600)
            return config
        }
    }
    return PricingConfig(0.0, 0.0, 0.0, 0.0, 0, 0, 1.0, 0.0, 0.0, 0.0, 0.0, 7.5, 7.5)
}

private fun updatePricingConfigInDb(
    baseFare: Double?, perKmRate: Double?, perMinuteRate: Double?, minFare: Double?, 
    milestoneInterval: Int?, milestoneDiscount: Int?, driverCommission: Double?, 
    peakMultiplier: Double?, dailyServiceFee: Double?,
    rentalOwnerComm: Double? = null, rentalGuestFee: Double? = null
) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "UPDATE pricing_config SET base_fare=COALESCE(?, base_fare), per_km_rate=COALESCE(?, per_km_rate), per_minute_rate=COALESCE(?, per_minute_rate), min_fare=COALESCE(?, min_fare), milestone_interval=COALESCE(?, milestone_interval), milestone_discount_percent=COALESCE(?, milestone_discount_percent), driver_commission_percent=COALESCE(?, driver_commission_percent), peak_multiplier=COALESCE(?, peak_multiplier), daily_service_fee=COALESCE(?, daily_service_fee), rental_owner_commission_percent=COALESCE(?, rental_owner_commission_percent), rental_customer_service_fee_percent=COALESCE(?, rental_customer_service_fee_percent)"
        conn.prepareStatement(sql).apply {
            setObject(1, baseFare); setObject(2, perKmRate); setObject(3, perMinuteRate); setObject(4, minFare)
            setObject(5, milestoneInterval); setObject(6, milestoneDiscount); setObject(7, driverCommission); setObject(8, peakMultiplier); setObject(9, dailyServiceFee)
            setObject(10, rentalOwnerComm); setObject(11, rentalGuestFee)
            executeUpdate()
        }
    }
    // Clear cache so changes are picked up immediately
    RedisManager.delete("config:pricing")
}

private fun getPendingPaymentsFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT t.*, d.full_name, d.phone FROM wallet_topups t JOIN drivers d ON t.driver_id = d.id WHERE t.status = 'PENDING' ORDER BY t.created_at DESC")
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"), 
                "driver_name" to rs.getString("full_name"), 
                "driver_phone" to rs.getString("phone"),
                "amount" to rs.getDouble("amount"), 
                "payment_type" to (rs.getString("payment_type") ?: "TOPUP"),
                "reference" to rs.getString("reference_code"),
                "created_at" to rs.getString("created_at")
            ))
        }
    }
    return list
}

private fun approvePaymentInDb(id: Int) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT driver_id, amount, payment_type, reference_code FROM wallet_topups WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) {
            val dId = rs.getInt(1)
            val amt = rs.getDouble(2)
            val type = rs.getString(3) ?: "TOPUP"
            val ref = rs.getString(4)

            if (type == "DAILY_FEE") {
                conn.prepareStatement("UPDATE drivers SET daily_fee_paid_at = CURRENT_DATE, daily_fee_expires_at = CURRENT_TIMESTAMP + interval '24 hours' WHERE id = ?").apply { setInt(1, dId); executeUpdate() }
                
                // Record in payments table too
                recordPaymentInDb(dId, "DRIVER", amt, "DAILY_FEE", ref)

                // Notify driver via WebSocket
                runBlocking {
                    sendToUser("DRIVER_$dId", "NOTIFICATION_RECEIVED", mapOf(
                        "title" to "Daily Fee Approved",
                        "message" to "Your daily service fee has been approved. You can now go online.",
                        "type" to "DAILY_FEE_PAID",
                        "createdAt" to java.time.LocalDateTime.now().toString()
                    ))
                }
            } else {
                conn.prepareStatement("UPDATE driver_stats SET total_earnings = total_earnings + ? WHERE driver_id = ?").apply { setDouble(1, amt); setInt(2, dId); executeUpdate() }
                recordPaymentInDb(dId, "DRIVER", amt, "TOPUP", ref)
            }

            conn.prepareStatement("UPDATE wallet_topups SET status = 'APPROVED' WHERE id = ?").apply { setInt(1, id); executeUpdate() }
        }
    }
}

private fun rejectPaymentInDb(id: Int) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.prepareStatement("UPDATE wallet_topups SET status = 'REJECTED' WHERE id = ?").apply {
            setInt(1, id)
            executeUpdate()
        }
    }
}

private fun getPlatformStatsFromDb(): Map<String, Any> {
    var rev = 0.0
    var debt = 0.0
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Total from Rides + Rentals + Daily Fees
        val rsRides = conn.createStatement().executeQuery("SELECT SUM(total_amount) FROM orders WHERE status = 'DELIVERED'")
        if (rsRides.next()) rev += rsRides.getDouble(1)
        
        val rsRentals = conn.createStatement().executeQuery("SELECT SUM(total_price) FROM rentals WHERE payment_status = 'PAID'")
        if (rsRentals.next()) rev += rsRentals.getDouble(1)

        val rsFees = conn.createStatement().executeQuery("SELECT SUM(amount) FROM payments WHERE payment_type = 'DAILY_FEE'")
        if (rsFees.next()) rev += rsFees.getDouble(1)
        
        val rsDebt = conn.createStatement().executeQuery("SELECT SUM(amount) FROM wallet_topups WHERE status = 'PENDING'")
        if (rsDebt.next()) debt = rsDebt.getDouble(1)
    }
    return mapOf("totalRevenue" to rev.toInt(), "totalDebt" to debt.toInt())
}

private fun getAllCustomersFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM customers ORDER BY id DESC")
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"), 
                "name" to rs.getString("name"), 
                "email" to rs.getString("email"), 
                "phone" to rs.getString("phone"), 
                "region" to (rs.getString("region") ?: "N/A"),
                "profile_pic" to (rs.getString("profile_picture") ?: ""),
                "is_active" to rs.getBoolean("is_active"),
                "date_joined" to (rs.getString("date_joined") ?: "")
            ))
        }
    }
    return list
}

private fun getAllAdminsFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM admins ORDER BY id DESC")
        while (rs.next()) {
            list.add(mapOf("id" to rs.getInt("id"), "username" to rs.getString("username"), "email" to rs.getString("email"), "role" to rs.getString("role"), "region" to (rs.getString("region") ?: "Nationwide"), "is_active" to rs.getBoolean("is_active")))
        }
    }
    return list
}

private fun createAdminInDb(user: String, email: String, pass: String, role: String, region: String?) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "INSERT INTO admins (username, email, password, role, region) VALUES (?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).apply {
            setString(1, user); setString(2, email); setString(3, pass); setString(4, role); setString(5, region)
            executeUpdate()
        }
    }
}

private fun loginAdminInDb(userOrEmail: String, pass: String): Map<String, Any>? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "SELECT id, username, role FROM admins WHERE (LOWER(username) = ? OR LOWER(email) = ?) AND password = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, userOrEmail.lowercase())
        stmt.setString(2, userOrEmail.lowercase())
        stmt.setString(3, pass)
        val rs = stmt.executeQuery()
        if (rs.next()) return mapOf(
            "id" to rs.getInt("id"),
            "username" to rs.getString("username"),
            "role" to rs.getString("role")
        )
    }
    return null
}

private fun getSystemSettingsFromDb(): Map<String, Any> {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM system_settings LIMIT 1")
        if (rs.next()) {
            return mapOf(
                "support_phone" to (rs.getString("support_phone") ?: ""),
                "support_email" to (rs.getString("support_email") ?: ""),
                "support_whatsapp" to (rs.getString("support_whatsapp") ?: ""),
                "app_version_customer" to (rs.getString("app_version_customer") ?: "1.0.0"),
                "app_version_driver" to (rs.getString("app_version_driver") ?: "1.0.0"),
                "maintenance_mode" to rs.getBoolean("maintenance_mode"),
                "paystack_mode" to (rs.getString("paystack_mode") ?: "TEST"),
                "paystack_test_secret" to (rs.getString("paystack_test_secret") ?: ""),
                "paystack_live_secret" to (rs.getString("paystack_live_secret") ?: ""),
                "paystack_test_public" to (rs.getString("paystack_test_public") ?: ""),
                "paystack_live_public" to (rs.getString("paystack_live_public") ?: "")
            )
        }
    }
    return mapOf(
        "support_phone" to "", "support_email" to "", "support_whatsapp" to "",
        "app_version_customer" to "1.0.0", "app_version_driver" to "1.0.0", "maintenance_mode" to false,
        "paystack_mode" to "TEST", "paystack_test_secret" to "", "paystack_live_secret" to "",
        "paystack_test_public" to "", "paystack_live_public" to ""
    )
}

private fun updateSystemSettingsInDb(
    phone: String?, email: String?, whatsapp: String?, verCustomer: String?, verDriver: String?, maintenance: Boolean,
    psMode: String? = null, psTestSec: String? = null, psLiveSec: String? = null, psTestPub: String? = null, psLivePub: String? = null
) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "UPDATE system_settings SET support_phone=COALESCE(?, support_phone), support_email=COALESCE(?, support_email), support_whatsapp=COALESCE(?, support_whatsapp), app_version_customer=COALESCE(?, app_version_customer), app_version_driver=COALESCE(?, app_version_driver), maintenance_mode=?, paystack_mode=COALESCE(?, paystack_mode), paystack_test_secret=COALESCE(?, paystack_test_secret), paystack_live_secret=COALESCE(?, paystack_live_secret), paystack_test_public=COALESCE(?, paystack_test_public), paystack_live_public=COALESCE(?, paystack_live_public), updated_at=CURRENT_TIMESTAMP"
        conn.prepareStatement(sql).apply {
            setString(1, phone)
            setString(2, email)
            setString(3, whatsapp)
            setString(4, verCustomer)
            setString(5, verDriver)
            setBoolean(6, maintenance)
            setString(7, psMode)
            setString(8, psTestSec)
            setString(9, psLiveSec)
            setString(10, psTestPub)
            setString(11, psLivePub)
            executeUpdate()
        }
    }
}

private fun getLiveLocationsFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "SELECT ds.*, d.full_name, d.vehicle_type FROM driver_stats ds JOIN drivers d ON ds.driver_id = d.id WHERE ds.is_online = true"
        val rs = conn.createStatement().executeQuery(sql)
        while (rs.next()) {
            list.add(mapOf("id" to rs.getInt("driver_id"), "name" to rs.getString("full_name"), "lat" to rs.getDouble("latitude"), "lng" to rs.getDouble("longitude"), "vehicle" to rs.getString("vehicle_type")))
        }
    }
    return list
}

private fun getActiveDeliveriesFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = """
            SELECT d.*, o.total_amount, dr.full_name as driver_name 
            FROM deliveries d 
            JOIN orders o ON d.order_id = o.id 
            LEFT JOIN drivers dr ON d.driver_id = dr.id
            WHERE d.status != 'DELIVERED' AND d.status != 'CANCELLED'
            ORDER BY d.id DESC
        """.trimIndent()
        val rs = conn.createStatement().executeQuery(sql)
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"), 
                "pickup" to rs.getString("pickup_location"), 
                "dropoff" to rs.getString("dropoff_location"), 
                "status" to rs.getString("status"), 
                "amount" to rs.getDouble("total_amount"),
                "service_type" to (rs.getString("service_type") ?: "RIDE_HAILING"),
                "driver_name" to (rs.getString("driver_name") ?: "Unassigned")
            ))
        }
    }
    return list
}

private fun getRentalRatesFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM rental_rates")
        while (rs.next()) list.add(mapOf("id" to rs.getInt("id"), "vehicle_type" to rs.getString("vehicle_type"), "daily_rate" to rs.getDouble("daily_rate")))
    }
    return list
}

private fun getOrderStatus(orderId: Int): String {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT status FROM orders WHERE id = ?").apply {
            setInt(1, orderId)
        }.executeQuery()
        return if (rs.next()) rs.getString("status") else "NOT_FOUND"
    }
}

private fun getNearbyDriversFromDb(lat: Double, lng: Double, radius: Double, vehicleType: String? = null): List<DriverLocation> {
    // 1. Try Redis First (Ultra-fast temporary database)
    try {
        val nearbyIds = RedisManager.getNearbyDrivers(lat, lng, radius)
        if (nearbyIds.isNotEmpty()) {
            println("Redis: Found ${nearbyIds.size} nearby drivers for dispatch.")
            // Note: Since Redis only stores IDs, we'd still need to check statuses/types 
            // OR store more metadata in Redis. For now, we use Redis to filter and Postgres to validate.
        }
    } catch (e: Exception) {
        println("Redis lookup failed, falling back to PostgreSQL: ${e.message}")
    }

    // 2. PostgreSQL / H3 Geospatial Logic (Permanent Record)
    val list = mutableListOf<DriverLocation>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Match requested service type to registered vehicle type and admin-assigned category
        val vehicleFilter = if (vehicleType != null) {
            val vType = vehicleType.lowercase()
            when {
                vType.contains("economy") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%economy%' OR d.vehicle_type ILIKE '%saloon%' OR d.vehicle_type ILIKE '%taxi%') AND (d.vehicle_category ILIKE 'Economy' OR d.vehicle_category IS NULL)"
                vType.contains("comfort") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%comfort%' OR d.vehicle_type ILIKE '%saloon%' OR d.vehicle_type ILIKE '%taxi%') AND d.vehicle_category ILIKE 'Comfort'"
                vType.contains("car") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%taxi%' OR d.vehicle_type ILIKE '%saloon%')"
                vType.contains("okada") || vType.contains("bike") -> "AND (d.vehicle_type ILIKE '%okada%' OR d.vehicle_type ILIKE '%bike%')"
                vType.contains("pragya") -> "AND (d.vehicle_type ILIKE '%pragya%' OR d.vehicle_type ILIKE '%tricycle%')"
                vType.contains("aboboyaa") -> "AND d.vehicle_type ILIKE '%aboboyaa%'"
                vType.contains("truck") -> "AND d.vehicle_type ILIKE '%truck%'"
                vType.contains("bicycle") -> "AND d.vehicle_type ILIKE '%bicycle%'"
                else -> "AND (d.vehicle_type ILIKE ? OR d.vehicle_category ILIKE ?)"
            }
        } else ""

        // Use H3 for Geospatial Indexing
        val h3Index = H3Helper.getIndex(lat, lng)
        val neighbors = H3Helper.getNeighbors(h3Index)
        val h3Filter = "AND ds.h3_index IN (${neighbors.joinToString(",") { "'$it'" }})"

        // Use Haversine formula to find drivers within radius (km)
        // If radius is large (e.g. > 100), ignore distance and return all online matching drivers (for testing)
        // Also ignore distance if lat/lng are missing (0,0)
        val useDistanceLimit = radius < 500.0 && !(lat == 0.0 && lng == 0.0)

        val sql = if (useDistanceLimit) {
            """
                SELECT ds.*, d.vehicle_type, d.vehicle_category,
                (6371 * acos(least(1.0, cos(radians(?)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ds.latitude))))) AS distance
                FROM driver_stats ds
                JOIN drivers d ON ds.driver_id = d.id
                WHERE ds.is_online = true AND d.status = 'APPROVED'
                AND ds.latitude IS NOT NULL AND ds.longitude IS NOT NULL
                $h3Filter
                $vehicleFilter
                AND (6371 * acos(least(1.0, cos(radians(?)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ds.latitude)))) ) <= ?
                ORDER BY (
                    (6371 * acos(least(1.0, cos(radians(?)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ds.latitude))))) * 0.7 
                    + (ds.completed_today * 0.5)
                ) ASC
            """.trimIndent()
        } else {
            """
                SELECT ds.*, d.vehicle_type, d.vehicle_category, 0.0 as distance
                FROM driver_stats ds
                JOIN drivers d ON ds.driver_id = d.id
                WHERE ds.is_online = true AND d.status = 'APPROVED'
                $vehicleFilter
                ORDER BY ds.completed_today ASC, d.id DESC
            """.trimIndent()
        }
        
        fun isPredefined(vt: String): Boolean {
            val v = vt.lowercase()
            return v.contains("economy") || v.contains("comfort") || v.contains("car") ||
                   v.contains("okada") || v.contains("bike") || v.contains("pragya") ||
                   v.contains("aboboyaa") || v.contains("truck") || v.contains("bicycle")
        }

        val stmt = conn.prepareStatement(sql)
        if (useDistanceLimit) {
            stmt.setDouble(1, lat); stmt.setDouble(2, lng); stmt.setDouble(3, lat)
            var paramIdx = 4
            if (vehicleType != null && !isPredefined(vehicleType)) {
                stmt.setString(paramIdx++, vehicleType)
                stmt.setString(paramIdx++, vehicleType)
            }
            stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
            stmt.setDouble(paramIdx++, radius)
            // Sorting parameters (Haversine again)
            stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
        } else {
            if (vehicleType != null && !isPredefined(vehicleType)) {
                stmt.setString(1, vehicleType)
                stmt.setString(2, vehicleType)
            }
        }
        
        val rs = stmt.executeQuery()
        while (rs.next()) {
            val dist = rs.getDouble("distance")
            // Simple ETA calculation: assume avg speed 30km/h in city
            val pickupEta = (dist / 30.0) * 60.0 + 2.0
            
            list.add(DriverLocation(
                id = rs.getInt("driver_id").toString(),
                latitude = rs.getDouble("latitude"),
                longitude = rs.getDouble("longitude"),
                bearing = rs.getFloat("bearing"),
                vehicleType = rs.getString("vehicle_type"),
                vehicleCategory = rs.getString("vehicle_category"),
                pickupEtaMin = pickupEta
            ))
        }
    }
    return list
}

private fun getAvailableDeliveriesByRadius(lat: Double, lng: Double, radius: Double, vehicleType: String, vehicleCategory: String? = null): List<Delivery> {
    val list = mutableListOf<Delivery>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Match service type to registered vehicle type and admin-assigned category
        val vType = vehicleType.lowercase()
        val vehicleFilter = when {
            vType == "car" -> "AND (service_type ILIKE 'car' OR service_type ILIKE 'Economy' OR service_type ILIKE 'Comfort')"
            vType == "okada" -> "AND (service_type ILIKE 'okada' OR service_type ILIKE 'bike')"
            vType == "pragya" -> "AND service_type ILIKE 'pragya'"
            vType == "aboboyaa" -> "AND service_type ILIKE 'aboboyaa'"
            vType == "truck" -> "AND service_type ILIKE 'truck'"
            vType == "bicycle" -> "AND service_type ILIKE 'bicycle'"
            else -> "AND (service_type ILIKE ? OR service_type ILIKE 'car')"
        }

        val sql = """
            SELECT d.*, c.name as customer_name, c.phone as customer_phone, o.total_amount as total_fare,
            (6371 * acos(cos(radians(?)) * cos(radians(d.pickup_lat)) * cos(radians(d.pickup_lng) - radians(?)) + sin(radians(?)) * sin(radians(d.pickup_lat)))) AS distance
            FROM deliveries d
            JOIN orders o ON d.order_id = o.id
            JOIN customers c ON o.customer_id = c.id
            WHERE d.status = 'PENDING'
            $vehicleFilter
            AND (6371 * acos(cos(radians(?)) * cos(radians(d.pickup_lat)) * cos(radians(d.pickup_lng) - radians(?)) + sin(radians(?)) * sin(radians(d.pickup_lat)))) <= ?
            ORDER BY distance ASC
        """.trimIndent()
        
        val stmt = conn.prepareStatement(sql)
        stmt.setDouble(1, lat); stmt.setDouble(2, lng); stmt.setDouble(3, lat)
        
        var paramIdx = 4
        if (vType != "car" && vType != "okada" && vType != "pragya" && vType != "aboboyaa" && vType != "truck" && vType != "bicycle") {
            stmt.setString(paramIdx++, vehicleType)
        }
        
        stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
        stmt.setDouble(paramIdx++, radius)
        
        val rs = stmt.executeQuery()
        while (rs.next()) {
            val dist = rs.getDouble("distance")
            // Simple ETA calculation: assume avg speed 25km/h for pickup
            val pickupEta = (dist / 25.0) * 60.0 + 1.0 
            
            list.add(Delivery(
                id = rs.getString("id"),
                orderId = rs.getInt("order_id"),
                driverId = null,
                pickupLocation = rs.getString("pickup_location"),
                dropOffLocation = rs.getString("dropoff_location"),
                pickupLat = rs.getDouble("pickup_lat"),
                pickupLng = rs.getDouble("pickup_lng"),
                dropOffLat = rs.getDouble("dropoff_lat"),
                dropOffLng = rs.getDouble("dropoff_lng"),
                status = DeliveryStatus.PENDING,
                distanceKm = rs.getDouble("distance_km"),
                estimatedEarnings = rs.getDouble("estimated_earnings"),
                pickupEtaMin = pickupEta,
                customerName = rs.getString("customer_name"),
                customerPhone = rs.getString("customer_phone"),
                totalFare = rs.getDouble("total_fare")
            ))
        }
    }
    return list
}

private fun updateDriverOnlineStatusInDb(id: String, online: Boolean): String? {
    if (online) {
        val paid = isDailyFeePaid(id.toInt())
        println("DEBUG: Driver $id check online status. Daily fee paid: $paid")
        if (!paid) {
            return "Daily service fee not paid. Please pay from your wallet to go online."
        }
    } else {
        // Remove from Redis real-time tracking when going offline
        RedisManager.removeDriverLocation(id)
    }
    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.prepareStatement("INSERT INTO driver_stats (driver_id, is_online) VALUES (?, ?) ON CONFLICT (driver_id) DO UPDATE SET is_online = EXCLUDED.is_online, updated_at = CURRENT_TIMESTAMP").apply { setInt(1, id.toInt()); setBoolean(2, online); executeUpdate() }
        conn.prepareStatement("UPDATE drivers SET is_online = ? WHERE id = ?").apply { setBoolean(1, online); setInt(2, id.toInt()); executeUpdate() }
    }
    return null
}

private fun isDailyFeePaid(driverId: Int): Boolean {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Check if paid today or within the 24h window
        val rs = conn.prepareStatement("SELECT 1 FROM drivers WHERE id = ? AND (daily_fee_paid_at >= CURRENT_DATE OR daily_fee_expires_at > CURRENT_TIMESTAMP)").apply { setInt(1, driverId) }.executeQuery()
        return rs.next()
    }
}

private fun updateDriverDocumentInDb(driverId: String, docType: String, fileUrl: String) {
    val column = when (docType) {
        "profile_pic" -> "profile_picture"
        "drivers_license" -> "license_image"
        "ghana_card" -> "id_front_image"
        "insurance_cert" -> "insurance_cert"
        "roadworthy_cert" -> "roadworthy_cert"
        else -> null
    }

    if (column != null) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE drivers SET $column = ?, status = CASE WHEN status = 'PENDING' THEN 'PENDING_DOCS' ELSE status END, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, fileUrl)
            stmt.setInt(2, driverId.toInt())
            stmt.executeUpdate()
        }
    }
}

private fun updateDriverLocationInDb(id: String, lat: Double, lng: Double, bearing: Float) {
    // 1. Update Redis (Temporary Database) for real-time performance
    RedisManager.updateDriverLocation(id, lat, lng, bearing)

    // 2. Update PostgreSQL (Permanent Record)
    val h3Index = H3Helper.getIndex(lat, lng)
    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.prepareStatement("INSERT INTO driver_stats (driver_id, latitude, longitude, bearing, h3_index) VALUES (?, ?, ?, ?, ?) ON CONFLICT (driver_id) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, bearing = EXCLUDED.bearing, h3_index = EXCLUDED.h3_index, updated_at = CURRENT_TIMESTAMP").apply {
            setInt(1, id.toInt())
            setDouble(2, lat)
            setDouble(3, lng)
            setFloat(4, bearing)
            setString(5, h3Index)
            executeUpdate()
        }
        
        // Log movement for intelligence
        val logSql = "INSERT INTO movement_logs (entity_id, entity_type, latitude, longitude) VALUES (?, 'DRIVER', ?, ?)"
        conn.prepareStatement(logSql).apply {
            setInt(1, id.toInt())
            setDouble(2, lat)
            setDouble(3, lng)
            executeUpdate()
        }
    }
}

private fun updateRentalVehicleLocationInDb(vehicleId: Int, lat: Double, lng: Double) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "UPDATE rental_vehicles SET latitude = ?, longitude = ? WHERE id = ?"
        conn.prepareStatement(sql).apply {
            setDouble(1, lat)
            setDouble(2, lng)
            setInt(3, vehicleId)
            executeUpdate()
        }
    }
}

private fun updateEmergencyContactsInDb(driverId: String, c1: String, c2: String) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "UPDATE drivers SET emergency_contact_1 = ?, emergency_contact_2 = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, c1)
        stmt.setString(2, c2)
        stmt.setInt(3, driverId.toInt())
        stmt.executeUpdate()
    }
}

private fun verifyPaystackSignature(body: String, signature: String): Boolean {
    return try {
        val settings = getSystemSettingsFromDb()
        val mode = settings["paystack_mode"] as? String ?: "TEST"
        val secret = if (mode == "LIVE") settings["paystack_live_secret"] as? String else settings["paystack_test_secret"] as? String
        
        if (secret.isNullOrBlank()) return false

        val hmac = Mac.getInstance("HmacSHA512")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA512")
        hmac.init(secretKey)
        val hash = hmac.doFinal(body.toByteArray())
        val computedSignature = hash.joinToString("") { "%02x".format(it) }
        computedSignature == signature
    } catch (e: Exception) {
        false
    }
}

private fun updateDriverVehicleInDb(id: String, type: String, number: String, model: String, service: String) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "UPDATE drivers SET vehicle_type = ?, vehicle_number = ?, vehicle_model = ?, service_types = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, type)
        stmt.setString(2, number)
        stmt.setString(3, model)
        stmt.setString(4, service)
        stmt.setInt(5, id.toInt())
        stmt.executeUpdate()
    }
}

private fun getFleetVehiclesFromDb(ownerId: Int): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Check for vehicles where this ID is the owner (legacy), the fleet owner directly, 
        // or the driver is linked to a fleet owner record.
        val sql = """
            SELECT * FROM rental_vehicles 
            WHERE owner_id = ? 
               OR fleet_owner_id = ? 
               OR fleet_owner_id = (SELECT fleet_owner_id FROM drivers WHERE id = ?)
            ORDER BY id DESC
        """.trimIndent()
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, ownerId)
        stmt.setInt(2, ownerId)
        stmt.setInt(3, ownerId)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"),
                "name" to rs.getString("name"),
                "model" to (rs.getString("model") ?: ""),
                "vehicle_type" to rs.getString("vehicle_type"),
                "vehicle_number" to (rs.getString("vehicle_number") ?: ""),
                "daily_rate" to rs.getDouble("daily_rate"),
                "status" to (rs.getString("status") ?: "AVAILABLE"),
                "is_available" to rs.getBoolean("is_available"),
                "description" to (rs.getString("description") ?: ""),
                "features" to (rs.getString("features") ?: ""),
                "image_urls" to (rs.getString("image_urls") ?: ""),
                "location" to (rs.getString("location") ?: ""),
                "seats" to rs.getInt("seats"),
                "transmission" to (rs.getString("transmission") ?: "Automatic"),
                "fuel_type" to (rs.getString("fuel_type") ?: "Petrol")
            ))
        }
    }
    return list
}

private fun getAllFleetVehiclesFromDb(): List<Map<String, Any>> {
    val list = mutableListOf<Map<String, Any>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT v.*, d.full_name as owner_name FROM rental_vehicles v LEFT JOIN drivers d ON v.owner_id = d.id ORDER BY v.id DESC")
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"),
                "name" to rs.getString("name"),
                "model" to (rs.getString("model") ?: ""),
                "vehicle_type" to rs.getString("vehicle_type"),
                "vehicle_number" to (rs.getString("vehicle_number") ?: ""),
                "daily_rate" to rs.getDouble("daily_rate"),
                "status" to (rs.getString("status") ?: "AVAILABLE"),
                "is_available" to rs.getBoolean("is_available"),
                "description" to (rs.getString("description") ?: ""),
                "features" to (rs.getString("features") ?: ""),
                "image_urls" to (rs.getString("image_urls") ?: ""),
                "location" to (rs.getString("location") ?: ""),
                "owner_name" to (rs.getString("owner_name") ?: "System"),
                "seats" to rs.getInt("seats"),
                "transmission" to (rs.getString("transmission") ?: "Automatic"),
                "fuel_type" to (rs.getString("fuel_type") ?: "Petrol")
            ))
        }
    }
    return list
}

private fun addVehicleToFleetInDb(
    ownerId: Int, name: String, model: String, type: String, number: String, rate: Double,
    description: String? = null, features: String? = null, imageUrls: String? = null,
    location: String? = null, lat: Double? = null, lng: Double? = null,
    seats: Int? = 5, transmission: String? = "Automatic", fuelType: String? = "Petrol"
) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Resolve if this is a driver who has a fleet owner or if this is the fleet owner directly
        var actualFleetOwnerId: Int? = null
        val driverRs = conn.prepareStatement("SELECT fleet_owner_id FROM drivers WHERE id = ?").apply { setInt(1, ownerId) }.executeQuery()
        if (driverRs.next()) {
            actualFleetOwnerId = driverRs.getObject("fleet_owner_id") as? Int
        }
        
        // If not a driver with fleet_owner_id, maybe the ownerId is a fleet_owner_id itself?
        if (actualFleetOwnerId == null) {
            val ownerRs = conn.prepareStatement("SELECT id FROM fleet_owners WHERE id = ?").apply { setInt(1, ownerId) }.executeQuery()
            if (ownerRs.next()) {
                actualFleetOwnerId = ownerId
            }
        }

        val sql = """
            INSERT INTO rental_vehicles (
                owner_id, fleet_owner_id, name, model, vehicle_type, vehicle_number, daily_rate, 
                description, features, image_urls, location, latitude, longitude,
                seats, transmission, fuel_type
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        conn.prepareStatement(sql).apply {
            setInt(1, ownerId)
            if (actualFleetOwnerId != null) setInt(2, actualFleetOwnerId) else setNull(2, java.sql.Types.INTEGER)
            setString(3, name)
            setString(4, model)
            setString(5, type)
            setString(6, number)
            setDouble(7, rate)
            setString(8, description)
            setString(9, features)
            setString(10, imageUrls)
            setString(11, location)
            if (lat != null) setDouble(12, lat) else setNull(12, java.sql.Types.DOUBLE)
            if (lng != null) setDouble(13, lng) else setNull(13, java.sql.Types.DOUBLE)
            setInt(14, seats ?: 5)
            setString(15, transmission ?: "Automatic")
            setString(16, fuelType ?: "Petrol")
            executeUpdate()
        }
    }
}

private fun updateFleetVehicleInDb(
    id: Int, name: String, model: String, type: String, number: String, rate: Double,
    description: String? = null, features: String? = null, imageUrls: String? = null,
    status: String? = null, seats: Int? = null, transmission: String? = null, fuelType: String? = null
) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = """
            UPDATE rental_vehicles SET 
                name = ?, model = ?, vehicle_type = ?, vehicle_number = ?, daily_rate = ?, 
                description = ?, features = ?, image_urls = ?, status = COALESCE(?, status),
                seats = COALESCE(?, seats), transmission = COALESCE(?, transmission), fuel_type = COALESCE(?, fuel_type)
            WHERE id = ?
        """.trimIndent()
        conn.prepareStatement(sql).apply {
            setString(1, name)
            setString(2, model)
            setString(3, type)
            setString(4, number)
            setDouble(5, rate)
            setString(6, description)
            setString(7, features)
            setString(8, imageUrls)
            setString(9, status)
            if (seats != null) setInt(10, seats) else setNull(10, java.sql.Types.INTEGER)
            setString(11, transmission)
            setString(12, fuelType)
            setInt(13, id)
            executeUpdate()
        }
    }
}

private fun deleteFleetVehicleFromDb(id: Int) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.prepareStatement("DELETE FROM rental_vehicles WHERE id = ?").apply {
            setInt(1, id)
            executeUpdate()
        }
    }
}

private fun getAllTablesFromDb(): List<String> {
    val tables = mutableListOf<String>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name"
        )
        while (rs.next()) {
            tables.add(rs.getString(1))
        }
    }
    return tables
}

private fun getTableDataFromDb(tableName: String, limit: Int = 100): Pair<List<String>, List<List<Any?>>> {
    val columns = mutableListOf<String>()
    val rows = mutableListOf<List<Any?>>()
    
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "SELECT * FROM \"$tableName\" LIMIT ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, limit)
        val rs = stmt.executeQuery()
        val meta = rs.metaData
        for (i in 1..meta.columnCount) {
            columns.add(meta.getColumnName(i))
        }
        while (rs.next()) {
            val row = mutableListOf<Any?>()
            for (i in 1..meta.columnCount) {
                row.add(rs.getObject(i))
            }
            rows.add(row)
        }
    }
    return Pair(columns, rows)
}

private fun getAllProductsFromDb(): List<Map<String, Any?>> {
    val list = mutableListOf<Map<String, Any?>>()
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.createStatement().executeQuery("SELECT * FROM products ORDER BY id DESC")
        while (rs.next()) {
            list.add(mapOf(
                "id" to rs.getInt("id"),
                "name" to rs.getString("name"),
                "description" to rs.getString("description"),
                "price" to rs.getDouble("price"),
                "category" to rs.getString("category"),
                "location" to rs.getString("location"),
                "images" to (rs.getString("images")?.split(",")?.map { it.replace("\\", "/") } ?: emptyList<String>()),
                "seller_id" to rs.getInt("seller_id")
            ))
        }
    }
    return list
}

private fun terminateDriverAccount(id: Int) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // 1. Get UID
        val rs = conn.prepareStatement("SELECT firebase_uid FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) {
            val uid = rs.getString("firebase_uid")
            if (!uid.isNullOrBlank()) {
                try { FirebaseAuth.getInstance().deleteUser(uid) } catch (e: Exception) { println("Firebase Delete Error: ${e.message}") }
            }
        }
        // 2. Delete Profile
        conn.prepareStatement("DELETE FROM driver_stats WHERE driver_id = ?").apply { setInt(1, id); executeUpdate() }
        conn.prepareStatement("DELETE FROM drivers WHERE id = ?").apply { setInt(1, id); executeUpdate() }
    }
}

private fun terminateCustomerAccount(id: Int) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // 1. Get UID
        val rs = conn.prepareStatement("SELECT firebase_uid FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) {
            val uid = rs.getString("firebase_uid")
            if (!uid.isNullOrBlank()) {
                try { FirebaseAuth.getInstance().deleteUser(uid) } catch (e: Exception) { println("Firebase Delete Error: ${e.message}") }
            }
        }
        // 2. Delete Profile
        conn.prepareStatement("DELETE FROM customers WHERE id = ?").apply { setInt(1, id); executeUpdate() }
    }
}


