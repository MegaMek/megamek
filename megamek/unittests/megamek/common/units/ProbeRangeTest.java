/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * How far each active probe reaches. A Bloodhound reaches 8 hexes (Core Rulebook p. 233); it used to be given the
 * generic 4 because the lookup compared its display name against an internal-name constant and never matched, which
 * a playtester noticed as a scout that could only scan 4 hexes.
 */
class ProbeRangeTest {

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private Entity mekCarrying(String internalName) throws Exception {
        EquipmentType probe = EquipmentType.get(internalName);
        assertNotNull(probe, internalName + " is in the equipment list");
        BipedMek mek = new BipedMek();
        mek.setGame(new Game());
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.addEquipment(probe, Mek.LOC_CENTER_TORSO);
        return mek;
    }

    @Test
    void testABloodhoundReachesEightHexes() throws Exception {
        assertEquals(8, mekCarrying(Sensor.BLOODHOUND).getBAPRange());
    }

    @Test
    void testTheOlderBloodhoundVariantReachesEightHexesToo() throws Exception {
        assertEquals(8, mekCarrying("THBBloodhoundActiveProbe").getBAPRange());
    }

    @Test
    void testABeagleProbeStillReachesFourHexes() throws Exception {
        assertEquals(4, mekCarrying(Sensor.BAP).getBAPRange());
    }

    @Test
    void testAClanActiveProbeStillReachesFiveHexes() throws Exception {
        assertEquals(5, mekCarrying(Sensor.CLAN_AP).getBAPRange());
    }
}
