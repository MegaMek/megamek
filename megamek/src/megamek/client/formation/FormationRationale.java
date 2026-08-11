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
import megamek.common.units.UnitRole;

/**
 * Why one formation looks the way it does: the doctrine name it earned, the ledger that scored it,
 * what could not be broken apart, and the closest grouping the assembler passed over. Produced by
 * {@link FormationAssembler#explain} and rendered by the lobby's formation rationale view.
 *
 * @param formationName       the formation's name in the force tree
 * @param type                the CamOps formation type it qualifies as, null when none does
 * @param organization        the doctrine it was assembled under (element size and naming)
 * @param units               its members
 * @param modalRole           the most common battlefield role among the members
 * @param modalRoleCount      how many members hold that role
 * @param slowestWalkMp       the walking speed the whole formation moves at
 * @param fastestWalkMp       the fastest member's walking speed
 * @param battleValue         the formation's total battle value
 * @param ecmCarriers         how many members carry ECM
 * @param bindings            groups that could not be split, in plain words (C3 nets, carried units)
 * @param closestAlternatives the swaps that came nearest to being chosen instead, cheapest first
 * @param unknownToCatalog    members with no unit-cache entry, which block any type from qualifying
 * @param qualificationDetail the {@link FormationType#qualificationReport} HTML, null without a type
 */
public record FormationRationale(String formationName, @Nullable FormationType type,
      Organization organization, List<AssemblyUnit> units, UnitRole modalRole, int modalRoleCount,
      int slowestWalkMp, int fastestWalkMp, long battleValue, int ecmCarriers, List<String> bindings,
      List<FormationRationale.AlternativeSwap> closestAlternatives, List<String> unknownToCatalog,
      @Nullable String qualificationDetail) {

    /**
     * A trade the assembler considered and rejected: exchanging one of this formation's units for one
     * from another formation.
     *
     * @param unitName        the member that would leave
     * @param otherUnitName   the unit that would arrive in its place
     * @param otherFormation  the formation it would come from
     * @param cost            how much worse the whole force scores after the swap; a small number
     *                        means it was a close call, a large one means the grouping is clear-cut
     */
    public record AlternativeSwap(String unitName, String otherUnitName, String otherFormation,
          double cost) {
    }

    /** @return the walking-speed spread across the formation, in MP */
    public int speedSpread() {
        return fastestWalkMp - slowestWalkMp;
    }

    /** @return the share of members holding {@link #modalRole}, 0 to 1 */
    public double rolePurity() {
        return units.isEmpty() ? 0 : (modalRoleCount / (double) units.size());
    }
}
