package com.albionsolomarket.app.domain
import org.junit.Assert.*
import org.junit.Test
class MarketEngineTest {
 @Test fun missingPriceNeverBecomesZero(){ assertNull(MarketEngine.cityToCity("T4_ORE","Fort Sterling","Lymhurst",null,100.0,500000.0,45,age=5)) }
 @Test fun calculatesNetRoiAndQuantity(){ val x=MarketEngine.cityToCity("T4_ORE","A","B",100.0,130.0,1000.0,30,taxRate=.10,age=5)!!; assertEquals(10,x.qty); assertEquals(170.0,x.net,.001); assertEquals(17.0,x.roi,.001); assertEquals(340.0,x.silverHour,.001) }
 @Test fun staleIsMarked(){ assertEquals(Confidence.STALE,MarketEngine.confidence(90,true)) }
}
