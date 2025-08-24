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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.entityreadout;

import megamek.client.ui.Messages;
import megamek.common.units.Entity;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.Infantry;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadVee;

final class EntityReadoutUnitType {

    /**
     * @param entity The unit to describe
     *
     * @return A unit type description specifically for the Entity Readout. It is more detailed than the plain high
     *       level type of the unit and contains some info that is not easy to see from the other data of the Entity
     *       Readout, such as "civilian", "tripod", "aerodyne" and "rail".
     *       <p>
     *       This class and method should stay with the Entity Readout alone so it can be freely adapted to whatever
     *       needs to be shown.
     */
    static String unitTypeAsString(Entity entity) {
        String result = "";
        if (entity.isPrimitive()) {
            result += Messages.getString("MekView.unitType.primitive") + " ";
        }
        if ((entity.isDropShip() || entity.isSmallCraft())) {
            if (!entity.isMilitary()) {
                result += Messages.getString("MekView.unitType.civilian") + " ";
            }
            if (entity.isAerodyne()) {
                result += Messages.getString("MekView.unitType.aerodyne") + " ";
            } else {
                result += Messages.getString("MekView.unitType.spheroid") + " ";
            }
        }
        if (entity instanceof Infantry inf && !entity.isBattleArmor() && inf.isMechanized()) {
            result += Messages.getString("MekView.unitType.mechanized") + " ";
        } else if (entity.getMovementMode().isMotorizedInfantry()) {
            result += Messages.getString("MekView.unitType.motorized") + " ";
        }
        if (entity.isSuperHeavy()) {
            result += Messages.getString("MekView.unitType.superHeavy") + " ";
        }
        if (entity.isTripodMek()) {
            result += Messages.getString("MekView.unitType.tripod") + " ";
        } else if (entity instanceof QuadVee) {
            result += Messages.getString("MekView.unitType.quadVee") + " ";
        } else if (entity.isQuadMek() || (entity instanceof ProtoMek pm && pm.isQuad())) {
            result += Messages.getString("MekView.unitType.quad") + " ";
        }
        if (entity.isIndustrialMek()) {
            result += Messages.getString("MekView.unitType.industrial") + " ";
        }
        if (entity.isConventionalFighter()) {
            result += Messages.getString("MekView.unitType.conventional") + " ";
        } else if (entity.isAerospaceFighter()) {
            result += Messages.getString("MekView.unitType.aerospace") + " ";
        }
        if (entity.isCombatVehicle() && !(entity instanceof GunEmplacement)) {
            result += Messages.getString("MekView.unitType.combat") + " ";
        } else if (entity.isFixedWingSupport()) {
            result += Messages.getString("MekView.unitType.fixedWingSupport") + " ";
        } else if (entity.isSupportVehicle()) {
            result += Messages.getString("MekView.unitType.support") + " ";
        }

        if (entity.isSpaceStation()) {
            if (entity.isMilitary()) {
                result += Messages.getString("MekView.unitType.military") + " ";
            } else {
                result += Messages.getString("MekView.unitType.civilian") + " ";
            }
            result += Messages.getString("MekView.unitType.spaceStation");
        } else if (entity.isJumpShip()) {
            result += Messages.getString("MekView.unitType.jumpShip");
        } else if (entity.isWarShip()) {
            result += Messages.getString("MekView.unitType.warShip");
        } else if (entity.isDropShip()) {
            result += Messages.getString("MekView.unitType.dropShip");
        } else if (entity.isSmallCraft()) {
            result += Messages.getString("MekView.unitType.smallCraft");
        } else if (entity.isProtoMek()) {
            result += Messages.getString("MekView.unitType.protoMek");
        } else if (entity.isBattleArmor()) {
            result += Messages.getString("MekView.unitType.battleArmor");
        } else if (entity.isConventionalInfantry()) {
            result += Messages.getString("MekView.unitType.infantry");
        } else if (entity.isMek() && !entity.isIndustrialMek()) {
            result += Messages.getString("MekView.unitType.battleMek");
        } else if (entity instanceof GunEmplacement) {
            result += Messages.getString("MekView.unitType.gunEmplacement");
        } else if (entity.isIndustrialMek()) {
            result += Messages.getString("MekView.unitType.onlyMek");
        } else if (entity.isVehicle() || entity.isFixedWingSupport()) {
            result += Messages.getString("MekView.unitType.vehicle");
        } else if (entity.isFighter() && !entity.isSupportVehicle()) {
            result += Messages.getString("MekView.unitType.fighter");
        } else if (entity instanceof HandheldWeapon) {
            result += Messages.getString("MekView.unitType.handHeld");
        }
        String addendum = "";
        if (entity.isVehicle()) {
            if (entity.getMovementMode().isSubmarine()) {
                addendum += Messages.getString("MekView.unitType.submarine");
            } else if (entity.getMovementMode().isVTOL()) {
                addendum += Messages.getString("MekView.unitType.vtol");
            } else if (entity.getMovementMode().isHover()) {
                addendum += Messages.getString("MekView.unitType.hover");
            } else if (entity.getMovementMode().isRail()) {
                addendum += Messages.getString("MekView.unitType.rail");
            } else if (entity.getMovementMode().isNaval() || entity.getMovementMode().isHydrofoil()) {
                addendum += Messages.getString("MekView.unitType.naval");
            } else if (entity.getMovementMode().isWiGE()) {
                addendum += Messages.getString("MekView.unitType.wige");
            }
        }
        return result + (addendum.isBlank() ? "" : " (%s)".formatted(addendum));
    }

    private EntityReadoutUnitType() {}
}
