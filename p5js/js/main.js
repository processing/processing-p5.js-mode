const path = require('node:path');
const { app, BrowserWindow, globalShortcut, ipcMain } = require('electron');

const createWindow = () => {
  const win = new BrowserWindow({
    width: 400,
    height: 400,
    useContentSize: true,
    autoHideMenuBar: true,
    alwaysOnTop: true,
    webPreferences: {
      nodeIntegration: true,
      preload: path.join(__dirname, "preload.js")
    }
  });

  win.loadFile('index.html');

  // Register the 'Escape' key shortcut
  globalShortcut.register('Escape', () => {
    win.close();
  });

  // Unregister the shortcut when window is closed
  win.on('closed', () => {
    globalShortcut.unregister('Escape');
  });
}

app.on('window-all-closed', () => {
  // Unregister all shortcuts when app is closing
  globalShortcut.unregisterAll();
  app.quit();
});

app.whenReady().then(() => {
  ipcMain.on("send-message", (event, message) => {
    console.log(message);
  });
  createWindow();
});
