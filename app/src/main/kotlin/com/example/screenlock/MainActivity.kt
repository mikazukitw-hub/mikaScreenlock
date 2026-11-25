package com.example.screenlock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.screenlock.ui.theme.ScreenLockTheme
import com.example.screenlock.ui.theme.SoftBackground
import com.example.screenlock.ui.theme.SoftBorder
import com.example.screenlock.ui.theme.SoftSageContainer
import com.example.screenlock.ui.theme.SoftSageOnContainer
import com.example.screenlock.ui.theme.SoftSageOnPrimary
import com.example.screenlock.ui.theme.SoftSagePrimary
import com.example.screenlock.ui.theme.SoftStatusActiveBg
import com.example.screenlock.ui.theme.SoftStatusActiveDot
import com.example.screenlock.ui.theme.SoftStatusActiveText
import com.example.screenlock.ui.theme.SoftStatusIdleBg
import com.example.screenlock.ui.theme.SoftStatusIdleDot
import com.example.screenlock.ui.theme.SoftStatusIdleText
import com.example.screenlock.ui.theme.SoftSurface
import com.example.screenlock.ui.theme.SoftSurfaceVariant
import com.example.screenlock.ui.theme.SoftTextMuted
import com.example.screenlock.ui.theme.SoftTextPrimary
import com.example.screenlock.ui.theme.SoftTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)
    private var isTransparentMode by mutableStateOf(false)
    private var versionInfoText by mutableStateOf("")

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkPermissions()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )

        updateVersionInfo()

        setContent {
            ScreenLockTheme {
                ScreenLockScreen(
                    isServiceRunning = isServiceRunning,
                    isTransparentMode = isTransparentMode,
                    versionInfoText = versionInfoText,
                    onModeSelected = { transparent ->
                        isTransparentMode = transparent
                        syncModeToService()
                    },
                    onStartClick = { checkPermissions() },
                    onLockNowClick = { testLockNow() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateVersionInfo()
        checkServiceStatus()
        syncModeToService()
    }

    private fun updateVersionInfo() {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        versionInfoText = "最新版本: v17 (新增通知列快捷開關)\n部署時間: $currentTime"
    }

    private fun syncModeToService() {
        val intent = Intent(this, LockAccessibilityService::class.java).apply {
            action = LockAccessibilityService.ACTION_SET_MODE
            putExtra(LockAccessibilityService.EXTRA_IS_TRANSPARENT, isTransparentMode)
        }
        startService(intent)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            Toast.makeText(this, "需要開啟「懸浮窗權限」", Toast.LENGTH_SHORT).show()
            overlayPermissionLauncher.launch(intent)
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "請開啟「ScreenLock」輔助功能", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        startLockService()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${LockAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun checkServiceStatus() {
        isServiceRunning = isAccessibilityServiceEnabled()
    }

    private fun startLockService() {
        val intent = Intent(this, LockService::class.java)
        startForegroundService(intent)
        syncModeToService()
    }

    private fun testLockNow() {
        syncModeToService()
        val intent = Intent(this, LockAccessibilityService::class.java).apply {
            action = LockService.ACTION_LOCK_NOW
        }
        startService(intent)
    }
}

@Composable
fun ScreenLockScreen(
    isServiceRunning: Boolean,
    isTransparentMode: Boolean,
    versionInfoText: String,
    onModeSelected: (Boolean) -> Unit,
    onStartClick: () -> Unit,
    onLockNowClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Icon Badge
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "ScreenLock Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = SoftSagePrimary.copy(alpha = 0.25f),
                        spotColor = SoftSagePrimary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = "ScreenLock",
                style = MaterialTheme.typography.headlineMedium,
                color = SoftTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Service Status Pill
            StatusPill(isRunning = isServiceRunning)

            Spacer(modifier = Modifier.height(24.dp))

            // Usage Guide Card
            SoftCard(title = "操作方式") {
                ShortcutRow(
                    badgeText = "音量下 × 2",
                    description = "快速鎖定螢幕"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = SoftBorder.copy(alpha = 0.7f)
                )
                ShortcutRow(
                    badgeText = "音量上 × 2",
                    description = "快速解除鎖定"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector Card
            SoftCard(title = "鎖定模式") {
                ModeOptionRow(
                    title = "模式 1：全透明屏蔽",
                    subtitle = "不影響螢幕亮度，維持原畫面顯示",
                    selected = isTransparentMode,
                    onClick = { onModeSelected(true) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ModeOptionRow(
                    title = "模式 2：深色覆蓋",
                    subtitle = "目前預設功能，柔和遮暗防誤觸",
                    selected = !isTransparentMode,
                    onClick = { onModeSelected(false) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Start Service Button
            Button(
                onClick = onStartClick,
                enabled = !isServiceRunning,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftSagePrimary,
                    contentColor = SoftSageOnPrimary,
                    disabledContainerColor = SoftSurfaceVariant,
                    disabledContentColor = SoftSageOnContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (isServiceRunning) "服務執行中 (v17)" else "點擊啟動終極屏蔽服務",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Test Lock Button
            Button(
                onClick = onLockNowClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftSageContainer,
                    contentColor = SoftSageOnContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "🔒 立即鎖定",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(24.dp))

            // Version Info Footer
            Text(
                text = versionInfoText,
                style = MaterialTheme.typography.bodySmall,
                color = SoftTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusPill(isRunning: Boolean) {
    val bgColor = if (isRunning) SoftStatusActiveBg else SoftStatusIdleBg
    val dotColor = if (isRunning) SoftStatusActiveDot else SoftStatusIdleDot
    val textColor = if (isRunning) SoftStatusActiveText else SoftStatusIdleText
    val label = if (isRunning) "輔助屏蔽服務執行中" else "尚未啟動輔助服務"

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
        }
    }
}

@Composable
private fun SoftCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SoftSurface,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = SoftTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ShortcutRow(
    badgeText: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SoftSurfaceVariant
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = SoftSageOnContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = SoftTextPrimary
        )
    }
}

@Composable
private fun ModeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) SoftSurfaceVariant.copy(alpha = 0.7f) else SoftBackground.copy(alpha = 0.6f)
    val borderColor = if (selected) SoftSagePrimary.copy(alpha = 0.55f) else SoftBorder.copy(alpha = 0.6f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = SoftSagePrimary,
                    unselectedColor = SoftTextMuted
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftTextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ScreenLockScreenPreview() {
    ScreenLockTheme {
        ScreenLockScreen(
            isServiceRunning = false,
            isTransparentMode = false,
            versionInfoText = "最新版本: v17 (新增通知列快捷開關)\n部署時間: 2026-11-25 19:30:00",
            onModeSelected = {},
            onStartClick = {},
            onLockNowClick = {}
        )
    }
}
