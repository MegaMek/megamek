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
package megamek.common.losDiagram;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;

/**
 * Unit type classification for the LOS elevation diagram. Controls which silhouette is drawn for attacker and target
 * units.
 */
public enum DiagramUnitType {
    MEK,
    VEHICLE,
    VTOL_TYPE,
    NAVAL,
    SUBMARINE,
    INFANTRY,
    BATTLE_ARMOR,
    PROTO_MEK,
    AERO,
    OTHER;

    /**
     * Determines the diagram unit type from a game entity.
     *
     * @param entity the game entity
     *
     * @return the corresponding diagram unit type
     */
    public static DiagramUnitType fromEntity(Entity entity) {
        if (entity instanceof Mek) {
            return MEK;
        } else if (entity instanceof VTOL) {
            return VTOL_TYPE;
        } else if (entity instanceof BattleArmor) {
            return BATTLE_ARMOR;
        } else if (entity instanceof Infantry) {
            return INFANTRY;
        } else if (entity instanceof ProtoMek) {
            return PROTO_MEK;
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode().isSubmarine()) {
                return SUBMARINE;
            }
            return entity.isNaval() ? NAVAL : VEHICLE;
        } else if (entity instanceof Aero) {
            return AERO;
        }
        return OTHER;
    }

    /**
     * Returns the standard TW height in levels for this unit type.
     *
     * @return the height in TW levels (e.g., Mek = 2, Vehicle = 1)
     */
    public int twHeight() {
        return switch (this) {
            case MEK -> 2;
            default -> 1;
        };
    }

    /**
     * Returns whether this unit type is a Mek (affects LOS calculations).
     *
     * @return true if this is a Mek
     */
    public boolean isMek() {
        return this == MEK;
    }
}
