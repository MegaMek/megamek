/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import megamek.common.ResolvedAttack;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

class WeaponGroupMembersTest {
    private Game game() {
        EquipmentType.initializeTypes();
        var game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER).setValue(true);
        game.addPlayer(0, new Player(0, "Pilot"));
        return game;
    }

    private AeroSpaceFighter fighter(Game game, int id) {
        var fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setOwner(game.getPlayer(0));
        fighter.setWeight(40);
        game.addEntity(fighter, false);
        return fighter;
    }

    @Test
    void mixedSquadronResolvesPhysicalGunsByOwnerAndPreservesTheCapturedMembership() throws Exception {
        var game = game();
        var squadron = new FighterSquadron();
        squadron.setId(10);
        squadron.setOwner(game.getPlayer(0));
        game.addEntity(squadron, false);
        for (int id = 11; id <= 12; id++) {
            var fighter = fighter(game, id);
            for (String name : id == 11 ? List.of("ISMediumLaser", "ISPPC") : List.of("ISPPC", "ISMediumLaser")) {
                fighter.addEquipment(EquipmentType.get(name), Aero.LOC_NOSE);
            }
            fighter.updateWeaponGroups();
            squadron.load(fighter, false, -1);
        }
        var laser = squadron.getWeaponGroupList().stream()
              .filter(mount -> mount.getType() == EquipmentType.get("ISMediumLaser")).findFirst().orElseThrow();
        var captured = ResolvedAttack.captureMounts(squadron, laser.getEquipmentNum());
        assertEquals(List.of("11:0", "12:1"), captured.stream().map(mount -> mount.entityId() + ":" + mount.equipmentIndex()).toList());
        ((Aero) game.getEntity(11)).setFCSHits(3);
        squadron.updateWeaponGroups();
        assertEquals(List.of("12:1"), ResolvedAttack.captureMounts(squadron, laser.getEquipmentNum()).stream()
              .map(mount -> mount.entityId() + ":" + mount.equipmentIndex()).toList());
        assertEquals(2, captured.size(), "Later damage must not change an already queued shot");
        laser.setNWeapons(0);
        assertTrue(ResolvedAttack.captureMounts(squadron, laser.getEquipmentNum()).isEmpty());
    }

    @Test
    void wingAndRearGroupsUseTheSameLocationsAsGroupCreation() throws Exception {
        var fighter = fighter(game(), 1);
        var laser = EquipmentType.get("ISMediumLaser");
        fighter.addEquipment(laser, Aero.LOC_LEFT_WING);
        fighter.addEquipment(laser, Aero.LOC_RIGHT_WING);
        fighter.addEquipment(laser, Aero.LOC_RIGHT_WING, true);
        fighter.updateWeaponGroups();
        for (WeaponMounted group : fighter.getWeaponGroupList()) {
            var expected = group.getLocation() == Aero.LOC_WINGS ? List.of(0, 1) : List.of(2);
            assertEquals(expected, fighter.getWeaponGroupMembers(group).stream().map(WeaponMounted::getEquipmentNum).toList());
            assertEquals(expected.size(), group.getNWeapons());
        }
        fighter.getEquipment(0).setDestroyed(true);
        var wings = fighter.getWeaponGroupList().stream().filter(mount -> mount.getLocation() == Aero.LOC_WINGS).findFirst().orElseThrow();
        assertEquals(List.of(1), fighter.getWeaponGroupMembers(wings).stream().map(WeaponMounted::getEquipmentNum).toList());
    }
}
