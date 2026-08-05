package megamek.common.rules.totalwarfare;

import megamek.common.rules.RulesArtillery;
import megamek.common.rules.core.CoreRulesArtillery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TWRulesArtilleryTest {

    RulesArtillery rules = new TWRulesArtillery();

    @BeforeEach
    void setUp() {
    }

    @ParameterizedTest
    @CsvSource({ "1, true, false, 4", "17, true, false, 4", "18, true, false, 4",   // Direct fire
                 "17, false, false, 4", "18, false, false, 7",                      // Indirect fire
                 "10, true, true, 3"                                                // Flak
    })
    void computeArtilleryBaseMod(int distance, boolean direct, boolean flak, int expected) {
        assertEquals(expected, rules.computeArtilleryBaseMod(distance, direct, flak));
    }
}
