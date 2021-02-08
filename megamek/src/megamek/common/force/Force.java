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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import megamek.common.Entity;

/**
 * Helper functions for the current (simple) implementation of forces that is 
 * only String-based instead of Force and Lance of MekHQ.
 * @author Simon
 */
public final class Force {
    
    /** A top levevl force object to hold all forces of all teams and force-less entities. */
    public static final String TOPLEVEL_FORCE = "TopLevel";
    public static final int NO_FORCE = -1;
    
    private String name;
    private int id;

    private int parent = NO_FORCE;
    private ArrayList<Integer> entities = new ArrayList<>();
    private ArrayList<Integer> subForces = new ArrayList<>();

    /** Creates a top-level force, i.e. one with no parent force. */
    public Force(String n) {
        Objects.requireNonNull(n);
        name = n;
    }

    public Force(String n, int nId, int fParent) {
        this(n);
        id = nId;
        parent = fParent;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }
    
    public int getId() {
        return id;
    }
    
    
    public int getParent() {
        return parent;
    }
    
    public boolean isTopLevel() {
        return parent == NO_FORCE;
    }
    
    public int subForceCount() {
        return subForces.size();
    }
    
    public int entityCount() {
        return entities.size();
    }
    
    public int getChildCount() {
        return entities.size() + subForces.size();
    }
    
    /** 
     * Returns the id of the entity at the provided index from the list of
     * direct members of this force (not subforces). Indices outside of
     * 0 ... entityCount() - 1 will result in an exception.
     */
    public int getEntityId(int index) {
        return entities.get(index);
    }
    
    /** 
     * Returns the id of the force at the provided index from the list of
     * subforces of this force. Indices outside of 0 ... subForceCount() - 1 
     * will result in an exception.
     */
    public int getSubForceId(int index) {
        return subForces.get(index);
    }
    
    /** 
     * Returns true if the provided entity is among the force's direct members. 
     * Does NOT check if the entity is part of any subforce. 
     */
    public boolean containsEntity(Entity entity) {
        return entities.contains(entity.getId());
    }
    
    /** 
     * Returns the index of the provided entity in the list of direct members of this force.
     * Returns -1 if the entity is no direct member of this force.  
     */
    public int entityIndex(Entity entity) {
        return entities.indexOf(entity.getId());
    }
    
    /** 
     * Returns true if the provided force is among this force's direct subforces. 
     */
    public boolean containsSubForce(Force force) {
        return subForces.contains(force.getId());
    }
    
    /** 
     * Returns the index of the provided force in the list of direct subforces of this force.
     * Returns -1 if the force is no direct subforce of this force.  
     */
    public int subForceIndex(Force force) {
        return subForces.indexOf(force.getId());
    }
    
    void addSubForce(Force subForce) {
        subForces.add(subForce.getId());
    }
    
    List<Integer> getEntities() {
        return Collections.unmodifiableList(entities);
    }
    
    List<Integer> getSubForces() {
        return Collections.unmodifiableList(subForces);
    }
    
    void addEntity(Entity entity) {
        entities.add(entity.getId());
    }
    
    void removeEntity(Entity entity) {
        entities.remove((Integer)entity.getId());
    }
}