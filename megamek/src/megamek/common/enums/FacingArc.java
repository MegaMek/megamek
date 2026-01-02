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
package megamek.common.enums;

import java.util.function.Function;

import megamek.common.board.Coords;
import megamek.common.units.Targetable;
import megamek.common.units.UnitPosition;

/**
 * All the different possible facing arcs
 *
 * @author Luana Coppio
 */
public enum FacingArc {
    ARC_360(0, 0, 360, arc -> true),
    ARC_FORWARD(1, 300, 60, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_LEFT_ARM(2, 240, 60, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_RIGHT_ARM(3, 300, 120, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_REAR(4, 120, 240, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_SIDE(5, 240, 300, arc -> ((arc.target() >= arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_SIDE(6, 60, 120, arc -> ((arc.target() > arc.start()) && (arc.target() <= arc.end()))),
    ARC_MAIN_GUN(7, 240, 120, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_NORTH(8, 270, 30, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_EAST(9, 30, 150, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_WEST(10, 150, 270, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_NOSE(11, 300, 60, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_LEFT_WING(12, 300, 0, arc -> ((arc.target() > arc.start()) || (arc.target() <= arc.end()))),
    ARC_RIGHT_WING(13, 0, 60, arc -> ((arc.target() >= arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_WING_AFT(14, 180, 240, arc -> ((arc.target() >= arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_WING_AFT(15, 120, 180, arc -> ((arc.target() > arc.start()) && (arc.target() <= arc.end()))),
    ARC_LEFT_SIDE_SPHERE(16, 240, 0, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_RIGHT_SIDE_SPHERE(17, 0, 120, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_SIDE_AFT_SPHERE(18, 180, 300, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_SIDE_AFT_SPHERE(19, 60, 180, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_BROADSIDE(20, 240, 300, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_RIGHT_BROADSIDE(21, 60, 120, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_AFT(22, 120, 240, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_SPHERE_GROUND(23, 180, 360, arc -> ((arc.target() >= arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_SPHERE_GROUND(24, 0, 180, arc -> ((arc.target() >= arc.start()) && (arc.target() < arc.end()))),
    ARC_TURRET(25, 330, 30, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_SPONSON_TURRET_LEFT(26, 180, 0, arc -> ((arc.target() >= arc.start()) || (arc.target() == arc.end()))),
    ARC_SPONSON_TURRET_RIGHT(27, 0, 180, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_PINTLE_TURRET_LEFT(28, 180, 0, arc -> ((arc.target() >= arc.start()) || (arc.target() == arc.end()))),
    ARC_PINTLE_TURRET_RIGHT(29, 0, 180, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_PINTLE_TURRET_FRONT(30, 270, 90, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end()))),
    ARC_PINTLE_TURRET_REAR(31, 90, 270, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_VGL_FRONT(32, 270, 90, arc -> (arc.target() >= arc.start()) || (arc.target() <= arc.end())),
    ARC_VGL_RF(33, 330, 150, arc -> (arc.target() >= arc.start()) || (arc.target() <= arc.end())),
    ARC_VGL_RR(34, 30, 210, arc -> (arc.target() >= arc.start()) && (arc.target() <= arc.end())),
    ARC_VGL_REAR(35, 90, 270, arc -> (arc.target() >= arc.start()) && (arc.target() <= arc.end())),
    ARC_VGL_LR(36, 150, 330, arc -> (arc.target() >= arc.start()) && (arc.target() <= arc.end())),
    ARC_VGL_LF(37, 210, 30, arc -> (arc.target() >= arc.start()) || (arc.target() <= arc.end())),

    // Expanded arcs for Waypoint Launched Capital Missiles
    ARC_NOSE_WPL(38, 240, 120, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_LEFT_WING_WPL(39, 240, 60, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_RIGHT_WING_WPL(40, 300, 120, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_LEFT_WING_AFT_WPL(41, 120, 300, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_WING_AFT_WPL(42, 60, 240, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_SIDE_SPHERE_WPL(43, 180, 60, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_RIGHT_SIDE_SPHERE_WPL(44, 300, 180, arc -> ((arc.target() > arc.start()) || (arc.target() < arc.end()))),
    ARC_LEFT_SIDE_AFT_SPHERE_WPL(45, 120, 360, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_RIGHT_SIDE_AFT_SPHERE_WPL(46, 0, 240, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_AFT_WPL(47, 60, 300, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),
    ARC_LEFT_BROADSIDE_WPL(48, 180, 360, arc -> ((arc.target() > arc.start()) && (arc.target() <= arc.end()))),
    ARC_RIGHT_BROADSIDE_WPL(49, 0, 180, arc -> ((arc.target() > arc.start()) && (arc.target() < arc.end()))),

    // Some additional arcs for buildings!
    ARC_BLDG_FR(50, 0, 120, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_BLDG_RR(51, 60, 180, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_BLDG_R(52, 120, 240, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_BLDG_RL(53, 180, 300, arc -> ((arc.target() >= arc.start()) && (arc.target() <= arc.end()))),
    ARC_BLDG_FL(54, 240, 0, arc -> ((arc.target() >= arc.start()) || (arc.target() <= arc.end())));

    private final int arcCode;
    private final int startAngle;
    private final int endAngle;
    private final Function<ArcTarget, Boolean> function;

    /**
     * Parameter record for calculating if the target is inside the arc
     *
     * @param start  start angle
     * @param end    end angle
     * @param target target angle
     */
    private record ArcTarget(int start, int end, int target) {}

    FacingArc(int arcCode, int startAngle, int endAngle, Function<ArcTarget, Boolean> function) {
        this.arcCode = arcCode;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.function = function;
    }

    public int getArcCode() {
        return arcCode;
    }

    public int getStartAngle() {
        return startAngle;
    }

    public int getEndAngle() {
        return endAngle;
    }

    public boolean isInsideArc(int angle) {
        return this.function.apply(new ArcTarget(startAngle, endAngle, angle));
    }

    public boolean isInsideArc(Coords source, int facing, Targetable target) {
        if (source == null || target == null) {
            return true;
        }
        return isInsideArc(UnitPosition.of(source, facing), UnitPosition.of(target));
    }

    public boolean isInsideArc(UnitPosition source, UnitPosition target) {
        if (source == null || target == null) {
            return true;
        }

        for (var sourceCoords : source.getCoords()) {
            for (var targetCoords : target.getCoords()) {
                if (this.isInsideArc(source.relativeDotProduct(sourceCoords, targetCoords))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the enum constant of this type with the legacy specified arcCode. The arcCode must match exactly an
     * arcCode used to declare an enum constant in this type.
     *
     * @return the enum constant with the specified legacy arcCode
     *
     * @throws IllegalArgumentException – if this enum type has no constant with the specified arcCode
     */
    public static FacingArc valueOf(int arcCode) {
        for (FacingArc facingArc : FacingArc.values()) {
            if (facingArc.arcCode == arcCode) {
                return facingArc;
            }
        }
        throw new IllegalArgumentException("FacingArc has no constant with arcCode " + arcCode);
    }
}
