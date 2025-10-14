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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.StraightArrowPolygon;
import megamek.common.compute.Compute;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * Sprite and info for movement vector (AT2 advanced movement). Does not actually use the image buffer as this can be
 * horribly inefficient for long diagonal lines.
 * <p>
 * Appears as angle arrow pointing to the hex this entity will move to based on current movement vectors.
 * <p>
 * TODO: Different color depending upon whether
 * entity has already moved this turn
 */
public class MovementSprite extends Sprite {

    private final double angle;
    private StraightArrowPolygon movePoly;
    private Color moveColor;

    private final Coords start;
    private final Coords end;
    private int vel;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public MovementSprite(BoardView boardView, Entity entity, int[] v, Color color, boolean isCurrent) {
        super(boardView);
        // private MovementVector mv;
        // get the starting and ending position
        start = entity.getPosition();
        end = Compute.getFinalPosition(start, v);

        // what is the velocity
        vel = 0;
        for (int element : v) {
            vel += element;
        }

        // color?
        // player colors
        moveColor = entity.getOwner().getColour().getColour();
        // TODO: Its not going transparent. Oh well, it is a minor issue at the moment
        /*
         * if (isCurrent) { int colour = color.getRGB(); int transparency =
         * GUIPreferences.getInstance().getInt(GUIPreferences.
         * ADVANCED_ATTACK_ARROW_TRANSPARENCY); moveColor = new Color(colour
         * | (transparency << 24), true); }
         */
        // red if offboard
        if (!this.bv.game.getBoard().contains(end)) {
            int colour = 0xff0000; // red
            int transparency = GUIP.getAttackArrowTransparency();
            moveColor = new Color(colour | (transparency << 24), true);
        }
        // dark gray if done
        if (entity.isDone()) {
            int colour = 0x696969; // gray
            int transparency = GUIP.getAttackArrowTransparency();
            moveColor = new Color(colour | (transparency << 24), true);
        }

        // angle of line connecting two hexes
        angle = (start.radian(end) + (Math.PI * 1.5)) % (Math.PI * 2); // angle
        makePoly();

        // set bounds
        bounds = new Rectangle(movePoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
              bounds.getSize().height + 1);
        // move poly to upper right of image
        movePoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

        // nullify image
        image = null;
    }

    private void makePoly() {
        // make a polygon
        Point a = bv.getHexLocation(start);
        Point t = bv.getHexLocation(end);
        // OK, that is actually not good. I do not like hard coded figures.
        // HEX_W/2 - x distance in pixels from origin of hex bounding box to
        // the center of hex.
        // HEX_H/2 - y distance in pixels from origin of hex bounding box to
        // the center of hex.
        // 18 - is actually 36/2 - we do not want arrows to start and end
        // directly
        // in the centers of hex and hiding mek under.

        a.x = a.x + (int) ((HexTileset.HEX_W / 2.0f) * bv.getScale())
              + (int) Math.round(Math.cos(angle) * (int) (18 * bv.getScale()));
        t.x = (t.x + (int) ((HexTileset.HEX_W / 2.0f) * bv.getScale()))
              - (int) Math.round(Math.cos(angle) * (int) (18 * bv.getScale()));
        a.y = a.y + (int) ((HexTileset.HEX_H / 2.0f) * bv.getScale())
              + (int) Math.round(Math.sin(angle) * (int) (18 * bv.getScale()));
        t.y = (t.y + (int) ((HexTileset.HEX_H / 2.0f) * this.bv.getScale()))
              - (int) Math.round(Math.sin(angle) * (int) (18 * bv.getScale()));
        movePoly = new StraightArrowPolygon(a, t, (int) (4 * bv.getScale()),
              (int) (8 * bv.getScale()), false);
    }

    @Override
    public Rectangle getBounds() {
        makePoly();
        // set bounds
        bounds = new Rectangle(movePoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
              bounds.getSize().height + 1);
        // move poly to upper right of image
        movePoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

        return bounds;
    }

    @Override
    public void prepare() {

    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        // don't draw anything if the unit has no velocity

        if (vel == 0) {
            return;
        }

        AttackSprite.createPolygon(g, x, y, movePoly, moveColor);

    }

    /**
     * Return true if the point is inside our polygon
     */
    @Override
    public boolean isInside(Point point) {
        return movePoly.contains(point.x - bounds.x, point.y - bounds.y);
    }
}
