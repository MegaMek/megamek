/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.rolls;

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
     * Double-logging style for situations where the mek automatically falls, but the pilot can still save to avoid
     * damage. The game will later strip out any automatic rolls when it lets the pilot roll to save.
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
