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
import megamek.common.alphaStrike.AlphaStrikeElement;

import java.awt.*;

public class ASVehicleCard extends ASCard {

    public ASVehicleCard(ASCardDisplayable element) {
        super(element);
    }

    @Override
    protected void initialize() {
        super.initialize();
        armorBoxWidth = 531;
        specialBoxWidth = 531;
        fluffYCenter = 277;
        fluffHeight = 318;
    }

    @Override
    protected void paintHits(Graphics2D g) {
        drawBox(g, 591, 442, 422, 180, BACKGROUND_GRAY, BOX_STROKE);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).maxWidth(380).draw(g);

            new StringDrawer("ENGINE").at(722, 509).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("1/2 MV and Damage").at(754, 509).centerY().font(specialsFont).maxWidth(248).draw(g);
            drawDamagePip(g, 728, 509);

            new StringDrawer("FIRE CONTROL").at(722, 538).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 538).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 538);
            drawDamagePip(g, 755, 538);
            drawDamagePip(g, 782, 538);
            drawDamagePip(g, 809, 538);

            new StringDrawer("WEAPONS").at(722, 565).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 565).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 565);
            drawDamagePip(g, 755, 565);
            drawDamagePip(g, 782, 565);
            drawDamagePip(g, 809, 565);

            new StringDrawer("MOTIVE").at(663, 593).useConfig(hitsTitleConfig).maxWidth(64).draw(g);
            drawDamagePip(g, 673, 593);
            drawDamagePip(g, 700, 593);
            new StringDrawer("-2 MV").at(724, 593).centerY().font(specialsFont).maxWidth(62).draw(g);
            drawDamagePip(g, 793, 593);
            drawDamagePip(g, 820, 593);
            new StringDrawer("1/2 MV").at(841, 593).centerY().font(specialsFont).maxWidth(64).draw(g);
            drawDamagePip(g, 919, 593);
            new StringDrawer("0 MV").at(944, 593).centerY().font(specialsFont).maxWidth(57).draw(g);
        }
    }
}
