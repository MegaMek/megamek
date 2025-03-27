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
    void testPlayerEloAjusted() {

        Game game = new Game();

        // Définir l'équipe victorieuse(équipe 10)
        game.setVictoryTeam(10);
        game.setForceVictory(true);

        // Création d'une equipe gagnante (équipe 10) avec des elos differents pour chaque
        String playerName = "Test Player 2";
        Player winner = new Player(1, playerName);
        winner.setTeam(10);
        winner.setPlayerElo(1500);

        String playerName2 = "Test Player 2";
        Player winner2 = new Player(2, playerName2);
        winner2.setTeam(10);
        winner2.setPlayerElo(1000);

        String playerName3 = "Test Player 2";
        Player winner3 = new Player(3, playerName3);
        winner3.setTeam(10);
        winner3.setPlayerElo(1300);

        // Création d'un joueur perdant (équipe 20) avec un Elo initial de 1500
        String playerName4 = "Test Player 2";
        Player loser = new Player(4, playerName4);
        loser.setTeam(20);
        loser.setPlayerElo(1500);

        // Ajout des joueurs à la partie
        game.addPlayer(winner.getId(), winner);
        game.addPlayer(winner2.getId(), winner2);
        game.addPlayer(winner3.getId(), winner3);
        game.addPlayer(loser.getId(), loser);

        // Simuler la fin de partie en passant la phase à END
        game.end(10,10);

        // Récupérer les Elo mis à jour
        int winnerNewElo = winner.getPlayerElo();
        int winnerNewElo2 = winner2.getPlayerElo();
        int winnerNewElo3 = winner3.getPlayerElo();
        int loserNewElo = loser.getPlayerElo();


        // Pour déboguer, on peut afficher les valeurs (optionnel)
        System.out.println("Elo du gagnant : " + winnerNewElo);
        System.out.println("Elo du gagnant : " + winnerNewElo2);
        System.out.println("Elo du gagnant : " + winnerNewElo3);
        System.out.println("Elo du perdant : " + loserNewElo);

        // Vérifier que l'Elo du gagnant a augmenté et que celui du perdant a diminué
        assertTrue(winnerNewElo > 1500, "Le joueur gagnant doit voir son Elo augmenter.");
        assertTrue(winnerNewElo2 > 1000, "Le joueur gagnant doit voir son Elo augmenter.");
        assertTrue(winnerNewElo3 > 1300, "Le joueur gagnant doit voir son Elo augmenter.");
        assertTrue(loserNewElo < 1500, "Le joueur perdant doit voir son Elo diminuer.");



    }
}
