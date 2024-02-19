package megamek.common.verifier;

import megamek.common.AeroSpaceFighter;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TestAeroTest {

    private AeroSpaceFighter aero;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        aero = new AeroSpaceFighter();
    }

    @Test
    void testCalculateEngineRatingForAeroSpaceFighter() {
        int rating;

        // 25-ton ASF desiring Safe Thrust of 2 should be 0
        rating = TestAero.calculateEngineRating(aero, 25, 2);
        assertEquals(0, rating);

        // 25-ton ASF desiring Safe Thrust of 5 should be 75 (tons * (desiredSafeThrust -2))
        rating = TestAero.calculateEngineRating(aero, 25, 5);
        assertEquals(75, rating);

        // 50-ton ASF desiring Safe Thrust of 5 should be 150
        rating = TestAero.calculateEngineRating(aero, 50, 5);
        assertEquals(150, rating);

        // 100-ton ASF desiring Safe Thrust of 6 should be 400
        rating = TestAero.calculateEngineRating(aero, 100, 6);
        assertEquals(400, rating);
    }
}