package com.mai.oaidviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bun.miitmdid.core.ErrorCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IdSupplier
import com.mai.oaidviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), IIdentifierListener {

    lateinit var binder: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binder.root)
        val ret = MdidSdkHelper.InitSdk(this, true, this)
        val errMsg: String = "$ret " + when (ret) {
            ErrorCode.INIT_ERROR_BEGIN->
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
        sb.append("support:$isSupport\n")
        sb.append("oaid:${_supplier.oaid}\n")
        sb.append("vaid:${_supplier.vaid}\n")
        sb.append("aaid:${_supplier.aaid}\n")
        setText(sb.toString())
    }

    private fun setText(str: String) {
        Log.e("OAIDViewer", str)
        binder.mainTv.post {
            binder.mainTv.text = str
        }
    }
}