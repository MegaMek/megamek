/*
 * Copyright (C) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.preference.PreferenceManager;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This enum represents the various Entity Movement Types
 */
public enum EntityMovementMode {
    //region Enum Declarations
    NONE("EntityMovementMode.NONE.text"),
    BIPED("EntityMovementMode.BIPED.text"),
    TRIPOD("EntityMovementMode.TRIPOD.text"),
    QUAD("EntityMovementMode.QUAD.text"),
    TRACKED("EntityMovementMode.TRACKED.text"),
    WHEELED("EntityMovementMode.WHEELED.text"),
    HOVER("EntityMovementMode.HOVER.text"),
    VTOL("EntityMovementMode.VTOL.text"),
    NAVAL("EntityMovementMode.NAVAL.text"),
    HYDROFOIL("EntityMovementMode.HYDROFOIL.text"),
    SUBMARINE("EntityMovementMode.SUBMARINE.text"),
    INF_LEG("EntityMovementMode.INF_LEG.text"),
    INF_MOTORIZED("EntityMovementMode.INF_MOTORIZED.text"),
    INF_JUMP("EntityMovementMode.INF_JUMP.text"),
    BIPED_SWIM("EntityMovementMode.BIPED_SWIM.text"),
    QUAD_SWIM("EntityMovementMode.QUAD_SWIM.text"),
    WIGE("EntityMovementMode.WIGE.text"),
    AERODYNE("EntityMovementMode.AERODYNE.text"),
    SPHEROID("EntityMovementMode.SPHEROID.text"),
    INF_UMU("EntityMovementMode.INF_UMU.text"),
    AEROSPACE("EntityMovementMode.AEROSPACE.text"), // this might be a synonym for AERODYNE.
    AIRSHIP("EntityMovementMode.AIRSHIP.text"),
    STATION_KEEPING("EntityMovementMode.STATION_KEEPING.text"),
    RAIL("EntityMovementMode.RAIL.text"),
    MAGLEV("EntityMovementMode.MAGLEV.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
            PreferenceManager.getClientPreferences().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    EntityMovementMode(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isBiped() {
        return this == BIPED;
    }

    public boolean isTripod() {
        return this == TRIPOD;
    }

    public boolean isQuad() {
        return this == QUAD;
    }

    public boolean isTracked() {
        return this == TRACKED;
    }

    public boolean isWheeled() {
        return this == WHEELED;
    }

    public boolean isHover() {
        return this == HOVER;
    }

    public boolean isVTOL() {
        return this == VTOL;
    }

    public boolean isNaval() {
        return this == NAVAL;
    }

    public boolean isHydrofoil() {
        return this == HYDROFOIL;
    }

    public boolean isSubmarine() {
        return this == SUBMARINE;
    }

    public boolean isLegInfantry() {
        return this == INF_LEG;
    }

    public boolean isMotorizedInfantry() {
        return this == INF_MOTORIZED;
    }

    public boolean isJumpInfantry() {
        return this == INF_JUMP;
    }

    public boolean isBipedSwim() {
        return this == BIPED_SWIM;
    }

    public boolean isQuadSwim() {
        return this == QUAD_SWIM;
    }

    public boolean isWiGE() {
        return this == WIGE;
    }

    public boolean isAerodyne() {
        return this == AERODYNE;
    }

    public boolean isSpheroid() {
        return this == SPHEROID;
    }

    public boolean isUMUInfantry() {
        return this == INF_UMU;
    }

    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    public boolean isAirship() {
        return this == AIRSHIP;
    }

    public boolean isStationKeeping() {
        return this == STATION_KEEPING;
    }

    public boolean isRail() {
        return this == RAIL;
    }

    public boolean isMaglev() {
        return this == MAGLEV;
    }

    public boolean isTrackedOrWheeled() {
        return isTracked() || isWheeled();
    }

    public boolean isTrackedWheeledOrHover() {
        return isTrackedOrWheeled() || isHover();
    }

    public boolean isCombatVehicle() {
        return isTrackedWheeledOrHover() || isVTOLOrWiGE() || isMarine();
    }

    public boolean isInfantryVehicle() {
        return isTrackedOrWheeled() || isMotorizedInfantry();
    }

    public boolean isHoverOrWiGE() {
        return isHover() || isWiGE();
    }

    public boolean isVTOLOrWiGE() {
        return isVTOL() || isWiGE();
    }

    public boolean isMarine() {
        return isNaval() || isHydrofoil() || isSubmarine();
    }

    public boolean isTrain() {
        return isRail() || isMaglev();
    }

    /**
     * Whether this movement mode is capable of detonating minefields.
     */
    public boolean detonatesGroundMinefields() {
        return isBiped() || isTripod() || isQuad() || isTrackedWheeledOrHover() || isLegInfantry()
                || isMotorizedInfantry() || isJumpInfantry() || isTrain();
    }
    //endregion Boolean Comparisons

    public static List<EntityMovementMode> getCombatVehicleModes() {
        return Stream.of(values()).filter(EntityMovementMode::isCombatVehicle).collect(Collectors.toList());
    }

    public static List<EntityMovementMode> getInfantryVehicleModes() {
        return Stream.of(values()).filter(EntityMovementMode::isInfantryVehicle).collect(Collectors.toList());
    }

    //region File I/O
    /**
     * @param text the string to parse
     * @return the EntityMovementMode, or NONE if there is an error in parsing
     */
    public static EntityMovementMode parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Splitting this off the baseline as it is a legacy call, the text should be saved uppercase
        try {
            return valueOf(text.toUpperCase());
        } catch (Exception ignored) {

        }

        try {
            switch (text.toLowerCase()) {
                case "building":
                    return NONE;
                case "microcopter":
                case "micro-copter":
                case "microlite":
                    return VTOL;
                case "leg":
                    return INF_LEG;
                case "motorized":
                    return INF_MOTORIZED;
                case "jump":
                    return INF_JUMP;
                case "glider":
                    return WIGE;
                case "umu":
                case "scuba":
                case "motorized scuba":
                    return INF_UMU;
                case "station":
                case "station_keeping":
                case "satellite":
                case "station-keeping":
                    return STATION_KEEPING;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into an EntityMovementMode. Returning NONE.");

        return NONE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
