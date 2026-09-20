package com.albionsolomarket.app
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
import androidx.compose.ui.unit.dp
import com.albionsolomarket.app.domain.MarketEngine

enum class AppTheme { AUTO,LIGHT,DARK }
enum class Mode(val label:String){ TRADE("Cidade → Cidade"),LOCAL("Flip Local"),BLACK("Caerleon / Black Market") }
class MainActivity:ComponentActivity(){ override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}} }
@Composable fun App(){
 var theme by remember{mutableStateOf(AppTheme.AUTO)}; var capital by remember{mutableStateOf("500000")}; var mins by remember{mutableStateOf("45")}; var mode by remember{mutableStateOf(Mode.TRADE)}
 val dark=when(theme){AppTheme.AUTO->isSystemInDarkTheme();AppTheme.DARK->true;else->false}
 MaterialTheme(colorScheme=if(dark) darkColorScheme() else lightColorScheme()){Surface(Modifier.fillMaxSize()){BoxWithConstraints{val wide=maxWidth>700.dp; Column(Modifier.padding(if(wide)32.dp else 18.dp).widthIn(max=900.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Albion Solo Market",style=MaterialTheme.typography.headlineMedium); Text("Americas • Beta 0.1")
  Text("Qual é a melhor maneira de tentar ganhar prata agora?",style=MaterialTheme.typography.titleMedium)
  if(wide) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Input(capital,{capital=it},"Capital",Modifier.weight(1f));Input(mins,{mins=it},"Minutos",Modifier.weight(1f))} else {Input(capital,{capital=it},"Capital");Input(mins,{mins=it},"Minutos")}
  Text("Modo"); Mode.entries.forEach{FilterChip(selected=mode==it,onClick={mode=it},label={Text(it.label)})}
  HorizontalDivider(); Text("Oportunidades",style=MaterialTheme.typography.titleLarge)
  Text("SEM DADOS",style=MaterialTheme.typography.titleMedium); Text("Nenhum preço confiável foi carregado. Cada preço exibirá fonte, horário e confiança.")
  Text(MarketEngine.nextBestAction(emptyList()))
  HorizontalDivider(); Text("Aparência"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AppTheme.entries.forEach{t->FilterChip(theme==t,{theme=t},{Text(when(t){AppTheme.AUTO->"Auto";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"})})}}
  Text("Lucro líquido • ROI • prata/hora • quantidade pelo capital • origem/destino • idade do preço • confiança",style=MaterialTheme.typography.bodySmall)
 }}}}
}
@Composable fun Input(v:String,on:(String)->Unit,label:String,m:Modifier=Modifier.fillMaxWidth())=OutlinedTextField(v,{on(it.filter(Char::isDigit))},label={Text(label)},modifier=m)
