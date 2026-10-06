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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FormationMixPreview}, which reports what a formation mix would have to work with.
 */
class FormationMixPreviewTest {

    private static final double TOLERANCE = 0.01;

    /**
     * Builds a node offering the given formations.
     *
     * @param namesAndWeights alternating formation name and ruleset weight
     */
    private static ForceDescriptor node(Object... namesAndWeights) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setUnitType(UnitType.MEK);
        Map<String, Integer> offered = new LinkedHashMap<>();
        for (int index = 0; index < namesAndWeights.length; index += 2) {
            offered.put((String) namesAndWeights[index], (Integer) namesAndWeights[index + 1]);
        }
        descriptor.setEligibleFormations(offered);
        return descriptor;
    }

    private static ForceDescriptor parentOf(ForceDescriptor... children) {
        ForceDescriptor parent = new ForceDescriptor();
        parent.setUnitType(UnitType.MEK);
        ArrayList<ForceDescriptor> subForces = new ArrayList<>();
        for (ForceDescriptor child : children) {
            subForces.add(child);
        }
        parent.setSubForces(subForces);
        return parent;
    }

    @Test
    void forceWithNoFormationsHasNothingToTweak() {
        FormationMixPreview preview = FormationMixPreview.of(parentOf(node(), node()));

        assertEquals(0, preview.formationNodes());
        assertEquals(0, preview.tweakableNodes());
        assertFalse(preview.hasAnythingToTweak());
        assertTrue(preview.offeredFormations().isEmpty());
    }

    @Test
    void singleOptionNodeIsCountedButNotTweakable() {
        // A command lance: its rule narrowed to one formation, so the mix cannot move it.
        FormationMixPreview preview = FormationMixPreview.of(parentOf(node("Command", 1)));

        assertEquals(1, preview.formationNodes());
        assertEquals(0, preview.tweakableNodes());
        assertFalse(preview.hasAnythingToTweak());
        assertTrue(preview.offeredFormations().isEmpty(),
              "a formation nobody could have chosen differently is not something to offer");
    }

    @Test
    void evenWeightsGiveAnEvenExpectedShare() {
        FormationMixPreview preview = FormationMixPreview.of(
              parentOf(node("Battle", 1, "Fire", 1), node("Battle", 1, "Fire", 1)));

        assertEquals(2, preview.tweakableNodes());
        assertEquals(50.0, preview.defaultShareFor("Battle"), TOLERANCE);
        assertEquals(50.0, preview.defaultShareFor("Fire"), TOLERANCE);
    }

    @Test
    void sharesFollowTheRulesetWeights() {
        // Weights 30 and 10: three quarters Battle, one quarter Recon.
        FormationMixPreview preview = FormationMixPreview.of(parentOf(node("Battle", 30, "Recon", 10)));

        assertEquals(75.0, preview.defaultShareFor("Battle"), TOLERANCE);
        assertEquals(25.0, preview.defaultShareFor("Recon"), TOLERANCE);
    }

    @Test
    void sharesAcrossDifferentMenusAverageByNode() {
        // One node can only be Battle or Fire; the other only Recon or Fire. Fire is the only type both offer, so
        // it should come out ahead of either exclusive one.
        FormationMixPreview preview = FormationMixPreview.of(
              parentOf(node("Battle", 1, "Fire", 1), node("Recon", 1, "Fire", 1)));

        assertEquals(2, preview.tweakableNodes());
        assertEquals(25.0, preview.defaultShareFor("Battle"), TOLERANCE);
        assertEquals(25.0, preview.defaultShareFor("Recon"), TOLERANCE);
        assertEquals(50.0, preview.defaultShareFor("Fire"), TOLERANCE);
    }

    @Test
    void sharesSumToOneHundredAcrossAllOfferedTypes() {
        FormationMixPreview preview = FormationMixPreview.of(
              parentOf(node("Battle", 30, "Fire", 20, "Recon", 10),
                    node("Battle", 5, "Assault", 5),
                    node("Command", 1)));

        double total = preview.defaultSharePercent().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, total, TOLERANCE, "expected shares describe a whole distribution");
        assertEquals(2, preview.tweakableNodes());
        assertEquals(3, preview.formationNodes(), "the single-option node still counts as a formation");
    }

    @Test
    void offeredFormationsExcludeTypesOnlyReachableWithoutAChoice() {
        FormationMixPreview preview = FormationMixPreview.of(
              parentOf(node("Battle", 1, "Fire", 1), node("Command", 1)));

        assertTrue(preview.offeredFormations().contains("Battle"));
        assertTrue(preview.offeredFormations().contains("Fire"));
        assertFalse(preview.offeredFormations().contains("Command"),
              "Command is assigned, never chosen, so it is not on the menu");
    }

    @Test
    void attachedForcesAreSurveyedToo() {
        ForceDescriptor root = parentOf(node("Battle", 1, "Fire", 1));
        ArrayList<ForceDescriptor> attached = new ArrayList<>();
        attached.add(node("Recon", 1, "Pursuit", 1));
        root.setAttached(attached);

        FormationMixPreview preview = FormationMixPreview.of(root);

        assertEquals(2, preview.tweakableNodes());
        assertTrue(preview.offeredFormations().contains("Recon"));
    }

    @Test
    void zeroWeightMenuIsCountedButContributesNoShare() {
        // Defensive: a malformed ruleset could weight every option at zero. It must not divide by zero.
        FormationMixPreview preview = FormationMixPreview.of(parentOf(node("Battle", 0, "Fire", 0)));

        assertEquals(1, preview.tweakableNodes());
        assertTrue(preview.defaultSharePercent().isEmpty());
    }

    @Test
    void nullRootYieldsTheEmptyPreview() {
        assertEquals(FormationMixPreview.EMPTY, FormationMixPreview.of(null));
    }
}
