var config = {
    type: Phaser.AUTO,
    parent: 'phaser-game-container',
    width: 400,
    height: 400,
    backgroundColor: '#B0BEC5',
    scene: { create: create }
};

// --- VARIABLES GLOBALES PARA PHASER ---
var game = new Phaser.Game(config);
var gridSize = 80;

// Hacemos que la escena y los objetos del juego sean accesibles desde cualquier script
window.phaserScene = null;
window.robot = null;
window.box = null;
window.target = null;


function create() {
    // Asignamos la escena actual a la variable global para poder usarla después
    window.phaserScene = this;

    // Dibujar la cuadrícula de fondo
    let graphics = this.add.graphics();
    graphics.lineStyle(2, 0xffffff, 0.5);
    for (let i = 0; i <= 5; i++) {
        graphics.moveTo(i * gridSize, 0).lineTo(i * gridSize, 400);
        graphics.moveTo(0, i * gridSize).lineTo(400, i * gridSize);
    }
    graphics.strokePath();

    // Creamos los objetos y los asignamos a las variables globales
    window.target = this.add.text(0, 0, '🏁', { fontSize: '50px' }).setOrigin(0.5);
    window.box = this.add.text(0, 0, '📦', { fontSize: '50px' }).setOrigin(0.5);
    window.robot = this.add.text(0, 0, '🤖', { fontSize: '60px' }).setOrigin(0.5);

    // Los ocultamos por defecto hasta que se cargue un nivel
    window.target.setVisible(false);
    window.box.setVisible(false);
    window.robot.setVisible(false);
}

// --- FUNCIONES GLOBALES PARA CONTROLAR LOS SPRITES ---

// Mueve el sprite del robot suavemente a una nueva celda
window.moveRobotVisual = function(gridX, gridY) {
    if (!window.phaserScene || !window.robot) return;
    window.phaserScene.tweens.add({
        targets: window.robot,
        x: (gridX * gridSize) + (gridSize / 2),
        y: (gridY * gridSize) + (gridSize / 2),
        duration: 400,
        ease: 'Power2'
    });
};

// Coloca un objeto en una celda de la cuadrícula y lo hace visible
window.updateElementPos = function(element, gridX, gridY) {
    if (!element) return;
    element.x = (gridX * gridSize) + (gridSize / 2);
    element.y = (gridY * gridSize) + (gridSize / 2);
    element.setVisible(true);
};