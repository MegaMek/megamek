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

import static megamek.common.units.UnitRole.BRAWLER;
import static megamek.common.units.UnitRole.JUGGERNAUT;
import static megamek.common.units.UnitRole.SKIRMISHER;
import static megamek.common.units.UnitRole.SNIPER;
import static megamek.common.units.UnitRole.STRIKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

/**
 * Regression tests asserting that selected FormationType definitions match the rules in Campaign Operations (2024 5th
 * Print, pp. 60-68).
 */
class FormationTypeTest {

    @Test
    void anvilLance_armorThresholdIs105_perCamOps() {
        FormationType anvil = FormationType.getFormationType("Anvil");
        assertNotNull(anvil, "Anvil formation should be registered");

        MekSummary armor40 = mock(MekSummary.class);
        when(armor40.getTotalArmor()).thenReturn(40);
        MekSummary armor104 = mock(MekSummary.class);
        when(armor104.getTotalArmor()).thenReturn(104);
        MekSummary armor105 = mock(MekSummary.class);
        when(armor105.getTotalArmor()).thenReturn(105);
        MekSummary armor200 = mock(MekSummary.class);
        when(armor200.getTotalArmor()).thenReturn(200);

        assertFalse(anvil.getMainCriteria().test(armor40),
              "Armor 40 must not satisfy Anvil mainCriteria (CamOps requires 105)");
        assertFalse(anvil.getMainCriteria().test(armor104),
              "Armor 104 must not satisfy Anvil mainCriteria");
        assertTrue(anvil.getMainCriteria().test(armor105),
              "Armor 105 must satisfy Anvil mainCriteria");
        assertTrue(anvil.getMainCriteria().test(armor200),
              "Armor 200 must satisfy Anvil mainCriteria");
        assertEquals("Armor 105+", anvil.getMainDescription());
    }

    @Test
    void pursuitLance_idealRoleIsStriker_perCamOps() {
        FormationType pursuit = FormationType.getFormationType("Pursuit");
        assertNotNull(pursuit, "Pursuit formation should be registered");
        assertEquals(STRIKER, pursuit.getIdealRole(),
              "Pursuit Lance ideal role per CamOps is Striker");
    }

    @Test
    void rangerLance_idealRoleIsSkirmisher_perCamOps() {
        FormationType ranger = FormationType.getFormationType("Ranger");
        assertNotNull(ranger, "Ranger formation should be registered");
        assertEquals(SKIRMISHER, ranger.getIdealRole(),
              "Ranger Lance ideal role per CamOps is Skirmisher");
    }

    @Test
    void fastAssaultLance_juggernautOrSnipers_arePairedConstraints() {
        FormationType fastAssault = FormationType.getFormationType("Fast Assault");
        assertNotNull(fastAssault, "Fast Assault formation should be registered");

        Iterator<FormationType.Constraint> iterator = fastAssault.getOtherCriteria();
        FormationType.Constraint juggernaut = null;
        FormationType.Constraint sniper = null;
        while (iterator.hasNext()) {
            FormationType.Constraint constraint = iterator.next();
            if ("Juggernaut".equals(constraint.getDescription())) {
                juggernaut = constraint;
            } else if ("Sniper".equals(constraint.getDescription())) {
                sniper = constraint;
            }
        }
        assertNotNull(juggernaut, "Fast Assault must declare a Juggernaut constraint");
        assertNotNull(sniper, "Fast Assault must declare a Sniper constraint");
        assertTrue(juggernaut.isPairedWithNext(),
              "Juggernaut constraint must pair with next (CamOps: 1 Juggernaut OR 2 Snipers)");
        assertTrue(sniper.isPairedWithPrevious(),
              "Sniper constraint must pair with previous (CamOps: 1 Juggernaut OR 2 Snipers)");
        assertEquals(1, juggernaut.getMinimum(4),
              "CamOps requires 1 Juggernaut as one alternative");
        assertEquals(2, sniper.getMinimum(4),
              "CamOps requires 2 Snipers as the other alternative");
    }

    @Test
    void vehicleCommandLance_requiresOnlyOnePair_perCamOps() {
        FormationType vehicleCommand = FormationType.getFormationType("Vehicle Command");
        assertNotNull(vehicleCommand, "Vehicle Command formation should be registered");
        FormationType.GroupingConstraint grouping = vehicleCommand.getGroupingCriteria();
        assertNotNull(grouping, "Vehicle Command must declare a grouping constraint");
        assertEquals(2, grouping.getGroupSize(), "Vehicle Command pair size is 2");
        assertEquals(1, grouping.getNumGroups(),
              "CamOps requires only one pair of vehicles with the listed roles");
    }

    @Test
    void qualifies_pairedConstraint_failsWhenNeitherAlternativeIsMet() throws Exception {
        FormationType ft = createFormationWithPairedJuggernautOrSniper();

        List<MekSummary> units = List.of(
              mockTank(BRAWLER), mockTank(BRAWLER),
              mockTank(BRAWLER), mockTank(BRAWLER));
        assertFalse(ft.qualifies(units),
              "qualifies() must reject a formation that has neither 1 Juggernaut nor 2 Snipers");
    }

    @Test
    void qualifies_pairedConstraint_passesWhenJuggernautAlternativeIsMet() throws Exception {
        FormationType ft = createFormationWithPairedJuggernautOrSniper();

        List<MekSummary> units = List.of(
              mockTank(JUGGERNAUT), mockTank(BRAWLER),
              mockTank(BRAWLER), mockTank(BRAWLER));
        assertTrue(ft.qualifies(units),
              "qualifies() must accept a formation when the Juggernaut alternative is met");
    }

    @Test
    void qualifies_pairedConstraint_passesWhenSniperAlternativeIsMet() throws Exception {
        FormationType ft = createFormationWithPairedJuggernautOrSniper();

        List<MekSummary> units = List.of(
              mockTank(SNIPER), mockTank(SNIPER),
              mockTank(BRAWLER), mockTank(BRAWLER));
        assertTrue(ft.qualifies(units),
              "qualifies() must accept a formation when the Sniper alternative is met");
    }

    private static FormationType createFormationWithPairedJuggernautOrSniper() throws Exception {
        FormationType ft = new FormationType("PairedConstraintTest");
        FormationType.Constraint juggernaut = new FormationType.CountConstraint(1,
              ms -> ms.getRole() == JUGGERNAUT, "Juggernaut");
        juggernaut.setPairedWithNext(true);
        FormationType.Constraint sniper = new FormationType.CountConstraint(2,
              ms -> ms.getRole() == SNIPER, "Sniper");
        sniper.setPairedWithPrevious(true);
        Field otherCriteriaField = FormationType.class.getDeclaredField("otherCriteria");
        otherCriteriaField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<FormationType.Constraint> otherCriteria =
              (List<FormationType.Constraint>) otherCriteriaField.get(ft);
        otherCriteria.add(juggernaut);
        otherCriteria.add(sniper);
        return ft;
    }

    private static MekSummary mockTank(UnitRole role) {
        MekSummary ms = mock(MekSummary.class);
        when(ms.getUnitType()).thenReturn("Tank");
        when(ms.getRole()).thenReturn(role);
        when(ms.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_MEDIUM);
        return ms;
    }
}
