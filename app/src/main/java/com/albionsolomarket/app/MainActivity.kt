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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
private fun tierItems(t:Int):LinkedHashMap<String,String>{val m=linkedMapOf<String,String>();fun add(id:String,n:String){m[id]=n}
 listOf("ORE" to "Minério","WOOD" to "Madeira","HIDE" to "Couro","FIBER" to "Fibra","ROCK" to "Pedra").forEach{(id,n)->add("T${t}_${id}","${n} T$t")}
 listOf("SWORD" to "Espada","2HSWORD" to "Espada larga","AXE" to "Machado","2HAXE" to "Machado 2M","MACE" to "Maça","2HMACE" to "Maça 2M","HAMMER" to "Martelo","2HHAMMER" to "Martelo 2M","SPEAR" to "Lança","2HSPEAR" to "Lança 2M","BOW" to "Arco","CROSSBOW" to "Besta","FIRESTAFF" to "Cajado de fogo","FROSTSTAFF" to "Cajado de gelo","HOLYSTAFF" to "Cajado sagrado","NATURESTAFF" to "Cajado da natureza","ARCANESTAFF" to "Cajado arcano","CURSEDSTAFF" to "Cajado amaldiçoado").forEach{(id,n)->add("T${t}_MAIN_${id}","${n} T$t")}
 listOf("PLATE_SET1" to "Soldado","LEATHER_SET1" to "Mercenário","CLOTH_SET1" to "Erudito").forEach{(set,n)->{add("T${t}_HEAD_${set}","Capacete ${n} T$t");add("T${t}_ARMOR_${set}","Armadura ${n} T$t");add("T${t}_SHOES_${set}","Calçados ${n} T$t")}}
 return m}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("albion_sm_preferences",Context.MODE_PRIVATE)}
 var theme by remember{mutableStateOf(enumValueOrDefault(prefs.getString("theme",null),AppTheme.AUTO))}
 var palette by remember{mutableStateOf(loadPalette(prefs))}
 var capital by remember{mutableStateOf(prefs.getString("capital","500000")?:"500000")}
 var mins by remember{mutableStateOf(prefs.getString("mins","45")?:"45")}
 var hours by remember{mutableStateOf(prefs.getString("hours","0")?:"0")}
 var mode by remember{mutableStateOf(enumValueOrDefault(prefs.getString("mode",null),Mode.TRADE))}
 var origin by remember{mutableStateOf(validCity(prefs.getString("origin",null),"Bridgewatch"))}
 var destination by remember{mutableStateOf(validDestination(prefs.getString("destination",null),"Martlock"))}
 var flipCity by remember{mutableStateOf(validCity(prefs.getString("flipCity",null),"Bridgewatch"))}
 var blackMarketFlip by remember{mutableStateOf(prefs.getBoolean("blackMarketFlip",false))}
 var tier by remember{mutableIntStateOf(prefs.getInt("tier",4))}
 var page by remember{mutableIntStateOf(0)}
 var opportunities by remember{mutableStateOf<List<Opportunity>>(emptyList())}
 var loading by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var lastUpdate by remember{mutableStateOf<String?>(null)}
 val scope=rememberCoroutineScope(); val client=remember{AodpClient()}
 LaunchedEffect(theme,palette,capital,mins,hours,mode,origin,destination,flipCity,blackMarketFlip,tier){prefs.edit().putString("theme",theme.name).putString("palette",palette.name).putString("accent",palette.name).putString("capital",capital).putString("mins",mins).putString("hours",hours).putString("mode",mode.name).putString("origin",origin).putString("destination",destination).putString("flipCity",flipCity).putBoolean("blackMarketFlip",blackMarketFlip).putInt("tier",tier).apply()}
 val dark=when(theme){AppTheme.AUTO->isSystemInDarkTheme();AppTheme.DARK->true;AppTheme.LIGHT->false}
 val focusManager=LocalFocusManager.current
 fun refresh(){
  if(loading)return
  if(mode!=Mode.TRADE){opportunities=emptyList();error=null;return}
  val cap=capital.toDoubleOrNull();val travel=(hours.toIntOrNull()?:0)*60+(mins.toIntOrNull()?:0)
  if(cap==null||cap<=0||travel<=0){error="Informe Prata inicial e tempo de jogo válidos.";opportunities=emptyList();return}
  loading=true;error=null
  val monitoredItems=tierItems(tier)
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
  Column(Modifier.fillMaxWidth().clickable(indication=null,interactionSource=remember{MutableInteractionSource()}){focusManager.clearFocus()}.padding(if(wide)32.dp else 18.dp).widthIn(max=900.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Text("Albion Solo Market",style=MaterialTheme.typography.headlineMedium);Text("Americas • Beta 0.4");Text("Qual é a melhor maneira de tentar ganhar prata agora?",style=MaterialTheme.typography.titleMedium)
   Input(capital,{capital=it},"Prata inicial");Text("Tempo de jogo",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Input(hours,{hours=it},"Horas",Modifier.weight(1f));Input(mins,{mins=it},"Minutos",Modifier.weight(1f))};PlaytimeHealth(hours,mins)
   Text("Modo");Mode.entries.forEach{item->FilterChip(selected=mode==item,onClick={mode=item;opportunities=emptyList();error=null},label={Text(item.label)})}
   if(mode==Mode.TRADE){Text("Rota",style=MaterialTheme.typography.titleMedium)
    if(wide)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){CitySelector("Cidade de origem",origin,cities.filter{it!=destination&&!(destination=="Black Market"&&it=="Caerleon")},{origin=it;opportunities=emptyList()},Modifier.weight(1f));CitySelector("Cidade de destino",destination,tradeDestinations.filter{it!=origin&&!(origin=="Caerleon"&&it=="Black Market")},{destination=it;opportunities=emptyList()},Modifier.weight(1f))}
    else{CitySelector("Cidade de origem",origin,cities.filter{it!=destination},{origin=it;opportunities=emptyList()});CitySelector("Cidade de destino",destination,tradeDestinations.filter{it!=origin},{destination=it;opportunities=emptyList()})}
    RouteRisk(origin,destination)
   }
   if(mode==Mode.LOCAL){Text("Flip Local",style=MaterialTheme.typography.titleMedium);CitySelector("Cidade",flipCity,cities,{flipCity=it;blackMarketFlip=false;opportunities=emptyList()});if(flipCity=="Caerleon"){Text("Tipo de flip",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=!blackMarketFlip,onClick={blackMarketFlip=false},label={Text(if(!blackMarketFlip)"✓ Market Flip" else "Market Flip")});FilterChip(selected=blackMarketFlip,onClick={blackMarketFlip=true},label={Text(if(blackMarketFlip)"✓ Black Market Flip" else "Black Market Flip")})};Text("Selecionado: "+if(blackMarketFlip)"BLACK MARKET FLIP — Caerleon Market → Black Market" else "MARKET FLIP — compra e revenda no mercado de Caerleon",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleSmall);if(blackMarketFlip)Text("Caerleon ↔ Black Market • mesma cidade • sem travessia de zona vermelha.",style=MaterialTheme.typography.bodySmall)};Text("Flip Local compara compra e revenda dentro do mesmo mercado. Em Caerleon, Black Market Flip é o termo usado aqui para negociar entre o mercado de Caerleon e o Black Market.",style=MaterialTheme.typography.bodySmall)}
   Button(onClick={refresh()},enabled=!loading&&mode==Mode.TRADE){Text(if(loading)"Atualizando..." else "Atualizar preços")}
   if((hours.toIntOrNull()?:0)*60+(mins.toIntOrNull()?:0)>=120) WellnessCard();lastUpdate?.let{Text("Última atualização: $it",style=MaterialTheme.typography.bodySmall)};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
   HorizontalDivider();Text("Oportunidades",style=MaterialTheme.typography.titleLarge);Text("Tier");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(1..4).forEach{t->FilterChip(selected=tier==t,onClick={tier=t;page=0;opportunities=emptyList()},label={Text("T$t")})}};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(5..8).forEach{t->FilterChip(selected=tier==t,onClick={tier=t;page=0;opportunities=emptyList()},label={Text("T$t")})}}
   if(mode!=Mode.TRADE){Text("SEM DADOS",style=MaterialTheme.typography.titleMedium);Text(if(blackMarketFlip&&flipCity=="Caerleon")"Black Market Flip será conectado aos preços reais em uma próxima etapa." else "Ainda não há oportunidade confiável carregada para este modo.")}
   else if(opportunities.isEmpty()){Text("SEM DADOS",style=MaterialTheme.typography.titleMedium);Text("Nenhuma oportunidade real e lucrativa foi carregada para esta rota.")}
   else {val visible=opportunities.drop(page*3).take(3);visible.forEach{OpportunityCard(it,tierItems(tier)[it.item]?:it.item)};if(opportunities.size>3)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){Text("Anterior")};OutlinedButton(onClick={page=if((page+1)*3<opportunities.size)page+1 else 0}){Text("Próximas 3")}}}
   Text(MarketEngine.nextBestAction(opportunities));HorizontalDivider();Text("Aparência")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AppTheme.entries.forEach{t->FilterChip(theme==t,{theme=t},{Text(when(t){AppTheme.AUTO->"Auto";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"})})}}
   Text("Paleta");Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Palette.entries.chunked(3).forEach{group->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){group.forEach{p->FilterChip(palette==p,{palette=p},{Text(paletteLabel(p))})}}}}
   Text("Fonte dos preços: Albion Online Data Project",style=MaterialTheme.typography.bodySmall)
  }
 }}}
}

@Composable private fun OpportunityCard(o:Opportunity,name:String){val accent=MaterialTheme.colorScheme.primary;Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth()){Box(Modifier.width(5.dp).height(190.dp).background(accent));Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("$name (${o.item})",style=MaterialTheme.typography.titleMedium,color=accent);Text("${o.origin} → ${o.destination}");Text("Lucro líquido: ${fmt(o.net)}",style=MaterialTheme.typography.titleMedium,color=accent);Text("ROI: ${"%.2f".format(o.roi)}%  •  Prata/hora: ${fmt(o.silverHour)}",color=accent);Text("Comprar: ${fmt(o.buy)}  •  Vender: ${fmt(o.sell)}  •  Quantidade: ${o.qty}");Text("Idade do preço: ${o.ageMinutes} min  •  Confiança: ${confidenceLabel(o.confidence)}");Text("Fonte: Albion Online Data Project",style=MaterialTheme.typography.bodySmall)}}}}
@Composable fun CitySelector(label:String,value:String,options:List<String>,onSelected:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth()){var open by remember{mutableStateOf(false)};OutlinedButton(onClick={open=true},modifier=modifier){Text("$label: $value")};if(open)AlertDialog(onDismissRequest={},containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.90f),modifier=Modifier.widthIn(max=330.dp),title={Text(label)},text={Column(Modifier.widthIn(max=280.dp)){options.forEach{city->TextButton(onClick={onSelected(city);open=false},modifier=Modifier.fillMaxWidth()){Text(city)}}}},confirmButton={},dismissButton={TextButton(onClick={open=false}){Text("Cancelar")}})}
@Composable fun Input(v:String,on:(String)->Unit,label:String,m:Modifier=Modifier.fillMaxWidth())=OutlinedTextField(v,{on(it.filter(Char::isDigit))},label={Text(label)},modifier=m,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
private fun paletteScheme(p:Palette,dark:Boolean):ColorScheme{val primary=when(p){Palette.GRAY->Color(0xFF607080);Palette.PURPLE->Color(0xFF8B72C8);Palette.ORANGE->Color(0xFFE58A4A);Palette.GREEN->Color(0xFF65A878);Palette.GRAY_GREEN->Color(0xFF5D9B72);Palette.ORANGE_PURPLE->if(dark)Color(0xFFE9A06C)else Color(0xFF8B72C8)};val bg=when(p){Palette.GRAY->if(dark)Color(0xFF17191C)else Color(0xFFF2F3F4);Palette.PURPLE->if(dark)Color(0xFF1C1822)else Color(0xFFF5F1F9);Palette.ORANGE->if(dark)Color(0xFF211A16)else Color(0xFFFBF3EC);Palette.GREEN->if(dark)Color(0xFF17201A)else Color(0xFFF0F7F2);Palette.GRAY_GREEN->if(dark)Color(0xFF181D1A)else Color(0xFFF1F4F2);Palette.ORANGE_PURPLE->if(dark)Color(0xFF20191F)else Color(0xFFF8F1F5)};val surface=if(dark)bg.copy(red=(bg.red+.035f).coerceAtMost(1f),green=(bg.green+.035f).coerceAtMost(1f),blue=(bg.blue+.035f).coerceAtMost(1f))else bg.copy(red=(bg.red-.025f).coerceAtLeast(0f),green=(bg.green-.025f).coerceAtLeast(0f),blue=(bg.blue-.025f).coerceAtLeast(0f));val secondary=when(p){Palette.ORANGE_PURPLE->Color(0xFFE58A4A);Palette.GRAY_GREEN->Color(0xFF65A878);else->primary};return if(dark)darkColorScheme(primary=primary,secondary=secondary,background=bg,surface=surface,secondaryContainer=surface)else lightColorScheme(primary=primary,secondary=secondary,background=bg,surface=surface,secondaryContainer=surface)}
private fun loadPalette(p:android.content.SharedPreferences):Palette{p.getString("palette",null)?.let{return enumValueOrDefault(it,Palette.GRAY)};return when(p.getString("accent",null)){"ORANGE"->Palette.ORANGE;"PURPLE"->Palette.PURPLE;"GREEN"->Palette.GREEN;else->Palette.GRAY}}
private fun paletteLabel(p:Palette)=when(p){Palette.GRAY->"Cinza";Palette.PURPLE->"Roxo";Palette.ORANGE->"Laranja";Palette.GREEN->"Verde";Palette.GRAY_GREEN->"Cinza / Verde";Palette.ORANGE_PURPLE->"Laranja / Roxo"}
private fun confidenceLabel(c:Confidence)=when(c){Confidence.HIGH->"ALTA";Confidence.MEDIUM->"MÉDIA";Confidence.LOW->"BAIXA";Confidence.STALE->"DESATUALIZADA";Confidence.NO_DATA->"SEM DADOS"}
private fun fmt(v:Double)="%,.0f".format(v)
@Composable private fun RouteRisk(origin:String,destination:String){val high=origin=="Caerleon"||destination=="Caerleon"||destination=="Black Market";val risk=if(high)"ALTO" else "MODERADO";val note=if(high)"A rota pode exigir zona vermelha. Indicada para jogadores experientes; emboscadas, PvP e perda de carga podem alterar totalmente o tempo real." else "Risco MODERADO: sem estimativa artificial de viagem. O tempo real varia com montaria, carga, rota, facção, desvios e PvP.";Text("Risco: $risk",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary);Text(note,style=MaterialTheme.typography.bodySmall)}
@Composable private fun WellnessCard(){Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("Pausa saudável",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary);Text("Você planejou uma sessão longa. Que tal uma pausa? Beba água, levante-se, movimente o corpo e descanse os olhos. O mercado pode esperar um pouco 🙂")}}}
@Composable private fun PlaytimeHealth(hours:String,mins:String){val total=(hours.toIntOrNull()?:0)*60+(mins.toIntOrNull()?:0);if(total>=120)Text("Sessão longa. Faça pausas regulares, hidrate-se, movimente-se e preserve sono e atividades fora do jogo. A OMS recomenda atenção ao tempo de jogo, mas não define um limite universal de horas para adultos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary);else Text("Dica: faça pausas regulares, descanse os olhos e mantenha água por perto.",style=MaterialTheme.typography.bodySmall)}
private fun validCity(v:String?,fallback:String)=v?.takeIf{it in cities}?:fallback
private fun validDestination(v:String?,fallback:String)=v?.takeIf{it in tradeDestinations}?:fallback
private inline fun <reified T:Enum<T>> enumValueOrDefault(v:String?,fallback:T):T=enumValues<T>().firstOrNull{it.name==v}?:fallback
