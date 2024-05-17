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

import java.awt.*;

import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.Flare;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * A Sprite for a flare, as the name indicates.
 */
class FlareSprite extends Sprite {

    private static final String FILENAME_FLARE_IMAGE = "flare.png";
    private static final Image FLARE_IMAGE;

    static {
        FLARE_IMAGE = ImageUtil.loadImageFromFile(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_FLARE_IMAGE).toString());
    }

    private final Flare flare;

    public FlareSprite(BoardView boardView1, final Flare f) {
        super(boardView1);
        flare = f;
        getBounds();
        image = FLARE_IMAGE;
    }

    @Override
    public Rectangle getBounds() {
        Dimension dim = new Dimension(bv.hex_size.width, bv.hex_size.height);
        bounds = new Rectangle(dim);
        bounds.setLocation(bv.getHexLocation(flare.position));
        return bounds;
    }

    @Override
    public void prepare() { }

    @Override
    public StringBuffer getTooltip() {
        return new StringBuffer(Messages.getString("BoardView1.flare", flare.turnsToBurn));
    }
}