console.log("EdgeViewer Web Loaded");

// UI elements
const img = document.getElementById("edgeImage");
const fpsLabel = document.getElementById("fps");
const resLabel = document.getElementById("resolution");

// Load sample processed frame
img.src = "edge_sample.png";

// FPS simulation
let frameCount = 0;
let lastTime = performance.now();

function updateStats() {
    frameCount++;
    const now = performance.now();
    const delta = now - lastTime;

    if (delta >= 1000) {
        const fps = Math.round((frameCount * 1000) / delta);
        fpsLabel.textContent = `FPS: ${fps}`;
        frameCount = 0;
        lastTime = now;
    }

    // update resolution text
    resLabel.textContent = `Resolution: ${img.naturalWidth}x${img.naturalHeight}`;

    requestAnimationFrame(updateStats);
}

updateStats();
