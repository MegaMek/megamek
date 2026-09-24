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

package megamek.common.weapons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.PilotOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponDistantFireHandler;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the "incoming artillery" hex marker raised when a fire mission is declared (issue #8685, PR review).
 *
 * <p>That marker is created with {@link SpecialHexDisplay#SHD_VISIBLE_TO_TEAM}, so only the firing player's team is
 * meant to see the hex being aimed at. {@link SpecialHexDisplay#isObscured(Player)} returns {@code false} for a marker
 * with no owner, and the server filters on exactly that call before sending markers to a client - so a marker built
 * with a null owner would be broadcast to every player, showing the aim point to the enemy under double-blind. The
 * handler therefore does not raise the marker at all when the firing player cannot be resolved.</p>
 */
class ArtilleryIncomingMarkerTest {

    private static final int BOARD_WIDTH = 16;
    private static final int BOARD_HEIGHT = 17;

    private Player attackingPlayer;
    private Player defendingPlayer;
    private TWGameManager gameManager;
    private Game game;
    private Server server;
    private Board board;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        attackingPlayer = new Player(0, "Independent Base Defenses");
        defendingPlayer = new Player(1, "Dust Devils");
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.createVictoryConditions();
        game.addPlayer(attackingPlayer.getId(), attackingPlayer);
        game.addPlayer(defendingPlayer.getId(), defendingPlayer);

        game.setOptions(new GameOptions());

        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        game.setBoard(board);

        server = ServerFactory.createServer(gameManager);
    }

    @AfterEach
    void tearDown() {
        server.die();
    }

    private Mek createMek(String chassis, Player owner, Coords position) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.setChassis(chassis);
        mek.setModel("Test");

        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Gunner", 0);
        crew.setOptions(new PilotOptions());
        mek.setCrew(crew);

        mek.setId(game.getNextEntityId());
        game.addEntity(mek);
        mek.setOwner(owner);
        mek.setDeployed(true);
        mek.setPosition(position);
        return mek;
    }

    /** Builds a Thumper-armed attacker, a target, and the handler for a fire mission between them. */
    private ArtilleryWeaponDistantFireHandler declareFireMission(Coords targetPosition) throws LocationFullException, EntityLoadingException {
        Mek attacker = createMek("Bombard", attackingPlayer, new Coords(1, 1));
        Mounted<?> thumper = attacker.addEquipment(EquipmentType.get(EquipmentTypeLookup.THUMPER_ARTY),
              Mek.LOC_CENTER_TORSO);
        AmmoType thumperAmmo = (AmmoType) EquipmentType.get("ISThumperAmmo");
        Mounted<?> ammoBin = attacker.addEquipment(thumperAmmo, Mek.LOC_CENTER_TORSO);
        thumper.setLinked(ammoBin);

        Mek defender = createMek("Marauder", defendingPlayer, targetPosition);

        ArtilleryAttackAction attack = new ArtilleryAttackAction(attacker.getId(),
              defender.getTargetType(),
              defender.getId(),
              attacker.getEquipmentNum(thumper),
              game);

        ToHitData toHit = new ToHitData();
        toHit.addModifier(4, "Gunnery Skill");
        return new ArtilleryWeaponDistantFireHandler(toHit, attack, game, gameManager);
    }

    private boolean hasIncomingMarkerAt(Coords coords) {
        Collection<SpecialHexDisplay> markers = board.getSpecialHexDisplay(coords);
        if (markers == null) {
            return false;
        }
        for (SpecialHexDisplay marker : markers) {
            if (marker.getType() == SpecialHexDisplay.Type.ARTILLERY_INCOMING) {
                return true;
            }
        }
        return false;
    }

    @Test
    void incomingMarkerIsRaisedWhenTheFiringPlayerIsInTheGame() throws LocationFullException, EntityLoadingException {
        Coords targetPosition = new Coords(8, 8);
        ArtilleryWeaponDistantFireHandler handler = declareFireMission(targetPosition);

        game.setPhase(GamePhase.TARGETING);
        handler.handle(GamePhase.TARGETING, new Vector<Report>());

        assertTrue(hasIncomingMarkerAt(targetPosition),
              "A fire mission from a player still in the game should warn that team");
    }

    @Test
    void noIncomingMarkerIsRaisedWhenTheFiringPlayerHasLeftTheGame() throws LocationFullException, EntityLoadingException {
        Coords targetPosition = new Coords(8, 8);
        ArtilleryWeaponDistantFireHandler handler = declareFireMission(targetPosition);

        // The firing player is dropped from the game, as happens when their last unit is destroyed while a round is
        // still in flight. A team-visible marker with no owner would be sent to everyone, revealing the aim hex.
        game.removePlayer(attackingPlayer.getId());

        game.setPhase(GamePhase.TARGETING);
        handler.handle(GamePhase.TARGETING, new Vector<Report>());

        assertFalse(hasIncomingMarkerAt(targetPosition),
              "A marker with no owner is visible to every player, so none should be raised at all");
    }
}
