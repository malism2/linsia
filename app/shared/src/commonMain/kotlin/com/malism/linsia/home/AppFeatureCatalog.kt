package com.malism.linsia.home

data class AppFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: AppFeatureIcon,
    val defaultVisible: Boolean = true,
)

enum class AppFeatureIcon {
    MenuBook,
    Newspaper,
    Keyboard,
    Library,
    Dictionary,
    Headphones,
    Bookmark,
}

object AppFeatureCatalog {
    const val LEARN_WORD = "learnword"
    const val NEWS_READ = "newsread"
    const val TYPE_WORD = "typeword"
    const val CLASSIC_READ = "classicread"
    const val DICT = "dict"
    const val DAILY_LISTEN = "dailylisten"
    const val VOCAB_BOOK = "vocabbook"

    val all: List<AppFeature> = listOf(
        AppFeature(
            id = LEARN_WORD,
            title = "记单词",
            subtitle = "学如潮汐 · 积微成博",
            icon = AppFeatureIcon.MenuBook,
        ),
        AppFeature(
            id = NEWS_READ,
            title = "时文阅读",
            subtitle = "慢速英语 · 边听边读",
            icon = AppFeatureIcon.Newspaper,
        ),
        AppFeature(
            id = DAILY_LISTEN,
            title = "每日听力",
            subtitle = "听懂今日 · 边听边练",
            icon = AppFeatureIcon.Headphones,
        ),
        AppFeature(
            id = TYPE_WORD,
            title = "打字记词",
            subtitle = "边打边记 · 肌肉记忆",
            icon = AppFeatureIcon.Keyboard,
        ),
        AppFeature(
            id = CLASSIC_READ,
            title = "经典名著",
            subtitle = "公版原著 · 章章可续",
            icon = AppFeatureIcon.Library,
        ),
        AppFeature(
            id = DICT,
            title = "英汉词典",
            subtitle = "ECDICT · 离线可查",
            icon = AppFeatureIcon.Dictionary,
        ),
        AppFeature(
            id = VOCAB_BOOK,
            title = "生词本",
            subtitle = "阅读生词 · 随时复习",
            icon = AppFeatureIcon.Bookmark,
        ),
    )
}
