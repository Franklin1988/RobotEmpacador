var gameMode = 'basic';
var currentLevelIdx = 0;
var isRunning = false;
var commandQueue = [];
var timerInterval;
var startTime;

function startMode(mode) {
    gameMode = mode;
    document.getElementById('main-menu').style.display = 'none';

    // 1. Encendemos la luz y mostramos la pantalla
    document.getElementById('game-screen').style.display = 'flex';

    // 2. Le damos un "respiro" de 50 milisegundos al navegador para que
    // termine de dibujar todo antes de llamar a nuestro pintor (Phaser)
    setTimeout(function() {
        if (window.iniciarMotorDeJuego) {
            window.iniciarMotorDeJuego();
        }

        if (mode === 'basic') {
            currentLevelIdx = 0;
            // Un pequeño retardo adicional para que la escena de Phaser se cree
            setTimeout(function() {
                loadLevel(currentLevelIdx);
            }, 200);
        }
    }, 50); // <-- Esos 50 son los milisegundos de espera
}

function goToMenu() {
    stopTimer();
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('records-screen').style.display = 'none';
    document.getElementById('victory-screen').style.display = 'none';
    document.getElementById('main-menu').style.display = 'block';
    resetGameState();
}

function resetGameState() {
    isRunning = false;
    commandQueue = [];
    document.getElementById('commands-list').innerHTML = '';
    document.getElementById('message').innerText = '';
    document.getElementById('runButton').style.display = 'block';
    document.getElementById('nextLevelBtn').style.display = 'none';
}

function startTimer() {
    startTime = Date.now();
    timerInterval = setInterval(function() {
        var elapsed = (Date.now() - startTime) / 1000;
        document.getElementById('timer').innerText = elapsed.toFixed(1) + 's';
    }, 100);
}

function stopTimer() {
    clearInterval(timerInterval);
}

function restartFullGame() {
    if (gameMode === 'basic') loadLevel(currentLevelIdx);
}
