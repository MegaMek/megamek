/*  
* MegaMek - Copyright (C) 2021 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.common.force;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import megamek.MegaMek;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;

public final class Forces implements Serializable {

    private static final long serialVersionUID = -1382468145554363945L;
    
    private HashMap<Integer, Force> forces = new HashMap<Integer, Force>();
    private IGame game;
    
    public Forces(IGame g) {
        game = g;
    }
    
    public int addForce(String forceName) {
        return addForce(forceName, Force.NO_FORCE);
    }
    
    public int addForce(String forceName, Force parent) {
        return addForce(forceName, parent.getId());
    }
    
    public int addForce(String forceName, int parentID) {
        // If the new force is not top-level, the parent must exist
        if (parentID != Force.NO_FORCE && !forces.containsKey(parentID)) {
            return Force.NO_FORCE;
        }
        int newId = 0;
        
        // Find a unique id
        if (!forces.isEmpty()) {
            newId = forces.keySet().stream().mapToInt(i -> i).max().getAsInt() + 1;
        }
        
        // Create and add the new force
        Force newForce = new Force(forceName, newId, parentID);
        forces.put(newId, newForce);
        
        // If the new force is not top-level, add it to the parent
        if (parentID != Force.NO_FORCE) {
            Force parent = forces.get(parentID);
            parent.addSubForce(newForce);
        }
        
        return newId;
    }
    
    public List<Entity> forcelessEntities() {
        return game.getEntitiesStream().filter(e -> !e.partOfForce()).collect(Collectors.toList());
    }
    
    /** Returns the number of top-level forces present, i.e. forces with no parent force. */
    public int getTopLevelForceCount() {
        return getTopLevelForces().size();
    }
    
    /** Returns a List of the top-level forces. */
    public List<Force> getTopLevelForces() {
        return forces.values().stream().filter(f -> f.isTopLevel()).collect(Collectors.toList());
    }
    
    /** 
     * Returns true if the provided force is part of the present forces either
     * as a top-level or a subforce. 
     */
    public boolean contains(Force force) {
        return (force != null) && forces.values().contains(force);
    }
    
    /** 
     * Returns the force with the given force id or null if there is no force with this id. 
     */
    public Force getForce(int id) {
        return forces.get(id);
    }
    
    /** 
     * Returns the entity with the provided id. (This is a convenience method
     * for objects that access force info but don't have a reference to game or client.)
     */
    public Entity getEntity(int id) {
        return game.getEntity(id);
    }
    
    
//    public int directMemberCount(Force force) {
//        return (int) game.getEntitiesVector().stream().filter(e -> e.getForceId() == force.getId()).count();
//    }
    
    public void addEntity(Entity entity, int forceId) {
        if (!forces.containsKey(forceId)) {
            MegaMek.getLogger().error("Tried to add entity to non-existing force");
            return;
        }
        int formerForce = getForceId(entity);
        if (formerForce == forceId) {
            return;
        }
        
        forces.get(forceId).addEntity(entity);
        entity.setForceId(forceId);
        if (formerForce != Force.NO_FORCE) {
            forces.get(formerForce).removeEntity(entity);
        }
        // send client update in lobby
    }
    
    /** Adds the provided entity to no particular force. */
//    public void addEntity(Entity entity) {
//        if (!forces.containsKey(forceId)) {
//            MegaMek.getLogger().error("Tried to add entity to non-existing force");
//            return;
//        }
//        int formerForce = getForceId(entity);
//        forces.get(formerForce).remove(entity);
        // remove the entity from its current force, if different from forceId
        // add the id to its new force
        // send update if changed
//    }
    
    /** 
     * Renames the Force with forceId with the provided name. The provided values are
     * fully verified before applying the change. A null name or empty name can safely
     * be provided. Duplicate names may be given; forces are identified via id.
     */
    public void renameForce(String name, int forceId) {
        if (!forces.containsKey(forceId) || !verifyForceName(name)) {
            return;
        }
        Force force = forces.get(forceId);
        force.setName(name);
    }
    
    /** 
     * Returns true if the provided name can be used as a Force name. It cannot
     * be empty or contain "|" or "\".
     */
    public boolean verifyForceName(String name) {
        return name != null && name.trim().length() > 0 && !name.contains("|") && !name.contains("\\");
    }
    
    public int addSubForce(String name, Force parent) {
        if (!forces.containsValue(parent) || !verifyForceName(name)) {
            return Force.NO_FORCE;
        }
        int newId = forces.keySet().stream().mapToInt(i -> i).max().getAsInt() + 1;
        Force newForce = new Force(name, newId, parent.getId());
        forces.put(newId, newForce);
        return newId;
    }
//    
    /** 
     * Returns the Force that the provided entity is a direct part of.
     * E.g., If it is part of a lance in a company, the lance will be returned.
     * If it is part of no force, returns Force.TOPLEVEL_FORCE. 
     */
    public Force getForce(Entity entity) {
        return forces.get(getForceId(entity.getId()));
    }
    
    /** 
     * Returns the id of the force that the provided entity is a direct part of.
     * E.g., If it is part of a lance in a company, the lance id will be returned.
     * If it is part of no force, returns Force.NO_FORCE. 
     */
    public int getForceId(Entity entity) {
        for (Force force: forces.values()) {
            if (force.containsEntity(entity)) {
                return force.getId();
            }
        }
        return Force.NO_FORCE;
    }
    
    /** 
     * Returns the id of the force that the provided entity (id) is a direct part of.
     * E.g., If it is part of a lance in a company, the lance id will be returned.
     * If it is part of no force, returns Force.NO_FORCE. 
     */
    public int getForceId(int id) {
        return getForceId(game.getEntity(id));
    }
    
    /**
     * Parses the force string of the provided entity. Reconstructs forces contained
     * in it and adds the entity accordingly.
     */
    public boolean parseForce(Entity entity) {
        //TODO
        return true;
    }
    
    public ArrayList<Force> getFullSubForces(Force force) {
        ArrayList<Force> result = new ArrayList<>();
        if (contains(force)) {
            for (int subForceId: force.getSubForces()) {
                result.add(forces.get(subForceId));
                result.addAll(getFullSubForces(forces.get(subForceId)));
            }
        }
        return result;
    }
    
    public ArrayList<Force> getAvailableForces(IPlayer player) {
        ArrayList<Force> result = new ArrayList<>();
        for (Force force: getTopLevelForces()) {
            if (isAvailable(force, player)) {
                result.add(force);
                result.addAll(getFullSubForces(force));
            }
        }
        return result;
    }
    
    public boolean isAvailable(Force force, IPlayer player) {
        for (int entityId: force.getEntities()) {
            if (game.getEntity(entityId).getOwner().isEnemyOf(player)) {
                return false;
            }
        }
        for (int forceId: force.getSubForces()) {
            return isAvailable(forces.get(forceId), player);
        }
        return true;
    }
    
    public String forceStringFor(Entity entity) {
        List<Force> ancestors = ancestors(entity);
        String result = "";
        for (Force ancestor: ancestors) {
            result += "\\" + ancestor.getName() + "|" + ancestor.getId();
        }
        // Remove the backslash at the start
        if (result.length() > 0) {
            result = result.substring(1);
        }
        return result;
    }
    
    public LinkedList<Force> ancestors(Entity entity) {
        LinkedList<Force> result = new LinkedList<>();
        int id = getForceId(entity);
        int loopsafe = 0;
        while (id != Force.NO_FORCE) {
            result.add(0, forces.get(id));
            id = forces.get(id).getParent();
            if (loopsafe++ > 100) {
                MegaMek.getLogger().error("Force tree over 100 forces deep. Probably an error.");
                break;
            }
        }
        return result;
    }
}
