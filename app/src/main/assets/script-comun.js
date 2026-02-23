let gameMode = 'basic';
let currentLevelIdx = 0;
let isRunning = false;
let commandQueue = [];
let timerInterval;
let startTime;

function startMode(mode) {
    gameMode = mode;
    document.getElementById('main-menu').style.display = 'none';
    document.getElementById('game-screen').style.display = 'flex';

    if (mode === 'basic') {
        currentLevelIdx = 0;
        loadLevel(currentLevelIdx);
    }
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
    timerInterval = setInterval(() => {
        const elapsed = (Date.now() - startTime) / 1000;
        document.getElementById('timer').innerText = elapsed.toFixed(1) + 's';
    }, 100);
}

function stopTimer() {
    clearInterval(timerInterval);
}

function restartFullGame() {
    if (gameMode === 'basic') loadLevel(currentLevelIdx);
}
