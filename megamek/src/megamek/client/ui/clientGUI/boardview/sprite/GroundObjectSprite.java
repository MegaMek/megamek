/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.board.Coords;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Represents cargo that can be picked up from the ground.
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
     * @param loc        - Hex location coordinates of building or bridge where warning will be visible.
     */
    public GroundObjectSprite(BoardView boardView1, Coords loc) {
        super(boardView1, loc);
        image = CARGO_IMAGE;
    }

    @Override
    public Rectangle getBounds() {
        Dimension dim = new Dimension(bv.getHexSize().width, bv.getHexSize().height);
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
        graph.scale(bv.getScale(), bv.getScale());
        graph.drawImage(CARGO_IMAGE, 0, 0, null);
    }
}
