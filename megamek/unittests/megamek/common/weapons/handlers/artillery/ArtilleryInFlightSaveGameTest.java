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
package megamek.common.weapons.handlers.artillery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that an artillery round still in flight when a game is saved knows who fired it after the game is loaded. The
 * round's handler saves its own copy of the firing unit, separate from the game's copy once the game has swapped in an
 * updated unit. Before issue #9287 was fixed, that saved copy came back with no owner, and a FASCAM round landing
 * after the load crashed the Offboard phase.
 */
class ArtilleryInFlightSaveGameTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private TWGameManager gameManager;
    private Server server;

    @BeforeEach
    void startServer() throws IOException {
        // Loading a weapon handler looks up the running server's game manager
        gameManager = new TWGameManager();
        server = ServerFactory.createServer(gameManager);
    }

    @AfterEach
    void stopServer() {
        server.die();
    }

    @Test
    @DisplayName("a round in flight across a save and load still knows the player who fired it")
    void inFlightRoundKeepsItsOwnerAfterLoad() throws LocationFullException, EntityLoadingException {
        Game game = gameManager.getGame();
        Player firingPlayer = new Player(0, "Firing Player");
        Player targetPlayer = new Player(1, "Target Player");
        game.addPlayer(firingPlayer.getId(), firingPlayer);
        game.addPlayer(targetPlayer.getId(), targetPlayer);

        Mek artilleryMek = createMek(game, firingPlayer, new Coords(5, 15));
        Mounted<?> arrowIV = artilleryMek.addEquipment(EquipmentType.get("CLArrowIV"), Mek.LOC_RIGHT_TORSO);
        Mek targetMek = createMek(game, targetPlayer, new Coords(5, 1));

        ArtilleryAttackAction attackAction = new ArtilleryAttackAction(artilleryMek.getId(),
              targetMek.getTargetType(),
              targetMek.getId(),
              artilleryMek.getEquipmentNum(arrowIV),
              game);
        ToHitData toHit = new ToHitData();
        toHit.addModifier(4, "Gunnery Skill");
        game.addAttack(new ArtilleryWeaponDistantFireHandler(toHit, attackAction, game, gameManager));

        // A unit update during play replaces the game's unit with a new object; the round keeps the old one
        Mek updatedMek = (Mek) SerializationHelper.getLoadSaveGameXStream()
              .fromXML(SerializationHelper.getSaveGameXStream().toXML(artilleryMek));
        updatedMek.setGame(game);
        game.setEntity(updatedMek.getId(), updatedMek);

        Game loadedGame = (Game) SerializationHelper.getLoadSaveGameXStream()
              .fromXML(SerializationHelper.getSaveGameXStream().toXML(game));
        gameManager.setGame(loadedGame);

        assertEquals(1, loadedGame.getAttacksVector().size(), "The round in flight should survive the save");
        AttackHandler loadedRound = loadedGame.getAttacksVector().getFirst();
        Entity savedFiringUnit = loadedRound.getAttacker();
        assertNotSame(loadedGame.getEntity(artilleryMek.getId()), savedFiringUnit,
              "The round should hold its own copy of the firing unit, as the issue's save does");
        assertSame(loadedGame, savedFiringUnit.getGame(), "The round's firing unit should be attached to the game");
        assertNotNull(savedFiringUnit.getOwner(), "The round's firing unit should have an owner after loading");
        assertEquals(firingPlayer.getId(), savedFiringUnit.getOwner().getId());
    }

    private static Mek createMek(Game game, Player owner, Coords position) {
        Mek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel(owner.getName());
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setName(owner.getName() + " Pilot", 0);
        mek.setCrew(crew);
        mek.setId(game.getNextEntityId());
        mek.setOwner(owner);
        mek.setDeployed(true);
        mek.setPosition(position);
        // Added last, once fully built: addEntity attaches the game and indexes the position
        game.addEntity(mek);
        return mek;
    }
}
