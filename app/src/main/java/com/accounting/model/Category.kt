package com.accounting.model

enum class Category(val displayName: String, val keywords: List<String>) {
    FOOD("餐饮", listOf("餐饮", "美食", "快餐", "外卖", "餐厅", "食堂")),
    TRANSPORT("交通", listOf("交通", "打车", "公交", "地铁", "停车", "加油")),
    SHOPPING("购物", listOf("购物", "超市", "商城", "京东", "淘宝", "拼多多")),
    ENTERTAINMENT("娱乐", listOf("娱乐", "电影", "游戏", "KTV", "演出", "旅游")),
    MEDICAL("医疗", listOf("医疗", "医院", "药店", "诊所")),
    HOUSING("住房", listOf("住房", "房租", "物业", "水电", "燃气")),
    EDUCATION("教育", listOf("教育", "培训", "学费", "书籍", "文具")),
    COMMUNICATION("通讯", listOf("通讯", "话费", "流量", "宽带")),
    TRANSFER("转账", listOf("转账", "汇款", "收款", "付款", "红包")),
    OTHER("其他", emptyList());

    companion object {
        fun categorize(text: String?): String {
            if (text.isNullOrEmpty()) return OTHER.displayName

            val lowerText = text.lowercase()

            for (category in entries) {
                for (keyword in category.keywords) {
                    if (lowerText.contains(keyword.lowercase())) {
                        return category.displayName
                    }
                }
            }

            return OTHER.displayName
        }
    }
}