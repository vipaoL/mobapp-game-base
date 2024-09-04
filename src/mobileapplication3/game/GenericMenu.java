/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.game;

import java.util.Vector;

import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Mathh;
import mobileapplication3.platform.Platform;
import mobileapplication3.platform.ui.Font;
import mobileapplication3.platform.ui.Graphics;
import mobileapplication3.platform.ui.RootContainer;
import mobileapplication3.ui.Container;
import mobileapplication3.ui.Keys;

/**
 *
 * @author vipaol
 */
public abstract class GenericMenu extends Container {
    private static final int PAUSE_DELAY = 5;
    private static final int KEY_PRESS_DELAY = 5;
    
    public int w, h;
    private int x0, y0, fontH, tick = 0, k = 10, keyPressDelay = 0,
            keyPressDelayAfterShowing = 5, firstReachable, lastReachable,
            firstDrawable = 0, specialOption = -1, pauseDelay = PAUSE_DELAY, lastKeyCode = 0;
    
    public int selected;
    
    // colors
    protected int normalColor = 0xffffff, selectedColor = 0xff4040,
            pressedColor = 0xE03838, specialOptionActivatedColor = 0xffff00,
            colUnreachable = 0x888888, colReachableEnabled = 0x88ff00, bgColor = 0x000000;
    private String[] options;
    
    private boolean isPressedByPointerNow, firstload = true,
            isSpecialOptionActivated = false, isSelectPressed = false,
            isSelectAlreadyPressed = false, isStatemapEnabled = false,
            fontFound = false;
    
    private boolean isKnownButton = true, isInited = false;
    public boolean isPaused = false;
    public boolean isStopped = false;
    private Font font;
    private int[] stateMap = null;
    public static final int STATE_INACTIVE = -1;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_NORMAL_ENABLED = 1;
    
    
    // key codes
    public static final int SIEMENS_KEY_FIRE = -26;
    public static final int SIEMENS_KEY_UP = -59;
    public static final int SIEMENS_KEY_DOWN = -60;
    public static final int SIEMENS_KEY_LEFT = -61;
    public static final int SIEMENS_KEY_RIGHT = -62;
    public static final int SIEMENS_KEY_LEFT_SOFT = -1;
    public static final int SIEMENS_KEY_RIGHT_SOFT = -4;
    public static final int SE_KEY_BACK = -11;
    public static final int KEY_SOFT_RIGHT = -7;
    
    public void paint(Graphics g) {
    	if (bgColor >= 0) {
    		g.setColor(bgColor);
    		g.fillRect(x0, y0, Math.max(w, h), Math.max(w, h));
    	}
        if (isInited) {
            for (int i = firstDrawable; i < options.length; i++) {
            	if (font != null) {
            		g.setFont(font);
            	}
                g.setColor(normalColor);
                int offset = 0;

                if (i == selected) { // highlighting selected option
                    offset = Mathh.sin(tick * 360 / 10); //waving
                    g.setColor(selectedColor);
                    if (isPressedByPointerNow) {
                        g.setColor(pressedColor);
                        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, font.getSize()));
                    }

                }

                if (isStatemapEnabled) { // coloring other options depending on theirs state (if we have this info)
                    if (stateMap[i] == STATE_NORMAL_ENABLED) {
                        g.setColor(colReachableEnabled);
                    } else if (stateMap[i] == STATE_INACTIVE) {
                        g.setColor(colUnreachable);
                    }
                }


                if (i == specialOption && isSpecialOptionActivated) { // painting special option in a different color
                    g.setColor(specialOptionActivatedColor);
                }

                int x = x0 + w / 2;
                int y = y0 + k * (i + 1 - firstDrawable) - fontH / 2 - h / (options.length + 1 - firstDrawable) / 2 + offset*Font.getDefaultFont().getHeight() / 8000;
                g.drawString(options[i], x, y, Graphics.HCENTER | Graphics.TOP); // draw option on (x, y) //

                if (DebugMenu.isDebugEnabled && DebugMenu.showFontSize) {
                    g.drawString(String.valueOf(font.getSize()), x0, y0, 0); // display text size (for debug)
                }
            }
            
            if (!isKnownButton) {
                g.setColor(0x808080);
                g.drawString(lastKeyCode + " - unknown keyCode", w, h, Graphics.BOTTOM | Graphics.RIGHT);
            }
        } else {
            g.setColor(0x808080);
            g.drawString("Loading the menu...", w / 2, h, Graphics.BOTTOM | Graphics.HCENTER);
        }
        Logger.paint(g);
    }
    
    public int findOptimalFont(int canvW, int canvH, String[] options) {
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
        
        // height
        if (font.getHeight() * options.length - firstDrawable >= canvH - canvH/16) {
            font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        }
        if (font.getHeight() * options.length - firstDrawable >= canvH - canvH/16) {
            font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        }
        
        // width
        if (font.getSize() != Font.SIZE_SMALL) {
            for (int i = firstDrawable; i < options.length - 1; i++) {
                if (font.stringWidth((String) options[i]) >= canvW - canvW/16) {
                    font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
                    if (font.stringWidth((String) options[i]) >= canvW - canvW/16) {
                        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
                        break;
                    }
                }
            }
        }
        return font.getHeight();
    }
    
    private boolean isOptionAvailable(int n) {
        if (isStatemapEnabled) {
            if (n >= stateMap.length) {
                return false;
            }
            if (stateMap[n] == STATE_INACTIVE) {
                return false;
            }
        }
        
        if (n < firstReachable || n > lastReachable) {
            return false;
        }
        
        return true;
    }
    
    public boolean isMenuInited() {
        return isInited;
    }
    
    public boolean pointerPressed(int x, int y) {
        handlePointer(x, y);
        return true;
    }

    public boolean pointerDragged(int x, int y) {
        handlePointer(x, y);
        return true;
    }

    public boolean pointerReleased(int x, int y) {
        if (handlePointer(x, y)) {
            selectPressed();
        }
        return true;
    }
    
    public boolean handlePointer(int x, int y) {
        x -= x0;
        y -= y0;
        isPaused = false;
        int selected = firstDrawable + y / k;
        if (selected < firstReachable && firstReachable < firstDrawable) {
            selected = firstReachable;
        }
        if (!isOptionAvailable(selected)) {
            isPressedByPointerNow = false;
            return false;
        }
        this.selected = selected;
        isPressedByPointerNow = true;
        return true;
    }
    
    private boolean handleKeyStates(int keyStates) {
        if (keyStates == 0) {
        	return false;
        }
        
        isPaused = false;
        isSelectAlreadyPressed = isSelectPressed;
        
        if (keyPressDelay < 1) {
            keyPressDelay = KEY_PRESS_DELAY;
            if (keyStates == Keys.LEFT) {
                selected = lastReachable; // back
            }
            switch (keyStates) {
				case Keys.RIGHT:
				case Keys.FIRE:
				case Keys.LEFT:
					isSelectPressed = true;
	            	isKnownButton = true;
					break;
			}
            
            boolean needRepeat = false;
            do {
                needRepeat = false;
                switch (keyStates) {
                	case Keys.UP:
                		isKnownButton = true;
                        isPaused = false;
                        if (selected > firstReachable) {
                            selected--;
                        } else {
                            selected = lastReachable;
                        }
                        //Main.log("up");
                        break;
                	case Keys.DOWN:
                		//Main.log("down");
                        isKnownButton = true;
                        isPaused = false;
                        if (selected < lastReachable) {
                            selected++;
                        } else {
                            selected = firstReachable;
                        }
                        break;
                }
                
                if (isStatemapEnabled && !isSelectPressed) {
                    needRepeat = stateMap[selected] == STATE_INACTIVE;
                }
            } while (needRepeat);
        }
        return isSelectPressed;// && !isSelectAlreadyPressed;
    }
    
    public boolean keyRepeated(int keyCode, int pressedCount) {
    	return keyPressed(keyCode, pressedCount);
    }
    
    public boolean keyPressed(int keyCode, int count) {
        if(handleKeyPressed(keyCode)) {
            selectPressed();
            isSelectPressed = false;
        }
        return true;
    }
    
    public boolean keyReleased(int keyCode, int count) {
    	return true;
    }
    
    public boolean handleKeyPressed(int keyCode) {
        lastKeyCode = keyCode;
        isKnownButton = false;
        isPaused = false;
        Logger.log("pressed:", keyCode);
        boolean pressed = false;
        int selected = -1;
        switch (keyCode) {
            case Keys.KEY_NUM1:
                selected = 0;
                pressed = true;
                break;
            case Keys.KEY_NUM2:
                selected = 1;
                pressed = true;
                break;
            case Keys.KEY_NUM3:
                selected = 2;
                pressed = true;
                break;
            case Keys.KEY_NUM4:
                selected = 3;
                pressed = true;
                break;
            case Keys.KEY_NUM5:
                selected = 4;
                pressed = true;
                break;
            case Keys.KEY_NUM6:
                selected = 5;
                pressed = true;
                break;
            case Keys.KEY_NUM7:
                selected = 6;
                pressed = true;
                break;
            case Keys.KEY_NUM8:
                selected = 7;
                pressed = true;
                break;
            case Keys.KEY_NUM9:
                selected = 8;
                pressed = true;
                break;
            case Keys.KEY_STAR:
                selected = 9;
                pressed = true;
                break;
            case Keys.KEY_NUM0:
                break;
            case KEY_SOFT_RIGHT:
                break;
            case SE_KEY_BACK:
                break;
            case SIEMENS_KEY_LEFT:
                break;
            case Keys.KEY_POUND:
                isKnownButton = true;
                if (keyPressDelay < 1) {
                    keyPressDelay = KEY_PRESS_DELAY;
                    return true;
                } else {
                    return false;
                }
            case SIEMENS_KEY_UP:
                isKnownButton = true;
                handleKeyStates(Keys.UP);
                break;
            case SIEMENS_KEY_DOWN:
                isKnownButton = true;
                handleKeyStates(Keys.DOWN);
                break;
            case SIEMENS_KEY_RIGHT:
                isKnownButton = true;
                if (keyPressDelay < 1) {
                    keyPressDelay = KEY_PRESS_DELAY;
                    return true;
                } else {
                    return false;
                }
            case SIEMENS_KEY_FIRE:
            case -6: // left soft button
                handleKeyStates(Keys.FIRE);
            default:
                return handleKeyStates(RootContainer.getGameActionn(keyCode));
        }
        selected += firstReachable;
        if (keyCode == Keys.KEY_NUM0 || keyCode == KEY_SOFT_RIGHT || keyCode == SIEMENS_KEY_LEFT || keyCode == SE_KEY_BACK) {
            return handleKeyStates(Keys.LEFT);
        }
        
        if (pressed) {
            isKnownButton = true;
            if (isOptionAvailable(selected)) {
                this.selected = selected;
                return true;
            }
        }
        return false;
    }
    
    public void keyReleased(int keyCode) {
        keyPressDelay = 0;
        isSelectPressed = false;
        isSelectAlreadyPressed = false;
    }
    
    protected void loadCanvasParams(int x0, int y0, int w, int h) {
        this.x0 = x0;
        this.y0 = y0;
        if (w <= 0 || h <= 0) {
        	return;
        }
        
        this.w = w;
        this.h = h;
        
        if (options != null) {
            k = (h + h / (options.length + 1 - firstDrawable)) / (options.length + 1 - firstDrawable);
            fontH = findOptimalFont(w, h, options);
            fontFound = true;
        }
    }
    public void reloadCanvasParameters(int scW, int scH) {
        reloadCanvasParameters(x0, y0, scW, scH);
    }
    public void reloadCanvasParameters(int x0, int y0, int w, int h) {
        int fontSize = -1;
        if (w - x0 == this.w - this.x0 && h - y0 == this.h - this.y0 && font != null) {
            fontSize = font.getSize();
        }
        loadCanvasParams(x0, y0, w, h);
    }
    
    /**
     * Should be placed to showNotify.
     * <p>handleHideNotify() in its right place is also needed.
     * <p>
     * It prevents siemens' bug that calls hideNotify right after
     * calling showNotify.
     */
    public void onShow() {
        Logger.log("menu:showNotify");
        
        isPaused = false;
        pauseDelay = PAUSE_DELAY;
    }
    
    public void onHide() {
        Logger.log("menu:hideNotify");
        // It prevents a bug on siemens that calls hideNotify right after calling showNotify.
        if (pauseDelay <= 0) {
            isPaused = true;
        }
    }
    
    protected void onSetBounds(int x0, int y0, int w, int h) {
        this.w = w;
        this.h = h;
        reloadCanvasParameters(w, h);
    }
    
    
    
    public void loadParams(String[] options, int[] statemap) {
        loadParams(options, 0, options.length - 1, options.length - 1, statemap);
    }
    public void loadParams(String[] options, int firstReachable, int lastReachable, int defaultSelected) {
        loadParams(options, firstReachable, lastReachable, defaultSelected, null);
    }
    public void loadParams(Vector options, int firstReachable, int lastReachable, int defaultSelected) {
        String[] optsArray = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            optsArray[i] = (String) options.elementAt(i);
        }
        loadParams(optsArray, firstReachable, lastReachable, defaultSelected);
    }
    public void loadParams(String[] options, int firstReachable, int lastReachable, int defaultSelected, int[] optionStateMap) {
        this.options = options;
        this.firstReachable = firstReachable;
        this.lastReachable = lastReachable;
        if (firstload) {
            selected = defaultSelected;
            firstload = false;
        }
        if (optionStateMap != null) {
            loadStatemap(optionStateMap);
        }
        isInited = true;
    }
    public void loadStatemap(int[] stateMap) {
        isStatemapEnabled = false;
        if (stateMap != null) {
            if (stateMap.length == options.length) {
                this.stateMap = stateMap;
                isStatemapEnabled = true;
                Logger.log("stateMap loaded");
            } else {
                Platform.showError("GenericMenu.loadStatemap:optionTypeMap.length must be == options.length");
            }
        } else {
            Platform.showError("GenericMenu.loadStatemap:null stateMap");
        }
    }
    public void setDefaultColor(int color_hex) {
        normalColor = color_hex;
    }
    public void setSelectedColor(int color_hex) {
        selectedColor = color_hex;
    }
    public void setPressedColor(int color_hex) {
        pressedColor = color_hex;
    }
    public void setColorEnabledOption(int color_hex) {
        colReachableEnabled = color_hex;
    }
    public void setColorUnreachableOption(int color_hex) {
        colUnreachable = color_hex;
    }
    public void setSpecialOptnActColor(int colorActivated) {
        specialOptionActivatedColor = colorActivated;
    }
    public void setSpecialOption(int n) {
        specialOption = n;
    }
    public void setIsSpecialOptnActivated (boolean isActivated) {
        isSpecialOptionActivated = isActivated;
    }
    public void setFirstDrawable(int n) {
        firstDrawable = n;
        k = (h + h / (options.length + 1 - firstDrawable)) / (options.length + 1 - firstDrawable);
    }
    public void setEnabledFor(boolean enabled, int i) {
        if (enabled) {
            setStateFor(1, i);
        } else {
            setStateFor(0, i);
        }
    }
    public void setStateFor(int state, int i) {
        if (stateMap == null) {
            return;
        }
        stateMap[i] = state;
    }
    public int getFontSize() {
        if (fontFound) {
            return font.getSize();
        } else {
            return -1;
        }
    }
    public void tick() {
        if (tick > 9) {
            tick = 0;
        } else {
            tick++;
        }
        if (pauseDelay > 0) {
            pauseDelay--;
        }
        if (keyPressDelayAfterShowing > 0) {
            keyPressDelayAfterShowing--;
        }
        if (keyPressDelay > 0) {
            keyPressDelay--;
        }
    }
    
    abstract void selectPressed();
}
