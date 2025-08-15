/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Font;
import java.awt.Rectangle;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.Entity;

/**
 * Sprite for an wreck. Consists of an image, drawn from the Tile Manager and an identification label.
 */
public class WreckSprite extends AbstractWreckSprite {
    public WreckSprite(BoardView boardView1, final Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
        this.secondaryPos = secondaryPos;

        String shortName = entity.getShortName();

        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47, 55, bv.getPanel().getFontMetrics(font).stringWidth(shortName) + 1,
              bv.getPanel().getFontMetrics(font).getAscent());
        Rectangle tempBounds = new Rectangle(bv.getHexSize()).union(modelRect);
        if (secondaryPos == -1) {
            tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos)));
        }

        bounds = tempBounds;
        image = null;
    }
}
