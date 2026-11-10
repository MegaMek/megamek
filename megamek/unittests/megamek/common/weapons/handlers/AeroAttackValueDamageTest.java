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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Attacks on an airborne aerospace unit with non-cluster weapons apply the Attack Value as one block of damage. A
 * weapon bay's own damage is the {@code DAMAGE_VARIABLE} marker (-3), so a bay that dealt its damage per hit in place
 * of its Attack Value healed the target (issue #9457). A charged PPC Capacitor must still add its 5 points (issue
 * #8362).
 */
class AeroAttackValueDamageTest extends GameBoardTestCase {

    /** Short range Attack Value of one Inner Sphere PPC. */
    private static final int PPC_ATTACK_VALUE = 10;
    private static final int PPC_CAPACITOR_BONUS = 5;
    private static final int STARTING_ARMOR = 40;

    static {
        initializeBoard("AERO_ATTACK_VALUE_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Player attackerOwner;
    private AeroSpaceFighter target;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setBoard(getBoard("AERO_ATTACK_VALUE_BOARD"));
        attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        target = new AeroSpaceFighter();
        target.setGame(game);
        target.setId(game.getNextEntityId());
        target.setChassis("Test Fighter");
        target.setModel("Target");
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setOwner(targetOwner);
        target.setWeight(50);
        for (int location = 0; location < target.locations(); location++) {
            target.initializeArmor(STARTING_ARMOR, location);
        }
        placeAirborne(target, new Coords(4, 4));
    }

    private void placeAirborne(Entity entity, Coords position) {
        game.addEntity(entity);
        entity.setPosition(position);
        entity.setFacing(0);
        entity.setAltitude(3);
        entity.setDeployed(true);
    }

    private Dropship addDropShip() {
        Dropship dropShip = new Dropship();
        dropShip.setGame(game);
        dropShip.setId(game.getNextEntityId());
        dropShip.setChassis("Test DropShip");
        dropShip.setModel("Attacker");
        dropShip.setCrew(new Crew(CrewType.VESSEL));
        dropShip.setOwner(attackerOwner);
        dropShip.setWeight(3000);
        placeAirborne(dropShip, new Coords(4, 0));
        return dropShip;
    }

    private AeroSpaceFighter addFighter() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Test Fighter");
        fighter.setModel("Attacker");
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(attackerOwner);
        fighter.setWeight(50);
        placeAirborne(fighter, new Coords(4, 0));
        return fighter;
    }

    /**
     * Fires the weapon with the handler the game itself would pick for it. A to-hit target of 2 always hits.
     */
    private Vector<Report> fire(Entity attacker, Mounted<?> weapon) {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(weapon));
        AttackHandler handler = ((Weapon) weapon.getType())
              .getCorrectHandler(new ToHitData(2, "test"), attack, game, gameManager);
        Vector<Report> reports = new Vector<>();
        handler.handle(GamePhase.FIRING, reports);
        return reports;
    }

    private int armorLost() {
        int remaining = 0;
        for (int location = 0; location < target.locations(); location++) {
            remaining += Math.max(0, target.getArmor(location));
        }
        return (STARTING_ARMOR * target.locations()) - remaining;
    }

    @Test
    void dropShipPpcBayDealsItsAttackValue() throws Exception {
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        for (int count = 0; count < 2; count++) {
            WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
            bay.addWeaponToBay(ppc);
        }
        assertTrue(dropShip.usesWeaponBays(), "an airborne DropShip fires by bay");
        assertTrue(target.isAirborne(), "the target is airborne");

        fire(dropShip, bay);

        assertEquals(2 * PPC_ATTACK_VALUE, armorLost(), "the bay deals the Attack Value of both PPCs");
    }

    @Test
    void fighterPpcDealsItsAttackValue() throws Exception {
        AeroSpaceFighter fighter = addFighter();
        Mounted<?> ppc = fighter.addEquipment(EquipmentType.get("ISPPC"), AeroSpaceFighter.LOC_NOSE);

        fire(fighter, ppc);

        assertEquals(PPC_ATTACK_VALUE, armorLost(), "a single PPC deals its Attack Value");
    }

    @Test
    void chargedPpcCapacitorAddsToTheAttackValue() throws Exception {
        AeroSpaceFighter fighter = addFighter();
        Mounted<?> ppc = fighter.addEquipment(EquipmentType.get("ISPPC"), AeroSpaceFighter.LOC_NOSE);
        Mounted<?> capacitor = fighter.addEquipment(EquipmentType.get("ISPPCCapacitor"), AeroSpaceFighter.LOC_NOSE);
        capacitor.setLinked(ppc);
        capacitor.setModeImmediately("Charge");
        assertEquals(1, ppc.hasChargedCapacitor(), "the capacitor is charged");

        fire(fighter, ppc);

        assertEquals(PPC_ATTACK_VALUE + PPC_CAPACITOR_BONUS, armorLost(), "the capacitor adds 5 to the Attack Value");
    }
}
