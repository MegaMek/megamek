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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import megamek.common.compute.Compute;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ManualRandomTest {

    @AfterEach
    void restoreRng() {
        Compute.setRNG(MMRandom.R_DEFAULT);
        ManualDice.popPurpose();
    }

    @Test
    void generateReturnsManualRandomForType3() {
        assertInstanceOf(ManualRandom.class, MMRandom.generate(MMRandom.R_MANUAL));
    }

    @Test
    void loadingGameOptionsInstallsManualRng() {
        GameOptions options = new GameOptions();
        options.initialize();
        options.getOption(OptionsConstants.BASE_RNG_TYPE).setValue(MMRandom.R_MANUAL);
        options.loadOptions(new File("this-file-does-not-exist-manual-rng.xml"), false);

        assertInstanceOf(ManualRandom.class, Compute.getRNG());
    }

    @Test
    void gunnerySkillRollDoesNotPromptByItself() {
        AtomicInteger calls = new AtomicInteger();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            calls.incrementAndGet();
            return Optional.of(List.of(4, 5));
        }));
        Crew crew = new Crew(CrewType.SINGLE);

        crew.rollGunnerySkill();

        assertEquals(0, calls.get());
    }

    @Test
    void weaponAttackPromptIncludesAttackerWeaponTargetAndNeed() {
        AtomicReference<String> seen = new AtomicReference<>();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            seen.set(purpose);
            return Optional.of(List.of(4, 5));
        }));

        String prompt = "To-hit\n\nArcher ARC-2K (Phoenix) fires Medium Laser at Locust LCT-1V (Princess)\nNeeds 8";
        int total = ManualDice.withPurpose(prompt, () -> Compute.d6(2));

        assertEquals(9, total);
        assertEquals(prompt, seen.get());
    }

    @Test
    void consumesQueuedFacesInOrder() {
        ManualRandom rng = new ManualRandom((n, purpose) -> Optional.of(List.of(3, 5)));

        Roll roll = ManualDice.withPurpose("To-hit", () -> rng.d6(2));

        assertEquals(8, roll.getIntValue());
        assertArrayEquals(new int[] { 3, 5 }, roll.getIntValues());
    }

    @Test
    void singleDieUsesTheSuppliedFace() {
        ManualRandom rng = new ManualRandom((n, purpose) -> Optional.of(List.of(4)));

        assertEquals(4, ManualDice.withPurpose("To-hit", () -> rng.d6().getIntValue()));
    }

    @Test
    void keepHighestUsesSuppliedFaces() {
        ManualRandom rng = new ManualRandom((n, purpose) -> Optional.of(List.of(1, 6, 2)));

        Roll roll = ManualDice.withPurpose("To-hit", () -> rng.d6(3, 2));

        assertEquals(8, roll.getIntValue());
        assertArrayEquals(new int[] { 6, 2 }, roll.getIntValues());
    }

    @Test
    void randomIntDoesNotConsumeOrRequestDice() {
        AtomicInteger calls = new AtomicInteger();
        ManualRandom rng = new ManualRandom((n, purpose) -> {
            calls.incrementAndGet();
            return Optional.of(List.of(1));
        });

        int value = rng.randomInt(10);

        assertEquals(0, calls.get());
        assertTrue((value >= 0) && (value < 10));
    }

    @Test
    void outsideCombatDoesNotPrompt() {
        AtomicInteger calls = new AtomicInteger();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            calls.incrementAndGet();
            return Optional.of(List.of(1, 1));
        }));

        Compute.d6(2);

        assertEquals(0, calls.get());
    }

    @Test
    void cancelledPromptFallsBackWithoutThrowing() {
        ManualRandom rng = new ManualRandom((n, purpose) -> Optional.empty());

        Roll roll = ManualDice.withPurpose("To-hit", () -> rng.d6(2));

        assertEquals(2, roll.getIntValues().length);
        for (int face : roll.getIntValues()) {
            assertTrue((face >= 1) && (face <= 6), "fallback face " + face);
        }
    }

    @Test
    void computeUsesManualRandomWhenPurposeIsSet() {
        Compute.setRNG(new ManualRandom((n, purpose) -> Optional.of(List.of(2, 6))));

        assertEquals(8, ManualDice.withPurpose("To-hit", () -> Compute.d6(2)));
    }

    @Test
    void sourceReceivesTheRollPurpose() {
        AtomicReference<String> seen = new AtomicReference<>();
        ManualRandom rng = new ManualRandom((n, purpose) -> {
            seen.set(purpose);
            return Optional.of(List.of(4, 2));
        });

        ManualDice.withPurpose("Hit location", () -> rng.d6(2));

        assertEquals("Hit location", seen.get());
    }

    @Test
    void hitLocationDoesNotPromptOutsideWeaponAttack() {
        AtomicInteger calls = new AtomicInteger();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            calls.incrementAndGet();
            return Optional.of(List.of(3, 4));
        }));

        CombatRollStackFrames.rollHitLocation();

        assertEquals(0, calls.get());
    }

    @Test
    void clusterHitsDoNotPromptOutsideWeaponAttack() {
        AtomicInteger calls = new AtomicInteger();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            calls.incrementAndGet();
            return Optional.of(List.of(6, 6));
        }));

        Compute.missilesHit(10);

        assertEquals(0, calls.get());
    }

    @Test
    void hitLocationPromptsDuringHumanWeaponAttack() {
        AtomicReference<String> seen = new AtomicReference<>();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            seen.set(purpose);
            return Optional.of(List.of(3, 4));
        }));
        String summary = "Archer ARC-2K (Phoenix) fires Medium Laser at Locust LCT-1V (Princess)";

        ManualDice.duringWeaponAttack(summary, CombatRollStackFrames::rollHitLocation);

        assertEquals("Hit location\n\n" + summary, seen.get());
    }

    @Test
    void clusterHitsPromptDuringHumanWeaponAttack() {
        AtomicReference<String> seen = new AtomicReference<>();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            seen.set(purpose);
            return Optional.of(List.of(6, 6));
        }));
        String summary = "Archer ARC-2K (Phoenix) fires LRM 10 at Locust LCT-1V (Princess)";

        ManualDice.duringWeaponAttack(summary, () -> Compute.missilesHit(10));

        assertEquals("Cluster hits\n\n" + summary, seen.get());
    }

    @Test
    void criticalHitsPromptDuringHumanWeaponAttack() {
        AtomicReference<String> seen = new AtomicReference<>();
        Compute.setRNG(new ManualRandom((n, purpose) -> {
            seen.set(purpose);
            return Optional.of(List.of(5));
        }));
        String summary = "Archer ARC-2K (Phoenix) fires Medium Laser at Locust LCT-1V (Princess)";

        ManualDice.duringWeaponAttack(summary, CombatRollStackFrames::addCritical);

        assertEquals("Critical hit\n\n" + summary, seen.get());
    }

    @Test
    void parseFacesAcceptsSpacesAndCommas() {
        assertEquals(List.of(4, 2), ManualRandom.parseFaces("4 2", 2));
        assertEquals(List.of(4, 2), ManualRandom.parseFaces("4,2", 2));
        assertEquals(List.of(6), ManualRandom.parseFaces("6", 1));
    }

    @Test
    void parseFacesRejectsWrongCountAndOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("4", 2));
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("0 3", 2));
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("3 7", 2));
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("8", 1));
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("two", 1));
        assertThrows(IllegalArgumentException.class, () -> ManualRandom.parseFaces("  ", 1));
    }

    @Test
    void purposeIsClearedAfterWeaponToHit() {
        Compute.setRNG(new ManualRandom((n, purpose) -> Optional.of(List.of(3, 3))));
        ManualDice.withPurpose("To-hit\n\nUnit fires weapon at target\nNeeds 8", () -> Compute.d6(2));
        assertNull(ManualDice.purpose());
    }
}
