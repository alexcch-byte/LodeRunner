package com.example.loderunner.game.levels

import android.content.Context
import android.content.SharedPreferences
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.TileType
import org.json.JSONArray
import org.json.JSONObject

class LevelManager(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("lode_runner_prefs", Context.MODE_PRIVATE)
    private val customLevels = mutableListOf<LevelData>()

    init {
        loadCustomLevelsFromPrefs()
    }

    fun getClassicLevelCount(): Int = ClassicLevels.ALL_LEVELS.size

    fun getClassicLevel(levelNum: Int): LevelData = ClassicLevels.getLevel(levelNum)

    fun getCustomLevels(): List<LevelData> = customLevels.toList()

    fun getCustomLevel(index: Int): LevelData? {
        if (index in customLevels.indices) {
            return customLevels[index].clone()
        }
        return null
    }

    fun saveCustomLevel(level: LevelData): Int {
        val existingIndex = customLevels.indexOfFirst { it.name == level.name }
        if (existingIndex >= 0) {
            customLevels[existingIndex] = level.clone()
            saveCustomLevelsToPrefs()
            return existingIndex
        } else {
            val assignedId = 1000 + customLevels.size
            val newLevel = level.copy(id = assignedId)
            customLevels.add(newLevel)
            saveCustomLevelsToPrefs()
            return customLevels.size - 1
        }
    }

    fun deleteCustomLevel(index: Int) {
        if (index in customLevels.indices) {
            customLevels.removeAt(index)
            saveCustomLevelsToPrefs()
        }
    }

    fun getHighScore(): Int {
        return prefs?.getInt("high_score", 0) ?: 0
    }

    fun saveHighScore(newScore: Int) {
        val currentHigh = getHighScore()
        if (newScore > currentHigh) {
            prefs?.edit()?.putInt("high_score", newScore)?.apply()
        }
    }

    private fun saveCustomLevelsToPrefs() {
        prefs ?: return
        val jsonArray = JSONArray()
        for (lvl in customLevels) {
            val obj = JSONObject()
            obj.put("id", lvl.id)
            obj.put("name", lvl.name)
            obj.put("runnerX", lvl.runnerStartX)
            obj.put("runnerY", lvl.runnerStartY)

            val gridArray = JSONArray()
            for (r in 0 until LevelData.ROWS) {
                val rowArray = JSONArray()
                for (c in 0 until LevelData.COLS) {
                    rowArray.put(lvl.grid[r][c])
                }
                gridArray.put(rowArray)
            }
            obj.put("grid", gridArray)

            val spawnsArray = JSONArray()
            for (sp in lvl.enemySpawns) {
                val spObj = JSONObject()
                spObj.put("x", sp.first)
                spObj.put("y", sp.second)
                spawnsArray.put(spObj)
            }
            obj.put("spawns", spawnsArray)

            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_levels_json", jsonArray.toString()).apply()
    }

    private fun loadCustomLevelsFromPrefs() {
        prefs ?: return
        val jsonStr = prefs.getString("custom_levels_json", null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            customLevels.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optInt("id", 1000 + i)
                val name = obj.optString("name", "Custom ${i + 1}")
                val rx = obj.optInt("runnerX", 14)
                val ry = obj.optInt("runnerY", 14)

                val gridArray = obj.getJSONArray("grid")
                val grid = Array(LevelData.ROWS) { r ->
                    val rowArray = gridArray.getJSONArray(r)
                    IntArray(LevelData.COLS) { c -> rowArray.getInt(c) }
                }

                val spawns = mutableListOf<Pair<Int, Int>>()
                if (obj.has("spawns")) {
                    val spawnsArray = obj.getJSONArray("spawns")
                    for (s in 0 until spawnsArray.length()) {
                        val spObj = spawnsArray.getJSONObject(s)
                        spawns.add(Pair(spObj.getInt("x"), spObj.getInt("y")))
                    }
                }

                customLevels.add(LevelData(id, name, grid, rx, ry, spawns))
            }
        } catch (_: Exception) {
            // Fallback gracefully on parsing issues
        }
    }
}
