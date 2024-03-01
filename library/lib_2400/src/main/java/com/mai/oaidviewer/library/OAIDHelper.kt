package com.mai.oaidviewer.library

import android.content.Context
import android.util.Log
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.miitmdid.interfaces.IIdentifierListener
import com.bun.miitmdid.interfaces.IPermissionCallbackListener
import com.bun.miitmdid.interfaces.IdSupplier

class OAIDHelper(context: Context, callback: OAIDCallback) : OAIDImpl(context, callback) {

    override fun init() {
        System.loadLibrary("msaoaidsec")

        Thread {
            // 加固版本在调用前必须载入SDK安全库,因为加载有延迟，推荐在application中调用loadLibrary方法
            try {
                Thread.sleep(1000L)
            } catch (ignore: Exception) {

            }

            if (MdidSdkHelper.SDK_VERSION_CODE != 20240110) {
                Log.w(
                    "OAIDHelper",
                    "SDK version not match. Current version code is '${MdidSdkHelper.SDK_VERSION_CODE}'"
                )
            }

            MdidSdkHelper.InitCert(context, loadPemFromAssetFile)

            val value = object : IIdentifierListener {
                override fun onSupport(_suppiler: IdSupplier?) {
                    callback.onSupport(object : OAIDSupplier {
                        override fun isSupported(): Boolean {
                            return _suppiler?.isSupported ?: false
                        }

                        override fun isLimited(): Boolean {
                            return _suppiler?.isLimited ?: false
                        }

                        /**
                         * 2.3.0新增，默认返回false
                         */
                        override fun isSupportRequestOAIDPermission(): Boolean {
                            return _suppiler?.isSupportRequestOAIDPermission ?: false
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

            //（可选）设置InitSDK接口回调超时时间(仅适用于接口为异步)，默认值为5000ms.
            // 注：请在调用前设置一次后就不再更改，否则可能导致回调丢失、重复等问题
            try {
                MdidSdkHelper.setGlobalTimeout(5000)
            } catch (error: Error) {
                error.printStackTrace()
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
        }.start()
    }

    override fun getSdkVersion(): Pair<String, String> {
        return Pair("2.4.0", MdidSdkHelper.SDK_VERSION_CODE.toString())
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