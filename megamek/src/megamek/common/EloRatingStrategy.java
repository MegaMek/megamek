package megamek.common;

public class EloRatingStrategy implements RatingStrategy {

    private static final double K_FACTOR = 32.0;

    @Override
    public double calculateNewRating(double currentRating, double opponentRating, int result) {
        double actualScore;
        if (result > 0) {
            actualScore = 1.0;  // Victoire
        } else if (result == 0) {
            actualScore = 0.5; // Match nul
        } else {
            actualScore = 0.0; // Défaite
        }
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - currentRating) / 400.0));
        return currentRating + K_FACTOR * (actualScore - expectedScore);
    }

}
