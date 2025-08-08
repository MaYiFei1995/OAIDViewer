package com.mai.oaidviewer.library

sealed class InitCallback {

    data class Success(val data: SupplierData) : InitCallback()

    data class Failure(val msg: String) : InitCallback()

}