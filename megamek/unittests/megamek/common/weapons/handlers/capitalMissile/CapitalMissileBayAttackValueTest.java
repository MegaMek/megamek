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
package megamek.common.weapons.handlers.capitalMissile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CapitalMissileBayAttackValueTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static <T extends Entity> T place(Game game, T entity, Player owner, Coords position) {
        entity.setGame(game);
        entity.setId(game.getNextEntityId());
        entity.setChassis("Test");
        entity.setModel(owner.getName());
        entity.setOwner(owner);
        entity.setPosition(position);
        entity.setDeployed(true);
        game.addEntity(entity);
        return entity;
    }

    /** A bay's Damage Value rounds its total normally (SO p.88), so one Stingray's 3.5 deals 4, not 3 */
    @Test
    void aStingrayBayRoundsItsHalfPointUp() throws Exception {
        TWGameManager gameManager = new TWGameManager();
        Game game = gameManager.getGame();
        game.getOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).setValue("Experimental");
        game.setBoard(Board.getSpaceBoard(16, 17));
        Player attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        Dropship dropShip = new Dropship();
        dropShip.setCrew(new Crew(CrewType.VESSEL));
        dropShip.setWeight(3000);
        place(game, dropShip, attackerOwner, new Coords(3, 3));
        AeroSpaceFighter target = new AeroSpaceFighter();
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setWeight(50);
        place(game, target, targetOwner, new Coords(3, 4));

        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(
              EquipmentType.get(EquipmentTypeLookup.SC_MISSILE_BAY), Aero.LOC_NOSE);
        WeaponMounted stingray = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("Stingray"), Aero.LOC_NOSE);
        AmmoMounted missiles = (AmmoMounted) dropShip.addEquipment(EquipmentType.get("Ammo Stingray"), Aero.LOC_NOSE);
        bay.addWeaponToBay(stingray);
        bay.addAmmoToBay(missiles);
        stingray.setLinked(missiles);

        WeaponAttackAction attack = new WeaponAttackAction(dropShip.getId(), target.getId(),
              dropShip.getEquipmentNum(bay));
        CapitalMissileBayHandler handler = new CapitalMissileBayHandler(new ToHitData(2, "test"), attack, game,
              gameManager);

        assertEquals(4, handler.calcAttackValue());
    }
}
