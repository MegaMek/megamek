/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Entity;
import megamek.common.Report;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;

/**
 * Describes a set of methods a class can use to represent an attack from some weapon.
 *         
 * @author Andrew Hunter
 * @since May 10, 2004
 */
public interface AttackHandler {

    // Does it care?
    public boolean cares(GamePhase phase);

    // If it cares, call this. If it needs to remain in queue, returns true,
    // else false.
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReports);

    // Frankly, wish I could get rid of this, but I think certain things
    // occasionally need to know the firer.
    public int getAttackerId();
    
    public Entity getAttacker();

    public boolean announcedEntityFiring();

    public void setAnnouncedEntityFiring(boolean announcedEntityFiring);

    public WeaponAttackAction getWaa();
    
    /**
     * Used to determine if the AttackHandler is handling a strafing run.
     * 
     * @return
     */
    public boolean isStrafing();
    
    public void setStrafing(boolean isStrafing);
    
    /**
     * Used to determine if this is the firt time a weapon is firing as part of
     * a strafing run.  This is used for handling heat, to prevent shots after
     * the first one from generating heat.
     * 
     * @return
     */
    public boolean isStrafingFirstShot();
    
    public void setStrafingFirstShot(boolean isFirstShotStrafing);

}
