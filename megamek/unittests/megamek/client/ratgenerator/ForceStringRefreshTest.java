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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Covers renumbering a command's formations before it is sent to a game, so rolls that were each numbered from one
 * do not collide, and the wrapper an accumulated model sits in stays out of the force strings.
 */
class ForceStringRefreshTest {

    @Test
    void twoRollsNumberedFromOneAreRenumberedAsOneCommand() throws Exception {
        ForceDescriptor battalion = formation("Battalion");
        ForceDescriptor battalionLance = formation("Lance");
        Entity mek = unit(battalionLance, "Atlas AS7-D", false);
        battalion.addSubForce(battalionLance);
        ForceDescriptor company = formation("Company");
        ForceDescriptor companyLance = formation("Lance");
        Entity tank = unit(companyLance, "Bulldog Medium Tank", true);
        company.addSubForce(companyLance);
        // Each roll stamped itself from one when it was generated.
        battalion.assignForceIds(1);
        company.assignForceIds(1);
        // The view nests the later, smaller roll under the earlier one, inside a wrapper it never numbers.
        battalion.addSubForce(company);
        ForceDescriptor wrapper = formation("Command Model");
        wrapper.addSubForce(battalion);

        battalion.refreshForceStrings();

        assertEquals("Battalion|1||Lance|2||", mek.getForceString());
        assertEquals("Battalion|1||Company|3||Lance|4||", tank.getForceString(),
              "The company's lance no longer shares id 2 with the battalion's, and the wrapper is absent");
    }

    private static ForceDescriptor formation(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    /** Adds a unit node under the formation, carrying a real entity, and returns the entity. */
    private static Entity unit(ForceDescriptor parent, String unitName, boolean isBlk) throws Exception {
        Entity entity = getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, unitName + " not found in the test data");
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(entity.getShortName());
        descriptor.setElement(true);
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        field.set(descriptor, entity);
        parent.addSubForce(descriptor);
        return entity;
    }
}
