/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import megamek.common.moves.MovePath;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/5/13 10:19 AM
 */
public class RankedPath implements Comparable<RankedPath> {
    /** Source of the monotonic {@link #creationId} used only as a stable final tie-breaker in {@link #compareTo}. */
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final MovePath path;
    private final double rank;
    private final String reason;
    private final transient Map<String, Double> scores = new HashMap<>();

    /**
     * Creation-order id, used only to break {@link #compareTo} ties in favour of the earliest-created path so the
     * ordering is a valid total order. Not part of {@link #equals(Object)}/{@link #hashCode()} identity.
     */
    private final long creationId = ID_GENERATOR.getAndIncrement();

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

    public RankedPath(double r, MovePath p, String reason) {
        rank = r;
        path = p;
        this.reason = reason;
    }

    @Override
    public int compareTo(RankedPath other) {
        if (rank < other.rank) {
            return -1;
        }
        if (other.rank < rank) {
            return 1;
        }
        if (path.getHexesMoved() < other.path.getHexesMoved()) {
            return -1;
        }
        if (other.path.getHexesMoved() < path.getHexesMoved()) {
            return 1;
        }
        if (expectedDamage < other.expectedDamage) {
            return -1;
        }
        if (other.expectedDamage < expectedDamage) {
            return 1;
        }
        // Final tie-break: among paths tied on rank, hexes moved and expected damage, prefer the
        // earliest-enumerated one, so the choice is stable and reproducible. The only consumer of this ordering is
        // PathRanker's reverse-ordered TreeSet<RankedPath>, whose first() (used by getBestPath) returns the natural
        // maximum - so the earliest path (lowest creationId) must compare as the greatest, hence other.creationId
        // is placed first in the comparison. This yields a valid total order (antisymmetric, transitive,
        // consistent) as required for correct TreeSet selection; the previous `hashCode() > 0 ? 1 : -1` ignored
        // the argument and was non-antisymmetric, so it could misorder or drop tied paths and getBestPath was not
        // guaranteed the true maximum.
        return Long.compare(other.creationId, creationId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {return true;}

        if (!(object instanceof RankedPath that)) {return false;}

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
