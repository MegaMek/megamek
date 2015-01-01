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

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;
import megamek.client.ui.AWT.TilesetManager;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.actions.ArtilleryAttackAction;

/**
 *
 * @author jwalt
 */
class ArtilleryAttackModel extends BranchGroup {

    public ArtilleryAttackModel(int type, ArtilleryAttackAction aaa, IGame game, TileTextureManager tileManager) {
        this(type, aaa.getTarget(game).getPosition(), game, tileManager);
        setUserData(aaa);
    }

    public ArtilleryAttackModel(int type, Coords c, IGame game, TileTextureManager tileManager) {
        IHex hex = game.getBoard().getHex(c);
        Vector3d tl = new Vector3d(BoardModel.getHexLocation(c, hex.surface()));
        Transform3D t = new Transform3D();
        if (type == TilesetManager.ARTILLERY_INCOMING) {
            // FIXME: nearly invisible on map view
            t.rotX(Math.PI/2);
            tl.z += BoardModel.HEX_DIAMETER;
        }
        t.setScale(new Vector3d(2*BoardModel.HEX_SIDE_LENGTH, BoardModel.HEX_DIAMETER, 1.0));
        t.setTranslation(tl);
        TransformGroup anim = new TransformGroup();
        anim.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        anim.addChild(tileManager.getArtilleryTarget(type));
        TransformGroup tg = new TransformGroup(t);
        tg.addChild(anim);
        addChild(tg);
        
        // FIXME: somehow, this looks weird. find mistake.
        //RotationInterpolator ri = new RotationInterpolator(C.defAlpha, anim);
        //ri.setSchedulingBounds(BoardModel.bounds);
        //ri.setTransformAxis(new Transform3D(C.mkquat(type, type, type, type), new Vector3d(0.0, 1.0, 0.0), 1.0));
        //addChild(ri);
        setCapability(ALLOW_DETACH);
    }

}
