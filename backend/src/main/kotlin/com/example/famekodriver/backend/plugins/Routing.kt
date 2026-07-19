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
import com.example.famekodriver.backend.db.DatabaseRepository
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
import org.mindrot.jbcrypt.BCrypt

val sosFlow = MutableSharedFlow<SOSAlert>(extraBufferCapacity = 64)

val PAYSTACK_SECRET = System.getenv("PAYSTACK_SECRET") ?: ""
val IS_PRODUCTION = System.getenv("PRODUCTION") == "true"

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
                val admin = DatabaseRepository.loginAdmin(email, password)
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
                    
                    val drivers = DatabaseRepository.getAllDrivers(region)
                    val fleetOwners = DatabaseRepository.getAllFleetOwners(region)
                    val stats = DatabaseRepository.getPlatformStats()
                    val rentals = DatabaseRepository.getAllRentals(region)
                    val liveDeliveries = DatabaseRepository.getActiveDeliveries()
                    val sosCount = RedisManager.getActiveSOSCount()

                    call.respond(ThymeleafContent("admin_dashboard", mapOf<String, Any>(
                        "drivers" to drivers.take(10),
                        "totalDrivers" to drivers.size,
                        "totalFleetOwners" to fleetOwners.size,
                        "pendingCount" to drivers.count { it["status"] == "PENDING" || it["status"] == "PENDING_DOCS" },
                        "pendingFleetOwners" to fleetOwners.count { it["status"] == "PENDING" },
                        "onlineCount" to drivers.count { it["is_online"] == true },
                        "totalRevenue" to (stats["totalRevenue"] ?: 0),
                        "totalDebt" to (stats["totalDebt"] ?: 0),
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
                    
                    val allDrivers = DatabaseRepository.getAllDrivers(principalRegion)
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
                    val driver = DatabaseRepository.getDriverDetails(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(ThymeleafContent("admin_driver_details", mapOf("driver" to driver, "activePage" to "drivers", "admin" to call.principal<AdminPrincipal>()!!)))
                }

                post("/driver/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val p = call.receiveParameters()
                    val category = p["category"] ?: "Economy"
                    
                    DatabaseRepository.approveDriver(id, category)
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateDriverStatus(id, "REJECTED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/suspend/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateDriverStatus(id, "SUSPENDED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/release/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateDriverStatus(id, "APPROVED")
                    call.respondRedirect("/admin/driver/$id")
                }

                post("/driver/terminate/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.terminateDriverAccount(id)
                    call.respondRedirect("/admin/drivers")
                }

                post("/customer/terminate/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.terminateCustomerAccount(id)
                    call.respondRedirect("/admin/customers")
                }

                // --- FLEET OWNERS ---
                get("/fleet-owners") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val statusFilter = call.parameters["status"]
                    val regionFilter = call.parameters["region"]
                    val principalRegion = if (principal.role == "REGIONAL_ADMIN") principal.region else null

                    val allOwners = DatabaseRepository.getAllFleetOwners(principalRegion)
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
                    val owner = DatabaseRepository.getFleetOwnerDetails(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(ThymeleafContent("admin_fleet_owner_details", mapOf(
                        "owner" to owner,
                        "activePage" to "fleet-owners",
                        "admin" to call.principal<AdminPrincipal>()!!
                    )))
                }

                post("/fleet-owner/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateFleetOwnerStatus(id, "APPROVED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateFleetOwnerStatus(id, "REJECTED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/suspend/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateFleetOwnerStatus(id, "SUSPENDED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/release/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateFleetOwnerStatus(id, "APPROVED")
                    call.respondRedirect("/admin/fleet-owner/$id")
                }

                post("/fleet-owner/terminate/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.terminateFleetOwnerAccount(id)
                    call.respondRedirect("/admin/fleet-owners")
                }


                get("/map") {
                    val principal = call.principal<AdminPrincipal>()!!
                    call.respond(ThymeleafContent("admin_map", mapOf("activePage" to "map", "admin" to principal)))
                }

                get("/live-locations") {
                    call.respond(DatabaseRepository.getLiveLocations())
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
                        "customers" to DatabaseRepository.getAllCustomers(),
                        "activePage" to "customers",
                        "admin" to principal
                    )))
                }

                get("/deliveries") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val filterType = call.parameters["type"] ?: "RIDE_HAILING"
                    
                    val allDeliveries = DatabaseRepository.getActiveDeliveries()
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
                        "payments" to DatabaseRepository.getPendingPayments(),
                        "activePage" to "daily-payments",
                        "admin" to principal
                    )))
                }

                post("/daily-payments/approve") {
                    val p = call.receiveParameters()
                    val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.approvePayment(id)
                    call.respondRedirect("/admin/daily-payments")
                }

                post("/daily-payments/reject") {
                    val p = call.receiveParameters()
                    val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.rejectPayment(id)
                    call.respondRedirect("/admin/daily-payments")
                }

                get("/rentals") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val region = if (principal.role == "REGIONAL_ADMIN") principal.region else null
                    call.respond(ThymeleafContent("admin_rentals", mapOf(
                        "rentals" to DatabaseRepository.getAllRentals(region),
                        "activePage" to "rentals",
                        "admin" to principal
                    )))
                }

                post("/rentals/approve/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateRentalStatus(id, "ACTIVE")
                    notifyRentalStatusUpdate(id, "ACTIVE")
                    call.respondRedirect("/admin/rentals")
                }

                post("/rentals/reject/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateRentalStatus(id, "REJECTED")
                    notifyRentalStatusUpdate(id, "REJECTED")
                    call.respondRedirect("/admin/rentals")
                }

                get("/pricing") {
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    val config = DatabaseRepository.getPricingConfig()
                    call.respond(ThymeleafContent("admin_pricing", mapOf(
                        "pricing" to config,
                        "rentalRates" to DatabaseRepository.getRentalRates(),
                        "rideCategories" to DatabaseRepository.getRideCategories(),
                        "activePage" to "pricing",
                        "admin" to principal,
                        "showSuccess" to (call.parameters["success"] == "true")
                    )))
                }

                post("/pricing/update") {
                    val p = call.receiveParameters()
                    DatabaseRepository.updatePricingConfig(
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

                post("/pricing/category/update") {
                    val p = call.receiveParameters()
                    val serviceId = p["serviceId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.updateRideCategoryPricing(
                        serviceId = serviceId,
                        baseFare = p["baseFare"]?.toDoubleOrNull() ?: 0.0,
                        perKmRate = p["perKmRate"]?.toDoubleOrNull() ?: 0.0,
                        perMinuteRate = p["perMinuteRate"]?.toDoubleOrNull() ?: 0.0,
                        minFare = p["minFare"]?.toDoubleOrNull() ?: 0.0,
                        commission = p["driverCommission"]?.toDoubleOrNull() ?: 15.0
                    )
                    call.respondRedirect("/admin/pricing?success=true")
                }

                post("/pricing/update-rentals") {
                    val p = call.receiveParameters()
                    DatabaseRepository.updatePricingConfig(
                        baseFare = null, perKmRate = null, perMinuteRate = null, minFare = null,
                        milestoneInterval = null, milestoneDiscount = null, driverCommission = null,
                        peakMultiplier = null, dailyServiceFee = null,
                        rentalOwnerComm = p["rentalOwnerCommission"]?.toDoubleOrNull(),
                        rentalGuestFee = p["rentalCustomerFee"]?.toDoubleOrNull()
                    )
                    
                    // Update individual vehicle rates
                    p.entries().forEach { (key, values) ->
                        if (key.startsWith("rate_")) {
                            val type = key.removePrefix("rate_")
                            val rate = values.firstOrNull()?.toDoubleOrNull() ?: 0.0
                            DatabaseRepository.updateRentalRate(type, rate)
                        }
                    }
                    call.respondRedirect("/admin/pricing?success=true")
                }

                get("/financials") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val month = call.parameters["month"]
                    val date = call.parameters["date"]
                    
                    val stats = DatabaseRepository.getFinancialStats(month, date)

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
                    val settings = DatabaseRepository.getSystemSettings()
                    call.respond(ThymeleafContent("admin_settings", mapOf(
                        "activePage" to "settings",
                        "admin" to principal,
                        "settings" to settings,
                        "showSuccess" to (call.parameters["success"] == "true")
                    )))
                }

                post("/settings/update") {
                    val p = call.receiveParameters()
                    DatabaseRepository.updateSystemSettings(
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
                    call.respond(ThymeleafContent("admin_manage", mapOf("admins" to DatabaseRepository.getAllAdmins(), "activePage" to "admins", "admin" to principal)))
                }

                post("/admins/create") {
                    val p = call.receiveParameters()
                    DatabaseRepository.createAdmin(p["username"]!!, p["email"]!!, p["password"]!!, p["role"]!!, p["region"])
                    call.respondRedirect("/admin/admins")
                }

                get("/fleet") {
                    val principal = call.principal<AdminPrincipal>()!!
                    val vehicles = DatabaseRepository.getAllFleetVehicles()
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
                    DatabaseRepository.updateFleetVehicle(
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
                    DatabaseRepository.deleteFleetVehicle(id)
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
                    if (IS_PRODUCTION) return@post call.respond(HttpStatusCode.Forbidden, "Simulations disabled in production")
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
                    if (IS_PRODUCTION) return@get call.respond(HttpStatusCode.Forbidden, "Database Explorer disabled in production")
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    
                    val tables = DatabaseRepository.getAllTables()
                    call.respond(ThymeleafContent("admin_db_explorer", mapOf<String, Any>(
                        "activePage" to "database",
                        "admin" to principal,
                        "tables" to tables,
                        "selectedTable" to ""
                    )))
                }

                get("/database/{tableName}") {
                    if (IS_PRODUCTION) return@get call.respond(HttpStatusCode.Forbidden, "Database Explorer disabled in production")
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respondRedirect("/admin/dashboard")
                    
                    val tableName = call.parameters["tableName"] ?: return@get call.respondRedirect("/admin/database")
                    val tables = DatabaseRepository.getAllTables()
                    
                    if (tableName !in tables) return@get call.respondRedirect("/admin/database")
                    
                    val (columns, rows) = DatabaseRepository.getTableData(tableName)
                    
                    call.respond(ThymeleafContent("admin_db_explorer", mapOf<String, Any>(
                        "activePage" to "database",
                        "admin" to principal,
                        "tables" to tables,
                        "selectedTable" to tableName,
                        "columns" to columns,
                        "rows" to rows
                    )))
                }

                get("/api/database/{tableName}") {
                    if (IS_PRODUCTION) return@get call.respond(HttpStatusCode.Forbidden, "API Access disabled in production")
                    val principal = call.principal<AdminPrincipal>()!!
                    if (principal.role != "SUPERADMIN") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val tableName = call.parameters["tableName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val tables = DatabaseRepository.getAllTables()
                    if (tableName !in tables) return@get call.respond(HttpStatusCode.NotFound)
                    
                    val (columns, rows) = DatabaseRepository.getTableData(tableName)
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

                get("/active-support") {
                    val allConvIds = RedisManager.getActiveConversationIds()
                    val supportConvIds = allConvIds.filter { it >= 1000000 }
                    
                    val driversWithActiveChat = supportConvIds.mapNotNull { convId ->
                        val driverId = convId - 1000000
                        val driver = DatabaseRepository.getDriverProfile(driverId)
                        if (driver != null) {
                            mapOf(
                                "driverId" to driverId,
                                "name" to (driver["name"] ?: "Driver $driverId"),
                                "phone" to (driver["phone"] ?: ""),
                                "profilePic" to (driver["profilePicture"] ?: ""),
                                "conversationId" to convId
                            )
                        } else null
                    }
                    call.respond(driversWithActiveChat)
                }

                get("/support") {
                    val principal = call.principal<AdminPrincipal>()!!
                    call.respond(ThymeleafContent("admin_chat", mapOf(
                        "activePage" to "support",
                        "admin" to principal
                    )))
                }
            }
        }

        // --- API ROUTES ---

        route("/api/admin") {
            get("/live-locations") {
                call.respond(DatabaseRepository.getLiveLocations())
            }

            get("/active-sos") {
                call.respond(RedisManager.getAllSOS())
            }

            get("/pending-drivers") {
                val drivers = DatabaseRepository.getAllDrivers(null).filter { it["status"] == "PENDING" || it["status"] == "PENDING_DOCS" }
                call.respond(drivers)
            }

            get("/stats") {
                val drivers = DatabaseRepository.getAllDrivers(null)
                val stats = DatabaseRepository.getPlatformStats()
                val liveDeliveries = DatabaseRepository.getActiveDeliveries()
                
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
                call.respond(DatabaseRepository.getPendingPayments())
            }

            get("/customers") {
                call.respond(DatabaseRepository.getAllCustomers())
            }

            get("/deliveries") {
                call.respond(DatabaseRepository.getActiveDeliveries())
            }

            get("/driver/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val driver = DatabaseRepository.getDriverDetails(id)
                if (driver != null) call.respond(driver) else call.respond(HttpStatusCode.NotFound)
            }

            get("/financials") {
                call.respond(DatabaseRepository.getFinancialStats(null, null))
            }
        }

        get("/api/weather") {
            val lat = call.parameters["lat"]?.toDoubleOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing lat")
            val lon = call.parameters["lon"]?.toDoubleOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing lon")
            
            try {
                val apiKey = System.getenv("OPENWEATHER_API_KEY") ?: throw IllegalStateException("OPENWEATHER_API_KEY not set")
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
                call.respond(DatabaseRepository.getAllRentals(region))
            }

            post("/payments/approve/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.approvePayment(id)
                call.respond(mapOf("success" to true))
            }

            post("/payments/reject/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.rejectPayment(id)
                call.respond(mapOf("success" to true))
            }

            post("/driver/approve/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateDriverStatus(id, "APPROVED")
                call.respond(mapOf("success" to true))
            }

            post("/driver/reject/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateDriverStatus(id, "REJECTED")
                call.respond(mapOf("success" to true))
            }
        }

        post("/customer/login") {
            try {
                val req = call.receive<LoginRequest>()
                val phone = req.phone
                val password = req.password
                if (phone.isNullOrBlank() || password.isNullOrBlank()) {
                    call.respond(AuthResponse(false, "Phone and password are required", null, null))
                    return@post
                }
                
                call.respond(DatabaseRepository.loginCustomer(phone, password))
            } catch (e: Throwable) {
                call.respond(AuthResponse(false, "Login error: ${e.localizedMessage}", null, null))
            }
        }



        post("/customer/google-login") {
            val req = call.receive<LoginRequest>()
            val idToken = req.googleToken
            if (idToken.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Google token is required", null, null))
                return@post
            }

            val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: throw IllegalStateException("GOOGLE_CLIENT_ID not set")
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(listOf(googleClientId))
                .build()

            try {
                val token = verifier.verify(idToken)
                if (token != null) {
                    val payload = token.payload
                    val email = payload.email
                    val uid = payload.subject // Google unique ID
                    
                    val user = DatabaseRepository.syncCustomerWithFirebase(email, uid)
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
                val userId = DatabaseRepository.registerCustomer(req)
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
            val profile = DatabaseRepository.getCustomerProfile(id)
            if (profile != null) {
                call.respond(profile)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Customer not found"))
            }
        }

        post("/customer/update-profile") {
            try {
                val p = call.receiveParameters()
                val idStr = p["customer_id"] ?: ""
                val id = idStr.toIntOrNull() ?: return@post call.respond(AuthResponse(false, "Invalid ID format: $idStr", null, null))
                
                val name = p["name"] ?: ""
                val email = p["email"] ?: ""
                val phone = DatabaseRepository.normalizePhone(p["phone"])
                val address = p["address"] ?: ""
                val region = p["region"] ?: ""
                val profilePicture = p["profile_picture"]

                println("DEBUG: Updating profile for customer ID: $id")
                val success = DatabaseRepository.updateCustomerProfile(
                    id = id,
                    name = name,
                    email = email,
                    phone = phone,
                    address = address,
                    region = region,
                    profilePicture = profilePicture
                )
                
                if (success) {
                    call.respond(AuthResponse(true, "Profile updated successfully", id.toString(), name))
                } else {
                    val exists = DatabaseRepository.getCustomerProfile(id) != null
                    val errorMsg = if (!exists) "Customer ID $id not found" else "Update failed (Check for duplicate email or phone)"
                    println("DEBUG: Profile update failed for ID $id. Exists: $exists")
                    call.respond(AuthResponse(false, errorMsg, null, null))
                }
            } catch (e: Exception) {
                println("DEBUG: Profile update error: ${e.message}")
                call.respond(AuthResponse(false, "Server Error: ${e.message}", null, null))
            }
        }

        get("/customer/saved-places/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(DatabaseRepository.getSavedPlaces(customerId))
        }

        get("/customer/trips/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(DatabaseRepository.getCustomerTrips(customerId))
        }

        post("/customer/saved-places") {
            try {
                val req = call.receive<SavedPlace>()
                val success = DatabaseRepository.addSavedPlace(req)
                call.respond(mapOf("success" to success))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
            }
        }

        put("/customer/saved-places/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<SavedPlace>()
                val success = DatabaseRepository.updateSavedPlace(id, req)
                call.respond(mapOf<String, Any>("success" to success))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
            }
        }

        delete("/customer/saved-places/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val success = DatabaseRepository.deleteSavedPlace(id)
            call.respond(mapOf<String, Any>("success" to success))
        }

        get("/driver/profile/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val profile = DatabaseRepository.getDriverProfile(id)
            if (profile != null) {
                call.respond(profile)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Driver not found"))
            }
        }

        post("/driver/login") {
            try {
                val req = call.receive<LoginRequest>()
                val phone = req.phone
                val password = req.password
                if (phone.isNullOrBlank() || password.isNullOrBlank()) {
                    call.respond(AuthResponse(false, "Phone and password are required", null, null))
                    return@post
                }

                call.respond(DatabaseRepository.loginDriver(phone, password))
            } catch (e: Throwable) {
                call.respond(AuthResponse(false, "Login error: ${e.localizedMessage}", null, null))
            }
        }



        post("/driver/google-login") {
            val req = call.receive<LoginRequest>()
            val idToken = req.googleToken
            if (idToken.isNullOrBlank()) {
                call.respond(AuthResponse(false, "Google token is required", null, null))
                return@post
            }

            val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: throw IllegalStateException("GOOGLE_CLIENT_ID not set")
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
                    
                    val user = DatabaseRepository.syncDriverWithFirebase(email, uid)
                    if (user != null) {
                        call.respond(AuthResponse(
                            success = true, 
                            message = "Success", 
                            user_id = user.id.toString(), 
                            name = user.fullName,
                            status = user.status,
                            user_role = user.userRole,
                            vehicle_type = user.vehicleType
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
                val driverId = DatabaseRepository.registerDriver(data)
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

        get("/pricing/config") { call.respond(DatabaseRepository.getPricingConfig()) }

        route("/api/v1") {
            get("/products") {
                call.respond(DatabaseRepository.getAllProducts())
            }
            get("/products/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val product = DatabaseRepository.getProduct(id)
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

            val config = DatabaseRepository.getPricingConfig()
            val peak = config.peakMultiplier
            
            val categories = DatabaseRepository.getRideCategories()
            val estimates = categories
                .filter { cat ->
                    // Set distance limits for specific vehicle types
                    // Pragya (tricycle) is limited to 10km
                    if (cat.serviceId.equals("Pragya", ignoreCase = true) && dist > 10.0) return@filter false
                    // Bicycle is limited to 5km
                    if (cat.serviceId.equals("Bicycle", ignoreCase = true) && dist > 5.0) return@filter false
                    true
                }
                .map { cat ->
                    // Find nearest driver for this category to get real ETA
                    val nearest = if (lat != 0.0) {
                        DatabaseRepository.getNearbyDrivers(lat, lng, 10.0, cat.serviceId).firstOrNull()
                    } else null
                    
                    val eta = nearest?.pickupEtaMin?.toInt() ?: 5 // Fallback to 5 if no drivers nearby or location missing

                    RideEstimateResponse(
                        serviceId = cat.serviceId,
                        name = cat.name,
                        description = cat.description ?: "",
                        fare = max(cat.minFare, (cat.baseFare + (dist * cat.perKmRate) + (dur * cat.perMinuteRate)) * peak),
                        pickupEtaMin = eta,
                        icon = cat.icon ?: "car",
                        serviceType = cat.serviceType,
                        isAvailableInRegion = true,
                        availabilityStatus = if (nearest != null) "AVAILABLE" else "BUSY"
                    )
                }
            
            call.respond(estimates)
        }

        route("/orders") {
            post("/create") {
                try {
                    val req = call.receive<OrderCreateRequest>()

                    // Enforce distance limits for specific vehicle types
                    if (req.requestedVehicleType?.equals("Pragya", ignoreCase = true) == true && req.distanceKm > 10.0) {
                        return@post call.respond(mapOf("success" to false, "message" to "Pragya is only available for trips under 10km"))
                    }
                    if (req.requestedVehicleType?.equals("Bicycle", ignoreCase = true) == true && req.distanceKm > 5.0) {
                        return@post call.respond(mapOf("success" to false, "message" to "Bicycle is only available for trips under 5km"))
                    }

                    println("DEBUG: New Order Request from Customer ${req.customerId}: Pickup=(${req.pickupLat}, ${req.pickupLng}), Type=${req.requestedVehicleType}")
                    val pin = (1000..9999).random().toString()
                    
                    val orderData = DatabaseRepository.createOrder(req, pin)
                    if (orderData != null) {
                        val (orderId, deliveryId) = orderData
                        val isScheduled = !req.scheduledTime.isNullOrBlank()
                        val initialStatus = if (isScheduled) "SCHEDULED" else "PENDING"

                        // 3. Notify Nearby Drivers (Only if NOT scheduled)
                        if (!isScheduled) {
                            application.launch {
                                var attempt = 1
                                var currentRadius = 3.0 // Start small
                                val maxAttempts = 3
                                
                                val config = DatabaseRepository.getPricingConfig()
                                val commissionPercent = config.driverCommissionPercent
                                val driverEarnings = if (req.serviceType == ServiceType.RENTAL) {
                                    req.estimatedFare * (1.0 - (commissionPercent / 100.0))
                                } else {
                                    req.estimatedFare
                                }

                                while (attempt <= maxAttempts) {
                                    println("DEBUG: Dispatch attempt $attempt for order $orderId (radius: ${currentRadius}km)")
                                    val nearby = DatabaseRepository.getNearbyDrivers(req.pickupLat, req.pickupLng, currentRadius, req.requestedVehicleType)
                                    val bestEta = nearby.firstOrNull()?.pickupEtaMin ?: 5.0

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
                                        pickupEtaMin = bestEta,
                                        customerName = DatabaseRepository.getCustomerName(req.customerId.toInt()) ?: "Customer",
                                        customerPhone = DatabaseRepository.getCustomerPhone(req.customerId.toInt()) ?: "",
                                        customerProfilePic = DatabaseRepository.getCustomerProfilePic(req.customerId.toInt()),
                                        totalFare = req.estimatedFare
                                    )

                                    // Try to lock and offer to multiple drivers (top 5)
                                    var offersCount = 0
                                    val maxOffersPerAttempt = 5

                                    for (driver in nearby) {
                                        if (RedisManager.tryLockDriver(driver.id, ttlSeconds = 20)) {
                                            println("DEBUG: Offering order $orderId to DRIVER_${driver.id}")
                                            sendToUser("DRIVER_${driver.id}", "NEW_DELIVERY", deliveryObj)
                                            offersCount++
                                            if (offersCount >= maxOffersPerAttempt) break
                                        }
                                    }

                                    if (offersCount > 0) {
                                        delay(15000)
                                        val status = DatabaseRepository.getOrderStatus(orderId)
                                        if (status != "PENDING") {
                                            println("DEBUG: Order $orderId no longer pending ($status). Dispatch finished.")
                                            break
                                        } else {
                                            println("DEBUG: Order $orderId still pending after 15s delay. Expanding search...")
                                        }
                                    } else {
                                        println("DEBUG: No available/unlocked drivers found for order $orderId in radius ${currentRadius}km. Immediate expansion.")
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
                                
                                if (DatabaseRepository.getOrderStatus(orderId) == "PENDING" && attempt > maxAttempts) {
                                    println("DEBUG: Order $orderId timed out after $maxAttempts attempts.")
                                    sendToUser("CUSTOMER_${req.customerId}", "ORDER_TIMEOUT", mapOf("orderId" to orderId))
                                }
                            }
                        }

                        call.respond(mapOf("success" to true, "orderId" to orderId, "status" to initialStatus))
                    } else {
                        call.respond(mapOf("success" to false, "message" to "Failed to create order record"))
                    }
                } catch (e: Exception) {
                    application.log.error("Order creation failed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to e.localizedMessage))
                }
            }

            get("/status/{orderId}") {
                val orderId = call.parameters["orderId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val details = DatabaseRepository.getOrderStatusDetails(orderId)
                if (details != null) {
                    call.respond(details)
                } else {
                    call.respond(mapOf("success" to false, "message" to "Order not found"))
                }
            }

            post("/cancel/{orderId}") {
                val orderId = call.parameters["orderId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val driverId = DatabaseRepository.getDriverIdForOrder(orderId)
                
                DatabaseRepository.cancelOrder(orderId)
                RedisManager.setChatRetention(orderId)
                
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
                
                val success = DatabaseRepository.verifyOrderPin(orderId, pin)
                call.respond(mapOf("success" to success, "message" to if (success) "" else "Invalid PIN"))
            }
        }

        post("/paystack-webhook") {
            // Deprecated - using /paystack/webhook instead
            call.respond(HttpStatusCode.MovedPermanently)
        }

        route("/fleet") {
            get("/vehicles") {
                val ownerId = call.parameters["owner_id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val role = call.parameters["role"]
                val vehicles = DatabaseRepository.getFleetVehicles(ownerId, role)
                call.respond(vehicles)
            }
            post("/add-vehicle") {
                try {
                    val p = call.receiveParameters()
                    val ownerId = p["owner_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    DatabaseRepository.addVehicleToFleet(
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
                        fuelType = p["fuel_type"],
                        ownerRole = p["owner_role"]
                    )
                    call.respond(mapOf("success" to true))
                } catch (e: Exception) {
                    println("Error adding vehicle: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to (e.message ?: "Server Error")))
                }
            }
            post("/update-vehicle") {
                val p = call.receiveParameters()
                val vehicleId = p["vehicle_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateFleetVehicle(
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
                val vehicles = DatabaseRepository.getAllFleetVehicles()
                call.respond(vehicles)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val rental = DatabaseRepository.getRentalById(id)
                if (rental != null) {
                    call.respond(mapOf("success" to true, "rental" to rental))
                } else {
                    call.respond(mapOf("success" to false))
                }
            }
            get("/rates") {
                call.respond(DatabaseRepository.getRentalRates())
            }
            post("/book") {
                try {
                    val req = call.receive<RentalBookRequest>()
                    application.log.info("Processing rental booking: customer=${req.customerId}, vehicle=${req.vehicleId}, total=${req.totalPrice}")
                    
                    val bookingCode = (1000..9999).random().toString()
                    
                    val rentalId = DatabaseRepository.bookRental(req, bookingCode)
                    if (rentalId != null) {
                        // Notify Owner
                        try {
                            val ownerInfo = DatabaseRepository.getVehicleOwnerInfo(req.vehicleId)
                            if (ownerInfo != null) {
                                val (ownerId, vName) = ownerInfo
                                val token = DatabaseRepository.getUserFcmToken(ownerId, "driver")
                                if (token != null) PushNotificationHelper.sendNotification(token, "New Rental Booking", "Your vehicle $vName has been booked.")
                            }
                        } catch (_: Exception) {}

                        val customerEmail = DatabaseRepository.getCustomerEmail(req.customerId) ?: "customer_${req.customerId}@example.com"
                        val reference = "RENTAL_${rentalId}_${System.currentTimeMillis()}"
                        
                        val paystackData = if (req.paymentMethod == "ELECTRONIC") {
                            try { 
                                DatabaseRepository.initializePaystackPayment(customerEmail, req.totalPrice, reference)
                            } catch (e: Exception) {
                                application.log.error("Paystack initialization failed", e)
                                null
                            }
                        } else null
                        
                        call.respond(mapOf(
                            "success" to true, 
                            "rentalId" to rentalId, 
                            "bookingCode" to bookingCode,
                            "checkoutUrl" to paystackData?.first,
                            "accessCode" to paystackData?.second
                        ))
                    } else {
                        call.respond(mapOf("success" to false, "message" to "Failed to create rental record"))
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
                val activeRental = DatabaseRepository.getActiveRental(customerId)
                if (activeRental != null) {
                    call.respond(mapOf("success" to true, "rental" to activeRental))
                } else {
                    call.respond(mapOf("success" to false))
                }
            }
            post("/cancel/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateRentalStatus(id, "CANCELLED")
                call.respond(mapOf("success" to true))
            }
            post("/end/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateRentalStatus(id, "COMPLETED")
                notifyRentalEnded(id, isManual = true)
                call.respond(mapOf("success" to true))
            }
            post("/update-location") {
                val p = call.receiveParameters()
                val vehicleId = p["vehicleId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val lat = p["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = p["lng"]?.toDoubleOrNull() ?: 0.0
                DatabaseRepository.updateRentalVehicleLocation(vehicleId, lat, lng)
                call.respond(mapOf("success" to true))
            }
            post("/update-status") {
                val p = call.receiveParameters()
                val id = p["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val status = p["status"] ?: ""
                DatabaseRepository.updateRentalStatus(id, status)
                call.respond(mapOf("success" to true))
            }
            post("/update-destination") {
                val p = call.receiveParameters()
                val rentalId = p["rentalId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val location = p["location"] ?: ""
                val lat = p["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = p["lng"]?.toDoubleOrNull() ?: 0.0
                val stops = p["stops"]
                
                val success = DatabaseRepository.updateRentalDestination(rentalId, location, lat, lng, stops)
                call.respond(mapOf("success" to success))
            }

            get("/history/{customerId}") {
                val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(DatabaseRepository.getRentalHistory(customerId))
            }
        }

        get("/customer/rentals/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(DatabaseRepository.getRentalHistory(customerId))
        }

        route("/driver") {
            post("/update-vehicle") {
                val p = call.receiveParameters()
                val id = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                DatabaseRepository.updateDriverVehicle(
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
                val rating = p["rating"]?.toFloatOrNull() ?: 5.0f
                
                val success = DatabaseRepository.submitRating(driverId, rating)
                call.respond(AuthResponse(success, if (success) "Rating submitted" else "Failed to submit rating", null, null))
            }

            get("/nearby") {
                val lat = call.parameters["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = call.parameters["lng"]?.toDoubleOrNull() ?: 0.0
                call.respond(DatabaseRepository.getNearbyDrivers(lat, lng, call.parameters["radius"]?.toDoubleOrNull() ?: 5.0))
            }
            post("/update-online-status") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: ""
                val online = p["is_online"]?.toBoolean() ?: false
                
                val error = DatabaseRepository.updateDriverOnlineStatus(driverId, online)
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
                DatabaseRepository.updateDriverLocation(driverIdStr, lat, lng, bearing)
                
                // Notify active customer if any
                val driverId = driverIdStr.toIntOrNull()
                if (driverId != null) {
                    val customerId = DatabaseRepository.getActiveCustomerIdForDriver(driverId)
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
                var deliveries = DatabaseRepository.getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                
                if (deliveries.isEmpty()) {
                    radius = 7.0 // Expand to 7km
                    deliveries = DatabaseRepository.getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }
                
                if (deliveries.isEmpty()) {
                    radius = 15.0 // Expand to 15km
                    deliveries = DatabaseRepository.getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }

                if (deliveries.isEmpty()) {
                    radius = 50.0 // Max expansion to 50km for remote areas
                    deliveries = DatabaseRepository.getAvailableDeliveriesByRadius(lat, lng, radius, vehicleType, vehicleCategory)
                }
                
                call.respond(deliveries)
            }
            post("/pay-daily-fee") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                
                val config = DatabaseRepository.getPricingConfig()
                val amount = config.dailyServiceFee
                
                val settings = DatabaseRepository.getSystemSettings()
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
                    val email = DatabaseRepository.getDriverEmail(driverId) ?: "driver_$driverId@example.com"
                    val reference = "DAILY_FEE_${driverId}_${System.currentTimeMillis()}"
                    val paystackData = DatabaseRepository.initializePaystackPayment(email, amount, reference)
                    
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
                val status = DatabaseRepository.getDriverStatus(id)
                if (status != null) call.respond(status) else call.respond(HttpStatusCode.NotFound)
            }
            get("/stats/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val stats = DatabaseRepository.getDriverStats(id)
                if (stats != null) call.respond(stats) else call.respond(HttpStatusCode.NotFound)
            }
            post("/upload-document") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val docType = p["doc_type"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val fileUrl = p["file_url"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                DatabaseRepository.updateDriverDocument(driverId, docType, fileUrl)
                call.respond(AuthResponse(true, "Document updated successfully", null, null))
            }
            post("/update-emergency-contacts") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val c1 = p["contact1"] ?: ""
                val c2 = p["contact2"] ?: ""
                
                DatabaseRepository.updateEmergencyContacts(driverId, c1, c2)
                call.respond(AuthResponse(true, "Contacts updated successfully", null, null))
            }

            post("/accept-delivery") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val deliveryId = p["delivery_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                
                val response = DatabaseRepository.acceptDelivery(driverId, deliveryId)
                if (response.success) {
                    val orderId = response.user_id?.toInt()
                    if (orderId != null) {
                        val customerId = DatabaseRepository.getCustomerIdForOrder(orderId)
                        if (customerId != null) {
                            sendToUser("CUSTOMER_$customerId", "ORDER_ACCEPTED", mapOf("orderId" to orderId, "deliveryId" to deliveryId))
                        }
                    }
                }
                call.respond(response)
            }

            get("/my-deliveries/{id}") {
                val driverId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(DatabaseRepository.getDriverDeliveries(driverId))
            }

            route("/rentals") {
                get("/{driverId}") {
                    val driverId = call.parameters["driverId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    call.respond(DatabaseRepository.getDriverRentals(driverId))
                }

                post("/verify") {
                    val p = call.receiveParameters()
                    val rentalId = p["rentalId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val code = p["code"] ?: ""
                    
                    val success = DatabaseRepository.verifyRentalBooking(rentalId, code)
                    call.respond(mapOf("success" to success, "message" to if (success) "" else "Invalid code"))
                }
            }
        }

        route("/delivery") {
            post("/update-status") {
                val p = call.receiveParameters()
                val deliveryId = p["delivery_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val status = p["status"] ?: ""
                
                val result = DatabaseRepository.updateDeliveryStatus(deliveryId, status)
                if (result != null) {
                    val (orderId, _) = result
                    if (status == "DELIVERED" || status == "CANCELLED") {
                        RedisManager.setChatRetention(orderId)
                    }
                    
                    val customerId = DatabaseRepository.getCustomerIdForOrder(orderId)
                    if (customerId != null) {
                        sendToUser("CUSTOMER_$customerId", "ORDER_STATUS_UPDATE", mapOf("orderId" to orderId, "status" to status))
                    }
                    call.respond(AuthResponse(true, "Status updated", null, null))
                } else {
                    call.respond(AuthResponse(false, "Failed to update status", null, null))
                }
            }
        }

        // --- WEBHOOKS ---
        post("/paystack/webhook") {
            try {
                val body = call.receiveText()
                val signature = call.request.headers["x-paystack-signature"]
                
                if (signature == null || !DatabaseRepository.verifyPaystackSignature(body, signature)) {
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
                        DatabaseRepository.updateRentalStatus(rentalId, "BOOKED")
                        
                        // Record Payment
                        DatabaseRepository.recordPayment(
                            userId = DatabaseRepository.getCustomerIdForRental(rentalId) ?: 0,
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
                val driverId = call.parameters["driverId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(mapOf("balance" to DatabaseRepository.getWalletBalance(driverId)))
            }

            get("/transactions/{driverId}") {
                val driverId = call.parameters["driverId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(DatabaseRepository.getWalletTransactions(driverId))
            }

            post("/request-topup") {
                val p = call.receiveParameters()
                val driverId = p["driver_id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val amount = p["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reference = p["reference"] ?: ""
                val type = if (reference.startsWith("DF_")) "DAILY_FEE" else "TOPUP"

                val topupId = DatabaseRepository.requestWalletTopup(driverId, amount, reference, type)
                
                val settings = DatabaseRepository.getSystemSettings()
                val mode = settings["paystack_mode"] as? String ?: "TEST"
                
                if (mode == "TEST" && type == "DAILY_FEE" && topupId != null) {
                    DatabaseRepository.approvePayment(topupId)
                    call.respond(AuthResponse(true, "Approved automatically (Test Mode)", null, null))
                } else {
                    call.respond(AuthResponse(true, "Request submitted for approval", null, null))
                }
            }

            get("/topup-history/{driverId}") {
                val driverId = call.parameters["driverId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(DatabaseRepository.getTopupHistory(driverId))
            }
        }

        route("/safety") {
            post("/sos") {
                val req = call.receive<SOSRequest>()
                val driver = DatabaseRepository.getDriverForSos(req.driverId.toInt())
                if (driver != null) {
                    val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                    val alert = SOSAlert(
                        id = id,
                        driverId = req.driverId,
                        driverName = driver["name"] ?: "",
                        driverPhone = driver["phone"] ?: "",
                        lat = req.latitude,
                        lng = req.longitude,
                        time = System.currentTimeMillis(),
                        type = req.type,
                        plateNumber = driver["plate"] ?: "",
                        vehicleModel = driver["model"] ?: ""
                    )
                    RedisManager.addSOS(alert)
                    sosFlow.tryEmit(alert)
                    call.respond(AuthResponse(true, "SOS Triggered", null, null))
                } else {
                    call.respond(AuthResponse(false, "Driver not found", null, null))
                }
            }

            get("/share-trip/{driverId}/{deliveryId}") {
                val driverId = call.parameters["driverId"] ?: "0"
                val deliveryId = call.parameters["deliveryId"] ?: "0"
                val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"
                val shareUrl = "$baseUrl/track/$driverId/$deliveryId"
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
            
            DatabaseRepository.updateFcmToken(userId, type, token)
            call.respond(mapOf("success" to true))
        }

        route("/chat") {
            post("/send") {
                val msg = call.receive<Message>()
                val savedMsg = msg.copy(
                    id = (System.currentTimeMillis() % 1000000).toInt(), // Temp ID
                    createdAt = java.time.Instant.now().toString()
                )
                
                // Store in Redis instead of Postgres
                RedisManager.saveChatMessage(savedMsg.conversationId, com.google.gson.Gson().toJson(savedMsg))
                
                // 1. Find recipient
                if (savedMsg.conversationId >= 1000000) {
                    // Support Chat (Admin <-> Driver)
                    if (savedMsg.senderType == "driver") {
                        // Notify all online admins
                        broadcastToAdmins("NEW_MESSAGE", savedMsg)
                    } else if (savedMsg.senderType == "admin") {
                        // Notify specific driver
                        val driverId = savedMsg.conversationId - 1000000
                        sendToUser("DRIVER_$driverId", "NEW_MESSAGE", savedMsg)
                    }
                } else {
                    // Trip Chat (Customer <-> Driver)
                    val recipientId = if (savedMsg.senderType == "customer") {
                        DatabaseRepository.getDriverIdForOrder(savedMsg.conversationId)?.let { "DRIVER_$it" }
                    } else {
                        DatabaseRepository.getCustomerIdForOrder(savedMsg.conversationId)?.let { "CUSTOMER_$it" }
                    }
                    
                    if (recipientId != null) {
                        sendToUser(recipientId, "NEW_MESSAGE", savedMsg)
                    }
                }
                
                call.respond(savedMsg)
            }

            get("/history/{convId}") {
                val convId = call.parameters["convId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val jsonList = RedisManager.getChatHistory(convId)
                val gson = com.google.gson.Gson()
                val list = jsonList.map { gson.fromJson(it, Message::class.java) }
                call.respond(list)
            }
        }
    }
}

// --- HELPERS ---

private suspend fun processDailyFeePayment(driverId: Int, amount: Double, reference: String) {
    println("DEBUG: Daily Fee Payment Success for Driver $driverId")
    
    val settings = DatabaseRepository.getSystemSettings()
    val mode = settings["paystack_mode"] as? String ?: "TEST"
    
    if (mode == "TEST") {
        DatabaseRepository.approveDailyFee(driverId)

        DatabaseRepository.updateDriverOnlineStatus(driverId.toString(), true)
        DatabaseRepository.recordPayment(
            userId = driverId,
            userType = "DRIVER",
            amount = amount,
            type = "DAILY_FEE",
            reference = reference
        )

        sendToUser("DRIVER_$driverId", "NOTIFICATION_RECEIVED", mapOf(
            "title" to "Payment Successful",
            "message" to "Daily fee received. You are now ONLINE automatically.",
            "type" to "DAILY_FEE_PAID",
            "isOnline" to true,
            "createdAt" to java.time.LocalDateTime.now().toString()
        ))
    } else {
        DatabaseRepository.queueDailyFeeForApproval(driverId, amount, reference)
        
        sendToUser("DRIVER_$driverId", "NOTIFICATION_RECEIVED", mapOf(
            "title" to "Payment Received",
            "message" to "Your daily fee payment of ₵$amount has been received and is awaiting admin approval.",
            "type" to "DAILY_FEE_PENDING",
            "createdAt" to java.time.LocalDateTime.now().toString()
        ))
    }
}
