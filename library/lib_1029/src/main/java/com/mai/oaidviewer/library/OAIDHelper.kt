package com.mai.oaidviewer.library

import android.content.Context
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IdSupplier

class OAIDHelper(context: Context, callback: OAIDCallback) : OAIDImpl(context, callback) {

    override fun init() {
        System.loadLibrary("nllvm1632808251147706677")
        MdidSdkHelper.InitCert(context, loadPemFromAssetFile)

        val value = object : IIdentifierListener {
            override fun onSupport(_suppiler: IdSupplier?) {
                callback.onSupport(object : OAIDSupplier {
                    override fun isSupported(): Boolean {
                        return _suppiler?.isSupported ?: false
                    }

                    override fun isLimited(): Boolean? {
                        return _suppiler?.isLimited
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
        }
        val ret = MdidSdkHelper.InitSdk(context, true, value)

        val errMsg: String = "$ret " + when (ret) {
            InfoCode.INIT_ERROR_CERT_ERROR -> {
                // 证书未初始化或证书无效，SDK内部不会回调onSupport
                "cert not init or check not pass"
            }

            InfoCode.INIT_ERROR_DEVICE_NOSUPPORT -> {
                // 不支持的设备, SDK内部不会回调onSupport
                "device not supported"
            }
            InfoCode.INIT_ERROR_LOAD_CONFIGFILE -> {
                // 加载配置文件出错, SDK内部不会回调onSupport
                "failed to load config file"
            }
            InfoCode.INIT_ERROR_MANUFACTURER_NOSUPPORT -> {
                // 不支持的设备厂商, SDK内部不会回调onSupport
                "manufacturer not supported"
            }
            InfoCode.INIT_ERROR_SDK_CALL_ERROR -> {
                // sdk调用出错, SSDK内部不会回调onSupport
                "sdk call error"
            }
            InfoCode.INIT_INFO_RESULT_DELAY -> {
                // 获取接口是异步的，SDK内部会回调onSupport
                "result delay (async)"
            }
            InfoCode.INIT_INFO_RESULT_OK -> {
                // 获取接口是同步的，SDK内部会回调onSupport
                "result ok (sync)"
            }
            else ->
                ""
        }
        if (errMsg.isNotEmpty())
            callback.onText(errMsg)
    }

    override fun getSdkVersion(): Pair<String, String> {
        return Pair("1.0.29", MdidSdkHelper.SDK_VERSION_CODE.toString())
    }

}