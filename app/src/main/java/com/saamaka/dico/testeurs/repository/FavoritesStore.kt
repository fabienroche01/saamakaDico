package com.saamaka.dico.testeurs.repository

import android.content.Context

class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_favorites",
        Context.MODE_PRIVATE
    )

    fun isFavorite(id: Int): Boolean =
        preferences.getBoolean("favorite_$id", false)

    fun setFavorite(id: Int, favorite: Boolean) {
        preferences.edit()
            .putBoolean("favorite_$id", favorite)
            .apply()
    }

    fun favoriteIds(): Set<Int> =
        preferences.all
            .filter { (key, value) ->
                key.startsWith("favorite_") && value == true
            }
            .mapNotNull { (key, _) ->
                key.removePrefix("favorite_").toIntOrNull()
            }
            .toSet()
}