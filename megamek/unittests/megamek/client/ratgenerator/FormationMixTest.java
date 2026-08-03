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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FormationMix} and {@link FormationMixReport}.
 */
class FormationMixTest {

    private static FormationMix mixOf(Object... namesAndPercents) {
        Map<String, Integer> percentages = new LinkedHashMap<>();
        for (int index = 0; index < namesAndPercents.length; index += 2) {
            percentages.put((String) namesAndPercents[index], (Integer) namesAndPercents[index + 1]);
        }
        return new FormationMix(percentages);
    }

    /** A preview offering the named formations, each an even share of one tweakable node, placeable everywhere. */
    private static FormationMixPreview previewOffering(int tweakableNodes, String... formationNames) {
        Map<String, Double> shares = new LinkedHashMap<>();
        Map<String, Integer> placeable = new LinkedHashMap<>();
        for (String formationName : formationNames) {
            shares.put(formationName, 100.0 / formationNames.length);
            placeable.put(formationName, tweakableNodes);
        }
        return new FormationMixPreview(tweakableNodes, tweakableNodes, shares, placeable);
    }

    // ===== FormationMix =====

    @Test
    void emptyMixRequestsNothing() {
        assertTrue(FormationMix.EMPTY.isEmpty());
        assertEquals(0, FormationMix.EMPTY.totalPercent());
        assertTrue(FormationMix.EMPTY.requestedFormations().isEmpty());
    }

    @Test
    void allZeroMixIsIndistinguishableFromEmpty() {
        // The regression guarantee: untouched controls must leave generation exactly as it was.
        FormationMix allZero = mixOf("Battle", 0, "Recon", 0);

        assertTrue(allZero.isEmpty());
        assertEquals(0, allZero.totalPercent());
    }

    @Test
    void retainsPositiveEntriesAndSumsThem() {
        FormationMix mix = mixOf("Battle", 30, "Fire", 20);

        assertFalse(mix.isEmpty());
        assertEquals(50, mix.totalPercent());
        assertEquals(30, mix.percentFor("Battle"));
        assertEquals(20, mix.percentFor("Fire"));
    }

    @Test
    void formationNamesAreTrimmed() {
        FormationMix mix = mixOf("  Light Battle  ", 40);

        assertEquals(40, mix.percentFor("Light Battle"));
        assertTrue(mix.requestedFormations().contains("Light Battle"));
    }

    @Test
    void unrequestedFormationReportsZero() {
        assertEquals(0, mixOf("Battle", 30).percentFor("Recon"));
        assertEquals(0, mixOf("Battle", 30).percentFor(null));
    }

    @Test
    void blankNamesAreDropped() {
        Map<String, Integer> percentages = new LinkedHashMap<>();
        percentages.put("  ", 40);
        percentages.put("Battle", 30);

        FormationMix mix = new FormationMix(percentages);

        assertEquals(1, mix.requestedFormations().size());
        assertEquals(30, mix.totalPercent());
    }

    @Test
    void percentageAbove100IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> mixOf("Battle", 101));
    }

    @Test
    void percentagesAreImmutableAfterConstruction() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("Battle", 30);
        FormationMix mix = new FormationMix(source);

        source.put("Recon", 20);

        assertEquals(1, mix.requestedFormations().size(), "the mix must not see later edits to the source map");
    }

    @Test
    void unavailableFormationsAreIdentified() {
        FormationMix mix = mixOf("Battle", 30, "Hunter", 20);
        FormationMixPreview preview = previewOffering(10, "Battle", "Recon", "Fire");

        assertEquals(1, mix.unavailableIn(preview).size());
        assertTrue(mix.unavailableIn(preview).contains("Hunter"),
              "a formation this force never offers must be reported before generating");
    }

    @Test
    void restrictingDropsWhatTheForceCannotSupply() {
        FormationMix mix = mixOf("Battle", 30, "Hunter", 20);
        FormationMixPreview preview = previewOffering(10, "Battle", "Recon");

        FormationMix restricted = mix.restrictedTo(preview);

        assertEquals(1, restricted.requestedFormations().size());
        assertEquals(30, restricted.percentFor("Battle"));
        assertEquals(0, restricted.percentFor("Hunter"));
    }

    @Test
    void restrictingToAFullySupportedMixReturnsTheSameInstance() {
        FormationMix mix = mixOf("Battle", 30, "Recon", 20);
        FormationMixPreview preview = previewOffering(10, "Battle", "Recon", "Fire");

        assertSame(mix, mix.restrictedTo(preview), "a fully supportable mix need not be copied");
    }

    // ===== FormationMixReport =====

    @Test
    void reportTalliesRequestedAgainstAssigned() {
        FormationMixPreview preview = previewOffering(20, "Battle", "Recon");
        FormationMixReport report = new FormationMixReport(preview,
              Map.of("Battle", 6, "Recon", 4), Map.of("Battle", 6, "Recon", 4), List.of());

        assertEquals(10, report.totalRequested());
        assertEquals(10, report.totalAssigned());
        assertTrue(report.wasFullyMet());
        assertEquals(6, report.assignedFor("Battle"));
    }

    @Test
    void reportWithWarningsIsNotFullyMet() {
        FormationMixPreview preview = previewOffering(20, "Battle");
        FormationMixReport report = new FormationMixReport(preview,
              Map.of("Battle", 10), Map.of("Battle", 4), List.of("Battle: asked for 10, placed 4"));

        assertFalse(report.wasFullyMet());
        assertEquals(4, report.totalAssigned());
    }

    @Test
    void achievedShareIsAgainstTheTweakableNodes() {
        FormationMixPreview preview = previewOffering(20, "Battle");
        FormationMixReport report = new FormationMixReport(preview,
              Map.of("Battle", 5), Map.of("Battle", 5), List.of());

        assertEquals(25.0, report.achievedSharePercent("Battle"), 0.01);
    }

    @Test
    void achievedShareIsZeroWhenNothingWasTweakable() {
        FormationMixReport report = new FormationMixReport(FormationMixPreview.EMPTY,
              Map.of(), Map.of(), List.of());

        assertEquals(0.0, report.achievedSharePercent("Battle"), 0.01);
        assertEquals(0, report.assignedFor(null));
    }

    // ===== ceilings and the restrictions override =====

    /**
     * The ceiling is what stops the editor offering a share the force cannot deliver. A formation only a quarter of
     * the lances are offered can never be more than a quarter of them.
     */
    @Test
    void aCeilingIsTheShareOfLancesThatCouldTakeTheFormation() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("Recon", 50.0);
        shares.put("Battle", 50.0);
        Map<String, Integer> placeable = new LinkedHashMap<>();
        placeable.put("Recon", 5);
        placeable.put("Battle", 20);
        FormationMixPreview preview = new FormationMixPreview(20, 20, shares, placeable);

        assertEquals(25, preview.ceilingPercentFor("Recon"));
        assertEquals(100, preview.ceilingPercentFor("Battle"));
    }

    @Test
    void aFormationNoLanceIsOfferedHasNoCeiling() {
        FormationMixPreview preview = previewOffering(10, "Battle");
        assertEquals(0, preview.ceilingPercentFor("Recon"));
        assertEquals(0, preview.ceilingPercentFor(null));
    }

    /** Restrictions are on unless the player turns them off, so the default mix never departs from the ruleset. */
    @Test
    void restrictionsAreOnByDefault() {
        assertFalse(mixOf("Recon", 50).allowUnofferedFormations());
        assertFalse(FormationMix.EMPTY.allowUnofferedFormations());
    }

    @Test
    void theOverrideSurvivesBeingNarrowedToWhatTheForceOffers() {
        FormationMix mix = new FormationMix(Map.of("Recon", 50, "Battle", 20), true);
        FormationMix narrowed = mix.restrictedTo(previewOffering(10, "Battle"));

        assertTrue(narrowed.allowUnofferedFormations(),
              "narrowing must not quietly put the ruleset restrictions back on");
    }

}
