package megamek.common.rules.core;

import megamek.common.rules.RulesArtillery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CoreRulesArtilleryTest {
    RulesArtillery rules = new CoreRulesArtillery();

    @BeforeEach
    void setUp() {
    }

    @ParameterizedTest
    @CsvSource({ "1, false, false, 4", "17, false, false, 4", "18, false, false, 4", "10, true, false, 4",
                 "10, true, true, 4"})
    void computeArtilleryBaseMod(int distance, boolean direct, boolean flak, int expected) {
        assertEquals(expected, rules.computeArtilleryBaseMod(distance, direct, flak));
    }
}
