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

import java.awt.Graphics2D;

import megamek.client.ui.util.StringDrawer;
import megamek.common.alphaStrike.ASCardDisplayable;

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
