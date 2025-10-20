class ColorHelper {
    static getColorVector(c) {
        return createVector(red(c), green(c), blue(c));
    }
    static rainbowColorBase() {
        return [
            color('red'),
            color('orange'),
            color('yellow'),
            color('green'),
            color(38, 58, 150),
            color('indigo'),
            color('violet')
        ];
    }
    static getColorsArray(total, baseColorArray = null) {
        if (baseColorArray == null) {
            baseColorArray = ColorHelper.rainbowColorBase();
        }
        var rainbowColors = baseColorArray.map(x => this.getColorVector(x));
        ;
        let colours = new Array();
        for (var i = 0; i < total; i++) {
            var colorPosition = i / total;
            var scaledColorPosition = colorPosition * (rainbowColors.length - 1);
            var colorIndex = Math.floor(scaledColorPosition);
            var colorPercentage = scaledColorPosition - colorIndex;
            var nameColor = this.getColorByPercentage(rainbowColors[colorIndex], rainbowColors[colorIndex + 1], colorPercentage);
            colours.push(color(nameColor.x, nameColor.y, nameColor.z));
        }
        return colours;
    }
    static getColorByPercentage(firstColor, secondColor, percentage) {
        var firstColorCopy = firstColor.copy();
        var secondColorCopy = secondColor.copy();
        var deltaColor = secondColorCopy.sub(firstColorCopy);
        var scaledDeltaColor = deltaColor.mult(percentage);
        return firstColorCopy.add(scaledDeltaColor);
    }
}
class PolygonHelper {
    static draw(numberOfSides, width) {
        push();
        const angle = TWO_PI / numberOfSides;
        const radius = width / 2;
        beginShape();
        for (let a = 0; a < TWO_PI; a += angle) {
            let sx = cos(a) * radius;
            let sy = sin(a) * radius;
            vertex(sx, sy);
        }
        endShape(CLOSE);
        pop();
    }
}
let numberOfShapesControl;
function setup() {
    console.log("🚀 - Setup initialized - P5 is running");
    createCanvas(windowWidth, windowHeight);
    rectMode(CENTER).noFill().frameRate(30);
    numberOfShapesControl = createSlider(1, 30, 15, 1).position(10, 10).style("width", "100px");
}
function windowResized() {
    resizeCanvas(windowWidth, windowHeight);
}
function draw() {
    background(0);
    translate(width / 2, height / 2);
    const numberOfShapes = numberOfShapesControl.value();
    const colours = ColorHelper.getColorsArray(numberOfShapes);
    const speed = (frameCount / (numberOfShapes * 30)) * 2;
    for (var i = 0; i < numberOfShapes; i++) {
        push();
        const lineWidth = 8;
        const spin = speed * (numberOfShapes - i);
        const numberOfSides = 3 + i;
        const width = 40 * i;
        strokeWeight(lineWidth);
        stroke(colours[i]);
        rotate(spin);
        PolygonHelper.draw(numberOfSides, width);
        pop();
    }
}
//# sourceMappingURL=TypeScript.js.map