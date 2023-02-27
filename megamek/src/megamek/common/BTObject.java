/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

/**
 * This interface represents any physical object that can find itself on the battlefield, including units of any sort
 * (Entity, AlphaStrikeElement, BF Unit, SBF Formation), but also objective markers, carryable objects, noncombatants,
 * buildings. This does not include players or forces. This interface is for all objects even if they are not part
 * of a game (e.g. a unit loaded in the unit selector).
 */
public interface BTObject {

    /**
     * Returns true when this object is a Mek (Industrial Mek or BattleMek) or of type BM/IM for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a Mek or BM/IM
     */
    default boolean isMek() {
        return false;
    }

    /**
     * Returns true when this object is an aerospace unit (fighter, aerospace support vehicle or large craft)
     * or of type AS/CF/SC/DS/DA/WS/JS/SS for Alpha Strike. An aerospace unit is not {@link #isGround()}.
     *
     * @return True when this is an aerospace unit (including aerospace support vehicles) or aerospace group (SBF)
     */
    default boolean isAerospace() {
        return isFighter() || isLargeAerospace();
    }

    /**
     * Returns true when this object is a ProtoMek or of type PM for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a ProtoMek
     */
    default boolean isProtoMek() {
        return false;
    }

    /**
     * Returns true when this object is a BattleMek or of type BM for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a BattleMek (not an Industrial Mek)
     */
    default boolean isBattleMek() {
        return isMek() && !isIndustrialMek();
    }

    /**
     * Returns true when this object is a Tripod Mek.
     * Returns false for any type of unit group even if it consists only of Tripods.
     *
     * @return True when this is a Tripod Mek
     */
    default boolean isTripodMek() {
        return false;
    }

    /**
     * Returns true when this object is a Quad Mek or QuadVee, regardless of conversion state.
     * Returns false for any type of unit group even if it consists only of Quad Meks.
     *
     * @return True when this is a Quad (four-legged) Mek
     */
    default boolean isQuadMek() {
        return false;
    }

    /**
     * Returns true when this object is an Industrial Mek or of type IM for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is an Industrial Mek (not a BattleMek)
     */
    default boolean isIndustrialMek() {
        return false;
    }

    /**
     * Returns true when this object is a ground unit (all types of Mek, Infantry and Vehicle except aerospace
     * support vehicles such as Fixed-Wing Support). A unit is a ground unit if it is not {@link #isAerospace()}.
     * This method should not require overriding.
     *
     * @return True when this is a ground unit or ground group (SBF)
     */
    default boolean isGround() {
        return !isAerospace();
    }

    /**
     * Returns true when this object is a fighter (aerospace or conventional) including
     * Fixed-Wing Support or of type CF/AF/SV(MV a) for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a fighter including fixed-wing support
     */
    default boolean isFighter() {
        return false;
    }

    /**
     * Returns true when this object is a large aerospace unit (SmallCraft, DropShip, JumpShip, WarShip, Space
     * Station).
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a large aerospace unit
     */
    default boolean isLargeAerospace() {
        return false;
    }

    /**
     * Returns true when this object is a BattleArmor unit or of type BA for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a BattleArmor unit
     */
    default boolean isBattleArmor() {
        return false;
    }

    /**
     * Returns true when this object is a Conventional Infantry unit or of type CI for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a Conventional Infantry unit
     */
    default boolean isConventionalInfantry() {
        return false;
    }

    /**
     * Returns true when this object is a Support Vehicle using aerospace movement such as a Fixed-Wing support.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is an aerospace Support Vehicle
     */
    default boolean isAerospaceSV() {
        return false;
    }

    /**
     * Returns true when this object is a SmallCraft (not a DropShip).
     * Returns false for any type of unit group.
     *
     * @return True when this is a SmallCraft
     */
    default boolean isSmallCraft() {
        return false;
    }

    /**
     * Returns true when this object is a DropShip.
     * Returns false for any type of unit group.
     *
     * @return True when this is a DropShip
     */
    default boolean isDropShip() {
        return false;
    }

    /**
     * Returns true when this object has the distinction between aerodyne and spheroid, i.e. if it
     * is a DropShip or SmallCraft. Returns false for fighters as they are always aerodyne and do not have
     * the distinction.
     * Returns false for any type of unit group. This method should not require overriding.
     *
     * @return True when this is object can be aerodyne or spheroid
     */
    default boolean hasAerodyneSpheroidDistinction() {
        return isDropShip() || isSmallCraft();
    }

    /**
     * Returns true when this object is aerodyne. All fighters are aerodyne. Units
     * that have the aerodyne/spheroid distinction, i.e. DropShips and SmallCraft return true when they are
     * aerodyne.
     * Returns false for any type of unit group.
     * This method refers to {@link #isSpheroid()} and should not require overriding.
     *
     * @return True when this is object is aerodyne (fighter, aerodyne DropShip or aerodyne SmallCraft)
     */
    default boolean isAerodyne() {
        return isFighter() || (hasAerodyneSpheroidDistinction() && !isSpheroid());
    }

    /**
     * Returns true when this object has the distinction between aerodyne and spheroid, i.e. if it
     * is a DropShip or SmallCraft and it is spheroid, false for any other type of object.
     * Returns false for any type of unit group and for any unit that does not have the distinction.
     *
     * @return True when this is object is spheroid
     */
    default boolean isSpheroid() {
        return false;
    }

    /**
     * Returns true when this object is a Support Vehicle of any kind (including ground and aerospace).
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a Support Vehicle
     */
    default boolean isSupportVehicle() {
        return false;
    }

    /**
     * Returns true when this object is any Infantry unit or of type CI/BA for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is an Infantry unit (BattleArmor and Conventional)
     */
    default boolean isInfantry() {
        return isConventionalInfantry() || isBattleArmor();
    }

    /**
     * Returns true when this object is a Combat Vehicle or ground Support Vehicle (including VTOL) or of
     * type CV, ground SV for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a ground vehicle (including support vehicle and VTOL)
     */
    default boolean isVehicle() {
        return isGround() && (isCombatVehicle() || isSupportVehicle());
    }

    /**
     * Returns true when this object is a ground Combat Vehicle, including VTOL but not including Support Vehicles,
     * or of type CV for Alpha Strike.
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this is a ground Combat Vehicle (not including Support Vehicle)
     */
    default boolean isCombatVehicle() {
        return false;
    }

    /**
     * For future reference.
     * Returns true when this object is a battlefield object (such as a crate) that can be picked up and carried by
     * some types of units.
     *
     * @return True when this is a carryable battlefield object.
     */
    default boolean isCarryableObject() {
        return false;
    }

    /**
     * For future reference.
     * Returns true when this object is an objective marker, marking a certain hex or building or other object as
     * valuable to conquer, hold or destroy.
     *
     * @return True when this is an objective marker.
     */
    default boolean isObjectiveMarker() {
        return false;
    }

    /**
     * Returns true when this object uses or can use aerospace movement. This includes all aerospace units as
     * well as LAMs (in fighter mode when in a TW game).
     * Returns false for any type of unit group even if it is of the right type.
     *
     * @return True when this may use aerospace movement (aerospace and LAM units)
     */
    default boolean isAero() {
        return false;
    }

    /**
     * Returns true when this is a group of units or elements such as a TW Squadron, BF Unit or SBF Formation
     * even if it happens to contain only a single element at the time.
     *
     * @return True when this is a group type unit
     */
    default boolean isUnitGroup() {
        return false;
    }

    /**
     * Returns true when this is a single unit such as a TW Entity or AlphaStrikeElement, false when it is a
     * group unit type, see {@link #isUnitGroup()}
     * This method fowards to {@link #isUnitGroup()} and should not require overriding.
     *
     * @return True when this is a single unit or element.
     */
    default boolean isSingleUnit() {
        return !isUnitGroup();
    }
}