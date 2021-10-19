package com.mai.oaidviewer

//import com.bun.miitmdid.core.ErrorCode
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
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IdSupplier
import com.bun.miitmdid.pojo.IdSupplierImpl
import com.mai.oaidviewer.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity(), IIdentifierListener {

    lateinit var binder: ActivityMainBinding
    var alertDialog: AlertDialog? = null

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
//                initOaid6()
                initOaid20210301()
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
            binder.mainTv.text = Html.fromHtml(str)
        }
    }

    private fun initOaid20210301() {
        System.loadLibrary("nllvm1632808251147706677");
        MdidSdkHelper.InitCert(this, loadPemFromAssetFile());
        val code = MdidSdkHelper.InitSdk(this, true, this)
        // 根据SDK返回的code进行不同处理
        val unsupportedIdSupplier = IdSupplierImpl()
        when (code) {
            InfoCode.INIT_ERROR_CERT_ERROR -> {
                // 证书未初始化或证书无效，SDK内部不会回调onSupport
                // APP自定义逻辑
                setText("cert not init or check not pass")
                onSupport(unsupportedIdSupplier)
            }
            InfoCode.INIT_ERROR_DEVICE_NOSUPPORT -> {
                // 不支持的设备, SDK内部不会回调onSupport
                // APP自定义逻辑
                setText("device not supported")
                onSupport(unsupportedIdSupplier)
            }
            InfoCode.INIT_ERROR_LOAD_CONFIGFILE -> {
                // 加载配置文件出错, SDK内部不会回调onSupport
                // APP自定义逻辑
                setText("failed to load config file")
                onSupport(unsupportedIdSupplier)
            }
            InfoCode.INIT_ERROR_MANUFACTURER_NOSUPPORT -> {
                // 不支持的设备厂商, SDK内部不会回调onSupport
                // APP自定义逻辑
                setText("manufacturer not supported")
                onSupport(unsupportedIdSupplier)
            }
            InfoCode.INIT_ERROR_SDK_CALL_ERROR -> {
                // sdk调用出错, SSDK内部不会回调onSupport
                // APP自定义逻辑
                setText("sdk call error")
                onSupport(unsupportedIdSupplier)
            }
            InfoCode.INIT_INFO_RESULT_DELAY -> {
                // 获取接口是异步的，SDK内部会回调onSupport
                setText("result delay (async)")
            }
            InfoCode.INIT_INFO_RESULT_OK -> {
                // 获取接口是同步的，SDK内部会回调onSupport
                setText("result ok (sync)")
            }
            else -> {
                // sdk版本高于DemoHelper代码版本可能出现的情况，无法确定是否调用onSupport
                // 不影响成功的OAID获取
                setText("getDeviceIds: unknown code: $code")
            }
        }
    }

    override fun onSupport(_supplier: IdSupplier?) {
        if (_supplier == null) {
            return
        }
        val sb = StringBuilder()
        sb.append("support:${_supplier.isSupported}<br>")
        sb.append("isLimited:${_supplier.isLimited}<br>")
        sb.append("oaid:${_supplier.oaid}<br>")
        sb.append("vaid:${_supplier.vaid}<br>")
        sb.append("aaid:${_supplier.aaid}<br>")
        if (_supplier.oaid.split("0").size - 1 > 10) {
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
        grantResults: IntArray
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

    private fun loadPemFromAssetFile(): String {
        return try {
            val `is`: InputStream = assets.open("com.example.oaidtest2.cert.pem")
            val `in` = BufferedReader(InputStreamReader(`is`))
            val builder = java.lang.StringBuilder()
            var line: String?
            while (`in`.readLine().also { line = it } != null) {
                builder.append(line)
                builder.append('\n')
            }
            builder.toString()
        } catch (e: IOException) {
            setText("loadPemFromAssetFile failed")
            ""
        }
    }
}