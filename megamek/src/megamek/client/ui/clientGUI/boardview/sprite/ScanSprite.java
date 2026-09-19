/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.image.ImageObserver;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.common.board.Coords;

/**
 * The sensor sweep a unit has ordered for the End Phase, drawn from the scanning unit to whatever it was pointed at.
 *
 * <p>An attack is a single arrow because something travels the line. A scan is a sweep, so this draws a run of
 * widening arcs instead, like the ripples on a radar plot, spaced along the line and facing the target. That reads as
 * "this unit is listening to that" rather than "this unit is shooting that", which matters because the two orders can
 * stand at the same time on the same board.</p>
 *
 * @author Claude Code (Opus 5)
 */
public class ScanSprite extends Sprite {

    /** Distance in unscaled pixels between one arc and the next along the line. */
    private static final int ARC_SPACING = 13;

    /** How wide, in unscaled pixels, the last and widest arc is drawn. */
    private static final int WIDEST_ARC = 15;

    /** The arcs stop short of both hexes so neither the scout nor its target is drawn over. */
    private static final int CLEARANCE = 20;

    /** The arc sweep in degrees, centred on the direction of travel. */
    private static final int SWEEP_DEGREES = 70;

    private final Coords scannerPosition;
    private final Coords targetPosition;
    private final Color sweepColor;

    private Point scannerPoint;
    private Point targetPoint;

    /**
     * @param boardView       the board view to draw on
     * @param scannerPosition where the scanning unit stands
     * @param targetPosition  the hex being read, which is the target unit's hex when a unit was picked
     * @param sweepColor      the scanning player's colour, so several players' orders stay apart
     */
    public ScanSprite(BoardView boardView, Coords scannerPosition, Coords targetPosition, Color sweepColor) {
        super(boardView);
        this.scannerPosition = scannerPosition;
        this.targetPosition = targetPosition;
        this.sweepColor = sweepColor;
        image = null;
    }

    /**
     * Works out the two screen points the sweep runs between, pulled in from both hex centres so the arcs sit in the
     * gap rather than over either unit.
     */
    private void locate() {
        double angle = scannerPosition.radian(targetPosition) + (Math.PI * 1.5);
        int clearance = (int) (CLEARANCE * bv.getScale());
        scannerPoint = hexCentre(scannerPosition);
        targetPoint = hexCentre(targetPosition);
        scannerPoint.translate((int) Math.round(Math.cos(angle) * clearance),
              (int) Math.round(Math.sin(angle) * clearance));
        targetPoint.translate((int) Math.round(-Math.cos(angle) * clearance),
              (int) Math.round(-Math.sin(angle) * clearance));
    }

    /**
     * @param coords the hex to locate
     *
     * @return the centre of that hex in board pixels
     */
    private Point hexCentre(Coords coords) {
        Point corner = bv.getHexLocation(coords);
        corner.translate((int) (HexTileset.HEX_W / 2.0 * bv.getScale()),
              (int) (HexTileset.HEX_H / 2.0 * bv.getScale()));
        return corner;
    }

    @Override
    public Rectangle getBounds() {
        locate();
        int pad = (int) ((WIDEST_ARC + 4) * bv.getScale());
        bounds = new Rectangle(Math.min(scannerPoint.x, targetPoint.x) - pad,
              Math.min(scannerPoint.y, targetPoint.y) - pad,
              Math.abs(targetPoint.x - scannerPoint.x) + (pad * 2),
              Math.abs(targetPoint.y - scannerPoint.y) + (pad * 2));
        return bounds;
    }

    @Override
    public void prepare() {
        getBounds();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void drawOnto(Graphics graphics, int x, int y, ImageObserver observer) {
        locate();
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double runX = targetPoint.x - (double) scannerPoint.x;
        double runY = targetPoint.y - (double) scannerPoint.y;
        double length = Math.hypot(runX, runY);
        int spacing = Math.max(4, (int) (ARC_SPACING * bv.getScale()));
        int arcCount = (int) (length / spacing);
        if (arcCount < 1) {
            graphics2D.dispose();
            return;
        }

        // Screen degrees run anticlockwise from east while the screen's y axis runs down, so the sweep's facing is
        // the negated travel angle. Each arc opens toward the target.
        double facingDegrees = Math.toDegrees(Math.atan2(-runY, runX));
        Stroke stroke = new BasicStroke(Math.max(1.4f, (float) (2.0 * bv.getScale())), BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND);
        graphics2D.setStroke(stroke);

        for (int index = 1; index <= arcCount; index++) {
            double along = index / (double) (arcCount + 1);
            double centreX = scannerPoint.x + (runX * along) + x;
            double centreY = scannerPoint.y + (runY * along) + y;
            // the sweep widens as it travels, so the run reads as a direction even on a short line
            double radius = Math.max(2.0, WIDEST_ARC * bv.getScale() * along);
            Arc2D arc = new Arc2D.Double(centreX - radius, centreY - radius, radius * 2, radius * 2,
                  facingDegrees - (SWEEP_DEGREES / 2.0), SWEEP_DEGREES, Arc2D.OPEN);
            // the far arcs are the faintest, so the eye is pulled toward what is being read
            int alpha = Math.min(255, (int) (90 + (140 * along)));
            graphics2D.setColor(new Color(sweepColor.getRed(), sweepColor.getGreen(), sweepColor.getBlue(), alpha));
            graphics2D.draw(arc);
        }
        graphics2D.dispose();
    }

    @Override
    public boolean isInside(Point point) {
        return false;
    }
}
