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

import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;

import java.util.ArrayList;
import java.util.Vector;

/**
 * FiringPlan is a series of {@link WeaponFireInfo} objects describing a full attack turn
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 12/18/13 1:20 pM
 */
public class FiringPlan extends ArrayList<WeaponFireInfo> {

    private static final long serialVersionUID = 8938385222775928559L;
    int twist;

    private double utility; // calculated elsewhere
    private Princess owner;
    private Targetable target;

    FiringPlan(Princess owner, Targetable target) {
        twist = 0;
        utility = 0;
        this.owner = owner;
        this.target = target;
    }

    public Targetable getTarget() {
        return target;
    }

    int getHeat() {
        final String METHOD_NAME = "getHeat()";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            int heat = 0;
            for (WeaponFireInfo f : this) {
                heat += f.getHeat();
            }
            return heat;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    double getExpectedDamage() {
        final String METHOD_NAME = "getExpectedDamage()";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            double exdam = 0;
            for (WeaponFireInfo f : this) {
                exdam += f.getExpectedDamageOnHit() * f.getProbabilityToHit();
            }
            return exdam;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    double getExpectedCriticals() {
        final String METHOD_NAME = "getExpectedCriticals()";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            double expcrit = 0;
            for (WeaponFireInfo f : this) {
                expcrit += f.getExpectedCriticals();
            }
            return expcrit;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    double getKillProbability() {
        final String METHOD_NAME = "getKillProbability()";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            double killprob = 0;
            for (WeaponFireInfo f : this) {
                killprob = killprob + ((1 - killprob) * f.getKillProbability());
            }
            return killprob;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    boolean containsWeapon(Mounted wep) {
        final String METHOD_NAME = "containsWeapon(Mounted)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            for (WeaponFireInfo f : this) {
                if (f.getWeapon() == wep) {
                    return true;
                }
            }
            return false;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public Vector<EntityAction> getEntityActionVector(IGame game) {
        final String METHOD_NAME = "getEntiyActionVector(IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            Vector<EntityAction> ret = new Vector<EntityAction>();
            if (size() == 0) {
                return ret;
            }
            if (twist == -1) {
                ret.add(new TorsoTwistAction(get(0).getShooter().getId(),
                                             FireControl.correct_facing(get(0).getShooter().getFacing() - 1)));
            } else if (twist == +1) {
                ret.add(new TorsoTwistAction(get(0).getShooter().getId(),
                                             FireControl.correct_facing(get(0).getShooter().getFacing() + 1)));
            }
            for (WeaponFireInfo f : this) {
                ret.add(f.getWeaponAttackAction());
            }
            return ret;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /*
     * Returns a string describing the firing actions, their likelyhood to
     * hit, and damage
     */
    String getDebugDescription(boolean detailed) {
        if (size() == 0) {
            return "Empty FiringPlan!";
        }
        String ret = new String("Firing Plan for "
                                        + get(0).getShooter().getChassis() + " at "
                                        + get(0).getTarget().getDisplayName() + " "
                                        + Integer.toString(size()) + " weapons fired \n");
        if (detailed) {
            for (WeaponFireInfo wfi : this) {
                ret += wfi.getDebugDescription() + "\n";
            }
        }
        ret += "Total Expected Damage="
                + Double.toString(getExpectedDamage()) + "\n";
        ret += "Total Expected Criticals="
                + Double.toString(getExpectedCriticals()) + "\n";
        ret += "Kill Probability=" + Double.toString(getKillProbability())
                + "\n";
        return ret;
    }

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }
}