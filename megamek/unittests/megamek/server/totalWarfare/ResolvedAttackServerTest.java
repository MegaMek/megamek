/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ResolvedAttack;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.handlers.EnergyWeaponHandler;
import megamek.common.weapons.handlers.FlamerHandler;
import megamek.common.weapons.handlers.MissileWeaponHandler;
import megamek.common.weapons.handlers.NarcHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResolvedAttackServerTest {
    @ParameterizedTest
    @ValueSource(strings = { "energy", "lrm", "srm", "flamer", "narc" })
    void weaponResolutionIncludesTheLocationsActuallyPassedToDamage(String family) throws Exception {
        manager.getGame().setPhase(GamePhase.FIRING);
        var ammo = family.equals("lrm") ? AmmoType.AmmoTypeEnum.LRM : family.equals("srm") ? AmmoType.AmmoTypeEnum.SRM : AmmoType.AmmoTypeEnum.NA;
        var weapon = family.equals("flamer") ? (WeaponMounted) attacker.addEquipment(EquipmentType.get("Flamer"), Mek.LOC_LEFT_ARM)
              : attacker.getWeaponList().stream().filter(mount -> mount.getType().getAmmoType() == ammo).findFirst().orElseThrow();
        if (family.equals("narc")) {
            weapon = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISNarcBeacon"), Mek.LOC_LEFT_ARM);
            weapon.setLinked(attacker.addEquipment(EquipmentType.get("ISNarc Pods"), Mek.LOC_LEFT_ARM));
        }
        var action = new WeaponAttackAction(attacker.getId(), Targetable.TYPE_ENTITY, target.getId(), weapon.getEquipmentNum());
        var toHit = new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "observed location test");
        WeaponHandler handler = family.equals("energy") ? new EnergyWeaponHandler(toHit, action, manager.getGame(), manager)
              : family.equals("flamer") ? new FlamerHandler(toHit, action, manager.getGame(), manager)
              : family.equals("narc") ? new NarcHandler(toHit, action, manager.getGame(), manager)
              : new MissileWeaponHandler(toHit, action, manager.getGame(), manager);
        manager.resolveAttack(handler, GamePhase.FIRING, new java.util.Vector<>());
        var result = deliveries.stream().map(Delivery::result).filter(event -> event.kind() == ResolvedAttack.Kind.SHOT).findFirst().orElseThrow();
        assertFalse(result.impacts().isEmpty(), "Resolved locations must reach the visual event for " + family);
        assertTrue(result.impacts().stream().allMatch(impact -> impact.weight() > 0));
        assertEquals(2, deliveries.stream().filter(delivery -> delivery.result().id().equals(result.id())).count());
    }

    @Test
    void findingATreeClubAddsEquipmentWithoutConsumingWoodlandCover() throws Exception {
        var resolve = TWGameManager.class.getDeclaredMethod("resolveFindClub", Entity.class);
        resolve.setAccessible(true);
        Hex hex = manager.getGame().getHexOf(attacker);
        for (int type : new int[] { Terrains.WOODS, Terrains.JUNGLE }) {
            for (int density = 1; density <= 3; density++) {
                hex.removeAllTerrains();
                hex.addTerrain(new Terrain(type, density));
                hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
                int equipment = attacker.getEquipment().size();
                resolve.invoke(manager, attacker);
                assertEquals(equipment + 1, attacker.getEquipment().size());
                assertEquals(EquipmentTypeLookup.TREE_CLUB, attacker.getEquipment().getLast().getType().getInternalName());
                assertEquals(density, hex.terrainLevel(type), "Taking a club must not reduce cover");
                assertEquals(2, hex.terrainLevel(Terrains.FOLIAGE_ELEV));
            }
        }
    }

    @Test
    void destructionEmitsOnceAtTheRulesTransitionAndRespectsVisibility() {
        target.setHidden(true);
        manager.destroyEntity(target, "review", true, true);
        assertTrue(target.isDoomed());
        assertEquals(List.of(1), deliveries.stream().map(Delivery::player).toList());
        assertEquals(ResolvedAttack.Kind.DEATH, deliveries.getFirst().result().kind());
        manager.destroyEntity(target, "review repeated", true, true);
        assertEquals(1, deliveries.size());
    }

    private record Delivery(int player, ResolvedAttack result) { }
    private final List<Delivery> deliveries = new ArrayList<>();
    private RulesManager rules;
    private TWGameManager manager;
    private Entity attacker, target;

    @BeforeEach
    void setup() throws Exception {
        rules = Game.rulesManager;
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doAnswer(call -> {
            Packet packet = call.getArgument(1);
            if (packet.command() == PacketCommand.ENTITY_ATTACK_RESOLVED) {
                deliveries.add(new Delivery(call.getArgument(0), (ResolvedAttack) packet.getObject(0)));
            }
            return null;
        }).when(manager).send(anyInt(), any(Packet.class));
        var game = manager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(new Board(4, 4, Stream.generate(Hex::new).limit(16).toArray(Hex[]::new)));
        for (int id = 0; id < 2; id++) {
            var player = new Player(id, "Player " + id);
            player.setTeam(id + 1);
            game.addPlayer(id, player);
        }
        attacker = unit(1, 0, new Coords(1, 1));
        target = unit(2, 1, new Coords(1, 2));
    }

    private Entity unit(int id, int player, Coords coords) throws Exception {
        var entity = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
        entity.setId(id);
        entity.setOwner(manager.getGame().getPlayer(player));
        entity.setPosition(coords);
        entity.setDeployed(true);
        manager.getGame().addEntity(entity, false);
        return entity;
    }

    @AfterEach
    void restoreRules() { Game.rulesManager = rules; }

    @Test
    void confirmedResultsCarryStableLoadoutIdentityWithoutChangingDamage() {
        int internal = target.getInternal(Mek.LOC_CENTER_TORSO);
        var weapon = attacker.getWeaponList().getFirst();
        int index = attacker.getEquipmentNum(weapon);
        manager.sendAttackAnimation(attacker, target, ResolvedAttack.Kind.SHOT, index, weapon.getLocation(), true);
        assertEquals(2, deliveries.size());
        var result = deliveries.getFirst().result();
        assertEquals(index, result.equipmentIndex());
        assertEquals(1, result.mounts().size());
        assertEquals(attacker.getId(), result.mounts().getFirst().entityId());
        assertEquals(index, result.mounts().getFirst().equipmentIndex());
        assertEquals(ResolvedAttack.Shot.capture(weapon), result.mounts().getFirst().shot());
        assertEquals(weapon.getType().getInternalName(), result.equipmentName());
        assertEquals(target.getPosition(), result.target().coords());
        assertEquals(internal, target.getInternal(Mek.LOC_CENTER_TORSO));
        assertTrue(result.hit());
    }

    @Test
    void hiddenParticipantsDoNotLeakEvenWithoutDoubleBlind() {
        attacker.setHidden(true);
        manager.sendAttackAnimation(attacker, target, ResolvedAttack.Kind.PUNCH, -1, Mek.LOC_LEFT_ARM, false);
        assertEquals(List.of(0), deliveries.stream().map(Delivery::player).toList());
        assertFalse(deliveries.getFirst().result().hit());
        deliveries.clear();
        target.setHidden(true);
        manager.sendAttackAnimation(attacker, target, ResolvedAttack.Kind.KICK, -1, Mek.LOC_LEFT_LEG, true);
        assertTrue(deliveries.isEmpty(), "Neither player can see both hidden participants");
    }

    @Test
    void crossBoardOrUnavailableParticipantsCannotProducePlayback() {
        target.setBoardId(1);
        manager.sendAttackAnimation(attacker, target, ResolvedAttack.Kind.SHOT, 0, 0, true);
        manager.sendAttackAnimation(null, target, ResolvedAttack.Kind.SHOT, 0, 0, true);
        assertTrue(deliveries.isEmpty());
    }
}
