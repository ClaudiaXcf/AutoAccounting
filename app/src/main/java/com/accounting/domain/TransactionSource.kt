package com.accounting.domain

enum class TransactionSource(val displayName: String, val packageName: String) {
    WECHAT("微信", "com.tencent.mm"),
    QQ("QQ", "com.tencent.mobileqq"),
    ALIPAY("支付宝", "com.eg.android.AlipayGphone"),
    JD("京东", "com.jingdong.app.mall"),
    TAOBAO("淘宝", "com.taobao.taobao"),
    PINDUODUO("拼多多", "com.xunmeng.pinduoduo"),
    MEITUAN("美团", "com.sankuai.meituan"),
    ELE("饿了么", "me.ele"),
    METRO("地铁", "com.zhicall.ztk"),
    BANK("银行", "unknown");

    companion object {
        fun fromPackage(packageName: String): TransactionSource? {
            return entries.find { it.packageName == packageName }
        }

        fun fromName(name: String): TransactionSource? {
            return entries.find { it.name == name }
        }
    }
}
