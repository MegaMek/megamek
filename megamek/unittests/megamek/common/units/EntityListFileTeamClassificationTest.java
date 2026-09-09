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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for issue #8890: PACAR transfers the player's units to an "@AI" bot on the player's own team, so the
 * player's real force must be gathered by team, not by exact owner, when the end-of-game MUL is written. The
 * {@code teamAsLiving} overload of {@link EntityListFile#saveTo} and its {@code countsAsPlayerOwn} predicate carry that
 * classification.
 */
class EntityListFileTeamClassificationTest {

    private static final int LOCAL_ID = 0;
    private static final int AI_BOT_ID = 1;
    private static final int ENEMY_ID = 2;
    private static final int PLAYER_TEAM = 1;
    private static final int ENEMY_TEAM = 2;

    /** A survivor-side chassis and an enemy-side chassis, chosen distinct so each can be found by name in the MUL. */
    private static final String SURVIVOR_UNIT = "Atlas AS7-CM";
    private static final String ENEMY_UNIT = "Archer ARC-2R";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Player localPlayer;
    private Player aiBot;
    private Player enemy;

    @BeforeEach
    void setUp() {
        localPlayer = new Player(LOCAL_ID, "Commander");
        localPlayer.setTeam(PLAYER_TEAM);
        aiBot = new Player(AI_BOT_ID, "Commander@AI");
        aiBot.setTeam(PLAYER_TEAM);
        enemy = new Player(ENEMY_ID, "Pirates");
        enemy.setTeam(ENEMY_TEAM);
    }

    // region countsAsPlayerOwn predicate

    @Test
    void ownUnitCountsRegardlessOfFlag() {
        assertTrue(EntityListFile.countsAsPlayerOwn(localPlayer, localPlayer, false));
        assertTrue(EntityListFile.countsAsPlayerOwn(localPlayer, localPlayer, true));
    }

    @Test
    void teammateCountsOnlyWhenTeamAsLiving() {
        assertFalse(EntityListFile.countsAsPlayerOwn(aiBot, localPlayer, false),
              "without teamAsLiving the @AI teammate is not the player's own");
        assertTrue(EntityListFile.countsAsPlayerOwn(aiBot, localPlayer, true),
              "with teamAsLiving the @AI teammate counts as the player's own");
    }

    @Test
    void enemyNeverCounts() {
        assertFalse(EntityListFile.countsAsPlayerOwn(enemy, localPlayer, false));
        assertFalse(EntityListFile.countsAsPlayerOwn(enemy, localPlayer, true));
    }

    // endregion

    // region saveTo section placement

    @Test
    void teamAsLivingPutsTheAiTeammateInSurvivorsAndEnemyInSalvage() throws Exception {
        Game game = gameWith(unit(SURVIVOR_UNIT, aiBot), unit(ENEMY_UNIT, enemy));
        String mul = writeMul(game, true);

        assertTrue(sectionOf(mul, "survivors").contains("Atlas"),
              "the @AI teammate's unit should be a survivor: " + mul);
        assertTrue(sectionOf(mul, "salvage").contains("Archer"),
              "the enemy unit should be salvage: " + mul);
    }

    @Test
    void withoutTeamAsLivingTheAiTeammateIsNotASurvivor() throws Exception {
        Game game = gameWith(unit(SURVIVOR_UNIT, aiBot), unit(ENEMY_UNIT, enemy));
        String mul = writeMul(game, false);

        assertFalse(sectionOf(mul, "survivors").contains("Atlas"),
              "without teamAsLiving the teammate's unit must not be listed as one of the player's own survivors");
        assertTrue(sectionOf(mul, "allies").contains("Atlas"),
              "instead it belongs in the allies section: " + mul);
    }

    // endregion

    // region helpers

    private Entity unit(String unitName, Player owner) throws Exception {
        Entity entity = new megamek.common.loaders.MekFileParser(
              new File("testresources/data/mekfiles/" + unitName + ".mtf")).getEntity();
        entity.setOwner(owner);
        return entity;
    }

    private Game gameWith(Entity... entities) {
        Game game = new Game();
        game.addPlayer(LOCAL_ID, localPlayer);
        game.addPlayer(AI_BOT_ID, aiBot);
        game.addPlayer(ENEMY_ID, enemy);
        for (Entity entity : entities) {
            entity.setGame(game);
            entity.setId(game.getNextEntityId());
            game.addEntity(entity);
        }
        return game;
    }

    private String writeMul(Game game, boolean teamAsLiving) throws Exception {
        Client client = mock(Client.class);
        when(client.getGame()).thenReturn(game);
        when(client.playerExists(anyInt())).thenReturn(true);
        File file = tempDir.resolve("victory-" + teamAsLiving + ".mul").toFile();
        EntityListFile.saveTo(file, client, localPlayer, teamAsLiving);
        return Files.readString(file.toPath());
    }

    /** Returns the text between {@code <section ...>} and {@code </section>}, or "" when the section is absent. */
    private static String sectionOf(String mul, String section) {
        int open = mul.indexOf('<' + section);
        int close = mul.indexOf("</" + section + '>');
        if ((open < 0) || (close < 0)) {
            return "";
        }
        return mul.substring(open, close);
    }

    // endregion
}
