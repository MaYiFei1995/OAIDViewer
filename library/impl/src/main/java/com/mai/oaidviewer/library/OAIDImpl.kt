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

    /**Uni
     * 获取SDK版本号
     *
     * @return first versionName
     *         second versionCode
     */
    fun getSdkVersion(): Pair<String, String>

    /**
     * 2.3.0新增，在isSupportRequestOAIDPermission返回true时请求权限
     */
    fun requestOAIDPermission(context: Context, callback: RequestPermissionCallback) {
        // ignore
    }

}