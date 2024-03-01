package com.mai.oaidviewer.library

interface RequestPermissionCallback {

    /**
     * 获取授权成功
     */
    fun onGranted(grPermission: Array<String>?)

    /**
     * 获取授权失败
     */
    fun onDenied(dePermissions: List<String>?)

    /**
     * 禁止再次询问
     */
    fun onAskAgain(asPermissions: List<String>?)

}