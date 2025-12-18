class ColorHelper {
    private static getColorVector(c: p5.Color) {
        return createVector(
            red(c),
            green(c),
            blue(c)
        );
    }

    public static rainbowColorBase() {
        return [
            color('red'),
            color('orange'),
            color('yellow'),
            color('green'),
            color(38, 58, 150), // blue
            color('indigo'),
            color('violet')
        ];
    }

    public static getColorsArray(total: number, baseColorArray: p5.Color[] = null): p5.Color[] {

        if (baseColorArray == null) {
            baseColorArray = ColorHelper.rainbowColorBase();
        }
        var rainbowColors = baseColorArray.map(x => this.getColorVector(x));;

        let colours = new Array<p5.Color>();
        for (var i = 0; i < total; i++) {
            var colorPosition = i / total;
            var scaledColorPosition = colorPosition * (rainbowColors.length - 1);

            var colorIndex = Math.floor(scaledColorPosition);
            var colorPercentage = scaledColorPosition - colorIndex;

            var nameColor = this.getColorByPercentage(rainbowColors[colorIndex],
                rainbowColors[colorIndex + 1],
                colorPercentage);

            colours.push(color(nameColor.x, nameColor.y, nameColor.z))
        }

        return colours;
    }

    private static getColorByPercentage(firstColor: p5.Vector, secondColor: p5.Vector, percentage: number) {
        // assumes colors are p5js vectors
        var firstColorCopy = firstColor.copy();
        var secondColorCopy = secondColor.copy();

        var deltaColor = secondColorCopy.sub(firstColorCopy);
        var scaledDeltaColor = deltaColor.mult(percentage);
        return firstColorCopy.add(scaledDeltaColor);
    }
}

class PolygonHelper {
  public static draw(numberOfSides: number, width: number) {
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

// GLOBAL VARS & TYPES
let numberOfShapesControl: p5.Element;

// P5 WILL AUTOMATICALLY USE GLOBAL MODE IF A DRAW() FUNCTION IS DEFINED
function setup() {
  createCanvas(windowWidth, windowHeight)
  rectMode(CENTER).noFill().frameRate(30);
  // NUMBER OF SHAPES SLIDER
  numberOfShapesControl = createSlider(1, 30, 15, 1).position(10, 10).style("width", "100px");
}

// p5 WILL AUTO RUN THIS FUNCTION IF THE BROWSER WINDOW SIZE CHANGES
function windowResized() {
  resizeCanvas(windowWidth, windowHeight);
}

// p5 WILL HANDLE REQUESTING ANIMATION FRAMES FROM THE BROWSER AND WIL RUN DRAW() EACH ANIMATION FROME
function draw() {

   // CLEAR BACKGROUND
  background(0);

  // CENTER OF SCREEN
  translate(width / 2,height / 2);

  const numberOfShapes = <number>numberOfShapesControl.value();
  const colours = ColorHelper.getColorsArray(numberOfShapes);

  // CONSISTENT SPEED REGARDLESS OF FRAMERATE
  const speed = (frameCount / (numberOfShapes * 30)) * 2;

  // DRAW ALL SHAPES
  for (var i = 0; i < numberOfShapes; i++) {
    push();
      const lineWidth = 8;
      const spin = speed * (numberOfShapes - i);
      const numberOfSides = 3 + i;
      const width = 40 * i;
      strokeWeight(lineWidth); 
      stroke(colours[i]);
      rotate(spin);
      PolygonHelper.draw(numberOfSides, width)
    pop();
  }
}
