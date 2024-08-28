/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.game;

import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Platform;
import mobileapplication3.platform.ui.Graphics;
import mobileapplication3.platform.ui.RootContainer;
import mobileapplication3.ui.Keys;
import utils.MgStruct;

/**
 *
 * @author vipaol
 */
public class MenuCanvas extends GenericMenu implements Runnable {
	
    private String[] menuOptions = {
    		"",
    		"Play",
    		"Ext Structs",
    		"Levels",
    		"About",
    		"Settings",
    		"Exit",
    		""};

    private static int defaultSelected = 1; // currently selected option in menu
    
    // states
    private boolean isInited = false;
    private boolean waitingToStartGame = false;
    private boolean isGameStarted = false;
    
    private MgStruct mgStruct; // for loading external structures
    
    private static boolean areExtStructsLoaded = false;

    public MenuCanvas() {
        Logger.log("menu:constr");
        // menu initialization
        loadParams(menuOptions, 1, menuOptions.length - 2, defaultSelected, new int[menuOptions.length]);
    }
    
    public void init() {
        Logger.log("menu:init");
        
        if (Logger.isOnScreenLogEnabled()) {
            Logger.enableOnScreenLog(h);
        }
        
        if (areExtStructsLoaded) { // highlight and change label of "Ext Structs" btn if it already loaded
            setStateFor(1, 2);
            menuOptions[2] = "Reload";
        }
        isInited = true;
        (new Thread(this, "menu canvas")).start();
    }
    
    public void run() {
        long sleep = 0; // for FPS/TPS control
        long start = 0; //

        while (!isStopped) { // *** main cycle of menu drawing ***
            if (waitingToStartGame) {
                startGame();
                return;
            }
            if (!isPaused) {
                start = System.currentTimeMillis();
                repaint(); // refresh picture on screen
                sleep = GameplayCanvas.TICK_DURATION - (System.currentTimeMillis() - start);
                sleep = Math.max(sleep, 0);
            } else {
                sleep = 100;
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void paint(Graphics g) {
        g.setColor(0);
        g.fillRect(0, 0, w, h);
        if (isInited) {
            super.paint(g);
            tick();
        }
    }

    public void startGame() {
        if (isGameStarted) {
            return;
        }
        isGameStarted = true;
        waitingToStartGame = false;
        Logger.log("menu:startGame()");
        repaint();
        try {
            isStopped = true;
            log("menu:new gCanvas");
            GameplayCanvas gameCanvas = new GameplayCanvas();
            log("menu:setting gCanvas displayable");
            RootContainer.setRootUIComponent(gameCanvas);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.enableOnScreenLog(h);
            Logger.log("ex in startGame():");
            Logger.log(ex.toString());
            repaint();
        }
    }
    
    public boolean keyPressed(int keyCode, int count) {         // Keyboard
        if (keyCode == Keys.KEY_STAR | keyCode == -10) {
            if (!Logger.isOnScreenLogEnabled()) {
                Logger.enableOnScreenLog(h);
            } else {
                Logger.disableOnScreenLog();
            }
        }
        return super.keyPressed(keyCode, count);
    }
    
    
    void selectPressed() { // Do something when pressed an option in the menu
        defaultSelected = selected;
        if (selected == 1) { // Play
            Logger.log("menu:selected == 1 -> gen = true");
            WorldGen.isEnabled = true;
            repaint();
            waitingToStartGame = true;
        }
        if (selected == 2) { // Ext Structs / Reload
            loadMG();
        }
        if (selected == 3) { // Levels
            isStopped = true;
            WorldGen.isEnabled = false;
            RootContainer.setRootUIComponent(new Levels());
        }
        if (selected == 4) { // About
            isStopped = true;
            RootContainer.setRootUIComponent(new AboutScreen());
        }
        if (selected == 5) { // Settings
            isStopped = true;
            RootContainer.setRootUIComponent(new SettingsScreen());
        }
        if (selected == 6) { // Exit
            isStopped = true;
            Platform.exit();
        }
    }
    
    void log(String s) {
        Logger.log(s);
        repaint();
    }

    private void loadMG() {
        (new Thread(new Runnable() {
            public void run() {
                menuOptions[2] = "Loading...";
                setStateFor(1, 2);
                mgStruct = new MgStruct();
                if (mgStruct.loadFromFiles()) {
                    areExtStructsLoaded = true;
                    menuOptions[2] = (MgStruct.loadedStructsNumber - MgStruct.loadedFromResNumber) + " loaded";
                    setColorEnabledOption(0x0099ff00);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    menuOptions[2] = "Reload";
                } else {
                    areExtStructsLoaded = false;
                    if (!mgStruct.loadCancelled) {
                        menuOptions[2] = "Nothing loaded";
                    } else {
                        menuOptions[2] = "Cancelled";
                    }
                    setColorEnabledOption(0x00880000);
                }
            }
        })).start();
    }
    
}