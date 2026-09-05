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

import java.util.Map;

import megamek.client.ratgenerator.TransportCalculator.BayLedger;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.PlatoonType;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Covers how DropShip lift is sized: which bays a unit may ride in, and which hulls are a sensible size for what
 * is still to be carried.
 */
class TransportCalculatorTest {

    /** An Excalibur (2786): 90 heavy vehicle bays, 12 Mek bays, no light vehicle bays. */
    private static final Map<Integer, Integer> EXCALIBUR = Map.of(UnitType.TANK, 90, UnitType.MEK, 12);
    /** A Triumph (2593): 45 heavy and 8 light vehicle bays. */
    private static final Map<Integer, Integer> TRIUMPH = Map.of(UnitType.TANK, 45, UnitType.VTOL, 8);
    /** An Overlord (2775) Combined Arms: 24 Mek bays, 12 heavy vehicle bays, 6 fighter bays. */
    private static final Map<Integer, Integer> OVERLORD_COMBINED_ARMS =
          Map.of(UnitType.MEK, 24, UnitType.TANK, 12, UnitType.AEROSPACE_FIGHTER, 6);

    @Test
    void lightVehiclesRideInHeavyBaysLeftOver() {
        BayLedger ledger = new BayLedger(Map.of());
        ledger.add(EXCALIBUR);

        assertEquals(90, ledger.free(UnitType.VTOL), "A heavy bay takes a light vehicle");
        ledger.claim(UnitType.TANK, 10);
        assertEquals(80, ledger.free(UnitType.VTOL), "Only what the heavy vehicles left");
        assertEquals(80, ledger.free(UnitType.TANK));
    }

    @Test
    void aHeavyVehicleCannotUseALightBay() {
        BayLedger ledger = new BayLedger(Map.of());
        ledger.add(TRIUMPH);

        assertEquals(45, ledger.free(UnitType.TANK));
        assertEquals(53, ledger.free(UnitType.VTOL));
    }

    @Test
    void lightVehiclesFillTheirOwnBaysBeforeBorrowingHeavyOnes() {
        BayLedger ledger = new BayLedger(Map.of());
        ledger.add(TRIUMPH);

        ledger.claim(UnitType.VTOL, 10);

        assertEquals(43, ledger.free(UnitType.TANK), "8 light bays first, then 2 heavy bays");
        assertEquals(43, ledger.free(UnitType.VTOL));
    }

    @Test
    void mekBaysAreNotVehicleBays() {
        BayLedger ledger = new BayLedger(Map.of());
        ledger.add(EXCALIBUR);

        assertEquals(12, ledger.free(UnitType.MEK));
        ledger.claim(UnitType.MEK, 12);
        assertEquals(0, ledger.free(UnitType.MEK));
        assertEquals(90, ledger.free(UnitType.TANK), "Meks took nothing from the vehicle bays");
    }

    @Test
    void hullsAccumulate() {
        BayLedger ledger = new BayLedger(Map.of());
        ledger.add(TRIUMPH);
        ledger.add(TRIUMPH);

        assertEquals(106, ledger.free(UnitType.VTOL));
    }

    @Test
    void aHullIsAReasonableFitWhenItWouldBeFilledAndCarriesAUsefulShare() {
        // Sixteen vehicles: a Gazelle's 15 bays are a fit, an Excalibur's 90 are not.
        assertTrue(TransportCalculator.isReasonableFit(15, 16));
        assertFalse(TransportCalculator.isReasonableFit(90, 16));
        // A Mek battalion: Unions, or an Overlord that is exactly the job, not a string of Leopards.
        assertTrue(TransportCalculator.isReasonableFit(12, 36));
        assertTrue(TransportCalculator.isReasonableFit(36, 36));
        assertFalse(TransportCalculator.isReasonableFit(4, 36));
        // A hull that would sail with empty bays is not a fit, however slight the excess.
        assertFalse(TransportCalculator.isReasonableFit(37, 36));
        // The last four Meks take a Leopard, not another Union.
        assertTrue(TransportCalculator.isReasonableFit(4, 4));
        assertFalse(TransportCalculator.isReasonableFit(12, 4));
        // A lone unit fits only a single-bay hull; the fallback finds the smallest hull otherwise.
        assertTrue(TransportCalculator.isReasonableFit(1, 1));
        assertFalse(TransportCalculator.isReasonableFit(2, 1));
    }

    @Test
    void aCombinedArmsHullIsNotDrawnForALoneTankCompany() {
        BayLedger ledger = new BayLedger(Map.of(UnitType.TANK, 12));

        assertFalse(ledger.wouldMostlyUse(OVERLORD_COMBINED_ARMS), "12 of 42 bays used is a hull sailing empty");
        assertFalse(ledger.wouldMostlyUse(TRIUMPH), "12 of 53 is no better");
        assertTrue(ledger.wouldMostlyUse(Map.of(UnitType.TANK, 12)), "A Condor's twelve vehicle bays are the job");
    }

    @Test
    void aMechanizedPlatoonNeedsBaySpacePerSquad() {
        Infantry footPlatoon = new ConvInfantry();
        footPlatoon.setMovementMode(EntityMovementMode.INF_LEG);
        footPlatoon.setSquadCount(4);
        assertEquals(PlatoonType.FOOT.getWeight(), TransportCalculator.infantryLiftTons(footPlatoon));

        Infantry mechanizedPlatoon = new ConvInfantry();
        mechanizedPlatoon.setMovementMode(EntityMovementMode.TRACKED);
        mechanizedPlatoon.setSquadCount(4);
        assertEquals(PlatoonType.MECHANIZED.getWeight() * 4, TransportCalculator.infantryLiftTons(mechanizedPlatoon),
              "A mechanized platoon takes a cubicle per squad, as the bay charges it");
    }

    @Test
    void infantryBayTonnageCountsAsPlatoonBerths() {
        // A Fury: 8 light vehicle bays and 4 foot platoon bays, which the data records as 20 tons.
        Map<Integer, Integer> fury = Map.of(UnitType.VTOL, 8, UnitType.INFANTRY, 20);

        BayLedger lightVehiclesOnly = new BayLedger(Map.of(UnitType.VTOL, 12));
        assertTrue(lightVehiclesOnly.wouldMostlyUse(fury), "8 of 12 berths used, not 8 of 28");

        BayLedger infantryOnly = new BayLedger(Map.of(UnitType.INFANTRY, 10));
        assertFalse(infantryOnly.wouldMostlyUse(fury), "Two platoons use 2 of 12 berths");
    }

    @Test
    void aCombinedArmsHullIsDrawnForACombinedArmsForce() {
        BayLedger ledger = new BayLedger(Map.of(UnitType.MEK, 36, UnitType.TANK, 12));

        assertTrue(ledger.wouldMostlyUse(OVERLORD_COMBINED_ARMS), "36 of 42 bays used");
    }

    @Test
    void whatIsAlreadyBerthedOrWaitingNoLongerCountsAsANeed() {
        BayLedger ledger = new BayLedger(Map.of(UnitType.MEK, 36));
        assertEquals(36, ledger.unmet(UnitType.MEK));

        ledger.add(Map.of(UnitType.MEK, 12));
        assertEquals(24, ledger.unmet(UnitType.MEK), "A hull drawn but not yet claimed still counts as waiting");
        ledger.claim(UnitType.MEK, 12);
        assertEquals(24, ledger.unmet(UnitType.MEK));

        ledger.add(EXCALIBUR);
        ledger.add(EXCALIBUR);
        ledger.claim(UnitType.MEK, 24);
        assertEquals(0, ledger.unmet(UnitType.MEK));
        assertFalse(ledger.wouldMostlyUse(EXCALIBUR), "Nothing is left that would use it");
    }
}
