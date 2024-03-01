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
import com.mai.oaidviewer.library.RequestPermissionCallback
import java.lang.reflect.Method
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
            val text = binder.mainTv.text.toString()
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                    "label",
                    text
                )
            )
            share(this, text)
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
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.CHINA
        )
    }

    private val oaidHelper: OAIDHelper
        get() {
            return OAIDHelper(this, this)
        }

    private fun initOaid() {
        // 设备系统与签名证书信息
        var headerText = "Arch: "

        if (isArchNotSupport()) {
            headerText += "x86 \n"
        } else {
            headerText += "arm\n"
            oaidHelper.init()
        }

        val (versionName, versionCode) = oaidHelper.getSdkVersion()
        headerText += "Version: $versionName ($versionCode)\n" +
                "Time: ${sdf.format(System.currentTimeMillis())}\n" +
                "Brand: ${Build.BRAND}\n" +
                "Manufacturer: ${Build.MANUFACTURER}\n" +
                "Model: ${Build.MODEL}\n" +
                "AndroidVersion: ${Build.VERSION.RELEASE}\n" +
                "\n\n" +
                oaidHelper.getCertInfo(sdf)

        binder.versionTv.text = headerText
    }

    private fun isArchNotSupport(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get: Method = clazz.getMethod("get", String::class.java, String::class.java)
            val value = get.invoke(clazz, "ro.product.cpu.abi", "") as String
            (value.contains("x86"))
        } catch (e: Throwable) {
            e.printStackTrace()
            true
        }
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
        sb.append("IsSupport: ${if (_supplier.isSupported()) "√" else "×"}<br>")
        sb.append("IsLimited: ${if (_supplier.isLimited()) "√" else "×"}<br>")
        sb.append("IsSupportRequestOAIDPermission: ${if (_supplier.isSupportRequestOAIDPermission()) "√" else "×"}<br>")
        sb.append("Oaid: ${_supplier.getOAID()}<br>")
        sb.append("Vaid: ${_supplier.getVAID()}<br>")
        sb.append("Aaid: ${_supplier.getAAID()}<br>")
        if (_supplier.getOAID().split("0").size - 1 > 10) {
            sb.append("<font color=\"#FF0000\"> 设置-> 隐私 -> 广告与隐私 -> \"限制广告追踪\" 开关需要关闭 </font>")
        }
        setText(sb.toString())

        // 受限时请求权限
        if (_supplier.isSupported() && _supplier.isLimited() && _supplier.isSupportRequestOAIDPermission()) {
            oaidHelper.requestOAIDPermission(this, object : RequestPermissionCallback {

                /**
                 * 获取授权成功
                 */
                override fun onGranted(grPermission: Array<String>?) {
                    val permissionStr = getPermissions(grPermission?.toList())
                    Log.i("OAIDViewer", "RequestPermissionCallback#onGranted:$permissionStr")

                    val text = "获取权限'${permissionStr}'成功"
                    binder.permissionTV.text = text
                }

                /**
                 * 获取授权失败
                 */
                override fun onDenied(dePermissions: List<String>?) {
                    val permissionStr = getPermissions(dePermissions)
                    Log.i("OAIDViewer", "RequestPermissionCallback#onDenied:$permissionStr")

                    val text = "获取权限'${permissionStr}'失败"
                    binder.permissionTV.text = text
                }

                /**
                 * 禁止再次询问
                 */
                override fun onAskAgain(asPermissions: List<String>?) {
                    val permissionStr = getPermissions(asPermissions)
                    Log.i("OAIDViewer", "onAskAgain#onDenied:$permissionStr")

                    val text = "禁止再次获取权限'${permissionStr}'"
                    binder.permissionTV.text = text
                }

            })
        }
    }

    private fun getPermissions(permissions: List<String>?): String {
        if (permissions != null) {
            val sb = StringBuilder()
            for (permission in permissions) {
                sb.append(permission).append(";")
            }
            return sb.toString()
        }
        return ""
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

    /**
     * 调用系统分享
     */
    private fun share(context: Context, adbCmdStr: String? = null) {
        val intent = Intent(Intent.ACTION_SEND)
        if (adbCmdStr != null) {
            intent.putExtra(Intent.EXTRA_TEXT, adbCmdStr)
            intent.type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "发送至..."))
    }

}