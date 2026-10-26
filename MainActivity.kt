package com.emeng.proxxy

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeng.proxxy.overlay.CrosshairOverlayService
import com.emeng.proxxy.storage.LocalStore
import com.emeng.proxxy.storage.Preset
import kotlin.random.Random

private val BG=Color(0xFF090D14); private val PANEL=Color(0xFF151C28); private val RED=Color(0xFFFF455D); private val CYAN=Color(0xFF42E8E0)
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        MaterialTheme(colorScheme=darkColorScheme(primary=RED,secondary=CYAN,background=BG,surface=PANEL,onSurface=Color.White,onBackground=Color.White)) {
            EmengApp(
                allowed={Settings.canDrawOverlays(this)},
                enable={ if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else startService(Intent(this,CrosshairOverlayService::class.java)) },
                launchGame={ if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else { startService(Intent(this,CrosshairOverlayService::class.java).setAction(CrosshairOverlayService.ACTION_LAUNCH_GAME)); moveTaskToBack(true) } },
                disable={stopService(Intent(this,CrosshairOverlayService::class.java))},
                device={"Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n${Build.MANUFACTURER} ${Build.MODEL}\nDisplay ${resources.displayMetrics.widthPixels} × ${resources.displayMetrics.heightPixels}\nDensity ${resources.displayMetrics.densityDpi} dpi"}
            )
        }
    } }
}

@Composable private fun EmengApp(allowed:()->Boolean, enable:()->Unit, launchGame:()->Unit, disable:()->Unit, device:()->String) {
    val ctx= androidx.compose.ui.platform.LocalContext.current
    val pages=listOf("DASHBOARD","AIM","VISUAL","EXTRA","INFO DEVICE","OVERLAY API","FLOATING MENU","PRESET MANAGER","TRAINING ARENA","APP SETTINGS")
    var page by remember { mutableStateOf("DASHBOARD") }
    var size by remember { mutableFloatStateOf(LocalStore.size(ctx)) }; var opacity by remember { mutableFloatStateOf(LocalStore.opacity(ctx)) }
    var color by remember { mutableStateOf(LocalStore.color(ctx)) }; var shape by remember { mutableStateOf(LocalStore.shape(ctx)) }
    var presets by remember { mutableStateOf(LocalStore.presets(ctx)) }; var newName by remember { mutableStateOf("") }
    var hits by remember { mutableIntStateOf(0) }; var misses by remember { mutableIntStateOf(0) }; var tx by remember { mutableFloatStateOf(.5f) }; var ty by remember { mutableFloatStateOf(.5f) }
    fun saveVisual(){LocalStore.saveVisual(ctx,size,opacity,color,shape)}
    Scaffold(containerColor=BG,topBar={Column(Modifier.fillMaxWidth().background(BG).padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { Surface(color=RED,shape=RoundedCornerShape(12.dp),modifier=Modifier.size(42.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Bolt,null)}}; Spacer(Modifier.width(12.dp)); Column { Text("KAKORO INJECTOR",fontWeight=FontWeight.Black,fontSize=20.sp); Text("ANDROID GAME TOOLKIT",color=CYAN,fontSize=10.sp,letterSpacing=2.sp) } }
        Spacer(Modifier.height(12.dp)); androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(pages){p->FilterChip(selected=page==p,onClick={page=p},label={Text(p,fontSize=10.sp)})}}
    }}) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            when(page){
                "DASHBOARD" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Card(colors=CardDefaults.cardColors(containerColor=PANEL),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(20.dp)){Text("CONTROL CENTER",color=CYAN,fontSize=11.sp,letterSpacing=2.sp);Text("Tune your visual setup.",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("Crosshair studio, local presets, training, and device tools.",color=Color.LightGray);Spacer(Modifier.height(12.dp));Button(onClick={launchGame}, modifier=Modifier.fillMaxWidth()){Text("ENTER FREE FIRE • OPEN FLOATING PANEL")} ; OutlinedButton(onClick={page="VISUAL"}){Text("Open Crosshair Studio")}}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("PRESETS",presets.size.toString(),Modifier.weight(1f));Metric("TRAINING HITS",hits.toString(),Modifier.weight(1f));Metric("OVERLAY",if(allowed())"READY" else "OFF",Modifier.weight(1f))}
                    Title("MODULES"); pages.filter{it!="DASHBOARD"}.forEach { p->Card(Modifier.fillMaxWidth().clickable{page=p},colors=CardDefaults.cardColors(containerColor=PANEL)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(iconFor(p),null,tint=CYAN);Spacer(Modifier.width(12.dp));Column{Text(p,fontWeight=FontWeight.Bold);Text(desc(p),fontSize=11.sp,color=Color.LightGray)}}} }
                    Note("Honest limitation","Overlay, presets and training work locally. This app does not inject code into Free Fire, change its internal aim, or reveal enemy positions.")
                }
                "AIM" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("AIM SETTINGS");Note("Sensitivity notes","These values are saved inside KAKORO INJECTOR for reference; they are not written into Free Fire automatically.");var sens by remember{mutableFloatStateOf(50f)};Text("Practice sensitivity: ${sens.toInt()}%",color=CYAN);Slider(value=sens,onValueChange={sens=it},valueRange=1f..100f);Button(onClick={LocalStore.savePreset(ctx,Preset("Aim ${sens.toInt()}",size,opacity,color,shape));presets=LocalStore.presets(ctx)},modifier=Modifier.fillMaxWidth()){Text("Save visual preset")};Note("Target lock","Automatic player aimlock is not included. Training Arena provides a local practice target.")}
                "VISUAL" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("CROSSHAIR STUDIO");Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF05080E))){Box(Modifier.fillMaxWidth().height(180.dp),contentAlignment=Alignment.Center){Crosshair(shape,color,size,opacity,Modifier.size(150.dp));Text("LIVE PREVIEW",Modifier.align(Alignment.BottomCenter).padding(8.dp),fontSize=9.sp,color=Color.Gray)}};Text("Shape");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("PLUS","DOT","CIRCLE").forEach{FilterChip(shape==it,{shape=it;saveVisual()},{Text(it)})}};Text("Color");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("RED","CYAN","GREEN","WHITE").forEach{FilterChip(color==it,{color=it;saveVisual()},{Text(it)})}};Text("Size: ${size.toInt()} dp");Slider(size,{size=it;saveVisual()},valueRange=8f..72f);Text("Opacity: ${(opacity*100).toInt()}%");Slider(opacity,{opacity=it;saveVisual()},valueRange=.15f..1f);Button(onClick=enable,modifier=Modifier.fillMaxWidth()){Text(if(allowed())"Show system crosshair" else "Grant overlay permission")};OutlinedButton(onClick=disable,modifier=Modifier.fillMaxWidth()){Text("Disable system crosshair")}}
                "EXTRA" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("EXTRA");Note("Visual tools","Crosshair is a visual-only overlay; it does not inspect the game screen or control aim.");Note("Performance","Use Android game mode and close unused apps. This app does not alter game files.")}
                "INFO DEVICE" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("DEVICE INFO");Note("Current device",device());Note("Overlay permission",if(allowed())"Granted" else "Not granted")}
                "OVERLAY API" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("OVERLAY API");Note("Permission",if(allowed())"Display over other apps is granted." else "Grant Android permission to display the crosshair over other apps.");Button(onClick=enable,modifier=Modifier.fillMaxWidth()){Text(if(allowed())"Start crosshair overlay" else "Open overlay permission settings")};OutlinedButton(onClick=disable,modifier=Modifier.fillMaxWidth()){Text("Stop overlay service")};Text("Uses WindowManager TYPE_APPLICATION_OVERLAY on Android 8+. Android permission can be revoked at any time.",fontSize=12.sp,color=Color.LightGray)}
                "FLOATING MENU" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("FLOATING MENU");Note("Current implementation","Floating utility panel that can be dragged, minimized, closed, and used to launch Free Fire.");Button(onClick=enable,modifier=Modifier.fillMaxWidth()){Text("Start floating panel")};OutlinedButton(onClick=disable,modifier=Modifier.fillMaxWidth()){Text("Close floating crosshair")}}
                "PRESET MANAGER" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("PRESET MANAGER");OutlinedTextField(newName,{newName=it.take(32)},label={Text("Preset name")},modifier=Modifier.fillMaxWidth(),singleLine=true);Button(onClick={if(newName.trim().isNotEmpty()){LocalStore.savePreset(ctx,Preset(newName.trim(),size,opacity,color,shape));presets=LocalStore.presets(ctx);newName=""}},modifier=Modifier.fillMaxWidth()){Text("Save current preset")};if(presets.isEmpty())Note("No presets","Save a preset to see it here.");presets.forEach{preset->Card(colors=CardDefaults.cardColors(containerColor=PANEL)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(preset.name,fontWeight=FontWeight.Bold);Text("${preset.shape} • ${preset.color} • ${preset.size.toInt()}dp",fontSize=11.sp,color=Color.LightGray)};TextButton(onClick={size=preset.size;opacity=preset.opacity;color=preset.color;shape=preset.shape;saveVisual()}){Text("APPLY")};IconButton(onClick={LocalStore.deletePreset(ctx,preset.name);presets=LocalStore.presets(ctx)}){Icon(Icons.Default.Delete,null,tint=RED)}}}}}
                "TRAINING ARENA" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("TRAINING ARENA");Note("Local practice","Tap the red target for a hit. Tap empty space for a miss. Statistics only describe this practice screen.");BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF05080E),RoundedCornerShape(16.dp))){val w=maxWidth;val h=maxHeight;Box(Modifier.fillMaxSize().clickable{misses++;tx=Random.nextFloat().coerceIn(.08f,.82f);ty=Random.nextFloat().coerceIn(.08f,.82f)});Box(Modifier.offset(x=w*tx-20.dp,y=h*ty-20.dp).size(40.dp).background(RED,CircleShape).clickable{hits++;tx=Random.nextFloat().coerceIn(.08f,.82f);ty=Random.nextFloat().coerceIn(.08f,.82f)},contentAlignment=Alignment.Center){Icon(Icons.Default.GpsFixed,null,tint=Color.White)};Text("HITS $hits  •  MISSES $misses",Modifier.align(Alignment.TopStart).padding(10.dp),color=CYAN,fontSize=11.sp)};Text("Accuracy: ${if(hits+misses==0)0 else hits*100/(hits+misses)}%",color=CYAN);OutlinedButton(onClick={hits=0;misses=0},modifier=Modifier.fillMaxWidth()){Text("Reset session")}}
                "APP SETTINGS" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Title("APP SETTINGS");Note("Privacy","Preferences and presets are stored locally. No account or network permission is required.");OutlinedButton(onClick={LocalStore.reset(ctx);presets=emptyList();size=28f;opacity=.9f;color="CYAN";shape="PLUS";saveVisual()},modifier=Modifier.fillMaxWidth()){Text("Reset local settings")};Text("KAKORO INJECTOR • v1.0.0",color=Color.Gray,fontSize=11.sp)}
            }
        }
    } }
}

@Composable private fun Title(t:String){Text(t,fontSize=17.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=1.sp)}
@Composable private fun Note(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=PANEL),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(title,fontWeight=FontWeight.Bold,color=CYAN);Spacer(Modifier.height(4.dp));Text(body,color=Color.LightGray,fontSize=12.sp,lineHeight=18.sp)}}}
@Composable private fun Metric(label:String,value:String,modifier:Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=PANEL)){Column(Modifier.padding(10.dp)){Text(label,fontSize=8.sp,color=Color.LightGray);Text(value,fontSize=18.sp,color=CYAN,fontWeight=FontWeight.Bold)}}}
@Composable private fun Crosshair(shape:String,colorName:String,size:Float,opacity:Float,modifier:Modifier=Modifier){val c=when(colorName){"RED"->Color(0xFFFF354F);"GREEN"->Color(0xFF4DFF91);"WHITE"->Color.White;else->CYAN}.copy(alpha=opacity);Canvas(modifier){val x=this.size.width/2;val y=this.size.height/2;val r=size.dp.toPx()/2;val sw=(size/12).coerceIn(1.5f,5f).dp.toPx();when(shape){"DOT"->drawCircle(c,sw*1.3f,Offset(x,y));"CIRCLE"->{drawCircle(c,r,Offset(x,y),style=androidx.compose.ui.graphics.drawscope.Stroke(sw));drawLine(c,Offset(x-r-sw,y),Offset(x-r/2,y),sw,cap=StrokeCap.Round);drawLine(c,Offset(x+r/2,y),Offset(x+r+sw,y),sw,cap=StrokeCap.Round);drawLine(c,Offset(x,y-r-sw),Offset(x,y-r/2),sw,cap=StrokeCap.Round);drawLine(c,Offset(x,y+r/2),Offset(x,y+r+sw),sw,cap=StrokeCap.Round)};else->{drawLine(c,Offset(x-r,y),Offset(x-r/3,y),sw,cap=StrokeCap.Round);drawLine(c,Offset(x+r/3,y),Offset(x+r,y),sw,cap=StrokeCap.Round);drawLine(c,Offset(x,y-r),Offset(x,y-r/3),sw,cap=StrokeCap.Round);drawLine(c,Offset(x,y+r/3),Offset(x,y+r),sw,cap=StrokeCap.Round);drawCircle(c,sw*.7f,Offset(x,y))}}}}
private fun iconFor(p:String)=when(p){"AIM"->Icons.Default.GpsFixed;"VISUAL"->Icons.Default.Visibility;"INFO DEVICE"->Icons.Default.PhoneAndroid;"APP SETTINGS"->Icons.Default.Settings;"PRESET MANAGER"->Icons.Default.Tune;else->Icons.Default.Bolt}
private fun desc(p:String)=when(p){"AIM"->"Sensitivity reference";"VISUAL"->"Crosshair controls";"EXTRA"->"Utility notes";"INFO DEVICE"->"Device details";"OVERLAY API"->"Permission and service";"FLOATING MENU"->"Overlay controls";"PRESET MANAGER"->"Save and apply";"TRAINING ARENA"->"Local target practice";else->"Local preferences"}
