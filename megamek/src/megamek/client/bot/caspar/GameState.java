package megamek.client.bot.caspar;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Hex;

import java.util.List;

public interface GameState {

    List<Entity> getEnemyUnits();

    List<Coords> getUnexploredAreas();

    List<Entity> getFriendlyUnits();

    List<Hex> getStrategicPoints();
}
