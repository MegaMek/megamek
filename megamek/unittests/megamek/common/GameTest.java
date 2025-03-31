/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import megamek.server.victory.VictoryResult;

class GameTest {

    @Test
    void testCancelVictory() {
        // Default test
        Game game = new Game();
        game.cancelVictory();
        assertFalse(game.isForceVictory());
        assertSame(Player.PLAYER_NONE, game.getVictoryPlayerId());
        assertSame(Player.TEAM_NONE, game.getVictoryTeam());

        // Test with members set to specific values
        Game game2 = new Game();
        game2.setVictoryPlayerId(10);
        game2.setVictoryTeam(10);
        game2.setForceVictory(true);

        game2.cancelVictory();
        assertFalse(game.isForceVictory());
        assertSame(Player.PLAYER_NONE, game.getVictoryPlayerId());
        assertSame(Player.TEAM_NONE, game.getVictoryTeam());
    }

    @Test
    void testGetVictoryReport() {
        Game game = new Game();
        game.createVictoryConditions();
        VictoryResult victoryResult = game.getVictoryResult();
        assertNotNull(victoryResult);

        // Note: this accessors are tested in VictoryResultTest
        assertSame(Player.PLAYER_NONE, victoryResult.getWinningPlayer());
        assertSame(Player.TEAM_NONE, victoryResult.getWinningTeam());

        int winningPlayer = 2;
        int winningTeam = 5;

        // Test an actual scenario
        Game game2 = new Game();
        game2.setVictoryTeam(winningTeam);
        game2.setVictoryPlayerId(winningPlayer);
        game2.setForceVictory(true);
        game2.createVictoryConditions();
        VictoryResult victoryResult2 = game2.getVictoryResult();

        assertSame(winningPlayer, victoryResult2.getWinningPlayer());
        assertSame(winningTeam, victoryResult2.getWinningTeam());
    }


    @Test
    void testPlayerEloAdjusted() {

        Game game = new Game();
        double rating1 = 2000;
        double rating2 = 1900;
        double rating3 = 1850;
        double rating4 = 2200;

        // Définir l'équipe victorieuse (équipe 10)
        game.setVictoryTeam(10);
        game.setForceVictory(true);

        // Création d'une équipe gagnante (équipe 10) avec des Elos différents
        Player winner1 = new Player(1, "Test Player 1");
        winner1.getRatingObject().setPlayerRating(rating1);
        winner1.setTeam(10);

        Player winner2 = new Player(2, "Test Player 2");
        winner2.getRatingObject().setPlayerRating(rating2);
        winner2.setTeam(10);

        Player winner3 = new Player(3, "Test Player 3");
        winner3.getRatingObject().setPlayerRating(rating3);
        winner3.setTeam(10);

        // Création d'un joueur perdant (équipe 20) avec un Elo initial de 1500
        Player loser = new Player(4, "Test Player 4");
        loser.getRatingObject().setPlayerRating(rating4);
        loser.setTeam(20);

        // Ajout des joueurs à la partie
        game.addPlayer(winner1.getId(), winner1);
        game.addPlayer(winner2.getId(), winner2);
        game.addPlayer(winner3.getId(), winner3);
        game.addPlayer(loser.getId(), loser);

        // Simuler la fin de partie en passant la phase à END
        game.end(10, 10);

        // Récupérer les Elo mis à jour
        int winnerNewElo1 = (int) winner1.getRatingObject().getCurrentRating();
        int winnerNewElo2 = (int) winner2.getRatingObject().getCurrentRating();
        int winnerNewElo3 = (int) winner3.getRatingObject().getCurrentRating();
        int loserNewElo = (int) loser.getRatingObject().getCurrentRating();

        System.out.println("Elo gagnant 1 : " + winnerNewElo1);
        System.out.println("Elo gagnant 2 : " + winnerNewElo2);
        System.out.println("Elo gagnant 3 : " + winnerNewElo3);
        System.out.println("Elo perdant : " + loserNewElo);

        // Vérifier que les gagnants voient leur Elo augmenter et que celui du perdant diminue
        assertTrue(winnerNewElo1 > rating1, "Le joueur gagnant 1 doit voir son Elo augmenter.");
        assertTrue(winnerNewElo2 > rating2, "Le joueur gagnant 2 doit voir son Elo augmenter.");
        assertTrue(winnerNewElo3 > rating3, "Le joueur gagnant 3 doit voir son Elo augmenter.");
        assertTrue(loserNewElo < rating4, "Le joueur perdant doit voir son Elo diminuer.");
    }

}
