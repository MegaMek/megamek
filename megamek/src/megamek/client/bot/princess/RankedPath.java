/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import megamek.common.MovePath;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/5/13 10:19 AM
 */
public class RankedPath implements Comparable<RankedPath> {
    private MovePath path;
    private double rank;
    private String reason;
    private final transient Map<String, Double> scores = new HashMap<>();

    // the expected damage resulting from the calculation of this ranked path
    private double expectedDamage;

    public String getReason() {
        return reason;
    }

    public MovePath getPath() {
        return path;
    }

    public double getRank() {
        return rank;
    }

    public double getExpectedDamage() {
        return expectedDamage;
    }

    void setExpectedDamage(double damage) {
        expectedDamage = damage;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public RankedPath() {
    }

    public RankedPath(double r, MovePath p, String reason) {
        rank = r;
        path = p;
        this.reason = reason;
    }

    public RankedPath(double r, MovePath p, String reason, double damage) {
        rank = r;
        path = p;
        this.reason = reason;
        expectedDamage = damage;
    }

    @Override
    public int compareTo(RankedPath p) {
        if (rank < p.rank) {
            return -1;
        }
        if (p.rank < rank) {
            return 1;
        }
        if (path.getHexesMoved() < p.path.getHexesMoved()) {
            return -1;
        }
        if (p.path.getHexesMoved() < path.getHexesMoved()) {
            return 1;
        }
        if (path.hasWaypoint() && !p.path.hasWaypoint()) {
            return -1;
        }
        if (p.path.hasWaypoint() && !path.hasWaypoint()) {
            return 1;
        }
        if (expectedDamage < p.expectedDamage) {
            return -1;
        }
        if (p.expectedDamage < expectedDamage) {
            return 1;
        }
        return hashCode() > 0 ? 1 : -1;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;

        if (!(object instanceof RankedPath that)) return false;

        return new EqualsBuilder()
            .append(getRank(), that.getRank())
            .append(getExpectedDamage(), that.getExpectedDamage())
            .append(getPath(), that.getPath())
            .append(getReason(), that.getReason())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getPath())
            .append(getRank())
            .append(getReason())
            .append(getExpectedDamage())
            .toHashCode();
    }

    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat("0.00");
        return "Rank (" + format.format(rank) + ")\tReason (" + reason + ")\nPath: " + path.toString();
    }
}
