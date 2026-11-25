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
package megamek.common.battleValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.Player;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.tag.ISTAG;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TagSurchargeTest {
    @ParameterizedTest
    @CsvSource({ "true, 125", "false, 150" })
    void mekTagSurchargeUsesTheMountFacing(boolean rearMounted, double expected) {
        RulesManager previous = Game.rulesManager;
        try {
            Game.rulesManager = new CoreRulesManager();
            Game game = mock(Game.class);
            Mek spotter = mock(Mek.class);
            Entity artillery = mock(Entity.class);
            Player owner = new Player(1, "owner");
            when(spotter.getGame()).thenReturn(game);
            when(spotter.getOwner()).thenReturn(owner);
            when(artillery.getOwner()).thenReturn(owner);
            when(game.getEntitiesVector()).thenReturn(List.of(spotter, artillery));
            WeaponType arrow = mock(WeaponType.class);
            when(arrow.hasFlag(WeaponType.F_ARROW_IV)).thenReturn(true);
            WeaponMounted launcher = mock(WeaponMounted.class);
            when(launcher.getType()).thenReturn(arrow);
            when(artillery.getWeaponList()).thenReturn(List.of(launcher));
            AmmoType homing = mock(AmmoType.class);
            when(homing.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_HOMING));
            AmmoMounted bin = mock(AmmoMounted.class);
            when(bin.getType()).thenReturn(homing);
            when(bin.getUsableShotsLeft()).thenReturn(1);
            when(artillery.getAmmo()).thenReturn(List.of(bin));
            WeaponMounted tag = mock(WeaponMounted.class);
            when(tag.getType()).thenReturn(new ISTAG());
            when(tag.getLocation()).thenReturn(Mek.LOC_LEFT_TORSO);
            when(tag.isRearMounted()).thenReturn(rearMounted);
            when(spotter.getWeaponList()).thenReturn(List.of(tag));
            BVCalculator calc = BVCalculator.getBVCalculator(spotter);
            calc.bvReport = new DummyCalculationReport();
            calc.adjustedBV = 100;
            calc.processTagBonus();
            assertEquals(expected, calc.adjustedBV, 0.001);
        } finally {
            Game.rulesManager = previous;
        }
    }
}
