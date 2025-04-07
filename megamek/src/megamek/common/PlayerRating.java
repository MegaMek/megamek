package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerRating implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int INITIAL_RATING = 1500;
    private int playerId;
    private String playerName;
    private boolean isBot;
    private double currentRating;
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
    private List<String> history; // Historique sous forme de chaînes de caractères
    // Elo Rating Strategy par défaut
    private RatingStrategy ratingStrategy = new EloRatingStrategy();

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
        this.ratingStrategy = new EloRatingStrategy(); // ← NÉCESSAIRE !
    }

    public void updateRating(double enemyTeamRating, int result) {
        double newRating = ratingStrategy.calculateNewRating(this.currentRating,enemyTeamRating,result);

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

    public void setPlayerRating(double playerRating) {
        this.currentRating = playerRating;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setRatingStrategy(RatingStrategy strategy) {
        this.ratingStrategy = strategy;
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

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.ratingStrategy = new EloRatingStrategy(); // Remet une stratégie par défaut
    }
}
