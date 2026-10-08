package com.mhss.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object MailSender {

    // 🔐 FYLL I dina riktiga värden (lägg helst i BuildConfig/secure storage)
    private const val FROM_EMAIL = "u5362781704@gmail.com"
    private const val APP_PASSWORD = "itkvpojzplpkksku" // App-lösenordet

    // 📬 Gmail SMTP
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    suspend fun sendMail(
        toEmail: String,
        subject: String,
        messageBody: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TLS/STARTTLS och TLS1.2 tvingas
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(FROM_EMAIL, APP_PASSWORD)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL, "Vård & Omsorg", "UTF-8"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject, "UTF-8")
                setText(messageBody, "UTF-8")
            }

            Transport.send(message)
            Result.success(Unit)

        } catch (e: AuthenticationFailedException) {
            e.printStackTrace()
            Result.failure(IllegalStateException("Autentisering misslyckades mot SMTP (kontrollera app-lösenordet).", e))

        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
            Result.failure(IllegalStateException("SMTP-provider saknas (lägg in ProGuard/R8 keep-reglerna).", e))

        } catch (e: MessagingException) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

