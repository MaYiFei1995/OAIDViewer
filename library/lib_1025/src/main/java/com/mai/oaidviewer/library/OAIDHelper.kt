package com.mai.oaidviewer.library

import android.content.Context
import com.bun.miitmdid.core.ErrorCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IdSupplier

class OAIDHelper(context: Context, callback: OAIDCallback) : OAIDImpl(context, callback) {

    override fun init() {
        val ret = MdidSdkHelper.InitSdk(context, true, object : IIdentifierListener {
            override fun OnSupport(isSupport: Boolean, _suppiler: IdSupplier?) {
                callback.onSupport(object : OAIDSupplier {
                    override fun isSupported(): Boolean {
                        return isSupport
                    }

                    override fun isLimited(): Boolean? {
                        return null
                    }

                    override fun getOAID(): String {
                        return _suppiler?.oaid ?: ""
                    }

                    override fun getVAID(): String {
                        return _suppiler?.vaid ?: ""
                    }

                    override fun getAAID(): String {
                        return _suppiler?.aaid ?: ""
                    }

                })
            }
        })

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
            else ->
                ""
        }
        if (errMsg.isNotEmpty())
            callback.onText(errMsg)
    }

    override fun getSdkVersion(): Pair<String, String> {
        return Pair("1.0.25", "20200702")
    }

}