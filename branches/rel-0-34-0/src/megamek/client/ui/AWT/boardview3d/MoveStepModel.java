/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * 
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.AWT.boardview3d;

import java.awt.Color;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.IEntityMovementMode;
import megamek.common.IEntityMovementType;
import megamek.common.IHex;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Tank;

class MoveStepModel extends ArrowModel {
    public MoveStepModel(MoveStep step, int count, IHex hex, ViewTransform currentView) {

        Color col;
        switch (step.getMovementType()) {
        case IEntityMovementType.MOVE_RUN:
        case IEntityMovementType.MOVE_VTOL_RUN:
            if (step.isUsingMASC()) {
                col = GUIPreferences.getInstance().getColor("AdvancedMoveMASCColor");
            } else {
                col = GUIPreferences.getInstance().getColor("AdvancedMoveRunColor");
            }
            break;
        case IEntityMovementType.MOVE_JUMP:
            col = GUIPreferences.getInstance().getColor("AdvancedMoveJumpColor");
            break;
        case IEntityMovementType.MOVE_ILLEGAL:
            col = GUIPreferences.getInstance().getColor("AdvancedMoveIllegalColor");
            break;
        default:
            if (step.getType() == MovePath.STEP_BACKWARDS) {
                col = GUIPreferences.getInstance().getColor("AdvancedMoveBackColor");
            } else {
                col = GUIPreferences.getInstance().getColor("AdvancedMoveDefaultColor");
            }
            break;
        }
        Color3f color = new Color3f(col);
        Appearance base = new Appearance();
        base.setMaterial(new Material(color, C.black, color, C.white, 64.0f));
        base.setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.SHADE_FLAT));
        base.setPolygonAttributes(C.noCull);
        base.setLineAttributes(C.defLine);
        
        Shape3D arrow = new Shape3D(polygon, base);
        Shape3D outline = new Shape3D(border, base);

        anim.addChild(arrow);
        anim.addChild(outline);
        String label = null;

        TransformGroup tg = new TransformGroup();
        tg.addChild(anim);
        
        Transform3D trans = new Transform3D();
        double centerOffset = 0.0;

        switch (step.getType()) {
        case MovePath.STEP_CLIMB_MODE_OFF:
            if (step.getParent().getEntity().getMovementMode() == IEntityMovementMode.WIGE) {
                label = Messages.getString("BoardView1.WIGEClimbOff"); //$NON-NLS-1$
            } else {
                label = Messages.getString("BoardView1.ClimbOff"); //$NON-NLS-1$
            }
            if (step.isPastDanger()) {
                label = "(" + label + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            anim.removeChild(arrow);
        case MovePath.STEP_GO_PRONE:
        case MovePath.STEP_HULL_DOWN:
        case MovePath.STEP_DOWN:
        case MovePath.STEP_DIG_IN:
        case MovePath.STEP_FORTIFY:
            trans.rotX(-Math.PI/2);
            break;
        case MovePath.STEP_CLIMB_MODE_ON:
            if (step.getParent().getEntity().getMovementMode() == IEntityMovementMode.WIGE) {
                label = Messages.getString("BoardView1.WIGEClimb"); //$NON-NLS-1$
            } else {
                label = Messages.getString("BoardView1.Climb"); //$NON-NLS-1$
            }
            if (step.isPastDanger()) {
                label = "(" + label + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            anim.removeChild(arrow);
        case MovePath.STEP_GET_UP:
        case MovePath.STEP_UP:
            trans.rotX(Math.PI/2);
            break;

        case MovePath.STEP_LOAD:
            tg.removeChild(anim);
            label = Messages.getString("BoardView1.Load"); //$NON-NLS-1$
            if (step.isPastDanger()) {
                label = "(" + label + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            break;
        case MovePath.STEP_UNLOAD:
            tg.removeChild(anim);
            label = Messages.getString("BoardView1.Unload"); //$NON-NLS-1$
            if (step.isPastDanger()) {
                label = "(" + label + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            break;
        
        case MovePath.STEP_TURN_LEFT:
        case MovePath.STEP_TURN_RIGHT:
            anim.removeChild(arrow);
            centerOffset = BoardModel.HEX_DIAMETER/6;
            trans.rotZ(-Math.PI/3*step.getFacing());
            break;
        default:
            centerOffset = -BoardModel.HEX_DIAMETER/3;
            trans.rotZ(-Math.PI/3*step.getFacing());
        }
        Vector3d translate = new Vector3d(0.0, centerOffset, -BoardModel.HEX_HEIGHT-count*.1);
        trans.transform(translate);
        trans.setTranslation(translate);
        
        if (label == null) {
            StringBuffer costStringBuf = new StringBuffer();
            costStringBuf.append(step.getMpUsed());

            if (step.isOnlyPavement() && step.getParent().getEntity() instanceof Tank) costStringBuf.append("+"); //$NON-NLS-1$

            if (step.isDanger()) costStringBuf.append("*"); //$NON-NLS-1$

            if (step.isPastDanger()) {
                costStringBuf.insert(0, "("); //$NON-NLS-1$
                costStringBuf.append(")"); //$NON-NLS-1$
            }

            if (step.isUsingMASC()) {
                costStringBuf.append("["); //$NON-NLS-1$
                costStringBuf.append(step.getTargetNumberMASC());
                costStringBuf.append("+]"); //$NON-NLS-1$
            }

            if (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK
                    || step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN
                    || step.getMovementType() == IEntityMovementType.MOVE_SUBMARINE_WALK
                    || step.getMovementType() == IEntityMovementType.MOVE_SUBMARINE_RUN
                    || step.getElevation() != 0) {
                costStringBuf.append("{").append(step.getElevation()).append("}");
            }

            label = costStringBuf.toString();
        }

        tg.setTransform(trans);

        Vector3d loc = new Vector3d(BoardModel.getHexLocation(step.getPosition(), step.getElevation()+hex.getElevation()+1));
        loc.z += BoardModel.HEX_HEIGHT + count*.1;

        TransformGroup l = new TransformGroup(new Transform3D(C.nullRot, loc, 1.0));
        l.addChild(currentView.makeViewRelative(new LabelModel(label, C.black, color, LabelModel.BIGBOLD), count*.1));
        
        l.addChild(tg);

        addChild(l);
    }


}
