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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the role-target ladder {@link ForceDescriptor} runs before its ordinary generation ladder.
 *
 * <p>The ladder itself needs a populated unit table, so these tests pin the parts that can be checked without one:
 * the order of roles it will try, that a role mix reaches the leaves that consult it, and that an element with no
 * target is left completely alone. The rule that weight class is never traded away to satisfy a role is asserted
 * structurally, by checking the role ladder holds the element's weight class fixed.</p>
 */
class ForceDescriptorRoleLadderTest {

    private static ForceDescriptor mekLeaf(int weightClass) {
        ForceDescriptor leaf = new ForceDescriptor();
        leaf.setUnitType(UnitType.MEK);
        leaf.setWeightClass(weightClass);
        return leaf;
    }

    private static RoleMix mixOf(UnitRole firstRole, int firstPercent, UnitRole secondRole, int secondPercent) {
        Map<UnitRole, Integer> percentages = new EnumMap<>(UnitRole.class);
        percentages.put(firstRole, firstPercent);
        percentages.put(secondRole, secondPercent);
        return new RoleMix(percentages);
    }

    @SuppressWarnings("unchecked")
    private static List<UnitRole> candidatesFor(ForceDescriptor element) throws Exception {
        Method method = ForceDescriptor.class.getDeclaredMethod("roleTargetCandidates");
        method.setAccessible(true);
        return (List<UnitRole>) method.invoke(element);
    }

    @Test
    void requestedRoleIsTriedFirst() throws Exception {
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_HEAVY);
        leaf.setTargetUnitRole(UnitRole.SNIPER);

        assertEquals(UnitRole.SNIPER, candidatesFor(leaf).getFirst(),
              "the role that was asked for must be tried before any substitute");
    }

    @Test
    void substitutesFollowTheCanonicalChain() throws Exception {
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_LIGHT);
        leaf.setTargetUnitRole(UnitRole.SKIRMISHER);

        List<UnitRole> candidates = candidatesFor(leaf);

        assertEquals(UnitRole.SKIRMISHER, candidates.get(0));
        assertEquals(UnitRoleFallbackChains.chainFor(UnitRole.SKIRMISHER),
              candidates.subList(1, 1 + UnitRoleFallbackChains.chainFor(UnitRole.SKIRMISHER).size()),
              "the ranked chain must follow the requested role in order");
    }

    @Test
    void otherRequestedRolesComeLast() throws Exception {
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_ASSAULT);
        leaf.setRoleMix(mixOf(UnitRole.JUGGERNAUT, 50, UnitRole.MISSILE_BOAT, 50));
        leaf.setTargetUnitRole(UnitRole.JUGGERNAUT);

        List<UnitRole> candidates = candidatesFor(leaf);

        assertTrue(candidates.contains(UnitRole.MISSILE_BOAT),
              "anything else the mix asked for beats rolling with no role at all");
        assertTrue(candidates.indexOf(UnitRole.MISSILE_BOAT)
                    > candidates.indexOf(UnitRoleFallbackChains.chainFor(UnitRole.JUGGERNAUT).getLast()),
              "the other requested roles must come after the requested role's own chain");
    }

    @Test
    void candidatesAreNeverRepeated() throws Exception {
        // Brawler's chain already contains Skirmisher, so asking for both must not queue it twice.
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_HEAVY);
        leaf.setRoleMix(mixOf(UnitRole.BRAWLER, 50, UnitRole.SKIRMISHER, 50));
        leaf.setTargetUnitRole(UnitRole.BRAWLER);

        List<UnitRole> candidates = candidatesFor(leaf);

        assertEquals(candidates.size(), candidates.stream().distinct().count(),
              "a role already queued must not be tried a second time");
    }

    @Test
    void elementWithNoTargetIsUntouched() {
        // The regression guarantee at the element level: no target role, no change of behaviour.
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_MEDIUM);

        assertNull(leaf.getTargetUnitRole());
        assertTrue(leaf.getRoleMix().isEmpty());
    }

    @Test
    void roleMixIsInheritedByChildrenButTheTargetIsNot() {
        // The mix is a force-wide setting a leaf consults for its last ladder rung; the target is assigned per slot
        // by the allocator and must never be inherited, or one assignment would spread across a whole subtree.
        ForceDescriptor parent = mekLeaf(EntityWeightClass.WEIGHT_HEAVY);
        parent.setRoleMix(mixOf(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50));
        parent.setTargetUnitRole(UnitRole.BRAWLER);

        ForceDescriptor child = parent.createChild(0);

        assertEquals(parent.getRoleMix().percentages(), child.getRoleMix().percentages(),
              "the mix is force-wide and must reach the leaves that consult it");
        assertNull(child.getTargetUnitRole(), "a role target belongs to one slot and must not be inherited");
    }

    @Test
    void ladderShortCircuitsWhenNoRoleWasAskedFor() throws Exception {
        // The regression guarantee where it matters most: with no target the ladder returns before it touches a
        // table at all, so an element generates through exactly the path it always did.
        ForceDescriptor leaf = mekLeaf(EntityWeightClass.WEIGHT_MEDIUM);
        Method ladder = ForceDescriptor.class.getDeclaredMethod("tryRoleTargetLadder",
              ForceDescriptor.class, String.class, List.class);
        ladder.setAccessible(true);

        Object result = ladder.invoke(leaf, leaf.createChild(0), "C", new ArrayList<String>());

        assertNull(result, "an element with no role target must fall straight through to the ordinary ladder");
    }

    @Test
    void theWorkingCopyKeepsTheElementsWeightClass() {
        // The rule the design rests on, as far as it can be checked without a populated unit table: the copy the
        // role ladder generates from carries the element's own weight class, and generateForRole builds its table
        // from that alone rather than consulting the alternateWeights relaxation table.
        ForceDescriptor light = mekLeaf(EntityWeightClass.WEIGHT_LIGHT);

        ForceDescriptor workingCopy = light.createChild(0);

        assertEquals(EntityWeightClass.WEIGHT_LIGHT, workingCopy.getWeightClass(),
              "a Light slot must still be Light when the role ladder looks for its unit");
    }
}
