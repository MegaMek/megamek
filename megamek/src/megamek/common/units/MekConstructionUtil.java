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

package megamek.common.units;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.logging.MMLogger;

/**
 * Helper methods for constructing/changing Mek units. This happens mostly in MML but some units have modular
 * equipment which MM must be able to switch out and the code for this is involved and should be unified as much as
 * possible.
 * <p>
 * This is the MM extension of MekUtil.
 */

public class MekConstructionUtil {

    private static final MMLogger LOGGER = MMLogger.create(MekConstructionUtil.class);

    /**
     * For the given Mek, removes all existing Clan CASE and then re-adds it to every location that has potentially
     * explosive equipment (this includes PPC Capacitors). Skips locations that already have (IS) CASE or CASE II.
     * Respects per-location opt-out.
     *
     * @param mek the mek to update
     */
    public static void updateClanCasePlacement(Mek mek) {
        boolean hadClanCase = mek.isClan() || mek.hasClanCaseEquipped();
        if (hadClanCase) {
            removeAllMounted(mek, EquipmentType.get(EquipmentTypeLookup.CLAN_CASE));
            addClanCaseToExplosiveLocations(mek);
        }
    }

    public static void removeAllMounted(Entity unit, EquipmentType et) {
        for (int pos = unit.getEquipment().size() - 1; pos >= 0; pos--) {
            Mounted<?> mount = unit.getEquipment().get(pos);
            if (mount.getType().equals(et)) {
                ConstructionUtil.removeMounted(unit, mount);
            }
        }
    }


    /**
     * Adds Clan CASE to all locations on the Mek that have explosive equipment and don't already have CASE or CASE II.
     * Unlike {@link Mek#addClanCase()}, this does not check tech base or existing Clan CASE presence. Respects
     * per-location opt-out via {@link Mek#isClanCaseOptedOut(int)}.
     *
     * @param mek the mek to add Clan CASE to
     */
    public static void addClanCaseToExplosiveLocations(Mek mek) {
        EquipmentType clCase = EquipmentType.get(EquipmentTypeLookup.CLAN_CASE);
        for (int i = 0; i < mek.locations(); i++) {
            if (mek.locationHasCase(i) || mek.hasCASEII(i)) {
                continue;
            }
            // Respect per-location opt-out
            if (mek.isClanCaseOptedOut(i)) {
                continue;
            }
            boolean explosiveFound = false;
            for (Mounted<?> m : mek.getEquipment()) {
                if (m.getType().isExplosive(m, true) && ((m.getLocation() == i) || (m.getSecondLocation() == i))) {
                    explosiveFound = true;
                    break;
                }
            }
            if (explosiveFound) {
                try {
                    mek.addEquipment(Mounted.createMounted(mek, clCase), i, false);
                } catch (Exception ignored) {
                    LOGGER.error("Unable to add Clan CASE to Mek!");
                    // 0-crit equipment shouldn't fail
                }
            }
        }
    }

}
