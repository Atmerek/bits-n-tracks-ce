package dev.qwxon.bitsntracks.content.suspension;

final class BntBogiePose {
    static final int PARTS = 10;
    final double[] affine = new double[PARTS * 6];
    final double[] turn = new double[PARTS * 2];

    BntBogiePose(double leadX, double leadY, double partnerX, double partnerY, double drop) {
        this.set(0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        double span = BntBogieModel.NOMOVE_TOP - BntBogieModel.NOMOVE_BOTTOM;
        double stretch = (span + drop) / span;
        this.set(1, 1.0, 0.0, 0.0, stretch, 0.0, BntBogieModel.NOMOVE_TOP * (1.0 - stretch), 0.0);
        this.side(0, 0.0, leadX, leadY, drop);
        this.side(1, -1.0, partnerX, partnerY, drop);
    }

    private void side(int side, double restX, double dx, double dy, double drop) {
        double pivotX = BntBogieModel.PIVOT_X;
        double pivotY = BntBogieModel.PIVOT_Y;
        double topX = BntBogieModel.TOP_X;
        double topY = BntBogieModel.TOP_Y;
        double cogX = restX + dx;
        double cogY = dy - drop;

        double swing = this.along(2 + side, pivotX, pivotY, restX, 0.0, pivotX, pivotY - drop, cogX, cogY, 1.0);
        this.rotate(8 + side, restX, 0.0, swing, cogX - restX, cogY);

        double lean = Math.atan2(cogY - topY, cogX - topX) - Math.atan2(-topY, restX - topX);
        this.rotate(4 + side, topX, topY, lean, 0.0, 0.0);

        double rest = Math.hypot(topX - restX, topY);
        double now = Math.hypot(topX - cogX, topY - cogY);
        double reach = Math.max(1.0, (now - BntBogieModel.SHAFT_GAP) / (rest - BntBogieModel.SHAFT_GAP));
        this.along(6 + side, restX, 0.0, topX, topY, cogX, cogY, topX, topY, reach * rest / now);
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
