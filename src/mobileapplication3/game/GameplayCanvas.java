/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.game;

import at.emini.physics2D.Body;
import at.emini.physics2D.Contact;
import at.emini.physics2D.UserData;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXUtil;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;
import mobileapplication3.platform.Battery;
import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Mathh;
import mobileapplication3.platform.Sound;
import mobileapplication3.platform.ui.Font;
import mobileapplication3.platform.ui.Graphics;
import mobileapplication3.platform.ui.RootContainer;
import mobileapplication3.ui.Container;
import mobileapplication3.ui.Keys;
import utils.MobappGameSettings;

/**
 *
 * @author vipaol
 */
public class GameplayCanvas extends Container implements Runnable {
    public static final int TICK_DURATION = 50;
    private static final String[] MENU_HINT = {"MENU:", "here(touch),", "D, #"};
    private static final String[] PAUSE_HINT = {"PAUSE:", "here(touch), *,", "B, right soft"};
    private static final int GAME_SPEED_MULTIPLIER = 2;
    private static final int FORCE_MULTIPLIER = GAME_SPEED_MULTIPLIER;
    public static final short EFFECT_SPEED = 0;
    private static final int BATT_UPD_PERIOD = 10000;
    
    // to prevent siemens' bug which calls hideNotify right after showing canvas
    private static final int PAUSE_DELAY = 5;
    private int pauseDelay = PAUSE_DELAY;
    private boolean previousPauseState = false;
    
    // state and mode
    private static boolean isFirstStart = true; // for displaying hints only on first start
    public static boolean isBusy = false;
    public static boolean uninterestingDebug = false;
    public static boolean shouldWait = false;
    public static boolean isWaiting = false;
    private boolean isWorldLoaded = false;
    private int hintVisibleTimer = 120; // in ticks
    private boolean unlimitFPS = true;
    private boolean showFPS = false;
    private boolean oneFrameTwoTicks = false;
    private boolean battIndicator = false;
    private int batLevel;
    
    private boolean paused = false;
    private boolean stopped = false;
    
    // screen
    private int scW, scH;
    private int maxScSide = Math.max(scW, scH);
    
    // car
    private boolean leftWheelContacts = false;
    private boolean rightWheelContacts = false;
    private int carVelocitySqr, speedMultipiler;
    private int carAngle = 0;
    // motor state
    private boolean accel = false;
    
    // indicators
    private int flipIndicator = 255; // for blinking counter when flip done
    private int loadingProgress = 0;
    private int speedoState = 0;
    private int tickTime;
    private int prevTickTime;
    private int fps;
    
    // touchscreen
    private int pointerX = 0, pointerY = 0;
    private boolean pauseTouched = false;
    private boolean menuTouched = false;
    
    // counters
    public static int points = 0;
    private int gameoverCountdown;
    private static final int GAME_OVER_COUNTDOWN_STEPS = 8;
    public static int timeFlying = 10;
    private int timeMotorTurnedOff = 50;
    private long lastBigTickTime;
    
    public static short[][] currentEffects = new short[1][];
    
    // fonts
    private static final Font smallfont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final int sFontH = smallfont.getHeight();
    private static final Font largefont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
    //private static final int lFontH = largefont.getHeight();
    private Font currentFont = largefont;
    private int currentFontH = currentFont.getHeight();
    
    private static final int TEN_FX = FXUtil.toFX(10);
    
    private GraphicsWorld world;
    private WorldGen worldgen;
    private FlipCounter flipCounter;

    public GameplayCanvas() {
        loadingProgress = 5;
        log("gcanvas constructor");
        repaintOnlyOnFlushGraphics = true;
    }
    
    public GameplayCanvas(GraphicsWorld w) {
    	this();
        world = w;
    }
    
    public void init() {
        isBusy = false;
        shouldWait = false;
        isWaiting = false;
        timeFlying = 10;
        try {
	        unlimitFPS = MobappGameSettings.isFPSUnlocked(unlimitFPS);
	        showFPS = MobappGameSettings.isFPSShown(showFPS);
	        oneFrameTwoTicks = MobappGameSettings.isSecFramesSkipEnabled(oneFrameTwoTicks);
	        battIndicator = MobappGameSettings.isBattIndicatorEnabled(battIndicator);

	        if (battIndicator) {
	        	if (!Battery.checkAndInit()) {
	        		battIndicator = false;
	        	}
	        }
        } catch (Throwable ex) {
			ex.printStackTrace();
		}
        
        log("gcanvas init");
        
        if (world == null) {
            // new world
            setDefaultWorld();
        } else {
            // init an existing world
            initWorld();
        }
        
        world.refreshScreenParameters(scW, scH);
        
        Logger.setLogMessageDelay(50);
        currentEffects = new short[1][];
        log("gcanvas:starting thread");
        (new Thread(this, "game canvas")).start();
    }
    
    void reset() {
        log("reset");
        points = 0;
        gameoverCountdown = 0;
        if (WorldGen.isEnabled) {
            worldgen = new WorldGen(world);
            flipCounter = new FlipCounter();
            log("wg started");
        }
        setLoadingProgress(50);
        world.addCar();
        setLoadingProgress(60);
    }
    
    private void initWorld() {
        world.setGravity(FXVector.newVector(0, 250 * GAME_SPEED_MULTIPLIER));
        world.getLandscape().getBody().shape().setElasticity(5);
        setLoadingProgress(40);
        reset();
        isWorldLoaded = true;
    }
    
    private void setDefaultWorld() {
        log("gCanv:reading world");
        PhysicsFileReader reader = new PhysicsFileReader("/emptyworld.phy");
        setLoadingProgress(25);
        
        log("gCanv:loading world");
        World w = World.loadWorld(reader);

        log("gCanv:new grWorld");
        // there's siemens c65 stucks if obfucsation is enabled
        world = new GraphicsWorld(w);

        log("gCanv:setting world");
        initWorld();

        log("gCanv:closing reader");
        try {
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // game thread with main cycle and preparing
    public void run() {
        setLoadingProgress(80);
        
        try {
            log("gcanvas:thread started");

            long sleep = 0;
            long start = 0;
            int bigTickN = 0;
            int tick = 0;

            Contact[][] contacts = new Contact[3][];

            // init music player if enabled
            if (DebugMenu.isDebugEnabled && DebugMenu.music) {
                log("Starting sound");
                Sound sound = new Sound();
                sound.start();
            }

            setLoadingProgress(100);

            log("thread:starting game cycle");

            Logger.setLogMessageDelay(0);
            int baseTimestepFX = world.getTimestepFX();
            long lastFPSMeasureTime = System.currentTimeMillis();
            int framesFromLastFPSMeasure = 0;
            long lastBattUpdateTime = 0;

            // Main game cycle
            while (!stopped) {
                if (!paused) {
                	int dtFromLastFPSMeasure = (int) (System.currentTimeMillis() - lastFPSMeasureTime);
                	if (dtFromLastFPSMeasure > 1000) {
                		lastFPSMeasureTime = System.currentTimeMillis();
                		fps = framesFromLastFPSMeasure * 1000 / dtFromLastFPSMeasure;
                		framesFromLastFPSMeasure = 0;
                	}

                	prevTickTime = tickTime;
                	tickTime = (int) (System.currentTimeMillis() - start);
                    if (unlimitFPS) {
                    	world.setTimestepFX(baseTimestepFX*Mathh.constrain(1, (tickTime + prevTickTime + 1) / 2, 100)/50);
                    }

                    start = System.currentTimeMillis();
                    boolean bigTick = false;
                    if (!unlimitFPS || start - lastBigTickTime > TICK_DURATION) {
                    	lastBigTickTime = start;
                    	bigTick = true;
                    }

                    isBusy = true;
                    world.tick();
                    if (!oneFrameTwoTicks || tick % 2 == 0) {
                        repaint();
                    }
                    isBusy = false;

                    // check if car contacts with the ground or sth else
                    contacts[0] = world.getContactsForBody(world.leftwheel);
                    contacts[1] = world.getContactsForBody(world.rightwheel);
                    contacts[2] = world.getContactsForBody(world.carbody);
                    leftWheelContacts = contacts[0][0] != null;
                    rightWheelContacts = contacts[1][0] != null;
                    
                    if (bigTick) {
	                    if ((!leftWheelContacts && !rightWheelContacts)) {
	                        timeFlying += 1;
	                    } else {
	                        timeFlying = 0;
	                    }
                    }

                    // set motor power according to car speed
                    // (fast start and saving limited speed)
                    FXVector velFX = world.carbody.velocityFX();
                    int vX = velFX.xAsInt() / GAME_SPEED_MULTIPLIER;
                    int vY = velFX.yAsInt() / GAME_SPEED_MULTIPLIER;
                    if (currentEffects[EFFECT_SPEED] != null) {
                        if (currentEffects[EFFECT_SPEED][0] > 0) {
                            vX = vX * 100 / currentEffects[EFFECT_SPEED][2];
                            vY = vY * 100 / currentEffects[EFFECT_SPEED][2];
                        }
                    }
                    carVelocitySqr = vX * vX + vY * vY;
                    //System.out.println(carVelocitySqr);
                    if (carVelocitySqr > 1000000) {
                        speedMultipiler = 2;
                        speedoState = 2;
                    } else if (carVelocitySqr > 100000) {
                        speedoState = 1;
                        speedMultipiler = 15;
                    } else {
                        speedMultipiler = 20;
                        speedoState = 0;
                    }
                    if (uninterestingDebug) {
                        timeFlying = 0;
                        speedMultipiler = 30;
                    }

                    speedMultipiler *= FORCE_MULTIPLIER;

                    // getting car angle
                    carAngle = 360 - FXUtil.angleInDegrees2FX(world.carbody.rotation2FX());

                    // when the motor is turned on
                    if (bigTick) if (accel) {
                        timeMotorTurnedOff = 0;
                        if (timeFlying > 2) {
                            // apply rotational force
                        	//System.out.println(world.carbody.rotationVelocity2FX());
                            if (world.carbody.rotationVelocity2FX() > 100000000) {
                                world.carbody.applyTorque(-world.carbody.rotationVelocity2FX()/16000*FORCE_MULTIPLIER);
                            } else {
                                world.carbody.applyTorque(FXUtil.toFX(-10000*FORCE_MULTIPLIER));
                            }
                        } else {
                            // apply motor force when on the ground
                            int directionOffset = 0;
                            if (currentEffects[EFFECT_SPEED] != null) {
                                if (currentEffects[EFFECT_SPEED][0] > 0) {
                                    directionOffset = currentEffects[EFFECT_SPEED][1];
                                    speedMultipiler = speedMultipiler * currentEffects[EFFECT_SPEED][2] / 100;
                                    Logger.log(speedMultipiler);
                                }
                            }
                            int motorForceX = FXUtil.divideFX(FXUtil.toFX(Mathh.cos(carAngle - 15 + directionOffset) * speedMultipiler), TEN_FX * 5);
                            int motorForceY = FXUtil.divideFX(FXUtil.toFX(Mathh.sin(carAngle - 15 + directionOffset) * speedMultipiler), TEN_FX * 5);
                            world.carbody.applyMomentum(new FXVector(motorForceX, -motorForceY));
                            boolean carBodyContacts = world.getContactsForBody(world.carbody)[0] != null;
                            if ((!leftWheelContacts && carBodyContacts) || rightWheelContacts) {
                                int force = -6000;
                                if (rightWheelContacts) {
                                    force *= 2;
                                }
                                world.carbody.applyTorque(FXUtil.toFX(force*FORCE_MULTIPLIER));
                            }
                        }
                    } else {
                        // brake for two seconds after motor turning off
                        if (timeMotorTurnedOff < 40 && !uninterestingDebug) {
                            try {
                                if (world.carbody.angularVelocity2FX() > 0) {
                                    world.carbody.applyTorque(FXUtil.toFX(world.carbody.angularVelocity2FX() * GAME_SPEED_MULTIPLIER / 4000));
                                }
                                if (timeFlying < 2) {
                                    world.carbody.applyMomentum(new FXVector(-world.carbody.velocityFX().xFX*GAME_SPEED_MULTIPLIER/5, -world.carbody.velocityFX().yFX*GAME_SPEED_MULTIPLIER/5));
                                }
                            	timeMotorTurnedOff++;
                            } catch (NullPointerException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    // if touched an interactive object (falling platform, effect plate)
                    for (int j = 0; j < contacts.length; j++) {
                        for (int i = 0; i < contacts[j].length; i++) {
                            if (contacts[j][i] != null) {
                                Body body = contacts[j][i].body1();
                                if (body == null) {
                                    continue;
                                }
                                UserData userData = body.getUserData();
                                if (!(userData instanceof MUserData)) {
                                    continue;
                                }
                                MUserData bodyUserData = (MUserData) body.getUserData();
                                int bodyType = bodyUserData.bodyType;
                                switch (bodyType) {
                                    // add fall countdown timer on falling platform
                                    case MUserData.TYPE_FALLING_PLATFORM:
                                        if (!world.waitingForDynamic.contains(body)) {
                                            world.waitingForDynamic.addElement(body);
                                            world.waitingTime.addElement(new Integer(600));
                                            if (uninterestingDebug) world.removeBody(body);
                                        }
                                        break;
                                    // apply effect if touched an effect plate
                                    case MUserData.TYPE_ACCELERATOR:
                                        giveEffect(bodyUserData.data);
                                        world.setWheelColor(bodyUserData.color);
                                        break;
                                }
                            }
                        }
                    }

                    if (bigTick) {
	                    for (int i = 0; i < currentEffects.length; i++) {
	                        if (currentEffects[i] != null) {
	                            if (currentEffects[i][0] > 0) {
	                                currentEffects[i][0]--;
	                                log("effect" + i + "," + currentEffects[i][0] + " ticks left");
	                            } else if (currentEffects[i][0] == 0) {
	                                currentEffects[i] = null;
	                            }
	                        }
	                    }

                        if (DebugMenu.simulationMode) {
                            world.rightwheel.setDynamic(false);
                            world.carbody.setDynamic(false);
                            world.leftwheel.setDynamic(false);

                            world.carbody.translate(new FXVector(FXUtil.ONE_FX*100, 0), 0);
                            world.leftwheel.translate(new FXVector(FXUtil.ONE_FX*100, 0), 0);
                            world.rightwheel.translate(new FXVector(FXUtil.ONE_FX*100, 0), 0);
                        }

                        if (isWorldLoaded) {
                            hintVisibleTimer--;
                        }

                        if (pauseDelay > 0) {
                            pauseDelay--;
                        }

                        if (WorldGen.isEnabled) {
                            // highlight the score counter on flip
                            if (flipIndicator < 255) {
                                flipIndicator+=64;
                                if (flipIndicator >= 255) {
                                    flipIndicator = 255;
                                }
                            }
                            flipCounter.tick();
                        }
                    }
                    
                    if (tick < 3) {
                    	tick++;
                    } else {
                    	tick = 0;
                    }

                    if (bigTickN < 3) {
                    	if (bigTick) {
                    		bigTickN++;
                    	}
                    } else {
                        bigTickN = 0;
                        
                        // start the final countdown and open main menu if the car
                        // lies upside down or fell out of the world
                        int lowestY = 5000;
                        if (worldgen != null) {
                            lowestY = WorldGen.getLowestY();
                        }
                        if (GraphicsWorld.carY > 2000 + lowestY || (carAngle > 140 && carAngle < 220 && world.carbody.getContacts()[0] != null)) {
                            if (uninterestingDebug) {
                                gameoverCountdown = 0;
                            }
                            if (gameoverCountdown < GAME_OVER_COUNTDOWN_STEPS) {
                                gameoverCountdown++;
                            } else {
                                openMenu();
                            }
                        } else {
                            if (gameoverCountdown > 0) {
                                gameoverCountdown--;
                            } else {
                                gameoverCountdown = 0;
                            }
                        }

                        world.tickBodies();
                    }
                    
                    if (System.currentTimeMillis() - lastBattUpdateTime > BATT_UPD_PERIOD) {
                    	batLevel = Battery.getBatteryLevel();
                    	lastBattUpdateTime = System.currentTimeMillis();
                    }

                    while (shouldWait) {
                        isWaiting = true;
                        Thread.yield();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    framesFromLastFPSMeasure++;

                    isWaiting = false;
                    
                    Thread.yield();

                    sleep = TICK_DURATION - (System.currentTimeMillis() - start);
                    sleep = Math.max(sleep, 0);
                } else {
                    // if paused
                    if (paused) {
                        sleep = 200;
                    } else {
                        sleep = 0;
                    }
                    if (isVisible) {
                        repaint();
                    }
                }
                // fps/tps control
                try {
                    if (sleep > 0 && (!unlimitFPS || paused)) {
                        Thread.sleep(sleep);
                    } else if (System.currentTimeMillis() == start) {
                    	Thread thread = Thread.currentThread();
                    	while (System.currentTimeMillis() == start) {
                    		synchronized (thread) {
								thread.wait(0, 30);
							}
                    	}
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException ex) {
            log(ex.toString());
            ex.printStackTrace();
        }
    }
    
    public void paint(Graphics g) {
    	g.setColor(0, 0, 0);
        g.fillRect(0, 0, maxScSide, maxScSide);
        if (loadingProgress < 100) {
            drawLoading(g);
        } else {
            world.drawWorld(g);
        }
        drawHUD(g);
    }

    public void repaint() {
    	try {
	        paint(getUGraphics());
	        flushGraphics();
        } catch (Exception ex) { }
    }
    
    // point counter, very beautiful pause menu,
    // debug info, on-screen log, game over screen
    private void drawHUD(Graphics g) {
        // show hint on first start
        if (isFirstStart && hintVisibleTimer > 0) {
            int color = 255 * hintVisibleTimer / 120;
            g.setColor(color/4, color/2, color/4);
            if (Logger.isOnScreenLogEnabled()) {
                //g.setColor(color/2, color/2, color/2);
            }
            g.fillRect(0, 0, scW/3, scH/6);
            g.fillRect(scW*2/3, 0, scW/3, scH/6);
            g.setColor(color/4, color/4, color);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            for (int i = 0; i < MENU_HINT.length; i++) {
                g.drawString(MENU_HINT[i], scW/6, i * sFontH + scH / 12 - sFontH*MENU_HINT.length/2, Graphics.HCENTER | Graphics.TOP);
            }
            for (int i = 0; i < PAUSE_HINT.length; i++) {
                g.drawString(PAUSE_HINT[i], scW*5/6, i * sFontH + scH / 12 - sFontH*PAUSE_HINT.length/2, Graphics.HCENTER | Graphics.TOP);
            }
        }
        
        // draw some debug info if debug is enabled
        setFont(smallfont, g);
        int hudLeftTextOffset = 0;
        if (battIndicator) {
        	if (batLevel < 30) {
        		g.setColor(0xffff00);
        	} else if (batLevel < 10) {
        		g.setColor(0xff8000);
        	} else if (batLevel < 6) {
        		g.setColor(0x00ff00);
        	} else {
        		g.setColor(0x00ff00);
        	}
        	
            g.drawString("BAT: " + batLevel + "%", 0, hudLeftTextOffset, 0);
            hudLeftTextOffset += currentFontH;
        }
        if (DebugMenu.isDebugEnabled) {
            // speedometer
            if (DebugMenu.speedo) {
                switch (speedoState) {
                    case 0:
                        g.setColor(0, 255, 0);
                        break;
                    case 1:
                        g.setColor(255, 255, 0);
                        break;
                    default:
                        g.setColor(255, 0, 0);
                        break;
                }
                g.fillRect(0, hudLeftTextOffset, currentFontH * 5, currentFontH);
                g.setColor(255, 255, 255);
                g.drawString(String.valueOf(carVelocitySqr), 0, hudLeftTextOffset, 0);
                hudLeftTextOffset += currentFontH;
            }
            // car angle
            if (DebugMenu.showAngle) {
                if (timeFlying > 0) {
                    g.setColor(0, 0, 255);
                } else {
                    g.setColor(255, 255, 255);
                }
                g.drawString(String.valueOf(FXUtil.angleInDegrees2FX(world.carbody.rotation2FX())), 0, hudLeftTextOffset, 0);
                hudLeftTextOffset += currentFontH;
            }
        }
        // show coordinates of car if enabled
        if (DebugMenu.coordinates) {
            g.setColor(127, 127, 127);
            g.drawString(GraphicsWorld.carX + " " + GraphicsWorld.carY, 0, hudLeftTextOffset, 0); 
            hudLeftTextOffset += currentFontH;
        }
        
        if (showFPS) {
            g.setColor(0, 255, 0);
            if (fps < 19) {
                g.setColor(127, 127, 0);
                if (fps < 15) {
                    g.setColor(255, 0, 0);
                }
            }
            if (oneFrameTwoTicks) {
                g.drawString("FPS:" + fps/2 + " TPS:" + fps, 0, hudLeftTextOffset, 0);
            } else {
                g.drawString("FPS:" + fps, 0, hudLeftTextOffset, 0);
            }
            hudLeftTextOffset += currentFontH;
        }
        
        try {
            if (DebugMenu.isDebugEnabled) {
                switch (WorldGen.currStep) {
                    case 0:
                        g.setColor(0, 255, 0);
                        break;
                    case 1:
                        g.setColor(127, 127, 255);
                        break;
                    case 2:
                        g.setColor(127, 127, 0);
                        break;
                    case 3:
                        g.setColor(255, 0, 0);
                        break;
                    case 4:
                        g.setColor(127, 0, 191);
                        break;
                    case 5:
                        g.setColor(127, 63, 0);
                        break;
                    default:
                        break;
                }
                g.drawString("wg: mspt" + WorldGen.mspt + " step:" + WorldGen.currStep, 0, hudLeftTextOffset, 0);
                hudLeftTextOffset += currentFontH;
                
                g.drawString("sgs" + worldgen.getSegmentCount() + " bds" + world.getBodyCount(), 0, hudLeftTextOffset, 0);
                hudLeftTextOffset += currentFontH;
            }
        } catch(NullPointerException ex) {
            
        }
        
        // game over screen
        if (gameoverCountdown > 1) {
            g.setFont(largefont);
            g.setColor(255, 0, 0);
            g.drawString("!", scW / 2, scH / 3 + currentFontH / 2, Graphics.HCENTER | Graphics.TOP);
            g.setColor(0, 0, Math.min(127 * (GAME_OVER_COUNTDOWN_STEPS - gameoverCountdown) / GAME_OVER_COUNTDOWN_STEPS, 255));
            g.fillRect(0, 0, scW, scH*gameoverCountdown/GAME_OVER_COUNTDOWN_STEPS/2 + 1);
            g.fillRect(0, scH - scH*gameoverCountdown/GAME_OVER_COUNTDOWN_STEPS/2, scW, scH - 1);
        }
        
        // draw on-screen log if enabled
        Logger.paint(g);
        
        // score counter
        if (WorldGen.isEnabled && world != null) {
            g.setColor(flipIndicator, flipIndicator, 255);
            setFont(largefont, g);
            g.drawString(String.valueOf(points), scW/2, scH - currentFontH * 3 / 2,
                    Graphics.HCENTER | Graphics.TOP);
        }
        
        // draw beautiful(isn't it?) pause screen
        if (paused) {
            int d = scH / 40;
            // change color if debug enabled
            if (!DebugMenu.isDebugEnabled) {
                g.setColor(0, 0, 255);
            } else {
                g.setColor(0, 255, 0);
            }
            
            if (shouldWait) {
                g.setColor(127, 0, 0);
            }
            for (int i = 0; i <= scH; i++) {
                g.drawLine(scW / 2, 0, d * i, scH);
            }
            if (shouldWait) {
                g.setColor(255, 0, 0);
                g.drawString("Thread is locked", 0, 0, 0);
                g.drawString("(WorldGen is busy)", 0, g.getFont().getHeight(), 0);
            }
            setFont(largefont, g);
            g.setColor(255, 255, 255);
            g.drawString("PAUSED", scW / 2, scH / 3 + currentFontH / 2, Graphics.HCENTER | Graphics.TOP);
        }
    }
    
    private void drawLoading(Graphics g) {
        g.setColor(255, 255, 255);
        int l = scW * 2 / 3;
        int h = scH / 24;
        g.drawRect(scW / 2 - l / 2, scH * 2 / 3, l, h);
        g.fillRect(scW / 2 - l / 2, scH * 2 / 3, l*loadingProgress/100, h);
    }
    public void setLoadingProgress(int percents) {
        loadingProgress = percents;
        log(percents + "%");
        repaint();
    }
    
    private void setFont(Font font, Graphics g) {
        g.setFont(font);
        currentFont = font;
        currentFontH = currentFont.getHeight();
    }
    
    // log and repaint
    private void log(String text) {
        Logger.log(text);
        if (Logger.isOnScreenLogEnabled()) repaint();
    }
    
    public void giveEffect(short[] data) {
        int id = data[0];
        int dataLength = data.length - 1;
        currentEffects[id] = new short[dataLength];
        for (int i = 1; i < data.length; i++) {
            currentEffects[id][i - 1] = data[i];
        }
    }
    
    public void openMenu() {
        log("opening menu");
        stopped = true;
        isFirstStart = false;
        uninterestingDebug = false;
        WorldGen.isEnabled = false;
        RootContainer.setRootUIComponent(new MenuCanvas());
    }
    
    void resume() {
        paused = false;
        if (worldgen != null) {
            worldgen.resume();
        }
        repaint();
    }

    // also used as pause
    public void onHide() {
        log("hideNotify");
        paused = true;
        if (worldgen != null) {
            worldgen.pause();
        }
        repaint();
        // to prevent siemens' bug that calls hideNotify right after showing canvas
        if (pauseDelay > 0) {
            if (previousPauseState == false) {
                resume();
            }
        }
    }
    
    public void onShow() {
        log("showNotify");
        
        repaint();
        
        // to prevent siemens' bug that calls hideNotify right after showing canvas
        pauseDelay = PAUSE_DELAY;
        previousPauseState = paused;
    }
    
    protected void onSetBounds(int x0, int y0, int w, int h) {
        scW = w;
        scH = h;
        maxScSide = Math.max(scW, scH);
        if (world != null) {
            world.refreshScreenParameters(w, h);
        }
        repaint();
    }
    
    private void pauseButtonPressed() {
        if (!paused) {
            pauseDelay = 0;
            onHide();
        } else {
            resume();
        }
    }

    // keyboard events
    public boolean keyReleased(int keyCode, int count) {
        // turn off motor
        accel = false;
        if (timeFlying > 0) {
            timeFlying = Math.max(5, timeFlying);
        }
        return true;
    }
    
    public boolean keyPressed(int keyCode, int count) {
        int gameAction = RootContainer.getGameActionn(keyCode);
        // pause
        if (keyCode == Keys.KEY_SOFT_RIGHT/* | keyCode == GenericMenu.SIEMENS_KEYCODE_RIGHT_SOFT*/) {
            pauseButtonPressed();
        } else // menu
        if (keyCode == Keys.KEY_POUND | gameAction == Keys.GAME_D) {
            openMenu();
        } else  // also pause. i'll rework it later
        if ((keyCode == Keys.KEY_STAR | gameAction == Keys.GAME_B)) {
            pauseButtonPressed();
            // no cheats. only pause
            /*if (DebugMenu.isDebugEnabled && DebugMenu.cheat) {
                FXVector pos = w.carbody.positionFX();
                int carX = pos.xAsInt();
                int carY = pos.yAsInt();
                worldgen.line(carX - 200, carY + 200, carX + 2000, carY + 0);
            }*/
        } else
        if (keyCode == Keys.KEY_NUM6) {
        } else {
            // if not an action button, turn on the motor
            accel = true;
        }
        return true;
    }
    
    public boolean keyRepeated(int keyCode, int pressedCount) { return true; }

    // touch events
    public boolean pointerPressed(int x, int y) {
        if (x > scW * 2 / 3 && y < scH / 6) {
            pauseTouched = true;
        } else if (x < scW / 3 && y < scH / 6) {
            menuTouched = true;
        } else {
            // if not on buttons, turn on the motor
            accel = true;
        }
        pointerX = x;
        pointerY = y;
        return true;
    }
    public boolean pointerDragged(int x, int y) {
        if (pauseTouched | menuTouched) {
            if (x - pointerX > 3 | y - pointerY > 3) {
                log((x - pointerX) + "dx/dy" + (y - pointerY));
                log("btnPress cancelled:dragged");
                pauseTouched = false;
                menuTouched = false;
            }
        }
        pointerX = x;
        pointerY = y;
        return true;
    }
    public boolean pointerReleased(int x, int y) {
        if (pauseTouched) {
            pauseButtonPressed();
        }
        if (menuTouched) {
            openMenu();
        }
        pauseTouched = false;
        menuTouched = false;
        // turn off the motor
        accel = false;
        return true;
    }
    
    class FlipCounter {
        int step = 0;
        boolean flipDirection = false;
        boolean prevFlipDirection = false;

        void tick() {
            if (DebugMenu.dontCountFlips) {
                return;
            }
            flipDirection = world.carbody.rotationVelocity2FX() >= 0;
            if (flipDirection != prevFlipDirection || GameplayCanvas.timeFlying < 1 && !GameplayCanvas.uninterestingDebug) {
                step = 0;
            }
            prevFlipDirection = flipDirection;

            int ang = carAngle;
            boolean isInNormalPos = ang < 45 || ang > 315;
            if (isInNormalPos && step % 2 == 0) {
                step++;
                if (step > 1) {
                    if (flipDirection) {
                        if ((step - 1) % 4 == 0) {
                            onFlipDone();
                        }
                    } else {
                        onFlipDone();
                    }
                }
            } else if (!isInNormalPos && step % 2 != 0) {
                step++;
            }
        }

        // blink point counter on flip and increment the score
        public void onFlipDone() {
            flipIndicator = 0;
            points++;
        }
    }
}
