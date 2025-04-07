package megamek.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRatingTest {

    @Test
    void testPlayerRatingInitialization() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        assertEquals(1500, playerRating.getCurrentRating(), "Le rating initial du joueur doit être de 1500.");
        assertEquals(0, playerRating.getMatchesPlayed(), "Le nombre de matchs joués doit être initialisé à 0.");
        assertEquals(0, playerRating.getWins(), "Le nombre de victoires doit être initialisé à 0.");
        assertEquals(0, playerRating.getDraws(), "Le nombre de matchs nuls doit être initialisé à 0.");
        assertEquals(0, playerRating.getLosses(), "Le nombre de défaites doit être initialisé à 0.");
        assertNotNull(playerRating.getHistory(), "L'historique des matchs doit être initialisé.");
    }

    @Test
    void testUpdateRating() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        double initialRating = playerRating.getCurrentRating();
        double enemyTeamRating = 1600;

        // Simulation d'une victoire
        playerRating.updateRating(enemyTeamRating, 1); // 1 pour victoire
        assertTrue(playerRating.getCurrentRating() > initialRating, "Le rating du joueur doit augmenter après une victoire.");

        // Simulation d'un match nul
        double newRatingAfterDraw = playerRating.getCurrentRating();
        playerRating.updateRating(enemyTeamRating, 0); // 0 pour match nul
        assertEquals(newRatingAfterDraw, playerRating.getCurrentRating(), "Le rating du joueur doit pas changer après un match nul.");

        // Simulation d'une défaite
        double newRatingAfterLoss = playerRating.getCurrentRating();
        playerRating.updateRating(enemyTeamRating, -1); // -1 pour défaite
        assertTrue(playerRating.getCurrentRating() < newRatingAfterLoss, "Le rating du joueur doit diminuer après une défaite.");
    }

    @Test
    void testRatingHistory() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        double enemyTeamRating = 1600;

        // Simuler une victoire
        playerRating.updateRating(enemyTeamRating, 1); // 1 pour victoire
        assertEquals(1, playerRating.getHistory().size(), "L'historique doit enregistrer une entrée après un match.");
        assertTrue(playerRating.getHistory().get(0).contains("Ancienne note"), "L'historique doit contenir la note avant le match.");
        assertTrue(playerRating.getHistory().get(0).contains("Nouvelle note"), "L'historique doit contenir la note après le match.");
    }


    @Test
    void testBotPlayerRating() {
        Player botPlayer = new Player(1, "Bot Player");
        botPlayer.setBot(true);
        PlayerRating botRating = new PlayerRating(botPlayer.getId(), botPlayer.getName(), botPlayer.isBot());

        assertTrue(botRating.isBot(), "Le joueur doit être un bot.");
        assertEquals(1500, botRating.getCurrentRating(), "Le rating initial du bot doit être de 1500.");

        double initialRating = botRating.getCurrentRating();
        double enemyTeamRating = 1600;

        // Simulation d'une victoire
        botRating.updateRating(enemyTeamRating, 1); // 1 pour victoire
        assertTrue(botRating.getCurrentRating() > initialRating, "Le rating du joueur doit augmenter après une victoire.");

        // Simulation d'un match nul
        double newRatingAfterDraw = botRating.getCurrentRating();
        botRating.updateRating(enemyTeamRating, 0); // 0 pour match nul
        assertEquals(newRatingAfterDraw, botRating.getCurrentRating(), "Le rating du joueur doit pas changer après un match nul.");

        // Simulation d'une défaite
        double newRatingAfterLoss = botRating.getCurrentRating();
        botRating.updateRating(enemyTeamRating, -1); // -1 pour défaite
        assertTrue(botRating.getCurrentRating() < newRatingAfterLoss, "Le rating du joueur doit diminuer après une défaite.");
    }

    @Test
    void testToString() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        String ratingString = playerRating.toString();
        assertTrue(ratingString.contains("playerId=1"), "La chaîne doit contenir l'ID du joueur.");
        assertTrue(ratingString.contains("playerName='Test Player'"), "La chaîne doit contenir le nom du joueur.");
        assertTrue(ratingString.contains("currentRating=" + playerRating.getCurrentRating()), "La chaîne doit contenir le rating actuel.");
    }

    @Test
    void testMultiplePlayerRatingAdjusted() {

        Game game = new Game();
        //partie competitive donc rating va être affecter
        game.setIsCompetitive(true);
        double rating1 = 2000;
        double rating2 = 1900;
        double rating3 = 1850;
        double rating4 = 2200;

        // Définir l'équipe gagnate (équipe 10)
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


        //Pour voir les modifications
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

    // Test d'isolation : vérifier que les parties non compétitives n'affectent pas les ratings
    @Test
    void testNonCompetitiveGamesDoNotAffectRatings() {
        Game game = new Game();
        // Configurer une partie non compétitive
        game.setIsCompetitive(false);
        double initialRating = 1500;

        Player player1 = new Player(1, "Test Player 1");
        player1.getRatingObject().setPlayerRating(initialRating);
        game.addPlayer(player1.getId(), player1);

        Player player2 = new Player(2, "Test Player 2");
        player2.getRatingObject().setPlayerRating(initialRating);
        game.addPlayer(player2.getId(), player2);


        game.setVictoryTeam(0);
        game.setForceVictory(false);
        game.end(0, 0);

        double player1NewRating = player1.getRatingObject().getCurrentRating();
        double player2NewRating = player2.getRatingObject().getCurrentRating();

        assertEquals(initialRating, player1NewRating, "Le rating du joueur 1 ne doit pas être modifié.");
        assertEquals(initialRating, player2NewRating, "Le rating du joueur 2 ne doit pas être modifié.");
    }
    @Test
    void testStatisticsTracking() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        // Initial
        assertEquals(0, playerRating.getMatchesPlayed());
        assertEquals(0, playerRating.getWins());
        assertEquals(0, playerRating.getDraws());
        assertEquals(0, playerRating.getLosses());

        // win
        playerRating.updateRating(1600, 1);
        assertEquals(1, playerRating.getMatchesPlayed());
        assertEquals(1, playerRating.getWins());
        assertEquals(0, playerRating.getDraws());
        assertEquals(0, playerRating.getLosses());

        // draw
        playerRating.updateRating(1600, 0);
        assertEquals(2, playerRating.getMatchesPlayed());
        assertEquals(1, playerRating.getWins());
        assertEquals(1, playerRating.getDraws());
        assertEquals(0, playerRating.getLosses());

        // loss
        playerRating.updateRating(1600, -1);
        assertEquals(3, playerRating.getMatchesPlayed());
        assertEquals(1, playerRating.getWins());
        assertEquals(1, playerRating.getDraws());
        assertEquals(1, playerRating.getLosses());
    }

    @Test
    void testExtremeRatingValues() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        // Tester avec un rating très élevé
        playerRating.setPlayerRating(3000);
        playerRating.updateRating(1500, -1); // Défaite contre un joueur moyen
        assertTrue(playerRating.getCurrentRating() < 3000,
              "Le rating élevé doit diminuer après une défaite.");

        // Tester avec un rating très bas
        playerRating.setPlayerRating(500);
        playerRating.updateRating(1500, 1); // Victoire contre un joueur moyen
        assertTrue(playerRating.getCurrentRating() > 500,
              "Le rating bas doit augmenter après une victoire.");
    }
    @Test
    void testSetPlayerRating() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        // Tester la modification directe du rating
        playerRating.setPlayerRating(2000);
        assertEquals(2000, playerRating.getCurrentRating(),
              "Le rating doit être mis à jour par setPlayerRating.");
    }

    @Test
    void testCalculWithDifferentResults() {
        Player player = new Player(1, "Test Player");
        PlayerRating playerRating = new PlayerRating(player.getId(), player.getName(), player.isBot());

        // Sauvegarder le rating initial
        double initialRating = playerRating.getCurrentRating();

        // Tester avec différentes valeurs de résultat
        playerRating.updateRating(1600, 2); // Valeur autre que 1
        assertTrue(playerRating.getCurrentRating() > initialRating,
              "Tout résultat positif doit être traité comme une victoire.");

        double ratingAfterWin = playerRating.getCurrentRating();
        playerRating.updateRating(1600, -2); // Valeur négative autre que -1
        assertTrue(playerRating.getCurrentRating() < ratingAfterWin,
              "Tout résultat négatif doit être traité comme une défaite.");
    }

}
