package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.rules.core.CoreRulesGame;
import megamek.common.rules.totalwarfare.TWRulesGame;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesGameTest {
    @Test
    void coreAndTotalWarfareRulesExerciseGameLogic() {
        CoreRulesGame core = new CoreRulesGame();
        TWRulesGame total = new TWRulesGame();
        Game game = Mockito.mock(Game.class);
        Entity entity = Mockito.mock(Entity.class);
        Player player = Mockito.mock(Player.class);

        Mockito.when(entity.getGame()).thenReturn(game);
        Mockito.when(entity.getDeployRound()).thenReturn(0);
        Mockito.when(entity.isDeployed()).thenReturn(false);
        Mockito.when(entity.getStartingPos()).thenReturn(Board.START_EDGE);
        Mockito.when(entity.getOwner()).thenReturn(player);
        Mockito.when(player.isBot()).thenReturn(false);
        Mockito.when(game.getCurrentRound()).thenReturn(2);
        Mockito.when(entity.isImmobile()).thenReturn(false);

        assertTrue(core.ammoDumping() == false);
        assertTrue(total.ammoDumping());
        assertTrue(core.eligibleForPhase(entity, GamePhase.MOVEMENT));
        assertTrue(total.eligibleForPhase(entity, GamePhase.MOVEMENT));
        assertTrue(core.getInitiativeOrder(new int[] {6, 3}, 0, 3, false) >= 1);
        assertTrue(total.getInitiativeOrder(new int[] {6, 3}, 0, 3, true) >= 1);
        assertTrue(core.isWalkOnDeployment());
        total.setWalkOnDeployment(true);
        assertTrue(total.isWalkOnDeployment());
        assertTrue(core.canWalkOnThisRound(entity));
        assertTrue(total.canWalkOnThisRound(entity));
        assertTrue(core.includeInMovement(GamePhase.MOVEMENT, entity));
        assertTrue(total.includeInMovement(GamePhase.MOVEMENT, entity));
        assertEquals(1, core.getDeploymentWidth(player, Board.START_EDGE, 8));
        assertEquals(1, total.getDeploymentWidth(player, Board.START_EDGE, 8));
    }
}
