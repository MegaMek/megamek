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

import megamek.common.MPCalculationSetting;
import megamek.common.compute.Compute;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Magnetic Pulse (MP, IO p.62) and Improved Magnetic Pulse (iATM IMP) missile effects modeled on
 * {@link Entity}. These exercise the real effect math (caps, fusion gating, the effect window threshold) rather than
 * constants.
 */
class MagneticPulseTest {

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
    }

    private static Mek fusionMek() {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        // Valid internals so getWalkMP()'s leg-damage logic doesn't treat legs as destroyed.
        mek.setInternal(3, 16, 12, 8, 12);
        return mek;
    }

    private static Mek iceMek() {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.COMBUSTION_ENGINE, 0));
        mek.setInternal(3, 16, 12, 8, 12);
        return mek;
    }

    @Nested
    @DisplayName("Improved Magnetic Pulse (iATM IMP) effect math")
    class ImpEffectTests {

        @Test
        @DisplayName("Three IMP warheads give +1 to-hit, -1 MP, ECM and +1 heat on a fusion unit")
        void threeWarheadsApplyOneStepOfEffect() {
            Mek mek = fusionMek();
            mek.addIMPHits(3);

            assertEquals(1, mek.getImpToHitModifier(), "3 warheads -> +1 to-hit");
            assertEquals(1, mek.getImpMpReduction(), "3 warheads -> -1 MP");
            assertTrue(mek.isImpEcmAffected(), "3 warheads -> treated as in hostile ECM");
            assertEquals(1, mek.heatFromExternal, "3 warheads -> +1 outside heat on a fusion unit");
        }

        @Test
        @DisplayName("Fewer than three warheads produce no to-hit, ECM or heat effect")
        void twoWarheadsProduceNoEffect() {
            Mek mek = fusionMek();
            mek.addIMPHits(2);

            assertEquals(0, mek.getImpToHitModifier(), "2 warheads -> no to-hit penalty");
            assertEquals(0, mek.getImpMpReduction(), "2 warheads -> no MP reduction");
            assertFalse(mek.isImpEcmAffected(), "2 warheads -> not yet ECM affected");
            assertEquals(0, mek.heatFromExternal, "2 warheads -> no heat yet");
        }

        @Test
        @DisplayName("To-hit and MP effects cap at +2/-2 for a Mek even with many hits")
        void effectCapsForMek() {
            Mek mek = fusionMek();
            mek.addIMPHits(12); // 12/3 = 4 steps, but capped at 2

            assertEquals(2, mek.getImpToHitModifier(), "Mek to-hit caps at +2");
            assertEquals(2, mek.getImpMpReduction(), "Mek MP reduction caps at -2");
        }

        @Test
        @DisplayName("Non-fusion units ignore MP reduction and heat but still take the to-hit and ECM")
        void nonFusionIgnoresMpAndHeat() {
            Mek mek = iceMek();
            mek.addIMPHits(6);

            assertEquals(2, mek.getImpToHitModifier(), "to-hit applies to non-fusion units");
            assertTrue(mek.isImpEcmAffected(), "ECM applies to non-fusion units");
            assertEquals(0, mek.getImpMpReduction(), "non-fusion units ignore MP reduction");
            assertEquals(0, mek.heatFromExternal, "non-fusion units ignore IMP heat");
        }

        @Test
        @DisplayName("Walk MP actually drops on a real BipedMek (regression: subclasses override getWalkMP)")
        void walkMpReducedOnBipedMek() {
            Mek mek = fusionMek();
            int baseWalk = mek.getWalkMP(MPCalculationSetting.NO_GRAVITY);
            mek.addIMPHits(6); // reduction of 2 (capped)
            int reducedWalk = mek.getWalkMP(MPCalculationSetting.NO_GRAVITY);

            assertEquals(2, baseWalk - reducedWalk, "IMP should drop a fusion BipedMek's Walk MP by 2");
        }

        @Test
        @DisplayName("Non-fusion BipedMek Walk MP is not reduced by IMP")
        void walkMpNotReducedForNonFusion() {
            Mek mek = iceMek();
            int baseWalk = mek.getWalkMP(MPCalculationSetting.NO_GRAVITY);
            mek.addIMPHits(6);
            int reducedWalk = mek.getWalkMP(MPCalculationSetting.NO_GRAVITY);

            assertEquals(0, baseWalk - reducedWalk, "Non-fusion units ignore the IMP MP reduction");
        }

        @Test
        @DisplayName("ProtoMeks cap at +3/-3 instead of +2/-2")
        void protoMekHasHigherCaps() {
            ProtoMek protoMek = new ProtoMek();
            protoMek.setWeight(5);
            protoMek.setEngine(new Engine(40, Engine.NORMAL_ENGINE, 0));
            protoMek.addIMPHits(12); // 4 steps, capped at 3 for ProtoMeks

            assertEquals(3, protoMek.getImpToHitModifier(), "ProtoMek to-hit caps at +3");
            assertEquals(3, protoMek.getImpMpReduction(), "ProtoMek MP reduction caps at -3");
        }
    }

    @Nested
    @DisplayName("Standard Magnetic Pulse (MP) effect")
    class StandardMpEffectTests {

        @Test
        @DisplayName("A hit sets the +1 to-hit window and banks LRM heat on a fusion unit")
        void lrmHitBanksHeatAndSetsToHit() {
            Mek mek = fusionMek();
            mek.applyMagneticPulse(10, 5); // 10 LRM warheads, +1 heat per 5

            assertTrue(mek.getMagneticPulseRounds() > 0, "MP hit sets the to-hit window");
            assertEquals(2, mek.heatFromExternal, "10 LRM warheads / 5 -> +2 heat");
        }

        @Test
        @DisplayName("SRM heat uses a divisor of 3")
        void srmHitUsesDivisorThree() {
            Mek mek = fusionMek();
            mek.applyMagneticPulse(6, 3); // 6 SRM warheads, +1 heat per 3

            assertEquals(2, mek.heatFromExternal, "6 SRM warheads / 3 -> +2 heat");
        }

        @Test
        @DisplayName("Heat remainder carries across salvos in the same turn (aligns with IMP)")
        void heatRemainderCarriesAcrossSalvos() {
            Mek mek = fusionMek();
            // Two SRM salvos of 2 warheads each: per-salvo flooring would give 0+0, but the remainder
            // carries so the combined 4 warheads / 3 = +1 heat.
            mek.applyMagneticPulse(2, 3);
            mek.applyMagneticPulse(2, 3);

            assertEquals(1, mek.heatFromExternal, "2 + 2 SRM warheads across salvos -> +1 heat (carried)");
        }

        @Test
        @DisplayName("LRM and SRM heat rates stay separate in the same turn (MML mixed modes)")
        void mixedLrmSrmHeatRatesDoNotCrossContaminate() {
            Mek mek = fusionMek();
            // An MML firing LRM mode then SRM mode: the LRM remainder must not be divided by the SRM
            // rate. Correct result is floor(7/5) + floor(5/3) = 1 + 1 = 2, not 3.
            mek.applyMagneticPulse(7, 5); // LRM rate
            mek.applyMagneticPulse(5, 3); // SRM rate

            assertEquals(2, mek.heatFromExternal, "7 LRM/5 + 5 SRM/3 -> +2 heat (rates not mixed)");
        }

        @Test
        @DisplayName("Non-fusion units take the to-hit window but no heat")
        void nonFusionTakesToHitButNoHeat() {
            Mek mek = iceMek();
            mek.applyMagneticPulse(10, 5);

            assertTrue(mek.getMagneticPulseRounds() > 0, "to-hit window still applies");
            assertEquals(0, mek.heatFromExternal, "non-fusion units take no MP heat");
        }
    }

    @Nested
    @DisplayName("Battle armor firing with zero shooting strength")
    class ZeroShootingStrengthTests {

        @Test
        @DisplayName("missilesHit returns 0 for a zero-missile pack instead of throwing")
        void missilesHitZeroReturnsZero() {
            // A battle armor squad with every trooper disabled by IMP missiles has a shooting
            // strength of 0, so its missile weapons resolve as rackSize * 0 = 0 missiles. This must
            // resolve to no hits rather than throwing "Could not find number of missiles in hit table".
            assertEquals(0, Compute.missilesHit(0, 0, false, false, false), "0 missiles -> 0 hits");
            assertEquals(0, Compute.missilesHit(0, 0, false), "0 missiles -> 0 hits (legacy overload)");
        }
    }

    @Nested
    @DisplayName("MagneticPulseState window and heat-remainder lifecycle")
    class StateLifecycleTests {

        @Test
        @DisplayName("A standard MP re-hit while already active does not extend the window")
        void reHitDoesNotExtendWindow() {
            MagneticPulseState state = new MagneticPulseState();
            state.applyStandardPulse(); // window starts at 2
            state.newRound();           // 2 -> 1
            state.applyStandardPulse(); // re-hit while active must NOT restart the window

            assertEquals(1, state.getStandardRounds(), "re-hit while active does not extend the window");
        }

        @Test
        @DisplayName("Heat remainders do not carry across turns")
        void heatRemainderClearsEachTurn() {
            MagneticPulseState state = new MagneticPulseState();
            state.computeStandardHeat(2, MagneticPulseState.SRM_HEAT_DIVISOR); // remainder 2, no heat yet
            state.newRound();                                                   // remainder must clear

            int heat = state.computeStandardHeat(2, MagneticPulseState.SRM_HEAT_DIVISOR);
            assertEquals(0, heat, "next turn's 2 warheads alone (2/3) give no heat; last turn's remainder is gone");
        }
    }
}
