let fps = 0;
let last = performance.now();

const img = document.getElementById("edge-image") as HTMLImageElement;
const fpsText = document.getElementById("fps") as HTMLElement;

function loop() {
    const now = performance.now();
    fps = Math.round(1000 / (now - last));
    last = now;

    fpsText.textContent = `FPS: ${fps}`;

    requestAnimationFrame(loop);
}

loop();
