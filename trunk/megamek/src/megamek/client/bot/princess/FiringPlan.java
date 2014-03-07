/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;

/**
 * FiringPlan is a series of {@link WeaponFireInfo} objects describing a full attack turn
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at gmail dot com)
 * @since: 12/18/13 1:20 pM
 */
public class FiringPlan extends ArrayList<WeaponFireInfo> {

    private static final long serialVersionUID = 8938385222775928559L;

    private double utility; // calculated elsewhere
    private Targetable target;
    private int twist;

    FiringPlan(Targetable target) {
        setTwist(0);
        setUtility(0);
        this.target = target;
    }

    /**
     * @return The total heat for all weapons being fired with this plan.
     */
    synchronized int getHeat() {
        int heat = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            heat += weaponFireInfo.getHeat();
        }
        return heat;
    }

    /**
     * @return The amount of damage based on the damage of each weapon and their odds of hitting.
     */
    synchronized double getExpectedDamage() {
        double expectedDamage = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedDamage += weaponFireInfo.getExpectedDamageOnHit() * weaponFireInfo.getProbabilityToHit();
        }
        return expectedDamage;
    }

    /**
     * @return The total number of expected critical hits based on the chance to hit, damage to target, toughness of
     *         target and odds of rolling a successful crit check.
     */
    synchronized double getExpectedCriticals() {
        double expectedCriticals = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedCriticals += weaponFireInfo.getExpectedCriticals();
        }
        return expectedCriticals;
    }

    /**
     * @return The odds of getting a kill based on the odds of each individual weapon getting a kill.
     */
    // todo This seems inherently flawed.  Each individual ML fired might have no chance for a kill, but there's a
    // todo (small) chance that 3 fired together could hit the head and score a kill (for example).
    // todo Same logic applies to getExpectedCriticals (above).
    synchronized double getKillProbability() {
        double killProbability = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            killProbability = killProbability + ((1 - killProbability) * weaponFireInfo.getKillProbability());
        }
        return killProbability;
    }

    /**
     * Searches the list of weapons contained in this plan to see if the given weapon is part of it.
     *
     * @param weapon The weapon being searched for.
     * @return TRUE if the given weapon is part of this plan.
     */
    synchronized boolean containsWeapon(Mounted weapon) {
        for (WeaponFireInfo weaponFireInfo : this) {
            if (weaponFireInfo.getWeapon() == weapon) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a {@link Vector} of all the actions, {@link EntityAction}, that make up this firing plan.
     *
     * @return The list of actions as a vector.
     */
    synchronized public Vector<EntityAction> getEntityActionVector() {
        Vector<EntityAction> actionVector = new Vector<EntityAction>();
        if (size() == 0) {
            return actionVector;
        }
        // todo Consider extended twists.
        if (getTwist() == -1) {
            actionVector.add(new TorsoTwistAction(get(0).getShooter().getId(),
                                                  FireControl.correctFacing(get(0).getShooter().getFacing() - 1)));
        } else if (getTwist() == +1) {
            actionVector.add(new TorsoTwistAction(get(0).getShooter().getId(),
                                                  FireControl.correctFacing(get(0).getShooter().getFacing() + 1)));
        }
        for (WeaponFireInfo weaponFireInfo : this) {
            actionVector.add(weaponFireInfo.getWeaponAttackAction());
        }
        return actionVector;
    }

    /*
     * Returns a string describing the firing actions, their likelihood to hit, and damage
     */
    String getDebugDescription(boolean detailed) {
        if (size() == 0) {
            return "Empty FiringPlan!";
        }
        StringBuilder description = new StringBuilder("Firing Plan for ").append(get(0).getShooter().getChassis())
                                                                         .append(" at ")
                                                                         .append(getTarget().getDisplayName())
                                                                         .append("; ").append(Integer.toString(size()))
                                                                         .append(" weapons fired ");
        if (detailed) {
            for (WeaponFireInfo weaponFireInfo : this) {
                description.append("\n\t\t").append(weaponFireInfo.getDebugDescription());
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.00000");
        description.append("\n\tTotal Expected Damage=").append(decimalFormat.format(getExpectedDamage()));
        description.append("\n\tTotal Expected Criticals=").append(decimalFormat.format(getExpectedCriticals()));
        description.append("\n\tKill Probability=").append(decimalFormat.format(getKillProbability()));
        description.append("\n\tUtility=").append(decimalFormat.format(getUtility()));
        return description.toString();
    }

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }

    public int getTwist() {
        return twist;
    }

    public void setTwist(int twist) {
        this.twist = twist;
    }

    /**
     * @return Who is being shot at?
     */
    public Targetable getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FiringPlan)) return false;
        if (!super.equals(o)) return false;

        FiringPlan that = (FiringPlan) o;

        final double TOLERANCE = 0.00001;
        if (twist != that.twist) return false;
        if (Math.abs(utility - that.utility) > TOLERANCE) return false;
        if (!target.equals(that.target)) return false;
        if (getHeat() != that.getHeat()) return false;
        if (Math.abs(getKillProbability() - that.getKillProbability()) > TOLERANCE) return false;
        if (Math.abs(getExpectedCriticals() - that.getExpectedCriticals()) > TOLERANCE) return false;
        if (Math.abs(getExpectedDamage() - that.getExpectedDamage()) > TOLERANCE) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(utility);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + target.hashCode();
        result = 31 * result + twist;
        return result;
    }
}