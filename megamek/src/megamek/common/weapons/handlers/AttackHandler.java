/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;

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
     *       called when this method returns true for the present phase.
     */
    boolean cares(GamePhase phase);

    /**
     * Whether this handler will add report content (not just internal bookkeeping) when {@code handle()} runs this
     * phase. Used to decide whether to print the "Weapons fire for X" announcement header: an in-flight artillery round
     * that merely decrements its flight timer in the offboard phase produces no report body, so announcing it would
     * leave a dangling empty header. Defaults to {@code true}; handlers with silent phases override it.
     *
     * @param phase The current game phase
     *
     * @return {@code true} if a report body will be produced this phase, {@code false} if the handler is silent this
     *       phase
     */
    default boolean producesReportThisPhase(GamePhase phase) {
        return true;
    }

    /**
     * This method is called to perform handling of its action or attack but only if cares() returns true in the present
     * game phase. This method must return true to keep this handler active for future phases. If it returns false, the
     * present handler will be discarded and no longer be called, ending any treatment of the associated attack or
     * action.
     *
     * @param phase         The present game phase
     * @param vPhaseReports The reports list to add new reports to
     *
     * @return True to keep this handler active, false to discard it
     */
    boolean handle(GamePhase phase, Vector<Report> vPhaseReports);

    // Frankly, wish I could get rid of this, but I think certain things
    // occasionally need to know the firer.
    int getAttackerId();

    Entity getAttacker();

    boolean announcedEntityFiring();

    void setAnnouncedEntityFiring(boolean announcedEntityFiring);

    WeaponAttackAction getWeaponAttackAction();

    /**
     * Used to determine if the AttackHandler is handling a strafing run.
     *
     */
    boolean isStrafing();

    void setStrafing(boolean isStrafing);

    /**
     * Used to determine if this is the first time a weapon is firing as part of a strafing run.  This is used for
     * handling heat, to prevent shots after the first one from generating heat.
     *
     */
    boolean isStrafingFirstShot();

    void setStrafingFirstShot(boolean isFirstShotStrafing);


    /**
     * Scale the {@code nDamage} based on if the unit is inside a building, partially sticking out of it, or not.
     *
     * @param entityTarget
     * @param bldg
     * @param targetStickingOutOfBuilding
     * @param nDamage
     *
     * @return adjusted {@code nDamage}
     */
    default int getBuildingDamageAdjustment(Entity entityTarget, IBuilding bldg,
          boolean targetStickingOutOfBuilding,
          int nDamage) {
        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if ((null != bldg) && !targetStickingOutOfBuilding && (entityTarget.isInBuilding())) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }
        return nDamage;
    }
}
