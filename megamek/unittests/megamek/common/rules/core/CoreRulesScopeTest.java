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

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.rules.totalwarfare.TWRulesArmor;
import megamek.common.rules.totalwarfare.TWRulesEquipment;
import megamek.common.units.Mek;
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
    @CsvSource({ "AC, 2, -3", "PAC, 4, -2", "AC, 5, -2", "PAC, 8, -1", "AC, 10, -1", "AC, 20, -1", "AC, 15, -2",
                 "AC_IMP, 10, -2" })
    void armorPiercingModifiersFollowCoreForThePrintedWeapons(AmmoType.AmmoTypeEnum family, int rackSize,
          int expected) {
        // Core p.191 prints AC/2, AC/5, AC/10, AC/20, LAC and PAC; other weapons and calibers keep TW p.140
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
    }

    @Test
    void damagedLifeSupportOnTheExtendedHeatScaleAddsOneUnavoidablePoint() {
        CoreRulesHeat heat = new CoreRulesHeat();
        // TO:AR p.102 adds 1 to the normal damage; only the highest Heat Scale effect applies (Core p.104)
        assertEquals(3, heat.checkLifeSupportHeat(32, false, true, false).damageAmount());
        assertEquals(3, heat.checkLifeSupportHeat(47, false, true, false).damageAmount());
        assertEquals(2, heat.checkLifeSupportHeat(47, false, false, false).damageAmount());
    }

    @Test
    void theSuperchargerHasNoActivationModifierUnderCore() {
        SupportTank supportVehicle = mock(SupportTank.class);
        // Core pp.180, 204: an unmodified Escalating Failure roll; TO:AUE p.157 gives support vehicles -1
        assertEquals(0, new CoreRulesEquipment().getSuperchargerActivationModifier(supportVehicle));
        assertEquals(-1, new TWRulesEquipment().getSuperchargerActivationModifier(supportVehicle));
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
}
