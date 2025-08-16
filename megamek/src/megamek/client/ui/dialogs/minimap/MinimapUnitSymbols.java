/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.minimap;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import java.awt.Dimension;
import java.awt.geom.Path2D;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Mek;
import megamek.common.MekWarrior;
import megamek.common.ProtoMek;
import megamek.common.Tank;
import megamek.common.VTOL;

/** Contains geometric paths for drawing the minimap unit symbols. */
public class MinimapUnitSymbols {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    public static final Path2D STRAT_BASE_RECT;
    public static final Path2D STRAT_INFANTRY;
    public static final Path2D STRAT_MEK;
    public static final Path2D STRAT_VTOL;
    public static final Path2D STRAT_TANK_TRACKED;
    public static final Path2D STRAT_AERO;
    public static final Path2D STRAT_SPHEROID;
    public static final Path2D STRAT_HOVER;
    public static final Path2D STRAT_WHEELED;
    public static final Path2D STRAT_NAVAL;
    public static final Path2D STRAT_DESTROYED;
    public static final Path2D FACING_ARROW;
    public static final Path2D STD_MEK;
    public static final Path2D STD_TANK;
    public static final Path2D STD_VTOL;
    public static final Path2D STD_AERO;
    public static final Path2D STD_INFANTRY;
    public static final Path2D STD_DESTROYED;
    public static final Path2D STD_MEKWARRIOR;
    public static final Path2D STD_NAVAL;
    public static final Path2D STD_SPHEROID;
    public static final Dimension STRAT_SYMBOL_SIZE = new Dimension(167, 103);
    public static final Dimension STD_SYMBOL_SIZE = new Dimension(100, 100);
    public static final double STRAT_CX = STRAT_SYMBOL_SIZE.getWidth() / 5; // X center for two symbols

    private static final double PI_HALF = PI / 2;

    static {
        FACING_ARROW = new Path2D.Double();
        FACING_ARROW.moveTo(0, -130);
        FACING_ARROW.lineTo(25, -130);
        FACING_ARROW.lineTo(0, -180);
        FACING_ARROW.lineTo(-25, -130);
        FACING_ARROW.closePath();

        STD_MEK = new Path2D.Double();
        STD_MEK.moveTo(-25, 45);
        STD_MEK.lineTo(25, 45);
        STD_MEK.lineTo(45, -45);
        STD_MEK.lineTo(-45, -45);
        STD_MEK.closePath();

        STD_TANK = new Path2D.Double();
        STD_TANK.moveTo(-25, -50);
        STD_TANK.lineTo(25, -50);
        STD_TANK.lineTo(25, 50);
        STD_TANK.lineTo(-25, 50);
        STD_TANK.closePath();

        STD_NAVAL = new Path2D.Double();
        STD_NAVAL.moveTo(-15, -50);
        STD_NAVAL.lineTo(15, -50);
        STD_NAVAL.curveTo(25, 0, 25, 0, 15, 50);
        STD_NAVAL.lineTo(-15, 50);
        STD_NAVAL.curveTo(-25, 0, -25, 0, -15, -50);
        STD_NAVAL.closePath();

        STD_VTOL = new Path2D.Double();
        int ofs = 10;
        STD_VTOL.moveTo(-50, 0);
        STD_VTOL.curveTo(-ofs, ofs, -ofs, ofs, 0, 50);
        STD_VTOL.curveTo(ofs, ofs, ofs, ofs, 50, 0);
        STD_VTOL.curveTo(ofs, -ofs, ofs, -ofs, 0, -50);
        STD_VTOL.curveTo(-ofs, -ofs, -ofs, -ofs, -50, 0);
        STD_VTOL.closePath();

        STD_AERO = new Path2D.Double();
        STD_AERO.moveTo(-30, 40);
        STD_AERO.lineTo(0, -50);
        STD_AERO.lineTo(30, 40);
        STD_AERO.lineTo(30, 50);
        STD_AERO.lineTo(-30, 50);
        STD_AERO.closePath();

        STD_SPHEROID = new Path2D.Double();
        double rad = 50;
        double r72 = toRadians(72);
        STD_SPHEROID.moveTo(rad * cos(PI_HALF), -rad * sin(PI_HALF));
        STD_SPHEROID.lineTo(rad * cos(PI_HALF + r72), -rad * sin(PI_HALF + r72));
        STD_SPHEROID.lineTo(rad * cos(PI_HALF + 2 * r72), -rad * sin(PI_HALF + 2 * r72));
        STD_SPHEROID.lineTo(rad * cos(PI_HALF + 3 * r72), -rad * sin(PI_HALF + 3 * r72));
        STD_SPHEROID.lineTo(rad * cos(PI_HALF + 4 * r72), -rad * sin(PI_HALF + 4 * r72));
        STD_SPHEROID.closePath();

        STD_INFANTRY = new Path2D.Double();
        STD_INFANTRY.moveTo(-50, 0);
        STD_INFANTRY.curveTo(0, 20, 0, 20, 50, 0);
        STD_INFANTRY.curveTo(0, -20, 0, -20, -50, 0);
        STD_INFANTRY.closePath();

        STD_DESTROYED = new Path2D.Double();
        STD_DESTROYED.moveTo(-70, 0);
        STD_DESTROYED.lineTo(70, 0);

        STD_MEKWARRIOR = new Path2D.Double();
        STD_MEKWARRIOR.moveTo(-30, 0);
        STD_MEKWARRIOR.curveTo(0, 15, 0, 15, 30, 0);
        STD_MEKWARRIOR.curveTo(0, -15, 0, -15, -30, 0);
        STD_MEKWARRIOR.closePath();

        // Base rectangle for all units (StratOps)
        STRAT_BASE_RECT = new Path2D.Double();
        STRAT_BASE_RECT.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 2, -STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_BASE_RECT.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 2, -STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_BASE_RECT.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 2, STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_BASE_RECT.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 2, STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_BASE_RECT.closePath();

        // Infantry Symbol
        STRAT_INFANTRY = new Path2D.Double();
        STRAT_INFANTRY.append(STRAT_BASE_RECT, false);
        STRAT_INFANTRY.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 2, -STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_INFANTRY.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 2, STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_INFANTRY.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 2, STRAT_SYMBOL_SIZE.getHeight() / 2);
        STRAT_INFANTRY.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 2, -STRAT_SYMBOL_SIZE.getHeight() / 2);

        STRAT_DESTROYED = new Path2D.Double();
        STRAT_DESTROYED.append(STRAT_BASE_RECT, false);
        STRAT_DESTROYED.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 2, 0);
        STRAT_DESTROYED.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 2, 0);


        STRAT_VTOL = new Path2D.Double();
        STRAT_VTOL.append(STRAT_BASE_RECT, false);
        STRAT_VTOL.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 4, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_VTOL.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 4, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_VTOL.lineTo(0, 0);
        STRAT_VTOL.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 4, -STRAT_SYMBOL_SIZE.getHeight() / 4);

        STRAT_VTOL.moveTo(STRAT_SYMBOL_SIZE.getWidth() / 4, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_VTOL.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 4, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_VTOL.lineTo(0, 0);
        STRAT_VTOL.closePath();

        STRAT_TANK_TRACKED = new Path2D.Double();
        STRAT_TANK_TRACKED.append(STRAT_BASE_RECT, false);
        double small = STRAT_SYMBOL_SIZE.getWidth() / 20;
        STRAT_TANK_TRACKED.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 3 + small, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_TANK_TRACKED.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3 - small, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_TANK_TRACKED.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3, -STRAT_SYMBOL_SIZE.getHeight() / 4 + small);
        STRAT_TANK_TRACKED.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3, STRAT_SYMBOL_SIZE.getHeight() / 4 - small);
        STRAT_TANK_TRACKED.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3 - small, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_TANK_TRACKED.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 3 + small, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_TANK_TRACKED.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 3, STRAT_SYMBOL_SIZE.getHeight() / 4 - small);
        STRAT_TANK_TRACKED.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 3, -STRAT_SYMBOL_SIZE.getHeight() / 4 + small);
        STRAT_TANK_TRACKED.closePath();

        STRAT_MEK = new Path2D.Double();
        STRAT_MEK.append(STRAT_BASE_RECT, false);

        STRAT_MEK.moveTo(-STRAT_CX - 1.5 * small, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_MEK.lineTo(-STRAT_CX - 3.0 * small, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_MEK.lineTo(-STRAT_CX + 3.0 * small, STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_MEK.lineTo(-STRAT_CX + 1.5 * small, -STRAT_SYMBOL_SIZE.getHeight() / 4);
        STRAT_MEK.closePath();

        STRAT_AERO = new Path2D.Double();
        STRAT_AERO.append(STRAT_BASE_RECT, false);
        rad = STRAT_SYMBOL_SIZE.getWidth() / 5;

        STRAT_AERO.moveTo(-STRAT_CX + rad / 3 * cos(PI_HALF + 2 * r72), rad / 3 * sin(PI_HALF + 2 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PI_HALF + 1 * r72), -rad * sin(PI_HALF + 1 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PI_HALF + 1 * r72), rad / 3 * sin(PI_HALF + 1 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PI_HALF + 2 * r72), -rad * sin(PI_HALF + 2 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PI_HALF), rad / 3 * sin(PI_HALF));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PI_HALF + 3 * r72), -rad * sin(PI_HALF + 3 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PI_HALF + 4 * r72), rad / 3 * sin(PI_HALF + 4 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PI_HALF + 4 * r72), -rad * sin(PI_HALF + 4 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PI_HALF + 3 * r72), rad / 3 * sin(PI_HALF + 3 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PI_HALF), -rad * sin(PI_HALF));
        STRAT_AERO.closePath();

        STRAT_SPHEROID = new Path2D.Double();
        STRAT_SPHEROID.append(STRAT_BASE_RECT, false);
        STRAT_SPHEROID.moveTo(rad * cos(PI_HALF), -rad * sin(PI_HALF));
        STRAT_SPHEROID.lineTo(rad * cos(PI_HALF + r72), -rad * sin(PI_HALF + r72));
        STRAT_SPHEROID.lineTo(rad * cos(PI_HALF + 2 * r72), -rad * sin(PI_HALF + 2 * r72));
        STRAT_SPHEROID.lineTo(rad * cos(PI_HALF + 3 * r72), -rad * sin(PI_HALF + 3 * r72));
        STRAT_SPHEROID.lineTo(rad * cos(PI_HALF + 4 * r72), -rad * sin(PI_HALF + 4 * r72));
        STRAT_SPHEROID.closePath();

        STRAT_HOVER = new Path2D.Double();
        STRAT_HOVER.append(STRAT_BASE_RECT, false);
        STRAT_HOVER.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 3, small);
        STRAT_HOVER.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 3, -small);
        STRAT_HOVER.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3, -small);
        STRAT_HOVER.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 3, small);

        STRAT_HOVER.moveTo(-STRAT_SYMBOL_SIZE.getWidth() / 6, -small);
        STRAT_HOVER.lineTo(-STRAT_SYMBOL_SIZE.getWidth() / 6, +small);
        STRAT_HOVER.moveTo(0, -small);
        STRAT_HOVER.lineTo(0, +small);
        STRAT_HOVER.moveTo(STRAT_SYMBOL_SIZE.getWidth() / 6, -small);
        STRAT_HOVER.lineTo(STRAT_SYMBOL_SIZE.getWidth() / 6, +small);

        STRAT_WHEELED = new Path2D.Double();
        STRAT_WHEELED.append(STRAT_BASE_RECT, false);
        double smaller = STRAT_SYMBOL_SIZE.getWidth() / 17;
        STRAT_WHEELED.moveTo(-STRAT_CX - smaller * 2, -smaller);
        STRAT_WHEELED.lineTo(STRAT_CX + smaller * 2, -smaller);
        STRAT_WHEELED.moveTo(-STRAT_CX, -smaller);
        STRAT_WHEELED.lineTo(-STRAT_CX - smaller, 0);
        STRAT_WHEELED.lineTo(-STRAT_CX, +smaller);
        STRAT_WHEELED.lineTo(-STRAT_CX + smaller, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo(STRAT_CX, -smaller);
        STRAT_WHEELED.lineTo(STRAT_CX - smaller, 0);
        STRAT_WHEELED.lineTo(STRAT_CX, +smaller);
        STRAT_WHEELED.lineTo(STRAT_CX + smaller, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo(0, -smaller);
        STRAT_WHEELED.lineTo(-smaller, 0);
        STRAT_WHEELED.lineTo(0, +smaller);
        STRAT_WHEELED.lineTo(smaller, 0);
        STRAT_WHEELED.closePath();

        STRAT_NAVAL = new Path2D.Double();
        STRAT_NAVAL.append(STRAT_BASE_RECT, false);
        STRAT_NAVAL.moveTo(0, -STRAT_SYMBOL_SIZE.getHeight() / 3);
        STRAT_NAVAL.lineTo(0, STRAT_SYMBOL_SIZE.getHeight() / 3);
        STRAT_NAVAL.moveTo(-STRAT_CX / 2, -STRAT_SYMBOL_SIZE.getHeight() / 5);
        STRAT_NAVAL.lineTo(STRAT_CX / 2, -STRAT_SYMBOL_SIZE.getHeight() / 5);

        STRAT_NAVAL.moveTo(rad, 0);
        STRAT_NAVAL.curveTo(
              rad * 0.8, STRAT_SYMBOL_SIZE.getHeight() / 3 * 0.8,
              rad * 0.8, STRAT_SYMBOL_SIZE.getHeight() / 3 * 0.8,
              0, STRAT_SYMBOL_SIZE.getHeight() / 3);
        STRAT_NAVAL.curveTo(
              -rad * 0.8, STRAT_SYMBOL_SIZE.getHeight() / 3 * 0.8,
              -rad * 0.8, STRAT_SYMBOL_SIZE.getHeight() / 3 * 0.8,
              -rad, 0);
    }

    /** Returns the Path2D minimap symbol shape for the given entity. */
    public static Path2D getForm(Entity entity) {
        boolean stratOps = GUIP.getMmSymbol();
        if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
            return stratOps ? STRAT_MEK : STD_MEK;
        } else if (entity instanceof VTOL) {
            return stratOps ? STRAT_VTOL : STD_VTOL;
        } else if (entity instanceof MekWarrior) {
            return stratOps ? STRAT_INFANTRY : STD_MEKWARRIOR;
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode() == EntityMovementMode.HOVER) {
                return stratOps ? STRAT_HOVER : STD_TANK;
            } else if (entity.getMovementMode() == EntityMovementMode.WHEELED) {
                return stratOps ? STRAT_WHEELED : STD_TANK;
            } else if ((entity.getMovementMode() == EntityMovementMode.HYDROFOIL) ||
                  (entity.getMovementMode() == EntityMovementMode.NAVAL)) {
                return stratOps ? STRAT_NAVAL : STD_NAVAL;
            } else {
                return stratOps ? STRAT_TANK_TRACKED : STD_TANK;
            }
        } else if (entity.isAero()) {
            if (entity.isFighter()) {
                return stratOps ? STRAT_AERO : STD_AERO;
            } else {
                return stratOps ? STRAT_SPHEROID : STD_SPHEROID;
            }
        } else {
            return stratOps ? STRAT_INFANTRY : STD_INFANTRY;
        }
    }
}
