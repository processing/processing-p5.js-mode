// TODO Beware of leakage into global scope!
const fs = require("node:fs/promises");
const p = require("node:path");

// const sharp = require("sharp");

let suffix = 0;

async function saveCnv(path) {
    const canvas = document.querySelector(".p5Canvas");
    const outPath = p.resolve(path, `out-${suffix.toString().padStart(5, "0")}.png`);

    canvas.toBlob(async (blob) => {
        const buffer = Buffer.from(await blob.arrayBuffer());
        // await sharp(buffer).rotate(45).grayscale().toFile(outPath);
        await fs.writeFile(outPath, buffer);
        suffix++;
    });
}