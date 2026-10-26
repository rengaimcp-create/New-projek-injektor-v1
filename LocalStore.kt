package com.emeng.proxxy.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Preset(val name: String, val size: Float, val opacity: Float, val color: String, val shape: String)
object LocalStore {
    private const val FILE = "emeng_settings"
    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun size(c: Context) = p(c).getFloat("size", 28f)
    fun opacity(c: Context) = p(c).getFloat("opacity", .9f)
    fun color(c: Context) = p(c).getString("color", "CYAN") ?: "CYAN"
    fun shape(c: Context) = p(c).getString("shape", "PLUS") ?: "PLUS"
    fun saveVisual(c: Context, size: Float, opacity: Float, color: String, shape: String) {
        p(c).edit().putFloat("size", size.coerceIn(8f,72f)).putFloat("opacity", opacity.coerceIn(.15f,1f)).putString("color",color).putString("shape",shape).apply()
    }
    fun presets(c: Context): List<Preset> = try {
        val a = JSONArray(p(c).getString("presets", "[]"))
        (0 until a.length()).map { val o=a.getJSONObject(it); Preset(o.getString("name"),o.optDouble("size",28.0).toFloat(),o.optDouble("opacity",.9).toFloat(),o.optString("color","CYAN"),o.optString("shape","PLUS")) }
    } catch (_: Exception) { emptyList() }
    fun savePreset(c: Context, item: Preset) {
        val all = presets(c).filterNot { it.name.equals(item.name,true) } + item.copy(name=item.name.take(32))
        val a=JSONArray(); all.forEach { a.put(JSONObject().put("name",it.name).put("size",it.size).put("opacity",it.opacity).put("color",it.color).put("shape",it.shape)) }
        p(c).edit().putString("presets",a.toString()).apply()
    }
    fun deletePreset(c: Context, name: String) {
        val a=JSONArray(); presets(c).filterNot { it.name==name }.forEach { a.put(JSONObject().put("name",it.name).put("size",it.size).put("opacity",it.opacity).put("color",it.color).put("shape",it.shape)) }; p(c).edit().putString("presets",a.toString()).apply()
    }
    fun reset(c: Context) = p(c).edit().clear().apply()
}
