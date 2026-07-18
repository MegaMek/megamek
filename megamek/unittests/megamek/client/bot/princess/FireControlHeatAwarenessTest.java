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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.game.Game;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the environmental / movement / TSM heat awareness added to {@link FireControl}. These
 * exercise the shared {@code FireControl} methods used by both Princess and CASPAR (CASPAR extends
 * Princess and reuses this class), so they cover both bots.
 *
 * @author The MegaMek Team
 */
class FireControlHeatAwarenessTest {

    private static final int HEAT_CAPACITY = 10;

    private FireControl fireControl;
    private Game mockGame;
    private PlanetaryConditions planetaryConditions;

    @BeforeEach
    void setUp() {
        fireControl = new FireControl(mock(Princess.class));

        planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setTemperature(25); // temperate baseline

        mockGame = mock(Game.class);
        when(mockGame.getPlanetaryConditions()).thenReturn(planetaryConditions);
    }

    private BipedMek newHeatTrackingMek() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.getHeatCapacity()).thenReturn(HEAT_CAPACITY);
        when(mek.getHeat()).thenReturn(0);
        when(mek.isStealthOn()).thenReturn(false);
        when(mek.isAero()).thenReturn(false);
        when(mek.isSpaceborne()).thenReturn(false);
        when(mek.getGame()).thenReturn(mockGame);
        when(mek.getShortName()).thenReturn("Test Mek");
        return mek;
    }

    // --- predictEnvironmentalHeat -------------------------------------------------------------

    @Test
    void temperateMapAddsNoEnvironmentalHeat() {
        planetaryConditions.setTemperature(25);
        assertEquals(0, fireControl.predictEnvironmentalHeat(newHeatTrackingMek()));
    }

    @Test
    void hotMapAddsEnvironmentalHeat() {
        planetaryConditions.setTemperature(70); // (70 - 50) / 10 = 2 points added
        assertEquals(2, fireControl.predictEnvironmentalHeat(newHeatTrackingMek()));
    }

    @Test
    void coldMapRemovesHeatAsFreeCooling() {
        planetaryConditions.setTemperature(-50); // (-30 - -50) / 10 = 2 points removed
        assertEquals(-2, fireControl.predictEnvironmentalHeat(newHeatTrackingMek()));
    }

    @Test
    void heatDissipatingArmorHalvesTheHotPenalty() {
        planetaryConditions.setTemperature(90); // (90 - 50) / 10 = 4 points, halved to 2
        BipedMek mek = newHeatTrackingMek();
        when(mek.hasIntactHeatDissipatingArmor()).thenReturn(true);
        assertEquals(2, fireControl.predictEnvironmentalHeat(mek));
    }

    @Test
    void spacebornUnitIgnoresEnvironmentalHeat() {
        planetaryConditions.setTemperature(90);
        BipedMek mek = newHeatTrackingMek();
        when(mek.isSpaceborne()).thenReturn(true);
        assertEquals(0, fireControl.predictEnvironmentalHeat(mek));
    }

    // --- calcHeatTolerance -------------------------------------------------------------------

    @Test
    void temperateToleranceMatchesLegacyValue() {
        // capacity 10 - heat 0 + 5 non-aero slack = 15
        assertEquals(15, fireControl.calcHeatTolerance(newHeatTrackingMek(), false));
    }

    @Test
    void hotMapLowersHeatTolerance() {
        planetaryConditions.setTemperature(70); // +2 environmental heat
        // capacity 10 - (heat 0 + environmental 2) + 5 = 13, i.e. 2 lower than the temperate 15
        assertEquals(13, fireControl.calcHeatTolerance(newHeatTrackingMek(), false));
    }

    @Test
    void coldMapRaisesHeatTolerance() {
        planetaryConditions.setTemperature(-50); // -2 environmental heat -> +2 tolerance
        // capacity 10 - (-2) + 5 = 17
        assertEquals(17, fireControl.calcHeatTolerance(newHeatTrackingMek(), false));
    }

    @Test
    void committedMovementHeatLowersHeatTolerance() {
        BipedMek mek = newHeatTrackingMek();
        mek.heatBuildup = 3; // e.g. jump heat already committed this turn
        // capacity 10 - (heat 0 + committed 3) + 5 = 12
        assertEquals(12, fireControl.calcHeatTolerance(mek, false));
    }

    @Test
    void predictedMovementHeatLowersHeatTolerance() {
        // capacity 10 - (heat 0 + predicted move 3) + 5 = 12
        assertEquals(12, fireControl.calcHeatTolerance(newHeatTrackingMek(), false, 3));
    }

    @Test
    void heatActivatedTsmMekGetsRaisedTolerance() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.hasTSM(false)).thenReturn(true);
        // TSM tolerance = (capacity 10 - load 0) + ceiling 13 = 23, above the normal non-TSM 15, so the
        // overheat penalty does not fight the Mek's climb to the activation band.
        assertEquals(HEAT_CAPACITY + FireControl.TSM_HEAT_CEILING,
              fireControl.calcHeatTolerance(mek, false));
    }

    @Test
    void nonTsmMekKeepsNormalTolerance() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.hasTSM(false)).thenReturn(false);
        assertEquals(15, fireControl.calcHeatTolerance(mek, false));
    }

    // --- projectedEndOfTurnHeat (dissipation-aware) ------------------------------------------

    @Test
    void projectedEndOfTurnHeatSubtractsDissipation() {
        // capacity 10: current heat 15 + weapon heat 4 - 10 dissipation = 9
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        assertEquals(9, fireControl.projectedEndOfTurnHeat(mek, 4));
    }

    @Test
    void projectedEndOfTurnHeatFloorsAtZeroWhenHeatIsShed() {
        // capacity 10 exceeds current heat 3 + weapon heat 4, so all heat dissipates
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(3);
        assertEquals(0, fireControl.projectedEndOfTurnHeat(mek, 4));
    }

    // --- TSM incentive -----------------------------------------------------------------------

    @Test
    void nonTsmMekGetsNoHeatIncentive() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(false);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4);

        fireControl.applyTsmHeatIncentive(mek, plan);

        verify(plan, never()).setUtility(anyDouble());
    }

    @Test
    void tsmMekReachingActivationHeatGetsFullBonus() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4); // end-of-turn heat 15 + 4 - 10 = 9 -> TSM active
        when(plan.getUtility()).thenReturn(100.0);

        fireControl.applyTsmHeatIncentive(mek, plan);

        verify(plan).setUtility(100.0 + FireControl.TSM_ACTIVATION_UTILITY);
    }

    @Test
    void tsmMekBelowActivationHeatGetsPartialBonus() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(2); // end-of-turn heat 15 + 2 - 10 = 7 -> partial 7/9
        when(plan.getUtility()).thenReturn(100.0);

        fireControl.applyTsmHeatIncentive(mek, plan);

        double expectedBonus = FireControl.TSM_ACTIVATION_UTILITY * (7.0 / FireControl.TSM_DESIRED_HEAT);
        verify(plan).setUtility(100.0 + expectedBonus);
    }

    @Test
    void tsmMekOvershootAboveTargetGetsTaperedBonus() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(17);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4); // end-of-turn heat 17 + 4 - 10 = 11, i.e. 2 into the safe band
        when(plan.getUtility()).thenReturn(100.0);

        fireControl.applyTsmHeatIncentive(mek, plan);

        int band = FireControl.TSM_HEAT_CEILING - FireControl.TSM_DESIRED_HEAT;
        double expectedBonus = FireControl.TSM_ACTIVATION_UTILITY * (1.0 - 2.0 / band);
        verify(plan).setUtility(100.0 + expectedBonus);
    }

    @Test
    void tsmMekAtCeilingGetsNoBonus() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(19);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4); // end-of-turn heat 19 + 4 - 10 = 13 = ceiling -> reward tapered to 0

        fireControl.applyTsmHeatIncentive(mek, plan);

        verify(plan, never()).setUtility(anyDouble());
    }

    @Test
    void tsmMekWithNoWeaponHeatGetsNoBonus() {
        // A Mek already at TSM heat from passive sources (here current heat 20 vs capacity 10) must not be
        // rewarded for a plan that fires nothing, since such a plan builds no heat toward activation.
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(20);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(0);

        fireControl.applyTsmHeatIncentive(mek, plan);

        verify(plan, never()).setUtility(anyDouble());
    }

    @Test
    void wellCooledTsmMekGetsNoBonusWhenHeatFullyDissipates() {
        // Regression for the Commando COM-7T playtest: a TSM Mek whose sinks shed everything it fires
        // never reaches the threshold, so it must earn no bonus (heat 0 + plan 9 - capacity 10 = 0).
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(0);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(9);

        fireControl.applyTsmHeatIncentive(mek, plan);

        verify(plan, never()).setUtility(anyDouble());
    }

    // --- firingActivatesTsm (spot-gate protection) -------------------------------------------

    @Test
    void firingActivatesTsmWhenShotReachesThreshold() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15); // without plan: 15 - 10 = 5 (below 9)
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4); // with plan: 15 + 4 - 10 = 9 -> activates
        assertTrue(fireControl.firingActivatesTsm(mek, plan));
    }

    @Test
    void firingDoesNotActivateTsmWhenShotFallsShort() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(2); // with plan: 15 + 2 - 10 = 7 -> still below 9
        assertFalse(fireControl.firingActivatesTsm(mek, plan));
    }

    @Test
    void firingDoesNotActivateTsmWhenAlreadyHot() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(20); // without plan: 20 - 10 = 10, already active
        when(mek.hasTSM(false)).thenReturn(true);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(3);
        assertFalse(fireControl.firingActivatesTsm(mek, plan));
    }

    @Test
    void firingNeverActivatesTsmForNonTsmMek() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(false);
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getHeat()).thenReturn(4);
        assertFalse(fireControl.firingActivatesTsm(mek, plan));
    }

    @Test
    void tsmMekPrefersHotterPlanTowardActivation() {
        BipedMek mek = newHeatTrackingMek();
        when(mek.getHeat()).thenReturn(15);
        when(mek.hasTSM(false)).thenReturn(true);

        FiringPlan coolPlan = mock(FiringPlan.class);
        when(coolPlan.getHeat()).thenReturn(1); // end-of-turn heat 15 + 1 - 10 = 6
        when(coolPlan.getUtility()).thenReturn(50.0);

        FiringPlan hotPlan = mock(FiringPlan.class);
        when(hotPlan.getHeat()).thenReturn(4); // end-of-turn heat 15 + 4 - 10 = 9 -> activates
        when(hotPlan.getUtility()).thenReturn(50.0);

        double coolBonus = FireControl.TSM_ACTIVATION_UTILITY * (6.0 / FireControl.TSM_DESIRED_HEAT);
        double hotBonus = FireControl.TSM_ACTIVATION_UTILITY;

        // The plan that reaches the activation threshold earns the larger incentive.
        assertTrue(hotBonus > coolBonus);

        fireControl.applyTsmHeatIncentive(mek, coolPlan);
        fireControl.applyTsmHeatIncentive(mek, hotPlan);
        verify(coolPlan).setUtility(50.0 + coolBonus);
        verify(hotPlan).setUtility(50.0 + hotBonus);
    }
}
