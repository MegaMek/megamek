package megamek.common;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerRating {
    private int INITIAL_RATING = 1500;
    private int playerId;
    private String playerName;
    private boolean isBot;
    private double currentRating;
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
    private List<String> history; // Historique sous forme de chaînes de caractères
    private static final double K_FACTOR = 32.0;

    /**
     * Constructeur de Rating.
     *
     * @param playerId Identifiant unique du joueur
     * @param playerName Nom du joueur
     * @param isBot Indique si le joueur est un bot
     */
    public PlayerRating(int playerId, String playerName, boolean isBot) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.isBot = isBot;
        this.currentRating = INITIAL_RATING;
        this.matchesPlayed = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.history = new ArrayList<>();
    }


    /**
     * Calcule la nouvelle note Elo pour ce joueur.
     *
     * @param globalRating moyenne de rating de l'équipe adverse.
     * @param result Résultat du match : 1 (victoire), 0 (match nul), -1 (défaite).
     * @return La nouvelle note calculée.
     */
    public double calculateNewEloRating(double globalRating, int result) {
        double actualScore;
        if (result > 0) {
            actualScore = 1.0;
        } else if (result == 0) {
            actualScore = 0.5;
        } else {
            actualScore = 0.0;
        }
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (globalRating - currentRating) / 400.0));
        return currentRating + K_FACTOR * (actualScore - expectedScore);
    }

    /**
     * Met à jour la note du joueur après un match, calcule la nouvelle note Elo et enregistre l'historique du changement.
     *
     * @param globalRating La note actuelle de l'adversaire.
     * @param result Résultat du match : 1 (victoire), 0 (match nul), -1 (défaite).
     */
    public void updateRating(double globalRating, int result) {
        double newRating = calculateNewEloRating(globalRating, result);

        String changeLog = "Ancienne note: " + currentRating +
              ", Nouvelle note: " + newRating +
              ", Résultat: " + result;
        history.add(changeLog);

        matchesPlayed++;
        if (result > 0) {
            wins++;
        } else if (result == 0) {
            draws++;
        } else {
            losses++;
        }
        currentRating = newRating;
    }

    // Getters et setters

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isBot() {
        return isBot;
    }

    public double getCurrentRating() {
        return currentRating;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getDraws() {
        return draws;
    }

    public int getLosses() {
        return losses;
    }

    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    // Méthode d'affichage de l'état du rating (pour débogage)
    @Override
    public String toString() {
        return "Rating{" +
              "playerId=" + playerId +
              ", playerName='" + playerName + '\'' +
              ", isBot=" + isBot +
              ", currentRating=" + currentRating +
              ", matchesPlayed=" + matchesPlayed +
              ", wins=" + wins +
              ", draws=" + draws +
              ", losses=" + losses +
              ", history=" + history +
              '}';
    }
}
