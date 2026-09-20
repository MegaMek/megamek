/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ResolvedAttack;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import megamek.server.totalWarfare.TWGameManager;

class WeaponPlaybackTest {
    private RulesManager rules;
    private TWGameManager manager;
    private Entity attacker, target;

    @BeforeEach
    void setup() throws Exception {
        rules = Game.rulesManager;
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doNothing().when(manager).send(anyInt(), any(Packet.class));
        var game = manager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(new Board(4, 4, Stream.generate(Hex::new).limit(16).toArray(Hex[]::new)));
        game.setPhase(GamePhase.FIRING);
        game.addPlayer(0, new Player(0, "Playback"));
        attacker = unit(1, new Coords(1, 1));
        target = unit(2, new Coords(1, 0));
    }

    private Entity unit(int id, Coords coords) throws Exception {
        var entity = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
        entity.setId(id);
        entity.setOwner(manager.getGame().getPlayer(0));
        entity.setPosition(coords);
        entity.setDeployed(true);
        manager.getGame().addEntity(entity, false);
        return entity;
    }

    private WeaponAttackAction action(WeaponMounted weapon) {
        return new WeaponAttackAction(attacker.getId(), Targetable.TYPE_ENTITY, target.getId(), attacker.getEquipmentNum(weapon));
    }

    @AfterEach
    void restoreRules() { Game.rulesManager = rules; }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void aRealEnergyResolutionPublishesExactlyOneHitOrMiss(boolean hit) throws Exception {
        var weapon = attacker.getWeaponList().stream().filter(item -> item.getType().getAmmoType() == AmmoType.AmmoTypeEnum.NA)
              .findFirst().orElseThrow();
        var handler = new EnergyWeaponHandler(new ToHitData(hit ? TargetRoll.AUTOMATIC_SUCCESS : TargetRoll.AUTOMATIC_FAIL, "test"),
              action(weapon), manager.getGame(), manager);
        handler.handle(GamePhase.TARGETING, new Vector<>());
        verify(manager, never()).sendAttackAnimation(any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        handler.handle(GamePhase.FIRING, new Vector<Report>());
        verify(manager, times(1)).sendAttackAnimation(attacker, target, ResolvedAttack.Kind.SHOT,
              attacker.getEquipmentNum(weapon), weapon.getLocation(), hit);
    }

    @Test
    void failedStreakLockDoesNotLaunchOrSpendAmmo() throws Exception {
        var weapon = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISStreakSRM2"), Mek.LOC_RIGHT_ARM);
        var ammo = attacker.addEquipment(EquipmentType.get("ISStreakSRM2 Ammo"), Mek.LOC_RIGHT_ARM);
        weapon.setLinked(ammo);
        int shots = ammo.getBaseShotsLeft();
        var handler = new StreakHandler(new ToHitData(TargetRoll.AUTOMATIC_FAIL, "no lock"), action(weapon), manager.getGame(), manager);
        handler.handle(GamePhase.FIRING, new Vector<>());
        assertEquals(shots, ammo.getBaseShotsLeft());
        verify(manager, never()).sendAttackAnimation(any(), any(), eq(ResolvedAttack.Kind.SHOT), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void missileClustersProduceOneLaunchEvent() throws Exception {
        var weapon = attacker.getWeaponList().stream().filter(item -> item.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
              .findFirst().orElseThrow();
        var handler = new MissileWeaponHandler(new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "test"), action(weapon), manager.getGame(), manager);
        handler.handle(GamePhase.FIRING, new Vector<>());
        verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
              eq(attacker.getEquipmentNum(weapon)), eq(weapon.getLocation()), anyBoolean());
    }
}
