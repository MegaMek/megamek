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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers folding the ships each accumulated roll brought into one naval branch at the top of the command.
 */
class TransportBranchMergerTest {

    @Test
    void aLaterRollsShipsJoinTheCommandsNavalBranch() throws Exception {
        ForceDescriptor battalion = formation("Mek Battalion");
        ForceDescriptor battalionShips = navalBranch(battalion, "Troopships", "Flotilla");
        ForceDescriptor overlord = battalionShips.getSubForces().get(0).getSubForces().get(0).getSubForces().get(0);

        ForceDescriptor company = formation("Vehicle Company");
        ForceDescriptor companyShips = navalBranch(company, "Troopships", "Flotilla");
        ForceDescriptor gazelle = companyShips.getSubForces().get(0).getSubForces().get(0).getSubForces().get(0);

        // The view nests a lower-echelon roll under the current top before folding.
        battalion.addSubForce(company);
        TransportBranchMerger.foldInto(battalion, company);

        assertTrue(company.getAttached().isEmpty(), "the company no longer carries a naval branch of its own");
        assertEquals(List.of(battalionShips), battalion.getAttached(), "one naval branch, at the top");
        ForceDescriptor troopships = battalionShips.getSubForces().get(0);
        assertEquals(2, troopships.getSubForces().size(), "both flotillas sit under one Troopships category");
        assertSame(overlord, troopships.getSubForces().get(0).getSubForces().get(0));
        assertSame(gazelle, troopships.getSubForces().get(1).getSubForces().get(0));
        assertSame(troopships, gazelle.getParent().getParent(), "the moved flotilla knows its new parent");
    }

    @Test
    void aCategoryTheCommandLacksIsAddedWhole() throws Exception {
        ForceDescriptor battalion = formation("Mek Battalion");
        ForceDescriptor battalionShips = navalBranch(battalion, "Troopships", "Flotilla");
        ForceDescriptor company = formation("Vehicle Company");
        navalBranch(company, "JumpShips", "Flotilla");

        battalion.addSubForce(company);
        TransportBranchMerger.foldInto(battalion, company);

        List<String> categories = battalionShips.getSubForces().stream().map(ForceDescriptor::parseName).toList();
        assertEquals(List.of("Troopships", "JumpShips"), categories);
    }

    @Test
    void theBranchMovesUpWhenALaterRollBecomesTheTop() throws Exception {
        ForceDescriptor company = formation("Vehicle Company");
        ForceDescriptor companyShips = navalBranch(company, "Troopships", "Flotilla");
        ForceDescriptor regiment = formation("Mek Regiment");

        // A higher-echelon roll takes the earlier command under it and becomes the top.
        regiment.addSubForce(company);
        TransportBranchMerger.foldInto(regiment, regiment);

        assertEquals(List.of(companyShips), regiment.getAttached(), "the branch now hangs off the new top");
        assertTrue(company.getAttached().isEmpty());
    }

    @Test
    void aRollWithoutShipsChangesNothing() throws Exception {
        ForceDescriptor battalion = formation("Mek Battalion");
        ForceDescriptor battalionShips = navalBranch(battalion, "Troopships", "Flotilla");
        ForceDescriptor company = formation("Vehicle Company");

        battalion.addSubForce(company);
        TransportBranchMerger.foldInto(battalion, company);

        assertEquals(List.of(battalionShips), battalion.getAttached());
        assertEquals(1, battalionShips.getSubForces().get(0).getSubForces().size());
    }

    private static ForceDescriptor formation(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    /**
     * Attaches the shape the transport stage produces: Naval Units -> category -> group -> one ship element.
     *
     * @return the naval branch
     */
    private static ForceDescriptor navalBranch(ForceDescriptor force, String categoryName, String groupName)
          throws Exception {
        ForceDescriptor branch = formation("Naval Units");
        Field flag = ForceDescriptor.class.getDeclaredField("transportRoot");
        flag.setAccessible(true);
        flag.set(branch, true);
        ForceDescriptor category = formation(categoryName);
        ForceDescriptor group = formation(groupName);
        ForceDescriptor ship = formation("Ship");
        ship.setElement(true);
        group.addSubForce(ship);
        category.addSubForce(group);
        branch.addSubForce(category);
        force.addAttached(branch);
        return branch;
    }
}
