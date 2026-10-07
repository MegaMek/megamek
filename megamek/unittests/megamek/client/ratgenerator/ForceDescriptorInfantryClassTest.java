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

import static megamek.client.ratgenerator.InfantryClassTest.infantry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * The infantry class on a force node: which units it lets through, and how it passes down the tree.
 */
class ForceDescriptorInfantryClassTest {

    @Test
    void anInfantryNodeWithAClassOnlyAcceptsThatClass() {
        ForceDescriptor platoon = node(UnitType.INFANTRY, InfantryClass.JUMP);

        assertTrue(platoon.acceptsForInfantryClass(infantry("Jump Infantry", "Jump", false)));
        assertFalse(platoon.acceptsForInfantryClass(infantry("Foot Infantry", "Leg", false)));
        assertFalse(platoon.acceptsForInfantryClass(infantry("Mechanized Infantry", "Tracked", false)));
    }

    @Test
    void withoutAClassAnythingIsAccepted() {
        ForceDescriptor platoon = node(UnitType.INFANTRY, null);

        assertTrue(platoon.acceptsForInfantryClass(infantry("Foot Infantry", "Leg", false)));
    }

    @Test
    void theClassDoesNotRestrictOtherUnitTypes() {
        // A Mek force's attached infantry has its class cleared, but a stray class on a non-infantry node must not
        // block that node's own units either.
        ForceDescriptor tankLance = node(UnitType.TANK, InfantryClass.JUMP);
        MekSummary tank = infantry("Manticore", "Tracked", false);
        tank.setUnitType("Tank");

        assertTrue(tankLance.acceptsForInfantryClass(tank));
    }

    @Test
    void aBeastForceOnlyAcceptsItsPinnedAnimal() {
        ForceDescriptor platoon = node(UnitType.INFANTRY, InfantryClass.BEAST);
        platoon.setInfantryClassChassis("Beast Infantry (Horse)");

        assertTrue(platoon.acceptsForInfantryClass(infantry("Beast Infantry (Horse)", "Leg", true)));
        assertFalse(platoon.acceptsForInfantryClass(infantry("Beast Infantry (Camel)", "Leg", true)),
              "every platoon rides the same animal");
    }

    @Test
    void childrenInheritTheClassAndThePinnedBeast() {
        ForceDescriptor company = node(UnitType.INFANTRY, InfantryClass.BEAST);
        company.setInfantryClassChassis("Beast Infantry (Horse)");

        ForceDescriptor platoon = company.createChild(0);

        assertEquals(InfantryClass.BEAST, platoon.getInfantryClass());
        assertEquals("Beast Infantry (Horse)", platoon.getInfantryClassChassis());
    }

    @Test
    void changingAwayFromBeastDropsThePinnedBeast() {
        ForceDescriptor company = node(UnitType.INFANTRY, InfantryClass.BEAST);
        company.setInfantryClassChassis("Beast Infantry (Horse)");

        company.setInfantryClass(InfantryClass.LIGHT);

        assertNull(company.getInfantryClassChassis());
    }

    private static ForceDescriptor node(int unitType, InfantryClass infantryClass) {
        ForceDescriptor node = new ForceDescriptor();
        node.setUnitType(unitType);
        node.setInfantryClass(infantryClass);
        return node;
    }
}
