package com.mai.oaidviewer.library


interface OAIDCallback {

    fun onText(msg: String)

    fun onSupport(_supplier: OAIDSupplier)

//    fun onError()
}