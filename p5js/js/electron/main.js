const path = require('node:path');
const { app, BrowserWindow, globalShortcut, ipcMain } = require('electron');

const createWindow = () => {
  const win = new BrowserWindow({
    width: 400,
    height: 400,
    show: false,
    autoHideMenuBar: true,
    alwaysOnTop: true,
    webPreferences: {
      contextIsolation: false,
      nodeIntegration: true,
      preload: path.join(__dirname, "preload.js")
    }
  });

  win.loadFile('electron/index.html');

  // Register the 'Escape' key shortcut
  globalShortcut.register('Escape', () => {
    win.close();
  });

  // Unregister the shortcut when window is closed
  win.on('closed', () => {
    globalShortcut.unregister('Escape');
  });

  return win;
}

app.on('window-all-closed', () => {
  // Unregister all shortcuts when app is closing
  globalShortcut.unregisterAll();
  app.quit();
});

app.whenReady().then(() => {
  const win = createWindow();

  ipcMain.on("send-message", (event, message) => {
    console.log(message);
  });

  ipcMain.on("resize", (event, {width, height}) => {
    win.setContentSize(width, height);
    win.show();
  });
});