package com.mai.oaidviewer

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.bun.miitmdid.core.ErrorCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IdSupplier
import com.mai.oaidviewer.databinding.ActivityMainBinding
import java.lang.Exception

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
            binder.mainTv.text = Html.fromHtml(str)
        }
    }

    private fun initOaid() {
        val ret = MdidSdkHelper.InitSdk(this, true, this)
        val errMsg: String = "$ret " + when (ret) {
            ErrorCode.INIT_ERROR_BEGIN ->
                "INIT_ERROR_BEGIN"
            ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT ->
                "不支持的设备"
            ErrorCode.INIT_ERROR_LOAD_CONFIGFILE ->
                "加载配置文件失败"
            ErrorCode.INIT_ERROR_MANUFACTURER_NOSUPPORT ->
                "不支持的厂商"
            ErrorCode.INIT_ERROR_RESULT_DELAY ->
                "信息将会延迟返回，获取数据可能在异步线程，取决于设备"
            ErrorCode.INIT_HELPER_CALL_ERROR ->
                "反射调用出错"
//            ErrorCode.INIT_ERROR_CONFIGFILE_MISMATCH ->
//                "配置文件不匹配"
            else ->
                "未知错误"
        }

        setText(errMsg)
    }

    override fun OnSupport(isSupport: Boolean, _supplier: IdSupplier?) {
        if (_supplier == null) {
            return
        }

        val sb = StringBuilder()
        sb.append("support:$isSupport<br>")
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
}