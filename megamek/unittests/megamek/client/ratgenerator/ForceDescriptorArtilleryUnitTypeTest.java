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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for artillery generation on a force that declares no unit type.
 *
 * <p>A descriptor's unit type is legitimately {@code null} when the ruleset places no restriction on
 * it - the ComStar and Word of Blake tables of contents both declare {@code <unitType>null</unitType>},
 * and the Level IV they build attaches artillery sub-formations. The artillery ladder compared the
 * boxed field to a {@code UnitType} constant, which unboxes it, so generating a Word of Blake Level IV
 * threw a {@link NullPointerException} instead of simply falling through to the Mek-then-vehicle
 * preference. Generation failed outright and no force was produced.</p>
 */
class ForceDescriptorArtilleryUnitTypeTest {

    private static final int ERA = 3050;
    /** The only faction the trimmed test era file rates units for. */
    private static final String TEST_FACTION = "LA";

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ForceGeneratorTestFixture.loadFromTestData(ERA);
        // The trimmed test data carries no faction_rules directory, so this initializes the ruleset
        // registry to an empty map rather than populating it. That is all this test needs: the
        // sub-force recursion reads the registry, and without the call it is left null.
        Ruleset.loadData();
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void artilleryForceWithNoUnitTypeGeneratesWithoutThrowing() {
        ForceDescriptor artilleryForce = new ForceDescriptor();
        artilleryForce.setFaction(TEST_FACTION);
        artilleryForce.setYear(ERA);
        artilleryForce.setUnitType(null);
        // An equipment rating is needed only so the rating fallback ladder has somewhere to start;
        // the unit type is the subject of this test.
        artilleryForce.setRating("A");
        artilleryForce.getRoles().add(MissionRole.ARTILLERY);
        // The battery-uniformity branch that picks one gun for the whole formation only runs on a
        // node that has children and no unit pinned yet, which is where the unboxing happened.
        artilleryForce.addSubForce(artilleryForce.createChild(0));

        assertDoesNotThrow(() -> artilleryForce.generateUnits(null, 0.0),
              "an artillery force with no unit type must fall through to the Mek/vehicle preference"
                    + " rather than unboxing a null unit type");
    }
}
