var basicLevels = [
    { robotPos: {x: 0, y: 4}, boxPos: {x: 2, y: 2}, targetPos: {x: 4, y: 0} },
    { robotPos: {x: 0, y: 0}, boxPos: {x: 4, y: 4}, targetPos: {x: 0, y: 4} },
    { robotPos: {x: 2, y: 4}, boxPos: {x: 2, y: 0}, targetPos: {x: 4, y: 4} }
];

var currentState = {
    robot: {x: 0, y: 0},
    box: {x: 0, y: 0},
    target: {x: 0, y: 0},
    hasBox: false
};

function loadLevel(idx) {
    currentLevelIdx = idx;
    const level = basicLevels[idx];

    currentState.robot = { ...level.robotPos };
    currentState.box = { ...level.boxPos };
    currentState.target = { ...level.targetPos };
    currentState.hasBox = false;

    document.getElementById('level-indicator').innerText = `Nivel ${idx + 1}`;
    resetGameState();

    // Verificamos si Phaser está listo antes de intentar posicionar los elementos
    if (window.phaserScene && window.robot && window.box && window.target) {
        window.updateElementPos(window.robot, currentState.robot.x, currentState.robot.y);
        window.updateElementPos(window.box, currentState.box.x, currentState.box.y);
        window.updateElementPos(window.target, currentState.target.x, currentState.target.y);
        window.robot.setText('🤖');
        window.box.setVisible(true);
    } else {
        // Si no está listo, esperamos un poco y volvemos a intentar
        console.log("Esperando a Phaser...");
        setTimeout(() => loadLevel(idx), 200);
    }
}

function nextLevel() {
    if (currentLevelIdx < basicLevels.length - 1) {
        loadLevel(currentLevelIdx + 1);
    } else {
        document.getElementById('game-screen').style.display = 'none';
        document.getElementById('victory-screen').style.display = 'flex';
    }
}