/*
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
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.util.milestoneReleaseInformation;

import java.util.List;

/**
 * Wrapper class for holding a list of milestone release information.
 *
 * <p>This class is primarily used as a container for transferring or serializing/deserializing a collection of
 * {@link MilestoneData} entries, such as when loading or processing milestone release details.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MilestoneReleaseWrapper {
    /** List of milestone release data entries. */
    private List<MilestoneData> milestone_releases;

    /**
     * Returns the list of milestone release data.
     *
     * @return a list containing {@link MilestoneData} objects representing each milestone release
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<MilestoneData> getMilestone_releases() {
        return milestone_releases;
    }

    /**
     * Sets the list of milestone release data.
     *
     * @param milestone_releases a list of {@link MilestoneData} objects to be set
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setMilestone_releases(List<MilestoneData> milestone_releases) {
        this.milestone_releases = milestone_releases;
    }
}
