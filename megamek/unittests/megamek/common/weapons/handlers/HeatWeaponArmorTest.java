/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.plasma.PlasmaCannonHandler;
import megamek.common.weapons.handlers.plasma.PlasmaMFUKWeaponHandler;
import megamek.common.weapons.handlers.plasma.PlasmaRifleHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeatWeaponArmorTest {
    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @CsvSource({
          "MFUK Plasma Rifle, true, 5", "MFUK Plasma Rifle, false, 2",
          "CLPlasmaCannon, true, 5", "CLPlasmaCannon, false, 2",
          "ISPlasmaRifle, true, 5", "ISPlasmaRifle, false, 2"
    })
    void reflectiveArmorAppliesTheRulesetToPlasmaHeat(String weaponName, boolean core, int expectedHeat)
          throws Exception {
        RulesManager originalRules = Game.rulesManager;
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            TWGameManager manager = new TWGameManager();
            Game game = manager.getGame();
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            game.setBoard(Board.createEmptyBoard(2, 1));
            Mek attacker = deployMek(game, 0);
            Mek target = deployMek(game, 1);
            target.setArmorType(EquipmentType.T_ARMOR_REFLECTIVE);
            WeaponMounted mount = (WeaponMounted) attacker.addEquipment(EquipmentType.get(weaponName),
                  Mek.LOC_LEFT_ARM);
            WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
                  mount.getEquipmentNum());
            ToHitData toHit = new ToHitData();
            WeaponHandler handler = switch (weaponName) {
                case "ISPlasmaRifle" -> {
                    mount.setLinked(attacker.addEquipment(EquipmentType.get("ISPlasmaRifleAmmo"), Mek.LOC_LEFT_TORSO));
                    yield new PlasmaRifleHandler(toHit, attack, game, manager);
                }
                case "CLPlasmaCannon" -> {
                    mount.setLinked(attacker.addEquipment(EquipmentType.get("CLPlasmaCannonAmmo"), Mek.LOC_LEFT_TORSO));
                    yield new PlasmaCannonHandler(toHit, attack, game, manager);
                }
                default -> new PlasmaMFUKWeaponHandler(toHit, attack, game, manager);
            };
            dice.when(Compute::d6).thenReturn(5);
            dice.when(() -> Compute.d6(2)).thenReturn(5);
            // Even a hit that strips the last pip strikes reflective armor.
            handler.nDamPerHit = 2;
            Vector<Report> reports = new Vector<>();
            handler.handleEntityDamage(target, reports, null, 1, 1, 0);

            if (weaponName.equals("ISPlasmaRifle")) {
                assertEquals(0, target.getArmor(handler.hit));
            }
            assertEquals(expectedHeat, target.heatFromExternal);
            assertEquals(expectedHeat == 5 ? 3400 : 3406, reports.lastElement().messageId);
        } finally {
            Game.rulesManager = originalRules;
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, true, 2", "true, true, 0", "false, false, 5", "true, false, 5" })
    void plasmaHeatMeetsTheArmorOfTheLocationThatTakesTheHit(boolean core, boolean armMissing, int expectedHeat)
          throws Exception {
        RulesManager originalRules = Game.rulesManager;
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            TWGameManager manager = new TWGameManager();
            Game game = manager.getGame();
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            game.setBoard(Board.createEmptyBoard(2, 1));
            Mek attacker = deployMek(game, 0);
            Mek target = deployMek(game, 1, spy(new BipedMek()));
            target.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
            // A missing arm passes the hit to the left torso, which keeps armor after the hit; an intact arm without
            // armor takes it unreduced (Core p.201, IO:AE p.81)
            target.initializeArmor(5, Mek.LOC_LEFT_TORSO);
            target.initializeArmor(armMissing ? IArmorState.ARMOR_DESTROYED : 0, Mek.LOC_LEFT_ARM);
            if (armMissing) {
                target.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_ARM);
            }
            doReturn(new HitData(Mek.LOC_LEFT_ARM)).when(target)
                  .rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
            WeaponMounted rifle = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISPlasmaRifle"),
                  Mek.LOC_LEFT_ARM);
            rifle.setLinked(attacker.addEquipment(EquipmentType.get("ISPlasmaRifleAmmo"), Mek.LOC_LEFT_TORSO));
            WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
                  rifle.getEquipmentNum());
            WeaponHandler handler = (WeaponHandler) ((Weapon) rifle.getType()).getCorrectHandler(new ToHitData(),
                  attack, game, manager);
            dice.when(Compute::d6).thenReturn(5);
            handler.nDamPerHit = 2;

            handler.handleEntityDamage(target, new Vector<>(), null, 1, 1, 0);

            assertEquals(expectedHeat, target.heatFromExternal);
        } finally {
            Game.rulesManager = originalRules;
        }
    }

    /**
     * Special armor acts while the location has armor at the time a hit is applied (Core p.201), so a plasma rifle or
     * flamer hit whose own damage strips the last heat-dissipating armor still has its heat halved (TW, IO:AE p.81) or
     * negated (Core p.202).
     */
    @ParameterizedTest
    @CsvSource({ "ISPlasmaRifle, false, 2", "ISPlasmaRifle, true, 0", "ISFlamer, false, 1", "ISFlamer, true, 0" })
    void heatMeetsTheArmorBeforeTheHitsOwnDamage(String weaponName, boolean core, int expectedHeat) throws Exception {
        RulesManager originalRules = Game.rulesManager;
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            TWGameManager manager = new TWGameManager();
            Game game = manager.getGame();
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            // Under TW a flamer deals heat along with damage only with this option (Core always)
            game.getOptions().getOption(OptionsConstants.BASE_FLAMER_HEAT).setValue(true);
            game.setBoard(Board.createEmptyBoard(2, 1));
            Mek attacker = deployMek(game, 0);
            Mek target = deployMek(game, 1, spy(new BipedMek()));
            target.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
            doReturn(new HitData(Mek.LOC_LEFT_TORSO)).when(target)
                  .rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
            WeaponMounted weapon = (WeaponMounted) attacker.addEquipment(EquipmentType.get(weaponName),
                  Mek.LOC_LEFT_ARM);
            if (weaponName.equals("ISPlasmaRifle")) {
                weapon.setLinked(attacker.addEquipment(EquipmentType.get("ISPlasmaRifleAmmo"), Mek.LOC_LEFT_TORSO));
            }
            WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
                  weapon.getEquipmentNum());
            WeaponHandler handler = (WeaponHandler) ((Weapon) weapon.getType()).getCorrectHandler(new ToHitData(),
                  attack, game, manager);
            dice.when(Compute::d6).thenReturn(5);
            handler.nDamPerHit = 2;
            handler.handleEntityDamage(target, new Vector<>(), null, 1, 1, 0);

            // The hit's damage stripped the single armor point it met
            assertTrue(target.getArmor(Mek.LOC_LEFT_TORSO) <= 0);
            assertEquals(expectedHeat, target.heatFromExternal);
        } finally {
            Game.rulesManager = originalRules;
        }
    }

    /** Heat isn't damage: a flamer whose damage the building around its target absorbs still heats the Mek (TW p.171) */
    @Test
    void aFlamerHeatsAMekWhoseBuildingAbsorbsTheDamage() throws Exception {
        RulesManager originalRules = Game.rulesManager;
        try {
            TWGameManager manager = spy(new TWGameManager());
            doReturn(new Vector<Report>()).when(manager).damageBuilding(any(), anyInt(), any());
            Game game = manager.getGame();
            // Under Core a flamer always deals heat along with its damage
            Game.rulesManager = new CoreRulesManager();
            game.setBoard(Board.createEmptyBoard(2, 1));
            Mek attacker = deployMek(game, 0);
            Mek target = deployMek(game, 1);
            WeaponMounted flamer = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISFlamer"),
                  Mek.LOC_LEFT_ARM);
            WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
                  flamer.getEquipmentNum());
            WeaponHandler handler = (WeaponHandler) ((Weapon) flamer.getType()).getCorrectHandler(new ToHitData(),
                  attack, game, manager);
            handler.nDamPerHit = 2;

            handler.handleEntityDamage(target, new Vector<>(), mock(IBuilding.class), 1, 1, 10);

            assertEquals(2, target.heatFromExternal);
        } finally {
            Game.rulesManager = originalRules;
        }
    }

    private static Mek deployMek(Game game, int x) {
        return deployMek(game, x, new BipedMek());
    }

    private static Mek deployMek(Game game, int x, Mek mek) {
        Player player = new Player(x, "Player " + x);
        game.addPlayer(x, player);
        mek.setOwner(player);
        mek.setPosition(new Coords(x, 0));
        mek.setWeight(50);
        mek.autoSetInternal();
        for (int loc = 0; loc < mek.locations(); loc++) {
            mek.initializeArmor(1, loc);
            if (mek.hasRearArmor(loc)) {
                mek.initializeRearArmor(1, loc);
            }
        }
        game.addEntity(mek);
        return mek;
    }
}
