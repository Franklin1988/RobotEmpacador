package com.example.robotempacador

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- ENUMS Y MODELOS ---
enum class AppScreen { MENU, MODO_CAJAS, MODO_SUMAS, RECORDS }

data class Position(val x: Int, val y: Int)

data class Level(
    val robotPos: Position,
    val boxPos: Position,
    val targetPos: Position,
    val walls: List<Position> = emptyList()
)

val levels = listOf(
    Level(Position(0, 4), Position(2, 2), Position(4, 0)),
    Level(Position(0, 0), Position(2, 2), Position(4, 4)),
    Level(Position(4, 4), Position(2, 2), Position(0, 0), walls = listOf(Position(1, 1), Position(3, 3))),
    Level(Position(2, 4), Position(0, 0), Position(2, 0), walls = listOf(Position(1, 2), Position(2, 2), Position(3, 2))),
    Level(Position(0, 2), Position(4, 2), Position(2, 2), walls = listOf(Position(1, 1), Position(1, 2), Position(1, 3))),
    Level(Position(4, 0), Position(0, 4), Position(2, 2), walls = listOf(Position(2, 0), Position(2, 1), Position(2, 3), Position(2, 4))),
    Level(Position(1, 1), Position(3, 3), Position(0, 0), walls = listOf(Position(0, 1), Position(2, 0))),
    Level(Position(3, 0), Position(1, 4), Position(4, 4), walls = listOf(Position(3, 1), Position(1, 1), Position(3, 3), Position(1, 3))),
    Level(Position(0, 4), Position(4, 0), Position(0, 0), walls = listOf(Position(1, 0), Position(1, 1), Position(1, 2), Position(1, 3))),
    Level(Position(2, 2), Position(0, 0), Position(4, 4), walls = listOf(Position(0, 1), Position(1, 1), Position(3, 1), Position(4, 1), Position(1, 3), Position(2, 3), Position(3, 3)))
)

data class GameState(
    val currentLevelIndex: Int = 0,
    val robotPos: Position = levels[0].robotPos,
    val boxPos: Position = levels[0].boxPos,
    val targetPos: Position = levels[0].targetPos,
    val hasBox: Boolean = false,
    val commands: List<String> = emptyList(),
    val isExecuting: Boolean = false,
    val isVictory: Boolean = false,
    val hasAttempted: Boolean = false,
    val executingCommandIndex: Int? = null,
    val errorMessage: String? = null,
    val secondsElapsed: Int = 0,
    val showLevelSelector: Boolean = false
)

class MainActivity : ComponentActivity() {
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("RobotGame", "Error ToneGenerator", e)
        }
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
            var gameState by remember { mutableStateOf(GameState()) }
            val scope = rememberCoroutineScope()

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE0F7FA)) {
                    when (currentScreen) {
                        AppScreen.MENU -> MainMenuScreen { screen ->
                            if (screen == AppScreen.MODO_CAJAS) {
                                // Resetear juego al entrar
                                val firstLevel = levels[0]
                                gameState = GameState(
                                    currentLevelIndex = 0,
                                    robotPos = firstLevel.robotPos,
                                    boxPos = firstLevel.boxPos,
                                    targetPos = firstLevel.targetPos
                                )
                            }
                            currentScreen = screen
                        }
                        AppScreen.MODO_CAJAS -> GameScreen(
                            gameState = gameState,
                            onStateChange = { gameState = it },
                            onBack = { currentScreen = AppScreen.MENU },
                            toneGenerator = toneGenerator
                        )
                        AppScreen.MODO_SUMAS -> ComingSoonScreen("🔢 Modo Sumas") { currentScreen = AppScreen.MENU }
                        AppScreen.RECORDS -> ComingSoonScreen("🏆 Récords") { currentScreen = AppScreen.MENU }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}

@Composable
fun MainMenuScreen(onNavigate: (AppScreen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🤖 Robot Kids",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        MenuButton("📦 Jugar Modo Cajas", Color(0xFF4CAF50)) { onNavigate(AppScreen.MODO_CAJAS) }
        Spacer(modifier = Modifier.height(20.dp))
        MenuButton("🔢 Jugar Modo Sumas", Color(0xFFFB8C00)) { onNavigate(AppScreen.MODO_SUMAS) }
        Spacer(modifier = Modifier.height(20.dp))
        MenuButton("🏆 Ver Récords", Color(0xFF9C27B0)) { onNavigate(AppScreen.RECORDS) }
    }
}

@Composable
fun MenuButton(text: String, color: Color, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.elevatedButtonElevation(10.dp)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
        Text(text = "¡Próximamente!", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
        Button(onClick = onBack) { Text("Volver al Menú") }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onStateChange: (GameState) -> Unit,
    onBack: () -> Unit,
    toneGenerator: ToneGenerator?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- CRONÓMETRO ---
    LaunchedEffect(gameState.isExecuting, gameState.isVictory) {
        while (!gameState.isVictory) {
            delay(1000L)
            onStateChange(gameState.copy(secondsElapsed = gameState.secondsElapsed + 1))
        }
    }

    if (gameState.showLevelSelector) {
        LevelSelectorScreen(
            onLevelSelected = { index ->
                val level = levels[index]
                onStateChange(GameState(
                    currentLevelIndex = index,
                    robotPos = level.robotPos,
                    boxPos = level.boxPos,
                    targetPos = level.targetPos,
                    showLevelSelector = false
                ))
            },
            onClose = { onStateChange(gameState.copy(showLevelSelector = false)) }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CABECERA CON VOLVER Y TIEMPO
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Home, contentDescription = "Menú", tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
                }
                
                Text(
                    text = "🌟 Nivel ${gameState.currentLevelIndex + 1} 🌟",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onStateChange(gameState.copy(showLevelSelector = true)) }
                )

                val minutes = gameState.secondsElapsed / 60
                val seconds = gameState.secondsElapsed % 60
                Text(
                    text = "⏱️ %02d:%02d".format(minutes, seconds),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE91E63)
                )
            }

            Text(text = "🤖 Robot Kids Pro", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF2196F3))

            if (gameState.errorMessage != null) {
                Text(text = gameState.errorMessage!!, color = Color.Red, fontWeight = FontWeight.Bold)
            }

            if (gameState.isVictory) {
                Button(
                    onClick = {
                        val nextIndex = (gameState.currentLevelIndex + 1) % levels.size
                        val nextLevel = levels[nextIndex]
                        onStateChange(GameState(
                            currentLevelIndex = nextIndex,
                            robotPos = nextLevel.robotPos,
                            boxPos = nextLevel.boxPos,
                            targetPos = nextLevel.targetPos
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Text(text = if (gameState.currentLevelIndex < 9) "¡SIGUIENTE NIVEL! 🚀" else "¡REINICIAR! 🔄", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f, fill = false)) { GameBoard(state = gameState) }
            Spacer(modifier = Modifier.height(8.dp))
            CommandList(commands = gameState.commands, executingIndex = gameState.executingCommandIndex)
            Spacer(modifier = Modifier.height(8.dp))

            // EJECUTAR
            Button(
                onClick = {
                    if (gameState.hasAttempted || gameState.errorMessage != null) {
                        val currentLevel = levels[gameState.currentLevelIndex]
                        onStateChange(gameState.copy(
                            commands = emptyList(),
                            robotPos = currentLevel.robotPos,
                            boxPos = currentLevel.boxPos,
                            hasBox = false,
                            isVictory = false,
                            hasAttempted = false,
                            executingCommandIndex = null,
                            errorMessage = null
                        ))
                    } else {
                        scope.launch {
                            val currentLevel = levels[gameState.currentLevelIndex]
                            onStateChange(gameState.copy(isExecuting = true, executingCommandIndex = 0, robotPos = currentLevel.robotPos, boxPos = currentLevel.boxPos, hasBox = false, isVictory = false, errorMessage = null))
                            
                            val commandsToRun = gameState.commands
                            var currentError: String? = null

                            for ((index, cmd) in commandsToRun.withIndex()) {
                                onStateChange(gameState.copy(executingCommandIndex = index))
                                val currentPos = gameState.robotPos
                                var newPos = currentPos
                                var newHasBox = gameState.hasBox
                                var newBoxPos = gameState.boxPos
                                var moveSuccess = false
                                var actionSuccess = false

                                when (cmd) {
                                    "ARRIBA" -> if (currentPos.y > 0 && !currentLevel.walls.contains(currentPos.copy(y = currentPos.y - 1))) { newPos = currentPos.copy(y = currentPos.y - 1); moveSuccess = true } else currentError = "¡Bip bop! Muro."
                                    "ABAJO" -> if (currentPos.y < 4 && !currentLevel.walls.contains(currentPos.copy(y = currentPos.y + 1))) { newPos = currentPos.copy(y = currentPos.y + 1); moveSuccess = true } else currentError = "¡Bip bop! Muro."
                                    "IZQUIERDA" -> if (currentPos.x > 0 && !currentLevel.walls.contains(currentPos.copy(x = currentPos.x - 1))) { newPos = currentPos.copy(x = currentPos.x - 1); moveSuccess = true } else currentError = "¡Bip bop! Muro."
                                    "DERECHA" -> if (currentPos.x < 4 && !currentLevel.walls.contains(currentPos.copy(x = currentPos.x + 1))) { newPos = currentPos.copy(x = currentPos.x + 1); moveSuccess = true } else currentError = "¡Bip bop! Muro."
                                    "TOMAR" -> if (currentPos == gameState.boxPos && !gameState.hasBox) { newHasBox = true; actionSuccess = true } else currentError = "No hay nada aquí."
                                    "DEJAR" -> if (gameState.hasBox) { newHasBox = false; newBoxPos = currentPos; actionSuccess = true } else currentError = "No tengo caja."
                                }

                                if (currentError != null) {
                                    playEffect(context, toneGenerator, true)
                                    onStateChange(gameState.copy(errorMessage = currentError, isExecuting = false, hasAttempted = true, executingCommandIndex = null))
                                    break
                                }

                                playEffect(context, toneGenerator, false)
                                if (newHasBox && newPos != currentPos) newBoxPos = newPos
                                onStateChange(gameState.copy(robotPos = newPos, hasBox = newHasBox, boxPos = newBoxPos))
                                delay(800L)
                            }

                            if (currentError == null) {
                                val victory = gameState.boxPos == gameState.targetPos && !gameState.hasBox
                                if (victory) playVictory(context, toneGenerator)
                                onStateChange(gameState.copy(isExecuting = false, isVictory = victory, errorMessage = if (victory) null else "¡A medias!", hasAttempted = true, executingCommandIndex = null))
                            }
                        }
                    }
                },
                enabled = !gameState.isExecuting && !gameState.isVictory && (gameState.commands.isNotEmpty() || gameState.hasAttempted || gameState.errorMessage != null),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (gameState.hasAttempted || gameState.errorMessage != null) Color.Red else Color(0xFF2196F3)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (gameState.hasAttempted || gameState.errorMessage != null) "🔄 VOLVER A INTENTAR" else "▶ ¡EMPEZAR!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            ControlPanel(
                onCommandAdd = { cmd -> if (!gameState.isExecuting && !gameState.isVictory && !gameState.hasAttempted && gameState.errorMessage == null) onStateChange(gameState.copy(commands = gameState.commands + cmd)) },
                onClear = { val level = levels[gameState.currentLevelIndex]; onStateChange(gameState.copy(commands = emptyList(), robotPos = level.robotPos, boxPos = level.boxPos, hasBox = false, isVictory = false, hasAttempted = false, executingCommandIndex = null, errorMessage = null, secondsElapsed = 0)) },
                isExecuting = gameState.isExecuting,
                isVictory = gameState.isVictory,
                hasAttempted = gameState.hasAttempted || gameState.errorMessage != null
            )
        }
    }
}

// --- FUNCIONES AUXILIARES DE EFECTOS ---
fun playEffect(context: Context, tg: ToneGenerator?, isError: Boolean) {
    tg?.startTone(if (isError) ToneGenerator.TONE_CDMA_ABBR_ALERT else ToneGenerator.TONE_PROP_BEEP, 100)
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    v.vibrate(VibrationEffect.createOneShot(if (isError) 200 else 50, VibrationEffect.DEFAULT_AMPLITUDE))
}

fun playVictory(context: Context, tg: ToneGenerator?) {
    tg?.startTone(ToneGenerator.TONE_DTMF_9, 300)
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
}

@Composable
fun LevelSelectorScreen(
    onLevelSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Seleccionar Nivel",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.width(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(levels) { index, _ ->
                    Button(
                        onClick = { onLevelSelected(index) },
                        modifier = Modifier.size(45.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(state: GameState) {
    val level = levels[state.currentLevelIndex]
    Column(
        modifier = Modifier.padding(8.dp).border(2.dp, Color.Gray, RoundedCornerShape(8.dp)).background(Color.White)
    ) {
        for (y in 0 until 5) {
            Row {
                for (x in 0 until 5) {
                    val pos = Position(x, y)
                    Box(
                        modifier = Modifier.size(55.dp).border(0.5.dp, Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (level.walls.contains(pos)) Box(Modifier.fillMaxSize().background(Color.DarkGray))
                        if (level.targetPos == pos) Text("🏁", fontSize = 20.sp)
                        if (state.boxPos == pos && !state.hasBox) Text("📦", fontSize = 26.sp)
                        if (state.robotPos == pos) Text(if (state.hasBox) "🤖📦" else "🤖", fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CommandList(commands: List<String>, executingIndex: Int?) {
    val listState = rememberLazyListState()
    LaunchedEffect(commands.size) { if (commands.isNotEmpty()) listState.animateScrollToItem(commands.size - 1) }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(55.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(commands) { index, cmd ->
            val isExecuting = executingIndex == index
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isExecuting) Color.Yellow else Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Text(
                    text = when(cmd) {
                        "ARRIBA" -> "⬆️"
                        "ABAJO" -> "⬇️"
                        "IZQUIERDA" -> "⬅️"
                        "DERECHA" -> "➡️"
                        "TOMAR" -> "🖐️"
                        "DEJAR" -> "📦"
                        else -> cmd
                    },
                    modifier = Modifier.padding(8.dp),
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ControlPanel(
    onCommandAdd: (String) -> Unit,
    onClear: () -> Unit,
    isExecuting: Boolean,
    isVictory: Boolean,
    hasAttempted: Boolean
) {
    val disabled = isExecuting || isVictory || hasAttempted
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("⬆️", "ARRIBA", onCommandAdd, disabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("⬅️", "IZQUIERDA", onCommandAdd, disabled)
            ControlButton("🖐️", "TOMAR", onCommandAdd, disabled, Color(0xFFBBDEFB))
            ControlButton("➡️", "DERECHA", onCommandAdd, disabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("🗑️", "CLEAR", { onClear() }, isExecuting || isVictory, Color(0xFFFFCCBC))
            ControlButton("⬇️", "ABAJO", onCommandAdd, disabled)
            ControlButton("📦", "DEJAR", onCommandAdd, disabled, Color(0xFFC8E6C9))
        }
    }
}

@Composable
fun ControlButton(icon: String, cmd: String, onClick: (String) -> Unit, disabled: Boolean, color: Color = Color.White) {
    Button(
        onClick = { onClick(cmd) },
        enabled = !disabled,
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(icon, fontSize = 24.sp)
    }
}
