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

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that pilot hits only count towards a unit's damage level when the caller asks for the crew to be checked.
 * MekHQ reads the damage level without the crew ({@code getDamageLevel(false)}), so a fully repaired unit with an
 * injured pilot must report {@link Entity#DMG_NONE} there. Forced withdrawal and the other in-game checks use the
 * no-argument methods, which must keep counting the pilot (MegaMek/megamek#9719).
 */
class CrippledCrewCheckTest {
    private static final int CRIPPLING_PILOT_HITS = 4;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * Pilot hits paired with the damage level they produce when the crew is checked.
     */
    static Stream<Arguments> pilotHitsAndDamageLevels() {
        return Stream.of(
              Arguments.of(1, Entity.DMG_LIGHT),
              Arguments.of(2, Entity.DMG_MODERATE),
              Arguments.of(3, Entity.DMG_HEAVY),
              Arguments.of(CRIPPLING_PILOT_HITS, Entity.DMG_CRIPPLED));
    }

    @Nested
    @DisplayName("Mek")
    class MekTests {
        @Test
        @DisplayName("4-hit pilot does not cripple the Mek when crew is not checked")
        void ignoresPilotHitsWhenCrewNotChecked() {
            BipedMek mek = createUndamagedMek(CRIPPLING_PILOT_HITS);

            assertFalse(mek.isCrippled(false));
        }

        @Test
        @DisplayName("4-hit pilot cripples the Mek when crew is checked")
        void countsPilotHitsWhenCrewChecked() {
            BipedMek mek = createUndamagedMek(CRIPPLING_PILOT_HITS);

            assertTrue(mek.isCrippled(true));
            assertTrue(mek.isCrippled(), "isCrippled() must keep counting the pilot for forced withdrawal");
        }

        @ParameterizedTest(name = "{0} pilot hits")
        @MethodSource("megamek.common.units.CrippledCrewCheckTest#pilotHitsAndDamageLevels")
        @DisplayName("Pilot hits only change the Mek's damage level when crew is checked")
        void damageLevelFollowsCrewCheck(int pilotHits, int expectedDamageLevelWithCrew) {
            BipedMek mek = createUndamagedMek(pilotHits);

            assertEquals(Entity.DMG_NONE, mek.getDamageLevel(false));
            assertEquals(expectedDamageLevelWithCrew, mek.getDamageLevel(true));
            assertEquals(expectedDamageLevelWithCrew, mek.getDamageLevel());
        }
    }

    @Nested
    @DisplayName("ProtoMek")
    class ProtoMekTests {
        @Test
        @DisplayName("4-hit pilot does not cripple the ProtoMek when crew is not checked")
        void ignoresPilotHitsWhenCrewNotChecked() throws LocationFullException {
            ProtoMek protoMek = createUndamagedProtoMek(CRIPPLING_PILOT_HITS);

            assertFalse(protoMek.isCrippled(false));
        }

        @Test
        @DisplayName("4-hit pilot cripples the ProtoMek when crew is checked")
        void countsPilotHitsWhenCrewChecked() throws LocationFullException {
            ProtoMek protoMek = createUndamagedProtoMek(CRIPPLING_PILOT_HITS);

            assertTrue(protoMek.isCrippled(true));
            assertTrue(protoMek.isCrippled(), "isCrippled() must keep counting the pilot for forced withdrawal");
        }

        @ParameterizedTest(name = "{0} pilot hits")
        @MethodSource("megamek.common.units.CrippledCrewCheckTest#pilotHitsAndDamageLevels")
        @DisplayName("Pilot hits only change the ProtoMek's damage level when crew is checked")
        void damageLevelFollowsCrewCheck(int pilotHits, int expectedDamageLevelWithCrew)
              throws LocationFullException {
            ProtoMek protoMek = createUndamagedProtoMek(pilotHits);

            assertEquals(Entity.DMG_NONE, protoMek.getDamageLevel(false));
            assertEquals(expectedDamageLevelWithCrew, protoMek.getDamageLevel(true));
            assertEquals(expectedDamageLevelWithCrew, protoMek.getDamageLevel());
        }
    }

    @Nested
    @DisplayName("Aerospace fighter")
    class AeroTests {
        @ParameterizedTest(name = "{0} pilot hits")
        @MethodSource("megamek.common.units.CrippledCrewCheckTest#pilotHitsAndDamageLevels")
        @DisplayName("Pilot hits only change the fighter's damage level when crew is checked")
        void damageLevelFollowsCrewCheck(int pilotHits, int expectedDamageLevelWithCrew) {
            AeroSpaceFighter fighter = createUndamagedFighter(pilotHits);

            assertEquals(Entity.DMG_NONE, fighter.getDamageLevel(false));
            assertEquals(expectedDamageLevelWithCrew, fighter.getDamageLevel(true));
            assertEquals(expectedDamageLevelWithCrew, fighter.getDamageLevel());
        }
    }

    private static BipedMek createUndamagedMek(int pilotHits) {
        BipedMek mek = new BipedMek();
        mek.setWeight(40);
        mek.autoSetInternal();
        // Full armor everywhere, otherwise the zero-armor Mek reads as heavily damaged
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(5, location);
        }
        mek.setCrew(createCrewWithHits(pilotHits));
        return mek;
    }

    private static ProtoMek createUndamagedProtoMek(int pilotHits) throws LocationFullException {
        ProtoMek protoMek = new ProtoMek();
        protoMek.addEquipment(EquipmentType.get("CLERMediumLaser"), ProtoMek.LOC_TORSO);
        for (int location = 0; location < protoMek.locations(); location++) {
            protoMek.initializeArmor(5, location);
        }
        protoMek.setCrew(createCrewWithHits(pilotHits));
        return protoMek;
    }

    private static AeroSpaceFighter createUndamagedFighter(int pilotHits) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setOSI(10);
        for (int location = 0; location < fighter.locations(); location++) {
            fighter.initializeArmor(10, location);
        }
        fighter.setCrew(createCrewWithHits(pilotHits));
        return fighter;
    }

    private static Crew createCrewWithHits(int pilotHits) {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setHits(pilotHits, 0);
        return crew;
    }
}
