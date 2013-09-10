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

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/8/13 1:16 PM
 */
public class BehaviorMods {
    private double aggression = 0;
    private double bravery = 0;
    private double fallShame = 0;
    private double herding = 0;
    private double selfPreservation = 0;
    private double shooting = 0;

    public double getAggression() {
        return aggression;
    }

    public void setAggression(double aggression) {
        this.aggression = aggression;
    }

    public double getBravery() {
        return bravery;
    }

    public void setBravery(double bravery) {
        this.bravery = bravery;
    }

    public double getFallShame() {
        return fallShame;
    }

    public void setFallShame(double fallShame) {
        this.fallShame = fallShame;
    }

    public double getHerding() {
        return herding;
    }

    public void setHerding(double herding) {
        this.herding = herding;
    }

    public double getSelfPreservation() {
        return selfPreservation;
    }

    public void setSelfPreservation(double selfPreservation) {
        this.selfPreservation = selfPreservation;
    }

    public double getShooting() {
        return shooting;
    }

    public void setShooting(double shooting) {
        this.shooting = shooting;
    }
}
