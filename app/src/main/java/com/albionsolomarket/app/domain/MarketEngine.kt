package com.albionsolomarket.app.domain
import kotlin.math.floor

enum class Confidence { HIGH, MEDIUM, LOW, STALE, NO_DATA }
data class Quote(val item:String,val city:String,val sellPrice:Double?,val buyPrice:Double?,val ageMinutes:Int?,val source:String="Albion Online Data Project")
data class Opportunity(val item:String,val origin:String,val destination:String,val buy:Double,val sell:Double,val qty:Int,val net:Double,val roi:Double,val silverHour:Double,val confidence:Confidence,val ageMinutes:Int)
object MarketEngine {
 fun confidence(age:Int?, hasPrices:Boolean, liquid:Boolean=true)=when { !hasPrices||age==null->Confidence.NO_DATA; age<=15&&liquid->Confidence.HIGH; age<=30->Confidence.MEDIUM; age<=60->Confidence.LOW; else->Confidence.STALE }
 fun cityToCity(item:String, origin:String,destination:String,buy:Double?,sell:Double?,capital:Double,minutes:Int,taxRate:Double=0.065,age:Int?):Opportunity? {
  if(buy==null||sell==null||buy<=0||sell<=0||capital<=0||minutes<=0)return null
  val qty=floor(capital/buy).toInt(); if(qty<=0)return null
  val net=(sell*(1-taxRate)-buy)*qty; val invested=buy*qty; val roi=net/invested*100; val sph=net*60/minutes
  return Opportunity(item,origin,destination,buy,sell,qty,net,roi,sph,confidence(age,true),age?:9999)
 }
 fun nextBestAction(opportunities:List<Opportunity>) = opportunities.filter{it.net>0 && it.confidence!=Confidence.STALE}.maxByOrNull{it.silverHour}?.let{"Melhor ação agora: ${it.origin} → ${it.destination}, ${it.item}, ~${it.silverHour.toInt()} prata/h."} ?: "Mercado nebuloso 👀. Sem oportunidade confiável agora: atualize os preços, tente Flip Local ou outra cidade. Não vamos inventar uma operação."
}
