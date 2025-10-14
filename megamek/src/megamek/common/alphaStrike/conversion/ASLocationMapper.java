/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.alphaStrike.conversion;

import static megamek.common.equipment.MiscType.F_HEAD_TURRET;
import static megamek.common.equipment.MiscType.F_QUAD_TURRET;
import static megamek.common.equipment.MiscType.F_SHOULDER_TURRET;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Mounted;
import megamek.common.units.*;

/**
 * This class provides AlphaStrike conversion utilities for converting all sorts of locations of TW units to the damage
 * conversion location index. Not useful for anything outside AlphaStrike conversion.
 */
public class ASLocationMapper {

    static int damageLocationsCount(Entity en) {
        if ((en instanceof Jumpship) || (en instanceof SmallCraft)) {
            return 4;
        } else if (en instanceof Tank) {
            if (((Tank) en).hasNoTurret()) {
                return 2;
            } else if (((Tank) en).hasNoDualTurret()) {
                return 3;
            } else {
                return 4;
            }
        } else if ((en instanceof TripodMek) || (en instanceof QuadVee)) {
            return 3;
        } else if (en instanceof Mek) {
            return (en.hasMisc(F_QUAD_TURRET) || en.hasMisc(F_SHOULDER_TURRET) || en.hasMisc(F_HEAD_TURRET)) ? 3 : 2;
        } else if (en instanceof Aero) {
            return 2;
        } else if (en instanceof BattleArmor) {
            return 1;
        } else if (en instanceof Infantry) {
            return ((Infantry) en).hasFieldWeapon() ? 2 : 1;
        } else {
            return 1;
        }
    }

    public static double damageLocationMultiplier(Entity en, int loc, Mounted<?> mount) {
        if (locationName(en, loc).startsWith("TUR") && (en instanceof Mek) && mount.isMekTurretMounted()) {
            return 1;
        } else if (en instanceof Warship) {
            return getWarShipLocationMultiplier(loc, mount.getLocation());
        } else if (en instanceof Jumpship) {
            return getJumpShipLocationMultiplier((Jumpship) en, loc, mount.getLocation(), mount.isRearMounted());
        } else if (en instanceof SmallCraft) {
            return getSmallCraftLocationMultiplier((SmallCraft) en, loc, mount.getLocation(), mount.isRearMounted());
        } else if ((en instanceof SupportTank) && !(en instanceof LargeSupportTank)) {
            return getSupportTankLocationMultiplier(loc, mount.getLocation());
        } else if (en instanceof VTOL) {
            return getVTOLLocationMultiplier(loc, mount.getLocation());
        } else if ((en instanceof SuperHeavyTank) || (en instanceof LargeSupportTank)) {
            return getSuperHeavyTankLocationMultiplier(loc, mount.getLocation());
        } else if (en instanceof Tank) {
            return getTankLocationMultiplier(loc, mount.getLocation());
        } else if (en instanceof Aero) {
            return getAeroLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
        } else if (en instanceof BattleArmor) {
            // A few weapons (e.g. Narc) are present in the weapon list for every trooper,
            // count only the first (loc = 1)
            // Don't count squad support weapons, these are handled separately
            return ((mount.getLocation() <= 1) && !mount.isSquadSupportWeapon()) ? 1 : 0;
        } else if (en instanceof Infantry) {
            // CI only ever have loc == 0 (no TUR, REAR, arcs); do not count standard
            // weapons when it has field guns
            return (!((Infantry) en).hasFieldWeapon() || (mount.getLocation() == Infantry.LOC_FIELD_GUNS)) ? 1 : 0;
        } else if (en instanceof TripodMek) {
            return getTripodMekLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
        } else if (en instanceof QuadVee) {
            return getQuadVeeLocationMultiplier(loc, mount.isRearMounted());
        } else if (en instanceof Mek) {
            return getMekLocationMultiplier(loc, mount.isRearMounted());
        } else {
            return 1;
        }
    }

    /**
     * Returns the value multiplier for the given Mounted of the given entity in the AS conversion location loc. The AS
     * conversion location is an {@link megamek.common.alphaStrike.ASSpecialAbilityCollection} assembling the specials
     * for the unit, a TUR ability, REAR and the arcs of large units, not the mounted's location on the entity. This
     * multiplier is only correct for weapon-based special abilities like MHQ, C3M, NARC or ART-x abilities.
     *
     * @param en    The entity
     * @param loc   The conversion location index, see locations[] in ASDamageConverter
     * @param mount the weapon
     *
     * @return The value multiplier, 1 meaning "counts for this location", 0 meaning "doesn't count"
     */
    public static double damageLocationMultiplierForSpecials(Entity en, int loc, Mounted<?> mount) {
        if (en.isFighter() || en.isProtoMek() || en.isMek()) {
            if ((loc == 0) && (mount.isRearMounted() || (en.isFighter() && mount.getLocation() == Aero.LOC_AFT))) {
                // Special abilities like MHQ or ART must count for loc 0 (standard specials)
                // even if in rear locations
                return 1;
            } else if (locationName(en, loc).equals("REAR")) {
                return 0;
            }
        }
        return damageLocationMultiplier(en, loc, mount);

    }

    public static String locationName(Entity en, int index) {
        if ((en instanceof Warship) || (en instanceof SmallCraft)) {
            return en.getLocationAbbreviations()[index];
        } else if (en instanceof Jumpship) {
            // Remove leading F from FLS and FRS
            String retVal = en.getLocationAbbreviations()[index];
            if (retVal.charAt(0) == 'F') {
                return retVal.substring(1);
            }
            return retVal;
        } else if (en instanceof SupportTank) {
            if (index == 1) {
                return "REAR";
            }
            if (index > 1) {
                if (((SupportTank) en).hasNoDualTurret()) {
                    return "TUR";
                } else {
                    return "TUR" + index;
                }
            } else {
                return "";
            }
        } else if (en instanceof Aero) {
            if (index == 1) {
                return "REAR";
            }
            return "";
        } else if (en instanceof Infantry) {
            if (index == 0) {
                return "";
            }
            return en.getLocationAbbreviations()[index];
        } else if ((en instanceof Mek) || (en instanceof Tank)) {
            if (index == 1) {
                return "REAR";
            } else if (index == 2) {
                return "TUR";
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private static double getWarShipLocationMultiplier(int index, int location) {
        // ASC p. 103
        return switch (location) {
            case Warship.LOC_NOSE, Warship.LOC_FLS, Warship.LOC_FRS -> (index == 0) ? 1 : 0;
            case Warship.LOC_LBS, Warship.LOC_ALS -> (index == 1) ? 1 : 0;
            case Warship.LOC_RBS, Warship.LOC_ARS -> (index == 2) ? 1 : 0;
            case Warship.LOC_AFT -> (index == 3) ? 1 : 0;
            default -> 0;
        };
    }

    private static double getJumpShipLocationMultiplier(Jumpship jumpship, int index, int location,
          boolean rearMounted) {
        switch (index) {
            case 0:
                if (location == Jumpship.LOC_NOSE) {
                    return 1;
                } else {
                    return (location == Jumpship.LOC_FLS || location == Jumpship.LOC_FRS) ? 0.5 : 0;
                }
            case 1:
                return (location == Jumpship.LOC_FLS || location == Jumpship.LOC_ALS) ? 0.5 : 0;
            case 2:
                return (location == Jumpship.LOC_FRS || location == Jumpship.LOC_ARS) ? 0.5 : 0;
            case 3:
                if (location == Jumpship.LOC_AFT) {
                    return 1;
                } else {
                    return (location == Jumpship.LOC_ALS || location == Jumpship.LOC_ARS) ? 0.5 : 0;
                }
        }
        return 0;
    }

    private static double getSmallCraftLocationMultiplier(SmallCraft en, int index, int location, boolean rearMounted) {
        switch (index) {
            case 0:
                if (location == SmallCraft.LOC_NOSE) {
                    return 1;
                }
                if (en.isSpheroid() && (location == SmallCraft.LOC_LEFT_WING || location == SmallCraft.LOC_RIGHT_WING)
                      && !rearMounted) {
                    return 0.5;
                }
                break;
            case 1:
            case 2:
                if (index == location) {
                    if (en.isSpheroid()) {
                        return 0.5;
                    }
                    if (!rearMounted) {
                        return 1;
                    }
                }
                break;
            case 3:
                if (location == SmallCraft.LOC_AFT) {
                    return 1;
                }
                if (rearMounted && (location == SmallCraft.LOC_LEFT_WING || location == SmallCraft.LOC_RIGHT_WING)) {
                    return en.isSpheroid() ? 0.5 : 1.0;
                }
                break;
        }
        return 0;
    }

    private static double getTankLocationMultiplier(int index, int location) {
        if ((index == 0) && (location != Tank.LOC_REAR)) {
            return 1;
        } else if ((index == 1) && (location == Tank.LOC_REAR)) {
            return 1;
        } else if ((index == 2) && ((location == Tank.LOC_TURRET) || (location == Tank.LOC_TURRET_2))) {
            return 1;
        }
        return 0;
    }

    private static double getAeroLocationMultiplier(int index, int location, boolean rearMounted) {
        if ((index == 0 && location != Aero.LOC_AFT && !rearMounted)
              || (index == 1 && (location == Aero.LOC_AFT || rearMounted))) {
            return 1;
        }
        return 0;
    }

    private static double getSupportTankLocationMultiplier(int index, int location) {
        if ((index == 0) && ((location == SupportTank.LOC_FRONT) || (location == SupportTank.LOC_LEFT)
              || (location == SupportTank.LOC_RIGHT)
              || (location == SupportTank.LOC_TURRET) || (location == SupportTank.LOC_TURRET_2))) {
            return 1;
        } else if (index == 1 && (location == SupportTank.LOC_REAR)) {
            return 1;
        } else if ((index == 2) && (location == SupportTank.LOC_TURRET)) {
            return 1;
        } else if ((index == 3) && (location == SupportTank.LOC_TURRET_2)) {
            return 1;
        }
        return 0;
    }

    private static double getMekLocationMultiplier(int index, boolean rearMounted) {
        if ((index == 0 && !rearMounted) || ((index == 1) && rearMounted)) {
            return 1;
        } else if (index == 2) {
            return 0;
        }
        return 0;
    }

    private static double getVTOLLocationMultiplier(int index, int location) {
        if ((index == 0) && (location != VTOL.LOC_REAR)) {
            return 1;
        } else if ((index == 1) && (location == VTOL.LOC_REAR)) {
            return 1;
        } else if ((index == 2) && ((location == VTOL.LOC_TURRET) || (location == VTOL.LOC_TURRET_2))) {
            return 1;
        }
        return 0;
    }

    private static double getTripodMekLocationMultiplier(int index, int location, boolean rearMounted) {
        if ((index == 0 && !rearMounted || (index == 1) && rearMounted)) {
            return 1;
        } else if (index == 2) {
            if (location != TripodMek.LOC_CENTER_LEG
                  && location != TripodMek.LOC_RIGHT_LEG
                  && location != TripodMek.LOC_LEFT_LEG) {
                return 1;
            }
        }
        return 0;
    }

    private static double getQuadVeeLocationMultiplier(int index, boolean rearMounted) {
        if ((index == 0 && !rearMounted || (index == 1) && rearMounted)) {
            return 1;
        } else if (index == 2) {
            return 1;
        }
        return 0;
    }

    // This looks the same as the Tank version, but the LOC constants are different!
    private static double getSuperHeavyTankLocationMultiplier(int index, int location) {
        if ((index == 0) && (location != SuperHeavyTank.LOC_REAR)) {
            return 1;
        } else if ((index == 1) && (location == SuperHeavyTank.LOC_REAR)) {
            return 1;
        } else if ((index == 2)
              && ((location == SuperHeavyTank.LOC_TURRET) || (location == SuperHeavyTank.LOC_TURRET_2))) {
            return 1;
        }
        return 0;
    }

    private ASLocationMapper() {
    }
}
