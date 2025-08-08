package com.mai.oaidviewer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Html
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.mai.oaidviewer.library.CertUtils
import com.mai.oaidviewer.library.InitCallback
import com.mai.oaidviewer.library.OAIDImpl
import com.mai.oaidviewer.library.RequestPermissionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "OAIDViewer"

class MainActivity : ComponentActivity() {

    private var permissionErrCount = 0

    private var headerText by mutableStateOf("")
    private var contentText by mutableStateOf("初始化中")
    private var permissionStatusText by mutableStateOf("")
    private var showPermissionDialog by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean ->
            checkPermission()
        }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        MyTopAppBar(title = "OAID Viewer")
                    },
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            herder = headerText,
                            content = contentText,
                            permissionStatus = permissionStatusText,
                            onCopyClicked = {
                                val textToCopy = "$headerText\n$contentText"
                                val clipboardManager =
                                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "oaid",
                                        textToCopy
                                    )
                                )
                                share(this@MainActivity, textToCopy)
                            },
                            showPermissionDialog = showPermissionDialog,
                            onDialogDismiss = { showPermissionDialog = false },
                            onDialogConfirm = {
                                checkPermission()
                            }
                        )
                    }
                }
            }
        }

        init()
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun init() {
        when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.Q..Int.MAX_VALUE -> {
                initOaid()
            }

            in Build.VERSION_CODES.M until Build.VERSION_CODES.Q -> {
                checkPermission()
            }

            else -> {
                initImei()
            }
        }
    }

    private val sdf: SimpleDateFormat by lazy {
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.CHINA
        )
    }

    private fun initOaid() {
        coroutineScope.launch {
            // 设备系统与签名证书信息
            var headerText = "Arch: " + if (isArchNotSupport()) {
                "x86\n"
            } else {
                "arm\n"
            }

            OAIDImpl.instance.init(this@MainActivity, { result: InitCallback ->
                if (result is InitCallback.Failure) {
                    if (result.msg.isNotEmpty()) {
                        onText("<font color=\"#FF0000\">初始化失败 - ${result.msg} </font>")
                    }
                } else {
                    (result as InitCallback.Success).data.let { data ->
                        onText(data.toString())
                        if (data.isSupported && data.isLimited && data.isSupportRequestOAIDPermission) {
                            OAIDImpl.instance.requestOAIDPermission(
                                this@MainActivity,
                                object : RequestPermissionCallback {

                                    /**
                                     * 获取授权成功
                                     */
                                    override fun onGranted(grPermission: Array<String>?) {
                                        val permissionStr =
                                            getPermissions(grPermission?.toList())
                                        Log.i(
                                            TAG,
                                            "RequestPermissionCallback#onGranted:$permissionStr"
                                        )
                                        permissionStatusText = "获取权限'${permissionStr}'成功"
                                    }

                                    /**
                                     * 获取授权失败
                                     */
                                    override fun onDenied(dePermissions: List<String>?) {
                                        val permissionStr = getPermissions(dePermissions)
                                        Log.i(
                                            TAG,
                                            "RequestPermissionCallback#onDenied:$permissionStr"
                                        )
                                        permissionStatusText = "获取权限'${permissionStr}'失败"
                                    }

                                    /**
                                     * 禁止再次询问
                                     */
                                    override fun onAskAgain(asPermissions: List<String>?) {
                                        val permissionStr = getPermissions(asPermissions)
                                        Log.i(TAG, "onAskAgain#onDenied:$permissionStr")
                                        permissionStatusText =
                                            "禁止再次获取权限'${permissionStr}'"
                                    }
                                })
                        }
                    }
                }
            })

            // getSdkVersion 需要在 loadLibrary 后
            val (versionName, versionCode) = OAIDImpl.instance.getSdkVersion()
            headerText += "Version: $versionName ($versionCode)\n" +
                    "Time: ${sdf.format(System.currentTimeMillis())}\n" +
                    "Brand: ${Build.BRAND}\n" +
                    "Manufacturer: ${Build.MANUFACTURER}\n" +
                    "Model: ${Build.MODEL}\n" +
                    "AndroidVersion: ${Build.VERSION.RELEASE}\n\n\n" +
                    CertUtils.getCertInfo(this@MainActivity, sdf)

            this@MainActivity.headerText = headerText
        }
    }

    @SuppressLint("PrivateApi")
    private fun isArchNotSupport(): Boolean {
        return Build.SUPPORTED_ABIS.any { it.contains("x86") }
    }

    /**
     * 更新信息
     */
    private fun onText(msg: String) {
        contentText = msg
    }

    private fun getPermissions(permissions: List<String>?): String {
        return permissions?.joinToString(";") ?: ""
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initImei()
        } else {
            if (++permissionErrCount > 3) {
                showPermissionDialog = false
                if (++permissionErrCount > 3) {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            ).setData(Uri.fromParts("package", packageName, null))
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "请手动到应用详情中允许应用电话权限",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun initImei() {
        try {
            val imei = (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
            onText("imei:$imei")
        } catch (e: Exception) {
            onText(e.toString())
        }
    }

    private fun share(context: Context, value: String? = null) {
        val intent = Intent(Intent.ACTION_SEND)
        if (value != null) {
            intent.putExtra(Intent.EXTRA_TEXT, value)
            intent.type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "发送至..."))
    }
}

@Composable
fun MainScreen(
    herder: String,
    content: String,
    permissionStatus: String,
    onCopyClicked: () -> Unit,
    showPermissionDialog: Boolean,
    onDialogDismiss: () -> Unit,
    onDialogConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = herder,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            },
            update = { textView ->
                textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(content)
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCopyClicked
        ) {
            Text(text = stringResource(id = R.string.copy))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = permissionStatus,
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
    if (showPermissionDialog) {
        PermissionDialog(
            onDismissRequest = onDialogDismiss,
            onConfirm = onDialogConfirm,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "获取IMEI需要授权电话权限",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(title: String) {
    TopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}