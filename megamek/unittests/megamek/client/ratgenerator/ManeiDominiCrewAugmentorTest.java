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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.enums.ManeiDominiAugmentationRank;
import megamek.common.enums.ManeiDominiImplants;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * Covers fitting a Word of Blake Shadow Division with its cybernetics.
 *
 * <p>The claims worth holding are that only a Shadow Division is augmented and only when the game
 * allows it, that every crew in the tree is reached however deeply it is nested, and that rank
 * decides how much a warrior receives.</p>
 */
class ManeiDominiCrewAugmentorTest {

    /** A rank index low enough to land on the most junior tier against the default table length. */
    private static final int JUNIOR_RANK_INDEX = 1;

    /** A rank index at the top of the default table, which is the most senior tier. */
    private static final int SENIOR_RANK_INDEX = 50;

    private static Mek warrior(List<Entity> collected) {
        Mek entity = mock(Mek.class);
        when(entity.isIndustrial()).thenReturn(false);
        when(entity.getShortName()).thenReturn("Grand Crusader GRN-D-01");
        Crew crew = mock(Crew.class);
        when(crew.getOptions()).thenReturn(new PilotOptions());
        when(crew.getName()).thenReturn("Precentor");
        when(entity.getCrew()).thenReturn(crew);
        collected.add(entity);
        return entity;
    }

    /** A formation holding one crewed unit, commanded by an officer of the given rank. */
    private static ForceDescriptor elementWith(List<Entity> collected, String faction, int rankIndex) {
        Mek entity = warrior(collected);
        CrewDescriptor commander = mock(CrewDescriptor.class);
        when(commander.getRank()).thenReturn(rankIndex);

        ForceDescriptor element = emptyFormation(faction);
        when(element.getEntity()).thenReturn(entity);
        when(element.getCo()).thenReturn(commander);
        return element;
    }

    private static ForceDescriptor emptyFormation(String faction) {
        ForceDescriptor formation = mock(ForceDescriptor.class);
        when(formation.getSubForces()).thenReturn(new ArrayList<>());
        when(formation.getAttached()).thenReturn(new ArrayList<>());
        when(formation.getFaction()).thenReturn(faction);
        // Left unset so the augmentor falls back to the standard table length rather than reaching for
        // the Ranks singleton, which would tie these tests to the shipped rank data.
        when(formation.getRankSystem()).thenReturn(null);
        return formation;
    }

    private static GameOptions maneiDominiRule(boolean enabled) {
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.RPG_MANEI_DOMINI).setValue(enabled);
        return options;
    }

    /** Every Manei Domini carries the explosive charge, so it marks a crew as having been fitted. */
    private static boolean isAugmented(Entity entity) {
        return entity.getCrew().getOptions().booleanOption(ManeiDominiImplants.getExplosiveCharge());
    }

    private static long implantCount(Entity entity) {
        PilotOptions options = entity.getCrew().getOptions();
        long count = 0;
        for (Enumeration<IOption> optionNames = options.getOptions(); optionNames.hasMoreElements(); ) {
            IOption option = optionNames.nextElement();
            if (option.booleanValue()) {
                count++;
            }
        }
        return count;
    }

    @Test
    void aShadowDivisionCrewIsFittedWhenTheRuleIsOn() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor element = elementWith(warriors,
              ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY, JUNIOR_RANK_INDEX);

        ManeiDominiCrewAugmentor.augment(element, maneiDominiRule(true));

        assertTrue(isAugmented(warriors.getFirst()), "a Shadow Division warrior must be fitted");
    }

    /**
     * The rule gates it because implants fitted with the rule off are stripped when the unit reaches
     * the lobby, so fitting them would only mislead.
     */
    @Test
    void nothingIsFittedWhenTheManeiDominiRuleIsOff() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor element = elementWith(warriors,
              ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY, JUNIOR_RANK_INDEX);

        ManeiDominiCrewAugmentor.augment(element, maneiDominiRule(false));

        assertFalse(isAugmented(warriors.getFirst()), "the rule is off, so nothing may be fitted");
    }

    @Test
    void nothingIsFittedWhenTheOptionsAreMissing() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor element = elementWith(warriors,
              ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY, JUNIOR_RANK_INDEX);

        ManeiDominiCrewAugmentor.augment(element, null);

        assertFalse(isAugmented(warriors.getFirst()), "with no options to read, nothing may be fitted");
    }

    /** Only the Shadow Divisions are Manei Domini; the wider Word of Blake is not. */
    @Test
    void aPlainWordOfBlakeForceIsNotAugmented() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor element = elementWith(warriors, "WOB", JUNIOR_RANK_INDEX);

        ManeiDominiCrewAugmentor.augment(element, maneiDominiRule(true));

        assertFalse(isAugmented(warriors.getFirst()), "only a Shadow Division is Manei Domini");
    }

    @Test
    void theFactionKeyIsMatchedWithoutRegardToCase() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor element = elementWith(warriors, "wob.sd", JUNIOR_RANK_INDEX);

        ManeiDominiCrewAugmentor.augment(element, maneiDominiRule(true));

        assertTrue(isAugmented(warriors.getFirst()), "the faction key must match regardless of case");
    }

    @Test
    void aNullForceIsIgnored() {
        assertDoesNotThrow(() -> ManeiDominiCrewAugmentor.augment(null, maneiDominiRule(true)));
    }

    /** A division is a tree, and a warrior two levels down is as much Manei Domini as the commander. */
    @Test
    void everyCrewInTheTreeIsReached() {
        List<Entity> warriors = new ArrayList<>();
        String faction = ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY;

        ForceDescriptor lance = emptyFormation(faction);
        List<ForceDescriptor> elements = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            elements.add(elementWith(warriors, faction, JUNIOR_RANK_INDEX));
        }
        when(lance.getSubForces()).thenReturn(new ArrayList<>(elements));

        ForceDescriptor division = emptyFormation(faction);
        when(division.getSubForces()).thenReturn(new ArrayList<>(List.of(lance)));

        ManeiDominiCrewAugmentor.augment(division, maneiDominiRule(true));

        assertEquals(4, warriors.size(), "the fixture must build four warriors");
        for (Entity entity : warriors) {
            assertTrue(isAugmented(entity), "a warrior nested two levels down must still be fitted");
        }
    }

    /** Attached support is part of the division too, and is reached down a different link. */
    @Test
    void attachedForcesAreReachedAsWellAsSubForces() {
        List<Entity> warriors = new ArrayList<>();
        String faction = ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY;

        ForceDescriptor attached = elementWith(warriors, faction, JUNIOR_RANK_INDEX);
        ForceDescriptor division = emptyFormation(faction);
        when(division.getAttached()).thenReturn(new ArrayList<>(List.of(attached)));

        ManeiDominiCrewAugmentor.augment(division, maneiDominiRule(true));

        assertTrue(isAugmented(warriors.getFirst()), "an attached crew must be fitted too");
    }

    /**
     * Rank is what decides how much a warrior receives, so the most senior tier must not come out with
     * fewer implants than the most junior one.
     */
    @Test
    void seniorWarriorsReceiveAtLeastAsMuchAsJuniorOnes() {
        List<Entity> juniorWarriors = new ArrayList<>();
        ForceDescriptor junior = elementWith(juniorWarriors,
              ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY, JUNIOR_RANK_INDEX);
        ManeiDominiCrewAugmentor.augment(junior, maneiDominiRule(true));

        List<Entity> seniorWarriors = new ArrayList<>();
        ForceDescriptor senior = elementWith(seniorWarriors,
              ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY, SENIOR_RANK_INDEX);
        ManeiDominiCrewAugmentor.augment(senior, maneiDominiRule(true));

        long juniorImplants = implantCount(juniorWarriors.getFirst());
        long seniorImplants = implantCount(seniorWarriors.getFirst());
        assertTrue(seniorImplants >= juniorImplants,
              "a senior warrior received " + seniorImplants + " implants against a junior's " + juniorImplants);
    }

    /**
     * The junior and senior rank indices used here must actually land on different tiers, or the test
     * above would pass without comparing anything.
     */
    @Test
    void theRankIndicesUsedHereSpanDifferentTiers() {
        ManeiDominiAugmentationRank junior =
              ManeiDominiAugmentationRank.forRankIndex(JUNIOR_RANK_INDEX, SENIOR_RANK_INDEX);
        ManeiDominiAugmentationRank senior =
              ManeiDominiAugmentationRank.forRankIndex(SENIOR_RANK_INDEX, SENIOR_RANK_INDEX);

        assertTrue(senior.ordinal() > junior.ordinal(),
              "expected different tiers, got " + junior + " and " + senior);
    }
}
