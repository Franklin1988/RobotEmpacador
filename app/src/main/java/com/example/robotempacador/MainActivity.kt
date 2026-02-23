package com.example.robotempacador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- CLASES DE DATOS (ESTADO DEL JUEGO) ---
data class Position(val x: Int, val y: Int)

data class GameState(
    val robotPos: Position = Position(0, 4),
    val boxPos: Position = Position(2, 2),
    val targetPos: Position = Position(4, 0),
    val hasBox: Boolean = false,
    val commands: List<String> = emptyList(),
    val isExecuting: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFE0F7FA)
                ) { innerPadding ->
                    var gameState by remember { mutableStateOf(GameState()) }
                    val scope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                            },
                            isExecuting = gameState.isExecuting,
                            onExecute = {
                                scope.launch {
                                    gameState = gameState.copy(isExecuting = true)
                                    val commandsToRun = gameState.commands
                                    for (cmd in commandsToRun) {
                                        val currentPos = gameState.robotPos
                                        var newPos = currentPos
                                        when (cmd) {
                                            "ARRIBA" -> if (currentPos.y > 0) newPos = currentPos.copy(y = currentPos.y - 1)
                                            "ABAJO" -> if (currentPos.y < 4) newPos = currentPos.copy(y = currentPos.y + 1)
                                            "IZQUIERDA" -> if (currentPos.x > 0) newPos = currentPos.copy(x = currentPos.x - 1)
                                            "DERECHA" -> if (currentPos.x < 4) newPos = currentPos.copy(x = currentPos.x + 1)
                                        }
                                        gameState = gameState.copy(robotPos = newPos)
                                        delay(500L)
                                    }
                                    gameState = gameState.copy(isExecuting = false, commands = emptyList())
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
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
fun ControlPanel(
    onCommandAdd: (String) -> Unit,
    onClear: () -> Unit,
    isExecuting: Boolean,
    onExecute: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Cruz de direcciones
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ControlButton(Icons.Default.KeyboardArrowUp, "ARRIBA", !isExecuting) { onCommandAdd("ARRIBA") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "IZQUIERDA", !isExecuting) { onCommandAdd("IZQUIERDA") }
                Spacer(modifier = Modifier.width(48.dp))
                ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "DERECHA", !isExecuting) { onCommandAdd("DERECHA") }
            }
            ControlButton(Icons.Default.KeyboardArrowDown, "ABAJO", !isExecuting) { onCommandAdd("ABAJO") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de acción
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onCommandAdd("TOMAR") },
                enabled = !isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TOMAR 📥")
            }
            Button(
                onClick = { onCommandAdd("DEJAR") },
                enabled = !isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DEJAR 📤")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Botón Ejecutar
        Button(
            onClick = onExecute,
            enabled = !isExecuting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("▶ EJECUTAR GUION", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Botón de Limpiar
        TextButton(onClick = onClear, enabled = !isExecuting) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = if (!isExecuting) Color.Red else Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "BORRAR GUION",
                color = if (!isExecuting) Color.Red else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ControlButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp)
            .background(if (enabled) Color.White else Color.LightGray.copy(alpha = 0.3f), CircleShape)
            .border(2.dp, if (enabled) Color(0xFF2196F3) else Color.Gray, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Color(0xFF2196F3) else Color.Gray,
            modifier = Modifier.size(32.dp)
        )
    }
}
