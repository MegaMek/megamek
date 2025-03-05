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

    public DefaultPathfindingEngine(Agent agent) {
        this.pathEnumerator = new PathEnumerator(agent);
    }

    @Override
    public List<MovePath> generatePaths(Entity unit, Game game, int depth) {
        // Use Princess's PathEnumerator to generate paths
        pathEnumerator.recalculateMovesFor(unit);
        return pathEnumerator.getUnitPaths().getOrDefault(unit.getId(), List.of());
    }
}
