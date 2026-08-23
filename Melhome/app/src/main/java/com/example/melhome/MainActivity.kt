package com.example.melhome

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.melhome.ui.theme.MelhomeTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

// ---------------------------------------------------------
// SERVEUR HTTP LOCAL (POUR INTÉGRATION WEBHOOKS)
// ---------------------------------------------------------
class LocalHttpServer(
    private val port: Int = 8080,
    private val onCommandReceived: suspend (Map<String, String>) -> String
) {
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val requestLine = reader.readLine()
            if (requestLine != null && requestLine.startsWith("GET")) {
                val parts = requestLine.split(" ")
                if (parts.size > 1) {
                    val pathParts = parts[1].split("?")
                    val queryParams = mutableMapOf<String, String>()
                    if (pathParts.size > 1) {
                        pathParts[1].split("&").forEach { param ->
                            val kv = param.split("=")
                            if (kv.size == 2) {
                                queryParams[kv[0]] = URLDecoder.decode(kv[1], "UTF-8")
                            }
                        }
                    }
                    val responseBody = onCommandReceived(queryParams)
                    val httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${responseBody.toByteArray().size}\r\n\r\n$responseBody"
                    clientSocket.getOutputStream().write(httpResponse.toByteArray())
                }
            }
            clientSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverJob?.cancel()
    }
}

// ---------------------------------------------------------
// GESTIONNAIRE DE TRADUCTION (FR / EN / ES)
// ---------------------------------------------------------
object Localization {
    private val strings = mapOf(
        "fr" to mapOf(
            "status" to "Statut",
            "logout" to "Déconnexion",
            "settings" to "Paramètres",
            "more_settings" to "Plus de réglages...",
            "power" to "Alimentation",
            "off" to "Éteint",
            "room_temp" to "T°",
            "target_temp" to "Consigne",
            "mode" to "Mode de fonctionnement",
            "fan" to "Vitesse de Ventilation",
            "vane" to "Réglage Ailettes (Orientation)...",
            "close" to "Fermer",
            "language" to "Langue",
            "back" to "Retour",
            "auto" to "Automatique",
            "swing" to "Balayage (Swing)",
            "device_order" to "Ordre des appareils",
            "card_colors" to "Couleur des cartes",
            "server_config" to "URL du Serveur Render",
            "save" to "Enregistrer"
        ),
        "en" to mapOf(
            "status" to "Status",
            "logout" to "Logout",
            "settings" to "Settings",
            "more_settings" to "More settings...",
            "power" to "Power",
            "off" to "Off",
            "room_temp" to "T°",
            "target_temp" to "Target",
            "mode" to "Operation Mode",
            "fan" to "Fan Speed",
            "vane" to "Vanes (Orientation)...",
            "close" to "Close",
            "language" to "Language",
            "back" to "Back",
            "auto" to "Automatic",
            "swing" to "Swing",
            "device_order" to "Device Order",
            "card_colors" to "Card Colors",
            "server_config" to "Render Server URL",
            "save" to "Save"
        ),
        "es" to mapOf(
            "status" to "Estado",
            "logout" to "Cerrar sesión",
            "settings" to "Ajustes",
            "more_settings" to "Más ajustes...",
            "power" to "Alimentación",
            "off" to "Apagado",
            "room_temp" to "T°",
            "target_temp" to "Consigna",
            "mode" to "Modo de operación",
            "fan" to "Velocidad de ventilación",
            "vane" to "Ajuste de lamas...",
            "close" to "Cerrar",
            "language" to "Idioma",
            "back" to "Volver",
            "auto" to "Automático",
            "swing" to "Oscilación (Swing)",
            "device_order" to "Orden de dispositivos",
            "card_colors" to "Colores de tarjetas",
            "server_config" to "URL del servidor Render",
            "save" to "Guardar"
        )
    )

    fun get(lang: String, key: String): String {
        return strings[lang]?.get(key) ?: strings["fr"]?.get(key) ?: key
    }
}

val cardColorPresets = mapOf(
    "default" to Color.Unspecified,
    "blue" to Color(0xFFE3F2FD),
    "cyan" to Color(0xFFB2EBF2),
    "teal" to Color(0xFFB2DFDB),
    "green" to Color(0xFFE8F5E9),
    "yellow" to Color(0xFFFFFDE7),
    "amber" to Color(0xFFFFECB3),
    "orange" to Color(0xFFFFE0B2),
    "pink" to Color(0xFFFFEBEE),
    "purple" to Color(0xFFEDE7F6),
    "gray" to Color(0xFFF5F5F5)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        setContent {
            MelhomeTheme {
                var devices = remember { mutableStateListOf<Map<String, Any>>() }
                var loading by remember { mutableStateOf(true) }
                var statusMessage by remember { mutableStateOf("Prêt") }
                var currentLang by remember { mutableStateOf(prefs.getString("app_lang", "fr") ?: "fr") }
                var showSettingsDialog by remember { mutableStateOf(false) }

                var cardColors by remember {
                    mutableStateOf(
                        try {
                            val json = prefs.getString("card_colors", "{}") ?: "{}"
                            Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type) as Map<String, String>
                        } catch (e: Exception) { emptyMap() }
                    )
                }

                val cookie = remember { TokenManager.getToken(this@MainActivity) ?: "" }
                val scope = rememberCoroutineScope()

                // Initialisation du serveur local HTTP
                val localServer = remember {
                    LocalHttpServer(port = 8080) { params ->
                        val targetId = params["id"]
                        val powerStr = params["power"]
                        val tempStr = params["temp"]
                        val mode = params["mode"]
                        val fanStr = params["fan"]

                        val targetDevice = devices.find {
                            (it["id"]?.toString() ?: it["ID"]?.toString()) == targetId
                        } ?: devices.firstOrNull()

                        if (targetDevice != null) {
                            val devId = targetDevice["id"]?.toString() ?: targetDevice["ID"]?.toString() ?: ""
                            val power = powerStr?.lowercase()?.toBoolean()
                            val temp = tempStr?.toFloatOrNull()
                            val fan = fanStr?.toIntOrNull()

                            statusMessage = "✓ CMD"
                            sendAtaunitCommand(
                                cookie = cookie,
                                device = targetDevice,
                                deviceId = devId,
                                power = power,
                                temp = temp,
                                mode = mode,
                                fanSpeed = fan,
                                onStatusUpdate = { statusMessage = it }
                            )
                            "OK: Commande exécutée"
                        } else {
                            "Erreur: Appareil introuvable"
                        }
                    }
                }

                DisposableEffect(Unit) {
                    localServer.start()
                    onDispose {
                        localServer.stop()
                    }
                }

                fun logoutAndRedirect() {
                    TokenManager.saveToken(this@MainActivity, "")

                    val savedLang = prefs.getString("app_lang", "fr")
                    val savedOrder = prefs.getString("device_order", "")
                    val savedColors = prefs.getString("card_colors", "{}")
                    val savedServerUrl = prefs.getString("render_server_url", "https://melhome-bridge.onrender.com")

                    prefs.edit().clear()
                        .putString("app_lang", savedLang)
                        .putString("device_order", savedOrder)
                        .putString("card_colors", savedColors)
                        .putString("render_server_url", savedServerUrl)
                        .commit()

                    android.webkit.CookieManager.getInstance().removeAllCookies {
                        android.webkit.CookieManager.getInstance().flush()
                        runOnUiThread {
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }

                fun saveOrder(currentDevices: List<Map<String, Any>>) {
                    val ids = currentDevices.map { it["id"]?.toString() ?: it["ID"]?.toString() ?: "" }
                    prefs.edit().putString("device_order", ids.joinToString(",")).apply()
                }

                fun saveCardColor(deviceId: String, colorKey: String) {
                    val mutableMap = cardColors.toMutableMap()
                    mutableMap[deviceId] = colorKey
                    cardColors = mutableMap
                    prefs.edit().putString("card_colors", Gson().toJson(mutableMap)).apply()
                }

                LaunchedEffect(Unit) {
                    if (cookie.isEmpty()) {
                        logoutAndRedirect()
                    } else {
                        val response = fetchJson(cookie)

                        if (response.first == 401) {
                            logoutAndRedirect()
                        } else if (response.first == 500 || response.first != 200) {
                            statusMessage = "Erreur serveur (${response.first}), mode hors-ligne"
                            loading = false
                        } else {
                            if (response.second.trim().startsWith("<!DOCTYPE", ignoreCase = true) || response.second.trim().startsWith("<html", ignoreCase = true)) {
                                logoutAndRedirect()
                            } else {
                                val parsed = parseDevices(response.second)
                                val savedOrderStr = prefs.getString("device_order", "") ?: ""

                                devices.clear()
                                if (savedOrderStr.isNotEmpty()) {
                                    val savedIds = savedOrderStr.split(",")
                                    val deviceMap = parsed.associateBy { it["id"]?.toString() ?: it["ID"]?.toString() ?: "" }
                                    for (id in savedIds) {
                                        deviceMap[id]?.let { devices.add(it) }
                                    }
                                    for (d in parsed) {
                                        val id = d["id"]?.toString() ?: d["ID"]?.toString() ?: ""
                                        if (!devices.any { (it["id"]?.toString() ?: it["ID"]?.toString() ?: "") == id }) {
                                            devices.add(d)
                                        }
                                    }
                                } else {
                                    devices.addAll(parsed)
                                }

                                statusMessage = if (devices.isEmpty()) "0" else "${devices.size}"
                                loading = false
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Melhome",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("⚙️", fontSize = 18.sp)
                                }

                                TextButton(
                                    onClick = { logoutAndRedirect() },
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(Localization.get(currentLang, "logout"), fontSize = 12.sp)
                                }
                            }
                        }

                        if (loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                items(
                                    items = devices,
                                    key = { device -> device["id"]?.toString() ?: device.hashCode() }
                                ) { device ->
                                    val deviceId = device["id"]?.toString() ?: device["ID"]?.toString() ?: ""
                                    val colorKey = cardColors[deviceId] ?: "default"
                                    val customColor = cardColorPresets[colorKey] ?: Color.Unspecified

                                    DeviceCard(
                                        device = device,
                                        cookie = cookie,
                                        currentLang = currentLang,
                                        cardBackgroundColor = customColor,
                                        onStatusUpdate = { statusMessage = it }
                                    ) {
                                        scope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            val response = fetchJson(cookie)
                                            if (response.first == 200) {
                                                val parsed = parseDevices(response.second)
                                                val savedOrderStr = prefs.getString("device_order", "") ?: ""
                                                devices.clear()
                                                if (savedOrderStr.isNotEmpty()) {
                                                    val savedIds = savedOrderStr.split(",")
                                                    val deviceMap = parsed.associateBy { it["id"]?.toString() ?: it["ID"]?.toString() ?: "" }
                                                    for (id in savedIds) {
                                                        deviceMap[id]?.let { devices.add(it) }
                                                    }
                                                    for (d in parsed) {
                                                        val id = d["id"]?.toString() ?: d["ID"]?.toString() ?: ""
                                                        if (!devices.any { (it["id"]?.toString() ?: it["ID"]?.toString() ?: "") == id }) {
                                                            devices.add(d)
                                                        }
                                                    }
                                                } else {
                                                    devices.addAll(parsed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        currentLang = currentLang,
                        devices = devices,
                        cardColors = cardColors,
                        cookie = cookie,
                        prefs = prefs,
                        onLanguageSelected = { newLang ->
                            currentLang = newLang
                            prefs.edit().putString("app_lang", newLang).apply()
                        },
                        onColorSelected = { deviceId, colorKey ->
                            saveCardColor(deviceId, colorKey)
                        },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val item = devices.removeAt(index)
                                devices.add(index - 1, item)
                                saveOrder(devices)
                            }
                        },
                        onMoveDown = { index ->
                            if (index < devices.size - 1) {
                                val item = devices.removeAt(index)
                                devices.add(index + 1, item)
                                saveOrder(devices)
                            }
                        },
                        onDismiss = { showSettingsDialog = false }
                    )
                }
            }
        }
    }

    private suspend fun fetchJson(cookie: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
        val safeCookie = cookie.trim().replace("\n", "").replace("\r", "")

        val rawXsrf = "XSRF-TOKEN=([^;]+)".toRegex(RegexOption.IGNORE_CASE).find(safeCookie)?.groupValues?.get(1) ?: "1"
        val decodedXsrf = try { URLDecoder.decode(rawXsrf, "UTF-8") } catch (e: Exception) { rawXsrf }

        val request = Request.Builder()
            .url("https://melcloudhome.com/api/user/context")
            .addHeader("Cookie", safeCookie)
            .addHeader("X-XSRF-TOKEN", decodedXsrf)
            .addHeader("X-Csrf", "1")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Accept", "application/json, text/plain, */*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Pair(response.code, response.body?.string() ?: "")
            }
        } catch (e: Exception) {
            Pair(500, "")
        }
    }

    private fun parseDevices(json: String): List<Map<String, Any>> {
        return try {
            val map: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
            val buildings = map["buildings"] as? List<*>
            (buildings?.firstOrNull() as? Map<*, *>)?.get("airToAirUnits") as? List<Map<String, Any>> ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}

// ---------------------------------------------------------
// DIALOGUE DES PARAMÈTRES
// ---------------------------------------------------------
@Composable
fun SettingsDialog(
    currentLang: String,
    devices: List<Map<String, Any>>,
    cardColors: Map<String, String>,
    cookie: String,
    prefs: SharedPreferences,
    onLanguageSelected: (String) -> Unit,
    onColorSelected: (String, String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var isLoadingCode by remember { mutableStateOf(false) }

    var serverUrlInput by remember {
        mutableStateOf(prefs.getString("render_server_url", "https://melhome-bridge.onrender.com") ?: "https://melhome-bridge.onrender.com")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = Localization.get(currentLang, "settings"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Configuration de l'URL Render personnelle
                Text(text = Localization.get(currentLang, "server_config"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("URL Render") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        prefs.edit().putString("render_server_url", serverUrlInput.trim()).apply()
                        Toast.makeText(context, "URL enregistrée !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Localization.get(currentLang, "save"))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Assistant Vocal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isLoadingCode = true
                        scope.launch {
                            val code = getGoogleHomePairingCode(context, cookie)
                            pairingCode = code ?: "Erreur"
                            isLoadingCode = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    if (isLoadingCode) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Associer à Google Home")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = Localization.get(currentLang, "language"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                val languages = listOf("fr" to "Français 🇫🇷", "en" to "English 🇬🇧", "es" to "Español 🇪🇸")
                languages.forEach { (code, label) ->
                    val isSelected = currentLang == code
                    Button(
                        onClick = { onLanguageSelected(code) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(label, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = Localization.get(currentLang, "device_order"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                devices.forEachIndexed { index, device ->
                    val deviceId = device["id"]?.toString() ?: device["ID"]?.toString() ?: ""
                    val devName = (device["givenDisplayName"] ?: device["GivenDisplayName"])?.toString() ?: "Appareil"
                    val currentColorKey = cardColors[deviceId] ?: "default"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${index + 1}. $devName", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row {
                                Button(
                                    onClick = { onMoveUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("⬆️", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { onMoveDown(index) },
                                    enabled = index < devices.size - 1,
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("⬇️", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = Localization.get(currentLang, "card_colors"), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))

                        val colorScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(colorScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            cardColorPresets.forEach { (key, color) ->
                                val isSelected = currentColorKey == key
                                val swatchColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.surface else color
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelected(deviceId, key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", fontSize = 12.sp, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(Localization.get(currentLang, "close"))
                }
            }
        }

        if (pairingCode != null) {
            AlertDialog(
                onDismissRequest = { pairingCode = null },
                title = { Text("Code d'association") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Ouvrez l'application Google Home et saisissez ce code pour lier vos appareils :", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = pairingCode!!,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { pairingCode = null }) {
                        Text("J'ai compris")
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------
// FONCTIONS DE LECTURE ROBUSTES
// ---------------------------------------------------------
fun getPowerState(device: Map<String, Any>): Boolean {
    val root = device["power"] ?: device["Power"]
    if (root is Boolean) return root
    if (root != null && root.toString().lowercase() == "true") return true

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("Power", true)) {
                        val v = item["value"]
                        return v == true || v.toString().lowercase() == "true"
                    }
                }
            }
        }
    }
    return false
}

fun getTemperature(device: Map<String, Any>): Float {
    val raw = device["setTemperature"] ?: device["SetTemperature"] ?: device["targetTemperature"]
    val valFromRoot = raw?.toString()?.toFloatOrNull()
    if (valFromRoot != null) return valFromRoot

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("SetTemperature", true) || name.equals("TargetTemperature", true)) {
                        val v = item["value"]?.toString()?.toFloatOrNull()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return 20.0f
}

fun getRoomTemperature(device: Map<String, Any>): Float? {
    val raw = device["roomTemperature"] ?: device["RoomTemperature"] ?: device["indoorTemperature"] ?: device["IndoorTemperature"]
    val valFromRoot = raw?.toString()?.toFloatOrNull()
    if (valFromRoot != null) return valFromRoot

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("RoomTemperature", true) || name.equals("IndoorTemperature", true)) {
                        val v = item["value"]?.toString()?.toFloatOrNull()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return null
}

fun getOperationMode(device: Map<String, Any>): String {
    val raw = device["operationMode"] ?: device["OperationMode"]
    if (raw != null) return raw.toString()

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("OperationMode", true)) {
                        val v = item["value"]?.toString()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return "Automatic"
}

fun getFanSpeed(device: Map<String, Any>): Int {
    val raw = device["setFanSpeed"] ?: device["SetFanSpeed"] ?: device["fanSpeed"]
    val valFromRoot = raw?.toString()?.toFloatOrNull()?.toInt()
    if (valFromRoot != null) return valFromRoot

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("SetFanSpeed", true) || name.equals("FanSpeed", true)) {
                        val v = item["value"]?.toString()?.toFloatOrNull()?.toInt()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return 0
}

fun getVaneVertical(device: Map<String, Any>): String {
    val raw = device["vaneVerticalDirection"] ?: device["VaneVerticalDirection"]
    if (raw != null) return raw.toString()

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("VaneVerticalDirection", true)) {
                        val v = item["value"]?.toString()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return "Auto"
}

fun getVaneHorizontal(device: Map<String, Any>): String {
    val raw = device["vaneHorizontalDirection"] ?: device["VaneHorizontalDirection"]
    if (raw != null) return raw.toString()

    val containers = listOfNotNull(device["settings"], device["unitSettings"])
    for (container in containers) {
        if (container is List<*>) {
            for (item in container) {
                if (item is Map<*, *>) {
                    val name = (item["name"] ?: item["Name"])?.toString() ?: ""
                    if (name.equals("VaneHorizontalDirection", true)) {
                        val v = item["value"]?.toString()
                        if (v != null) return v
                    }
                }
            }
        }
    }
    return "Auto"
}

// ---------------------------------------------------------
// CARTE D'AFFICHAGE PRINCIPALE
// ---------------------------------------------------------
@Composable
fun DeviceCard(
    device: Map<String, Any>,
    cookie: String,
    currentLang: String,
    cardBackgroundColor: Color,
    onStatusUpdate: (String) -> Unit,
    onActionDone: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val deviceId = (device["id"] ?: device["ID"])?.toString() ?: ""

    var isPowerOn by remember { mutableStateOf(getPowerState(device)) }
    var currentTemp by remember { mutableStateOf(getTemperature(device)) }
    var roomTemp by remember { mutableStateOf(getRoomTemperature(device)) }

    LaunchedEffect(device) {
        isPowerOn = getPowerState(device)
        currentTemp = getTemperature(device)
        roomTemp = getRoomTemperature(device)
    }

    val name = (device["givenDisplayName"] ?: device["GivenDisplayName"])?.toString() ?: "Appareil"
    val roomTempLabel = Localization.get(currentLang, "room_temp")
    val targetTempLabel = Localization.get(currentLang, "target_temp")

    val cardColors = if (cardBackgroundColor != Color.Unspecified) {
        CardDefaults.cardColors(containerColor = cardBackgroundColor, contentColor = Color.Black)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isPowerOn,
                    onCheckedChange = { newState ->
                        isPowerOn = newState
                        scope.launch {
                            onStatusUpdate("Power -> $newState")
                            sendAtaunitCommand(cookie, device, deviceId, power = newState, onStatusUpdate = onStatusUpdate)
                            onActionDone()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val roomTempDisplay = if (roomTemp != null) "$roomTempLabel : ${roomTemp}°C" else "$roomTempLabel : --"
            Text(
                text = roomTempDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = if (cardBackgroundColor != Color.Unspecified) Color.DarkGray else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPowerOn) "$targetTempLabel : ${currentTemp}°C" else Localization.get(currentLang, "off"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPowerOn) (if (cardBackgroundColor != Color.Unspecified) Color.Black else MaterialTheme.colorScheme.onSurface) else MaterialTheme.colorScheme.error
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            currentTemp -= 0.5f
                            scope.launch {
                                onStatusUpdate("Temp -")
                                sendAtaunitCommand(cookie, device, deviceId, temp = currentTemp, onStatusUpdate = onStatusUpdate)
                                onActionDone()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            currentTemp += 0.5f
                            scope.launch {
                                onStatusUpdate("Temp +")
                                sendAtaunitCommand(cookie, device, deviceId, temp = currentTemp, onStatusUpdate = onStatusUpdate)
                                onActionDone()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Localization.get(currentLang, "more_settings"))
            }
        }
    }

    if (showDialog) {
        DeviceDetailsDialog(
            device = device,
            cookie = cookie,
            deviceName = name,
            currentLang = currentLang,
            onDismiss = { showDialog = false },
            onStatusUpdate = onStatusUpdate,
            onActionDone = onActionDone
        )
    }
}

// ---------------------------------------------------------
// LA BULLE DE CONTRÔLE (RÉGLAGES AVANCÉS)
// ---------------------------------------------------------
@Composable
fun DeviceDetailsDialog(
    device: Map<String, Any>,
    cookie: String,
    deviceName: String,
    currentLang: String,
    onDismiss: () -> Unit,
    onStatusUpdate: (String) -> Unit,
    onActionDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val deviceId = (device["id"] ?: device["ID"])?.toString() ?: ""

    var currentMode by remember { mutableStateOf(getOperationMode(device)) }
    var currentFan by remember { mutableStateOf(getFanSpeed(device)) }
    var currentVaneV by remember { mutableStateOf(getVaneVertical(device)) }
    var currentVaneH by remember { mutableStateOf(getVaneHorizontal(device)) }

    var showVaneDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Text(Localization.get(currentLang, "mode"), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ModeButton("❄️", "Froid", "Cool", currentMode) { modeVal ->
                        currentMode = modeVal
                        scope.launch { sendAtaunitCommand(cookie, device, deviceId, mode = modeVal, onStatusUpdate = onStatusUpdate); onActionDone() }
                    }
                    ModeButton("☀️", "Chaud", "Heat", currentMode) { modeVal ->
                        currentMode = modeVal
                        scope.launch { sendAtaunitCommand(cookie, device, deviceId, mode = modeVal, onStatusUpdate = onStatusUpdate); onActionDone() }
                    }
                    ModeButton("💧", "Sec", "Dry", currentMode) { modeVal ->
                        currentMode = modeVal
                        scope.launch { sendAtaunitCommand(cookie, device, deviceId, mode = modeVal, onStatusUpdate = onStatusUpdate); onActionDone() }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                        ModeButton("💨", "Vent", "Fan", currentMode) { modeVal ->
                            currentMode = modeVal
                            scope.launch { sendAtaunitCommand(cookie, device, deviceId, mode = modeVal, onStatusUpdate = onStatusUpdate); onActionDone() }
                        }
                    }
                    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                        ModeButton("🔄", "Auto", "Automatic", currentMode) { modeVal ->
                            currentMode = modeVal
                            scope.launch { sendAtaunitCommand(cookie, device, deviceId, mode = modeVal, onStatusUpdate = onStatusUpdate); onActionDone() }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text(Localization.get(currentLang, "fan"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    val fanOptions = listOf(0, 1, 2, 3, 4, 5)
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        fanOptions.forEach { f ->
                            val isSelected = f == currentFan
                            Text(
                                text = if (f == 0) "${Localization.get(currentLang, "auto")} (0)" else "Vitesse $f",
                                fontSize = if (isSelected) 18.sp else 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp).clickable {
                                    currentFan = f
                                    scope.launch {
                                        sendAtaunitCommand(cookie, device, deviceId, fanSpeed = f, onStatusUpdate = onStatusUpdate)
                                        onActionDone()
                                    }
                                }
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                OutlinedButton(onClick = { showVaneDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(Localization.get(currentLang, "vane"))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(Localization.get(currentLang, "close"))
                }
            }
        }
    }

    if (showVaneDialog) {
        Dialog(onDismissRequest = { showVaneDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = Localization.get(currentLang, "vane"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_vert_pos_3),
                            contentDescription = "Ailette Verticale",
                            modifier = Modifier.size(50.dp).padding(end = 8.dp)
                        )
                        Text("Direction Verticale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        val vaneVOptions = listOf(
                            "Auto" to Localization.get(currentLang, "auto"),
                            "One" to "Position 1 (Haut)",
                            "Two" to "Position 2",
                            "Three" to "Position 3",
                            "Four" to "Position 4",
                            "Five" to "Position 5 (Bas)",
                            "Swing" to Localization.get(currentLang, "swing")
                        )
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            vaneVOptions.forEach { (vCode, vLabel) ->
                                val isSelected = currentVaneV == vCode
                                Text(
                                    text = vLabel,
                                    fontSize = if (isSelected) 16.sp else 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 3.dp).clickable {
                                        currentVaneV = vCode
                                        scope.launch {
                                            sendAtaunitCommand(cookie, device, deviceId, vaneV = vCode, onStatusUpdate = onStatusUpdate)
                                            onActionDone()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_horiz_full_pos_3_circle),
                            contentDescription = "Ailette Horizontale",
                            modifier = Modifier.size(50.dp).padding(end = 8.dp)
                        )
                        Text("Direction Horizontale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        val vaneHOptions = listOf(
                            "Auto" to Localization.get(currentLang, "auto"),
                            "Left" to "Position 1 (Gauche)",
                            "LeftCentre" to "Position 2",
                            "Centre" to "Position 3",
                            "RightCentre" to "Position 4",
                            "Right" to "Position 5 (Droite)",
                            "Swing" to Localization.get(currentLang, "swing")
                        )
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            vaneHOptions.forEach { (hCode, hLabel) ->
                                val isSelected = currentVaneH == hCode
                                Text(
                                    text = hLabel,
                                    fontSize = if (isSelected) 16.sp else 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 3.dp).clickable {
                                        currentVaneH = hCode
                                        scope.launch {
                                            sendAtaunitCommand(cookie, device, deviceId, vaneH = hCode, onStatusUpdate = onStatusUpdate)
                                            onActionDone()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { showVaneDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(Localization.get(currentLang, "back"))
                    }
                }
            }
        }
    }
}

@Composable
fun ModeButton(emoji: String, label: String, modeValue: String, currentMode: String, onClick: (String) -> Unit) {
    val isSelected = modeValue == currentMode
    OutlinedButton(
        onClick = { onClick(modeValue) },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Text("$emoji $label", fontSize = 12.sp)
    }
}

// ---------------------------------------------------------
// ENVOI DE L'ÉTAT COMPLET CONFORME AU PAYLOAD OFFICIEL
// ---------------------------------------------------------
suspend fun sendAtaunitCommand(
    cookie: String,
    device: Map<String, Any>,
    deviceId: String,
    power: Boolean? = null,
    temp: Float? = null,
    mode: String? = null,
    fanSpeed: Int? = null,
    vaneV: String? = null,
    vaneH: String? = null,
    onStatusUpdate: (String) -> Unit
) = withContext(Dispatchers.IO) {

    val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
    val safeCookie = cookie.trim().replace("\n", "").replace("\r", "")
    val rawXsrf = "XSRF-TOKEN=([^;]+)".toRegex(RegexOption.IGNORE_CASE).find(safeCookie)?.groupValues?.get(1) ?: "1"
    val decodedXsrf = try { URLDecoder.decode(rawXsrf, "UTF-8") } catch (e: Exception) { rawXsrf }

    val jsonMap = mutableMapOf<String, Any?>()
    jsonMap.putAll(device)

    if (power != null) jsonMap["power"] = power
    if (temp != null) jsonMap["setTemperature"] = temp
    if (mode != null) jsonMap["operationMode"] = mode
    if (fanSpeed != null) jsonMap["setFanSpeed"] = fanSpeed
    if (vaneV != null) jsonMap["vaneVerticalDirection"] = vaneV
    if (vaneH != null) jsonMap["vaneHorizontalDirection"] = vaneH

    val jsonBody = Gson().toJson(jsonMap)

    val request = Request.Builder()
        .url("https://melcloudhome.com/api/ataunit/$deviceId")
        .put(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .addHeader("Cookie", safeCookie)
        .addHeader("X-XSRF-TOKEN", decodedXsrf)
        .addHeader("X-Csrf", "1")
        .addHeader("X-Requested-With", "XMLHttpRequest")
        .addHeader("Accept", "application/json, text/plain, */*")
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                onStatusUpdate("✓")
            } else {
                onStatusUpdate("Err ${response.code}")
            }
        }
    } catch (e: Exception) {
        onStatusUpdate("Err")
    }
}

// ---------------------------------------------------------
// RÉCUPÉRATION DU CODE D'ASSOCIATION GOOGLE HOME (DYNAMIQUE)
// ---------------------------------------------------------
suspend fun getGoogleHomePairingCode(context: Context, cookie: String): String? = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val serverUrl = prefs.getString("render_server_url", "https://melhome-bridge.onrender.com") ?: "https://melhome-bridge.onrender.com"

    val jsonBody = JSONObject().apply {
        put("cookie", cookie)
    }.toString()

    val request = Request.Builder()
        .url("$serverUrl/api/save-cookie")
        .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    try {
        val client = OkHttpClient()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success")) {
                        return@withContext jsonResponse.optString("pairCode")
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}