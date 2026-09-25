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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.BuildingBlock;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercise the server's real phase/initiative entry points rather than forcing a mobile's done flag for movement. */
class MobileStructureTurnEligibilityTest {
    private static final Coords ORIGIN = new Coords(8, 8);

    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private MobileStructure loadDesign(EntityMovementMode mode, double mp) throws Exception {
        var design = new MobileStructure(BuildingType.MEDIUM, IBuilding.STANDARD);
        design.setChassis("Mobile turn eligibility");
        design.setModel("Regression");
        design.setYear(3150);
        design.setTechLevel(TechConstants.T_IS_ADVANCED);
        design.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 2, 20, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        design.setMovementMode(mode);
        design.setMaximumMP(mp);
        design.getDesign().setEnvironmentalSealing(mode == EntityMovementMode.SUBMARINE);
        return (MobileStructure) new BLKStructureFile(BLKFile.getBlock(design)).getEntity();
    }

    private TWGameManager deploy(MobileStructure mobile) {
        var manager = new LocalManager();
        var game = manager.getGame();
        game.getOptions().initialize();
        game.getOptions().getOption(OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT).setValue(true);
        var board = BoardLoader.initializeBoard("size 20 20\nend\n");
        if (mobile.isWaterStructure()) {
            for (int x = 0; x < 20; x++) {
                for (int y = 0; y < 20; y++) {
                    board.getHex(new Coords(x, y)).addTerrain(new Terrain(Terrains.WATER, 20));
                }
            }
        }
        game.setBoard(board);
        var owner = new Player(0, "Mobile owner");
        owner.setTeam(Player.TEAM_NONE);
        owner.setStartingPos(Board.START_ANY);
        game.addPlayer(owner.getId(), owner);
        game.setupTeams();
        owner.getInitiative().addRoll(0, "");
        game.getTeams().forEach(team -> team.getInitiative().addRoll(0, ""));
        mobile.setId(1);
        mobile.setOwner(owner);
        mobile.setStartingPos(Board.START_ANY);
        game.addEntity(mobile);
        assertTrue(mobile.isDeploymentPositionAndFacingValid(ORIGIN, 0, 0, 0));
        mobile.setPosition(ORIGIN);
        mobile.setDeployed(true);
        mobile.setDone(true);
        mobile.updateBuildingEntityHexes(0, manager);
        return manager;
    }

    private void startMovementRound(TWGameManager manager, int round) {
        var game = manager.getGame();
        game.setRoundCount(round);
        game.setPhase(GamePhase.INITIATIVE);
        manager.resetEntityRound();
        game.setPhase(GamePhase.MOVEMENT);
        manager.resetEntityPhase(GamePhase.MOVEMENT);
        manager.setIneligible(GamePhase.MOVEMENT);
        manager.determineTurnOrder(GamePhase.MOVEMENT);
    }

    private void assertUsableTurn(TWGameManager manager, MobileStructure mobile, int quarters) {
        var game = manager.getGame();
        assertFalse(mobile.isDone(), "real phase preparation must release the deployment/previous-round done flag");
        assertTrue(mobile.isActive());
        assertFalse(mobile.isImmobile());
        assertTrue(mobile.isEligibleFor(GamePhase.MOVEMENT));
        assertTrue(mobile.isSelectableThisTurn());
        assertEquals(quarters, mobile.getWalkMP());
        assertEquals(1, game.getTurnsList().size());
        assertTrue(game.getTurnsList().getFirst().isValidEntity(mobile, game),
              "the generated movement turn must actually accept the mobile's entity class");
    }

    @ParameterizedTest
    @CsvSource({ "TRACKED, 0.25", "TRACKED, 1", "NAVAL, 0.25", "NAVAL, 1",
                 "SUBMARINE, 0.25", "SUBMARINE, 1", "VTOL, 0.25", "VTOL, 1" })
    void loadedMobileGetsMovementTurnsAfterDeploymentAndOnTheFollowingRound(EntityMovementMode mode, double mp)
          throws Exception {
        var mobile = loadDesign(mode, mp);
        var manager = deploy(mobile);
        startMovementRound(manager, 1);
        assertUsableTurn(manager, mobile, (int) (mp * 4));

        mobile.mpUsed = mobile.getWalkMP();
        mobile.setDone(true);
        assertFalse(mobile.isSelectableThisTurn());
        startMovementRound(manager, 2);
        assertEquals(0, mobile.mpUsed);
        assertUsableTurn(manager, mobile, (int) (mp * 4));
    }

    @Test
    void genuineMekBayDesignGetsItsMovementTurnThroughTheSamePhaseSequence() throws Exception {
        try (var input = Files.newInputStream(Path.of("testresources/megamek/common/units/MekBayMobileStructure.blk"))) {
            var mobile = (MobileStructure) new BLKStructureFile(new BuildingBlock(input)).getEntity();
            var manager = deploy(mobile);
            startMovementRound(manager, 1);
            assertUsableTurn(manager, mobile, 5);
        }
    }

    @Test
    void immobilizedMobileIsSkippedButGetsATurnAgainWhenItsMotivePowerReturns() throws Exception {
        var mobile = loadDesign(EntityMovementMode.TRACKED, 1);
        var manager = deploy(mobile);
        mobile.setGrounded(true);
        startMovementRound(manager, 1);
        assertTrue(mobile.isDone());
        assertEquals(0, mobile.getWalkMP());
        assertTrue(manager.getGame().getTurnsList().isEmpty());
        mobile.setGrounded(false);
        startMovementRound(manager, 2);
        assertUsableTurn(manager, mobile, 4);
    }
}
