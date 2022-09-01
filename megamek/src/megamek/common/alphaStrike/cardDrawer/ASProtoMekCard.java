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

public class ASProtoMekCard extends ASCard {

    public ASProtoMekCard(ASCardDisplayable element) {
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

            new StringDrawer("FIRE CONTROL").at(722, 510).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 510).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 510);
            drawDamagePip(g, 755, 510);
            drawDamagePip(g, 782, 510);
            drawDamagePip(g, 809, 510);

            new StringDrawer("MP").at(722, 552).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("1/2 MV Each").at(834, 552).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 552);
            drawDamagePip(g, 755, 552);
            drawDamagePip(g, 782, 552);
            drawDamagePip(g, 809, 552);

            new StringDrawer("WEAPONS").at(722, 593).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 593).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 593);
            drawDamagePip(g, 755, 593);
            drawDamagePip(g, 782, 593);
            drawDamagePip(g, 809, 593);
        }
    }
}
