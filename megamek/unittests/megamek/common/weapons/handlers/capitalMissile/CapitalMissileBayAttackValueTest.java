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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayWeapons.capital.CapitalMissileBayWeapon;
import megamek.common.weapons.handlers.BayWeaponHandler;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapitalMissileBayAttackValueTest {
    private TWGameManager gameManager;
    private Game game;
    private Dropship dropShip;
    private AeroSpaceFighter target;

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

    @BeforeEach
    void setUpCombatants() {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.getOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).setValue("Experimental");
        game.setBoard(Board.getSpaceBoard(16, 17));
        Player attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        dropShip = new Dropship();
        dropShip.setCrew(new Crew(CrewType.VESSEL));
        dropShip.setWeight(3000);
        place(game, dropShip, attackerOwner, new Coords(3, 3));
        target = new AeroSpaceFighter();
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setWeight(50);
        place(game, target, targetOwner, new Coords(3, 4));
    }

    private WeaponMounted addMissileBay(EquipmentType bayType, String missileType) throws Exception {
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(bayType, Aero.LOC_NOSE);
        WeaponMounted launcher = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(missileType), Aero.LOC_NOSE);
        AmmoMounted missiles = (AmmoMounted) dropShip.addEquipment(EquipmentType.get("Ammo " + missileType),
              Aero.LOC_NOSE);
        bay.addWeaponToBay(launcher);
        bay.addAmmoToBay(missiles);
        launcher.setLinked(missiles);
        return bay;
    }

    /** A bay's Damage Value rounds its total normally (SO p.88), so one Stingray's 3.5 deals 4, not 3 */
    @Test
    void aStingrayBayRoundsItsHalfPointUp() throws Exception {
        WeaponMounted bay = addMissileBay(EquipmentType.get(EquipmentTypeLookup.SC_MISSILE_BAY), "Stingray");
        WeaponAttackAction attack = new WeaponAttackAction(dropShip.getId(), target.getId(),
              dropShip.getEquipmentNum(bay));
        CapitalMissileBayHandler handler = new CapitalMissileBayHandler(new ToHitData(2, "test"), attack, game,
              gameManager);

        assertEquals(4, handler.calcAttackValue());
    }

    @Test
    void aBearingsOnlyMissileInFlightResolvesAfterLoadingAnOlderSave() throws Exception {
        Server server = ServerFactory.createServer(gameManager);
        try {
            // At the standard launch velocity of 50 hexes per turn this missile has one turn to impact.
            game.setBoard(Board.getSpaceBoard(8, 55));
            target.setPosition(new Coords(3, 53));
            game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_LAUNCH)
                  .setValue(true);
            CapitalMissileBayWeapon bayType = new CapitalMissileBayWeapon();
            bayType.adaptToGameOptions(game.getOptions());
            WeaponMounted bay = addMissileBay(bayType, "Barracuda");
            bay.setModeImmediately(Weapon.MODE_CAP_MISSILE_BEARING_SHORT);
            ArtilleryAttackAction attack = new ArtilleryAttackAction(dropShip.getId(), target.getTargetType(),
                  target.getId(), dropShip.getEquipmentNum(bay), game);
            assertEquals(1, attack.getTurnsTilHit());
            var missile = assertInstanceOf(CapitalMissileBearingsOnlyHandler.class,
                  bayType.getCorrectHandler(new ToHitData(TargetRoll.AUTOMATIC_FAIL, "test"), attack, game, gameManager));
            game.addAttack(missile);
            assertTrue(missile.handle(GamePhase.TARGETING, new Vector<>()));

            var oldFormat = SerializationHelper.getSaveGameXStream();
            oldFormat.omitField(BayWeaponHandler.class, "burnedOut");
            String xml = oldFormat.toXML(game);
            assertFalse(xml.contains("<burnedOut"));
            Game loadedGame = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
            gameManager.setGame(loadedGame);
            var loadedMissile = assertInstanceOf(CapitalMissileBearingsOnlyHandler.class,
                  loadedGame.getAttacksVector().getFirst());

            assertTrue(loadedMissile.handle(GamePhase.FIRING, new Vector<>()));
            assertFalse(loadedMissile.handle(GamePhase.FIRING, new Vector<>()));
        } finally {
            server.die();
        }
    }
}
