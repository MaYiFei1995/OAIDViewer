package com.mai.oaidviewer.library

interface OAIDSupplier {

    fun isSupported(): Boolean

    /**
     * 不支持的版本返回false
     */
    fun isLimited(): Boolean {
        return false
    }

    /**
     * 2.3.0新增，默认返回false
     */
    fun isSupportRequestOAIDPermission(): Boolean {
        return false
    }

    fun getOAID(): String

    fun getVAID(): String

    fun getAAID(): String

}