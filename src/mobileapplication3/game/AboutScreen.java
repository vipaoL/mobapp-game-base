/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.game;

import java.io.IOException;

import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;
import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Platform;
import mobileapplication3.platform.ui.Font;
import mobileapplication3.platform.ui.Graphics;
import mobileapplication3.platform.ui.Image;
import mobileapplication3.platform.ui.RootContainer;

/**
 *
 * @author vipaol
 */
public class AboutScreen extends GenericMenu implements Runnable {
    private static final String URL = "https://github.com/vipaoL/mobap-game";
    private static final String URL_PREVIEW = "GitHub: vipaoL/mobap-game";
    private static final String[] STRINGS = {"J2ME game on emini", "physics engine"};
    private static final String[] MENU_OPTS = {""/*there is qr code*/,
        URL_PREVIEW,
        "Version: " + Platform.getAppProperty("MIDlet-Version"),
        "Back"};
    private int counter = 17;
    private int scW, scH;
    private int extraVerticalMargin = 0;
    private int qrSide = 0;
    private int margin = 0;
    private static int fontSizeCache = -1;
    private Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL),
            font3 = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
    private int fontH = font.getHeight();
    private boolean bigQRIsDrawn = false;
    
    private Image qr, qrBig;

    public AboutScreen() {
    	loadParams(MENU_OPTS, 0, MENU_OPTS.length - 1, MENU_OPTS.length - 1);
    }
    
    public void init() {
        setFirstDrawable(1);
        (new Thread(this, "about canvas")).start();
    }
    
    protected void onSetBounds(int x0, int y0, int w, int h) {
    	super.onSetBounds(x0, y0, w, h);
        if (scW == w && scH == h && qr != null) {
            return;
        }
        scW = w;
        scH = h;
        qrSide = scH/* - font2H*/ - fontH * (STRINGS.length + MENU_OPTS.length + 1);
        margin = fontH/2;
        if (qrSide > scW - margin*2) {
            qrSide = scW - margin*2;
        }
        
        int headerAndQrH = fontH * (STRINGS.length) + 3*margin + qrSide;
        int buttonsFontH = findOptimalFont(scW, scH - headerAndQrH - margin, MENU_OPTS);
        extraVerticalMargin = (scH - (headerAndQrH + (3*MENU_OPTS.length/2)*buttonsFontH + margin)) / 4;
        if (extraVerticalMargin < 0) {
            extraVerticalMargin = 0;
        }
        
        int menuBtnsOffsetH = drawHeaderAndQR(null);
        int menuH = scH - menuBtnsOffsetH - margin - extraVerticalMargin;
        loadCanvasParams(
        		0, h - menuH,
                scW, menuH);
        
        try {
            qr = Image.createImage("/qr.png").scale(qrSide, qrSide);
        } catch (IOException ex) {
            try {
                qr = Image.createImage("resource://qr.png").scale(qrSide, qrSide);
            } catch (IOException e) {
                ex.printStackTrace();
                e.printStackTrace();
            }
        }
        
        try {
            qrBig = Image.createImage("/qr.png").scale(Math.min(scW, scH), Math.min(scW, scH));
        } catch (IOException ex) {
            try {
                qrBig = Image.createImage("resource://qr.png").scale(Math.min(scW, scH), Math.min(scW, scH));
            } catch (IOException e) {
                ex.printStackTrace();
                e.printStackTrace();
            }
        }
    }

    public void destroyApp(boolean unconditional) {
        isStopped = true;
        Platform.exit();
    }
    
    public void setIsPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public void run() {
        long sleep;
        long start;
        String commitHash = Platform.getAppProperty("Commit");
        if (commitHash != null) {
            for (int i = 0; i < MENU_OPTS.length; i++) {
                if (MENU_OPTS[i].startsWith("Version: ")) {
                    MENU_OPTS[i] += "-" + commitHash;
                    break;
                }
            }
        }

        while (!isStopped) {
            if (!isPaused) {
                start = System.currentTimeMillis();
                repaint();
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
    
    public void paint(Graphics g) {
    	//   if qr isn't selected, repaint on each frame
        //
        //   if big qr is open, draw it oncely,
        // and then we don't need to refresh screen
        if (bigQRIsDrawn) {
            return;
        }
        g.setColor(0, 0, 0);
        g.fillRect(0, 0, scW, scH);
        drawHeaderAndQR(g);
        super.paint(g);
        tick();
        
        if (selected == 0) {
            drawBigQR(g);
        } else {
            bigQRIsDrawn = false;
        }
    }
    
    int drawHeaderAndQR(Graphics g) {
        if (g != null) {
            g.setColor(255, 255, 255);
        }
        
        int offset = margin + extraVerticalMargin;
        for (int i = 0; i < STRINGS.length; i++) {
            if (g != null) {
                g.setFont(font);
                g.drawString(STRINGS[i], scW/2, offset, Graphics.HCENTER | Graphics.TOP);
            }
            offset += fontH;
        }
        offset += margin + extraVerticalMargin;
        
        if (g != null && selected != 0) {
            try {
                g.drawImage(qr, scW / 2, offset, Graphics.HCENTER | Graphics.TOP);
            } catch (NullPointerException ex) {
                g.drawLine(margin, offset, scW - margin, offset);
                g.drawLine(margin, offset + qrSide, scW - margin, offset + qrSide);
                g.drawLine(margin, offset, margin, offset + qrSide);
                g.drawLine(scW - margin, offset, scW - margin, offset + qrSide);
                g.setFont(font3);
                int x = scW / 2;
                int y = offset + qrSide / 2;
                g.drawString("Your ad", x, y, Graphics.HCENTER|Graphics.BOTTOM);
                g.drawString("could be here.", x, y, Graphics.HCENTER|Graphics.TOP);
            }
        }
        offset += qrSide;
        offset += margin + extraVerticalMargin;
        //g.drawLine(0, offset, scW, offset);
        return offset;
    }
    
    void drawBigQR(Graphics g) {
        try {
            g.drawImage(qrBig, scW / 2, scH / 2, Graphics.HCENTER | Graphics.VCENTER);
        } catch (NullPointerException ex) {
            bigQRIsDrawn = true;
        }
    }

    void openLink() {
        Logger.log(URL);
        if (Platform.platformRequest(URL)) {
            Platform.exit();
        }
    }

    void selectPressed() {
        int selected = this.selected;
        if (selected == MENU_OPTS.length - 3) {
            openLink();
        }
        if (selected == MENU_OPTS.length - 2) {
            counter+=1;
            if (counter == 20) {
                isStopped = true;
                
                WorldGen.isEnabled = true;
                
                World test3 = new World();
                test3.setGravity(FXVector.newVector(10, 100));
                GraphicsWorld.bgOverride = true;
                GraphicsWorld test2 = new GraphicsWorld(test3);
                GameplayCanvas test = new GameplayCanvas(test2);
                GameplayCanvas.uninterestingDebug = true;
                RootContainer.setRootUIComponent(test);
            }
        }
        if (selected == MENU_OPTS.length - 1) {
            isStopped = true;
            RootContainer.setRootUIComponent(new MenuCanvas());
        }
    }
}
