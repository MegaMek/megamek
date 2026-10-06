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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the gamemaster temporary skill modifiers: how they change the crew's effective skills, that the stored
 * skills are never touched, and that the modifiers count down and clear themselves like the other timed effects.
 */
class TemporarySkillModifiersTest {

    @Test
    void freshModifiersLeaveEverySkillUntouched() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();

        assertFalse(modifiers.isActive());
        assertEquals(4, modifiers.adjustGunnery(4));
        assertEquals(5, modifiers.adjustPiloting(5));
        assertEquals(0, modifiers.getInitiativeDelta());
    }

    @Test
    void negativeDeltaImprovesTheSkillAndPositiveWorsensIt() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(-1, 2, 0, 3);

        assertEquals(3, modifiers.adjustGunnery(4));
        assertEquals(7, modifiers.adjustPiloting(5));
    }

    @Test
    void effectiveSkillIsHeldToTheNormalSkillRange() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(-8, 8, 0, 3);

        assertEquals(0, modifiers.adjustGunnery(4));
        assertEquals(Crew.MAX_SKILL, modifiers.adjustPiloting(5));
    }

    @Test
    void modifiersCountDownAndClearThemselves() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(1, 1, 1, 2);

        modifiers.newRound();
        assertTrue(modifiers.isActive());
        assertEquals(5, modifiers.adjustGunnery(4));

        modifiers.newRound();
        assertFalse(modifiers.isActive());
        assertEquals(4, modifiers.adjustGunnery(4));
        assertEquals(0, modifiers.getInitiativeDelta());
    }

    @Test
    void permanentModifiersNeverCountDown() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(1, 0, 0, TemporarySkillModifiers.PERMANENT);

        for (int round = 0; round < 20; round++) {
            modifiers.newRound();
        }

        assertTrue(modifiers.isActive());
        assertEquals(5, modifiers.adjustGunnery(4));
    }

    @Test
    void settingZeroRoundsClearsTheModifiers() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(1, 1, 1, 3);
        modifiers.set(2, 2, 2, 0);

        assertFalse(modifiers.isActive());
        assertEquals(4, modifiers.adjustGunnery(4));
    }

    @Test
    void clearRemovesAnActiveModifierAtOnce() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(1, 1, 1, TemporarySkillModifiers.PERMANENT);
        modifiers.clear();

        assertFalse(modifiers.isActive());
        assertEquals(4, modifiers.adjustGunnery(4));
        assertEquals(0, modifiers.getInitiativeDelta());
    }

    @Test
    void initiativeDeltaOnlyCountsWhileActive() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.set(0, 0, 2, 1);

        assertEquals(2, modifiers.getInitiativeDelta());
        modifiers.newRound();
        assertEquals(0, modifiers.getInitiativeDelta());
    }

    @Test
    void eachModifierCountsDownOnItsOwnClock() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.setGunnery(1, 1);
        modifiers.setPiloting(2, 2);
        modifiers.setInitiative(3, TemporarySkillModifiers.PERMANENT);

        modifiers.newRound();
        // the gunnery round is spent, the piloting modifier has a round left, the initiative one never expires
        assertEquals(4, modifiers.adjustGunnery(4));
        assertEquals(7, modifiers.adjustPiloting(5));
        assertEquals(3, modifiers.getInitiativeDelta());

        modifiers.newRound();
        assertEquals(5, modifiers.adjustPiloting(5));
        assertEquals(3, modifiers.getInitiativeDelta());
        assertTrue(modifiers.isActive(), "the permanent initiative modifier keeps the modifiers active");
    }

    @Test
    void settingOneModifierLeavesTheOthersAlone() {
        TemporarySkillModifiers modifiers = new TemporarySkillModifiers();
        modifiers.setGunnery(2, 3);
        modifiers.setPiloting(1, TemporarySkillModifiers.PERMANENT);

        modifiers.setGunnery(0, 3);

        // clearing the gunnery modifier by its zero delta does not touch the piloting one
        assertEquals(4, modifiers.adjustGunnery(4));
        assertEquals(6, modifiers.adjustPiloting(5));
        assertEquals(TemporarySkillModifiers.PERMANENT, modifiers.getPilotingRounds());
        assertEquals(0, modifiers.getGunneryRounds());
    }

    @Test
    void crewEffectiveSkillsFollowTheModifiersButStoredSkillsStayRaw() {
        Crew crew = new Crew(CrewType.SINGLE);
        int baseGunnery = crew.getGunnery();
        int basePiloting = crew.getPiloting();

        crew.getSkillModifiers().set(-1, 1, 0, 3);

        // The effective skill moves with the modifier...
        assertEquals(baseGunnery - 1, crew.getGunnery());
        assertEquals(basePiloting + 1, crew.getPiloting());
        // ...while the per-slot raw skill, which export and customization read, does not.
        assertEquals(baseGunnery, crew.getGunnery(0));
        assertEquals(basePiloting, crew.getPiloting(0));
    }

    @Test
    void crewSkillsReturnToBaseWhenTheModifierExpires() {
        Crew crew = new Crew(CrewType.SINGLE);
        int baseGunnery = crew.getGunnery();

        crew.getSkillModifiers().set(2, 2, 0, 1);
        assertEquals(baseGunnery + 2, crew.getGunnery());

        crew.getSkillModifiers().newRound();
        assertEquals(baseGunnery, crew.getGunnery());
    }

    @Test
    void appliedGunneryModifierIsTheEffectiveMinusStoredSkill() {
        Crew crew = new Crew(CrewType.SINGLE);
        assertEquals(0, crew.appliedGunneryModifier());

        crew.getSkillModifiers().set(2, 0, 0, 3);
        // the to-hit breakdown rebuilds the raw skill from these two, so together they must equal the effective
        assertEquals(2, crew.appliedGunneryModifier());
        assertEquals(crew.getGunnery(), crew.getGunnery(0) + crew.appliedGunneryModifier());

        // held to the skill range: a huge bonus only counts for what the clamp lets through
        crew.getSkillModifiers().set(-8, 0, 0, 3);
        assertEquals(crew.getGunnery() - crew.getGunnery(0), crew.appliedGunneryModifier());
        assertEquals(crew.getGunnery(), crew.getGunnery(0) + crew.appliedGunneryModifier());
    }
}
