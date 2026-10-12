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
package megamek.client.ratgenerator;

import java.util.ArrayList;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Picks the one animal a beast-mounted infantry force rides.
 *
 * <p>Each beast is its own chassis in the unit files ("Beast Infantry (Horse)", "Beast Infantry (Branth)"), and that
 * chassis's models are the weapon variants. Pinning the chassis on the root before the force tree is built means every
 * platoon inherits it, so the whole force is on horses, while each platoon still rolls its own weapons.</p>
 */
final class BeastMountSelector {

    private static final MMLogger LOGGER = MMLogger.create(BeastMountSelector.class);

    private BeastMountSelector() {
    }

    /**
     * For a {@link InfantryClass#BEAST} infantry force, picks one beast chassis available to the faction and year and
     * pins it on the root. Does nothing for any other force.
     *
     * @param root the force about to be built
     *
     * @return the pinned chassis, or {@code null} when nothing was pinned
     */
    static @Nullable String pinBeast(ForceDescriptor root) {
        boolean isInfantryForce = (root.getUnitType() != null) && (root.getUnitType() == UnitType.INFANTRY);
        if ((root.getInfantryClass() != InfantryClass.BEAST) || !isInfantryForce) {
            return null;
        }
        if (root.getFactionRec() == null) {
            LOGGER.warn("[ForceGen][InfantryClass] Beast force: unknown faction {}; no beast pinned", root.getFaction());
            return null;
        }
        UnitTable table = UnitTable.findTable(root.getFactionRec(),
              UnitType.INFANTRY,
              root.getYear(),
              root.ratGeneratorRating(),
              new ArrayList<>(),
              ModelRecord.NETWORK_NONE,
              new ArrayList<>(),
              new ArrayList<>(),
              0);
        MekSummary beast = table.generateUnit(unit -> InfantryClass.BEAST.matches(unit, root.getFlags()));
        if (beast == null) {
            // Expected until beast-mounted infantry has unit-table availability: the platoons then find nothing
            // and the empty slots are removed, so say plainly why the force came out empty.
            LOGGER.info("[ForceGen][InfantryClass] Beast force: no beast-mounted infantry available to faction={}"
                  + " year={} options={}; the force will be empty", root.getFaction(), root.getYear(),
                  root.getFlags());
            return null;
        }
        root.setInfantryClassChassis(beast.getChassis());
        LOGGER.info("[ForceGen][InfantryClass] Beast force: every platoon rides '{}' (faction={} year={} options={})",
              beast.getChassis(), root.getFaction(), root.getYear(), root.getFlags());
        return beast.getChassis();
    }
}
