package com.example.famekodriver.backend.services

import java.util.Properties
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

object EmailService {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "465"
    private val SMTP_USER = (System.getenv("SMTP_EMAIL") ?: "lampteyjosephtds01@gmail.com").trim()
    private val SMTP_PASS = (System.getenv("APP_PASSWORD") ?: "jgrjdazptkpmlsui").replace(" ", "").trim()

    fun sendOtpEmail(recipientEmail: String, otp: String) {
        println("EmailService: Preparing to send OTP to $recipientEmail")
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.socketFactory.port", SMTP_PORT)
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.debug", "true") 
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                println("EmailService: Authenticating with Gmail...")
                return PasswordAuthentication(SMTP_USER, SMTP_PASS)
            }
        })
        session.debug = true

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SMTP_USER, "Fameko Support"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                subject = "Fameko - Password Reset OTP"
                setText("Hello,\n\nYour 6-digit verification code is: $otp\n\nThis code will expire in 10 minutes.\n\nBest regards,\nFameko Team")
            }

            // Send in a background thread to avoid blocking the API response
            Thread {
                try {
                    println("EmailService: Attempting Transport.send...")
                    Transport.send(message)
                    println("EmailService: Email sent successfully to $recipientEmail")
                } catch (e: Exception) {
                    println("EmailService ERROR: Failed to send email to $recipientEmail: ${e.message}")
                    e.printStackTrace()
                }
            }.start()
            
        } catch (e: Exception) {
            println("EmailService ERROR: Error creating message: ${e.message}")
            e.printStackTrace()
        }
    }
}
