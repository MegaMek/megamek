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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A chassis availability of zero means the faction does not field that chassis, even where a parent faction does. The
 * rule is documented in {@code docs/Customization/RAT and Force Generator Stuff/rat-generator.txt}: "A value of zero is
 * a special case which indicates unavailable even if available to a parent faction."
 *
 * <p>The lookup honours it, but for a year that falls between two era buckets the generator used to interpolate the
 * current era's zero towards the next era's value before testing it, which put the chassis back in the table. These
 * tests pin the zero down at both era boundaries and in between.</p>
 */
class ChassisZeroAvailabilityTest {

    private static final int ERA = 3050;
    private static final int NEXT_ERA = 3060;
    /** Between the two era buckets, which is the only place the interpolation runs. */
    private static final int YEAR_BETWEEN_ERAS = 3055;

    private static final String ZERO_RATED_FACTION = "MRG";
    private static final String AVAILABLE_FACTION = "LA";
    private static final String ARCHER = "Archer ARC-2R";

    private static RATGenerator ratGenerator;

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ratGenerator = ForceGeneratorTestFixture.loadFromTestData(ERA);
        // The interpolation reads the next bucket, so it has to be loaded too
        ratGenerator.loadYear(NEXT_ERA);
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void zeroRatedChassisIsKeptOutOfTheTableBetweenEras() {
        List<UnitTable.TableEntry> table = generateMekTableFor(ZERO_RATED_FACTION, YEAR_BETWEEN_ERAS);

        assertFalse(containsUnit(table, ARCHER),
              "A chassis rated zero must stay out of the table; interpolating towards the next era's value put it"
                    + " back in");
    }

    @Test
    void zeroRatedChassisIsKeptOutOfTheTableOnTheEraBoundary() {
        List<UnitTable.TableEntry> table = generateMekTableFor(ZERO_RATED_FACTION, ERA);

        assertFalse(containsUnit(table, ARCHER), "A chassis rated zero must stay out of the table");
    }

    @Test
    void theSameChassisReturnsOnceTheNextEraRatesIt() {
        List<UnitTable.TableEntry> table = generateMekTableFor(ZERO_RATED_FACTION, NEXT_ERA);

        assertTrue(containsUnit(table, ARCHER),
              "The next era rates this chassis above zero, so honouring the zero must not keep it out for good");
    }

    @Test
    void aFactionThatFieldsTheChassisIsUnaffected() {
        List<UnitTable.TableEntry> table = generateMekTableFor(AVAILABLE_FACTION, YEAR_BETWEEN_ERAS);

        assertTrue(containsUnit(table, ARCHER),
              "Honouring another faction's zero must not disturb a faction that does field the chassis");
    }

    private static List<UnitTable.TableEntry> generateMekTableFor(String factionKey, int year) {
        FactionRecord factionRecord = ratGenerator.getFaction(factionKey);
        assertNotNull(factionRecord, "Test data should provide faction " + factionKey);

        return ratGenerator.generateTable(factionRecord, UnitType.MEK, year, null, null, 0, null, null, 0, null);
    }

    private static boolean containsUnit(List<UnitTable.TableEntry> table, String unitName) {
        return table.stream()
              .filter(UnitTable.TableEntry::isUnit)
              .anyMatch(tableEntry -> unitName.equals(tableEntry.getUnitEntry().getName()));
    }
}
