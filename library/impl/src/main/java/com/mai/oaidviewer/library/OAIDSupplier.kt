package com.mai.oaidviewer.library

interface OAIDSupplier {
    fun isSupported(): Boolean

    fun isLimited(): Boolean?

    fun getOAID(): String

    fun getVAID(): String

    fun getAAID(): String
}