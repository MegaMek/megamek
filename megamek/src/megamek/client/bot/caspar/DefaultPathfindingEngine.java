package megamek.client.bot.caspar;

import megamek.client.bot.Agent;
import megamek.client.bot.princess.PathEnumerator;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.List;

/**
 * Default implementation that uses the Princess PathEnumerator.
 */
public class DefaultPathfindingEngine implements PathfindingEngine {
    private final PathEnumerator pathEnumerator;

    public DefaultPathfindingEngine(Agent agent, Game game, Board board) {
        this.pathEnumerator = new PathEnumerator(agent, game, board);
    }

    @Override
    public List<MovePath> generatePaths(Entity unit, Game game, int depth) {
        // Use Princess's PathEnumerator to generate paths
        return pathEnumerator.getUnitPaths(unit, game, depth);
    }
}
