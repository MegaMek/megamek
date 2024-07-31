/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 *  Represents cargo that can be picked up from the ground.
 */
public class GroundObjectSprite extends HexSprite {
	private static final String FILENAME_CARGO_IMAGE = "cargo.png";
    private static final Image CARGO_IMAGE;

    static {
    	CARGO_IMAGE = ImageUtil.loadImageFromFile(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_CARGO_IMAGE).toString());
    }

    /**
     * @param boardView1 - parent BoardView object this sprite will be displayed on.
     * @param loc - Hex location coordinates of building or bridge where warning will be visible.
     */
    public GroundObjectSprite(BoardView boardView1, Coords loc) {
        super(boardView1, loc);
        image = CARGO_IMAGE;
    }
    
    @Override
    public Rectangle getBounds() {
        Dimension dim = new Dimension(bv.hex_size.width, bv.hex_size.height);
        bounds = new Rectangle(dim);
        bounds.setLocation(bv.getHexLocation(loc));
        return bounds;
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.scale, bv.scale);
        graph.drawImage(CARGO_IMAGE, 0, 0, null);
    }
}