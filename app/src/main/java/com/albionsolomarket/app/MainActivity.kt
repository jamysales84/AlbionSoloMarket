package com.albionsolomarket.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.albionsolomarket.app.data.AodpClient
import com.albionsolomarket.app.domain.Confidence
import com.albionsolomarket.app.domain.MarketEngine
import com.albionsolomarket.app.domain.Opportunity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

enum class AppTheme { AUTO, LIGHT, DARK }
enum class Palette { GRAY, PURPLE, ORANGE, GREEN, GRAY_GREEN, ORANGE_PURPLE }
enum class Mode(val label:String){ TRADE("Cidade → Cidade"), LOCAL("Flip Local") }
private val cities=listOf("Bridgewatch","Martlock","Thetford","Fort Sterling","Lymhurst","Caerleon")
private val tradeDestinations=cities+"Black Market"
private val monitoredItems=linkedMapOf("T4_ORE" to "Minério T4","T5_ORE" to "Minério T5","T4_WOOD" to "Madeira T4","T5_WOOD" to "Madeira T5","T4_HIDE" to "Couro T4","T5_HIDE" to "Couro T5","T4_FIBER" to "Fibra T4","T5_FIBER" to "Fibra T5","T4_ROCK" to "Pedra T4","T5_ROCK" to "Pedra T5")

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("albion_sm_preferences",Context.MODE_PRIVATE)}
 var theme by remember{mutableStateOf(enumValueOrDefault(prefs.getString("theme",null),AppTheme.AUTO))}
 var palette by remember{mutableStateOf(loadPalette(prefs))}
 var capital by remember{mutableStateOf(prefs.getString("capital","500000")?:"500000")}
 var mins by remember{mutableStateOf(prefs.getString("mins","45")?:"45")}
 var mode by remember{mutableStateOf(enumValueOrDefault(prefs.getString("mode",null),Mode.TRADE))}
 var origin by remember{mutableStateOf(validCity(prefs.getString("origin",null),"Bridgewatch"))}
 var destination by remember{mutableStateOf(validDestination(prefs.getString("destination",null),"Martlock"))}
 var flipCity by remember{mutableStateOf(validCity(prefs.getString("flipCity",null),"Bridgewatch"))}
 var blackMarketFlip by remember{mutableStateOf(prefs.getBoolean("blackMarketFlip",false))}
 var opportunities by remember{mutableStateOf<List<Opportunity>>(emptyList())}
 var loading by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var lastUpdate by remember{mutableStateOf<String?>(null)}
 val scope=rememberCoroutineScope(); val client=remember{AodpClient()}
 LaunchedEffect(theme,palette,capital,mins,mode,origin,destination,flipCity,blackMarketFlip){prefs.edit().putString("theme",theme.name).putString("palette",palette.name).putString("accent",palette.name).putString("capital",capital).putString("mins",mins).putString("mode",mode.name).putString("origin",origin).putString("destination",destination).putString("flipCity",flipCity).putBoolean("blackMarketFlip",blackMarketFlip).apply()}
 val dark=when(theme){AppTheme.AUTO->isSystemInDarkTheme();AppTheme.DARK->true;AppTheme.LIGHT->false}
 fun refresh(){
  if(loading)return
  if(mode!=Mode.TRADE){opportunities=emptyList();error=null;return}
  val cap=capital.toDoubleOrNull();val travel=mins.toIntOrNull()
  if(cap==null||cap<=0||travel==null||travel<=0){error="Informe Prata inicial e Minutos válidos.";opportunities=emptyList();return}
  loading=true;error=null
  scope.launch{
   val result=withContext(Dispatchers.IO){client.fetchPrices(monitoredItems.keys.toList(),listOf(origin,destination))}
   result.onSuccess{prices->
    val byKey=prices.associateBy{it.itemId to it.city}
    opportunities=monitoredItems.keys.mapNotNull{item->
     val from=byKey[item to origin];val to=byKey[item to destination]
     val age=if(from?.sellAgeMinutes!=null&&to?.buyAgeMinutes!=null)max(from.sellAgeMinutes,to.buyAgeMinutes)else null
     MarketEngine.cityToCity(item,origin,destination,from?.sellPrice,to?.buyPrice,cap,travel,age=age)
    }.filter{it.net>0}.sortedByDescending{it.silverHour}
    lastUpdate=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
   }.onFailure{opportunities=emptyList();error="Não foi possível atualizar os preços."}
   loading=false
  }
 }
 MaterialTheme(colorScheme=paletteScheme(palette,dark)){Surface(Modifier.fillMaxSize()){BoxWithConstraints{
  val wide=maxWidth>700.dp
  Column(Modifier.padding(if(wide)32.dp else 18.dp).widthIn(max=900.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Text("Albion Solo Market",style=MaterialTheme.typography.headlineMedium);Text("Americas • Beta 0.4");Text("Qual é a melhor maneira de tentar ganhar prata agora?",style=MaterialTheme.typography.titleMedium)
   if(wide)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Input(capital,{capital=it},"Prata inicial",Modifier.weight(1f));Input(mins,{mins=it},"Tempo da rota (min)",Modifier.weight(1f))}else{Input(capital,{capital=it},"Prata inicial");Input(mins,{mins=it},"Tempo da rota (min)")}
   Text("Modo");Mode.entries.forEach{item->FilterChip(selected=mode==item,onClick={mode=item;opportunities=emptyList();error=null},label={Text(item.label)})}
   if(mode==Mode.TRADE){Text("Rota",style=MaterialTheme.typography.titleMedium)
    if(wide)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){CitySelector("Cidade de origem",origin,cities.filter{it!=destination},{origin=it;opportunities=emptyList()},Modifier.weight(1f));CitySelector("Cidade de destino",destination,tradeDestinations.filter{it!=origin},{destination=it;opportunities=emptyList()},Modifier.weight(1f))}
    else{CitySelector("Cidade de origem",origin,cities.filter{it!=destination},{origin=it;opportunities=emptyList()});CitySelector("Cidade de destino",destination,tradeDestinations.filter{it!=origin},{destination=it;opportunities=emptyList()})}
    RouteRisk(origin,destination,mins)
   }
   if(mode==Mode.LOCAL){Text("Flip Local",style=MaterialTheme.typography.titleMedium);CitySelector("Cidade",flipCity,cities,{flipCity=it;blackMarketFlip=false;opportunities=emptyList()});if(flipCity=="Caerleon"){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=!blackMarketFlip,onClick={blackMarketFlip=false},label={Text("Market Flip")});FilterChip(selected=blackMarketFlip,onClick={blackMarketFlip=true},label={Text("Black Market Flip")})};if(blackMarketFlip)Text("Caerleon ↔ Black Market • mesma cidade • sem travessia de zona vermelha.",style=MaterialTheme.typography.bodySmall)};Text("Flip Local compara compra e revenda dentro do mesmo mercado. Em Caerleon, Black Market Flip é o termo usado aqui para negociar entre o mercado de Caerleon e o Black Market.",style=MaterialTheme.typography.bodySmall)}
   Button(onClick={refresh()},enabled=!loading&&mode==Mode.TRADE){Text(if(loading)"Atualizando..." else "Atualizar preços")}
   lastUpdate?.let{Text("Última atualização: $it",style=MaterialTheme.typography.bodySmall)};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
   HorizontalDivider();Text("Oportunidades",style=MaterialTheme.typography.titleLarge)
   if(mode!=Mode.TRADE){Text("SEM DADOS",style=MaterialTheme.typography.titleMedium);Text(if(blackMarketFlip&&flipCity=="Caerleon")"Black Market Flip será conectado aos preços reais em uma próxima etapa." else "Ainda não há oportunidade confiável carregada para este modo.")}
   else if(opportunities.isEmpty()){Text("SEM DADOS",style=MaterialTheme.typography.titleMedium);Text("Nenhuma oportunidade real e lucrativa foi carregada para esta rota.")}
   else opportunities.forEach{OpportunityCard(it,monitoredItems[it.item]?:it.item)}
   Text(MarketEngine.nextBestAction(opportunities));HorizontalDivider();Text("Aparência")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AppTheme.entries.forEach{t->FilterChip(theme==t,{theme=t},{Text(when(t){AppTheme.AUTO->"Auto";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"})})}}
   Text("Paleta");Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Palette.entries.chunked(3).forEach{group->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){group.forEach{p->FilterChip(palette==p,{palette=p},{Text(paletteLabel(p))})}}}}
   Text("Fonte dos preços: Albion Online Data Project",style=MaterialTheme.typography.bodySmall)
  }
 }}}
}

@Composable private fun OpportunityCard(o:Opportunity,name:String){val accent=MaterialTheme.colorScheme.primary;Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth()){Box(Modifier.width(5.dp).height(190.dp).background(accent));Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("$name (${o.item})",style=MaterialTheme.typography.titleMedium,color=accent);Text("${o.origin} → ${o.destination}");Text("Lucro líquido: ${fmt(o.net)}",style=MaterialTheme.typography.titleMedium,color=accent);Text("ROI: ${"%.2f".format(o.roi)}%  •  Prata/hora: ${fmt(o.silverHour)}",color=accent);Text("Comprar: ${fmt(o.buy)}  •  Vender: ${fmt(o.sell)}  •  Quantidade: ${o.qty}");Text("Idade do preço: ${o.ageMinutes} min  •  Confiança: ${confidenceLabel(o.confidence)}");Text("Fonte: Albion Online Data Project",style=MaterialTheme.typography.bodySmall)}}}}
@Composable fun CitySelector(label:String,value:String,options:List<String>,onSelected:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth()){var open by remember{mutableStateOf(false)};OutlinedButton(onClick={open=true},modifier=modifier){Text("$label: $value")};if(open)AlertDialog(onDismissRequest={},modifier=Modifier.widthIn(max=360.dp),title={Text(label)},text={Column(Modifier.widthIn(max=300.dp)){options.forEach{city->TextButton(onClick={onSelected(city);open=false},modifier=Modifier.fillMaxWidth()){Text(city)}}}},confirmButton={},dismissButton={TextButton(onClick={open=false}){Text("Cancelar")}})}
@Composable fun Input(v:String,on:(String)->Unit,label:String,m:Modifier=Modifier.fillMaxWidth())=OutlinedTextField(v,{on(it.filter(Char::isDigit))},label={Text(label)},modifier=m)
private fun paletteScheme(p:Palette,dark:Boolean):ColorScheme{val primary=when(p){Palette.GRAY->Color(0xFF607080);Palette.PURPLE->Color(0xFF8B72C8);Palette.ORANGE->Color(0xFFE58A4A);Palette.GREEN->Color(0xFF65A878);Palette.GRAY_GREEN->Color(0xFF5D9B72);Palette.ORANGE_PURPLE->if(dark)Color(0xFFE9A06C)else Color(0xFF8B72C8)};val bg=when(p){Palette.GRAY->if(dark)Color(0xFF17191C)else Color(0xFFF2F3F4);Palette.PURPLE->if(dark)Color(0xFF1C1822)else Color(0xFFF5F1F9);Palette.ORANGE->if(dark)Color(0xFF211A16)else Color(0xFFFBF3EC);Palette.GREEN->if(dark)Color(0xFF17201A)else Color(0xFFF0F7F2);Palette.GRAY_GREEN->if(dark)Color(0xFF181D1A)else Color(0xFFF1F4F2);Palette.ORANGE_PURPLE->if(dark)Color(0xFF20191F)else Color(0xFFF8F1F5)};val surface=if(dark)bg.copy(red=(bg.red+.035f).coerceAtMost(1f),green=(bg.green+.035f).coerceAtMost(1f),blue=(bg.blue+.035f).coerceAtMost(1f))else Color.White;val secondary=when(p){Palette.ORANGE_PURPLE->Color(0xFFE58A4A);Palette.GRAY_GREEN->Color(0xFF65A878);else->primary};return if(dark)darkColorScheme(primary=primary,secondary=secondary,background=bg,surface=surface,secondaryContainer=surface)else lightColorScheme(primary=primary,secondary=secondary,background=bg,surface=surface,secondaryContainer=surface)}
private fun loadPalette(p:android.content.SharedPreferences):Palette{p.getString("palette",null)?.let{return enumValueOrDefault(it,Palette.GRAY)};return when(p.getString("accent",null)){"ORANGE"->Palette.ORANGE;"PURPLE"->Palette.PURPLE;"GREEN"->Palette.GREEN;else->Palette.GRAY}}
private fun paletteLabel(p:Palette)=when(p){Palette.GRAY->"Cinza";Palette.PURPLE->"Roxo";Palette.ORANGE->"Laranja";Palette.GREEN->"Verde";Palette.GRAY_GREEN->"Cinza / Verde";Palette.ORANGE_PURPLE->"Laranja / Roxo"}
private fun confidenceLabel(c:Confidence)=when(c){Confidence.HIGH->"ALTA";Confidence.MEDIUM->"MÉDIA";Confidence.LOW->"BAIXA";Confidence.STALE->"DESATUALIZADA";Confidence.NO_DATA->"SEM DADOS"}
private fun fmt(v:Double)="%,.0f".format(v)
@Composable private fun RouteRisk(origin:String,destination:String,mins:String){val high=origin=="Caerleon"||destination=="Caerleon"||destination=="Black Market";val risk=if(high)"ALTO" else "MODERADO";val note=if(high)"A rota pode exigir zona vermelha. Indicada para jogadores experientes; emboscadas, PvP e perda de carga podem alterar totalmente o tempo real." else "A rota pode ser feita sem zona vermelha, mas regras de facção, PvP e desvios ainda podem alterar o tempo real.";Text("Tempo da rota: ${mins.ifBlank{"—"}} min • Risco: $risk",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary);Text(note,style=MaterialTheme.typography.bodySmall)}
private fun validCity(v:String?,fallback:String)=v?.takeIf{it in cities}?:fallback
private fun validDestination(v:String?,fallback:String)=v?.takeIf{it in tradeDestinations}?:fallback
private inline fun <reified T:Enum<T>> enumValueOrDefault(v:String?,fallback:T):T=enumValues<T>().firstOrNull{it.name==v}?:fallback
