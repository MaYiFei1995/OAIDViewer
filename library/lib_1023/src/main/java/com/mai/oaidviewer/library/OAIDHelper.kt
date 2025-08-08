package com.mai.oaidviewer.library

import android.content.Context
import com.bun.miitmdid.core.ErrorCode
import com.bun.miitmdid.core.MdidSdkHelper

class OAIDHelper : OAIDImpl {

    override suspend fun init(context: Context, callback: (result: InitCallback) -> Unit) {
        val ret = MdidSdkHelper.InitSdk(context, true) { isSupport, supplier ->
            callback.invoke(
                InitCallback.Success(
                    SupplierData(
                        isSupport,
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
        } catch (error: Error) {
            errMsg = error.toString()
        }
        if (errMsg.isNotEmpty()) {
            callback.invoke(InitCallback.Failure(errMsg))
        }
    }

    override fun getSdkVersion(): Pair<String, String> {
        return Pair("1.0.23", "20200702")
    }

}