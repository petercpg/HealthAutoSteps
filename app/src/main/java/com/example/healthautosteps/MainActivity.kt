package com.example.healthautosteps

import android.Manifest
import android.app.KeyguardManager
import android.os.Bundle
import android.util.Log
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var settingsManager: SettingsManager

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d("HealthAutoSteps", "Permissions granted result: $granted")
        if (granted.containsAll(healthConnectManager.permissions)) {
            Toast.makeText(this, "Health Connect 權限已授予！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "部分權限未授予 (${granted.size}/${healthConnectManager.permissions.size})", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "通知權限已授予！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "通知權限被拒絕，您將無法收到執行結果通知", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectManager = HealthConnectManager(this)
        settingsManager = SettingsManager(this)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        Log.d("HealthAutoSteps", "onCreate SDK Status: ${androidx.health.connect.client.HealthConnectClient.getSdkStatus(this)}")
        Log.d("HealthAutoSteps", "Device Secure (Screen Lock): ${km.isDeviceSecure}")

        checkNotificationPermission()

        setContent {
            HealthAutoStepsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onCheckPermissions = { checkAndRequestPermissions() },
                        onScheduleWorker = { scheduleStepWorker() },
                        onManualWrite = { steps -> manualWriteSteps(steps) },
                        healthConnectManager = healthConnectManager,
                        settings = settingsManager
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkAndRequestPermissions() {
        lifecycleScope.launch {
            val sdkStatus = androidx.health.connect.client.HealthConnectClient.getSdkStatus(this@MainActivity)
            Log.d("HealthAutoSteps", "SDK Status: $sdkStatus")
            if (sdkStatus == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE) {
                Log.e("HealthAutoSteps", "SDK_UNAVAILABLE")
                Toast.makeText(this@MainActivity, "系統不支援或是沒有安裝 Health Connect", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (sdkStatus == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                Log.e("HealthAutoSteps", "SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
                Toast.makeText(this@MainActivity, "Health Connect 需要更新", Toast.LENGTH_LONG).show()
                return@launch
            }

            val grantedPermissions = healthConnectManager.hasAllPermissions()
            Log.d("HealthAutoSteps", "Current hasAllPermissions: $grantedPermissions")

            val currentGranted = androidx.health.connect.client.HealthConnectClient.getOrCreate(this@MainActivity)
                .permissionController.getGrantedPermissions()
            Log.d("HealthAutoSteps", "Actual granted: $currentGranted")

            if (grantedPermissions) {
                Log.d("HealthAutoSteps", "Already has all permissions")
                Toast.makeText(this@MainActivity, "已有權限", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("HealthAutoSteps", "Requesting permissions set: ${healthConnectManager.permissions}")
                Toast.makeText(this@MainActivity, "請求權限中...", Toast.LENGTH_SHORT).show()
                try {
                    requestPermissionLauncher.launch(healthConnectManager.permissions)
                    Log.d("HealthAutoSteps", "Launcher.launch() called successfully")
                } catch (e: Exception) {
                    Log.e("HealthAutoSteps", "Error launching permissions", e)
                    Toast.makeText(this@MainActivity, "啟動權限請求失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun scheduleStepWorker() {
        WorkScheduler.scheduleNextWork(this)
        Toast.makeText(this, "每日自動同步已設定於 ${settingsManager.syncTime}", Toast.LENGTH_SHORT).show()
    }

    private fun manualWriteSteps(steps: Long) {
        lifecycleScope.launch {
            if (!healthConnectManager.hasAllPermissions()) {
                Toast.makeText(this@MainActivity, "請先授予權限", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val now = java.time.Instant.now()
                healthConnectManager.writeSteps(steps, now.minusSeconds(1800), now)
                Toast.makeText(this@MainActivity, "成功寫入 $steps 步", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("HealthAutoSteps", "Manual write failed", e)
                Toast.makeText(this@MainActivity, "寫入失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainScreen(
    onCheckPermissions: () -> Unit,
    onScheduleWorker: () -> Unit,
    onManualWrite: (Long) -> Unit,
    healthConnectManager: HealthConnectManager,
    settings: SettingsManager
) {
    var minSteps by remember { mutableStateOf(settings.minSteps.toString()) }
    var maxSteps by remember { mutableStateOf(settings.maxSteps.toString()) }
    var startTime by remember { mutableStateOf(settings.startTime.toString()) }
    var endTime by remember { mutableStateOf(settings.endTime.toString()) }
    var syncTime by remember { mutableStateOf(settings.syncTime.toString()) }
    var manualSteps by remember { mutableStateOf("1000") }
    var records by remember { mutableStateOf<List<androidx.health.connect.client.records.StepsRecord>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (healthConnectManager.hasAllPermissions()) {
            try {
                records = healthConnectManager.readRecentRecords(5)
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to load records", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Handles top camera cutout
            .displayCutoutPadding() // Specifically handles cutouts
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Health Auto Steps", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCheckPermissions) {
            Text("檢測 / 授予權限")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("同步設定", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it; runCatching { settings.startTime = LocalTime.parse(it) } },
            label = { Text("開始時間 (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it; runCatching { settings.endTime = LocalTime.parse(it) } },
            label = { Text("結束時間 (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = minSteps,
            onValueChange = { minSteps = it; runCatching { settings.minSteps = it.toInt() } },
            label = { Text("最少步數") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maxSteps,
            onValueChange = { maxSteps = it; runCatching { settings.maxSteps = it.toInt() } },
            label = { Text("最多步數") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = syncTime,
            onValueChange = { syncTime = it; runCatching { settings.syncTime = LocalTime.parse(it) } },
            label = { Text("每日同步時間 (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onScheduleWorker, modifier = Modifier.fillMaxWidth()) {
            Text("啟動 / 更新每日同步排程")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("手動寫入測試", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = manualSteps,
                onValueChange = { manualSteps = it },
                label = { Text("步數") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onManualWrite(manualSteps.toLongOrNull() ?: 1000L) }) {
                Text("立即寫入")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("最近分段寫入紀錄 (共 ${records.size} 筆)", style = MaterialTheme.typography.titleMedium)
        Text("每一筆代表固定時段內的分配步數", style = MaterialTheme.typography.bodySmall)
        records.forEach { record ->
            val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            val start = record.startTime.atZone(ZoneId.systemDefault()).format(formatter)
            Text("${record.count} 步 ($start)")
        }

        Button(onClick = {
            coroutineScope.launch {
                if (healthConnectManager.hasAllPermissions()) {
                    records = healthConnectManager.readRecentRecords(5)
                }
            }
        }) {
            Text("重新整理紀錄")
        }
    }
}

@Composable
fun HealthAutoStepsTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
