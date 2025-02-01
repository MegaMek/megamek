/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.bot.caspar.ai.utility.tw;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;
import megamek.common.util.Counter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Cluster {

    private final int team;
    private final List<Entity> members;
    private double centroidX;
    private double centroidY;
    private Coords centroid;
    private UnitRole unitRole;

    public Cluster(int team) {
        this.team = team;
        this.members = new ArrayList<>();
    }

    public void addMember(Entity e) {
        members.add(e);
    }

    public void computeClusterRole() {
        Counter<UnitRole> counter = new Counter<>(members.stream().map(Entity::getRole).toList());
        unitRole = counter.top();
    }

    public void computeCentroid() {
        double sumX = 0.0;
        double sumY = 0.0;
        for (Entity e : members) {
            sumX += e.getPosition().getX();
            sumY += e.getPosition().getY();
        }
        centroidX = sumX / members.size();
        centroidY = sumY / members.size();
        centroid = new Coords((int) Math.round(centroidX), (int) Math.round(centroidY));
    }

    public UnitRole getUnitRole() {
        return unitRole;
    }

    public double getCentroidX() {
        return centroidX;
    }

    public double getCentroidY() {
        return centroidY;
    }

    public Coords getCentroid() {
        return centroid;
    }

    public List<Entity> getMembers() {
        return members;
    }

    public int getTeam() {
        return team;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Cluster.class.getSimpleName() + "[", "]")
            .add("team=" + team)
            .add("members=" + members)
            .add("centroidX=" + centroidX)
            .add("centroidY=" + centroidY)
            .add("centroid=" + centroid)
            .add("unitRole=" + unitRole)
            .toString();
    }
}
