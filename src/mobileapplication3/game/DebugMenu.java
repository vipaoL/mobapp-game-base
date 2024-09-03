/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mobileapplication3.game;

import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Sound;
import mobileapplication3.platform.ui.RootContainer;

/**
 *
 * @author vipaol
 */
public class DebugMenu extends GenericMenu implements Runnable {
    private static final String[] MENU_OPTS = {
        "Enable debug options",
        "Show log",
        "Simulation mode",
        "GAMING MODE",
        "What?",
        "Music",
        "Back"
    };
    
    // array with states of all buttons (active/inactive/enabled)
    private final int[] statemap = new int[MENU_OPTS.length];
    private static int fontSizeCache = -1;
    public static boolean isDebugEnabled = false;
    public static boolean closerWorldgen = false;
    public static boolean coordinates = false;
    public static boolean discoMode = false;
    public static boolean speedo = false;
    public static boolean cheat = false;
    public static boolean music = false;
    public static boolean showFontSize = false;
    public static boolean mgstructOnly = false;
    public static boolean dontCountFlips = false;
    public static boolean showAngle = false;
    public static boolean showLinePoints = false;
    public static boolean simulationMode = false;
    public static boolean whatTheGame = false;
    public static boolean showContacts = false;
    
    public DebugMenu() {
    	statemap[1] = GenericMenu.STATE_INACTIVE; // set "-----" separator as inactive button
        loadParams(MENU_OPTS, statemap);
	}
    
    public void init() {
        fontSizeCache = getFontSize();
        setSpecialOption(0);
        refreshStates();
        (new Thread(this, "debug menu")).start();
    }

    public void run() {
        long sleep;
        long start;

        while (!isStopped) {
            if (!isPaused) {
                start = System.currentTimeMillis();
                
                repaint();
                tick();

                sleep = GameplayCanvas.TICK_DURATION - (System.currentTimeMillis() - start);
                sleep = Math.max(sleep, 0);
            } else {
                sleep = 200;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    void selectPressed() {
        int selected = this.selected;
        switch (selected) {
            case 0:
                isDebugEnabled = !isDebugEnabled;
                showFontSize = isDebugEnabled;
                setIsSpecialOptnActivated(isDebugEnabled);
                Logger.logToStdout(isDebugEnabled);
                break;
            case 1:
                if (!Logger.isOnScreenLogEnabled()) {
                    Logger.enableOnScreenLog(h);
                } else {
                    Logger.disableOnScreenLog();
                }
                break;
            case 2:
                simulationMode = !simulationMode;
                break;
            case 3:
                discoMode = !discoMode;
                GraphicsWorld.bgOverride = discoMode;
                break;
            case 4:
                whatTheGame = !whatTheGame;
                break;
            case 5:
                music = !music;
                if (music) {
                    Sound sound = new Sound();
                    sound.start();
                }   break;
            default:
                break;
        }
        if (selected == MENU_OPTS.length - 1) {
            isStopped = true;
            RootContainer.setRootUIComponent(new SettingsScreen());
        } else {
            refreshStates();
        }
    }
    void refreshStates() {
        setIsSpecialOptnActivated(DebugMenu.isDebugEnabled);
        if (DebugMenu.isDebugEnabled) {
            setEnabledFor(Logger.isOnScreenLogEnabled(), 1);
            setEnabledFor(simulationMode, 2);
            setEnabledFor(discoMode, 3);
            setEnabledFor(whatTheGame, 4);
            setStateFor(/*music*/GenericMenu.STATE_INACTIVE, 5); // set "music" as inactive button. it's buggy
        } else {
            for (int i = 2; i < MENU_OPTS.length - 1; i++) {
                setStateFor(-1, i);
            }
        }
    }
}
