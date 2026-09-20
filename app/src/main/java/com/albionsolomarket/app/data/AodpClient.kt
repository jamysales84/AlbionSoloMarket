package com.albionsolomarket.app.data
import okhttp3.OkHttpClient
import okhttp3.Request
class AodpClient(private val client:OkHttpClient=OkHttpClient()) {
 private val base="https://west.albion-online-data.com/api/v2/stats/prices/"
 fun pricesUrl(itemIds:List<String>,locations:List<String>):String = base + itemIds.joinToString(",") + "?locations=" + locations.joinToString(",")
 fun fetchRaw(itemIds:List<String>,locations:List<String>):Result<String> = runCatching {
  val req=Request.Builder().url(pricesUrl(itemIds,locations)).get().build()
  client.newCall(req).execute().use { r -> if(!r.isSuccessful) error("AODP HTTP ${r.code}"); r.body?.string() ?: error("Resposta vazia") }
 }
}
