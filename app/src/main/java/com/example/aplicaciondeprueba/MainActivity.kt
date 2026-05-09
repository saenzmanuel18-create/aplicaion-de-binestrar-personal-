package com.example.aplicaciondeprueba

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.aplicaciondeprueba.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

enum class Screen { LOGIN, JOURNAL, GROWTH, CIRCLES, JUVENTUD }

data class Ritual(val id: Int, val name: String, val duration: String, val isDone: Boolean = false, val baserowId: Int? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedEmail = sharedPrefs.getString("user_email", null)

        enableEdgeToEdge()
        setContent {
            AplicacionDePruebaTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val apiKey = "Token P2hWYmrnX6vhHToRX2ere6CwRTnshwx3"
                
                var currentScreen by remember { mutableStateOf(if (savedEmail == null) Screen.LOGIN else Screen.JOURNAL) }
                var rituals by remember { mutableStateOf(emptyList<Ritual>()) }
                var isLoading by remember { mutableStateOf(false) }
                var showAddRitualDialog by remember { mutableStateOf(false) }
                var userEmail by remember { mutableStateOf(savedEmail) }
                val snackbarHostState = remember { SnackbarHostState() }
                var showReflectionDialog by remember { mutableStateOf(false) }

                // Cargar datos al iniciar sesión o al cambiar de pantalla
                LaunchedEffect(userEmail) {
                    if (userEmail != null) {
                        isLoading = true
                        try {
                            val response = BaserowClient.service.getRituals(apiKey)
                            rituals = response.results.mapIndexed { index, br ->
                                Ritual(index + 1, br.name, br.duration, br.isDone, br.id)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                }

                if (showAddRitualDialog) {
                    AddRitualDialog(
                        onDismiss = { showAddRitualDialog = false },
                        onAdd = { name, duration ->
                            scope.launch {
                                try {
                                    val newBaserowRitual = BaserowClient.service.addRitual(
                                        apiKey, BaserowRitual(name = name, duration = duration, isDone = false)
                                    )
                                    rituals = rituals + Ritual(rituals.size + 1, name, duration, false, newBaserowRitual.id)
                                    snackbarHostState.showSnackbar("Ritual añadido con éxito")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showAddRitualDialog = false
                        }
                    )
                }

                if (showReflectionDialog) {
                    ReflectionDialog(
                        onDismiss = { showReflectionDialog = false },
                        onSave = { reflection ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Reflexión guardada")
                            }
                            showReflectionDialog = false
                        }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (currentScreen != Screen.LOGIN) {
                            BottomNavigation(currentScreen) { currentScreen = it }
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == Screen.JOURNAL) {
                            FloatingActionButton(
                                onClick = { showAddRitualDialog = true },
                                containerColor = PrimaryGreen,
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    },
                    containerColor = BackgroundCream
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.LOGIN -> LoginScreen(
                                onLoginSuccess = { email ->
                                    userEmail = email
                                    sharedPrefs.edit().putString("user_email", email).apply()
                                    scope.launch {
                                        try {
                                            BaserowClient.service.registerUser(apiKey, BaserowUser(email = email))
                                        } catch (e: Exception) {}
                                        currentScreen = Screen.JOURNAL
                                        snackbarHostState.showSnackbar("¡Hola de nuevo!")
                                    }
                                },
                                onGoogleLogin = {
                                    scope.launch {
                                        val credentialManager = CredentialManager.create(context)
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId("TU_CLIENT_ID_DE_GOOGLE.apps.googleusercontent.com")
                                            .setAutoSelectEnabled(true)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        try {
                                            val result = credentialManager.getCredential(context, request)
                                            val credential = result.credential
                                            if (credential is GoogleIdTokenCredential) {
                                                val email = credential.id
                                                userEmail = email
                                                sharedPrefs.edit().putString("user_email", email).apply()
                                                BaserowClient.service.registerUser(apiKey, BaserowUser(email = email))
                                                currentScreen = Screen.JOURNAL
                                                snackbarHostState.showSnackbar("Sesión iniciada con Google")
                                            }
                                        } catch (e: Exception) {
                                            // Mock para propósitos visuales si no hay Client ID configurado
                                            val mockEmail = "usuario.demo@gmail.com"
                                            userEmail = mockEmail
                                            sharedPrefs.edit().putString("user_email", mockEmail).apply()
                                            currentScreen = Screen.JOURNAL
                                            snackbarHostState.showSnackbar("Modo Demo: Sesión iniciada")
                                        }
                                    }
                                }
                            )
                            Screen.JOURNAL -> JournalScreen(
                                userName = userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Invitado",
                                rituals = rituals,
                                onRitualToggle = { id ->
                                    val ritual = rituals.find { it.id == id }
                                    if (ritual != null && ritual.baserowId != null) {
                                        scope.launch {
                                            try {
                                                val updatedStatus = !ritual.isDone
                                                BaserowClient.service.updateRitual(
                                                    apiKey, 
                                                    ritual.baserowId, 
                                                    BaserowRitual(name = ritual.name, duration = ritual.duration, isDone = updatedStatus)
                                                )
                                                rituals = rituals.map {
                                                    if (it.id == id) it.copy(isDone = updatedStatus) else it
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                },
                                onCompleteAll = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡Excelente progreso!")
                                    }
                                },
                                onWriteReflection = {
                                    showReflectionDialog = true
                                }
                            )
                            Screen.GROWTH -> GrowthJourneyScreen {
                                scope.launch { snackbarHostState.showSnackbar("Función próximamente") }
                            }
                            Screen.CIRCLES -> CirclesScreen(
                                onJoinChallenge = { scope.launch { snackbarHostState.showSnackbar("¡Te has unido!") } },
                                onExploreAll = { scope.launch { snackbarHostState.showSnackbar("Explorando comunidades...") } }
                            )
                            Screen.JUVENTUD -> JuventudScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onGoogleLogin: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo decorativo o color sólido
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Icono central (Tu monstruito)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White, CircleShape)
                    .padding(8.dp)
                    .shadow(10.dp, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icono_montruo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Bienvenido a\nSapling & Stone",
                style = Typography.displayLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Cultiva tu mente, nutre tu espíritu.",
                style = Typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Campo de Email con estilo moderno
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryGreen) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (email.contains("@")) {
                        onLoginSuccess(email)
                    } else {
                        Toast.makeText(context, "Por favor, ingresa un correo válido", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                Text(
                    " O continúa con ", 
                    modifier = Modifier.padding(horizontal = 16.dp), 
                    style = Typography.labelSmall,
                    color = Color.Gray
                )
                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón de Google con estilo oficial
            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Login, // Usamos un icono representativo
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Iniciar sesión con Google", 
                        color = OnBackgroundDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- RESTO DE PANTALLAS (Journal, Growth, Circles, Juventud) ---

@Composable
fun JournalScreen(
    userName: String, 
    rituals: List<Ritual>, 
    onRitualToggle: (Int) -> Unit,
    onCompleteAll: () -> Unit,
    onWriteReflection: () -> Unit,
    onLogout: () -> Unit
) {
    val completedCount = rituals.count { it.isDone }
    val totalCount = rituals.size
    val percentage = if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sapling & Stone", style = Typography.titleLarge.copy(fontSize = 18.sp), color = PrimaryGreen)
            }
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.Default.Logout, 
                    contentDescription = "Logout", 
                    modifier = Modifier.size(28.dp),
                    tint = OnBackgroundDark
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Hoy es un buen día", style = Typography.labelSmall, color = Color.Gray)
        Text("Hola, $userName", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = PrimaryGreen)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("12 Días de Racha", fontWeight = FontWeight.Bold)
                    Text("Sigue creciendo cada día", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Progreso Diario", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnBackgroundDark)
                        Text("Has completado $completedCount de $totalCount rituales", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(
                        text = "$percentage%",
                        style = Typography.titleLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = PrimaryGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Bar(if(percentage > 0) percentage/100f else 0.1f, PrimaryGreen.copy(alpha = 0.9f))
                    Bar(0.75f, PrimaryGreen.copy(alpha = 0.7f))
                    Bar(0.45f, PrimaryGreen.copy(alpha = 0.5f))
                    Bar(0.85f, PrimaryGreen.copy(alpha = 0.3f))
                    Bar(0.25f, BarColor1)
                    Bar(0.20f, BarColor1.copy(alpha = 0.5f))
                    Bar(0.15f, BarColor1.copy(alpha = 0.3f))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onCompleteAll,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Completar Ritual Mañanero")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rituales de Mañana", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        rituals.forEach { ritual ->
            RitualItem(ritual.name, ritual.duration, ritual.isDone) {
                onRitualToggle(ritual.id)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceHighest)
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.imagen_de_bosque),
                    contentDescription = "Bosque",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("REFLEXIÓN NOCTURNA", style = Typography.labelSmall, color = Color.Gray)
                    Text(
                        "El espacio entre pensamientos es donde comienza el crecimiento.",
                        style = Typography.headlineMedium.copy(fontSize = 24.sp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(
                        "Tómate un momento esta noche para observar el silencio. Sin juicios, solo consciencia.",
                        style = Typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onWriteReflection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Escribir Reflexión", color = OnBackgroundDark)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        InsightCard(
            icon = Icons.Default.Psychology,
            title = "Claridad Mental",
            description = "Tu puntaje de enfoque mejoró un 12% esta semana.",
            progress = 0.7f
        )
        
        InsightCard(
            icon = Icons.Default.Eco,
            title = "Ritmo Sostenible",
            description = "Has mantenido la consistencia sin signos de agotamiento.",
            progress = 0.4f
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InsightCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE8F0E5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackgroundDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryGreen,
                    trackColor = Color.LightGray.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
fun RitualItem(name: String, value: String, isDone: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDone) Color(0xFFE8F0E5) else Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.dp, if (isDone) PrimaryGreen else Color.LightGray, CircleShape)
                        .background(if (isDone) PrimaryGreen else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) Icon(Icons.Default.Check, null, Modifier.size(16.dp), Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, fontWeight = FontWeight.Medium)
            }
            Text(value, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BottomNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(containerColor = BackgroundCream, tonalElevation = 0.dp) {
        NavigationBarItem(
            icon = { Icon(if(currentScreen == Screen.JOURNAL) Icons.Default.AutoStories else Icons.Outlined.AutoStories, null) },
            label = { Text("DIARIO", style = Typography.labelSmall) },
            selected = currentScreen == Screen.JOURNAL,
            onClick = { onScreenSelected(Screen.JOURNAL) }
        )
        NavigationBarItem(
            icon = { Icon(if(currentScreen == Screen.GROWTH) Icons.Default.QueryStats else Icons.Outlined.QueryStats, null) },
            label = { Text("CRECIMIENTO", style = Typography.labelSmall) },
            selected = currentScreen == Screen.GROWTH,
            onClick = { onScreenSelected(Screen.GROWTH) }
        )
        NavigationBarItem(
            icon = { Icon(if(currentScreen == Screen.CIRCLES) Icons.Default.Groups else Icons.Outlined.Groups, null) },
            label = { Text("COMUNIDAD", style = Typography.labelSmall) },
            selected = currentScreen == Screen.CIRCLES,
            onClick = { onScreenSelected(Screen.CIRCLES) }
        )
        NavigationBarItem(
            icon = { Icon(if(currentScreen == Screen.JUVENTUD) Icons.Default.Spa else Icons.Outlined.Spa, null) },
            label = { Text("VITALIDAD", style = Typography.labelSmall) },
            selected = currentScreen == Screen.JUVENTUD,
            onClick = { onScreenSelected(Screen.JUVENTUD) }
        )
    }
}

@Composable
fun RowScope.Bar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
    )
}

@Composable
fun AddRitualDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Ritual", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del ritual") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duración (ej. 10m)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && duration.isNotBlank()) onAdd(name, duration) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Agregar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun ReflectionDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var reflection by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diario de Reflexión", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("¿Cómo te sientes hoy?", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    label = { Text("Mi reflexión") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reflection.isNotBlank()) onSave(reflection) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun JuventudScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Spa, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VITALITY & YOUTH", style = Typography.labelSmall, color = PrimaryGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Longevity Lab", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)
        Text("Ciencia aplicada a tu vitalidad", style = Typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("EDAD BIOLÓGICA ESTIMADA", style = Typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("24.5", style = Typography.displayLarge.copy(fontSize = 48.sp, color = Color.White))
                Text("-2.4 años este mes", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { 0.65f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Bio-rituales diarios", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        BioRitualItem("Exposición al frío", "3 min", Icons.Default.AcUnit)
        BioRitualItem("Terapia de luz roja", "10 min", Icons.Default.LightMode)
        BioRitualItem("Ayuno intermitente", "16:8", Icons.Default.Timer)

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NightsStay, null, tint = Color(0xFF5C6BC0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CALIDAD DE SUEÑO", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("8h 12m", style = Typography.displayLarge.copy(fontSize = 32.sp))
                Text("Sueño profundo +15%", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ver análisis")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BioRitualItem(title: String, target: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(SurfaceLow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(target, fontSize = 12.sp, color = Color.Gray)
                }
            }
            var checked by remember { mutableStateOf(false) }
            Checkbox(checked = checked, onCheckedChange = { checked = it })
        }
    }
}

@Composable
fun GrowthJourneyScreen(onAdjustSchedule: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Eco, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("The Living Journal", style = Typography.labelSmall, color = PrimaryGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Crecimiento Personal", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)
        Text("Métricas de tu evolución", style = Typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("IMPULSO ACTUAL", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("42", style = Typography.displayLarge.copy(fontSize = 56.sp, color = PrimaryGreen))
                    Text(" días", style = Typography.titleLarge.copy(color = Color.Gray), modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                }
                Text(
                    "Tu racha de reflexión profunda lleva más de un mes. El crecimiento es exponencial.", 
                    style = Typography.bodyLarge, 
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Bar(0.2f, PrimaryGreen.copy(0.1f))
                    Bar(0.3f, PrimaryGreen.copy(0.2f))
                    Bar(0.4f, PrimaryGreen.copy(0.3f))
                    Bar(0.25f, PrimaryGreen.copy(0.4f))
                    Bar(0.5f, PrimaryGreen.copy(0.5f))
                    Bar(0.6f, PrimaryGreen.copy(0.7f))
                    Bar(0.7f, PrimaryGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EBE6))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(Modifier.size(40.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircleOutline, null, tint = PrimaryGreen)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("TASA DE COMPLETADO", style = Typography.labelSmall, color = Color.Gray)
                Text("94%", style = Typography.displayLarge.copy(fontSize = 40.sp), color = OnBackgroundDark)
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.Gray.copy(0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("\"La consistencia es la marca del maestro.\"", style = Typography.labelSmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("TENDENCIAS MENSUALES", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                TrendRow("Yoga Mañanero", 0.8f)
                TrendRow("Detox Digital", 0.6f)
                TrendRow("Hidratación", 0.95f)

                Spacer(modifier = Modifier.height(24.dp))
                
                Image(
                    painter = painterResource(id = R.drawable.imagen_de_bosque),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("MAPA DE HÁBITOS", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(7) { col ->
                                val intensity = (0..10).random()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                intensity > 8 -> PrimaryGreen
                                                intensity > 5 -> PrimaryGreen.copy(alpha = 0.6f)
                                                intensity > 2 -> PrimaryGreen.copy(alpha = 0.3f)
                                                else -> Color.LightGray.copy(alpha = 0.2f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Pico de Consistencia", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Eres más activo entre las 7 AM y 9 AM. Esta es tu \"Ventana de Oro\".",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAdjustSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ajustar Horarios")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TrendRow(label: String, progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(100.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
            color = PrimaryGreen,
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun CirclesScreen(onJoinChallenge: () -> Unit, onExploreAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(32.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.imagen_de_bosque),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "DESAFÍO COMUNITARIO DESTACADO",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = Typography.labelSmall,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "21 Días de Mañanas\nConscientes",
                    style = Typography.displayLarge.copy(fontSize = 32.sp, color = Color.White),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Únete a 1,240 personas en un viaje colectivo para reclamar tus mañanas.",
                    style = Typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onJoinChallenge,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Unirse al Desafío")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Mis Círculos", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackgroundDark)
                Text("Tus espacios de crecimiento", fontSize = 12.sp, color = Color.Gray)
            }
            TextButton(onClick = onExploreAll) {
                Text("Explorar todos", color = Color.Gray, fontSize = 14.sp)
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircleItem("Madrugadores", "42 Activos hoy", Icons.Default.LightMode, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLow.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text("Momentos de la Comunidad", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackgroundDark)
        
        Spacer(modifier = Modifier.height(24.dp))

        MomentCard(
            author = "Sarah Bloom",
            time = "Hace 2 horas en Madrugadores",
            content = "\"Viendo el amanecer con un té caliente. Hay magia en el silencio antes de que el mundo despierte. 🌿\"",
            imageRes = R.drawable.imagen_de_bosque,
            cheers = 24,
            comments = 4
        )

        MomentCard(
            author = "Marcus Stone",
            time = "Hace 5 horas en Plant-Based",
            content = "Finalmente dominé la receta de tacos de nuez. Mi cuerpo se siente mucho más ligero hoy. 🥑✨",
            cheers = 56,
            comments = 12
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CircleItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFD8E6D5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .background(Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width((-4).dp))
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+12", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MomentCard(author: String, time: String, content: String, imageRes: Int? = null, cheers: Int, comments: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color.LightGray, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(author, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(time, fontSize = 11.sp, color = Color.Gray)
                }
                Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(content, fontSize = 14.sp, color = OnBackgroundDark, lineHeight = 20.sp)
            
            if (imageRes != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$cheers Cheers", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$comments", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
