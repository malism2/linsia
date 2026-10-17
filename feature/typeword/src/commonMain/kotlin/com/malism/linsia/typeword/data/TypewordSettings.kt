package com.malism.linsia.typeword.data

import com.russhwolf.settings.Settings
import com.malism.linsia.typeword.domain.TypewordDefaults

class TypewordSettings(private val settings: Settings) {
    var dictId: String
        get() = settings.getString(DICT_ID, TypewordDefaults.DefaultDictId)
        set(value) = settings.putString(DICT_ID, value)

    var chapterIndex: Int
        get() = settings.getInt(CHAPTER, 0)
        set(value) = settings.putInt(CHAPTER, value)

    var ukAccent: Boolean
        get() = settings.getBoolean(UK, false)
        set(value) = settings.putBoolean(UK, value)

    var dictation: Boolean
        get() = settings.getBoolean(DICTATION, false)
        set(value) = settings.putBoolean(DICTATION, value)

    var showPhonetic: Boolean
        get() = settings.getBoolean(PHONETIC, true)
        set(value) = settings.putBoolean(PHONETIC, value)

    private companion object {
        const val DICT_ID = "typeword.dictId"
        const val CHAPTER = "typeword.chapterIndex"
        const val UK = "typeword.ukAccent"
        const val DICTATION = "typeword.dictation"
        const val PHONETIC = "typeword.showPhonetic"
    }
}
