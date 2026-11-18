package com.yeferson.videojuegolab13.game

import android.content.Context

class RecordStore(context: Context) {
    private val preferences = context.getSharedPreferences("defensa_espacial", Context.MODE_PRIVATE)

    fun load(): Int = preferences.getInt("record", 0)

    fun save(score: Int) {
        preferences.edit().putInt("record", score).apply()
    }
}
