/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
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
              (int) (HexTileset.HEX_W * bv.getScale()),
              (int) (HexTileset.HEX_H * bv.getScale()));
        bounds.setLocation(bv.getHexLocation(loc));
    }

    /**
     * Creates a new empty transparent image for this HexSprite. The size follows the current values of
     * <code>bounds</code>.
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
     * Returns true when this Sprite should be hidden by overlapping terrain in isometric mode, i.e. hidden behind
     * mountains. By default, this method returns true.
     *
     * @return True for Sprites that should be hidden by overlapping terrain in isometric mode
     */
    public boolean isBehindTerrain() {
        return true;
    }
}
