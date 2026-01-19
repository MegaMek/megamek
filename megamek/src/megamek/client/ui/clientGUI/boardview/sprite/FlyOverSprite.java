/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.ImageObserver;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * Sprite and info for an aero flyover route. Does not actually use the image buffer as this can be horribly inefficient
 * for long diagonal lines.
 */
public class FlyOverSprite extends Sprite {

    private Polygon flyOverPoly = null;

    protected Entity en;

    Color spriteColor;

    public FlyOverSprite(BoardView boardView1, final Entity e) {
        super(boardView1);
        en = e;
        spriteColor = e.getOwner().getColour().getColour();
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

        Point currPoint = bv.getCentreHexLocation(curr, true);
        final double lw = bv.getScale() * BoardView.FLY_OVER_LINE_WIDTH;

        // This is a bend
        double diff;
        if (forward) {
            diff = nextAngle - prevAngle;
        } else {
            diff = prevAngle - nextAngle;
        }
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
     * Creates the flyover polygon, which is essentially a wide line from the part of the fly path to the end.
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
        final double lw = bv.getScale() * BoardView.FLY_OVER_LINE_WIDTH;
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
        angle = (curr.radian(next) + Math.PI) % (2 * Math.PI);
        flyOverPoly.addPoint(currPoint.x + (int) (Math.cos(angle) * lw + 0.5),
              currPoint.y + (int) (Math.sin(angle) * lw + 0.5));

        // Now go in reverse order - to add second half of points
        // Handle Last Coords - only draw to the hex edge
        curr = en.getPassedThrough().get(numPassedThrough - 1);
        next = en.getPassedThrough().get(numPassedThrough - 2);
        currPoint = bv.getCentreHexLocation(curr, true);
        nextPoint = bv.getCentreHexLocation(next, true);
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
        makePoly();
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

        Graphics2D g2 = (Graphics2D) g;
        g.setColor(spriteColor);
        g.fillPolygon(drawPoly);
        if (en.equals(bv.getSelectedEntity())) {
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
