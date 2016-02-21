/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

public class PilotingRollData extends TargetRoll {
    private static final long serialVersionUID = -8965684775619336323L;
    private int entityId;

    public PilotingRollData(int entityId) {
        this.entityId = entityId;
    }

    public PilotingRollData(int entityId, int value, String desc) {
        super(value, desc);
        this.entityId = entityId;
    }

    public PilotingRollData(int entityId, int value, String desc, boolean cumulative) {
        super(value, desc, cumulative);
        this.entityId = entityId;
    }

    /**
     * Double-logging style for situations where the mech automatically falls,
     * but the pilot can still save to avoid damage. The game will later strip
     * out any automatic rolls when it lets the pilot roll to save.
     */
    public PilotingRollData(int entityId, int value, int pilotValue, String desc) {
        super(value, desc);
        addModifier(pilotValue, desc);
        this.entityId = entityId;
    }

    public int getEntityId() {
        return entityId;
    }

}
