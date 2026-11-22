package dev.qwxon.bitsntracks.content.suspension;

final class BntBogiePose {
    static final int PARTS = 10;
    private static final float PIVOT_X = -0.5F;
    private static final float PIVOT_Y = 0.20711F;
    private static final float TOP_X = -0.5F;
    private static final float TOP_Y = 1.20711F;
    private static final float SHAFT_GAP = 0.73218F;
    private static final float NOMOVE_TOP = 0.79281F;
    private static final float NOMOVE_BOTTOM = 0.08188F;
    final double[] affine = new double[PARTS * 6];
    final double[] turn = new double[PARTS * 2];
    final double[] follow = new double[PARTS * 3];

    BntBogiePose(double leadX, double leadY, double partnerX, double partnerY, double drop) {
        this(leadX, leadY, partnerX, partnerY, drop, 0.0, 0.0);
    }

    BntBogiePose(double leadX, double leadY, double partnerX, double partnerY, double drop, double shiftX, double shiftY) {
        this.set(0, 1.0, 0.0, 0.0, 1.0, shiftX, shiftY, 0.0);
        double span = NOMOVE_TOP - NOMOVE_BOTTOM;
        double stretch = (span + drop) / span;
        this.set(1, 1.0, 0.0, 0.0, stretch, shiftX, NOMOVE_TOP * (1.0 - stretch) + shiftY, 0.0);
        this.follow[2] = 1.0;
        this.follow[5] = 1.0;
        this.side(0, 0.0, leadX, leadY, drop, shiftX, shiftY);
        this.side(1, -1.0, partnerX, partnerY, drop, shiftX, shiftY);
    }

    private void side(int side, double restX, double dx, double dy, double drop, double shiftX, double shiftY) {
        double cogX = restX + dx;
        double cogY = dy - drop;
        double pivotX = PIVOT_X + shiftX;
        double pivotY = PIVOT_Y + shiftY - drop;
        double topX = TOP_X + shiftX;
        double topY = TOP_Y + shiftY;

        double swing = this.along(2 + side, PIVOT_X, PIVOT_Y, restX, 0.0, pivotX, pivotY, cogX, cogY, 1.0);
        this.rotate(8 + side, restX, 0.0, swing, cogX - restX, cogY);

        double lean = Math.atan2(cogY - topY, cogX - topX) - Math.atan2(-TOP_Y, restX - TOP_X);
        this.rotate(4 + side, TOP_X, TOP_Y, lean, shiftX, shiftY);

        double rest = Math.hypot(TOP_X - restX, TOP_Y);
        double now = Math.hypot(topX - cogX, topY - cogY);
        double reach = Math.max(1.0, (now - SHAFT_GAP) / (rest - SHAFT_GAP));
        this.along(6 + side, restX, 0.0, TOP_X, TOP_Y, cogX, cogY, topX, topY, reach * rest / now);

        this.follow(2 + side, cogX, cogY, pivotX, pivotY);
        this.follow(4 + side, cogX, cogY, topX, topY);
        this.follow(6 + side, cogX, cogY, topX, topY);
    }

    private void follow(int part, double cogX, double cogY, double frameX, double frameY) {
        double dx = frameX - cogX;
        double dy = frameY - cogY;
        double square = dx * dx + dy * dy;
        if (square < 1.0E-8) {
            return;
        }
        int at = part * 3;
        this.follow[at] = dx / square;
        this.follow[at + 1] = dy / square;
        this.follow[at + 2] = -(cogX * dx + cogY * dy) / square;
    }

    private double along(
        int part, double fromX, double fromY, double toX, double toY,
        double nowFromX, double nowFromY, double nowToX, double nowToY, double extra
    ) {
        double restLength = Math.hypot(toX - fromX, toY - fromY);
        double length = Math.hypot(nowToX - nowFromX, nowToY - nowFromY);
        double u0x = (toX - fromX) / restLength;
        double u0y = (toY - fromY) / restLength;
        double ux = (nowToX - nowFromX) / length;
        double uy = (nowToY - nowFromY) / length;
        double stretch = length / restLength * extra;
        double a = stretch * ux * u0x + uy * u0y;
        double b = stretch * ux * u0y - uy * u0x;
        double c = stretch * uy * u0x - ux * u0y;
        double d = stretch * uy * u0y + ux * u0x;
        double angle = Math.atan2(uy, ux) - Math.atan2(u0y, u0x);
        this.set(part, a, b, c, d, nowFromX - a * fromX - b * fromY, nowFromY - c * fromX - d * fromY, angle);
        return angle;
    }

    private void rotate(int part, double originX, double originY, double angle, double shiftX, double shiftY) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        this.set(part, cos, -sin, sin, cos,
            originX - cos * originX + sin * originY + shiftX,
            originY - sin * originX - cos * originY + shiftY, angle);
    }

    private void set(int part, double a, double b, double c, double d, double tx, double ty, double angle) {
        int at = part * 6;
        this.affine[at] = a;
        this.affine[at + 1] = b;
        this.affine[at + 2] = c;
        this.affine[at + 3] = d;
        this.affine[at + 4] = tx;
        this.affine[at + 5] = ty;
        this.turn[part * 2] = Math.cos(angle);
        this.turn[part * 2 + 1] = Math.sin(angle);
    }
}
