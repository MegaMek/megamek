package megamek.server.totalwarfare.Target;

import java.util.Vector;
import megamek.common.*;

public interface TargetHandler {
    Vector<Report> handle(Targetable target, Game game, Entity attacker, int missiles, int attackerId);
}