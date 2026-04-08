package com.example.aplicaciondeprueba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplicaciondeprueba.ui.theme.*

enum class Screen { JOURNAL, GROWTH, CIRCLES, JUVENTUD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AplicacionDePruebaTheme {
                var currentScreen by remember { mutableStateOf(Screen.JOURNAL) }
                
                Scaffold(
                    bottomBar = {
                        BottomNavigation(currentScreen) { currentScreen = it }
                    },
                    floatingActionButton = {
                        if (currentScreen == Screen.JOURNAL) {
                            FloatingActionButton(
                                onClick = { },
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
                            Screen.JOURNAL -> JournalScreen()
                            Screen.GROWTH -> GrowthJourneyScreen()
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

@Composable
fun JournalScreen() {
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
        Text("Good Morning, Elena", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)

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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Daily Growth", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("You've completed 4 of 6 rituals today", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("66%", style = Typography.displayLarge.copy(fontSize = 32.sp), color = OnBackgroundDark)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Simplified Chart
                Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Bar(0.6f, PrimaryGreen)
                    Bar(0.8f, PrimaryGreen.copy(alpha = 0.7f))
                    Bar(0.4f, PrimaryGreen.copy(alpha = 0.5f))
                    Bar(0.9f, PrimaryGreen.copy(alpha = 0.3f))
                    Bar(0.2f, Color.LightGray)
                    Bar(0.15f, Color.LightGray)
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
        RitualItem("Breathwork", "10m", true)
        RitualItem("Hydration", "500ml", false)
        RitualItem("Sunlight exposure", "15m", false)

        Spacer(modifier = Modifier.height(32.dp))

        // Reflection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceHighest)
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(PrimaryGreen.copy(alpha = 0.2f))) {
                    // Placeholder for leaf image
                    Icon(Icons.Default.Park, null, Modifier.align(Alignment.Center).size(64.dp), tint = PrimaryGreen.copy(0.3f))
                }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Write Reflection", color = OnBackgroundDark)
                        }
                        TextButton(onClick = { }) {
                            Text("View History", color = Color.Gray)
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RitualItem(name: String, value: String, isDone: Boolean) {
    Card(
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

// ... Mantener GrowthJourneyScreen, Bar, TrendItem y previews ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthJourneyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Growth Journey",
            style = Typography.displayLarge.copy(fontSize = 40.sp),
            color = OnBackgroundDark
        )
        Text(
            text = "Metrics of your evolving self.",
            style = Typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLow)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CURRENT MOMENTUM", style = Typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("42", style = Typography.displayLarge.copy(color = PrimaryGreen))
                    Text(" days", style = Typography.titleLarge.copy(color = Color.Gray), modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                }
                Text("Your \"Deep Reflection\" habit has been consistent for over a month.", style = Typography.bodyLarge, modifier = Modifier.padding(vertical = 16.dp))
                Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Bar(0.4f, BarColor1)
                    Bar(0.6f, BarColor2)
                    Bar(0.55f, BarColor3)
                    Bar(0.8f, BarColor4)
                    Bar(0.7f, BarColor5)
                    Bar(0.9f, BarColor6)
                    Bar(1.0f, BarColor7)
                }
            }
        }
        // ... El resto de GrowthJourneyScreen ...
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

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AplicacionDePruebaTheme {
        JournalScreen()
    }
}
