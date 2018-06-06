package megamek.client.bot.princess;

import java.util.List;

import megamek.common.Targetable;

/**
 * This class is a data structure meant to hold Fire Control related
 * state, to keep the FireControl class relatively stateless.
 */
public class FireControlState {
    private List<Targetable> additionalTargets;
    
    /**
     * The list of "additional targets", such as buildings, bridges and arbitrary hexes
     * that the bot will want to shoot
     * @return Additional target list.
     */
    public List<Targetable> getAdditionalTargets() {
        return additionalTargets;
    }
    
    /**
     * Directly sets the list of "additional targets" to a value.
     * @param value The new list of additional targets.
     */
    public void setAdditionalTargets(List<Targetable> value) {
        additionalTargets = value;
    }
}
