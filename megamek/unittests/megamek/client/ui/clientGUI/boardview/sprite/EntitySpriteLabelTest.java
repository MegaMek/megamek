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
package megamek.client.ui.clientGUI.boardview.sprite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers the Mek name shown by the abbreviated unit label styles (issue 9481): a Clan OmniMek must not shrink to its
 * configuration letter.
 */
class EntitySpriteLabelTest {

    @Test
    void innerSphereDesignationShowsModelOnly() {
        assertEquals("MAD-3R", EntitySprite.abbreviatedMekName("Marauder", "MAD-3R"));
        assertEquals("AV1-OA", EntitySprite.abbreviatedMekName("Avatar", "AV1-OA"));
    }

    @Test
    void omniConfigurationKeepsChassis() {
        assertEquals("Mad Cat Prime", EntitySprite.abbreviatedMekName("Mad Cat", "Prime"));
        assertEquals("Thor A", EntitySprite.abbreviatedMekName("Thor", "A"));
    }

    @Test
    void shortHyphenatedConfigurationKeepsChassis() {
        assertEquals("Pariah A-Z", EntitySprite.abbreviatedMekName("Pariah", "A-Z"));
        assertEquals("Ha Otoko -HR", EntitySprite.abbreviatedMekName("Ha Otoko", "-HR"));
    }

    @Test
    void numberedModelKeepsChassis() {
        assertEquals("Supernova 2", EntitySprite.abbreviatedMekName("Supernova", "2"));
    }

    @Test
    void blankModelShowsChassis() {
        assertEquals("Hellion", EntitySprite.abbreviatedMekName("Hellion", ""));
        assertEquals("Hellion", EntitySprite.abbreviatedMekName("Hellion", "  "));
        assertEquals("Hellion", EntitySprite.abbreviatedMekName("Hellion", null));
    }
}
