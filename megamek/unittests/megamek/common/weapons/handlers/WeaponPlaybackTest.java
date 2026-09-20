/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
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
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.lrm.LRMDeadFireHandler;
import megamek.common.weapons.handlers.lrm.LRMHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class WeaponPlaybackTest {
    @Test
    void machineGunArrayVisualsUseTheRulesMembershipAndActualAmmoCount() throws Exception {
        useUnarmedCraft();
        var array = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISMGA"), 0);
        var wrongLocation = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISMG"), 1);
        var first = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISMG"), 0);
        var second = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISMG"), 0);
        for (var gun : java.util.List.of(wrongLocation, first, second)) { array.addWeaponToBay(gun.getEquipmentNum()); }
        array.setLinked(attacker.addEquipment(EquipmentType.get("IS Ammo MG - Full"), 0));
        array.getLinked().setShotsLeft(1);
        assertEquals(2, array.getCurrentShots());
        var handler = new MGAWeaponHandler(new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "MGA"), action(array), manager.getGame(), manager);
        assertEquals(0, array.getLinked().getBaseShotsLeft());
        assertEquals(java.util.List.of(first.getEquipmentNum()), handler.firingMounts().stream().map(ResolvedAttack.Mount::equipmentIndex).toList());
        handler.reportAttackAnimation(true);
        verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
              eq(array.getEquipmentNum()), eq(array.getLocation()), eq(true), any(), eq(handler.firingMounts()));
    }

    @Test
    void bayPlaybackUsesOnlySpentGunsIncludingTheirLastRound() throws Exception {
        useUnarmedCraft();
        var craft = attacker;
        var bay = (WeaponMounted) craft.addEquipment(EquipmentType.get(megamek.common.equipment.EquipmentTypeLookup.AC_BAY), 0);
        var ready = (WeaponMounted) craft.addEquipment(EquipmentType.get("ISAC2"), 0);
        var empty = (WeaponMounted) craft.addEquipment(EquipmentType.get("ISAC5"), 0);
        var broken = (WeaponMounted) craft.addEquipment(EquipmentType.get("ISAC10"), 0);
        for (var gun : java.util.List.of(ready, empty, broken)) {
            bay.addWeaponToBay(gun.getEquipmentNum());
            gun.setLinked(craft.addEquipment(EquipmentType.get("ISAC" + gun.getType().getRackSize() + " Ammo"), 0));
        }
        ready.getLinked().setShotsLeft(1);
        empty.getLinked().setShotsLeft(0);
        broken.setDestroyed(true);
        var handler = new AmmoBayWeaponHandler(new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "bay"), action(bay), manager.getGame(), manager);
        assertTrue(handler.calcAttackValue() > 0);
        assertEquals(0, ready.getLinked().getBaseShotsLeft());
        handler.reportAttackAnimation(true);
        @SuppressWarnings("unchecked")
        var mounts = ArgumentCaptor.forClass(java.util.List.class);
        verify(manager).sendAttackAnimation(eq(craft), eq(target), eq(ResolvedAttack.Kind.SHOT), eq(bay.getEquipmentNum()),
              eq(bay.getLocation()), eq(true), any(), mounts.capture());
        assertEquals(java.util.List.of(ready.getEquipmentNum()), ((java.util.List<ResolvedAttack.Mount>) mounts.getValue()).stream()
              .map(ResolvedAttack.Mount::equipmentIndex).toList());
    }

    private void useUnarmedCraft() {
        var craft = new megamek.common.units.AeroSpaceFighter();
        craft.setId(3);
        craft.setOwner(attacker.getOwner());
        craft.setPosition(attacker.getPosition());
        craft.setWeight(50);
        manager.getGame().addEntity(craft, false);
        attacker = craft;
    }

    @Test
    void actualAmsExpenditureProducesOneDefensiveEventAndShutdownProducesNone() throws Exception {
        var missile = attacker.getWeaponList().stream().filter(mount -> mount.getType().hasFlag(WeaponType.F_MISSILE))
              .findFirst().orElseThrow();
        var ams = (WeaponMounted) target.addEquipment(EquipmentType.get("ISAMS"), Mek.LOC_LEFT_ARM);
        ams.setLinked(target.addEquipment(EquipmentType.get("IS Ammo AMS"), Mek.LOC_LEFT_ARM));
        target.setFacing(3);
        target.setSecondaryFacing(3);
        var action = action(missile);
        action.addCounterEquipment(ams);
        var handler = new MissileWeaponHandler(new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "review"), action, manager.getGame(), manager);
        target.setShutDown(true);
        assertEquals(0, handler.getAMSHitsMod(new Vector<>()));
        verify(manager, never()).sendAttackAnimation(eq(target), eq(attacker), any(), anyInt(), anyInt(), anyBoolean(), any());
        target.setShutDown(false);
        int ammo = ams.getLinked().getBaseShotsLeft();
        assertEquals(-4, handler.getAMSHitsMod(new Vector<>()));
        assertEquals(ammo - 1, ams.getLinked().getBaseShotsLeft());
        var profile = ArgumentCaptor.forClass(ResolvedAttack.Shot.class);
        verify(manager, times(1)).sendAttackAnimation(eq(target), eq(attacker), eq(ResolvedAttack.Kind.SHOT),
              eq(ams.getEquipmentNum()), eq(ams.getLocation()), eq(true), profile.capture());
        assertTrue(profile.getValue().defensive());
        handler.getAMSHitsMod(new Vector<>());
        verify(manager, times(1)).sendAttackAnimation(eq(target), eq(attacker), any(), anyInt(), anyInt(), anyBoolean(), any());
    }

    private RulesManager rules;
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void artilleryCarriesTheResolvedLandingWithoutRerollingScatter(boolean hit) throws Exception {
        useUnarmedCraft();
        var board = new Board(32, 32, Stream.generate(Hex::new).limit(1024).toArray(Hex[]::new));
        manager.getGame().setBoard(board);
        attacker.setPosition(new Coords(15, 16));
        target.setPosition(new Coords(15, 15));
        var cannon = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISLongTomCannon"), 0);
        cannon.setLinked(attacker.addEquipment(EquipmentType.get("ISLongTomCannon Ammo"), 0));
        var handler = new megamek.common.weapons.handlers.artillery.ArtilleryCannonWeaponHandler(new ToHitData(hit ? 2 : 9, "artillery"),
              action(cannon), manager.getGame(), manager);
        handler.roll = org.mockito.Mockito.mock(megamek.common.rolls.Roll.class);
        org.mockito.Mockito.when(handler.roll.getIntValue()).thenReturn(8);
        var origin = attacker.getPosition();
        attacker.setPosition(new Coords(15, 17));
        handler.handle(GamePhase.FIRING, new Vector<>());
        var profile = ArgumentCaptor.forClass(ResolvedAttack.Shot.class);
        verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
              eq(cannon.getEquipmentNum()), eq(cannon.getLocation()), eq(hit), profile.capture(), any());
        assertEquals(origin, profile.getValue().launch().coords(), "The gun may have moved while its round was in flight");
        assertTrue(profile.getValue().artillery());
        var landing = profile.getValue().impact();
        assertTrue(board.contains(landing.coords()));
        if (hit) { assertEquals(target.getPosition(), landing.coords()); }
        else {
            assertTrue(board.getSpecialHexDisplay(target.getPosition()).stream()
                  .anyMatch(marker -> landing.coords().equals(marker.getDriftHex())));
        }
    }

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
        verify(manager, never()).sendAttackAnimation(any(), any(), any(), anyInt(), anyInt(), anyBoolean(), any());
        handler.handle(GamePhase.FIRING, new Vector<Report>());
        verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
              eq(attacker.getEquipmentNum(weapon)), eq(weapon.getLocation()), eq(hit), any());
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
        verify(manager, never()).sendAttackAnimation(any(), any(), eq(ResolvedAttack.Kind.SHOT), anyInt(), anyInt(), anyBoolean(), any());
    }

    @Test
    void missileClustersProduceOneLaunchEvent() throws Exception {
        var weapon = attacker.getWeaponList().stream().filter(item -> item.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
              .findFirst().orElseThrow();
        var handler = new MissileWeaponHandler(new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "test"), action(weapon), manager.getGame(), manager);
        handler.handle(GamePhase.FIRING, new Vector<>());
        verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
              eq(attacker.getEquipmentNum(weapon)), eq(weapon.getLocation()), anyBoolean(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "lrm", "srm", "atm", "iatm", "dead-fire", "inferno" })
    void clusterPlaybackCarriesMissilesRatherThanDamagePoints(String type) throws Exception {
        var weapon = attacker.getWeaponList().stream().filter(item -> item.getType().getAmmoType()
              == (type.equals("srm") || type.equals("inferno") ? AmmoType.AmmoTypeEnum.SRM : AmmoType.AmmoTypeEnum.LRM))
              .findFirst().orElseThrow();
        if (type.equals("atm") || type.equals("iatm")) {
            String equipment = type.equals("iatm") ? "CLIATM12" : "CLATM12";
            weapon = (WeaponMounted) attacker.addEquipment(EquipmentType.get(equipment), Mek.LOC_RIGHT_ARM);
            weapon.setLinked(attacker.addEquipment(EquipmentType.get(equipment + " Ammo"), Mek.LOC_RIGHT_ARM));
        }
        var toHit = new ToHitData(2, "fixed cluster test");
        WeaponHandler handler = switch (type) {
            case "lrm" -> new LRMHandler(toHit, action(weapon), manager.getGame(), manager);
            case "dead-fire" -> new LRMDeadFireHandler(toHit, action(weapon), manager.getGame(), manager);
            case "atm" -> new ATMHandler(toHit, action(weapon), manager.getGame(), manager);
            case "iatm" -> new CLIATMHandler(toHit, action(weapon), manager.getGame(), manager);
            case "inferno" -> new megamek.common.weapons.handlers.srm.SRMInfernoHandler(toHit, action(weapon), manager.getGame(), manager);
            default -> new MissileWeaponHandler(toHit, action(weapon), manager.getGame(), manager);
        };
        int hits = type.equals("srm") || type.equals("inferno") ? 3 : type.endsWith("atm") ? 7 : 12;
        try (var compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.missilesHit(anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(hits);
            handler.handle(GamePhase.FIRING, new Vector<>());
            var profile = ArgumentCaptor.forClass(ResolvedAttack.Shot.class);
            verify(manager, times(1)).sendAttackAnimation(eq(attacker), eq(target), eq(ResolvedAttack.Kind.SHOT),
                  eq(attacker.getEquipmentNum(weapon)), eq(weapon.getLocation()), eq(true), profile.capture());
            assertEquals(weapon.getType().getRackSize(), profile.getValue().missiles());
            assertEquals(hits, profile.getValue().missileHits());
            assertTrue(profile.getValue().missiles() > hits);
        }
    }

    @Test
    void largeMissileCaliberDoesNotBecomeProjectileCount() throws Exception {
        var weapon = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISThunderbolt20"), Mek.LOC_RIGHT_ARM);
        assertTrue(weapon.getType().hasFlag(WeaponType.F_LARGE_MISSILE));
        assertEquals(1, ResolvedAttack.Shot.capture(weapon).missiles());
    }

    @Test
    void swarmContinuationDoesNotInventAnotherFullRackLaunch() throws Exception {
        var weapon = attacker.getWeaponList().stream().filter(item -> item.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
              .findFirst().orElseThrow();
        var action = action(weapon);
        action.setSwarmingMissiles(true);
        action.setSwarmMissiles(8);
        var handler = new MissileWeaponHandler(new ToHitData(2, "continuation"), action, manager.getGame(), manager);
        handler.reportAttackAnimation(true);
        verify(manager, never()).sendAttackAnimation(any(), any(), any(), anyInt(), anyInt(), anyBoolean(), any());
    }
}
