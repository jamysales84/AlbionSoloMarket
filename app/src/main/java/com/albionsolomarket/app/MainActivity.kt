package com.albionsolomarket.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.albionsolomarket.app.domain.MarketEngine

enum class AppTheme { AUTO, LIGHT, DARK }
enum class AccentColor { DEFAULT, ORANGE, PURPLE, GREEN }
enum class Mode(val label:String){ TRADE("Cidade → Cidade"), LOCAL("Flip Local"), BLACK("Caerleon / Black Market") }

private val cities = listOf("Bridgewatch", "Martlock", "Thetford", "Fort Sterling", "Lymhurst", "Caerleon")

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){ super.onCreate(b); setContent{ App() } }
}

@Composable fun App(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("albion_sm_preferences",Context.MODE_PRIVATE)}
 var theme by remember{mutableStateOf(enumValueOrDefault(prefs.getString("theme",null),AppTheme.AUTO))}
 var accent by remember{mutableStateOf(enumValueOrDefault(prefs.getString("accent",null),AccentColor.DEFAULT))}
 var capital by remember{mutableStateOf(prefs.getString("capital","500000") ?: "500000")}
 var mins by remember{mutableStateOf(prefs.getString("mins","45") ?: "45")}
 var mode by remember{mutableStateOf(enumValueOrDefault(prefs.getString("mode",null),Mode.TRADE))}
 var origin by remember{mutableStateOf(validCity(prefs.getString("origin",null),"Bridgewatch"))}
 var destination by remember{mutableStateOf(validCity(prefs.getString("destination",null),"Martlock"))}

 LaunchedEffect(theme,accent,capital,mins,mode,origin,destination){
  prefs.edit().putString("theme",theme.name).putString("accent",accent.name)
   .putString("capital",capital).putString("mins",mins).putString("mode",mode.name)
   .putString("origin",origin).putString("destination",destination).apply()
 }

 val dark=when(theme){AppTheme.AUTO->isSystemInDarkTheme();AppTheme.DARK->true;AppTheme.LIGHT->false}
 val base=if(dark) darkColorScheme() else lightColorScheme()
 val scheme=accentScheme(base,accent,dark)

 MaterialTheme(colorScheme=scheme){
  Surface(Modifier.fillMaxSize()){
   BoxWithConstraints{
    val wide=maxWidth>700.dp
    Column(
     Modifier.padding(if(wide)32.dp else 18.dp).widthIn(max=900.dp).verticalScroll(rememberScrollState()),
     verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
     Text("Albion Solo Market",style=MaterialTheme.typography.headlineMedium)
     Text("Americas • Beta 0.2")
     Text("Qual é a melhor maneira de tentar ganhar prata agora?",style=MaterialTheme.typography.titleMedium)

     if(wide) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
      Input(capital,{capital=it},"Prata inicial",Modifier.weight(1f))
      Input(mins,{mins=it},"Minutos",Modifier.weight(1f))
     } else {
      Input(capital,{capital=it},"Prata inicial")
      Input(mins,{mins=it},"Minutos")
     }

     Text("Modo")
     Mode.entries.forEach{item->FilterChip(selected=mode==item,onClick={mode=item},label={Text(item.label)})}

     if(mode==Mode.TRADE){
      Text("Rota",style=MaterialTheme.typography.titleMedium)
      if(wide) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
       CitySelector("Cidade de origem",origin,cities.filter{it!=destination},{origin=it},Modifier.weight(1f))
       CitySelector("Cidade de destino",destination,cities.filter{it!=origin},{destination=it},Modifier.weight(1f))
      } else {
       CitySelector("Cidade de origem",origin,cities.filter{it!=destination},{origin=it})
       CitySelector("Cidade de destino",destination,cities.filter{it!=origin},{destination=it})
      }
     }

     HorizontalDivider()
     Text("Oportunidades",style=MaterialTheme.typography.titleLarge)
     Text("SEM DADOS",style=MaterialTheme.typography.titleMedium)
     Text("Nenhum preço confiável foi carregado. Cada preço exibirá fonte, horário e confiança.")
     Text(MarketEngine.nextBestAction(emptyList()))

     HorizontalDivider()
     Text("Aparência")
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      AppTheme.entries.forEach{t->FilterChip(theme==t,{theme=t},{Text(when(t){AppTheme.AUTO->"Auto";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"})})}
     }
     Text("Cor de destaque")
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      AccentColor.entries.forEach{a->FilterChip(accent==a,{accent=a},{Text(when(a){AccentColor.DEFAULT->"Padrão";AccentColor.ORANGE->"Laranja";AccentColor.PURPLE->"Roxo";AccentColor.GREEN->"Verde"})})}
     }
     Text("Lucro líquido • ROI • prata/hora • quantidade pela prata inicial • origem/destino • idade do preço • confiança",style=MaterialTheme.typography.bodySmall)
    }
   }
  }
 }
}

@Composable
fun CitySelector(label:String,value:String,options:List<String>,onSelected:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth()){
 var expanded by remember{mutableStateOf(false)}
 Box(modifier){
  OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text("$label: $value")}
  DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
   options.forEach{city->DropdownMenuItem(text={Text(city)},onClick={onSelected(city);expanded=false})}
  }
 }
}

@Composable
fun Input(v:String,on:(String)->Unit,label:String,m:Modifier=Modifier.fillMaxWidth())=
 OutlinedTextField(v,{on(it.filter(Char::isDigit))},label={Text(label)},modifier=m)

private fun accentScheme(base:ColorScheme,accent:AccentColor,dark:Boolean):ColorScheme{
 if(accent==AccentColor.DEFAULT)return base
 val primary=when(accent){
  AccentColor.ORANGE->Color(0xFFE58A4A)
  AccentColor.PURPLE->Color(0xFF8B72C8)
  AccentColor.GREEN->Color(0xFF65A878)
  AccentColor.DEFAULT->base.primary
 }
 val onPrimary=if(dark) Color(0xFF111111) else Color.White
 return base.copy(primary=primary,onPrimary=onPrimary,secondary=primary,onSecondary=onPrimary,tertiary=primary)
}

private fun validCity(value:String?,fallback:String)=value?.takeIf{it in cities} ?: fallback
private inline fun <reified T:Enum<T>> enumValueOrDefault(value:String?,fallback:T):T=
 enumValues<T>().firstOrNull{it.name==value} ?: fallback
