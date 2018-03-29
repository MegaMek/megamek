package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;

public interface IPathRanker {

    ArrayList<RankedPath> rankPaths(List<MovePath> movePaths, IGame game, int maxRange, double fallTolerance,
            int startingHomeDistance, List<Entity> enemies, List<Entity> friends);

    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    void initUnitTurn(Entity unit, IGame game);

}