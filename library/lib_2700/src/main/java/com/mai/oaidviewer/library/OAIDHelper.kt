package com.mai.oaidviewer.library

import android.content.Context
import android.util.Log
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IPermissionCallbackListener
import kotlinx.coroutines.delay

class OAIDHelper : OAIDImpl {

    override suspend fun init(context: Context, callback: (result: InitCallback) -> Unit) {
        System.loadLibrary("msaoaidsec")
        // 加固版本在调用前必须载入SDK安全库,因为加载有延迟，推荐在application中调用loadLibrary方法
        delay(1000L)

        val startTime = System.nanoTime()

        if (MdidSdkHelper.SDK_VERSION_CODE != 20250430) {
            Log.w("OAIDHelper", "SDK version not match.")
        }
        MdidSdkHelper.InitCert(context, CertUtils.getPemFileContent(context))

        //（可选）设置InitSDK接口回调超时时间(仅适用于接口为异步)，默认值为5000ms.
        // 注：请在调用前设置一次后就不再更改，否则可能导致回调丢失、重复等问题
        try {
            MdidSdkHelper.setGlobalTimeout(5000)
        } catch (error: Error) {
            Log.w("OAIDHelper", error)
        }

        val ret = MdidSdkHelper.InitSdk(context, true, true, true, true) { supplier ->
            val endTime = System.nanoTime()
            callback.invoke(
                InitCallback.Success(
                    SupplierData(
                        supplier.isSupported,
                        supplier.isLimited,
                        supplier.isSupportRequestOAIDPermission,
                        supplier?.oaid ?: "",
                        supplier?.vaid ?: "",
                        supplier.aaid ?: "",
                        endTime - startTime
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
        return Pair("2.7.0", MdidSdkHelper.SDK_VERSION_CODE.toString())
    }

    /**
     * 2.3.0新增，在isSupportRequestOAIDPermission返回true时请求权限
     */
    override fun requestOAIDPermission(context: Context, callback: RequestPermissionCallback) {
        MdidSdkHelper.requestOAIDPermission(context, object : IPermissionCallbackListener {

            override fun onGranted(grPermission: Array<String>) {
                callback.onGranted(grPermission)
            }

            override fun onDenied(dePermissions: MutableList<String>) {
                callback.onDenied(dePermissions)
            }

            override fun onAskAgain(asPermissions: MutableList<String>) {
                callback.onAskAgain(asPermissions)
            }

        })
    }

}