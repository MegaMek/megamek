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
package megamek.client.ui.clientGUI.boardview;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.*;

/**
 * Unit type classification for the LOS elevation diagram. Controls which side-view silhouette shape is drawn for
 * attacker and target units in the cross-section diagram.
 */
enum DiagramUnitType {
    /** BattleMek biped - humanoid shape, 2 levels tall. */
    BATTLE_MEK,
    /** BattleMek quad - four-legged mek, 2 levels tall. */
    QUAD_MEK,
    /** SuperHeavy Mek - massive mek, 3 levels tall. */
    SUPERHEAVY_MEK,
    /** IndustrialMek - blockier humanoid shape, 2 levels tall. */
    INDUSTRIAL_MEK,
    /** Tracked combat vehicle - low hull with turret. */
    TRACKED_VEHICLE,
    /** Wheeled vehicle - wheeled chassis profile. */
    WHEELED_VEHICLE,
    /** Hover vehicle - hovercraft profile. */
    HOVER_VEHICLE,
    /** WiGE vehicle - ground-effect craft. */
    WIGE_VEHICLE,
    /** Support vehicle - truck/utility profile, no turret. */
    SUPPORT_VEHICLE,
    /** VTOL - helicopter with rotor disc. */
    VTOL_TYPE,
    /** Naval surface vessel - hull with superstructure above waterline. */
    NAVAL,
    /** Submarine - elongated oval hull with conning tower. */
    SUBMARINE,
    /** Leg infantry - foot soldiers. */
    INFANTRY,
    /** Jump infantry - jump pack equipped soldiers. */
    JUMP_INFANTRY,
    /** Motorized infantry - motorcycle/ATV mounted. */
    MOTORIZED_INFANTRY,
    /** Mechanized infantry - APC/IFV transported. */
    MECHANIZED_INFANTRY,
    /** Battle Armor - stocky powered suit. */
    BATTLE_ARMOR,
    /** ProtoMek - small hunched mek shape. */
    PROTO_MEK,
    /** Aerospace fighter - swept-wing side profile. */
    AEROSPACE_FIGHTER,
    /** Conventional fighter - straight-wing propeller/jet aircraft. */
    CONVENTIONAL_FIGHTER,
    /** DropShip - large egg/teardrop spacecraft. */
    DROPSHIP,
    /** Small craft - compact shuttle/transport. */
    SMALL_CRAFT,
    /** JumpShip - long vessel with solar sail. */
    JUMPSHIP,
    /** WarShip - large armed capital ship. */
    WARSHIP,
    /** Space station - cylindrical/modular structure. */
    SPACE_STATION,
    /** Unknown or unclassified unit type. */
    OTHER;

    /**
     * Determines the diagram unit type from a game entity. Order of instanceof checks matters due to class hierarchy
     * (e.g., BattleArmor extends Infantry, VTOL extends Tank, Dropship extends SmallCraft extends Aero).
     *
     * @param entity the game entity
     * @return the corresponding diagram unit type
     */
    public static DiagramUnitType fromEntity(Entity entity) {
        // Meks - check superheavy, then quad, then industrial vs battle
        if (entity instanceof Mek mek) {
            if (mek.isSuperHeavy()) {
                return SUPERHEAVY_MEK;
            }
            if (mek.isIndustrial()) {
                return INDUSTRIAL_MEK;
            }
            if (entity instanceof QuadMek) {
                return QUAD_MEK;
            }
            return BATTLE_MEK;
        }

        // Infantry hierarchy: BattleArmor before Infantry
        if (entity instanceof BattleArmor) {
            return BATTLE_ARMOR;
        }
        if (entity instanceof Infantry) {
            if (entity.getMovementMode().isJumpInfantry()) {
                return JUMP_INFANTRY;
            }
            if (entity.getMovementMode().isMotorizedInfantry()) {
                return MOTORIZED_INFANTRY;
            }
            if (entity.getMovementMode().isTrackedWheeledOrHover()) {
                return MECHANIZED_INFANTRY;
            }
            return INFANTRY;
        }

        if (entity instanceof ProtoMek) {
            return PROTO_MEK;
        }

        // Vehicle hierarchy: VTOL before Tank, SupportTank before Tank
        if (entity instanceof VTOL) {
            return VTOL_TYPE;
        }
        if (entity instanceof SupportTank) {
            return SUPPORT_VEHICLE;
        }
        if (entity instanceof Tank) {
            if (entity.getMovementMode().isSubmarine()) {
                return SUBMARINE;
            }
            if (entity.getMovementMode().isWiGE()) {
                return WIGE_VEHICLE;
            }
            if (entity.getMovementMode().isWheeled()) {
                return WHEELED_VEHICLE;
            }
            if (entity.getMovementMode().isHover()) {
                return HOVER_VEHICLE;
            }
            return entity.isNaval() ? NAVAL : TRACKED_VEHICLE;
        }

        // Aero hierarchy: most specific first
        if (entity instanceof SpaceStation) {
            return SPACE_STATION;
        }
        if (entity instanceof Warship) {
            return WARSHIP;
        }
        if (entity instanceof Jumpship) {
            return JUMPSHIP;
        }
        if (entity instanceof Dropship) {
            return DROPSHIP;
        }
        if (entity instanceof SmallCraft) {
            return SMALL_CRAFT;
        }
        if (entity instanceof ConvFighter) {
            return CONVENTIONAL_FIGHTER;
        }
        if (entity instanceof AeroSpaceFighter) {
            return AEROSPACE_FIGHTER;
        }

        return OTHER;
    }

    /**
     * Returns the standard TW height in levels for this unit type.
     *
     * @return the height in TW levels (Meks = 2, all others = 1)
     */
    public int twHeight() {
        return switch (this) {
            case BATTLE_MEK, QUAD_MEK, INDUSTRIAL_MEK -> 2;
            case SUPERHEAVY_MEK -> 3;
            default -> 1;
        };
    }

    /**
     * Returns whether this unit type is a Mek (affects LOS calculations).
     *
     * @return true if this is a BattleMek, QuadMek, or IndustrialMek
     */
    public boolean isMek() {
        return (this == BATTLE_MEK) || (this == QUAD_MEK) || (this == SUPERHEAVY_MEK) || (this == INDUSTRIAL_MEK);
    }

    /**
     * Returns whether this unit type uses altitude (TW p.43) when airborne. Altitude refers to airborne aerospace units
     * and has fixed, unchanging values independent of hex terrain level.
     *
     * @return true if this unit type uses altitude when airborne
     */
    public boolean isAltitudeUnit() {
        return switch (this) {
            case AEROSPACE_FIGHTER, CONVENTIONAL_FIGHTER, DROPSHIP, SMALL_CRAFT,
                 JUMPSHIP, WARSHIP, SPACE_STATION -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this unit type uses elevation (TW p.43) when airborne. Elevation refers to non-aerospace airborne
     * units (VTOLs, WiGE) and is relative to the underlying hex level, synonymous with level but used for such airborne
     * units.
     *
     * @return true if this unit type uses elevation when airborne
     */
    public boolean isElevationUnit() {
        return (this == VTOL_TYPE) || (this == WIGE_VEHICLE);
    }
}
