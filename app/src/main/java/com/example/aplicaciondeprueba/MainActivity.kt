package com.example.aplicaciondeprueba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplicaciondeprueba.ui.theme.*
import kotlinx.coroutines.launch

enum class Screen { JOURNAL, GROWTH, CIRCLES, JUVENTUD }

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
                val apiKey = "Token P2hWYmrnX6vhHToRX2ere6CwRTnshwx3"
                
                var currentScreen by remember { mutableStateOf(Screen.JOURNAL) }
                var rituals by remember { mutableStateOf(emptyList<Ritual>()) }
                var isLoading by remember { mutableStateOf(false) }
                var showAddRitualDialog by remember { mutableStateOf(false) }
                var userEmail by remember { mutableStateOf(savedEmail) }
                var showEmailDialog by remember { mutableStateOf(userEmail == null) }

                // Cargar datos al iniciar
                LaunchedEffect(Unit) {
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
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showAddRitualDialog = false
                        }
                    )
                }

                if (showEmailDialog) {
                    EmailRegistrationDialog(
                        onDismiss = { showEmailDialog = false },
                        onRegister = { email ->
                            scope.launch {
                                try {
                                    BaserowClient.service.registerUser(
                                        apiKey, BaserowUser(email = email)
                                    )
                                    sharedPrefs.edit().putString("user_email", email).apply()
                                    userEmail = email
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showEmailDialog = false
                        }
                    )
                }

                Scaffold(
                    bottomBar = {
                        BottomNavigation(currentScreen) { currentScreen = it }
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
                        if (isLoading && rituals.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryGreen)
                            }
                        } else {
                            when (currentScreen) {
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
                                    }
                                )
                                Screen.GROWTH -> GrowthJourneyScreen()
                                Screen.CIRCLES -> CirclesScreen()
                                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Próximamente")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen(userName: String, rituals: List<Ritual>, onRitualToggle: (Int) -> Unit) {
    val completedCount = rituals.count { it.isDone }
    val totalCount = rituals.size
    val percentage = if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Top Logo Area
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
            Icon(
                Icons.Default.AccountCircle, 
                contentDescription = null, 
                modifier = Modifier.size(32.dp),
                tint = OnBackgroundDark
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Greeting
        Text("Thursday, Oct 24", style = Typography.labelSmall, color = Color.Gray)
        Text("Good Morning, $userName", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)

        Spacer(modifier = Modifier.height(24.dp))

        // Streak Card
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
                    Text("12 Day Streak", fontWeight = FontWeight.Bold)
                    Text("Keep growing daily", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Growth Rituals Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // AQUÍ EL PORCENTAJE ESTÁ HORIZONTAL CON EL TÍTULO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Growth", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnBackgroundDark)
                        Text("You've completed $completedCount of $totalCount rituals", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(
                        text = "$percentage%",
                        style = Typography.titleLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = PrimaryGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Gráfica de barras
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
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Complete Morning Ritual")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Morning Rituals List
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Morning Rituals", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        rituals.forEach { ritual ->
            RitualItem(ritual.name, ritual.duration, ritual.isDone) {
                onRitualToggle(ritual.id)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reflection Card (CON LA IMAGEN DEL BOSQUE)
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
                    Text("EVENING REFLECTION", style = Typography.labelSmall, color = Color.Gray)
                    Text(
                        "The space between thoughts is where growth begins.",
                        style = Typography.headlineMedium.copy(fontSize = 24.sp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(
                        "Take a moment tonight to observe the quiet. No judgment, just awareness.",
                        style = Typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Write Reflection", color = OnBackgroundDark)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INSIGHTS SECTION (LO NUEVO)
        InsightCard(
            icon = Icons.Default.Psychology,
            title = "Mental Clarity",
            description = "Your focus score improved by 12% this week after consistent breathwork rituals.",
            progress = 0.7f
        )
        
        InsightCard(
            icon = Icons.Default.Eco,
            title = "Sustainable Pace",
            description = "You've maintained a consistent habit velocity without burnout signs for 20 days.",
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
            label = { Text("JOURNAL", style = Typography.labelSmall) },
            selected = currentScreen == Screen.JOURNAL,
            onClick = { onScreenSelected(Screen.JOURNAL) }
        )
        NavigationBarItem(
            icon = { Icon(if(currentScreen == Screen.GROWTH) Icons.Default.QueryStats else Icons.Outlined.QueryStats, null) },
            label = { Text("GROWTH", style = Typography.labelSmall) },
            selected = currentScreen == Screen.GROWTH,
            onClick = { onScreenSelected(Screen.GROWTH) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Groups, null) },
            label = { Text("CIRCLES", style = Typography.labelSmall) },
            selected = currentScreen == Screen.CIRCLES,
            onClick = { onScreenSelected(Screen.CIRCLES) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Spa, null) },
            label = { Text("JUVENTUD", style = Typography.labelSmall) },
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
fun EmailRegistrationDialog(onDismiss: () -> Unit, onRegister: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome!", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Please enter your email to get started.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onRegister(email) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Register", color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun GrowthJourneyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Logo de la parte superior (opcional pero coherente)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Eco, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("The Living Journal", style = Typography.labelSmall, color = PrimaryGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Growth Journey", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)
        Text("Metrics of your evolving self.", style = Typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // 1. CURRENT MOMENTUM CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CURRENT MOMENTUM", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("42", style = Typography.displayLarge.copy(fontSize = 56.sp, color = PrimaryGreen))
                    Text(" days", style = Typography.titleLarge.copy(color = Color.Gray), modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                }
                Text(
                    "Your \"Deep Reflection\" habit has been consistent for over a month. Growth is compound.", 
                    style = Typography.bodyLarge, 
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                // Gráfica de barras ascendente
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

        // 2. COMPLETION RATE CARD (INDEPENDIENTE)
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
                Text("COMPLETION RATE", style = Typography.labelSmall, color = Color.Gray)
                Text("94%", style = Typography.displayLarge.copy(fontSize = 40.sp), color = OnBackgroundDark)
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.Gray.copy(0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("\"Consistency is the signature of mastery.\"", style = Typography.labelSmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. MONTHLY TRENDS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("MONTHLY TRENDS", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                TrendRow("Morning Yoga", 0.8f)
                TrendRow("Digital Detox", 0.6f)
                TrendRow("Hydration", 0.95f)

                Spacer(modifier = Modifier.height(24.dp))
                
                // Imagen decorativa al final de la tarjeta
                Image(
                    painter = painterResource(id = R.drawable.imagen_de_bosque), // Reutilizamos tu imagen o puedes poner otra
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. HABIT HEATMAP CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("HABIT HEATMAP", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cuadrícula de actividad (Heatmap)
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
                Text("Consistency Peak", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "You are most active between 7 AM and 9 AM. This is your \"Golden Window\" for high-impact growth activities.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Adjust Schedule")
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
fun CirclesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // 1. FEATURED CHALLENGE CARD
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
                        "FEATURED COMMUNITY CHALLENGE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = Typography.labelSmall,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "21-Day Mindful\nMorning Sprint",
                    style = Typography.displayLarge.copy(fontSize = 32.sp, color = Color.White),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Join 1,240 others in a collective journey to reclaim your mornings through silence and movement.",
                    style = Typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Join Challenge")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. MY CIRCLES SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("My Circles", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackgroundDark)
                Text("Your thriving growth spaces", fontSize = 12.sp, color = Color.Gray)
            }
            TextButton(onClick = { }) {
                Text("Explore all", color = Color.Gray, fontSize = 14.sp)
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal row for Circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircleItem("Early Risers", "42 Active Today", Icons.Default.LightMode, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLow.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 3. COMMUNITY MOMENTS
        Text("Community Moments", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackgroundDark)
        
        Spacer(modifier = Modifier.height(24.dp))

        MomentCard(
            author = "Sarah Bloom",
            time = "2 hours ago in Early Risers",
            content = "\"Watched the sunrise with a hot cup of lemon water. There is so much magic in the quiet hours before the world wakes up. 🌿\"",
            imageRes = R.drawable.imagen_de_bosque,
            cheers = 24,
            comments = 4
        )

        MomentCard(
            author = "Marcus Stone",
            time = "5 hours ago in Plant-Based",
            content = "Finally mastered the walnut-based 'taco meat'! High protein, high energy. My body feels so much lighter lately. 🥑✨",
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
