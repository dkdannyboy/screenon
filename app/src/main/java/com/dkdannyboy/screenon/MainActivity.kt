package com.dkdannyboy.screenon

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val Pine = Color(0xFF142D27)
private val Lime = Color(0xFFC7F69B)
private val LightScheme = lightColorScheme(
    primary = Pine, onPrimary = Color.White, primaryContainer = Lime, onPrimaryContainer = Pine,
    background = Color(0xFFF5F6F2), onBackground = Pine,
    surface = Color(0xFFFFFFFF), onSurface = Pine,
    surfaceVariant = Color(0xFFE9ECE5), onSurfaceVariant = Color(0xFF53635B),
    outline = Color(0xFF75847C), error = Color(0xFFB3261E)
)
private val DarkScheme = darkColorScheme(
    primary = Lime, onPrimary = Pine, primaryContainer = Lime, onPrimaryContainer = Pine,
    background = Color(0xFF101915), onBackground = Color(0xFFECF1E9),
    surface = Color(0xFF1D2A23), onSurface = Color(0xFFECF1E9),
    surfaceVariant = Color(0xFF2A3930), onSurfaceVariant = Color(0xFFB8C7BE),
    outline = Color(0xFF889E8F), error = Color(0xFFFFB4AB)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme) {
                ScreenOnScreen()
            }
        }
    }
}

@Composable
private fun ScreenOnScreen() {
    val context = LocalContext.current
    val state by AwakeController.state.collectAsState()
    var help by remember { mutableStateOf(false) }
    var compatibilityDialog by remember { mutableStateOf(false) }
    val guard = remember { TimeoutGuard(context) }
    var compatibility by remember { mutableStateOf(guard.enabled()) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!state.enabled) {
            val restored = guard.restore()
            if (!restored) AwakeController.publish(context, AwakeState(error = TimeoutGuard.RESTORE_ERROR))
        }
        ScreenOnWidget.refresh(context)
        Toast.makeText(context, if (Settings.System.canWrite(context)) "설정이 준비됐어요. ON을 눌러주세요." else "권한이 허용되지 않았어요. 호환 모드 설정에서 다시 확인해 주세요.", Toast.LENGTH_LONG).show()
    }
    DisposableEffect(state.enabled) {
        val window = (context as? ComponentActivity)?.window
        if (state.enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional. ON is independent. */ }
    fun switch(enabled: Boolean) {
        if (enabled && guard.needsPermission()) {
            compatibilityDialog = true
            return
        }
        AwakeController.setEnabled(context, enabled)
        if (enabled && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val prefs = context.getSharedPreferences("ui", 0)
            if (!prefs.getBoolean("notificationAsked", false)) {
                prefs.edit().putBoolean("notificationAsked", true).apply()
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Box(Modifier.safeDrawingPadding().fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Pine), contentAlignment = Alignment.Center) {
                            PowerGlyph(Lime, Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("ScreenOn", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
                    }
                    TextButton(onClick = { help = true }) { Text("사용 안내", color = c.onSurfaceVariant) }
                }
                Spacer(Modifier.height(40.dp))
                Text("필요한 동안,", fontSize = 32.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp)
                Text("화면은 그대로.", fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Spacer(Modifier.height(12.dp))
                Text("다른 앱을 써도 꺼지지 않게", fontSize = 15.sp, color = c.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                PowerControl(state.enabled) { switch(!state.enabled) }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.size(8.dp).background(if (state.enabled) c.primary else c.onSurfaceVariant, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.enabled) "화면 켜짐 유지 중" else "화면 켜짐 유지 꺼짐", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Spacer(Modifier.height(6.dp))
                ElapsedLabel(state)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surfaceVariant).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModeButton("OFF", !state.enabled, Modifier.weight(1f)) { switch(false) }
                    ModeButton("ON", state.enabled, Modifier.weight(1f)) { switch(true) }
                }
                TextButton(onClick = { compatibilityDialog = true }, enabled = !state.enabled) {
                    Text(if (compatibility) "호환 모드 켜짐 · 설정" else "화면이 꺼진다면 · 호환 모드 설정")
                }
                state.error?.let {
                    Text(it, color = c.error, modifier = Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Assertive })
                }
                Spacer(Modifier.height(24.dp))
                Surface(shape = RoundedCornerShape(24.dp), color = c.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("홈 화면에서 더 간편하게", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("위젯으로 상태를 보고, 바로 켜고 끄세요.", color = c.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            if (manager.isRequestPinAppWidgetSupported) {
                                if (!manager.requestPinAppWidget(ComponentName(context, ScreenOnWidget::class.java), null, null)) {
                                    Toast.makeText(context, "홈 화면을 길게 눌러 위젯에서 ScreenOn을 추가해 주세요.", Toast.LENGTH_LONG).show()
                                }
                            } else Toast.makeText(context, "홈 화면을 길게 눌러 위젯에서 ScreenOn을 추가해 주세요.", Toast.LENGTH_LONG).show()
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                            Text("+  홈 화면에 위젯 추가", fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("광고 없이 · 떠다니는 버튼 없이", fontSize = 12.sp, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("전원 버튼으로 잠그면 자동으로 OFF 됩니다.", fontSize = 12.sp, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (compatibilityDialog) AlertDialog(
        onDismissRequest = { compatibilityDialog = false },
        title = { Text("갤럭시 화면 유지 호환 모드") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ON인데도 화면이 꺼지는 기기에서 사용하세요. 화면 유지 잠금과 함께 자동 꺼짐 시간을 늘려 보호합니다. 다른 앱 위에는 아무것도 표시하지 않습니다.")
            Text("최초 한 번 '시스템 설정 변경 허용'을 켜주세요. 돌아온 뒤 ON을 누르면 적용되며, OFF 또는 전원 버튼으로 잠그면 원래 시간으로 복원합니다.")
            Text("호환 모드 사용 중 앱을 강제 종료하거나 삭제하면 긴 화면 꺼짐 시간이 남을 수 있습니다. 먼저 OFF를 누르세요. 강제 종료한 경우 앱을 다시 열면 복원을 시도합니다.")
            Text("기기 절전·관리 정책에 따라 제한될 수 있습니다. 허용 여부만으로 이 기기에서의 유지가 검증되는 것은 아닙니다.")
        } },
        confirmButton = { TextButton(onClick = {
            guard.setEnabled(true)
            compatibility = true
            compatibilityDialog = false
            if (!Settings.System.canWrite(context)) {
                try {
                    settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                } catch (_: RuntimeException) {
                    Toast.makeText(context, "설정 앱의 특별한 접근 → 시스템 설정 변경에서 ScreenOn을 허용해 주세요.", Toast.LENGTH_LONG).show()
                }
            } else {
                guard.restore()
                ScreenOnWidget.refresh(context)
            }
        }) { Text("호환 모드 설정") } },
        dismissButton = { TextButton(onClick = {
            if (guard.restore()) {
                guard.setEnabled(false)
                compatibility = false
                compatibilityDialog = false
                ScreenOnWidget.refresh(context)
            } else AwakeController.publish(context, AwakeState(error = TimeoutGuard.RESTORE_ERROR))
        }) { Text("기본 모드 사용") } }
    )
    if (help) AlertDialog(
        onDismissRequest = { help = false }, title = { Text("화면 켜짐, 필요한 만큼만") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ON을 누르면 다른 앱을 사용해도 화면이 켜진 상태로 유지됩니다. OFF를 누르면 원래 화면 꺼짐 시간이 다시 적용됩니다.")
            Text("다른 앱 위에는 아무것도 표시하지 않습니다. Android의 실행 알림은 알림 영역에 조용히 표시되며, 알림에서도 끌 수 있습니다.")
            Text("홈 화면 위젯은 ON/OFF 부분을 누르면 전환되고, ScreenOn 이름을 누르면 앱이 열립니다.")
            Text("전원 버튼으로 잠그거나 기기를 재시작하면 OFF 됩니다. 오래 켜두면 배터리가 더 소모됩니다.")
            Text("제조사 절전 기능이나 회사 기기 정책에 따라 실행이 중단될 수 있습니다. 계정, 인터넷, 접근성 또는 오버레이 권한은 사용하지 않습니다.")
        } }, confirmButton = { TextButton(onClick = { help = false }) { Text("확인") } }
    )
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Button(onClick = onClick, modifier = modifier.heightIn(min = 54.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) c.primary else Color.Transparent, contentColor = if (selected) c.onPrimary else c.onSurfaceVariant),
        elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun PowerControl(enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (enabled) Lime else c.surfaceVariant, tween(180), label = "power")
    Box(Modifier.size(204.dp).border(1.dp, c.outline.copy(alpha = .35f), CircleShape).padding(12.dp), contentAlignment = Alignment.Center) {
        Button(onClick = onClick, shape = CircleShape, modifier = Modifier.fillMaxSize().semantics {
            contentDescription = "화면 켜짐 유지"
            role = Role.Switch
            toggleableState = if (enabled) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off
            stateDescription = if (enabled) "켜짐" else "꺼짐"
        }, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = Pine), elevation = ButtonDefaults.buttonElevation(0.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerGlyph(if (enabled) Pine else c.onSurfaceVariant, Modifier.size(54.dp))
                Spacer(Modifier.height(14.dp))
                Text(if (enabled) "ON" else "OFF", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = if (enabled) Pine else c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PowerGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.width * .085f
        drawArc(color, -48f, 276f, false, Offset(size.width * .16f, size.height * .16f), Size(size.width * .68f, size.height * .68f), style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(color, Offset(size.width / 2, size.height * .07f), Offset(size.width / 2, size.height * .47f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ElapsedLabel(state: AwakeState) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.enabled, state.startedAt) {
        while (state.enabled) { now = SystemClock.elapsedRealtime(); delay(1000) }
    }
    val seconds = ((now - state.startedAt) / 1000).coerceAtLeast(0)
    Text(if (state.enabled) "${String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)} 동안 유지 중" else "평소 화면 꺼짐 설정이 적용돼요",
        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
