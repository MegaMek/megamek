package megamek.server.resolver;

import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.server.EntityTargetPair;
import megamek.server.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolveWhatPlayerCanSeeUnits {
    /**
     * Called to what players can see what units. This is used to determine who
     * can see what in double blind reports.
     * @param server
     * @param game
     */
    public static void resolveWhatPlayersCanSeeWhatUnits(Server server, Game game) {
        List<ECMInfo> allECMInfo = null;
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
            allECMInfo = ComputeECM.computeAllEntitiesECMInfo(game
                    .getEntitiesVector());
        }
        Map<EntityTargetPair, LosEffects> losCache = new HashMap<>();
        for (Entity entity : game.getEntitiesVector()) {
            // We are hidden once again!
            entity.clearSeenBy();
            entity.clearDetectedBy();
            // Handle visual spotting
            for (Player p : server.whoCanSee(entity, false, losCache)) {
                entity.addBeenSeenBy(p);
            }
            // Handle detection by sensors
            for (Player p : server.whoCanDetect(entity, allECMInfo, losCache)) {
                    entity.addBeenDetectedBy(p);
            }
        }
    }
}
