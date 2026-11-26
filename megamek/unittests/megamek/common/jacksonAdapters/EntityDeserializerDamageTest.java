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
package megamek.common.jacksonAdapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * A V2 scenario unit can carry {@code damage: light / moderate / heavy / random}, which rolls the same pre-existing
 * damage (FSW p.144) as the unit editor's button.
 */
class EntityDeserializerDamageTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    void heavyDamageRemovesArmorOrStructureFromAMek() throws Exception {
        Entity undamaged = readUnit("fullname: Atlas AS7-D");
        Entity damaged = readUnit("fullname: Atlas AS7-D\ndamage: heavy");

        // Heavy is 4 points per 5 tons: 80 points on a 100-ton Atlas
        int undamagedPoints = undamaged.getTotalArmor() + undamaged.getTotalInternal();
        int damagedPoints = damaged.getTotalArmor() + damaged.getTotalInternal();
        assertTrue(damagedPoints < undamagedPoints,
              "heavy damage should remove points: " + damagedPoints + " vs " + undamagedPoints);
    }

    @Test
    void heavyDamageAlsoWorksOnAVehicle() throws Exception {
        Entity undamaged = readUnit("fullname: Bulldog Medium Tank");
        Entity damaged = readUnit("fullname: Bulldog Medium Tank\ndamage: HEAVY");

        assertTrue(damaged.getTotalArmor() + damaged.getTotalInternal()
              < undamaged.getTotalArmor() + undamaged.getTotalInternal());
    }

    @Test
    void noneLeavesTheUnitUndamaged() throws Exception {
        Entity undamaged = readUnit("fullname: Atlas AS7-D");
        Entity unchanged = readUnit("fullname: Atlas AS7-D\ndamage: none");

        assertEquals(undamaged.getTotalArmor(), unchanged.getTotalArmor());
        assertEquals(undamaged.getTotalInternal(), unchanged.getTotalInternal());
    }

    @Test
    void randomRollsTheTableAndLoads() throws Exception {
        Entity undamaged = readUnit("fullname: Atlas AS7-D");
        Entity rolled = readUnit("fullname: Atlas AS7-D\ndamage: random");

        // A roll of 1 means no damage, so only check the unit loaded and was never repaired above new
        assertTrue(rolled.getTotalArmor() <= undamaged.getTotalArmor());
        assertTrue(rolled.getTotalInternal() <= undamaged.getTotalInternal());
    }

    @Test
    void unitTypeTheTableDoesNotCoverLoadsUndamaged() throws Exception {
        Entity undamaged = readUnit("fullname: Elemental Battle Armor [Laser](Sqd5)");
        Entity unchanged = readUnit("fullname: Elemental Battle Armor [Laser](Sqd5)\ndamage: heavy");

        assertEquals(undamaged.getTotalInternal(), unchanged.getTotalInternal());
    }

    @Test
    void unknownDamageLevelStopsTheLoad() {
        Exception exception = assertThrows(Exception.class,
              () -> readUnit("fullname: Atlas AS7-D\ndamage: crippled"));

        assertTrue(rootMessage(exception).contains("Unknown damage level crippled"), rootMessage(exception));
    }

    private Entity readUnit(String yaml) throws Exception {
        return YAML_MAPPER.readValue(yaml, Entity.class);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }
}
