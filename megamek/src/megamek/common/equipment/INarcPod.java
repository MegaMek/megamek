/*

 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * @author Sebastian Brocks This class represents an iNarc pod attached to an entity. This class is immutable. Once it
 *       is created, it can not be changed. An iNarc pod can be targeted for a "brush off" attack.
 */
public record INarcPod(int team, int type, int location) implements Serializable, Targetable {
    @Serial
    private static final long serialVersionUID = -3566809840132774242L;
    public static final int HOMING = 1;
    public static final int ECM = 2;
    public static final int HAYWIRE = 4;
    public static final int NEMESIS = 8;

    /**
     * Creates a new <code>INarcPod</code>, from the team and of the type specified.
     */
    public INarcPod {
    }

    /**
     * Determine if the other object is an equivalent INarc pod. <p> Overrides
     * <code>Object#equals(Object)</code>.
     *
     * @param obj the other <code>Object</code> which may be an equivalent INarc pod.
     *
     * @return <code>true</code> if the other object matches this one,
     *       <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
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
     * Get a <code>String</code> representing this INarc pod. <p> Overrides
     * <code>Object#toString()</code>.
     *
     * @return a <code>String</code> that represents this INarc pod.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
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
     *
     * @return a new <code>INarcPod</code> that matches the ID.
     */
    public static INarcPod idToInstance(int id) {
        // Fun games with bitmasks.
        // TODO : test the @#$% out of this!!
        return new INarcPod((id & 0xFFF0) >>> 4, (id & 0x000F), 0);
    }

    // Implementation of Targetable interface.

    @Override
    public int getTargetType() {
        return Targetable.TYPE_I_NARC_POD;
    }

    @Override
    public int getId() {
        // All INarcPods of the same type from the
        // same team are interchangeable targets.
        return ((team << 4) + type);
    }

    @Override
    public void setId(int newId) {}

    @Override
    public int getOwnerId() {
        return Player.PLAYER_NONE;
    }

    @Override
    public void setOwnerId(int newOwnerId) {}

    @Override
    public int getStrength() {
        return 0;
    }

    @Override
    public Coords getPosition() {
        // Hopefully, this will **never** get called.
        throw new IllegalStateException("Never ask for the coords of an INarcPod.");
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        // Hopefully, this will **never** get called.
        throw new IllegalStateException("Never ask for the coords of an INarcPod.");
    }


    @Override
    public int relHeight() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getElevation() {
        return 0;
    }

    @Override
    public boolean isImmobile() {
        // No -4 to-hit bonus.
        return false;
    }

    @Override
    public String getDisplayName() {
        return toString();
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isOffBoard()
     */
    @Override
    public boolean isOffBoard() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isAirborne()
     */
    @Override
    public boolean isAirborne() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isAirborneVTOLorWIGE()
     */
    @Override
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }

    @Override
    public int getAltitude() {
        return 0;
    }

    @Override
    public boolean isEnemyOf(Entity other) {
        return true;
    }

    @Override
    public String generalName() {
        return toString();
    }

    @Override
    public String specificName() {
        return "";
    }
}
