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

package megamek.common.rules.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.game.Game;
import megamek.common.rules.RulesGame;
import megamek.common.rules.RulesManager;
import megamek.common.rules.totalwarfare.TWRulesArmor;
import megamek.common.rules.totalwarfare.TWRulesEquipment;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Rule values the Core ruleset takes from the printed Core Rulebook, and the Total Warfare values it keeps for
 * equipment and situations the Core Rulebook does not cover.
 */
class CoreRulesScopeTest {

    @Test
    void aFallIntoWaterCountsTheDepthOnceAndIsHalved() {
        CoreRulesPSR psr = new CoreRulesPSR();
        // Core p.115: a 50-ton Mek falling from a Level 2 hill into Depth 2 water falls four levels, 5 x 5 / 2
        assertEquals(12, psr.reduceFallDamageIntoWater(0, 2, 4, 50).waterDamage());
        assertEquals(0, psr.reduceFallDamageIntoWater(0, 2, 4, 50).damage());
        // Standing in Depth 1 water: a 0-level fall, halved
        assertEquals(2, psr.reduceFallDamageIntoWater(0, 1, 0, 50).waterDamage());
    }

    @ParameterizedTest(name = "{0} physical damage leaves {1}")
    @CsvSource({ "1, 1", "2, 1", "3, 2", "4, 2", "5, 3", "6, 4", "10, 6" })
    void impactResistantArmorReducesPhysicalDamageByOnePerThreeOrFraction(int damage, int expected) {
        // Core p.202 and IO:AE p.82: "1 point for every 3 points delivered (or fraction thereof)", minimum 1
        HitData hit = new HitData(Mek.LOC_CENTER_TORSO);
        assertEquals(expected, new CoreRulesArmor().reduceImpactDamage(1, hit, damage, new Vector<Report>(),
              HitDamageType.DAMAGE_PHYSICAL));
        assertEquals(expected, new TWRulesArmor().reduceImpactDamage(1, hit, damage, new Vector<Report>(),
              HitDamageType.DAMAGE_PHYSICAL));
    }

    @ParameterizedTest(name = "falling {0} levels onto a target of MegaMek height {1} multiplies by {2}")
    @CsvSource({ "3, 1, 1", "5, 1, 3", "3, 0, 2", "2, 1, 1" })
    void accidentalFallDamageUsesTheTargetsCoreHeightWithAMinimumOfOne(int levels, int height, int expected) {
        // Core p.116 with Core p.57 heights: a standing Mek is 2 levels tall, a prone one 1
        assertEquals(expected, new CoreRulesMovement().getAccidentalFallElevation(levels, height));
    }

    @ParameterizedTest(name = "{0}/{1} gives {2}")
    @CsvSource({ "AC, 2, -3", "PAC, 4, -2", "AC, 5, -2", "PAC, 8, -1", "AC, 10, -1", "AC, 20, -1", "AC, 15, -1",
                 "AC_IMP, 10, -1" })
    void armorPiercingModifiersFollowCoreForEveryAutocannon(AmmoType.AmmoTypeEnum family, int rackSize,
          int expected) {
        // Core p.191 prints AC/2, AC/5, AC/10, AC/20, LAC and PAC; every autocannon uses that scale by caliber
        AmmoType ammo = mock(AmmoType.class);
        when(ammo.getAmmoType()).thenReturn(family);
        when(ammo.getRackSize()).thenReturn(rackSize);
        assertEquals(expected, new CoreRulesAmmo().armorPiercingMod(ammo));
    }

    @Test
    void ballisticReinforcedArmorDoesNotReduceArmorPiercingAmmoOrAcidMissilesUnderCore() {
        CoreRulesArmor core = new CoreRulesArmor();
        assertTrue(core.ballisticReinforcedArmorReduces(HitDamageType.DAMAGE_BALLISTIC), "Core p.201: DB attacks");
        assertTrue(core.ballisticReinforcedArmorReduces(HitDamageType.DAMAGE_MISSILE), "Core p.201: M attacks");
        assertFalse(core.ballisticReinforcedArmorReduces(HitDamageType.DAMAGE_ARMOR_PIERCING), "Core p.191");
        assertFalse(core.ballisticReinforcedArmorReduces(HitDamageType.DAMAGE_AX), "Core p.192");
        assertTrue(new TWRulesArmor().ballisticReinforcedArmorReduces(HitDamageType.DAMAGE_ARMOR_PIERCING));
        // Nor do ablation and hardened armor keep their critical-hit protections against it (Core p.191)
        assertFalse(core.criticalProtectionsApplyAgainst(HitDamageType.DAMAGE_ARMOR_PIERCING));
        assertTrue(core.criticalProtectionsApplyAgainst(HitDamageType.DAMAGE_BALLISTIC));
        assertTrue(new TWRulesArmor().criticalProtectionsApplyAgainst(HitDamageType.DAMAGE_ARMOR_PIERCING));
        // Ablation armor: -2 to those critical checks under TW (IO:AE p.80); Core ignores the TAC instead (Core p.201)
        assertEquals(-2, new TWRulesArmor().ablationArmorPiercingModifier());
        assertEquals(0, core.ablationArmorPiercingModifier());
    }

    @Test
    void theSuperchargerHasNoActivationModifierUnderCore() {
        SupportTank supportVehicle = mock(SupportTank.class);
        // Core pp.180, 204: an unmodified Escalating Failure roll; TO:AUE p.157 gives support vehicles -1
        assertEquals(0, new CoreRulesEquipment().getSuperchargerActivationModifier(supportVehicle));
        assertEquals(-1, new TWRulesEquipment().getSuperchargerActivationModifier(supportVehicle));
    }

    @Test
    void theTagSurchargeIsFiftyPerHomingLauncherPerTag() {
        Game game = mock(Game.class);
        Player player = mock(Player.class);
        Entity spotter = mock(Entity.class);
        Entity artillery = mock(Entity.class);
        when(spotter.getGame()).thenReturn(game);
        when(spotter.getOwner()).thenReturn(player);
        when(artillery.getOwner()).thenReturn(player);
        when(game.getEntitiesVector()).thenReturn(List.of(spotter, artillery));

        WeaponType arrowIV = mock(WeaponType.class);
        when(arrowIV.hasFlag(WeaponTypeFlag.F_ARROW_IV)).thenReturn(true);
        WeaponMounted arrowIVLauncher = mock(WeaponMounted.class);
        when(arrowIVLauncher.getType()).thenReturn(arrowIV);
        WeaponType longTom = mock(WeaponType.class);
        when(longTom.hasFlag(WeaponTypeFlag.F_ARTILLERY)).thenReturn(true);
        WeaponMounted longTomLauncher = mock(WeaponMounted.class);
        when(longTomLauncher.getType()).thenReturn(longTom);
        when(artillery.getWeaponList()).thenReturn(List.of(arrowIVLauncher, longTomLauncher));
        AmmoType homing = mock(AmmoType.class);
        when(homing.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_HOMING));
        AmmoMounted homingBin = mock(AmmoMounted.class);
        when(homingBin.getType()).thenReturn(homing);
        when(homingBin.getUsableShotsLeft()).thenReturn(5);
        when(artillery.getAmmo()).thenReturn(List.of(homingBin));

        WeaponType tagType = mock(WeaponType.class);
        when(tagType.hasFlag(WeaponTypeFlag.F_TAG)).thenReturn(true);
        WeaponMounted tag = mock(WeaponMounted.class);
        when(tag.getType()).thenReturn(tagType);
        when(spotter.getWeaponList()).thenReturn(List.of(tag));

        // Core errata v0.2 p.1, applied to all Homing: 50 BV per launcher on a unit with Homing ammo, once per TAG;
        // half for a rear-mounted TAG only
        CoreRulesGame core = new CoreRulesGame();
        assertEquals(1100, core.tagBVBump(spotter, mock(CalculationReport.class), 1000, 1, false));
        assertEquals(1200, core.tagBVBump(spotter, mock(CalculationReport.class), 1000, 2, false));
        assertEquals(1050, core.tagBVBump(spotter, mock(CalculationReport.class), 1000, 1, true));
    }

    @Test
    void eachLauncherInABayPaysTheTagSurcharge() {
        Game game = mock(Game.class);
        Player player = mock(Player.class);
        Entity spotter = mock(Entity.class);
        Entity dropShip = mock(Entity.class);
        when(spotter.getGame()).thenReturn(game);
        when(spotter.getOwner()).thenReturn(player);
        when(dropShip.getOwner()).thenReturn(player);
        when(game.getEntitiesVector()).thenReturn(List.of(spotter, dropShip));

        WeaponType arrowIV = mock(WeaponType.class);
        when(arrowIV.hasFlag(WeaponTypeFlag.F_ARROW_IV)).thenReturn(true);
        WeaponMounted launcher = mock(WeaponMounted.class);
        when(launcher.getType()).thenReturn(arrowIV);
        WeaponMounted bay = mock(WeaponMounted.class);
        when(bay.getType()).thenReturn(mock(WeaponType.class));
        when(bay.getBayWeapons()).thenReturn(List.of(launcher, launcher));
        when(dropShip.getWeaponList()).thenReturn(List.of(bay));
        AmmoType homing = mock(AmmoType.class);
        when(homing.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_HOMING));
        AmmoMounted homingBin = mock(AmmoMounted.class);
        when(homingBin.getType()).thenReturn(homing);
        when(homingBin.getUsableShotsLeft()).thenReturn(5);
        when(dropShip.getAmmo()).thenReturn(List.of(homingBin));

        // Core errata v0.2 p.1 charges by the number of launchers, so a bay of two pays twice
        assertEquals(100, new CoreRulesGame().tagBVBump(spotter, mock(CalculationReport.class), 0, 1, false));
    }

    @Test
    void transferredDamageDropsTheArmorPiercingCheck() {
        HitData hit = new HitData(Mek.LOC_LEFT_ARM);
        hit.setSpecCriticalModifier(-2);
        // "Damage that transfers to a location with armor does not trigger a new critical damage check" (TW p.140;
        // Core p.191, errata v0.2)
        assertFalse(new BipedMek().getTransferLocation(hit).getSpecCrit());
    }

    @Test
    void jumpingWithDamageInTwoLegsTakesTwoLandingRollsAndAQuadIgnoresItsFirstHip() {
        // Core p.110: an upper leg actuator lost in one leg and a lower leg actuator in the other make two PSRs
        BipedMek biped = new BipedMek();
        biped.getCritical(Mek.LOC_LEFT_LEG, 1).setDestroyed(true);
        assertEquals(1, CoreRulesUnits.legsForcingLandingChecks(biped));
        biped.getCritical(Mek.LOC_RIGHT_LEG, 2).setDestroyed(true);
        assertEquals(2, CoreRulesUnits.legsForcingLandingChecks(biped));
        // Core p.238: a quad's first hip hit has no standard effects; the second counts as a biped's hip hit
        QuadMek quad = new QuadMek();
        quad.getCritical(Mek.LOC_LEFT_LEG, 0).setDestroyed(true);
        assertEquals(0, CoreRulesUnits.legsForcingLandingChecks(quad));
        quad.getCritical(Mek.LOC_RIGHT_LEG, 0).setDestroyed(true);
        assertEquals(1, CoreRulesUnits.legsForcingLandingChecks(quad));
    }

    @Test
    void theNovaHasThreeHexEcmAndTheWatchdogTheClanSix() {
        MiscType nova = mock(MiscType.class);
        when(nova.hasFlag(MiscType.F_NOVA)).thenReturn(true);
        MiscType watchdog = mock(MiscType.class);
        when(watchdog.hasFlag(MiscType.F_WATCHDOG)).thenReturn(true);
        // IO:AE pp.60, 212 for the Nova; Core p.201 and TO:AUE p.90 for the Watchdog
        assertEquals(3, new CoreRulesEquipment().getECMRanges(nova));
        assertEquals(6, new CoreRulesEquipment().getECMRanges(watchdog));
        assertEquals(6, new TWRulesEquipment().getECMRanges(watchdog));
    }

    @Test
    void assetPriceUsesTheActiveRulesAndTheSelectedCrewSkill() {
        RulesManager previous = Game.rulesManager;
        try {
            Game.rulesManager = mock(RulesManager.class);
            RulesGame gameRules = mock(RulesGame.class);
            when(Game.rulesManager.getRulesGame()).thenReturn(gameRules);
            BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
            asset.setSkill(6);
            asset.setVeteranSkill(4);
            when(gameRules.assetTagSurchargeBsp(asset, 6)).thenReturn(3);
            when(gameRules.assetTagSurchargeBsp(asset, 4)).thenReturn(7);
            assertEquals(3, asset.tagSurchargeBsp(false));
            assertEquals(7, asset.tagSurchargeBsp(true));
        } finally {
            Game.rulesManager = previous;
        }
    }
}
