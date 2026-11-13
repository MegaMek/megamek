/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.formation;

import java.util.List;

import megamek.client.ratgenerator.FormationType;
import megamek.common.annotations.Nullable;

/**
 * One formation the {@link FormationAssembler} proposes: a name ready for the force tree, the CamOps
 * formation type it qualified as (the future sub-bot preset key), and the units in it. This is a plan,
 * not a force - the lobby turns it into a {@code Force} through the normal force packets.
 *
 * @param name  the formation name, e.g. "Battle Lance Alpha" - already unique against the names the
 *              caller reported in use
 * @param type  the best-matching CamOps formation type, or {@code null} when no type qualified (the group is
 *              still a legal formation, it just has no doctrine name or preset)
 * @param units the units assigned to this formation
 */
public record AssembledFormation(String name, @Nullable FormationType type, List<AssemblyUnit> units) {

    /** @return a chat-ready summary, e.g. {@code "Battle Lance Alpha (4)"} */
    public String describe() {
        return name + " (" + units.size() + ")";
    }
}
