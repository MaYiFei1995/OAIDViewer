package com.mai.oaidviewer.library

import android.content.Context
import android.util.Log
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper

class OAIDHelper : OAIDImpl {

    override suspend fun init(context: Context, callback: (result: InitCallback) -> Unit) {
        System.loadLibrary("nllvm1630571663641560568")
        if (MdidSdkHelper.SDK_VERSION_CODE != 20210801) {
            Log.w("OAIDHelper", "SDK version not match.")
        }

        MdidSdkHelper.InitCert(context, CertUtils.getPemFileContent(context))

        val ret = MdidSdkHelper.InitSdk(context, true) { supplier ->
            callback.invoke(
                InitCallback.Success(
                    SupplierData(
                        supplier.isSupported,
                        supplier.isLimited,
                        oaid = supplier?.oaid ?: "",
                        vaid = supplier?.vaid ?: "",
                        aaid = supplier.aaid ?: ""
                    )
                )
            )
        }
        var errMsg: String
        try {
            errMsg = "$ret " + when (ret) {
                InfoCode.INIT_ERROR_CERT_ERROR ->
                    // 证书未初始化或证书无效，SDK内部不会回调onSupport
                    "cert not init or check not pass"

                InfoCode.INIT_ERROR_DEVICE_NOSUPPORT ->
                    // 不支持的设备, SDK内部不会回调onSupport
                    "device not supported"

                InfoCode.INIT_ERROR_LOAD_CONFIGFILE ->
                    // 加载配置文件出错, SDK内部不会回调onSupport
                    "failed to load config file"

                InfoCode.INIT_ERROR_MANUFACTURER_NOSUPPORT ->
                    // 不支持的设备厂商, SDK内部不会回调onSupport
                    "manufacturer not supported"

                InfoCode.INIT_ERROR_SDK_CALL_ERROR ->
                    // sdk调用出错, SSDK内部不会回调onSupport
                    "sdk call error"

                InfoCode.INIT_INFO_RESULT_DELAY ->
                    // 获取接口是异步的，SDK内部会回调onSupport
                    "result delay (async)"

                InfoCode.INIT_INFO_RESULT_OK ->
                    // 获取接口是同步的，SDK内部会回调onSupport
                    "result ok (sync)"

                else ->
                    ""
            }
        } catch (error: Error) {
            errMsg = error.toString()
        }
        if (errMsg.isNotEmpty()) {
            callback.invoke(InitCallback.Failure(errMsg))
        }
    }


    override fun getSdkVersion(): Pair<String, String> {
        return Pair("1.0.27", MdidSdkHelper.SDK_VERSION_CODE.toString())
    }

}