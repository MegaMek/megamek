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

    /**
     * @param phase The current game phase
     *
     * @return True when this handler acts or might act in this phase. The handle() method of this handler is only
     * called when this method returns true for the present phase.
     */
    boolean cares(GamePhase phase);

    /**
     * This method is called to perform handling of its action or attack but only if cares() returns true in the
     * present game phase. This method must return true to keep this handler active for future phases. If it returns
     * false, the present handler will be discarded and no longer be called, ending any treatment of the associated
     * attack or action.
     *
     * @param phase The present game phase
     * @param vPhaseReports The reports list to add new reports to
     * @return True to keep this handler active, false to discard it
     */
    boolean handle(GamePhase phase, Vector<Report> vPhaseReports);

    // Frankly, wish I could get rid of this, but I think certain things
    // occasionally need to know the firer.
    int getAttackerId();

    Entity getAttacker();

    boolean announcedEntityFiring();

    void setAnnouncedEntityFiring(boolean announcedEntityFiring);

    WeaponAttackAction getWaa();

    /**
     * Used to determine if the AttackHandler is handling a strafing run.
     *
     * @return
     */
    public boolean isStrafing();

    public void setStrafing(boolean isStrafing);

    /**
     * Used to determine if this is the firt time a weapon is firing as part of a strafing run.  This is used for
     * handling heat, to prevent shots after the first one from generating heat.
     *
     * @return
     */
    public boolean isStrafingFirstShot();

    public void setStrafingFirstShot(boolean isFirstShotStrafing);

}
