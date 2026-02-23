async function addCmd(action, icon) {
    if (isRunning) return;
    commandQueue.push({ action, icon });

    const list = document.getElementById('commands-list');
    const cmdElement = document.createElement('div');
    cmdElement.style.cssText = "background: #E1F5FE; padding: 2px 8px; border-radius: 8px; font-size: 18px; border: 1px solid #B3E5FC;";
    cmdElement.innerText = icon;
    list.appendChild(cmdElement);
}

function resetLevelCommands() {
    if (isRunning) return;
    commandQueue = [];
    document.getElementById('commands-list').innerHTML = '';
    document.getElementById('message').innerText = '';
}

async function runCode() {
    if (isRunning || commandQueue.length === 0) return;
    isRunning = true;
    startTimer();

    document.getElementById('runButton').style.display = 'none';
    const message = document.getElementById('message');
    message.innerText = "🤖 ¡Robot trabajando!";

    for (let cmd of commandQueue) {
        let action = cmd.action;
        let success = true;

        if (action === 'ARRIBA') {
            if (currentState.robot.y > 0) currentState.robot.y--;
            else success = false;
        } else if (action === 'ABAJO') {
            if (currentState.robot.y < 4) currentState.robot.y++;
            else success = false;
        } else if (action === 'IZQUIERDA') {
            if (currentState.robot.x > 0) currentState.robot.x--;
            else success = false;
        } else if (action === 'DERECHA') {
            if (currentState.robot.x < 4) currentState.robot.x++;
            else success = false;
        } else if (action === 'TOMAR') {
            if (currentState.robot.x === currentState.box.x && currentState.robot.y === currentState.box.y && !currentState.hasBox) {
                currentState.hasBox = true;
                window.box.setVisible(false);
                window.robot.setText('🤖📦');
            } else {
                success = false;
            }
        } else if (action === 'DEJAR') {
            if (currentState.hasBox) {
                currentState.hasBox = false;
                currentState.box.x = currentState.robot.x;
                currentState.box.y = currentState.robot.y;
                window.robot.setText('🤖');
                window.updateElementPos(window.box, currentState.box.x, currentState.box.y);
            } else {
                success = false;
            }
        }

        if (!success) {
            message.innerText = "❌ ¡Oops! Algo salió mal";
            stopTimer();
            setTimeout(() => loadLevel(currentLevelIdx), 1500);
            return;
        }

        window.moveRobotVisual(currentState.robot.x, currentState.robot.y);
        await new Promise(r => setTimeout(r, 500));
    }

    checkWin();
}

function checkWin() {
    isRunning = false;
    stopTimer();
    const message = document.getElementById('message');

    if (currentState.box.x === currentState.target.x && currentState.box.y === currentState.target.y && !currentState.hasBox) {
        message.innerHTML = "<span style='color: #4CAF50; font-size: 20px;'>🌟 ¡MUY BIEN ISAAC! 🌟</span>";
        document.getElementById('nextLevelBtn').style.display = 'block';
    } else {
        message.innerText = "📦 Tienes que llevar la caja a la meta";
        setTimeout(() => {
            if (!document.getElementById('nextLevelBtn').style.display || document.getElementById('nextLevelBtn').style.display === 'none') {
                loadLevel(currentLevelIdx);
            }
        }, 2000);
    }
}
