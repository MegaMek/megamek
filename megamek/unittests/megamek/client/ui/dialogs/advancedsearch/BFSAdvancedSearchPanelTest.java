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
package megamek.client.ui.dialogs.advancedsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import megamek.common.battlefieldSupport.BFSArtilleryType;
import megamek.common.battlefieldSupport.BFSDamage;
import megamek.common.battlefieldSupport.BFSSpecial;
import megamek.common.battlefieldSupport.BFSSpecialType;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

class BFSAdvancedSearchPanelTest {

    @Test
    void inactivePanelMatchesNullAndNonAssets() {
        BFSAdvancedSearchPanel panel = new BFSAdvancedSearchPanel();

        assertTrue(panel.matches(null));
        assertTrue(panel.matches(new MekSummary()));
    }

    @Test
    void multipleNumericSpecialFiltersAllMustMeetTheirMinimums() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.numericSpecialSelected = List.of(BFSSpecialType.INDIRECT_FIRE, BFSSpecialType.ECM);
            state.numericSpecialMinTexts = Map.of(
                  BFSSpecialType.INDIRECT_FIRE, "2",
                  BFSSpecialType.ECM, "4");
        });

        assertTrue(panel.matches(asset(
              numericSpecial(BFSSpecialType.INDIRECT_FIRE, 2),
              numericSpecial(BFSSpecialType.ECM, 6))));
        assertFalse(panel.matches(asset(
              numericSpecial(BFSSpecialType.INDIRECT_FIRE, 1),
              numericSpecial(BFSSpecialType.ECM, 6))));
        assertFalse(panel.matches(asset(numericSpecial(BFSSpecialType.INDIRECT_FIRE, 3))));
    }

    @Test
    void blankNumericMinimumRequiresTheSpecialWithoutConstrainingItsValue() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.numericSpecialSelected = List.of(BFSSpecialType.APC);
            state.numericSpecialMinTexts = Map.of(BFSSpecialType.APC, "");
        });

        assertTrue(panel.matches(asset(numericSpecial(BFSSpecialType.APC, 1))));
        assertFalse(panel.matches(asset(BFSSpecial.of(BFSSpecialType.NIMBLE.canonicalCode()))));
    }

    @Test
    void generalSpecialSectionUsesAndSemanticsAndExcludesParameterizedSpecials() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.specialsUse = true;
            state.specialsSelected = List.of(
                  BFSSpecialType.NIMBLE,
                  BFSSpecialType.SPOTTER,
                  BFSSpecialType.INDIRECT_FIRE,
                  BFSSpecialType.ARTILLERY);
        });

        assertEquals(List.of(BFSSpecialType.NIMBLE, BFSSpecialType.SPOTTER),
              panel.getState().specialsSelected);
        assertTrue(panel.matches(asset(
              BFSSpecial.of(BFSSpecialType.NIMBLE.canonicalCode()),
              BFSSpecial.of(BFSSpecialType.SPOTTER.canonicalCode()))));
        assertFalse(panel.matches(asset(BFSSpecial.of(BFSSpecialType.NIMBLE.canonicalCode()))));
    }

    @Test
    void selectedArtilleryTypesUseOrSemantics() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.artilleryTypeUse = true;
            state.artilleryTypeSelected = List.of(BFSArtilleryType.THUMPER, BFSArtilleryType.LONG_TOM);
        });

        assertTrue(panel.matches(asset(artillerySpecial(BFSArtilleryType.THUMPER))));
        assertTrue(panel.matches(asset(artillerySpecial(BFSArtilleryType.LONG_TOM))));
        assertFalse(panel.matches(asset(artillerySpecial(BFSArtilleryType.SNIPER))));
        assertFalse(panel.matches(asset(BFSSpecial.of(BFSSpecialType.SPOTTER.canonicalCode()))));
    }

    @Test
    void emptyArtillerySelectionMatchesAnyArtilleryType() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> state.artilleryTypeUse = true);

        assertTrue(panel.matches(asset(artillerySpecial(BFSArtilleryType.SNIPER))));
        assertFalse(panel.matches(asset(BFSSpecial.of(BFSSpecialType.SPOTTER.canonicalCode()))));
    }

    @Test
    void damageFiltersMatchTotalPerHitAndHitCount() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.damageUse = true;
            state.damageFromText = "15";
            state.damageToText = "25";
            state.damagePerHitUse = true;
            state.damagePerHitFromText = "4";
            state.damagePerHitToText = "6";
            state.damageHitsUse = true;
            state.damageHitsFromText = "3";
            state.damageHitsToText = "5";
        });

        assertTrue(panel.matches(asset(new BFSDamage(5, 4))));
        assertFalse(panel.matches(asset(new BFSDamage(3, 6))));
        assertFalse(panel.matches(asset(new BFSDamage(10, 2))));
    }

    @Test
    void stateRoundTripPreservesIndependentSpecialAndDamageFilters() {
        AdvSearchState.BfsState state = new AdvSearchState.BfsState();
        state.damagePerHitUse = true;
        state.damagePerHitFromText = "3";
        state.damagePerHitToText = "7";
        state.damageHitsUse = true;
        state.damageHitsFromText = "2";
        state.damageHitsToText = "4";
        state.numericSpecialSelected = List.of(BFSSpecialType.INDIRECT_FIRE, BFSSpecialType.MASH);
        state.numericSpecialMinTexts = Map.of(
              BFSSpecialType.INDIRECT_FIRE, "2",
              BFSSpecialType.MASH, "1");
        state.artilleryTypeUse = true;
        state.artilleryTypeSelected = List.of(BFSArtilleryType.SNIPER);

        BFSAdvancedSearchPanel panel = new BFSAdvancedSearchPanel();
        panel.applyState(state);
        AdvSearchState.BfsState captured = panel.getState();

        assertTrue(captured.damagePerHitUse);
        assertEquals("3", captured.damagePerHitFromText);
        assertEquals("7", captured.damagePerHitToText);
        assertTrue(captured.damageHitsUse);
        assertEquals("2", captured.damageHitsFromText);
        assertEquals("4", captured.damageHitsToText);
        assertEquals(state.numericSpecialSelected, captured.numericSpecialSelected);
        assertEquals(state.numericSpecialMinTexts, captured.numericSpecialMinTexts);
        assertTrue(captured.artilleryTypeUse);
        assertEquals(state.artilleryTypeSelected, captured.artilleryTypeSelected);
    }

    @Test
    void resetValuesRestoresTheSavedSpecialFilters() {
        BFSAdvancedSearchPanel panel = panelWithState(state -> {
            state.numericSpecialSelected = List.of(BFSSpecialType.ECM);
            state.numericSpecialMinTexts = Map.of(BFSSpecialType.ECM, "4");
        });
        panel.saveValues();

        AdvSearchState.BfsState replacement = new AdvSearchState.BfsState();
        replacement.numericSpecialSelected = List.of(BFSSpecialType.APC);
        replacement.numericSpecialMinTexts = Map.of(BFSSpecialType.APC, "2");
        panel.applyState(replacement);
        panel.resetValues();

        assertEquals(List.of(BFSSpecialType.ECM), panel.getState().numericSpecialSelected);
        assertEquals(Map.of(BFSSpecialType.ECM, "4"), panel.getState().numericSpecialMinTexts);
    }

    private static BFSAdvancedSearchPanel panelWithState(Consumer<AdvSearchState.BfsState> configure) {
        AdvSearchState.BfsState state = new AdvSearchState.BfsState();
        configure.accept(state);
        BFSAdvancedSearchPanel panel = new BFSAdvancedSearchPanel();
        panel.applyState(state);
        return panel;
    }

    private static MekSummary asset(BFSSpecial... specials) {
        return asset(BFSDamage.NONE, specials);
    }

    private static MekSummary asset(BFSDamage damage, BFSSpecial... specials) {
        MekSummary summary = new MekSummary();
        summary.setEntityType(Entity.ETYPE_BATTLEFIELD_SUPPORT_ASSET);
        summary.setBfsDamage(damage);
        summary.setBfsSpecialDetails(List.of(specials));
        summary.setBfsSpecials(Arrays.stream(specials)
              .map(BFSSpecial::knownType)
              .flatMap(java.util.Optional::stream)
              .distinct()
              .toList());
        return summary;
    }

    private static BFSSpecial numericSpecial(BFSSpecialType type, int value) {
        return BFSSpecial.of(type.canonicalCode(), value);
    }

    private static BFSSpecial artillerySpecial(BFSArtilleryType type) {
        return BFSSpecial.of(BFSSpecialType.ARTILLERY.canonicalCode(), type.code());
    }
}
