package com.mai.oaidviewer.library

import android.content.Context
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat

object CertUtils {

    private var pemFileContent: String = ""

    fun getPemFileContent(context: Context): String {
        if (pemFileContent.isEmpty()) {
            pemFileContent = try {
                val `is`: InputStream = context.assets.open("com.example.oaidtest2.cert.pem")
                val `in` = BufferedReader(InputStreamReader(`is`))
                val builder = java.lang.StringBuilder()
                var line: String?
                while (`in`.readLine().also { line = it } != null) {
                    builder.append(line)
                    builder.append('\n')
                }
                builder.toString()
            } catch (e: IOException) {
                ""
            }
        }
        return pemFileContent
    }

    fun getCertInfo(context: Context, sdf: SimpleDateFormat): String {
        val pemContent = getPemFileContent(context)
        if (pemContent.isEmpty()) {
            return "error"
        }
        val fact: CertificateFactory
        val `in`: InputStream = ByteArrayInputStream(pemContent.toByteArray())
        val appCert: X509Certificate
        try {
            fact = CertificateFactory.getInstance("X.509")
            appCert = fact.generateCertificate(`in`) as X509Certificate
        } catch (e: CertificateException) {
            return "[Cert Format Error]"
        }

        val certInfo = """
               Cert: 
               SubjectName: ${appCert.subjectX500Principal.name}
               Not Before: ${sdf.format(appCert.notBefore)}
               Not After: ${sdf.format(appCert.notAfter)}
               """.trimIndent()
        try {
            appCert.checkValidity()
        } catch (e: CertificateExpiredException) {
            return "$certInfo\n[Expired]"
        } catch (e: CertificateNotYetValidException) {
            return "$certInfo\n[NotYetValid]"
        }
        return "$certInfo\n[Valid]"
    }

}