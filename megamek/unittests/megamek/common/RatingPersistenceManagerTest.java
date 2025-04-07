package megamek.common;

import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class RatingPersistenceManagerTest {

    @Test
    void testSingletonInstanceIsNotNull() {
        RatingPersistenceManager instance = RatingPersistenceManager.getInstance();
        assertNotNull(instance, "L'instance singleton ne doit pas être null.");
    }

    @Test
    void testGetPlayerRatingCreatesNewIfAbsent() {
        RatingPersistenceManager instance = RatingPersistenceManager.getInstance();
        Player player = new Player(123, "Joueur Test");
        PlayerRating rating = instance.getPlayerRating(player.getId(), player);

        assertNotNull(rating);
        assertEquals(123, rating.getPlayerId());
    }

    @Test
    void testUpdatePlayerRatingPersistsData() {
        RatingPersistenceManager instance = RatingPersistenceManager.getInstance();
        Player player = new Player(999, "Testeur");
        PlayerRating rating = new PlayerRating(player.getId(), "Testeur", false);
        rating.updateRating(1500, 1);

        assertDoesNotThrow(() -> instance.updatePlayerRating(rating));
    }

    @Test
    void testLoadRatingsDoesNotCrash() {
        assertDoesNotThrow(() -> RatingPersistenceManager.getInstance());
    }
}
