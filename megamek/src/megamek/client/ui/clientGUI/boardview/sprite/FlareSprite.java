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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.Configuration;
import megamek.common.equipment.Flare;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * A Sprite for a flare, as the name indicates.
 */
public class FlareSprite extends Sprite {

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
        Dimension dim = new Dimension(bv.getHexSize().width, bv.getHexSize().height);
        bounds = new Rectangle(dim);
        bounds.setLocation(bv.getHexLocation(flare.position));
        return bounds;
    }

    @Override
    public void prepare() {}

    @Override
    public StringBuffer getTooltip() {
        return new StringBuffer(Messages.getString("BoardView1.flare", flare.turnsToBurn));
    }
}
