package com.mai.oaidviewer.library

import android.content.Context
import kotlin.reflect.full.primaryConstructor

interface OAIDImpl {

    companion object {

        val instance: OAIDImpl by lazy {
            Class.forName("com.mai.oaidviewer.library.OAIDHelper").kotlin
                .primaryConstructor!!
                .call() as OAIDImpl
        }

    }

    suspend fun init(context: Context, callback: (result: InitCallback) -> Unit)

    /**
     * 获取SDK版本号
     */
    fun getSdkVersionCode(): Int

    /**
     * 2.3.0新增，在isSupportRequestOAIDPermission返回true时请求权限
     */
    fun requestOAIDPermission(context: Context, callback: RequestPermissionCallback) {
        // ignore
    }

}