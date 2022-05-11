/*
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
package megamek.client.ui.minimap;

import java.awt.Dimension;
import java.awt.geom.Path2D;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import static java.lang.Math.*;

/** Contains geometric paths for drawing the minimap unit symbols. */
public class MinimapUnitSymbols {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    public static final Path2D STRAT_BASERECT;
    public static final Path2D STRAT_INFANTRY;
    public static final Path2D STRAT_MECH;
    public static final Path2D STRAT_VTOL;
    public static final Path2D STRAT_TANKTRACKED;
    public static final Path2D STRAT_AERO;
    public static final Path2D STRAT_SPHEROID;
    public static final Path2D STRAT_HOVER;
    public static final Path2D STRAT_WHEELED;
    public static final Path2D STRAT_NAVAL;
    public static final Path2D STD_MECH;
    public static final Path2D STD_TANK;
    public static final Path2D STD_VTOL;
    public static final Path2D STD_AERO;
    public static final Path2D STD_INFANTRY;
    public static final Path2D STD_MECHWARRIOR;
    public static final Path2D STD_NAVAL;
    public static final Path2D STD_SPHEROID;
    public static final Dimension STRAT_SYMBOLSIZE = new Dimension(167, 103);
    public static final Dimension STD_SYMBOLSIZE = new Dimension(100, 100);
    public static final double STRAT_CX = STRAT_SYMBOLSIZE.getWidth() / 5; // X center for two symbols
    
    private static final double PIHALF = PI / 2;
    
    static {
        STD_MECH = new Path2D.Double();
        STD_MECH.moveTo(-25,  45);
        STD_MECH.lineTo( 25,  45);
        STD_MECH.lineTo( 45, -45);
        STD_MECH.lineTo(-45, -45);
        STD_MECH.closePath();
        
        STD_TANK = new Path2D.Double();
        STD_TANK.moveTo(-25, -50);
        STD_TANK.lineTo( 25, -50);
        STD_TANK.lineTo( 25,  50);
        STD_TANK.lineTo(-25,  50);
        STD_TANK.closePath();
        
        STD_NAVAL = new Path2D.Double();
        STD_NAVAL.moveTo(-15, -50);
        STD_NAVAL.lineTo( 15, -50);
        STD_NAVAL.curveTo(25,  0,  25,  0,  15,  50);
        STD_NAVAL.lineTo(-15,  50);
        STD_NAVAL.curveTo(-25, 0, -25,  0, -15, -50);
        STD_NAVAL.closePath();
        
        STD_VTOL = new Path2D.Double();
        int ofs = 10;
        STD_VTOL.moveTo(-50,   0);
        STD_VTOL.curveTo(-ofs,  ofs, -ofs,  ofs,   0,  50);
        STD_VTOL.curveTo( ofs,  ofs,  ofs,  ofs,  50,   0);
        STD_VTOL.curveTo( ofs, -ofs,  ofs, -ofs,   0, -50);
        STD_VTOL.curveTo(-ofs, -ofs, -ofs, -ofs, -50,   0);
        STD_VTOL.closePath();
        
        STD_AERO = new Path2D.Double();
        STD_AERO.moveTo(-30,  40);
        STD_AERO.lineTo(  0, -50);
        STD_AERO.lineTo( 30,  40);
        STD_AERO.lineTo( 30,  50);
        STD_AERO.lineTo(-30,  50);
        STD_AERO.closePath();
        
        STD_SPHEROID = new Path2D.Double();
        double rad = 50;
        double r72 = toRadians(72);
        STD_SPHEROID.moveTo(rad * cos(PIHALF),           -rad * sin(PIHALF));
        STD_SPHEROID.lineTo(rad * cos(PIHALF + r72),     -rad * sin(PIHALF + r72));
        STD_SPHEROID.lineTo(rad * cos(PIHALF + 2 * r72), -rad * sin(PIHALF + 2 * r72));
        STD_SPHEROID.lineTo(rad * cos(PIHALF + 3 * r72), -rad * sin(PIHALF + 3 * r72));
        STD_SPHEROID.lineTo(rad * cos(PIHALF + 4 * r72), -rad * sin(PIHALF + 4 * r72));
        STD_SPHEROID.closePath();
        
        STD_INFANTRY = new Path2D.Double();
        STD_INFANTRY.moveTo(-50,   0);
        STD_INFANTRY.curveTo(0,  20,  0,  20,  50,   0);
        STD_INFANTRY.curveTo(0, -20,  0, -20, -50,   0);
        STD_INFANTRY.closePath();
        
        STD_MECHWARRIOR = new Path2D.Double();
        STD_MECHWARRIOR.moveTo(-30,   0);
        STD_MECHWARRIOR.curveTo(0,  15,  0,  15,  30,   0);
        STD_MECHWARRIOR.curveTo(0, -15,  0, -15, -30,   0);
        STD_MECHWARRIOR.closePath();
        
        // Base rectangle for all units (StratOps)
        STRAT_BASERECT = new Path2D.Double();
        STRAT_BASERECT.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 2, -STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_BASERECT.lineTo( STRAT_SYMBOLSIZE.getWidth() / 2, -STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_BASERECT.lineTo( STRAT_SYMBOLSIZE.getWidth() / 2,  STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_BASERECT.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 2,  STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_BASERECT.closePath();
        
        // Infantry Symbol
        STRAT_INFANTRY = new Path2D.Double();
        STRAT_INFANTRY.append(STRAT_BASERECT, false);
        STRAT_INFANTRY.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 2, -STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_INFANTRY.lineTo( STRAT_SYMBOLSIZE.getWidth() / 2,  STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_INFANTRY.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 2,  STRAT_SYMBOLSIZE.getHeight() / 2);
        STRAT_INFANTRY.lineTo( STRAT_SYMBOLSIZE.getWidth() / 2, -STRAT_SYMBOLSIZE.getHeight() / 2);
        
        STRAT_VTOL = new Path2D.Double();
        STRAT_VTOL.append(STRAT_BASERECT, false);
        STRAT_VTOL.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 4, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_VTOL.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 4,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_VTOL.lineTo( 0,  0);
        STRAT_VTOL.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 4, -STRAT_SYMBOLSIZE.getHeight() / 4);
        
        STRAT_VTOL.moveTo( STRAT_SYMBOLSIZE.getWidth() / 4,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_VTOL.lineTo( STRAT_SYMBOLSIZE.getWidth() / 4, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_VTOL.lineTo( 0, 0);
        STRAT_VTOL.closePath();
        
        STRAT_TANKTRACKED = new Path2D.Double();
        STRAT_TANKTRACKED.append(STRAT_BASERECT, false);
        double small = STRAT_SYMBOLSIZE.getWidth() / 20; 
        STRAT_TANKTRACKED.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 3 + small, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_TANKTRACKED.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3 - small, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_TANKTRACKED.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3,       -STRAT_SYMBOLSIZE.getHeight() / 4 + small);
        STRAT_TANKTRACKED.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3,        STRAT_SYMBOLSIZE.getHeight() / 4 - small);
        STRAT_TANKTRACKED.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3 - small,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_TANKTRACKED.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 3 + small,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_TANKTRACKED.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 3,        STRAT_SYMBOLSIZE.getHeight() / 4 - small);
        STRAT_TANKTRACKED.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 3,       -STRAT_SYMBOLSIZE.getHeight() / 4 + small);
        STRAT_TANKTRACKED.closePath();
        
        STRAT_MECH = new Path2D.Double();
        STRAT_MECH.append(STRAT_BASERECT, false);
        
        STRAT_MECH.moveTo(-STRAT_CX - 1.5 * small, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_MECH.lineTo(-STRAT_CX - 3.0 * small,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_MECH.lineTo(-STRAT_CX + 3.0 * small,  STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_MECH.lineTo(-STRAT_CX + 1.5 * small, -STRAT_SYMBOLSIZE.getHeight() / 4);
        STRAT_MECH.closePath();
        
        STRAT_AERO = new Path2D.Double();
        STRAT_AERO.append(STRAT_BASERECT, false);
        rad = STRAT_SYMBOLSIZE.getWidth() / 5;
        
        STRAT_AERO.moveTo(-STRAT_CX + rad / 3 * cos(PIHALF + 2 * r72), rad / 3 * sin(PIHALF + 2 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PIHALF + 1 * r72), -rad * sin(PIHALF + 1 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PIHALF + 1 * r72), rad / 3 * sin(PIHALF + 1 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PIHALF + 2 * r72), -rad * sin(PIHALF + 2 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PIHALF), rad / 3 * sin(PIHALF));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PIHALF + 3 * r72), -rad * sin(PIHALF + 3 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PIHALF + 4 * r72), rad / 3 * sin(PIHALF + 4 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PIHALF + 4 * r72), -rad * sin(PIHALF + 4 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad / 3 * cos(PIHALF + 3 * r72), rad / 3 * sin(PIHALF + 3 * r72));
        STRAT_AERO.lineTo(-STRAT_CX + rad * cos(PIHALF),       -rad * sin(PIHALF));
        STRAT_AERO.closePath();
        
        STRAT_SPHEROID = new Path2D.Double();
        STRAT_SPHEROID.append(STRAT_BASERECT, false);
        STRAT_SPHEROID.moveTo(rad * cos(PIHALF),           -rad * sin(PIHALF));
        STRAT_SPHEROID.lineTo(rad * cos(PIHALF + r72),     -rad * sin(PIHALF + r72));
        STRAT_SPHEROID.lineTo(rad * cos(PIHALF + 2 * r72), -rad * sin(PIHALF + 2 * r72));
        STRAT_SPHEROID.lineTo(rad * cos(PIHALF + 3 * r72), -rad * sin(PIHALF + 3 * r72));
        STRAT_SPHEROID.lineTo(rad * cos(PIHALF + 4 * r72), -rad * sin(PIHALF + 4 * r72));
        STRAT_SPHEROID.closePath();
        
        STRAT_HOVER = new Path2D.Double();
        STRAT_HOVER.append(STRAT_BASERECT, false);
        STRAT_HOVER.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 3,  small);
        STRAT_HOVER.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 3, -small);
        STRAT_HOVER.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3, -small);
        STRAT_HOVER.lineTo( STRAT_SYMBOLSIZE.getWidth() / 3,  small);

        STRAT_HOVER.moveTo(-STRAT_SYMBOLSIZE.getWidth() / 6, -small);
        STRAT_HOVER.lineTo(-STRAT_SYMBOLSIZE.getWidth() / 6, +small);
        STRAT_HOVER.moveTo(0, -small);
        STRAT_HOVER.lineTo(0, +small);
        STRAT_HOVER.moveTo( STRAT_SYMBOLSIZE.getWidth() / 6, -small);
        STRAT_HOVER.lineTo( STRAT_SYMBOLSIZE.getWidth() / 6, +small);
        
        STRAT_WHEELED = new Path2D.Double();
        STRAT_WHEELED.append(STRAT_BASERECT, false);
        double smallr = STRAT_SYMBOLSIZE.getWidth()/17;
        STRAT_WHEELED.moveTo(-STRAT_CX - smallr * 2, -smallr);
        STRAT_WHEELED.lineTo(+STRAT_CX + smallr * 2, -smallr);
        STRAT_WHEELED.moveTo(-STRAT_CX, -smallr);
        STRAT_WHEELED.lineTo(-STRAT_CX - smallr, 0);
        STRAT_WHEELED.lineTo(-STRAT_CX, +smallr);
        STRAT_WHEELED.lineTo(-STRAT_CX + smallr, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo( STRAT_CX, -smallr);
        STRAT_WHEELED.lineTo( STRAT_CX - smallr, 0);
        STRAT_WHEELED.lineTo( STRAT_CX, +smallr);
        STRAT_WHEELED.lineTo( STRAT_CX + smallr, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo(0, -smallr);
        STRAT_WHEELED.lineTo(-smallr, 0);
        STRAT_WHEELED.lineTo(0, +smallr);
        STRAT_WHEELED.lineTo(smallr, 0);
        STRAT_WHEELED.closePath();
        
        STRAT_NAVAL = new Path2D.Double();
        STRAT_NAVAL.append(STRAT_BASERECT, false);
        STRAT_NAVAL.moveTo(0, -STRAT_SYMBOLSIZE.getHeight() / 3);
        STRAT_NAVAL.lineTo(0,  STRAT_SYMBOLSIZE.getHeight() / 3);
        STRAT_NAVAL.moveTo(-STRAT_CX / 2, -STRAT_SYMBOLSIZE.getHeight() / 5);
        STRAT_NAVAL.lineTo( STRAT_CX / 2, -STRAT_SYMBOLSIZE.getHeight() / 5);
        
        STRAT_NAVAL.moveTo(rad, 0);
        STRAT_NAVAL.curveTo(
                rad*0.8, STRAT_SYMBOLSIZE.getHeight() / 3 * 0.8,
                rad*0.8, STRAT_SYMBOLSIZE.getHeight() / 3 * 0.8,
                0, STRAT_SYMBOLSIZE.getHeight() / 3);
        STRAT_NAVAL.curveTo(
                -rad*0.8, STRAT_SYMBOLSIZE.getHeight() / 3 * 0.8,
                -rad*0.8, STRAT_SYMBOLSIZE.getHeight() / 3 * 0.8,
                -rad, 0);
    }
    
    /** Returns the Path2D minimap symbol shape for the given entity. */
    public static Path2D getForm(Entity entity) {
        boolean stratOps = GUIP.getBoolean(GUIPreferences.MMSYMBOL);
        
        if ((entity instanceof Mech) || (entity instanceof Protomech)) {
            return stratOps ? STRAT_MECH : STD_MECH;
        } else if (entity instanceof VTOL) {
            return stratOps ? STRAT_VTOL : STD_VTOL;
        } else if (entity instanceof MechWarrior) {
            return stratOps ? STRAT_INFANTRY : STD_MECHWARRIOR;
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode() == EntityMovementMode.HOVER) {
                return stratOps ? STRAT_HOVER : STD_TANK;
            } else if (entity.getMovementMode() == EntityMovementMode.WHEELED) {
                return stratOps ? STRAT_WHEELED : STD_TANK;
            } else if ((entity.getMovementMode() == EntityMovementMode.HYDROFOIL) ||
                    (entity.getMovementMode() == EntityMovementMode.NAVAL)) {
                return stratOps ? STRAT_NAVAL : STD_NAVAL; 
            } else {
                return stratOps ? STRAT_TANKTRACKED : STD_TANK;
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
