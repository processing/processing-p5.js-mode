const targetNode = document.querySelector("body");
const config = { childList: true };

const observer = new MutationObserver((mutationList) => {
    // Once p5 is ready, it adds a <main> node containing the drawing canvas
    const p5Ready = mutationList.filter(
        mutation => mutation.addedNodes[0]?.nodeName === "MAIN"
    ).length > 0;
    if (p5Ready) {
        const { width, height } = document.querySelector("canvas");
        pde.resize({
            width,
            height
        });
    }
});

observer.observe(targetNode, config);

