package megamek.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerStatsTest {

    @Test
    void testGetBVWithMockedGame() {
        // Arrange
        Player player = new Player(1, "Test Player");

        InGameObject unit1 = mock(InGameObject.class);
        when(unit1.getOwnerId()).thenReturn(1);
        when(unit1.countForStrengthSum()).thenReturn(true);
        when(unit1.getStrength()).thenReturn(100);

        InGameObject unit2 = mock(InGameObject.class);
        when(unit2.getOwnerId()).thenReturn(1);
        when(unit2.countForStrengthSum()).thenReturn(true);
        when(unit2.getStrength()).thenReturn(200);

        List<InGameObject> units = List.of(unit1, unit2);

        IGame mockGame = mock(IGame.class);
        when(mockGame.getInGameObjects()).thenReturn(units);

        PlayerStats stats = new PlayerStats(player, mockGame);

        // Act + Assert
        assertEquals(300, stats.getBV(), "Le BV combiné des unités du joueur devrait être 300.");
    }


}
