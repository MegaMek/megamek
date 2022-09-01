/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike.cardDrawer;

import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.alphaStrike.ASCardDisplayable;

import java.awt.*;

/**
 * This class acts as a super class for heat tracking AlphaStrike card classes (for Meks and Fighters) as
 * these use a different layout from the other cards including the added heat bar.
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
        drawBox(g, 36, 372, BOX_WIDTH_WIDE, height, Color.LIGHT_GRAY, BOX_STROKE);

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
