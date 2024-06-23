package megamek.server.sbf;

import megamek.common.strategicBattleSystems.SBFGame;

import java.util.HashMap;
import java.util.Map;

import static megamek.server.sbf.SBFVisibilityStatus.*;

public final class SBFVisibilityHelper {

    /**
     * This map holds the visibility status. It maps a player ID to an inner map having pairs of formation ID and
     * visibility status. The default value if no entry can be found is INVISIBLE.
     */
    private final Map<Integer, Map<Integer, SBFVisibilityStatus>> visibilityMap = new HashMap<>();

    //TODO: Team vision shares info among players. Must find the best visibility of all players on a team

    public void setVisibility(int viewingPlayer, int formationID, SBFVisibilityStatus visibilityStatus) {
        if (visibilityStatus == INVISIBLE) {
            setInvisible(viewingPlayer, formationID);
        } else {
            Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.computeIfAbsent(viewingPlayer, k -> new HashMap<>());
            playerMap.put(formationID, visibilityStatus);
        }
    }

    public SBFVisibilityStatus getVisibility(int viewingPlayer, int formationID) {
        Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.get(viewingPlayer);
        if (playerMap == null) {
            return INVISIBLE;
        } else {
            return playerMap.getOrDefault(formationID, INVISIBLE);
        }
    }

    public boolean isVisible(int viewingPlayer, int formationID) {
        return getVisibility(viewingPlayer, formationID) == VISIBLE;
    }

    public void setVisible(int viewingPlayer, int formationID) {
        setVisibility(viewingPlayer, formationID, VISIBLE);
    }

    public void setInvisible(int viewingPlayer, int formationID) {
        Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.get(viewingPlayer);
        if (playerMap != null) {
            playerMap.remove(formationID);
        }
    }
}
