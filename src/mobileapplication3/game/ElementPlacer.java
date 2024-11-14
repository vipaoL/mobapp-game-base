package mobileapplication3.game;

import at.emini.physics2D.Body;
import at.emini.physics2D.Landscape;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXUtil;
import at.emini.physics2D.util.FXVector;
import mobileapplication3.platform.Logger;
import mobileapplication3.platform.Mathh;

public class ElementPlacer {
    public static final short EOF = 0;
    public static final short END_POINT = 1;
    public static final short LINE = 2;
    public static final short CIRCLE = 3;
    public static final short BROKEN_LINE = 4;
    public static final short BROKEN_CIRCLE = 5;
    public static final short SINE = 6;
    public static final short ACCELERATOR = 7;
    public static final short TRAMPOLINE = 8;
    public static final short LEVEL_START = 9;
    public static final short LEVEL_FINISH = 10;

    private int lineCount;
    private final GraphicsWorld w;
    private final Landscape landscape;
    private final boolean dontPlaceBodies;

    public ElementPlacer(GraphicsWorld world, boolean dontPlaceBodies) {
        lineCount = 0;
        w = world;
        landscape = world.getLandscape();
        this.dontPlaceBodies = dontPlaceBodies;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void place(short[] data, int originX, int originY) {
        short id = data[0];
        switch (id) {
            case LINE:
                line(originX + data[1], originY + data[2], originX + data[3], originY + data[4]);
                break;
            case CIRCLE:
                arc(originX + data[1], originY + data[2], data[3], data[4], data[5], data[6] / 10, data[7] / 10);
                break;
            case BROKEN_LINE:
                if (!dontPlaceBodies) {
                    int x1 = data[1];
                    int y1 = data[2];
                    int x2 = data[3];
                    int y2 = data[4];
                    int dx = x2 - x1;
                    int dy = y2 - y1;
                    int platfH = data[5];
                    int platfL = data[6];
                    int spacing = data[7];
                    int l = data[8];
                    int ang = data[9];
                    int n = (l + spacing) / (platfL + spacing);

                    Shape rect = Shape.createRectangle(platfL, platfH);
                    rect.setMass(1);
                    rect.setFriction(10);
                    rect.setElasticity(0);
                    dx /= (l / platfL);
                    int spX = spacing * dx / l; // TODO fix this mess (the editor should be fixed too, it will be a new mgstruct format version)
                    dy /= (l / platfL);
                    int spY = spacing * dy / l;
                    int offsetX = platfL / 2 * Mathh.cos(ang) / 1000;
                    int offsetY = platfL / 2 * Mathh.sin(ang) / 1000;

                    for (int i = 0; i < n; i++) {
                        Body fallinPlatf = new Body(originX + x1 + i * (dx + spX) + offsetX, originY + y1 + i * (dy + spY) + offsetY, rect, false);
                        fallinPlatf.setRotation2FX(FXUtil.TWO_PI_2FX / 360 * ang);
                        fallinPlatf.setUserData(new MUserData(MUserData.TYPE_FALLING_PLATFORM, new short[]{20}));
                        w.addBody(fallinPlatf);
                    }
                    updateLowestY(Math.max(y1, y2) + platfH);
                }
                break;
            case BROKEN_CIRCLE:
                // not implemented yet
                arc(originX + data[1], originY + data[2], data[3], 360, 0);
                break;
            case SINE:
                sin(originX + data[1], originY + data[2], data[3], data[4], data[5], data[6]);
                break;
            case ACCELERATOR: {
                int x = originX + data[1];
                int y = originY + data[2];
                int l = data[3];
                int thickness = data[4];
                int ang = data[5];

                short effectID = GameplayCanvas.EFFECT_SPEED;
                short effectDuration = data[8];
                short directionOffset = data[6];
                short speedMultiplier = data[7];

                int centerX = x + l * Mathh.cos(ang) / 2000;
                int centerY = y + l * Mathh.sin(ang) / 2000;

                int colorModifier = (speedMultiplier - 100) * 3;
                int red = Math.min(255, Math.max(0, colorModifier));
                int blue = Math.min(255, Math.max(0, -colorModifier));
                int green = blue;
                if (red < 50 && blue < 50) {
                    red = 50;
                    blue = 50;
                }

                int color = ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
                Shape plate = Shape.createRectangle(l, thickness);
                Body pressurePlate = new Body(centerX, centerY, plate, false);
                MUserData mUserData = new MUserData(MUserData.TYPE_ACCELERATOR, new short[]{effectID, effectDuration, directionOffset, speedMultiplier});
                mUserData.color = color;
                pressurePlate.setUserData(mUserData);
                //Main.log(pressurePlate.getUserData().bodyType);
                pressurePlate.setRotation2FX(FXUtil.TWO_PI_2FX / 360 * ang);
                w.addBody(pressurePlate);
                updateLowestY(y + Math.max(l, thickness));
                break;
            }
            case LEVEL_FINISH: {
                int x = originX + data[1];
                int y = originY + data[2];
                int l = data[3];
                int thickness = data[4];
                int ang = data[5];

                Shape plate = Shape.createRectangle(l, thickness);
                Body pressurePlate = new Body(x, y, plate, false);
                MUserData mUserData = new MUserData(MUserData.TYPE_LEVEL_FINISH, null);
                pressurePlate.setUserData(mUserData);
                //Main.log(pressurePlate.getUserData().bodyType);
                pressurePlate.setRotation2FX(FXUtil.TWO_PI_2FX / 360 * ang);
                w.addBody(pressurePlate);
                updateLowestY(y + Math.max(l, thickness));
                break;
            }
        }
        Logger.log("lowestY=", w.lowestY);
    }

    public void sin(int x, int y, int l, int halfPeriods, int offset, int amp) {    //3
        if (amp == 0) {
            line(x, y, x + l, y);
        } else {
            int step = 30;
            int startA = offset;
            int endA = offset + halfPeriods * 180;
            int a = endA - startA;

            int prevPointX = x;
            int prevPointY = y + amp * Mathh.sin(offset) / 1000;
            int nextPointX;
            int nextPointY;

            for (int i = startA; i <= endA; i+=30) {
                nextPointX = x + (i - startA)*l/a;
                nextPointY = y + amp*Mathh.sin(i)/1000;
                line1(prevPointX, prevPointY, nextPointX, nextPointY);
                prevPointX = nextPointX;
                prevPointY = nextPointY;
            }

            if (a % step != 0) {
                nextPointX = x + l;
                nextPointY = y + amp*Mathh.sin(endA)/1000;
                line1(prevPointX, prevPointY, nextPointX, nextPointY);
            }
        }
        updateLowestY(y + amp);
    }
    public void arc(int x, int y, int r, int ang, int of) {
        arc(x, y, r, ang, of, 10, 10);
    }
    public void arc(int x, int y, int r, int ang, int of, int kx, int ky) { //k: 100 = 1.0
        // calculated formula. r=20: sn=5,sl=72; r=1000: sn=36,sl=10
        int sl=10000/(140+r);
        sl = Math.min(72, Math.max(10, sl));

        while (of < 0) {
            of += 360;
        }

        int linesFacing = 0;
        if (ang == 360) {
            linesFacing = 1; // these lines push bodies only in one direction
        }

        int lastAng = 0;
        for(int i = 0; i <= ang - sl; i+=sl) {
            line(x+Mathh.cos(i+of)*kx*r/10000, y+Mathh.sin(i+of)*ky*r/10000, x+Mathh.cos(i+sl+of)*kx*r/10000,y+Mathh.sin(i+sl+of)*ky*r/10000, linesFacing);
            lastAng = i + sl;
        }

        // close the circle if the angle is not multiple of the step (sl)
        if (ang % sl != 0) {
            line(x+Mathh.cos(lastAng+of)*kx*r/10000, y+Mathh.sin(lastAng+of)*ky*r/10000, x+Mathh.cos(ang+of)*kx*r/10000,y+Mathh.sin(ang+of)*ky*r/10000, linesFacing);
        }

        updateLowestY(y + r);
    }

    public void line(int x1, int y1, int x2, int y2) {
        line(x1, y1, x2, y2, 0);
    }

    public void line1(int x1, int y1, int x2, int y2) {
        line(x1, y1, x2, y2, 1);
    }

    //int prevLineK = Integer.MIN_VALUE;
    public void line(int x1, int y1, int x2, int y2, int facing) {
        //x1 += 1;
        //System.out.println(x1 + " " + x2);
        int dx = x2-x1;
        int dy = y2-y1;
        if (dx == 0 && dy == 0) {
            return;
        }

        /*
        * experimental optimization. instead of adding a new line with same
        * tilt angle, move end point of previous line.
        * It is buggy when it concatenates a line with line from
        * another (previous) structure. That's why I disabled it
        *
        int lineK;
        if (dx != 0) {
            lineK = 1000*dy/dx; // TODO: experiment with "1000"
        } else {
            lineK = Integer.MIN_VALUE;
        }
        if (lineK == prevLineK) {
            int prevLineEndPointID = lndscp.segmentCount()-1;
            int prevLineEndPointX = lndscp.elementEndPoints()[prevLineEndPointID].xAsInt();
            int prevLineEndPointY = lndscp.elementEndPoints()[prevLineEndPointID].yAsInt();
            if (x1 == prevLineEndPointX && y1 == prevLineEndPointY) {
                lndscp.elementEndPoints()[prevLineEndPointID] = FXVector.newVector(x2, y2);
                int prevStructID = structlogger.getElementID(structlogger.getNumberOfLogged() - 1);
                structlogger.structLog[prevStructID][1] -= 1;
            }
        } else {*/
        landscape.addSegment(FXVector.newVector(x1, y1), FXVector.newVector(x2, y2), (short) facing);
            /*prevLineK = lineK;
        }*/
        lineCount++;
        updateLowestY(Math.max(y1, y2));
    }

    private void updateLowestY(int y) {
        w.lowestY = Math.max(y, w.lowestY);
    }
}
