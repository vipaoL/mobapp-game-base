package mobileapplication3.game;

import mobileapplication3.platform.Battery;
import mobileapplication3.platform.Logger;
import mobileapplication3.platform.ui.RootContainer;
import utils.MobappGameSettings;

public class SettingsScreen extends GenericMenu implements Runnable {
    private static String[] menuOpts = {
            "Better graphics",
            "Unlock FPS",
            "Show FPS",
            "Skip every second frame",
            "Enable background",
            "Show battery level",
            "Debug settings",
            "Back"
        };
        
        // array with states of all buttons (active/inactive/enabled)
        private final int[] statemap = new int[menuOpts.length];
        private static int fontSizeCache = -1;
        private boolean batFailed = false;
        
        public SettingsScreen() {
        	loadParams(menuOpts, statemap);
		}
        
        public void init() {
            fontSizeCache = getFontSize();
            
            setSpecialOption(menuOpts.length - 2); // highlight "Debug settings" if enabled
            setIsSpecialOptnActivated(DebugMenu.isDebugEnabled);
            
            refreshStates();
            (new Thread(this, "debug menu")).start();
        }

        public void run() {
            long sleep;
            long start;
            
            if (!isMenuInited()) {
                init();
            }

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
                    MobappGameSettings.toggleBetterGraphics();
                    break;
                case 1:
                    MobappGameSettings.toggleFPSUnlocked();
                    break;
                case 2:
                	MobappGameSettings.toggleFPSShown();
                	break;
                case 3:
                	MobappGameSettings.toggleSecFramesSkipEnabled();
                	break;
                case 4:
                	MobappGameSettings.toggleBG();
                	break;
                case 5:
                	if (!MobappGameSettings.isBattIndicatorEnabled()) {
                		if (!Battery.checkAndInit()) {
                			batFailed = true;
                			Logger.log("Battery init failed");
                    		break;
                    	} else {
                    		int batLevel = Battery.getBatteryLevel();
                    		if (batLevel == Battery.ERROR) {
                    			String err = "Can't get battery level";
                    			menuOpts[selected] = err;
                    			Logger.log(err);
                    			break;
                    		} else {
                    			menuOpts[selected] = "Battery: " + batLevel + "%";
                    			Logger.log("bat method: " + Battery.getMethod());
                    		}
                    	}
                	}
                	MobappGameSettings.toggleBattIndicator();
                	break;
                default:
                    break;
            }
            if (selected == menuOpts.length - 2) {
                isStopped = true;
                RootContainer.setRootUIComponent(new DebugMenu());
            } else if (selected == menuOpts.length - 1) {
                isStopped = true;
                RootContainer.setRootUIComponent(new MenuCanvas());
            } else {
                refreshStates();
            }
        }
        void refreshStates() {
        	setEnabledFor(MobappGameSettings.isBetterGraphicsEnabled(), 0);
        	setEnabledFor(MobappGameSettings.isFPSUnlocked(), 1);
        	setEnabledFor(MobappGameSettings.isFPSShown(), 2);
        	setEnabledFor(MobappGameSettings.isSecFramesSkipEnabled(), 3);
        	setEnabledFor(MobappGameSettings.isBGEnabled(), 4);
        	if (!batFailed) {
        		setEnabledFor(MobappGameSettings.isBattIndicatorEnabled(), 5);
        	} else {
        		setStateFor(STATE_INACTIVE, 5);
        	}
        }
    }