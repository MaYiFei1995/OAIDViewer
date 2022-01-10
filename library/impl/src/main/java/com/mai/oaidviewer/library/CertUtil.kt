package com.mai.oaidviewer.library

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.*
import java.text.SimpleDateFormat

object CertUtil {

    fun getCertInfo(sdf: SimpleDateFormat, certFile: String): String {
        val fact: CertificateFactory
        val `in`: InputStream = ByteArrayInputStream(certFile.toByteArray())
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