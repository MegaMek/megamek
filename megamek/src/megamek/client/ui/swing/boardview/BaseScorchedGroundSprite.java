/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.tileset.HexTileset;
import megamek.common.Coords;
import megamek.common.Terrains;
import megamek.common.util.ImageUtil;

import java.awt.*;

/**
 * Contains common functionality for wreck sprites (currently isometric and regular)
 * @author Luana Coppio
 */
public abstract class BaseScorchedGroundSprite extends Sprite {
    protected Coords coords;
    protected Coords offset;
    protected int radius;

    public BaseScorchedGroundSprite(BoardView boardView, Coords coords, Coords offset, int radius) {
        super(boardView);
        this.coords = coords;
        this.offset = offset;
        this.radius = radius;
    }

    @Override
    public Rectangle getBounds() {
        // Start with the hex and add the label
        bounds = new Rectangle(0, 0, bv.hex_size.width, bv.hex_size.height);

        // Move to board position, save this origin for correct drawing
        Point hexOrigin = bounds.getLocation();
        Point ePos = bv.getHexLocation(coords);
        bounds.setLocation(hexOrigin.x + ePos.x, hexOrigin.y + ePos.y);

        return bounds;
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        if (bv.game.getBoard().getHex(coords).containsAnyTerrainOf(Terrains.ULTRA_SUBLEVEL, Terrains.MAGMA)) {
            return;
        }
        // create image for buffer
        image = ImageUtil.createAcceleratedImage(HexTileset.HEX_W, HexTileset.HEX_H);
        Graphics2D graph = (Graphics2D) image.getGraphics();

        // if the entity is underwater or would sink underwater, we want to make the wreckage translucent
        // so it looks like it sunk
        boolean entityIsUnderwater = bv.game.getBoard().getHex(coords).containsTerrain(Terrains.WATER);

        if (entityIsUnderwater) {
            graph.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.35f));
        }
        int y = offset.getY();
        if (coords.getX() % 2 == 0) {
            y = Math.max(y - 1, 0);
        }
        Image destroyed = bv.tileManager.bottomLayerExplosionMarker(offset.getX(), y, radius * 2 + 1);
        graph.drawImage(destroyed, 0, 0, this);

        if (entityIsUnderwater) {
            graph.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
        }

        // create final image
        image = bv.getScaledImage(image, false);
        graph.dispose();
    }

    public Coords getPosition() {
        return coords;
    }
}
