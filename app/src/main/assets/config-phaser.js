var config = {
    type: Phaser.AUTO,
    parent: 'phaser-game-container',
    width: 400,
    height: 400,
    backgroundColor: '#B0BEC5',
    scale: {
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH
    },
    scene: { create: create }
};

// 1. Iniciamos la variable vacía. Aún no hay juego.
window.game = null;
var gridSize = 80;

// Variables globales para la escena y objetos
window.phaserScene = null;
window.robot = null;
window.box = null;
window.target = null;

// 2. Creamos nuestro "paquete" (función) que llamará al pintor
// Esta función se encarga de iniciar el motor cuando sea necesario
window.iniciarMotorDeJuego = function() {
    // Verificamos si el juego está vacío.
    // Esto evita que creemos varios juegos si el usuario entra y sale del menú.
    if (window.game === null) {
        window.game = new Phaser.Game(config);
    }
};

// Mantenemos el alias para compatibilidad con script-comun.js
window.initPhaserGame = window.iniciarMotorDeJuego;

function create() {
    // Asignamos la escena actual a la variable global
    window.phaserScene = this;

    // Dibujar la cuadrícula de fondo
    var graphics = this.add.graphics();
    graphics.lineStyle(2, 0xffffff, 0.5);
    for (var i = 0; i <= 5; i++) {
        graphics.lineBetween(i * gridSize, 0, i * gridSize, 400);
        graphics.lineBetween(0, i * gridSize, 400, i * gridSize);
    }

    // Creamos los objetos ( emojis )
    window.target = this.add.text(0, 0, '🏁', { fontSize: '50px' }).setOrigin(0.5);
    window.box = this.add.text(0, 0, '📦', { fontSize: '50px' }).setOrigin(0.5);
    window.robot = this.add.text(0, 0, '🤖', { fontSize: '60px' }).setOrigin(0.5);

    // Los ocultamos por defecto hasta que se cargue un nivel
    window.target.setVisible(false);
    window.box.setVisible(false);
    window.robot.setVisible(false);
}

// Funciones de control visual
window.moveRobotVisual = function(gridX, gridY) {
    if (window.phaserScene && window.robot) {
        window.phaserScene.tweens.add({
            targets: window.robot,
            x: (gridX * gridSize) + (gridSize / 2),
            y: (gridY * gridSize) + (gridSize / 2),
            duration: 400,
            ease: 'Power2'
        });
    }
};

window.updateElementPos = function(element, gridX, gridY) {
    if (element) {
        element.x = (gridX * gridSize) + (gridSize / 2);
        element.y = (gridY * gridSize) + (gridSize / 2);
        element.setVisible(true);
    }
};
