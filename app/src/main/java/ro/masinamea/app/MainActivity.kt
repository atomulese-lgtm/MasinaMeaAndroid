package ro.masinamea.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Vehicle(
    val id: Long,
    val name: String,
    val plate: String,
    val vin: String = "",
    val mileage: String = "",
    val documents: List<CarDocument> = emptyList()
)
data class CarDocument(
    val id: Long,
    val type: String,
    val expires: String,
    val cost: String = "",
    val photoUri: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
        }
        val request = PeriodicWorkRequestBuilder<DocumentReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("document_reminders", ExistingPeriodicWorkPolicy.UPDATE, request)
        setContent { MaterialTheme { CarApp() } }
    }
}

private const val PREFS = "masina_mea"
private const val VEHICLES_KEY = "vehicles"

fun loadVehicles(context: Context): List<Vehicle> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(VEHICLES_KEY, "[]") ?: "[]"
    val arr = JSONArray(raw)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val docs = o.optJSONArray("documents") ?: JSONArray()
        Vehicle(o.optLong("id"), o.optString("name"), o.optString("plate"), o.optString("vin"), o.optString("mileage"),
            (0 until docs.length()).map { j ->
                val d = docs.getJSONObject(j)
                CarDocument(d.optLong("id"), d.optString("type"), d.optString("expires"), d.optString("cost"), d.optString("photoUri"))
            })
    }
}
fun saveVehicles(context: Context, vehicles: List<Vehicle>) {
    val arr = JSONArray()
    vehicles.forEach { v ->
        val docs = JSONArray()
        v.documents.forEach { d ->
            docs.put(JSONObject().put("id", d.id).put("type", d.type).put("expires", d.expires)
                .put("cost", d.cost).put("photoUri", d.photoUri))
        }
        arr.put(JSONObject().put("id", v.id).put("name", v.name).put("plate", v.plate)
            .put("vin", v.vin).put("mileage", v.mileage).put("documents", docs))
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(VEHICLES_KEY, arr.toString()).apply()
}
fun nextId() = System.currentTimeMillis()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarApp() {
    val context = LocalContext.current
    var vehicles by remember { mutableStateOf(loadVehicles(context)) }
    var selectedVehicle by remember { mutableStateOf<Long?>(null) }
    var showAddCar by remember { mutableStateOf(false) }
    var showAddDoc by remember { mutableStateOf<Long?>(null) }
    fun update(list: List<Vehicle>) { vehicles = list; saveVehicles(context, list) }
    val selected = vehicles.firstOrNull { it.id == selectedVehicle }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mașina Mea") }, actions = {
            IconButton(onClick = { showAddCar = true }) { Icon(Icons.Default.Add, contentDescription = "Adaugă mașină") }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCar = true }) { Icon(Icons.Default.Add, contentDescription = "Adaugă mașină") }
        }
    ) { padding ->
        if (selected == null) {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Mașinile mele", style = MaterialTheme.typography.headlineSmall) }
                item { Text("Adaugă fiecare mașină și urmărește expirarea documentelor sale.") }
                if (vehicles.isEmpty()) item {
                    Card { Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        Text("Nu ai adăugat încă nicio mașină.")
                        Text("Apasă + pentru a începe.")
                    } }
                }
                items(vehicles, key = { it.id }) { v ->
                    Card(onClick = { selectedVehicle = v.id }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(v.name, style = MaterialTheme.typography.titleMedium)
                            Text(v.plate)
                            Text("${v.documents.size} document(e)")
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    TextButton(onClick = { selectedVehicle = null }) { Text("← Toate mașinile") }
                    Text(selected.name, style = MaterialTheme.typography.headlineSmall)
                    Text("Număr: ${selected.plate}")
                    if (selected.vin.isNotBlank()) Text("VIN: ${selected.vin}")
                    if (selected.mileage.isNotBlank()) Text("Kilometraj: ${selected.mileage} km")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showAddDoc = selected.id }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Adaugă document")
                    }
                    Text("Documente", style = MaterialTheme.typography.titleLarge)
                }
                if (selected.documents.isEmpty()) item { Text("Nu ai documente salvate pentru această mașină.") }
                items(selected.documents, key = { it.id }) { doc ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(doc.type, style = MaterialTheme.typography.titleMedium)
                            Text("Expiră: ${doc.expires}")
                            if (doc.cost.isNotBlank()) Text("Cost: ${doc.cost} lei")
                            if (doc.photoUri.isNotBlank()) Text("Are atașată o fotografie/document.")
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    update(vehicles.map { v -> if (v.id == selected.id) v.copy(documents = v.documents.filterNot { it.id == doc.id }) else v })
                                }) { Icon(Icons.Default.Delete, contentDescription = null); Text("Șterge") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCar) AddCarDialog(onDismiss = { showAddCar = false }, onSave = { name, plate, vin, mileage ->
        update(vehicles + Vehicle(nextId(), name, plate, vin, mileage))
        showAddCar = false
    })
    showAddDoc?.let { vehicleId ->
        AddDocumentDialog(onDismiss = { showAddDoc = null }, onSave = { type, expires, cost, uri ->
            update(vehicles.map { v -> if (v.id == vehicleId) v.copy(documents = v.documents + CarDocument(nextId(), type, expires, cost, uri)) else v })
            showAddDoc = null
        })
    }
}

@Composable
fun AddCarDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adaugă mașină") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Marcă și model") }, singleLine = true)
            OutlinedTextField(plate, { plate = it }, label = { Text("Număr înmatriculare") }, singleLine = true)
            OutlinedTextField(vin, { vin = it }, label = { Text("Serie VIN (opțional)") }, singleLine = true)
            OutlinedTextField(mileage, { mileage = it }, label = { Text("Kilometraj (opțional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank() && plate.isNotBlank(), onClick = { onSave(name.trim(), plate.trim(), vin.trim(), mileage.trim()) }) { Text("Salvează") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } })
}

@Composable
fun AddDocumentDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var type by remember { mutableStateOf("RCA") }
    var expires by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    val types = listOf("RCA", "CASCO", "ITP", "Rovinietă", "Alt document")
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            photoUri = uri.toString()
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adaugă document") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tip document")
            types.forEach { option ->
                Row {
                    RadioButton(selected = type == option, onClick = { type = option })
                    Text(option, modifier = Modifier.padding(top = 12.dp))
                }
            }
            OutlinedTextField(expires, { expires = it }, label = { Text("Data expirării (ZZ.LL.AAAA)") }, singleLine = true)
            OutlinedTextField(cost, { cost = it }, label = { Text("Cost în lei (opțional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf")) }) {
                Icon(Icons.Default.Photo, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (photoUri.isBlank()) "Atașează fotografie/PDF" else "Document atașat")
            }
            Text("Introdu data ca 31.12.2026.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(enabled = validDate(expires), onClick = { onSave(type, expires.trim(), cost.trim(), photoUri) }) { Text("Salvează") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } })
}

fun validDate(s: String): Boolean = try {
    val f = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).apply { isLenient = false }
    f.parse(s); s.length == 10
} catch (_: Exception) { false }

class DocumentReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val vehicles = loadVehicles(applicationContext)
        val today = System.currentTimeMillis()
        val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vehicles.forEach { car ->
            car.documents.forEach { doc ->
                try {
                    val expiry = fmt.parse(doc.expires)?.time ?: return@forEach
                    val days = TimeUnit.MILLISECONDS.toDays(expiry - today)
                    if (days in 0..30 && (days == 30L || days == 7L || days == 1L || days == 0L)) {
                        val id = (car.id + doc.id + days).toInt()
                        val title = if (days == 0L) "Documentul expiră astăzi" else "${doc.type} expiră în $days zile"
                        val notification = NotificationCompat.Builder(applicationContext, "documents")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(title)
                            .setContentText("${car.name} (${car.plate}) · ${doc.type} · ${doc.expires}")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true).build()
                        manager.notify(id, notification)
                    }
                } catch (_: Exception) { }
            }
        }
        return Result.success()
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("documents", "Expirare documente", NotificationManager.IMPORTANCE_DEFAULT)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
