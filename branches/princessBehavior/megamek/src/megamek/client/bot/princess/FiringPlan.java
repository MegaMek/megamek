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

import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * FiringPlan is a series of {@link WeaponFireInfo} objects describing a full attack turn
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/17/13 8:13 AM
 */
public class FiringPlan {

    private static final long serialVersionUID = 8938385222775928559L;
    private final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
    private final NumberFormat LOG_DEC = DecimalFormat.getInstance();

    private final List<WeaponFireInfo> firingInfo = Collections.synchronizedList(new ArrayList<WeaponFireInfo>());

    private int twist;
    private Targetable target;
    private boolean initialized;
    private int heat;
    private double expectedDamage;
    private double expectedCriticals;
    private double killProbability;
    private double utility;
    private IGame game;
    private Entity shooter;

    FiringPlan(Targetable target, Entity shooter, IGame game) {
        if (target == null) {
            throw new IllegalArgumentException("Target is NULL!");
        }
        if (shooter == null) {
            throw new IllegalArgumentException("Shooter is NULL!");
        }
        if (game == null) {
            throw new IllegalArgumentException("Game is NULL!");
        }

        twist = 0;
        initialized = false;
        heat = 0;
        expectedCriticals = 0;
        expectedDamage = 0;
        killProbability = 0;
        utility = 0;
        this.target = target;
        this.game = game;
        this.shooter = shooter;
    }

    private void initialize() {
        calcHeat();
        calcKillProbability();
        calcExpectedDamage();
        calcExpectedCriticals();
        synchronized (firingInfo) {
            initialized = true;
        }
    }

    public Entity getShooter() {
        return shooter;
    }

    public IGame getGame() {
        return game;
    }

    public Targetable getTarget() {
        return target;
    }

    public int getTwist() {
        return twist;
    }

    public void setTwist(int twist) {
        this.twist = twist;
    }

    public void addWeaponFire(WeaponFireInfo weaponFire) {
        synchronized (firingInfo) {
            firingInfo.add(weaponFire);
            initialized = false;
        }
    }

    public List<WeaponFireInfo> getFiringInfo() {
        return new ArrayList<WeaponFireInfo>(firingInfo);
    }

    public void addWeaponFireList(List<WeaponFireInfo> fireList) {
        synchronized (firingInfo) {
            firingInfo.addAll(fireList);
            initialized = false;
        }
    }

    public int getHeat() {
        if (!initialized) {
            initialize();
        }
        return heat;
    }

    private void calcHeat() {
        final String METHOD_NAME = "calcHeat()";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            heat = 0;
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    heat += weaponFireInfo.getHeat();
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public double getExpectedDamage() {
        if (!initialized) {
            initialize();
        }
        return expectedDamage;
    }

    private void calcExpectedDamage() {
        final String METHOD_NAME = "calcExpectedDamage()";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            expectedDamage = 0;
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    expectedDamage += weaponFireInfo.getExpectedDamage();
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }

    }

    public double getExpectedCriticals() {
        if (!initialized) {
            initialize();
        }
        return expectedCriticals;
    }

    private void calcExpectedCriticals() {
        final String METHOD_NAME = "calcExpectedCriticals()";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            expectedCriticals = 0;
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    expectedCriticals += weaponFireInfo.getExpectedCriticals();
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public double getKillProbability() {
        if (!initialized) {
            initialize();
        }
        return killProbability;
    }

    private void calcKillProbability() {
        final String METHOD_NAME = "calcKillProbability()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            killProbability = 0;
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    killProbability = killProbability + ((1 - killProbability) * weaponFireInfo.getKillProbability());
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public boolean containsWeapon(Mounted weapon) {
        if (weapon == null) {
            return false;
        }

        final String METHOD_NAME = "containsWeapon(Mounted)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    if (weapon.equals(weaponFireInfo.getWeapon())) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public Vector<EntityAction> getEntityActionVector() {
        final String METHOD_NAME = "getEntiyActionVector(IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            if (firingInfo.size() == 0) {
                return new Vector<EntityAction>(0);
            }

            int size = firingInfo.size() + ((twist != 0) ? 1 : 0);
            Vector<EntityAction> ret = new Vector<EntityAction>(size);
            if (twist != 0) {
                Entity shooter = firingInfo.get(0).getShooter();
                ret.add(new TorsoTwistAction(shooter.getId(), FireControl.correctFacing(shooter.getFacing() + twist)));
            }
            synchronized (firingInfo) {
                for (WeaponFireInfo weaponFireInfo : firingInfo) {
                    ret.add(weaponFireInfo.getWeaponAttackAction());
                }
            }
            return ret;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /*
     * Returns a string describing the firing actions, their likelyhood to
     * hit, and damage
     */
    public String getDebugDescription(boolean detailed) {

        if (firingInfo.size() == 0) {
            return "Empty FiringPlan!";
        }
        StringBuilder ret = new StringBuilder("Firing Plan for ").append(firingInfo.get(0).getShooter().getDisplayName())
                            .append(" at ").append(getTarget().getDisplayName()).append(" ")
                            .append(Integer.toString(firingInfo.size())).append(" weapons fired \n");
        if (detailed) {
            synchronized (firingInfo) {
                for (WeaponFireInfo wfi : firingInfo) {
                    ret.append(wfi.getDebugDescription()).append("\n");
                }
            }
        }
        ret.append("Total Expected Damage=").append(LOG_DEC.format(getExpectedDamage())).append("\n");
        ret.append("Total Expected Criticals=").append(LOG_DEC.format(getExpectedCriticals())).append("\n");
        ret.append("Kill Probability=").append(LOG_PER.format(getKillProbability()));
        return ret.toString();
    }

    public void calcUtility(int overheatValue) {
        final String METHOD_NAME = "calcUtility(int)";
        double damageUtility = 1.0;
        double criticalUtility = 10.0;
        double killUtility = 50.0;
        double overheatDisutility = -5.0;
        double commanderUtility = 20.0;
        double buildingAsCoverUtility = 10.0;
        int overheat = 0;
        if (getHeat() > overheatValue) {
            overheat = getHeat() - overheatValue;
        }

        double total = 0;
        StringBuilder msg = new StringBuilder("Calculating Firing Plan Utility:");
        msg.append(getDebugDescription(true));

        // Damage
        double expected = getExpectedDamage();
        double mod = expected * damageUtility;
        msg.append("\n\tExpected Damage Mod = ")
           .append(LOG_DEC.format(expected))
           .append(" * ")
           .append(LOG_DEC.format(damageUtility))
           .append(" = ")
           .append(LOG_DEC.format(mod));
        total += mod;

        // Crits
        expected = getExpectedCriticals();
        mod = expected * criticalUtility;
        msg.append("\n\tExpected Crit Mod = ")
           .append(LOG_DEC.format(expected))
           .append(" * ")
           .append(LOG_DEC.format(criticalUtility))
           .append(" = ")
           .append(LOG_DEC.format(mod));
        total += mod;

        // Kill
        expected = getKillProbability();
        mod = expected * killUtility;
        msg.append("\n\tKill Probability Mod = ")
           .append(LOG_DEC.format(expected))
           .append(" * ")
           .append(killUtility)
           .append(" = ")
           .append(LOG_DEC.format(mod));
        total += mod;

        // Commander
        mod = new EntityState(getTarget()).isCommander() ? commanderUtility : 0;
        msg.append("\n\tCommander Mod = ").append(LOG_DEC.format(mod));
        total += mod;

        // Using a building for cover.
        if (getTarget() instanceof BuildingTarget) {
            Enumeration<Entity> enemies = game.getEnemyEntities(target.getPosition(), shooter);
            int enemyCount = 0;
            while (enemies.hasMoreElements()) {
                Entity enemy = enemies.nextElement();
                if (enemy.isAirborne()) {
                    continue; // Not in or on a building.
                }
                enemyCount++;
            }
            mod = enemyCount * buildingAsCoverUtility;
            msg.append("\n\tBuilding as Cover mod: ")
               .append(enemyCount)
               .append(" units * ")
               .append(LOG_DEC.format(buildingAsCoverUtility))
               .append(" = ")
               .append(LOG_DEC.format(mod));
            total += mod;
        }

        // Overheating.
        mod = overheat * overheatDisutility;
        msg.append("\n\tOverheat Mod = ")
           .append(overheat)
           .append(" * ")
           .append(LOG_DEC.format(overheatDisutility))
           .append(" = ")
           .append(mod);
        total += mod;

        // Final total.
        msg.append("\n\tTOTAL = ").append(LOG_DEC.format(total));
        utility = total;
        Logger.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
    }

    public double getUtility() {
        return utility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FiringPlan that = (FiringPlan) o;

        if (twist != that.twist) {
            return false;
        }
        if (!firingInfo.equals(that.firingInfo)) {
            return false;
        }
        if (!shooter.equals(that.shooter)) {
            return false;
        }
        if (!target.equals(that.target)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = firingInfo.hashCode();
        result = 31 * result + twist;
        result = 31 * result + target.hashCode();
        result = 31 * result + shooter.hashCode();
        return result;
    }
}