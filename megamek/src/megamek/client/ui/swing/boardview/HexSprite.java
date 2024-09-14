/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;

import megamek.client.ui.swing.tileset.HexTileset;
import megamek.common.Coords;

/**
 * An ancestor class for all Sprites that can be enclosed within a single hex.
 *
 * @author Saginatio
 */
public abstract class HexSprite extends Sprite {

    protected Coords loc;

    public HexSprite(BoardView boardView1, Coords loc) {
        super(boardView1);
        this.loc = loc;
        updateBounds();
    }

    public Coords getPosition() {
        return loc;
    }

    protected void updateBounds() {
        bounds = new Rectangle(
                (int) (HexTileset.HEX_W * bv.scale),
                (int) (HexTileset.HEX_H * bv.scale));
        bounds.setLocation(bv.getHexLocation(loc));
    }

    /**
     * Creates a new empty transparent image for this HexSprite. The
     * size follows the current values of <code>bounds</code>.
     */
    protected Image createNewHexImage() {
        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();

        // a compatible image should be ideal for blitting to the screen
        return config.createCompatibleImage(bounds.width, bounds.height,
                Transparency.TRANSLUCENT);
    }

    /**
     * Returns true when this Sprite should be hidden by overlapping terrain in
     * isometric mode,
     * i.e. hidden behind mountains.
     * By default, this method returns true.
     *
     * @return True for Sprites that should be hidden by overlapping terrain in
     *         isometric mode
     */
    protected boolean isBehindTerrain() {
        return true;
    }
}
