package com.example.robotempacador

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val showLevelSelector: Boolean = false
)

// --- MODELOS MODO SUMAS ---
data class MathLevel(
    val robotPos: Position,
    val targetPos: Position,
    val targetSum: Int,
    val numbers: Map<Position, Int>,
    val walls: List<Position> = emptyList()
)

val mathLevels = listOf(
    MathLevel(Position(0, 4), Position(4, 0), targetSum = 10, numbers = mapOf(Position(2, 2) to 5, Position(2, 4) to 3, Position(4, 2) to 2)),
    MathLevel(Position(0, 0), Position(4, 4), targetSum = 7, numbers = mapOf(Position(2, 2) to 4, Position(0, 2) to 3, Position(4, 0) to 1), walls = listOf(Position(1, 1))),
    MathLevel(Position(4, 4), Position(0, 0), targetSum = 15, numbers = mapOf(Position(2, 2) to 8, Position(0, 2) to 7, Position(2, 0) to 5), walls = listOf(Position(3, 3)))
)

data class MathGameState(
    val currentLevelIndex: Int = 0,
    val robotPos: Position = mathLevels[0].robotPos,
    val currentSum: Int = 0,
    val pickedPositions: List<Position> = emptyList(),
    val commands: List<String> = emptyList(),
    val isExecuting: Boolean = false,
    val isVictory: Boolean = false,
    val hasAttempted: Boolean = false,
    val executingCommandIndex: Int? = null,
    val errorMessage: String? = null,
    val showLevelSelector: Boolean = false
)

// --- GESTOR DE RÉCORDS ---
class RecordManager(context: Context) {
    private val prefs = context.getSharedPreferences("robot_kids_records", Context.MODE_PRIVATE)

    fun saveBestTime(levelIndex: Int, timeInSeconds: Int) {
        val currentBest = getBestTime(levelIndex)
        if (currentBest == null || timeInSeconds < currentBest) {
            prefs.edit().putInt("cajas_level_$levelIndex", timeInSeconds).apply()
        }
    }

    fun getBestTime(levelIndex: Int): Int? {
        val time = prefs.getInt("cajas_level_$levelIndex", -1)
        return if (time == -1) null else time
    }
}

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
            val recordManager = remember { RecordManager(context) }
            var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
            var gameState by remember { mutableStateOf(GameState()) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE0F7FA)) {
                    when (currentScreen) {
                        AppScreen.MENU -> MainMenuScreen { screen ->
                            if (screen == AppScreen.MODO_CAJAS) {
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
                            toneGenerator = toneGenerator,
                            recordManager = recordManager
                        )
                        AppScreen.MODO_SUMAS -> MathGameScreen(
                            onBack = { currentScreen = AppScreen.MENU },
                            toneGenerator = toneGenerator
                        )
                        AppScreen.RECORDS -> RecordsScreen(recordManager) { currentScreen = AppScreen.MENU }
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
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
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
fun MathGameScreen(onBack: () -> Unit, toneGenerator: ToneGenerator?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mathState by remember { mutableStateOf(MathGameState()) }
    val currentMathLevel = mathLevels[mathState.currentLevelIndex]
    
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CABECERA
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Home, contentDescription = "Menú", tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
            }
            
            Text(
                text = "🌟 Nivel ${mathState.currentLevelIndex + 1} 🌟",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(32.dp))
        }

        // BANNER COLORIDO
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD600)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                text = "🔋 Necesito una suma de: ${currentMathLevel.targetSum} 🔋",
                modifier = Modifier.padding(12.dp).align(Alignment.CenterHorizontally),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
        
        Text(
            text = "🎒 Mochila actual: ${mathState.currentSum}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (mathState.errorMessage != null) {
            Text(text = mathState.errorMessage!!, color = Color.Red, fontWeight = FontWeight.Bold)
        }

        if (mathState.isVictory) {
            Button(
                onClick = {
                    val nextIndex = (mathState.currentLevelIndex + 1) % mathLevels.size
                    val nextLevel = mathLevels[nextIndex]
                    mathState = MathGameState(
                        currentLevelIndex = nextIndex,
                        robotPos = nextLevel.robotPos
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth().height(45.dp)
            ) {
                Text(text = if (mathState.currentLevelIndex < mathLevels.size - 1) "¡SIGUIENTE TESORO! 💎" else "¡REINICIAR! 🔄", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f, fill = false)) { MathGameBoard(state = mathState) }
        Spacer(modifier = Modifier.height(8.dp))
        CommandList(commands = mathState.commands, executingCommandIndex = mathState.executingCommandIndex)
        Spacer(modifier = Modifier.height(8.dp))

        // EJECUTAR
        Button(
            onClick = {
                if (mathState.hasAttempted || mathState.errorMessage != null) {
                    val currentLevel = mathLevels[mathState.currentLevelIndex]
                    mathState = mathState.copy(
                        commands = emptyList(),
                        robotPos = currentLevel.robotPos,
                        currentSum = 0,
                        pickedPositions = emptyList(),
                        isVictory = false,
                        hasAttempted = false,
                        executingCommandIndex = null,
                        errorMessage = null
                    )
                } else {
                    scope.launch {
                        val level = mathLevels[mathState.currentLevelIndex]
                        var currentLocalState = mathState.copy(
                            isExecuting = true, 
                            executingCommandIndex = 0, 
                            robotPos = level.robotPos, 
                            currentSum = 0,
                            pickedPositions = emptyList(),
                            isVictory = false, 
                            errorMessage = null
                        )
                        mathState = currentLocalState
                        
                        val commandsToRun = currentLocalState.commands
                        var currentError: String? = null

                        for ((index, cmd) in commandsToRun.withIndex()) {
                            currentLocalState = currentLocalState.copy(executingCommandIndex = index)
                            mathState = currentLocalState
                            delay(400L)
                            
                            val curPos = currentLocalState.robotPos
                            var nPos = curPos
                            var nSum = currentLocalState.currentSum
                            var nPicked = currentLocalState.pickedPositions.toMutableList()
                            
                            when (cmd) {
                                "ARRIBA" -> if (curPos.y > 0 && !level.walls.contains(curPos.copy(y = curPos.y - 1))) nPos = curPos.copy(y = curPos.y - 1) else currentError = "¡Bip bop! Muro."
                                "ABAJO" -> if (curPos.y < 4 && !level.walls.contains(curPos.copy(y = curPos.y + 1))) nPos = curPos.copy(y = curPos.y + 1) else currentError = "¡Bip bop! Muro."
                                "IZQUIERDA" -> if (curPos.x > 0 && !level.walls.contains(curPos.copy(x = curPos.x - 1))) nPos = curPos.copy(x = curPos.x - 1) else currentError = "¡Bip bop! Muro."
                                "DERECHA" -> if (curPos.x < 4 && !level.walls.contains(curPos.copy(x = curPos.x + 1))) nPos = curPos.copy(x = curPos.x + 1) else currentError = "¡Bip bop! Muro."
                                "TOMAR" -> {
                                    val numberAtPos = level.numbers[curPos]
                                    if (numberAtPos != null && !currentLocalState.pickedPositions.contains(curPos)) {
                                        nSum += numberAtPos
                                        nPicked.add(curPos)
                                    } else {
                                        currentError = "No hay números aquí"
                                    }
                                }
                                "DEJAR" -> {
                                    if (curPos == level.targetPos) {
                                        if (nSum == level.targetSum) {
                                            // Victoria se marca abajo
                                        } else {
                                            currentError = "¡Suma incorrecta! Llevas ${nSum} pero necesito ${level.targetSum}"
                                        }
                                    } else {
                                        currentError = "Solo puedes entregar en la meta"
                                    }
                                }
                            }

                            if (currentError != null) {
                                playEffect(context, toneGenerator, true)
                                currentLocalState = currentLocalState.copy(errorMessage = currentError, isExecuting = false, hasAttempted = true, executingCommandIndex = null)
                                mathState = currentLocalState
                                break
                            }

                            playEffect(context, toneGenerator, false)
                            currentLocalState = currentLocalState.copy(robotPos = nPos, currentSum = nSum, pickedPositions = nPicked)
                            mathState = currentLocalState
                            delay(600L)
                        }

                        if (currentError == null) {
                            val victory = currentLocalState.robotPos == level.targetPos && currentLocalState.currentSum == level.targetSum && currentLocalState.commands.last() == "DEJAR"
                            if (victory) {
                                playVictory(context, toneGenerator)
                            }
                            currentLocalState = currentLocalState.copy(
                                isExecuting = false, 
                                isVictory = victory, 
                                errorMessage = if (victory) null else "¡Te faltó entregar en la meta!", 
                                hasAttempted = true, 
                                executingCommandIndex = null
                            )
                            mathState = currentLocalState
                        }
                    }
                }
            },
            enabled = !mathState.isExecuting && !mathState.isVictory && (mathState.commands.isNotEmpty() || mathState.hasAttempted || mathState.errorMessage != null),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (mathState.hasAttempted || mathState.errorMessage != null) Color.Red else Color(0xFFFB8C00)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = if (mathState.hasAttempted || mathState.errorMessage != null) "🔄 VOLVER A INTENTAR" else "▶ ¡EMPEZAR!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
        ControlPanel(
            onCommandAdd = { cmd -> if (!mathState.isExecuting && !mathState.isVictory && !mathState.hasAttempted && mathState.errorMessage == null) mathState = mathState.copy(commands = mathState.commands + cmd) },
            onClear = { val level = mathLevels[mathState.currentLevelIndex]; mathState = mathState.copy(commands = emptyList(), robotPos = level.robotPos, currentSum = 0, pickedPositions = emptyList(), isVictory = false, hasAttempted = false, executingCommandIndex = null, errorMessage = null) },
            isExecuting = mathState.isExecuting,
            isVictory = mathState.isVictory,
            hasAttempted = mathState.hasAttempted || mathState.errorMessage != null
        )
    }
}

@Composable
fun MathGameBoard(state: MathGameState) {
    val level = mathLevels[state.currentLevelIndex]
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
                        if (level.walls.contains(pos)) Text("🧱", fontSize = 26.sp)
                        
                        if (level.targetPos == pos) Text("🔋🏁", fontSize = 20.sp)
                        
                        val numberAtPos = level.numbers[pos]
                        if (numberAtPos != null && !state.pickedPositions.contains(pos)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💎", fontSize = 20.sp)
                                Text(
                                    text = numberAtPos.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD4E157)
                                )
                            }
                        }
                        
                        if (state.robotPos == pos) Text("🤖", fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordsScreen(recordManager: RecordManager, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏆 Récords Modo Cajas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(vertical = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(10) { index ->
                val bestTime = recordManager.getBestTime(index)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Nivel ${index + 1}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (bestTime != null) "${bestTime}s" else "No jugado",
                            fontSize = 18.sp,
                            color = if (bestTime != null) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Volver al Menú")
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onStateChange: (GameState) -> Unit,
    onBack: Unit -> Unit,
    toneGenerator: ToneGenerator?,
    recordManager: RecordManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var secondsElapsed by remember(gameState.currentLevelIndex) { mutableIntStateOf(0) }
    val bestTime = remember(gameState.currentLevelIndex, gameState.isVictory) { 
        recordManager.getBestTime(gameState.currentLevelIndex) 
    }

    LaunchedEffect(gameState.isExecuting, gameState.isVictory) {
        if (gameState.isExecuting && !gameState.isVictory) {
            while (gameState.isExecuting && !gameState.isVictory) {
                delay(1000L)
                secondsElapsed++
            }
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
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Home, contentDescription = "Menú", tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌟 Nivel ${gameState.currentLevelIndex + 1} 🌟",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onStateChange(gameState.copy(showLevelSelector = true)) }
                    )
                    Text(
                        text = "🏆 Mejor: ${bestTime?.let { "${it}s" } ?: "--"}",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                val minutes = secondsElapsed / 60
                val seconds = secondsElapsed % 60
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
            CommandList(commands = gameState.commands, executingCommandIndex = gameState.executingCommandIndex)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (gameState.hasAttempted || gameState.errorMessage != null) {
                        secondsElapsed = 0
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
                            var currentLocalState = gameState.copy(
                                isExecuting = true, 
                                executingCommandIndex = 0, 
                                robotPos = currentLevel.robotPos, 
                                boxPos = currentLevel.boxPos, 
                                hasBox = false, 
                                isVictory = false, 
                                errorMessage = null
                            )
                            onStateChange(currentLocalState)
                            
                            val commandsToRun = currentLocalState.commands
                            var currentError: String? = null

                            for ((index, cmd) in commandsToRun.withIndex()) {
                                currentLocalState = currentLocalState.copy(executingCommandIndex = index)
                                onStateChange(currentLocalState)
                                delay(400L)
                                
                                val currentPos = currentLocalState.robotPos
                                var newPos = currentPos
                                var newHasBox = currentLocalState.hasBox
                                var newBoxPos = currentLocalState.boxPos
                                
                                when (cmd) {
                                    "ARRIBA" -> if (currentPos.y > 0 && !currentLevel.walls.contains(currentPos.copy(y = currentPos.y - 1))) newPos = currentPos.copy(y = currentPos.y - 1) else currentError = "¡Bip bop! Muro."
                                    "ABAJO" -> if (currentPos.y < 4 && !currentLevel.walls.contains(currentPos.copy(y = currentPos.y + 1))) newPos = currentPos.copy(y = currentPos.y + 1) else currentError = "¡Bip bop! Muro."
                                    "IZQUIERDA" -> if (currentPos.x > 0 && !currentLevel.walls.contains(currentPos.copy(x = currentPos.x - 1))) newPos = currentPos.copy(x = currentPos.x - 1) else currentError = "¡Bip bop! Muro."
                                    "DERECHA" -> if (currentPos.x < 4 && !currentLevel.walls.contains(currentPos.copy(x = currentPos.x + 1))) newPos = currentPos.copy(x = currentPos.x + 1) else currentError = "¡Bip bop! Muro."
                                    "TOMAR" -> if (currentPos == currentLocalState.boxPos && !currentLocalState.hasBox) newHasBox = true else currentError = "No hay nada aquí."
                                    "DEJAR" -> if (currentLocalState.hasBox) { newHasBox = false; newBoxPos = currentPos } else currentError = "No tengo caja."
                                }

                                if (currentError != null) {
                                    playEffect(context, toneGenerator, true)
                                    currentLocalState = currentLocalState.copy(errorMessage = currentError, isExecuting = false, hasAttempted = true, executingCommandIndex = null)
                                    onStateChange(currentLocalState)
                                    break
                                }

                                playEffect(context, toneGenerator, false)
                                if (newHasBox && newPos != currentPos) newBoxPos = newPos
                                currentLocalState = currentLocalState.copy(robotPos = newPos, hasBox = newHasBox, boxPos = newBoxPos)
                                onStateChange(currentLocalState)
                                delay(600L)
                            }

                            if (currentError == null) {
                                val victory = currentLocalState.boxPos == currentLocalState.targetPos && !currentLocalState.hasBox
                                if (victory) {
                                    playVictory(context, toneGenerator)
                                    recordManager.saveBestTime(currentLocalState.currentLevelIndex, secondsElapsed)
                                }
                                currentLocalState = currentLocalState.copy(
                                    isExecuting = false, 
                                    isVictory = victory, 
                                    errorMessage = if (victory) null else "¡A medias!", 
                                    hasAttempted = true, 
                                    executingCommandIndex = null
                                )
                                onStateChange(currentLocalState)
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
                onClear = { val level = levels[gameState.currentLevelIndex]; secondsElapsed = 0; onStateChange(gameState.copy(commands = emptyList(), robotPos = level.robotPos, boxPos = level.boxPos, hasBox = false, isVictory = false, hasAttempted = false, executingCommandIndex = null, errorMessage = null)) },
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(if (isError) 200 else 50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(if (isError) 200 else 50)
    }
}

fun playVictory(context: Context, tg: ToneGenerator?) {
    tg?.startTone(ToneGenerator.TONE_DTMF_9, 300)
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(longArrayOf(0, 100, 50, 100), -1)
    }
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
                        if (level.walls.contains(pos)) Text("🧱", fontSize = 26.sp)
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
fun CommandList(commands: List<String>, executingCommandIndex: Int?) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(executingCommandIndex) {
        if (executingCommandIndex == 0) {
            listState.scrollToItem(0)
        } else if (executingCommandIndex != null) {
            listState.animateScrollToItem(executingCommandIndex)
        }
    }

    LaunchedEffect(commands.size) {
        if (commands.isNotEmpty() && executingCommandIndex == null) {
            listState.animateScrollToItem(commands.size - 1)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(55.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(commands) { index, c ->
            Card(colors = CardDefaults.cardColors(containerColor = if (executingCommandIndex == index) Color.Yellow else Color.White)) {
                Text(when(c){"ARRIBA"->"⬆️";"ABAJO"->"⬇️";"IZQUIERDA"->"⬅️";"DERECHA"->"➡️";"TOMAR"->"🖐️";"DEJAR"->"📦";else->c}, modifier = Modifier.padding(8.dp))
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
