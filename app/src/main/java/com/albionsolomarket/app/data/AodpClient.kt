package com.albionsolomarket.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.Duration
import java.time.Instant

data class AodpPrice(
 val itemId:String,
 val city:String,
 val sellPrice:Double?,
 val buyPrice:Double?,
 val sellAgeMinutes:Int?,
 val buyAgeMinutes:Int?
)

class AodpClient(private val client:OkHttpClient=OkHttpClient()) {
 private val base="https://west.albion-online-data.com/api/v2/stats/prices/"
 fun pricesUrl(itemIds:List<String>,locations:List<String>):String =
  base + itemIds.joinToString(",") + "?locations=" + locations.joinToString(",") + "&qualities=1"

 fun fetchRaw(itemIds:List<String>,locations:List<String>):Result<String> = runCatching {
  val req=Request.Builder().url(pricesUrl(itemIds,locations)).get().build()
  client.newCall(req).execute().use { r ->
   if(!r.isSuccessful) error("AODP HTTP ${r.code}")
   r.body?.string() ?: error("Resposta vazia")
  }
 }

 fun fetchPrices(itemIds:List<String>,locations:List<String>):Result<List<AodpPrice>> =
  fetchRaw(itemIds,locations).mapCatching(::parsePrices)

 internal fun parsePrices(raw:String,now:Instant=Instant.now()):List<AodpPrice>{
  val array=JSONArray(raw)
  return buildList {
   for(i in 0 until array.length()){
    val o=array.getJSONObject(i)
    val sell=o.optDouble("sell_price_min",0.0).takeIf{it>0}
    val buy=o.optDouble("buy_price_max",0.0).takeIf{it>0}
    add(AodpPrice(
     itemId=o.getString("item_id"),
     city=o.getString("city"),
     sellPrice=sell,
     buyPrice=buy,
     sellAgeMinutes=if(sell!=null) ageMinutes(o.optString("sell_price_min_date"),now) else null,
     buyAgeMinutes=if(buy!=null) ageMinutes(o.optString("buy_price_max_date"),now) else null
    ))
   }
  }
 }

 private fun ageMinutes(value:String,now:Instant):Int? = runCatching {
  val instant=Instant.parse(if(value.endsWith("Z")) value else value+"Z")
  Duration.between(instant,now).toMinutes().coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
 }.getOrNull()
}
