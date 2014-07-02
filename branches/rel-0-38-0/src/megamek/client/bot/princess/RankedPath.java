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

import megamek.common.MovePath;

import java.text.DecimalFormat;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version $Id$
 * @since 12/5/13 10:19 AM
 */
class RankedPath implements Comparable<RankedPath> {
    public MovePath path;
    public double rank;
    public String reason;

    public RankedPath() {
    }

    public RankedPath(double r, MovePath p, String reason) {
        rank = r;
        path = p;
        this.reason = reason;
    }

    public int compareTo(RankedPath p) {
        if (rank < p.rank) {
            return -1;
        }
        if (p.rank < rank) {
            return 1;
        }
        if (path.getKey().hashCode() < p.path.getKey().hashCode()) {
            return -1;
        }
        if (path.getKey().hashCode() > p.path.getKey().hashCode()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RankedPath that = (RankedPath) o;

        if (Double.compare(that.rank, rank) != 0) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = path.hashCode();
        temp = Double.doubleToLongBits(rank);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat("0.00");
        return "Rank (" + format.format(rank) + ")\tReason (" + reason + ")\nPath: " + path.toString();
    }
}
