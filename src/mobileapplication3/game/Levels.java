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

/**
 *
 * @author vipaol
 */
public class Levels extends GenericMenu implements Runnable {

    private String[] levelPaths = new String[0];
    private String[] buttons = new String[2];
    
    private static int fontSizeCache = -1;
    
    public static final String LEVELS_FOLDER_NAME = "Levels";
    
    public Levels() {
        Logger.log("Levels:constr");
        buttons = new String[2];
        try {
            getLevels();
            buttons = new String[levelPaths.length + 2];
        } catch (SecurityException e) {
            e.printStackTrace();
            buttons[0] = "no read permission";
        } catch (Exception e) {
            e.printStackTrace();
            buttons[0] = e.toString();
        }
    	buttons[0] = "Levels";
        // TODO: separate with pages -----------------------!
        buttons[buttons.length-1] = "Back";
        loadParams(buttons, 1, buttons.length - 1, buttons.length - 1);
	}

    public void init() {
        isStopped = false;
        fontSizeCache = getFontSize();
        (new Thread(this, "levels")).start();
    }
    
    public void getLevels() {
        Logger.log("Levels:getLevels()");
        levelPaths = GameFileUtils.listFilesInAllPlaces(LEVELS_FOLDER_NAME);
    }
    
    public void startLevel(final String path) {
        (new Thread(new Runnable() {
            public void run() {
                GameplayCanvas gameCanvas = new GameplayCanvas(readWorldFile(path));
                RootContainer.setRootUIComponent(gameCanvas);
                isStopped = true;
            }
        })).start();
    }
    
    public GraphicsWorld readWorldFile(String path) {
        PhysicsFileReader reader;
        try {
            InputStream is;
            is = FileUtils.fileToDataInputStream(path);
            reader = new PhysicsFileReader(is);
            GraphicsWorld w = new GraphicsWorld(World.loadWorld(reader));
            reader.close();
            is.close();
            return w;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public void selectPressed() {
        if (selected == buttons.length - 1) {
            isStopped = true;
            RootContainer.setRootUIComponent(new MenuCanvas());
        } else {
            try {
                startLevel(levelPaths[selected]);
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
