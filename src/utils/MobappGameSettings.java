package utils;

import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Settings;

public class MobappGameSettings {
    private static final String
    		RECORD_STORE_SETTINGS = "settings",
		    IS_SETUP_WIZARD_COMPLETED = "wizardCompleted",
			MGSTRUCTS_FOLDER_PATH = "mgPath",
		    IS_BETTER_GRAPHICS_ENABLED = "btrGr",
		    SHOW_FPS = "showFPS",
		    SHOW_BG = "enBG",
		    BATTERY_INDICATOR = "Batt";
    
    private static String mgstructsFolderPath = null;
    
    private static Settings settingsInst = null;
    
    private MobappGameSettings() { }
    
    private static Settings getSettingsInst() {
    	if (settingsInst == null) {
    		settingsInst = new Settings(new String[]{
    	            IS_SETUP_WIZARD_COMPLETED,
    	            MGSTRUCTS_FOLDER_PATH,
    	            IS_BETTER_GRAPHICS_ENABLED,
    	            SHOW_FPS,
    	            SHOW_BG,
    	            BATTERY_INDICATOR
    	        }, RECORD_STORE_SETTINGS);
    	}
    	return settingsInst;
    }
    
    public static boolean isBattIndicatorEnabled() {
        return getSettingsInst().getBool(BATTERY_INDICATOR);
    }
    
    public static boolean isBattIndicatorEnabled(boolean defaultValue) {
        return getSettingsInst().getBool(BATTERY_INDICATOR, defaultValue);
    }
    
    public static void setBattIndicatorEnabled(boolean b) {
    	getSettingsInst().set(BATTERY_INDICATOR, b);
    }
    
    public static boolean toggleBattIndicator() {
    	return getSettingsInst().toggleBool(BATTERY_INDICATOR);
    }
    
    ///
    
    public static boolean isBGEnabled() {
        return getSettingsInst().getBool(SHOW_BG);
    }
    
    public static boolean isBGEnabled(boolean defaultValue) {
        return getSettingsInst().getBool(SHOW_BG, defaultValue);
    }
    
    public static void setBGEnabled(boolean b) {
    	getSettingsInst().set(SHOW_BG, b);
    }
    
    public static boolean toggleBG() {
    	return getSettingsInst().toggleBool(SHOW_BG);
    }
    
    ///
    
    public static boolean isFPSShown() {
        return getSettingsInst().getBool(SHOW_FPS);
    }
    
    public static boolean isFPSShown(boolean defaultValue) {
        return getSettingsInst().getBool(SHOW_FPS, defaultValue);
    }
    
    public static void setFPSShown(boolean b) {
    	getSettingsInst().set(SHOW_FPS, b);
    }
    
    public static boolean toggleFPSShown() {
    	return getSettingsInst().toggleBool(SHOW_FPS);
    }
    
    ///
    
    public static boolean isBetterGraphicsEnabled() {
        return getSettingsInst().getBool(IS_BETTER_GRAPHICS_ENABLED);
    }
    
    public static boolean isBetterGraphicsEnabled(boolean defaultValue) {
        return getSettingsInst().getBool(IS_BETTER_GRAPHICS_ENABLED, defaultValue);
    }
    
    public static void setBetterGraphicsEnabled(boolean b) {
    	getSettingsInst().set(IS_BETTER_GRAPHICS_ENABLED, b);
    }
    
    public static boolean toggleBetterGraphics() {
    	return getSettingsInst().toggleBool(IS_BETTER_GRAPHICS_ENABLED);
    }
    
    ///
    
    public static String getMgstructsFolderPath() {
        if (mgstructsFolderPath == null) {
            mgstructsFolderPath = getSettingsInst().getStr(MGSTRUCTS_FOLDER_PATH);
        }
        
        Logger.log("mgstructsFolderPath=" + mgstructsFolderPath);
        return mgstructsFolderPath;
    }

    public static void setMgstructsFolderPath(String path) {
    	getSettingsInst().set(MGSTRUCTS_FOLDER_PATH, path);
    }
    
    ///
    
    public static boolean isSetupWizardCompleted() {
        return getSettingsInst().getBool(IS_SETUP_WIZARD_COMPLETED);
    }

    public static void setIsSetupWizardCompleted(boolean b) {
    	getSettingsInst().set(IS_SETUP_WIZARD_COMPLETED, b);
    }
}
