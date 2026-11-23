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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.TaserEffects;
import megamek.common.weapons.battleArmor.BATaserHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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
        handler.attackingEntity = baTaser ? new BattleArmor() : new BipedMek();
        handler.weapon = mock(WeaponMounted.class);
        when(handler.weapon.getLocation()).thenReturn(1);
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
    @MethodSource("armorHits")
    void armorChangesDamageAndEffectsOnTheFacingHit(TaserEffects taser, int armorType, int roll, int armorLost,
          int shutdown, int interference) {
        RulesManager saved = Game.rulesManager;
        try {
            TWGameManager manager = new TWGameManager();
            Game.rulesManager = new TWRulesManager();
            BipedMek mek = new BipedMek();
            HitData rear = new HitData(Mek.LOC_CENTER_TORSO, true);
            mek.setOwner(new Player(1, "Target"));
            mek.setId(1);
            mek.setGame(manager.getGame());
            mek.setArmorType(armorType, rear.getLocation());
            mek.initializeArmor(10, rear.getLocation());
            mek.initializeRearArmor(1, rear.getLocation());
            mek.initializeInternal(10, rear.getLocation());
            boolean baTaser = taser == TaserEffects.BATTLE_ARMOR;
            MekTaserHandler handler = baTaser ? mock(BATaserHandler.class, CALLS_REAL_METHODS)
                  : mock(MekTaserHandler.class, CALLS_REAL_METHODS);
            handler.hit = rear;
            handler.gameManager = manager;
            handler.attackingEntity = baTaser ? new BattleArmor() : new BipedMek();
            handler.weapon = mock(WeaponMounted.class);
            when(handler.weapon.getLocation()).thenReturn(1);
            handler.weaponType = mock(WeaponType.class);
            handler.damageType = DamageType.NONE;
            when(handler.weaponType.hasFlag(WeaponType.F_BA_WEAPON)).thenReturn(baTaser);
            try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                Roll effect = mock(Roll.class);
                when(effect.getIntValue()).thenReturn(roll);
                dice.when(() -> Compute.rollD6(2)).thenReturn(effect);
                handler.applyEntityDamage(mek, 1, new Vector<>());
            }
            assertEquals(1 - armorLost, mek.getArmor(rear));
            assertEquals(10, mek.getArmor(rear.getLocation()));
            assertEquals(10, mek.getInternal(rear));
            assertEquals(shutdown, mek.getTaserShutdownRounds());
            assertEquals(interference, mek.getTaserInterference());
            assertEquals(baTaser ? 3 : 0, handler.attackingEntity.getTaserFeedBackRounds());
        } finally {
            Game.rulesManager = saved;
        }
    }

    static Stream<Arguments> armorHits() {
        return Stream.of(
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_HARDENED, 12, 1, 0, 0),
              Arguments.of(TaserEffects.BATTLE_ARMOR, EquipmentType.T_ARMOR_HARDENED, 12, 1, 0, 0),
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_FERRO_LAMELLOR, 12, 1, 0, 0),
              Arguments.of(TaserEffects.BATTLE_ARMOR, EquipmentType.T_ARMOR_FERRO_LAMELLOR, 12, 0, 3, 0),
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_REACTIVE, 12, 1, 0, 0),
              Arguments.of(TaserEffects.BATTLE_ARMOR, EquipmentType.T_ARMOR_REACTIVE, 12, 1, 0, 0),
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_REFLECTIVE, 9, 1, 3, 0),
              Arguments.of(TaserEffects.BATTLE_ARMOR, EquipmentType.T_ARMOR_REFLECTIVE, 10, 1, 3, 0),
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_IMPACT_RESISTANT, 10, 1, 3, 0),
              Arguments.of(TaserEffects.MEK, EquipmentType.T_ARMOR_STANDARD, 9, 1, 0, 2));
    }

    @ParameterizedTest
    @MethodSource("battleArmorHits")
    void battleArmorGradeKeepsReactiveProtectionAndHasNoReflectivePenalty(int armorType, int result,
          boolean blocked) {
        BattleArmor squad = new BattleArmor();
        HitData trooper = new HitData(BattleArmor.LOC_TROOPER_1);
        squad.setArmorType(armorType);
        squad.initializeArmor(1, trooper.getLocation());
        squad.initializeInternal(1, trooper.getLocation());
        Vector<Report> reports = new Vector<>();
        try (var dice = mockStatic(Compute.class)) {
            Roll roll = mock(Roll.class);
            when(roll.getIntValue()).thenReturn(result);
            dice.when(() -> Compute.rollD6(2)).thenReturn(roll);
            TaserEffects.BATTLE_ARMOR.resolve(mock(TWGameManager.class), squad, trooper, 1,
                  damage -> squad.setArmor(squad.getArmor(trooper) - damage, trooper), reports);
        }
        assertEquals(0, squad.getArmor(trooper));
        assertEquals(1, squad.getInternal(trooper));
        assertEquals(blocked, reports.stream().anyMatch(report -> report.messageId == 3702));
    }

    static Stream<Arguments> battleArmorHits() {
        return Stream.of(Arguments.of(EquipmentType.T_ARMOR_BA_REACTIVE, 9, true),
              Arguments.of(EquipmentType.T_ARMOR_BA_REFLECTIVE, 7, false));
    }

    @ParameterizedTest
    @CsvSource({"1,true", "2,false"})
    void edpDischargeStacksItsOwnModifierWithRemainingAblationArmor(int armorBefore, boolean shutdown) {
        Tank vehicle = new Tank();
        HitData hit = new HitData(Tank.LOC_FRONT);
        vehicle.setArmorType(EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION, hit.getLocation());
        vehicle.setArmor(armorBefore, hit);
        RulesManager saved = Game.rulesManager;
        try (var dice = mockStatic(Compute.class)) {
            Game.rulesManager = new TWRulesManager();
            Roll roll = mock(Roll.class);
            when(roll.getIntValue()).thenReturn(11);
            dice.when(() -> Compute.rollD6(2)).thenReturn(roll);
            TaserEffects.EDP.resolve(mock(TWGameManager.class), vehicle, hit, 1,
                  damage -> vehicle.setArmor(vehicle.getArmor(hit) - damage, hit), new Vector<>());
            assertEquals(shutdown, vehicle.getTaserShutdownRounds() > 0);
        } finally {
            Game.rulesManager = saved;
        }
    }

    @ParameterizedTest
    @CsvSource({ "1, 1, 2", "2, 2, 2", "0, 2, 2", "0, 1, 1" })
    void failedFeedbackDisablesTheFiringSuit(int mountLocation, int firingTrooper, int squadSize) {
        BattleArmor squad = new BattleArmor();
        squad.setSquadSize(squadSize);
        for (int trooper = 1; trooper <= squadSize; trooper++) {
            squad.initializeInternal(1, trooper);
        }
        ConvInfantry target = new ConvInfantry();
        MekTaserHandler handler = mock(BATaserHandler.class, CALLS_REAL_METHODS);
        handler.attackingEntity = squad;
        handler.target = target;
        handler.gameManager = mock(TWGameManager.class);
        handler.weapon = mock(WeaponMounted.class);
        when(handler.weapon.getLocation()).thenReturn(mountLocation);
        handler.weaponType = mock(WeaponType.class);
        when(handler.weaponType.hasFlag(WeaponType.F_BA_WEAPON)).thenReturn(true);
        handler.hit = new HitData(ConvInfantry.LOC_INFANTRY);
        handler.damageType = DamageType.NONE;
        doAnswer(invocation -> new Vector<Report>()).when(handler.gameManager)
              .damageEntity(any(), any(), anyInt(), anyBoolean(), any(), anyBoolean(), anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean());
        when(handler.gameManager.destroyEntity(squad, "all troopers eliminated", true)).thenReturn(new Vector<>());

        Roll feedback = mock(Roll.class);
        when(feedback.getIntValue()).thenReturn(6);
        try (var dice = mockStatic(Compute.class)) {
            dice.when(() -> Compute.rollD6(2)).thenReturn(feedback);
            dice.when(() -> Compute.randomInt(squadSize)).thenReturn(squadSize - 1);
            assertEquals(1, handler.calcDamagePerHit());
            handler.applyEntityDamage(target, 1, new Vector<>());
        }
        for (int trooper = 1; trooper <= squadSize; trooper++) {
            assertEquals(trooper != firingTrooper, squad.isTrooperActive(trooper));
        }
        if (squadSize == 1) {
            verify(handler.gameManager).destroyEntity(squad, "all troopers eliminated", true);
        }
    }
}
