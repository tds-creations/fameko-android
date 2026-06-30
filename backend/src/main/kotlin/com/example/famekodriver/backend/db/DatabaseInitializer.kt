package com.example.famekodriver.backend.db

import com.example.famekodriver.core.network.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import javax.sql.DataSource

object DatabaseInitializer {
    private var dataSource: HikariDataSource? = null

    fun init(): DataSource {
        if (dataSource == null) {
            println("Initializing Database connection...")
            try {
                val envUrl = System.getenv("DATABASE_URL") ?: System.getenv("DB_URL")
                
                var finalUrl: String
                var finalUser: String? = System.getenv("DB_USER")
                var finalPass: String? = System.getenv("DB_PASS")

                if (envUrl != null && (envUrl.startsWith("postgresql://") || envUrl.startsWith("postgres://"))) {
                    println("Parsing DATABASE_URL from environment...")
                    try {
                        // Standard Railway/Heroku postgres URL: postgresql://user:pass@host:port/db
                        val cleanUrl = envUrl.replace("postgresql://", "http://").replace("postgres://", "http://")
                        val uri = java.net.URI(cleanUrl)
                        val userInfo = uri.userInfo
                        if (userInfo != null && userInfo.contains(":")) {
                            val parts = userInfo.split(":")
                            finalUser = parts[0]
                            finalPass = parts[1]
                        }
                        
                        val host = uri.host
                        val port = if (uri.port != -1) uri.port else 5432
                        val path = uri.path
                        finalUrl = "jdbc:postgresql://$host:$port$path"
                        
                        // Handle SSL mode and query parameters
                        if (uri.query != null) {
                            finalUrl += "?" + uri.query
                        } else {
                            // If running on Railway (not localhost), try without forcing disable first
                            // but allow sslmode=require if needed. 
                            // Standard for Railway is often no extra params needed for internal networking.
                            if (envUrl.contains("localhost") || envUrl.contains("127.0.0.1")) {
                                finalUrl += "?sslmode=disable"
                            }
                        }
                    } catch (e: Exception) {
                        println("Failed to parse URI, using raw URL as JDBC: ${e.message}")
                        finalUrl = if (envUrl.startsWith("jdbc:")) envUrl else "jdbc:$envUrl"
                    }
                } else {
                    finalUrl = envUrl ?: DatabaseConfig.getJdbcUrl()
                }

                finalUser = finalUser ?: DatabaseConfig.DB_USER
                finalPass = finalPass ?: DatabaseConfig.DB_PASS

                println("Connecting to database at: ${finalUrl.split("?")[0]} (user: $finalUser)")

                Class.forName("org.postgresql.Driver")
                val config = HikariConfig().apply {
                    jdbcUrl = finalUrl
                    username = finalUser
                    password = finalPass
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 10
                    connectionTimeout = 30000
                    idleTimeout = 600000
                    maxLifetime = 1800000
                    isAutoCommit = true
                }
                dataSource = HikariDataSource(config)
                
                dataSource!!.connection.use { conn ->
                    println("Database connection established. Running migrations...")
                    createTables(conn)
                    migrateTables(conn)
                    seedAdmin(conn)
                    println("Database initialization complete.")
                }
            } catch (e: Exception) {
                println("DATABASE CRITICAL ERROR: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
        return dataSource!!
    }

    fun getDataSource(): DataSource = dataSource ?: init()

    private fun createTables(conn: Connection) {
        val statements = listOf(
            """
            CREATE TABLE IF NOT EXISTS admins (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT DEFAULT 'REGIONAL_ADMIN',
                region TEXT,
                can_manage_drivers BOOLEAN DEFAULT TRUE,
                can_view_analytics BOOLEAN DEFAULT TRUE,
                can_manage_orders BOOLEAN DEFAULT TRUE,
                can_manage_admins BOOLEAN DEFAULT FALSE,
                is_active BOOLEAN DEFAULT TRUE,
                date_joined TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS drivers (
                id SERIAL PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                phone TEXT UNIQUE NOT NULL,
                region TEXT NOT NULL,
                password TEXT NOT NULL,
                license_number TEXT UNIQUE NOT NULL,
                vehicle_type TEXT,
                vehicle_number TEXT,
                vehicle_model TEXT,
                status TEXT DEFAULT 'PENDING',
                is_online BOOLEAN DEFAULT FALSE,
                service_types TEXT DEFAULT 'BOTH',
                profile_picture TEXT,
                license_image TEXT,
                id_front_image TEXT,
                id_back_image TEXT,
                vehicle_image TEXT,
                insurance_cert TEXT,
                roadworthy_cert TEXT,
                emergency_contact_1 TEXT,
                emergency_contact_2 TEXT,
                rating DOUBLE PRECISION DEFAULT 5.0,
                rating_count INTEGER DEFAULT 0,
                daily_fee_paid_at DATE,
                daily_fee_expires_at TIMESTAMP,
                date_joined TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS customers (
                id SERIAL PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                phone TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                region TEXT,
                default_address TEXT,
                profile_picture TEXT,
                is_active BOOLEAN DEFAULT TRUE,
                date_joined TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS pricing_config (
                id SERIAL PRIMARY KEY,
                base_fare NUMERIC(12, 2) DEFAULT 0.0,
                per_km_rate NUMERIC(12, 2) DEFAULT 0.0,
                per_minute_rate NUMERIC(12, 2) DEFAULT 0.0,
                min_fare NUMERIC(12, 2) DEFAULT 0.0,
                milestone_interval INTEGER DEFAULT 0,
                milestone_discount_percent INTEGER DEFAULT 0,
                driver_commission_percent DOUBLE PRECISION DEFAULT 0.0,
                peak_multiplier DOUBLE PRECISION DEFAULT 1.0,
                daily_service_fee NUMERIC(12, 2) DEFAULT 0.0,
                rental_daily_rate NUMERIC(12, 2) DEFAULT 0.0,
                rental_commission_percent DOUBLE PRECISION DEFAULT 0.0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS rental_vehicles (
                id SERIAL PRIMARY KEY,
                owner_id INTEGER REFERENCES drivers(id),
                name TEXT NOT NULL,
                model TEXT,
                vehicle_type TEXT NOT NULL,
                vehicle_number TEXT UNIQUE,
                daily_rate NUMERIC(12, 2) NOT NULL,
                description TEXT,
                features TEXT, -- comma separated
                image_urls TEXT, -- comma separated
                is_available BOOLEAN DEFAULT TRUE,
                status TEXT DEFAULT 'AVAILABLE',
                location TEXT,
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS rental_rates (
                id SERIAL PRIMARY KEY,
                vehicle_type TEXT UNIQUE NOT NULL,
                daily_rate NUMERIC(12, 2) NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS rentals (
                id SERIAL PRIMARY KEY,
                customer_id INTEGER REFERENCES customers(id) NOT NULL,
                vehicle_id INTEGER REFERENCES rental_vehicles(id) NOT NULL,
                driver_id INTEGER REFERENCES drivers(id),
                pickup_location TEXT,
                destination_location TEXT,
                pickup_lat DOUBLE PRECISION,
                pickup_lng DOUBLE PRECISION,
                destination_lat DOUBLE PRECISION,
                destination_lng DOUBLE PRECISION,
                stops TEXT,
                duration_hours INTEGER NOT NULL,
                total_price NUMERIC(12, 2) NOT NULL,
                status TEXT DEFAULT 'PENDING',
                booking_code TEXT UNIQUE,
                trip_notes TEXT,
                start_time TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS orders (
                id SERIAL PRIMARY KEY,
                customer_id INTEGER REFERENCES customers(id) NOT NULL,
                total_amount NUMERIC(12, 2) NOT NULL,
                status TEXT DEFAULT 'Pending',
                pickup_location TEXT,
                dropoff_location TEXT,
                verification_pin VARCHAR(4),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS deliveries (
                id SERIAL PRIMARY KEY,
                order_id INTEGER REFERENCES orders(id) NOT NULL,
                driver_id INTEGER REFERENCES drivers(id),
                pickup_location TEXT NOT NULL,
                dropoff_location TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                service_type TEXT DEFAULT 'RIDE_HAILING',
                estimated_earnings NUMERIC(12, 2) DEFAULT 0.0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS driver_stats (
                driver_id INTEGER PRIMARY KEY REFERENCES drivers(id),
                is_online BOOLEAN DEFAULT FALSE,
                total_deliveries INTEGER DEFAULT 0,
                total_earnings NUMERIC(12, 2) DEFAULT 0.0,
                earnings_today NUMERIC(12, 2) DEFAULT 0.0,
                completed_today INTEGER DEFAULT 0,
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                bearing REAL DEFAULT 0.0,
                h3_index TEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS wallet_topups (
                id SERIAL PRIMARY KEY,
                driver_id INTEGER REFERENCES drivers(id) NOT NULL,
                amount INTEGER NOT NULL,
                reference_code TEXT NOT NULL,
                payment_type TEXT DEFAULT 'TOPUP',
                status TEXT DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS system_settings (
                id SERIAL PRIMARY KEY,
                support_phone TEXT DEFAULT '+233 24 000 0000',
                support_email TEXT DEFAULT 'finance@example.com',
                support_whatsapp TEXT DEFAULT '+233 24 000 0000',
                app_version_customer TEXT DEFAULT '1.0.0',
                app_version_driver TEXT DEFAULT '1.0.0',
                maintenance_mode BOOLEAN DEFAULT FALSE,
                paystack_mode TEXT DEFAULT 'TEST',
                paystack_test_secret TEXT,
                paystack_live_secret TEXT,
                paystack_test_public TEXT,
                paystack_live_public TEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS payments (
                id SERIAL PRIMARY KEY,
                user_id INTEGER,
                user_type TEXT, -- 'DRIVER' or 'CUSTOMER'
                amount NUMERIC(12, 2) NOT NULL,
                payment_type TEXT, -- 'DAILY_FEE', 'RENTAL_BOOKING', 'TOPUP'
                reference TEXT UNIQUE NOT NULL,
                status TEXT DEFAULT 'SUCCESS',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id SERIAL PRIMARY KEY,
                conversation_id INTEGER NOT NULL,
                sender_type TEXT NOT NULL, -- 'driver' or 'customer'
                sender_id INTEGER NOT NULL,
                body TEXT NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS movement_logs (
                id SERIAL PRIMARY KEY,
                entity_id INTEGER,
                entity_type TEXT, -- 'DRIVER' or 'CUSTOMER'
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                weather TEXT, -- 'CLEAR', 'RAIN', 'STORM'
                time_of_day TEXT, -- 'MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS simulation_runs (
                id SERIAL PRIMARY KEY,
                scenario_name TEXT,
                parameters TEXT, -- JSON string of input parameters
                results TEXT, -- JSON string of results
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS products (
                id SERIAL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                price NUMERIC(12, 2) NOT NULL,
                category TEXT,
                location TEXT,
                images TEXT, -- comma separated
                seller_id INTEGER REFERENCES drivers(id),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS saved_places (
                id SERIAL PRIMARY KEY,
                customer_id INTEGER REFERENCES customers(id) NOT NULL,
                label TEXT NOT NULL,
                address TEXT NOT NULL,
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
        conn.createStatement().use { stmt ->
            statements.forEach { stmt.execute(it) }
        }
    }

    private fun migrateTables(conn: Connection) {
        val migrations = listOf(
            "DO $$ BEGIN IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='rentals' AND column_name='assigned_driver_id') THEN ALTER TABLE rentals RENAME COLUMN assigned_driver_id TO driver_id; END IF; END $$;",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS driver_id INTEGER REFERENCES drivers(id);",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS booking_code TEXT UNIQUE;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS insurance_cert TEXT;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS roadworthy_cert TEXT;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS user_role TEXT DEFAULT 'DRIVER';",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS company_name TEXT;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS registration_number TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES drivers(id);",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS description TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS features TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS image_urls TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS location TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'AVAILABLE';",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS model TEXT;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS vehicle_number TEXT UNIQUE;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS seats INTEGER DEFAULT 5;",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS transmission TEXT DEFAULT 'Automatic';",
            "ALTER TABLE rental_vehicles ADD COLUMN IF NOT EXISTS fuel_type TEXT DEFAULT 'Petrol';",
            "UPDATE rental_vehicles SET status = 'AVAILABLE' WHERE status IS NULL;",
            "UPDATE rental_vehicles SET is_available = TRUE WHERE is_available IS NULL;",
            "UPDATE drivers SET profile_picture = NULL WHERE profile_picture = 'null' OR profile_picture = '' OR profile_picture = 'undefined' OR profile_picture LIKE '%placeholder%';",
            "UPDATE drivers SET license_image = NULL WHERE license_image = 'null' OR license_image = '' OR license_image = 'undefined' OR license_image LIKE '%placeholder%';",
            "UPDATE drivers SET insurance_cert = NULL WHERE insurance_cert = 'null' OR insurance_cert = '' OR insurance_cert = 'undefined' OR insurance_cert LIKE '%placeholder%';",
            "UPDATE drivers SET roadworthy_cert = NULL WHERE roadworthy_cert = 'null' OR roadworthy_cert = '' OR roadworthy_cert = 'undefined' OR roadworthy_cert LIKE '%placeholder%';",
            "UPDATE drivers SET id_front_image = NULL WHERE id_front_image = 'null' OR id_front_image = '' OR id_front_image = 'undefined' OR id_front_image LIKE '%placeholder%';",
            "ALTER TABLE pricing_config ADD COLUMN IF NOT EXISTS rental_owner_commission_percent DOUBLE PRECISION DEFAULT 7.5;",
            "ALTER TABLE pricing_config ADD COLUMN IF NOT EXISTS rental_customer_service_fee_percent DOUBLE PRECISION DEFAULT 7.5;",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS base_price NUMERIC(12, 2);",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS owner_commission_amount NUMERIC(12, 2);",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS customer_service_fee NUMERIC(12, 2);",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS owner_earnings NUMERIC(12, 2);",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS payment_status TEXT DEFAULT 'PENDING';",
            "ALTER TABLE rentals ADD COLUMN IF NOT EXISTS is_self_drive BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS fcm_token TEXT;",
            "ALTER TABLE customers ADD COLUMN IF NOT EXISTS fcm_token TEXT;",
            "ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS pickup_lat DOUBLE PRECISION;",
            "ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS pickup_lng DOUBLE PRECISION;",
            "ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS dropoff_lat DOUBLE PRECISION;",
            "ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS dropoff_lng DOUBLE PRECISION;",
            "ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS distance_km DOUBLE PRECISION;",
            "ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS paystack_mode TEXT DEFAULT 'TEST';",
            "ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS paystack_test_secret TEXT;",
            "ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS paystack_live_secret TEXT;",
            "ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS paystack_test_public TEXT;",
            "ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS paystack_live_public TEXT;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS daily_fee_paid_at DATE;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS daily_fee_expires_at TIMESTAMP;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS vehicle_category TEXT DEFAULT 'Economy';",
            "ALTER TABLE drivers ADD COLUMN IF NOT EXISTS rating_count INTEGER DEFAULT 0;",
            "ALTER TABLE wallet_topups ADD COLUMN IF NOT EXISTS payment_type TEXT DEFAULT 'TOPUP';",
            "ALTER TABLE orders ADD COLUMN IF NOT EXISTS scheduled_time TIMESTAMP;",
            "ALTER TABLE driver_stats ADD COLUMN IF NOT EXISTS h3_index TEXT;",
            "CREATE INDEX IF NOT EXISTS idx_driver_stats_h3 ON driver_stats(h3_index);",
            "CREATE INDEX IF NOT EXISTS idx_chat_messages_conv ON chat_messages(conversation_id);",
            """
            CREATE TABLE IF NOT EXISTS saved_places (
                id SERIAL PRIMARY KEY,
                customer_id INTEGER REFERENCES customers(id) NOT NULL,
                label TEXT NOT NULL,
                address TEXT NOT NULL,
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        conn.createStatement().use { stmt ->
            migrations.forEach { 
                try { stmt.execute(it) } catch (e: Exception) { /* Ignore */ }
            }
        }
    }

    private fun seedAdmin(conn: Connection) {
        try {
            val res = conn.createStatement().executeQuery("SELECT COUNT(*) FROM admins WHERE username = 'admin'")
            if (res.next() && res.getInt(1) == 0) {
                conn.createStatement().execute("INSERT INTO admins (username, email, password, role, can_manage_admins) VALUES ('admin', 'niiodartei24@gmail.com', 'feroA5002', 'SUPERADMIN', true)")
                println("Superadmin seeded.")
            }
            
            val resPricing = conn.createStatement().executeQuery("SELECT COUNT(*) FROM pricing_config")
            if (resPricing.next() && resPricing.getInt(1) == 0) {
                conn.createStatement().execute("""
                    INSERT INTO pricing_config (
                        base_fare, per_km_rate, per_minute_rate, min_fare, 
                        milestone_interval, milestone_discount_percent, 
                        driver_commission_percent, peak_multiplier, daily_service_fee
                    ) VALUES (5.0, 2.0, 0.5, 10.0, 50, 10, 15.0, 1.0, 10.0)
                """.trimIndent())
                println("Pricing config initialized.")
            }
            
            val resSettings = conn.createStatement().executeQuery("SELECT COUNT(*) FROM system_settings")
            if (resSettings.next() && resSettings.getInt(1) == 0) {
                conn.createStatement().execute("INSERT INTO system_settings (id, support_phone, support_email) VALUES (1, '+233 24 123 4567', 'support@fameko.com')")
                println("System settings initialized.")
            }

            val resProducts = conn.createStatement().executeQuery("SELECT COUNT(*) FROM products")
            if (resProducts.next() && resProducts.getInt(1) == 0) {
                conn.createStatement().execute("""
                    INSERT INTO products (name, description, price, category, location, images) VALUES 
                    ('Smartphone X', 'Latest model with 5G', 2500.0, 'Electronics', 'Accra', 'phone1.jpg,phone2.jpg'),
                    ('Leather Jacket', 'Genuine leather, black', 450.0, 'Fashion', 'Kumasi', 'jacket1.jpg'),
                    ('Mountain Bike', '21 speed, all-terrain', 1200.0, 'Sports', 'Tema', 'bike1.jpg')
                """.trimIndent())
                println("Initial products seeded.")
            }
        } catch (e: Exception) {
            println("Seeding error: ${e.message}")
        }
    }
}
