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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * Covers fitting the Clan enhanced imaging implant.
 *
 * <p>The two claims worth holding are that EI warriors serve together rather than one to a star, and
 * that only about one warrior in twenty carries it at all.</p>
 */
class ClanEnhancedImagingAugmentorTest {

    private static final int SAMPLES = 400;

    private static Mek meks(List<Entity> collected) {
        Mek entity = mock(Mek.class);
        when(entity.isIndustrial()).thenReturn(false);
        Crew crew = mock(Crew.class);
        when(crew.getOptions()).thenReturn(new PilotOptions());
        when(entity.getCrew()).thenReturn(crew);
        collected.add(entity);
        return entity;
    }

    /** A star of five Meks under one formation. */
    private static ForceDescriptor starOf(List<Entity> collected, int size) {
        List<ForceDescriptor> children = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            // Built before the stubbing rather than inside it: Mockito refuses a stub whose argument
            // does its own stubbing.
            Mek warrior = meks(collected);
            ForceDescriptor element = emptyFormation();
            when(element.getEntity()).thenReturn(warrior);
            children.add(element);
        }
        ForceDescriptor star = emptyFormation();
        when(star.getSubForces()).thenReturn(new ArrayList<>(children));
        return star;
    }

    private static ForceDescriptor emptyFormation() {
        ForceDescriptor formation = mock(ForceDescriptor.class);
        when(formation.getSubForces()).thenReturn(new ArrayList<>());
        when(formation.getAttached()).thenReturn(new ArrayList<>());
        when(formation.getFlags()).thenReturn(new HashSet<>());
        when(formation.parseName()).thenReturn("Star");
        return formation;
    }

    private static GameOptions neuralInterfacesOn() {
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)
              .setValue(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY);
        return options;
    }

    private static long implantedCount(List<Entity> warriors) {
        return warriors.stream()
                     .filter(entity -> entity.getCrew().getOptions()
                                             .booleanOption(OptionsConstants.MD_EI_IMPLANT))
                     .count();
    }

    /**
     * The whole point of rolling per formation: a star either is an EI unit or it is not. A star with
     * one or two implanted warriors would be the scattered picture the Inner Sphere mistakenly formed.
     */
    @Test
    void anEiUnitIsImplantedWholesaleOrNotAtAll() {
        int starsImplanted = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            List<Entity> warriors = new ArrayList<>();
            ForceDescriptor star = starOf(warriors, 5);
            ClanEnhancedImagingAugmentor.augment(star, true, neuralInterfacesOn());

            long implanted = implantedCount(warriors);
            assertTrue((implanted == 0) || (implanted == warriors.size()),
                  "a star must be all EI or none, got " + implanted + " of " + warriors.size());
            if (implanted > 0) {
                starsImplanted++;
            }
        }
        assertTrue(starsImplanted > 0, "some stars must be EI units across " + SAMPLES + " samples");
    }

    /** Around one warrior in twenty, not most of them and not none. */
    @Test
    void onlyASmallShareOfWarriorsCarryTheImplant() {
        long implanted = 0;
        int total = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            List<Entity> warriors = new ArrayList<>();
            ForceDescriptor star = starOf(warriors, 5);
            ClanEnhancedImagingAugmentor.augment(star, true, neuralInterfacesOn());
            implanted += implantedCount(warriors);
            total += warriors.size();
        }
        double share = (double) implanted / total;
        assertTrue((share > 0.0) && (share < 0.25),
              "roughly one warrior in twenty should carry it, got " + Math.round(share * 100) + "%");
    }

    @Test
    void aNonClanForceIsNeverImplanted() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor star = starOf(warriors, 5);
        ClanEnhancedImagingAugmentor.augment(star, false, neuralInterfacesOn());
        assertEquals(0, implantedCount(warriors), "enhanced imaging is the Clans' alone");
    }

    /** With the neural interface rules off the implant does nothing, so none is fitted. */
    @Test
    void nothingIsImplantedWhenTheNeuralInterfaceRulesAreOff() {
        List<Entity> warriors = new ArrayList<>();
        ForceDescriptor star = starOf(warriors, 5);
        ClanEnhancedImagingAugmentor.augment(star, true, new GameOptions());
        assertEquals(0, implantedCount(warriors));
    }
}
