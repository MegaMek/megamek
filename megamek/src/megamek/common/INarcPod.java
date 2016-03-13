/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Sebastian Brocks This class represents an iNarc pod attached to an
 *         entity. This class is immutable. Once it is created, it can not be
 *         changed. An iNarc pod can be targeted for a "brush off" attack.
 */

public class INarcPod implements Serializable, Targetable {

    /**
     *
     */
    private static final long serialVersionUID = -3566809840132774242L;
    public static final int HOMING = 1;
    public static final int ECM = 2;
    public static final int HAYWIRE = 4;
    public static final int NEMESIS = 8;

    private int team;
    private int type;
    private int location;

    /**
     * Creates a new <code>INarcPod</code>, from the team and of the type
     * specified.
     */
    public INarcPod(int team, int type, int location) {
        this.team = team;
        this.type = type;
        this.location = location;
    }

    public int getTeam() {
        return team;
    }

    public int getType() {
        return type;
    }

    public int getLocation() {
        return location;
    }

    /**
     * Determine if the other object is an equivalent INarc pod. <p/> Overrides
     * <code>Object#equals(Object)</code>.
     *
     * @param other the other <code>Object</code> which may be an equivalent
     *            INarc pod.
     * @return <code>true</code> if the other object matches this one,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final INarcPod other = (INarcPod) obj;
        return (type == other.type) && (team == other.team);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, team);
    }

    /**
     * Get a <code>String</code> representing this INarc pod. <p/> Overrides
     * <code>Object#toString()</code>.
     *
     * @return a <code>String</code> that represents this INarc pod.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        switch (type) {
            case HOMING:
                buf.append("Homing");
                break;
            case ECM:
                buf.append("ECM");
                break;
            case HAYWIRE:
                buf.append("Haywire");
                break;
            case NEMESIS:
                buf.append("Nemesis");
                break;
        }
        buf.append(" iNarc pod from Team #").append(team);
        return buf.toString();
    }

    /**
     * Create a new iNarc pod that is equivalent to the given ID.
     *
     * @param id the <code>int</code> ID of the iNarc pod.
     * @return a new <code>INarcPod</code> that matches the ID.
     */
    public static INarcPod idToInstance(int id) {
        // Fun games with bitmasks.
        // TODO : test the @#$% out of this!!
        return new INarcPod((id & 0xFFF0) >>> 4, (id & 0x000F), 0);
    }

    // Implementation of Targetable interface.

    public int getTargetType() {
        return Targetable.TYPE_INARC_POD;
    }

    public int getTargetId() {
        // All INarcPods of the same type from the
        // same team are interchangable targets.
        return ((team << 4) + type);
    }

    public Coords getPosition() {
        // Hopefully, this will **never** get called.
        throw new IllegalStateException(
                "Never ask for the coords of an INarcPod.");
    }
    
    public Map<Integer, Coords> getSecondaryPositions() {
        // Hopefully, this will **never** get called.
        throw new IllegalStateException(
                "Never ask for the coords of an INarcPod.");
    }
    

    public int relHeight() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public int getElevation() {
        return 0;
    }

    public boolean isImmobile() {
        // No -4 to-hit bonus.
        return false;
    }

    public String getDisplayName() {
        return toString();
    }

    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }
    
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isOffBoard()
     */
    public boolean isOffBoard() {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborne()
     */
    public boolean isAirborne() {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborneVTOLorWIGE()
     */
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }
    
    public int getAltitude() {
        return 0;
    }
}
