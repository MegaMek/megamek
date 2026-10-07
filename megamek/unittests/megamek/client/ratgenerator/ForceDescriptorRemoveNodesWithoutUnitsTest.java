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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * A slot the unit tables cannot fill used to stay in the tree, get a commander, and show up as a named pilot with
 * nothing to crew - an empty force once MekHQ built its TO&amp;E from it (#9200). The guard removes those slots, and
 * any formation they leave empty, before commanders are assigned.
 */
class ForceDescriptorRemoveNodesWithoutUnitsTest {

    @Test
    void removesUnfilledSlotAndKeepsFilledOnes() throws Exception {
        ForceDescriptor root = new ForceDescriptor();
        ForceDescriptor star = addChild(root);
        ForceDescriptor filledSlot = addChild(star);
        markHasUnit(filledSlot);
        addChild(star); // no unit

        int removed = root.removeNodesWithoutUnits();

        assertEquals(1, removed);
        assertEquals(1, star.getSubForces().size());
        assertSame(filledSlot, star.getSubForces().getFirst());
    }

    @Test
    void removesFormationLeftEmptyByItsSlots() throws Exception {
        ForceDescriptor root = new ForceDescriptor();
        ForceDescriptor filledStar = addChild(root);
        markHasUnit(addChild(filledStar));
        ForceDescriptor emptyStar = addChild(root);
        addChild(emptyStar);
        addChild(emptyStar);

        int removed = root.removeNodesWithoutUnits();

        // Two empty slots, then the star they emptied
        assertEquals(3, removed);
        assertEquals(1, root.getSubForces().size());
        assertSame(filledStar, root.getSubForces().getFirst());
    }

    @Test
    void removesUnfilledAttachment() throws Exception {
        ForceDescriptor root = new ForceDescriptor();
        markHasUnit(addChild(root));
        ForceDescriptor attachment = root.createChild(1);
        root.addAttached(attachment);

        int removed = root.removeNodesWithoutUnits();

        assertEquals(1, removed);
        assertEquals(0, root.getAttached().size());
        assertEquals(1, root.getSubForces().size());
    }

    @Test
    void keepsRootEvenWhenNothingFilled() {
        ForceDescriptor root = new ForceDescriptor();

        assertEquals(0, root.removeNodesWithoutUnits());
    }

    @Test
    void aForceWithOnlyAttachedSupportHasNoLineUnits() throws Exception {
        // A Beast regiment with no beast data: every line slot removed, the artillery attachment left
        ForceDescriptor regiment = new ForceDescriptor();
        ForceDescriptor artillery = regiment.createChild(0);
        markHasUnit(artillery);
        regiment.addAttached(artillery);

        assertFalse(regiment.hasLineUnits());
    }

    @Test
    void aForceWithAUnitInItsLineHasLineUnits() throws Exception {
        ForceDescriptor company = new ForceDescriptor();
        ForceDescriptor platoon = addChild(addChild(company));
        markHasUnit(platoon);

        assertTrue(company.hasLineUnits());
    }

    private static ForceDescriptor addChild(ForceDescriptor parent) {
        ForceDescriptor child = parent.createChild(parent.getSubForces().size());
        parent.addSubForce(child);
        return child;
    }

    /**
     * Flags a slot as holding a unit, which is what {@code setUnit} does once the unit tables return a model. Set
     * directly so the test needs no unit data.
     */
    private static void markHasUnit(ForceDescriptor slot) throws Exception {
        Field element = ForceDescriptor.class.getDeclaredField("element");
        element.setAccessible(true);
        element.setBoolean(slot, true);
    }
}
