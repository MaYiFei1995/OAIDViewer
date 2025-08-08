package com.mai.oaidviewer.library

data class SupplierData(

    /**
     * SDK是否支持设备
     */
    val isSupported: Boolean,

    /**
     * 此应用是否被限制获取
     * 不支持的版本返回false
     */
    val isLimited: Boolean = false,

    /**
     * 是否支持主动请求OAID获取权限
     * 2.3.0新增，默认返回false
     */
    val isSupportRequestOAIDPermission: Boolean = false,

    /**
     * 开放匿名设备标识符（OAID）：可以连接所有应用数据的标识符，可用于广告等业务。可以通过 SDK 获取到接口状态（限制、关闭）、ID 值
     */
    val oaid: String,

    /**
     * 开发者匿名设备标识符（VAID）：用于开放给开发者的设备标识符，可用于同一开发者不同应用之间的推荐。可以通过 SDK 获取到ID 值
     */
    val vaid: String,

    /**
     *应用匿名设备标识符（AAID）：第三方应用获取的匿名设备标识，可用于用户统计等。可以通过 SDK 获取到 ID 值
     */
    val aaid: String,

    /**
     * 获取消耗的时间
     */
    val timeConsume: Long = 0L,
) {

    override fun toString(): String {
        var ret = "IsSupport: ${getChar(isSupported)}<br>" +
                "IsLimited: ${getChar(isLimited)}<br>" +
                "IsSupportRequestOAIDPermission: ${getChar(isSupportRequestOAIDPermission)}<br>" +
                "Oaid: $oaid<br>" +
                "Vaid: $vaid<br>" +
                "Aaid: $aaid<br>"
        if (oaid.isEmpty() || oaid.split("0").size > 10) {
            ret += "<font color=\"#FF0000\"> 设置-> 隐私 -> 广告与隐私 -> \"限制广告追踪\" 开关需要关闭 </font><br>"
        }
        if (timeConsume > 0) {
            ret += "it takes ${timeConsume / 1_000_000.0} ms"
        }
        return ret
    }

    private fun getChar(bool: Boolean): String {
        return if (bool) "√" else "×"
    }

}