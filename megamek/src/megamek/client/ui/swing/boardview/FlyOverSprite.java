package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Coords;
import megamek.common.Entity;

/**
 * Sprite and info for an aero flyover route. Does not actually use the image
 * buffer as this can be horribly inefficient for long diagonal lines.
 */
class FlyOverSprite extends Sprite {

    private Polygon flyOverPoly = null;

    protected Entity en;

    Color spriteColor;

    public FlyOverSprite(BoardView1 boardView1, final Entity e) {
        super(boardView1);
        en = e;
        spriteColor = PlayerColors.getColor(e.getOwner().getColorIndex());
        image = null;
        prepare();
    }

    @Override
    public void prepare() {
        makePoly();
        getBounds();
    }

    private void addPolyPoint(Coords curr, Coords next, Coords prev,
            boolean forward) {
        int newX, newY;
        int prevX, prevY, nextX, nextY;
        double nextAngle = curr.radian(next);
        double prevAngle = prev.radian(curr);

        Point currPoint = this.bv.getCentreHexLocation(curr, true);
        final double lw = bv.scale * BoardView1.FLY_OVER_LINE_WIDTH;

        // This is a bend
        double diff;
        if (forward) {
            diff = nextAngle - prevAngle;
        } else {
            diff = prevAngle - nextAngle;
        }
        diff = nextAngle - prevAngle;
        if (Math.abs(diff) > 0.001) {
            // Inside Corner - Add one point
            double bendAngle = (Math.PI + diff);
            if (bendAngle > (2 * Math.PI)) {
                bendAngle -= 2 * Math.PI;
            } else if (bendAngle < 0) {
                bendAngle += 2 * Math.PI;
            }
            // Outside Corner
            if (bendAngle < Math.PI) {
                newX = currPoint.x + (int) (Math.cos(prevAngle) * lw + 0.5);
                newY = currPoint.y + (int) (Math.sin(prevAngle) * lw + 0.5);
                flyOverPoly.addPoint(newX, newY);
                newX = currPoint.x + (int) (Math.cos(nextAngle) * lw + 0.5);
                newY = currPoint.y + (int) (Math.sin(nextAngle) * lw + 0.5);
                flyOverPoly.addPoint(newX, newY);
            } else { // Inside corner
                prevX = currPoint.x + (int) (Math.cos(prevAngle) * lw + 0.5);
                prevY = currPoint.y + (int) (Math.sin(prevAngle) * lw + 0.5);
                nextX = currPoint.x + (int) (Math.cos(nextAngle) * lw + 0.5);
                nextY = currPoint.y + (int) (Math.sin(nextAngle) * lw + 0.5);
                int d = prevX - nextX;
                if ((prevY < nextY && prevX < nextX)
                        || (prevY > nextY && prevX > nextX)) {
                    d *= -1;
                }
                flyOverPoly.addPoint(nextX + d, nextY);
                flyOverPoly.addPoint(prevX + d, prevY);
            }
        } else { // Not a bend
            // Only need to add one
            newX = currPoint.x + (int) (Math.cos(nextAngle) * lw + 0.5);
            newY = currPoint.y + (int) (Math.sin(nextAngle) * lw + 0.5);
            flyOverPoly.addPoint(newX, newY);
        }
    }

    /**
     * Creates the flyover polygon, which is essentially a wide line from the
     * part of the fly path to the end.
     */
    private void makePoly() {
        // make polygon
        flyOverPoly = new Polygon();

        // Check for degenerate case
        if ((en.getPosition() == null) || (en.getPassedThrough().size() < 2)) {
            flyOverPoly = new Polygon();
            flyOverPoly.addPoint(0, 0);
            flyOverPoly.addPoint(1, 0);
            flyOverPoly.addPoint(0, 1);
            return;
        }

        // line width
        final double lw = bv.scale * BoardView1.FLY_OVER_LINE_WIDTH;
        int numPassedThrough = en.getPassedThrough().size();
        double angle;
        double xDiff, yDiff;
        Coords prev, curr, next;
        Point currPoint, nextPoint;

        // Handle First Coords
        curr = en.getPassedThrough().get(0);
        next = en.getPassedThrough().get(1);
        currPoint = bv.getCentreHexLocation(curr, true);
        angle = curr.radian(next);
        flyOverPoly.addPoint(currPoint.x + (int) (Math.cos(angle) * lw + 0.5),
                currPoint.y + (int) (Math.sin(angle) * lw + 0.5));

        // Handle Middle Coords
        for (int i = 1; i < (numPassedThrough - 1); i++) {
            prev = en.getPassedThrough().get(i - 1);
            curr = en.getPassedThrough().get(i);
            next = en.getPassedThrough().get(i + 1);
            addPolyPoint(curr, next, prev, true);
        }

        // Handle Last Coords - only draw to the hex edge
        curr = en.getPassedThrough().get(numPassedThrough - 1);
        next = en.getPassedThrough().get(numPassedThrough - 2);
        currPoint = bv.getCentreHexLocation(curr, true);
        nextPoint = bv.getCentreHexLocation(next, true);
        if (bv.useIsometric()) {
            xDiff = Math.sqrt(Math.pow(currPoint.x - nextPoint.x, 2));
            yDiff = Math.sqrt(Math.pow(currPoint.y - nextPoint.y, 2));
            if (nextPoint.x > currPoint.x) {
                xDiff *= -1;
            }
            if (nextPoint.y >= currPoint.y) {
                yDiff *= -1;
            }
            currPoint.x = (int) (currPoint.x - xDiff / 2 + 0.5);
            currPoint.y = (int) (currPoint.y - yDiff / 2 + 0.5);
        }
        angle = (curr.radian(next) + Math.PI) % (2 * Math.PI);
        flyOverPoly.addPoint(currPoint.x + (int) (Math.cos(angle) * lw + 0.5),
                currPoint.y + (int) (Math.sin(angle) * lw + 0.5));

        // Now go in reverse order - to add second half of points
        // Handle Last Coords - only draw to the hex edge
        curr = en.getPassedThrough().get(numPassedThrough - 1);
        next = en.getPassedThrough().get(numPassedThrough - 2);
        currPoint = bv.getCentreHexLocation(curr, true);
        nextPoint = bv.getCentreHexLocation(next, true);
        if (bv.useIsometric()) {
            xDiff = Math.sqrt(Math.pow(currPoint.x - nextPoint.x, 2));
            yDiff = Math.sqrt(Math.pow(currPoint.y - nextPoint.y, 2));
            if (nextPoint.x > currPoint.x) {
                xDiff *= -1;
            }
            if (nextPoint.y >= currPoint.y) {
                yDiff *= -1;
            }
            currPoint.x = (int) (currPoint.x - xDiff / 2 + 0.5);
            currPoint.y = (int) (currPoint.y - yDiff / 2 + 0.5);
        }
        angle = curr.radian(next);
        flyOverPoly.addPoint(currPoint.x + (int) (Math.cos(angle) * lw + 0.5),
                currPoint.y + (int) (Math.sin(angle) * lw + 0.5));

        // Handle Middle Coords (in reverse)
        for (int i = (numPassedThrough - 2); i > 0; i--) {
            prev = en.getPassedThrough().get(i + 1);
            curr = en.getPassedThrough().get(i);
            next = en.getPassedThrough().get(i - 1);
            addPolyPoint(curr, next, prev, false);
        }

        // Handle First Coords
        curr = en.getPassedThrough().get(0);
        prev = en.getPassedThrough().get(1);
        currPoint = bv.getCentreHexLocation(curr, true);
        angle = prev.radian(curr);
        flyOverPoly.addPoint(currPoint.x + (int) (Math.cos(angle) * lw + 0.5),
                currPoint.y + (int) (Math.sin(angle) * lw + 0.5));
    }

    @Override
    public Rectangle getBounds() {
        if (true) {
            makePoly();
        }
        // set bounds
        bounds = new Rectangle(flyOverPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
        return bounds;
    }

    @Override
    public boolean isReady() {
        return flyOverPoly != null;
    }

    public int getEntityId() {
        return en.getId();
    }

    public Entity getEntity() {
        return en;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        Polygon drawPoly = new Polygon(flyOverPoly.xpoints, flyOverPoly.ypoints,
                flyOverPoly.npoints);

        g.setColor(spriteColor);
        g.fillPolygon(drawPoly);
        if (en.equals(bv.selectedEntity) && (g instanceof Graphics2D)) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1));
            g2.setColor(Color.blue);
            g2.drawPolygon(drawPoly);
            g2.setStroke(oldStroke);
        } else {
            g.setColor(Color.black);
            g.drawPolygon(drawPoly);
        }
    }

    /**
     * Return true if the point is inside our polygon
     */
    @Override
    public boolean isInside(Point point) {
        return flyOverPoly.contains(point.x - bounds.x, point.y - bounds.y);
    }

}