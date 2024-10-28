/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.game;

import java.io.IOException;
import java.io.InputStream;

import at.emini.physics2D.World;
import at.emini.physics2D.util.PhysicsFileReader;
import mobileapplication3.platform.FileUtils;
import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Platform;
import mobileapplication3.platform.ui.RootContainer;
import utils.GameFileUtils;
import utils.MgStruct;

/**
 *
 * @author vipaol
 */
public class Levels extends GenericMenu implements Runnable {

    private String[] levelPaths = new String[0];
    private String[] buttons;
    
    public static final String LEVELS_FOLDER_NAME = "MobappGame/Levels";
    
    public Levels() {
        Logger.log("Levels:constr");
        buttons = new String[2];
        try {
            levelPaths = getLevels();
            buttons = new String[levelPaths.length + 2];
            System.arraycopy(levelPaths, 0, buttons, 1, levelPaths.length);
        } catch (SecurityException e) {
            e.printStackTrace();
            buttons[0] = "no read permission";
        } catch (Exception e) {
            e.printStackTrace();
            buttons[0] = e.toString();
        }
    	buttons[0] = "Load emini \".phy\" world";
        // TODO: separate with pages -----------------------!
        buttons[buttons.length-1] = "Back";
        loadParams(buttons, 1, buttons.length - 1, buttons.length - 1);
	}

    public void init() {
        isStopped = false;
        getFontSize();
        (new Thread(this, "levels")).start();
    }
    
    public String[] getLevels() {
        Logger.log("Levels:getLevels()");
        return GameFileUtils.listFilesInAllPlaces(LEVELS_FOLDER_NAME);
    }
    
    public void startLevel(final String path) {
        (new Thread(new Runnable() {
            public void run() {
                GameplayCanvas gameCanvas = null;
                if (path.endsWith(".phy")) {
                    gameCanvas = new GameplayCanvas(readWorldFile(path));
                } else if (path.endsWith(".mglvl")) {
                    gameCanvas = loadLevel(path);
                }
                if (gameCanvas != null) {
                    RootContainer.setRootUIComponent(gameCanvas);
                    isStopped = true;
                }
            }
        })).start();
    }

    private static GameplayCanvas loadLevel(String path) {
        try {
            short[][] structure = MgStruct.readFromDataInputStream(FileUtils.fileToDataInputStream(path));
            if (structure != null) {
                return new GameplayCanvas(new GraphicsWorld()).loadLevel(structure);
            }
        } catch (IOException ex) {
            Platform.showError(ex);
        }
        return null;
    }

    public GraphicsWorld readWorldFile(String path) {
        PhysicsFileReader reader;
        InputStream is = FileUtils.fileToDataInputStream(path);
        reader = new PhysicsFileReader(is);
        GraphicsWorld w = new GraphicsWorld(World.loadWorld(reader));
        reader.close();
        return w;
    }
    
    public void selectPressed() {
        if (selected == buttons.length - 1) {
            isStopped = true;
            RootContainer.setRootUIComponent(new MenuCanvas());
        } else {
            try {
                startLevel(levelPaths[selected - 1]);
            } catch (Exception ex) {
                Platform.showError(ex);
            }
        }
    }
    

    public void run() {
        Logger.log("Levels:run()");
        long sleep = 0;
        long start = 0;
        
        isPaused = false;
        while (!isStopped) {
            if (!isPaused) {
                start = System.currentTimeMillis();
                
                repaint();
                tick();

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
}
