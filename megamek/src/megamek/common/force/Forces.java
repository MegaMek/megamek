/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */ 
package megamek.common.force;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import megamek.MegaMek;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.annotations.Nullable;

import static megamek.common.force.Force.*;
import static java.util.stream.Collectors.*;

/**
 * Manages a collection of Forces for a game. The game only needs to hold one Forces object.
 * Like in Campaign.java in MHQ this is mainly a map of id -> Force along with many utility functions.
 * Force management and changes are directed through this object. 
 * 
 * @author Simon
 */
public final class Forces implements Serializable {

    private static final long serialVersionUID = -1382468145554363945L;
    
    private HashMap<Integer, Force> forces = new HashMap<>();
    private transient IGame game;
    
    public Forces(IGame g) {
        game = g;
    }
    
    /** 
     * Adds a top-level force with the provided name and the provided owner. Verifies the name
     * and the force before applying the change.
     * Returns the id of the newly created Force or Force.NO_FORCE if no force was 
     * created.  
     */
    public synchronized int addTopLevelForce(String forceName, IPlayer owner) {
        if (!verifyForceName(forceName) || owner == null) {
            return NO_FORCE;
        }

        int newId = newId();
        Force newForce = new Force(forceName, newId, owner);
        forces.put(newId, newForce);
        return newId;
    }
    
    /** 
     * Adds a subforce with the provided name to the provided parent. Verifies the name
     * and the force before applying the change.
     * Returns the id of the newly created Force or Force.NO_FORCE if no new subforce was 
     * created.  
     */
    public synchronized int addSubForce(String forceName, Force parent) {
        if (!contains(parent) || !verifyForceName(forceName)) {
            return NO_FORCE;
        }
        
        // Create and add the new force
        int newId = newId();
        Force newForce = new Force(forceName, newId, parent);
        forces.put(newId, newForce);
        parent.addSubForce(newForce);
        return newId;
    }
    
    /** Returns a list of all the game's entities that are not part of any force. */
    public List<Entity> forcelessEntities() {
        return game.getEntitiesStream().filter(e -> !e.partOfForce()).collect(toList());
    }
    
    /** Returns the number of top-level forces present, i.e. forces with no parent force. */
    public int getTopLevelForceCount() {
        return getTopLevelForces().size();
    }
    
    /** Returns a List of the top-level forces. */
    public List<Force> getTopLevelForces() {
        return forces.values().stream().filter(f -> f.isTopLevel()).collect(toList());
    }
    
    /** 
     * Returns true if the provided force is part of the present forces either
     * as a top-level or a subforce. 
     */
    public boolean contains(Force force) {
        return (force != null) && forces.containsValue(force);
    }
    
    /** 
     * Returns true if the provided forceId is part of the present forces either
     * as a top-level or a subforce. 
     */
    public boolean contains(int forceId) {
        return forces.containsKey(forceId);
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
    
    
    /** 
     * Adds the provided Entity to the provided force. Does nothing if the force doesn't exist
     * or if the entity is already in the targeted force. Removes the entity from any former force.
     * Returns a list of all changed forces, i.e. the former force, if any, and the new force.
     * The list will be empty if no actual change occurred. 
     */
    public ArrayList<Force> addEntity(Entity entity, int forceId) {
        ArrayList<Force> result = new ArrayList<>();
        if (!forces.containsKey(forceId)) {
            MegaMek.getLogger().error("Tried to add entity to non-existing force");
            return result;
        }
        int formerForce = getForceId(entity);
        if (formerForce == forceId) {
            return result;
        }
        
        forces.get(forceId).addEntity(entity);
        entity.setForceId(forceId);
        result.add(getForce(forceId));
        if (formerForce != NO_FORCE) {
            forces.get(formerForce).removeEntity(entity);
            result.add(getForce(formerForce));
        }
        return result;
    }

    /** 
     * Removes the provided entities from their current forces, if any. Does nothing if an entity
     * is already force-less (forceId == Force.NO_FORCE). ÅReturns a list of all changed forces.
     * The list will be empty if no actual change occurred. 
     */
    public synchronized LinkedHashSet<Force> removeEntityFromForces(Collection<Entity> entities) {
        LinkedHashSet<Force> result = new LinkedHashSet<>();
        for (Entity entity: entities) {
            result.addAll(removeEntityFromForces(entity));
        }
        return result;
    }

    
    /** 
     * Removes the provided Entity from its current force, if any. Does nothing if the entity
     * is already force-less (forceId == Force.NO_FORCE). 
     * Returns a list of all changed forces, i.e. the former force, if any.
     * The list will be empty if no actual change occurred. 
     */
    public synchronized ArrayList<Force> removeEntityFromForces(Entity entity) {
        ArrayList<Force> result = new ArrayList<>();
        int formerForce = getForceId(entity);
        if (formerForce == NO_FORCE) {
            return result;
        }
        entity.setForceId(NO_FORCE);
        
        if (contains(formerForce)) {
            result.add(getForce(formerForce));
            getForce(formerForce).removeEntity(entity);
        } else {
            MegaMek.getLogger().warning("Removed entity from non-existent force!");
        }
        return result;
    }
    
    /** 
     * Removes the provided entity ID from its current force, if any. Does nothing if the entity
     * is already force-less (forceId == Force.NO_FORCE). 
     * Returns a list of all changed forces, i.e. the former force, if any.
     * The list will be empty if no actual change occurred. 
     */
    public synchronized ArrayList<Force> removeEntityFromForces(int entityId) {
        ArrayList<Force> result = new ArrayList<>();
        int formerForce = getForceId(entityId);
        if (formerForce == NO_FORCE) {
            return result;
        }
        if (game.getEntity(entityId) != null) {
            game.getEntity(entityId).setForceId(NO_FORCE);
        }
        
        if (contains(formerForce)) {
            result.add(getForce(formerForce));
            getForce(formerForce).removeEntity(entityId);
        } else {
            MegaMek.getLogger().warning("Removed entity from non-existent force!");
        }
        return result;
    }
    
    /** 
     * Renames the Force with forceId to the provided name. The provided values are
     * fully verified before applying the change. A null name or empty name can safely
     * be passed. Duplicate names may be given; forces are identified via id.
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
    
    /** 
     * Returns the owner Id of the owner of this force. 
     */
    public int getOwnerId(Force force) {
        return force.getOwnerId();
    }
    
    /** 
     * Returns the owner of this force. 
     */
    public IPlayer getOwner(Force force) {
        return game.getPlayer(getOwnerId(force));
    }
    
    /** 
     * Returns the owner of this force. 
     */
    public IPlayer getOwner(int forceId) {
        return getOwner(getForce(forceId));
    }
    
    /** 
     * Returns the Force that the provided entity is a direct part of.
     * E.g., If it is part of a lance in a company, the lance will be returned.
     * If it is part of no force, returns null. 
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
        return getForceId(entity.getId());
    }
    
    /** 
     * Returns the id of the force that the provided entity (id) is a direct part of.
     * E.g., If it is part of a lance in a company, the lance id will be returned.
     * If it is part of no force, returns Force.NO_FORCE. 
     */
    public int getForceId(int id) {
        for (Force force: forces.values()) {
            if (force.containsEntity(id)) {
                return force.getId();
            }
        }
        return Force.NO_FORCE;
    }
    
    /**
     * Parses the force string of the provided entity. 
     * Returns a List of Force stubs in the order of highest to lowest force.
     * The Force stubs cannot be added to a Forces object directly! They 
     * contain only the name and id and have no parent and no owner and 
     * the passed entity is not added to them!
     */
    public static ArrayList<Force> parseForceString(Entity entity) {
        ArrayList<Force> result = new ArrayList<>();
        String forceString = entity.getForceString();
        StringTokenizer st = new StringTokenizer(forceString, "|\\");
        
        while (st.hasMoreTokens()) {
            String forceName = st.nextToken();
            String forceId = st.nextToken();
            int id = Integer.parseInt(forceId);
            result.add(new Force(forceName, id));
        }
        return result;
    }
    
    /** 
     * Returns a list of all subforces of the provided force, including
     * subforces of subforces to any depth. 
     */
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
    
    /** 
     * For the given player, returns a list of forces that are his own or belong to his team. 
     */
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
    
    private boolean isAvailable(Force force, IPlayer player) {
        IPlayer owner = game.getPlayer(getOwnerId(force));
        return (owner != null) && !owner.isEnemyOf(player);
    }
    
    public String forceStringFor(Entity entity) {
        List<Force> ancestors = forceChain(entity);
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
    
    /** 
     * Returns a ArrayList of Forces that make up the chain of forces to the provided entity.
     * The list starts with the top-level force containing the entity and ends with 
     * the force that the entity is an immediate member of.
     */
    public ArrayList<Force> forceChain(Entity entity) {
        if (getForce(entity) != null) {
            return forceChain(getForce(entity));
        } else {
            return new ArrayList<>();
        }
    }
    
    /** 
     * Returns a ArrayList of Forces that make up the chain of forces to the provided force.
     * The list starts with the top-level force containing the provided force and ends with 
     * (includes!) the provided force itself.
     */
    public ArrayList<Force> forceChain(Force force) {
        ArrayList<Force> result = new ArrayList<>();
        if (!force.isTopLevel()) {
            result.addAll(forceChain(forces.get(force.getParentId())));
        }
        result.add(force);
        return result;
    }
    
    /** Return a free Force id. */
    private int newId() {
        int result = 0;
        if (!forces.isEmpty()) {
            result = forces.keySet().stream().mapToInt(i -> i).max().getAsInt() + 1;
        }
        return result;
    }
    
    /** 
     * Overwrites the previous force mapped to forceId with the provided force
     * or adds it if no force was present with that forceId. 
     * Only used when the server sends force updates. 
     */
    public void replace(int forceId, Force force) {
        forces.put(forceId, force);
    }
    
    /** 
     * Sets the game reference to the provided IGame. Used when transferring
     * the forces between client and server.
     */
    public void setGame(IGame g) {
        game = g;
    }
    
    /** Returns a clone of this Forces object, including clones of all contained forces. */
    public Forces clone() {
        Forces clone = new Forces(game);
        for (Entry<Integer, Force> entry: forces.entrySet()) {
            clone.forces.put(entry.getKey(), entry.getValue().clone());
        }
        return clone;
    }

    /** 
     * Returns true if this Forces object is valid. 
     * @see #isValid(Collection)
     */

    public boolean isValid() {
        return isValid(new ArrayList<>());
    }
    
    /** 
     * Returns true if this Forces object is valid. If any updatedEntities are given,
     * the validity check will test these instead of the current game's.
     * @see #isValid()
     */
    public boolean isValid(Collection<Entity> updatedEntities) {
        Set<Integer> entIds = new TreeSet<>();
        Set<Integer> subIds = new TreeSet<>();
        for (Entry<Integer, Force> entry: forces.entrySet()) {
            // check if map ids == force ids
            if (entry.getKey() != entry.getValue().getId()) {
                return false;
            }
            
            // Create a copy of the game's entity list and overwrite with the given entities
            LinkedHashMap<Integer, Entity> allEntities = new LinkedHashMap<>();
            for (Entity entity: game.getEntitiesVector()) {
                allEntities.put(entity.getId(), entity);
            }
            for (Entity entity: updatedEntities) {
                allEntities.put(entity.getId(), entity);
            }
            
            // check if all entities exist/live
            // check if owner exists
            // check if entities match owners/team
            // check if no entity is contained twice
            // check if entity.forceId matches forceId
            for (int entityId: entry.getValue().getEntities()) {
                if (!allEntities.containsKey(entityId) 
                        || game.getPlayer(getOwnerId(entry.getValue())) == null
                        || game.getPlayer(allEntities.get(entityId).getOwnerId()).isEnemyOf(game.getPlayer(getOwnerId(entry.getValue())))
                        || !entIds.add(entityId)
                        || allEntities.get(entityId).getForceId() != entry.getKey()) {
                    return false;
                }
            }
            // check if subforces exist
            // check if no subforce is contained twice
            // check if subforces agree on the parent
            // check if subforces and parents share teams
            for (int subforceId: entry.getValue().getSubForces()) {
                if (!contains(subforceId) 
                        || !subIds.add(subforceId)
                        || entry.getKey() != getForce(subforceId).getParentId()
                        || getOwner(getForce(subforceId)).isEnemyOf(getOwner(entry.getValue()))) {
                    return false;
                }
            }
        }
        // check if no circular parents 
        Set<Integer> forceIds = new TreeSet<>(forces.keySet());
        for (Force toplevel: getTopLevelForces()) {
            for (Force subforce: getFullSubForces(toplevel)) {
                forceIds.remove(subforce.getId());
            }
            forceIds.remove(toplevel.getId());
        }
        if (!forceIds.isEmpty()) {
            return false;
        }
        return true;
    }
    
    /** 
     * Corrects this Forces object as much as possible. Also corrects entities
     * when necessary (wrong forceId).
     * <LI>Incorrect links (false IDs, nonexistent or dual entities or forces)
     * <LI>Enemy force/entity connections
     * <LI>Incorrect links
     * 
     * @see #isValid()
     */
    public void correct() {
        Set<Integer> entIds = new TreeSet<>();
        Set<Integer> subIds = new TreeSet<>();
        for (Entry<Integer, Force> entry: forces.entrySet()) {
            // master list id must be equal to force id
            entry.getValue().setId(entry.getKey());

            // Create a copy of the game's entity list
            HashMap<Integer, Entity> allEntities = new HashMap<>();
            for (Entity entity: game.getEntitiesVector()) {
                allEntities.put(entity.getId(), entity);
            }

            var entityIds = new ArrayList<Integer>(entry.getValue().getEntities());
            for (int entityId: entityIds) {
                // Remove non-existent/dead entities
                if (!allEntities.containsKey(entityId)) {
                    entry.getValue().removeEntity(entityId);
                }
                // Remove dual entity entries
                if (!entIds.add(entityId)) {
                    entry.getValue().removeEntity(entityId);
                } else {
                    // Entity forceID must match force entry
                    game.getEntity(entityId).setForceId(entry.getKey());
                }
                // Remove entities from enemy forces
                IPlayer enOwner = game.getPlayer(allEntities.get(entityId).getOwnerId());
                IPlayer foOwner = game.getPlayer(getOwnerId(entry.getValue()));
                if (enOwner != null && enOwner.isEnemyOf(foOwner)) {
                    removeEntityFromForces(game.getEntity(entityId));
                }
            }

            var subForceIds = new ArrayList<Integer>(entry.getValue().getSubForces());
            for (int subforceId: subForceIds) {
                // Remove nonexistent subforces
                if (!contains(subforceId)) {
                    entry.getValue().removeSubForce(subforceId);
                }
                // Remove dual subforce entries
                if (!subIds.add(subforceId)) {
                    entry.getValue().removeSubForce(subforceId);
                }
                // Correct parentID (the subforce's parent entry must be equal to the current force
                getForce(subforceId).setParent(entry.getKey());
                // Remove subforces from enemy forces
                IPlayer subFoOwner = game.getPlayer(getOwnerId(getForce(subforceId)));
                IPlayer foOwner = game.getPlayer(getOwnerId(entry.getValue()));
                if (subFoOwner != null && subFoOwner.isEnemyOf(foOwner)) {
                    promoteForce(getForce(subforceId));
                }
            }
        }
    }
    
    /** 
     * Removes the given force from these forces if it is empty. Returns a list
     * of affected forces which contains the parent if the deleted force was a subforce
     * and is empty otherwise.
     */
    public ArrayList<Force> deleteForce(int forceId) {
        ArrayList<Force> result = new ArrayList<>();
        if (contains(forceId) && getForce(forceId).getChildCount() == 0) {
            Force force = getForce(forceId);
            if (!force.isTopLevel()) {
                Force parent = getForce(force.getParentId());
                parent.removeSubForce(forceId);
                result.add(parent);
            }
            forces.remove((Integer)forceId);
        }
        return result;
    }
    
    /** 
     * Removes the given forces and all their subforces from these Forces. Returns a list
     * of affected surviving forces. This method does not check if the forces are empty.
     * <P>NOTE: Any entities in the removed forces are NOT updated by this method!
     * It is necessary to update any entities' forceId unless these are deleted as well. 
     */
    public ArrayList<Force> deleteForces(Collection<Force> delForces) {
        ArrayList<Force> result = new ArrayList<>();
        Set<Force> allForces = new HashSet<>(delForces);
        delForces.stream().map(this::getFullSubForces).forEach(allForces::addAll);
        // Remember the IDs to prevent updates to already-deleted parents
        Set<Integer> allForceIds = allForces.stream().map(Force::getId).collect(toSet());
        for (Force force: allForces) {
            if (contains(force)) {
                if (!force.isTopLevel() && !allForceIds.contains(force.getParentId())) {
                    Force parent = getForce(force.getParentId());
                    parent.removeSubForce(force.getId());
                    result.add(parent);
                }
                forces.remove((Integer)force.getId());
            }
        }
        return result;
    }

    /** Returns a list of all forces and subforces in no particular order. */
    public ArrayList<Force> getAllForces() {
        return new ArrayList<>(forces.values());
    }
    
    /** 
     * Attaches a force to a new parent. The new parent force cannot be a subforce
     * of the force and cannot belong to an enemy of its owner.
     * Returns a list of affected forces. This may be empty and may contain up to
     * three forces (the force, the new parent and the former parent).
     */
    public ArrayList<Force> attachForce(Force force, Force newParent) {
        ArrayList<Force> result = new ArrayList<>();
        IPlayer forceOwner = game.getPlayer(getOwnerId(force));
        IPlayer parentOwner = game.getPlayer(getOwnerId(newParent));
        if (isSubForce(force, newParent) || forceOwner == null 
                || forceOwner.isEnemyOf(parentOwner) || force.getParentId() == newParent.getId()) {
            return result;
        }
        if (!force.isTopLevel()) {
            // Remove from the former parent
            Force oldParent = getForce(force.getParentId());
            oldParent.removeSubForce(force.getId());
            result.add(oldParent);
        }
        // Set to its new parent
        force.setParent(newParent.getId());
        result.add(force);
        newParent.addSubForce(force);
        result.add(newParent);
        return result;
    }
    
    /** Returns true when possibleSubForce is one of the subforces (in any depth) of the given force. */
    public boolean isSubForce(Force force, Force possibleSubForce) {
        return getFullSubForces(force).contains(possibleSubForce);
    }
    
    /** 
     * Promotes a force to top-level (unattaches it from its parent force if it has one). 
     * Returns a list of affected forces which may be empty. 
     */
    public ArrayList<Force> promoteForce(Force force) {
        ArrayList<Force> result = new ArrayList<>();
        if (!force.isTopLevel()) {
            // Remove from the former parent
            Force oldParent = getForce(force.getParentId());
            oldParent.removeSubForce(force.getId());
            result.add(oldParent);
            // Set to top-level
            force.setParent(Force.NO_FORCE);
            result.add(force);
        }
        return result;
    }
    
    /** 
     * Changes the owner of the given force to the given newOwner without affecting
     * anything else. Will only do something if the new owner is a teammate of the 
     * present owner. Returns a list of affected forces containing the force if it 
     * was changed, empty otherwise.
     */
    public ArrayList<Force> assignForceOnly(Force force, IPlayer newOwner) {
        ArrayList<Force> result = new ArrayList<>();
        if (getOwner(force).isEnemyOf(newOwner)) {
            MegaMek.getLogger().error("Tried to reassign a force without units to an enemy.");
            return result; 
        }
        if (getOwnerId(force) != newOwner.getId()) {
            force.setOwnerId(newOwner.getId());
            result.add(force);
        }
        return result;
    }
   
    /** 
     * Changes the owner of the given force and all subforces to the given newOwner. 
     * Promotes the force to top-level if the parent force is now an enemy force.  
     * Returns a list of affected forces.
     */
    public Set<Force> assignFullForces(Force force, IPlayer newOwner) {
        Set<Force> result = new HashSet<>();

        // gather up this whole force tree
        ArrayList<Force> affected = getFullSubForces(force);
        affected.add(force);
        if (!getOwner(force).isEnemyOf(newOwner)) {
            // If the force is a teammate of the new owner, all subforces are as well
            // and they can be assigned with the simpler method, no dislodging necessary
            for (Force f: affected) {
                result.addAll(assignForceOnly(f, newOwner));
            }
        } else {
            // If the new owner is an enemy, dislodge the uppermost force
            // The other forces must all be set to the new owner as well
            result.addAll(promoteForce(force));
            result.add(force);
            for (Force f: affected) {
                if (getOwnerId(f) != newOwner.getId()) {
                    f.setOwnerId(newOwner.getId());
                    result.add(f);
                }
            }
        }
        return result; 
    }
    
    @Override
    public String toString() {
        List<String> forceStrings = forces.values().stream().map(Force::toString).collect(toList());
        return String.join("\n", forceStrings);
    }
    
    /** 
     * Returns a list of all entities of the given force and all its subforces to any depth. 
     */
    public List<Entity> getFullEntities(final @Nullable Force force) {
        if (force == null) {
            return new ArrayList<>();
        }

        final List<Entity> result = new ArrayList<>();
        if (contains(force)) {
            for (int entityId : force.getEntities()) {
                final Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    result.add(entity);
                }
            }
            for (int subForceId : force.getSubForces()) {
                result.addAll(getFullEntities(forces.get(subForceId)));
            }
        }
        return result;
    }
    
    /** 
     * Returns a list of the direct subordinate entities of the given force.
     * Entities in subforces of this force are ignored. 
     */
    public ArrayList<Entity> getDirectEntities(Force force) {
        ArrayList<Entity> result = new ArrayList<>();
        if (contains(force)) {
            for (int entityId: force.getEntities()) {
                result.add(game.getEntity(entityId));
            }
        }
        return result;
    }
    
    /** 
     * Moves up the given entity in the list of entities of its force if possible.
     * Returns true when an actual change occurred. 
     */
    public ArrayList<Force> moveUp(Entity entity) {
        ArrayList<Force> result = new ArrayList<>();
        Force force = getForce(entity);
        if (force != null) {
            if (force.moveUp(entity.getId())) {
                result.add(force);
            }
        }
        return result;
    }
    
    /** 
     * Moves down the given entity in the list of entities of its force if possible.
     * Returns true when an actual change occurred. 
     */
    public ArrayList<Force> moveDown(Entity entity) {
        ArrayList<Force> result = new ArrayList<>();
        Force force = getForce(entity);
        if (force != null) {
            if (force.moveDown(entity.getId())) {
                result.add(force);
            }
        }
        return result;
    }
    
    /** 
     * Moves up the given subforce in the list of subforces of its parent if possible.
     * Returns true when an actual change occurred. 
     */
    public ArrayList<Force> moveUp(Force subForce) {
        ArrayList<Force> result = new ArrayList<>();
        if (contains(subForce) && !subForce.isTopLevel()) {
            Force parent = getForce(subForce.getParentId());
            if (parent.moveUp(subForce)) {
                result.add(parent);
            }
        }
        return result;
    }
    
    /** 
     * Moves down the given subforce in the list of subforces of its parent if possible.
     * Returns true when an actual change occurred. 
     */
    public ArrayList<Force> moveDown(Force subForce) {
        ArrayList<Force> result = new ArrayList<>();
        if (contains(subForce) && !subForce.isTopLevel()) {
            Force parent = getForce(subForce.getParentId());
            if (parent.moveDown(subForce)) {
                result.add(parent);
            }
        }
        return result;
    }
    
}
