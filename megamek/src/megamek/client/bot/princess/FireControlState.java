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
    
    public FireControlState() {
        additionalTargets = new ArrayList<>();
        entityIDFStates = new HashMap<>();
        orderedFiringEntities = new LinkedList<>();
        weaponRanges = new HashMap<>();
        airborneTargetWeaponRanges = new HashMap<>();
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
    
    public void clearTransientData() {
    	clearEntityIDFStates();
    	clearOrderedFiringEntities();
    	weaponRanges.clear();
    	airborneTargetWeaponRanges.clear();
    }
}
