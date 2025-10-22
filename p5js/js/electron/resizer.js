const targetNode = document.querySelector("body");
const config = { childList: true };

const observer = new MutationObserver((mutationList) => {
    // Once p5 is ready, it adds a <main> node containing the drawing canvas
    const p5Ready = mutationList.filter(
        mutation => mutation.addedNodes[0]?.nodeName === "MAIN"
    ).length > 0;
    if (p5Ready) {
        // Choose CSS over device pixels for proper window size across all screens
        const { style } = document.querySelector("canvas");
        const width = parseInt(style.width);
        const height = parseInt(style.height);
        pde.resize({
            width,
            height
        });
    }
});

observer.observe(targetNode, config);

