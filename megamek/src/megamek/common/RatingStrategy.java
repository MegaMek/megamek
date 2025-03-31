package megamek.common;

public interface RatingStrategy {
    double calculateNewRating(double currentRating, double opponentRating, int result);
}
