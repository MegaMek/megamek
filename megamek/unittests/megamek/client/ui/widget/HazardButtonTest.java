/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class HazardButtonTest {

    @Test
    void transparentPixelsStayTransparent() {
        int transparent = new Color(0, 0, 0, 0).getRGB();
        assertEquals(transparent, HazardButton.tint(transparent));
    }

    @Test
    void greyFacePixelsBecomeRed() {
        Color tinted = new Color(HazardButton.tint(new Color(90, 90, 90).getRGB()), true);
        assertTrue(tinted.getRed() > (2 * tinted.getGreen()), "red should dominate, was " + tinted);
        assertTrue(tinted.getRed() > (2 * tinted.getBlue()), "red should dominate, was " + tinted);
        assertEquals(255, tinted.getAlpha());
    }

    @Test
    void darkerGreyGivesDarkerRed() {
        Color shadow = new Color(HazardButton.tint(new Color(40, 40, 40).getRGB()), true);
        Color face = new Color(HazardButton.tint(new Color(120, 120, 120).getRGB()), true);
        assertTrue(shadow.getRed() < face.getRed(), "shadow " + shadow + " should be darker than face " + face);
    }

    @Test
    void warmFramePixelsBecomeYellow() {
        // The skin's bevel is orange: much more red than blue.
        Color tinted = new Color(HazardButton.tint(new Color(200, 120, 60).getRGB()), true);
        assertTrue(tinted.getGreen() > 150, "yellow needs strong green, was " + tinted);
        assertTrue(tinted.getRed() >= tinted.getGreen(), "yellow keeps red at least as strong, was " + tinted);
        assertEquals(0, tinted.getBlue());
    }
}
