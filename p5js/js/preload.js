const { ipcRenderer } = require("electron");

window.pde = {
  sendMessage: (message) => ipcRenderer.send("send-message", message)
};

// Force-disable security warning
// https://www.electronjs.org/docs/latest/tutorial/security#:~:text=ELECTRON_DISABLE_SECURITY_WARNINGS
window.ELECTRON_DISABLE_SECURITY_WARNINGS = true;