package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.Targetable;

/**
 * This class is a data structure meant to hold Fire Control related
 * state, to keep the FireControl class relatively stateless.
 */
public class FireControlState {
    private List<Targetable> additionalTargets;
    private Map<Integer, Boolean> entityIDFStates;
    private LinkedList<Entity> orderedFiringEntities;
    private Map<Integer, Integer> weaponRanges;
    private Map<Integer, Integer> airborneTargetWeaponRanges;
    private Map<Integer, Boolean> isCommander;
    private Map<Integer, Boolean> isSubCommander;
    
    public FireControlState() {
        additionalTargets = new ArrayList<>();
        entityIDFStates = new HashMap<>();
        orderedFiringEntities = new LinkedList<>();
        weaponRanges = new HashMap<>();
        airborneTargetWeaponRanges = new HashMap<>();
        isCommander = new HashMap<>();
        isSubCommander = new HashMap<>();
    }
    
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
    
    public void clearEntityIDFStates() {
        entityIDFStates.clear();
    }
    
    /**
     * Accessor for the data structure containing a mapping between entities and whether or not
     * they have indirect fire capability as in LRMs.
     */
    public Map<Integer, Boolean> getEntityIDFStates() {
        return entityIDFStates;
    }
    
    public LinkedList<Entity> getOrderedFiringEntities() {
        return orderedFiringEntities;
    }
    
    public void clearOrderedFiringEntities() {
        this.orderedFiringEntities.clear();
    }
    
    public Map<Integer, Integer> getWeaponRanges(boolean airborneTarget) {
        return airborneTarget ? airborneTargetWeaponRanges : weaponRanges;
    }
    
    public boolean subCommanderCached(Entity entity) {
        return isSubCommander.containsKey(entity.getId());
    }
    
    public boolean commanderCached(Entity entity) {
        return isCommander.containsKey(entity.getId());
    }
    
    public boolean isSubCommander(Entity entity) {
        return isSubCommander.get(entity.getId());
    }
    
    public boolean isCommander(Entity entity) {
        return isCommander.get(entity.getId());
    }
    
    public void setSubCommander(Entity entity, boolean value) {
        isSubCommander.put(entity.getId(), value);                
    }
    
    public void setCommander(Entity entity, boolean value) {
        isCommander.put(entity.getId(), value);                
    }
    
    /**
     * Clears data that shouldn't persist phase-to-phase
     */
    public void clearTransientData() {
        clearEntityIDFStates();
        clearOrderedFiringEntities();
        weaponRanges.clear();
        airborneTargetWeaponRanges.clear();
        isCommander.clear();
        isSubCommander.clear();
    }
}
