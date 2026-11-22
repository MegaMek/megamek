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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.TaserEffects;
import megamek.common.weapons.battleArmor.BATaserHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TaserHandlerTest {
    @ParameterizedTest
    @CsvSource({"false,false,2,11,false", "false,false,1,11,true", "false,true,2,12,false",
          "false,true,1,12,true", "true,false,1,11,false", "true,true,1,12,false", "true,true,0,12,true"})
    void ablationChecksTheStruckFacingAndOnlyModifiesTheTaserEffect(boolean core, boolean baTaser,
          int startingArmor, int roll, boolean shutdown) {
        BipedMek mek = new BipedMek();
        HitData rear = new HitData(Mek.LOC_CENTER_TORSO, true);
        mek.setArmorType(EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION, rear.getLocation());
        mek.initializeArmor(10, rear.getLocation());
        mek.initializeRearArmor(startingArmor, rear.getLocation());
        MekTaserHandler handler = baTaser ? mock(BATaserHandler.class, CALLS_REAL_METHODS)
              : mock(MekTaserHandler.class, CALLS_REAL_METHODS);
        handler.hit = rear;
        handler.gameManager = mock(TWGameManager.class);
        handler.attackingEntity = new BipedMek();
        handler.weaponType = mock(WeaponType.class);
        handler.damageType = DamageType.NONE;
        when(handler.weaponType.hasFlag(WeaponType.F_BA_WEAPON)).thenReturn(baTaser);
        doAnswer(invocation -> {
            mek.setArmor(Math.max(0, startingArmor - 1), rear);
            return new Vector<Report>();
        }).when(handler.gameManager).damageEntity(any(), any(), anyInt(), anyBoolean(), any(),
              anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        RulesManager saved = Game.rulesManager;
        try (var dice = mockStatic(Compute.class)) {
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            Roll effectRoll = mock(Roll.class);
            when(effectRoll.getIntValue()).thenReturn(roll);
            dice.when(() -> Compute.rollD6(2)).thenReturn(effectRoll);
            Vector<Report> reports = new Vector<>();
            handler.applyEntityDamage(mek, 1, reports);
            assertEquals(baTaser ? 3 : 0, handler.attackingEntity.getTaserFeedBackRounds());
            assertEquals(shutdown, mek.getTaserShutdownRounds() > 0);
            assertEquals(core && startingArmor > 0, reports.stream().anyMatch(report -> report.messageId == 3702));
            assertEquals(!core && !shutdown, reports.stream().anyMatch(report -> report.messageId == 3710));
        } finally {
            Game.rulesManager = saved;
        }
    }

    @ParameterizedTest
    @CsvSource({"1,true", "2,false"})
    void edpDischargeStacksItsOwnModifierWithRemainingAblationArmor(int armorBefore, boolean shutdown) {
        Tank vehicle = new Tank();
        HitData hit = new HitData(Tank.LOC_FRONT);
        vehicle.setArmorType(EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION, hit.getLocation());
        vehicle.setArmor(armorBefore - 1, hit);
        RulesManager saved = Game.rulesManager;
        try (var dice = mockStatic(Compute.class)) {
            Game.rulesManager = new TWRulesManager();
            Roll roll = mock(Roll.class);
            when(roll.getIntValue()).thenReturn(11);
            dice.when(() -> Compute.rollD6(2)).thenReturn(roll);
            TaserEffects.EDP.resolve(mock(TWGameManager.class), vehicle, hit, armorBefore, new Vector<>());
            assertEquals(shutdown, vehicle.getTaserShutdownRounds() > 0);
        } finally {
            Game.rulesManager = saved;
        }
    }
}
