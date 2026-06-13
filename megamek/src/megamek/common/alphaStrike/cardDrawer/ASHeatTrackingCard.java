/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.alphaStrike.cardDrawer;

import java.awt.Color;
import java.awt.Graphics2D;

import megamek.client.ui.util.StringDrawer;
import megamek.common.alphaStrike.ASCardDisplayable;

/**
 * This class acts as a super class for heat tracking AlphaStrike card classes (for Meks and Fighters) as these use a
 * different layout from the other cards including the added heat bar.
 */
public class ASHeatTrackingCard extends ASCard {

    protected ASHeatTrackingCard(ASCardDisplayable element) {
        super(element);
    }

    @Override
    protected void initialize() {
        super.initialize();
        setHeatTrackingLayout();
        fluffYCenter = 277;
        fluffHeight = 318;
    }

    private void setHeatTrackingLayout() {
        baseInfoBoxHeight = 84;
        damageBoxY = 268;
        damageBoxHeight = 88;
        armorBoxY = 442;
        armorBoxHeight = 79;
        specialBoxY = 537;
        specialBoxHeight = 84;
    }

    @Override
    protected void paintHeat(Graphics2D g) {
        int height = 54;
        drawBox(g, 36, 372, BOX_WIDTH_WIDE, height, BACKGROUND_GRAY, BOX_STROKE);

        g.drawLine(36 + 141, 372 + 12, 36 + 141, 372 + height - 12);
        // Heat Scale Box
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(36 + 377, 372 + 6, 210, height - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(36 + 377, 372 + 6, 210, height - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.drawLine(36 + 429, 372 + 7, 36 + 429, 372 + height - 7);
        g.drawLine(36 + 482, 372 + 7, 36 + 482, 372 + height - 7);
        g.drawLine(36 + 535, 372 + 7, 36 + 535, 372 + height - 7);
        int ym = 372 + height / 2;
        g.setFont(headerFont);
        g.setColor(Color.WHITE);
        new StringDrawer("1").at(36 + 403, ym).center().draw(g);
        new StringDrawer("2").at(36 + 455, ym).center().draw(g);
        new StringDrawer("3").at(36 + 508, ym).center().draw(g);
        new StringDrawer("S").at(36 + 561, ym).center().draw(g);
        g.setColor(Color.BLACK);

        if (element != null) {
            new StringDrawer("OV:").at(49, ym).centerY().font(headerFont).maxWidth(45).draw(g);
            new StringDrawer(element.getOV() + "").at(111, ym).useConfig(valueConfig).maxWidth(54).draw(g);
            new StringDrawer("HEAT SCALE").at(193, ym).centerY().font(headerFont).maxWidth(208).draw(g);
        }
    }

    @Override
    protected void paintSpecial(Graphics2D g) {
        drawBox(g, specialBoxX, specialBoxY, specialBoxWidth, specialBoxHeight, BACKGROUND_GRAY, BOX_STROKE);
        paintSpecialTextLines(g, element, specialsFont, specialBoxX + 8, specialBoxY + 2,
              specialBoxWidth - 16, 24);
    }
}
