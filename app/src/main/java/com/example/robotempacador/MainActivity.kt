package com.example.robotempacador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- CLASES DE DATOS (ESTADO DEL JUEGO) ---
data class Position(val x: Int, val y: Int)

data class GameState(
    val robotPos: Position = Position(0, 4),
    val boxPos: Position = Position(2, 2),
    val targetPos: Position = Position(4, 0),
    val hasBox: Boolean = false,
    val commands: List<String> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFE0F7FA)
                ) {
                    var gameState by remember { mutableStateOf(GameState()) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🤖 Robot Kids Nativo",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // EL TABLERO DE JUEGO
                        GameBoard(state = gameState)

                        Spacer(modifier = Modifier.height(16.dp))

                        // COLA DE COMANDOS (GUION)
                        CommandList(commands = gameState.commands)

                        Spacer(modifier = Modifier.height(16.dp))

                        // PANEL DE CONTROLES
                        ControlPanel(
                            onCommandAdd = { cmd ->
                                gameState = gameState.copy(commands = gameState.commands + cmd)
                            },
                            onClear = {
                                gameState = gameState.copy(commands = emptyList())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(state: GameState) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
            .border(4.dp, Color.White, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(5) { y ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(5) { x ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentPos = Position(x, y)
                            when {
                                state.robotPos == currentPos -> {
                                    Text(text = if (state.hasBox) "🤖📦" else "🤖", fontSize = 32.sp)
                                }
                                state.boxPos == currentPos && !state.hasBox -> {
                                    Text(text = "📦", fontSize = 28.sp)
                                }
                                state.targetPos == currentPos -> {
                                    Text(text = "🏁", fontSize = 28.sp)
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
fun CommandList(commands: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Guion de movimientos:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF455A64),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(commands) { cmd ->
                CommandIcon(cmd)
            }
        }
    }
}

@Composable
fun CommandIcon(cmd: String) {
    val icon = when (cmd) {
        "ARRIBA" -> Icons.Default.KeyboardArrowUp
        "ABAJO" -> Icons.Default.KeyboardArrowDown
        "IZQUIERDA" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        "DERECHA" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        "TOMAR" -> Icons.Default.Add
        "DEJAR" -> Icons.Default.ArrowDropDown
        else -> Icons.Default.Info
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF2196F3), CircleShape)
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = cmd,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ControlPanel(onCommandAdd: (String) -> Unit, onClear: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Cruz de direcciones
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ControlButton(Icons.Default.KeyboardArrowUp, "ARRIBA") { onCommandAdd("ARRIBA") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "IZQUIERDA") { onCommandAdd("IZQUIERDA") }
                Spacer(modifier = Modifier.width(48.dp))
                ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "DERECHA") { onCommandAdd("DERECHA") }
            }
            ControlButton(Icons.Default.KeyboardArrowDown, "ABAJO") { onCommandAdd("ABAJO") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onCommandAdd("TOMAR") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TOMAR 📥")
            }
            Button(
                onClick = { onCommandAdd("DEJAR") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DEJAR 📤")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Botón de Limpiar
        TextButton(onClick = onClear) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(4.dp))
            Text("BORRAR GUION", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ControlButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, Color(0xFF2196F3), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(32.dp)
        )
    }
}
