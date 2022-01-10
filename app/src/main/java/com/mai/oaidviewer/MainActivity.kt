package com.mai.oaidviewer

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mai.oaidviewer.databinding.ActivityMainBinding
import com.mai.oaidviewer.library.OAIDCallback
import com.mai.oaidviewer.library.OAIDHelper
import com.mai.oaidviewer.library.OAIDSupplier
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), OAIDCallback {

    private lateinit var binder: ActivityMainBinding
    private var alertDialog: AlertDialog? = null

    private var permissionErrCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binder.root)

        init()

        binder.copyBtn.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                    "label",
                    binder.mainTv.text.toString()
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionErrCount >= 3) {
            if (alertDialog != null && alertDialog!!.isShowing) {
                alertDialog!!.dismiss()
            }
            checkPermission()
        }
    }

    private fun init() {
        when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.Q..Int.MAX_VALUE -> {
                initOaid()
            }
            in Build.VERSION_CODES.M until Build.VERSION_CODES.Q -> {
                checkPermission()
            }
            else -> {
                initImei()
            }
        }
    }

    private fun setText(str: String) {
        Log.e("OAIDViewer", str)
        binder.mainTv.post {
            binder.mainTv.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(str)
            }
        }
    }

    private val sdf: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.CHINA)
    }

    private fun initOaid() {
        val oaidHelper = OAIDHelper(this, this)
        oaidHelper.init()

        // 设备系统与签名证书信息
        val (versionName, versionCode) = oaidHelper.getSdkVersion()
        val headerText = "Version: $versionName ($versionCode)\n" +
                "Time: ${sdf.format(System.currentTimeMillis())}\n" +
                "Brand: ${Build.BRAND}\n" +
                "Manufacturer: ${Build.MANUFACTURER}\n" +
                "Model: ${Build.MODEL}\n" +
                "AndroidVersion: ${Build.VERSION.RELEASE}\n" +
                "\n\n" +
                oaidHelper.getCertInfo(sdf)
        binder.versionTv.text = headerText
    }

    /**
     * 更新信息
     */
    override fun onText(msg: String) {
        // 回调可能在子线程，需要 runOnUiThread
        runOnUiThread { setText(msg) }
    }

    /**
     * OAID回调封装
     */
    override fun onSupport(_supplier: OAIDSupplier) {
        val sb = StringBuilder()
        sb.append("Support: ${if (_supplier.isSupported()) "√" else "×"}<br>")
        sb.append("IsLimited: ${
            when {
                _supplier.isLimited() == null -> "null"
                _supplier.isLimited()!! -> "√"
                else -> "×"
            }
        }<br>")
        sb.append("Oaid: ${_supplier.getOAID()}<br>")
        sb.append("Vaid: ${_supplier.getVAID()}<br>")
        sb.append("Aaid: ${_supplier.getAAID()}<br>")
        if (_supplier.getOAID().split("0").size - 1 > 10) {
            sb.append("<font color=\"#FF0000\"> 设置-> 隐私 -> 广告与隐私 -> \"限制广告追踪\" 开关需要关闭 </font>")
        }
        setText(sb.toString())
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PERMISSION_GRANTED
        ) {
            initImei()
        } else {
            alertDialog = AlertDialog.Builder(this).setMessage("应用需要电话权限！").setCancelable(false)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    if (++permissionErrCount > 3) {
                        try {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                ).setData(Uri.fromParts("package", packageName, null))
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "请手动到应用详情中允许应用电话权限",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            9527
                        )
                    }
                }.create()
            alertDialog!!.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermission()
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun initImei() {
        try {
            val imei = (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
            setText("imei:${imei}")
        } catch (e: Exception) {
            binder.mainTv.post {
                binder.mainTv.text = e.toString()
            }
        }
    }


}