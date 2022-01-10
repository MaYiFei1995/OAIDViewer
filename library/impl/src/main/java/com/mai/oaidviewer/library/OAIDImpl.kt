package com.mai.oaidviewer.library

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat

abstract class OAIDImpl(protected val context: Context, protected val callback: OAIDCallback) {

    abstract fun init()

    val loadPemFromAssetFile: String by lazy {
        try {
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
            callback.onText("loadPemFromAssetFile failed")
            ""
        }
    }

    /**
     * 获取SDK版本号
     *
     * @return first versionName
     *         second versionCode
     */
    abstract fun getSdkVersion(): Pair<String, String>

    open fun getCertInfo(sdf: SimpleDateFormat): String {
        return CertUtil.getCertInfo(sdf, loadPemFromAssetFile)
    }
}