const fs = require('fs');

let version
function setup() {
    createCanvas(400, 400);

    // Read a file (for example, package.json)
    fs.readFile('package.json', 'utf8', (err, data) => {
        if (err) {
            console.error('Error reading file:', err);
            return;
        }
        var json = JSON.parse(data)
        version = json.version;
    });
}

function draw() {
    background(220);

    textAlign(CENTER)
    text(`I'm version ${version}`, width / 2, height / 2)
}
