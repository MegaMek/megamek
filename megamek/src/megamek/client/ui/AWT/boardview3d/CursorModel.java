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

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;
import megamek.common.Coords;
import megamek.common.IHex;

class CursorModel extends HexModel {
    public CursorModel(Color3f color) {
    setTransform(new Transform3D(C.nullRot, new Vector3d(), 1.0));
        setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance base = new Appearance();
        base.setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.SHADE_FLAT));
        base.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        base.setLineAttributes(C.defLine);
        
        Shape3D shape = new Shape3D(border, base);
        shape.setAppearance(base);
    addChild(shape);
    }    

    public void setColor(Color3f color) {
        ((Shape3D)getChild(0)).getAppearance().setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.SHADE_FLAT));
    }
    
    public void move(Coords c, IHex hex) {
        if (c == null || hex == null) {
            hide();
            return;
        }

        Vector3d loc = new Vector3d(BoardModel.getHexLocation(c, hex.getElevation()));
        loc.z += 0.4;
        Transform3D t = new Transform3D(C.nullRot, loc, 1.0);
        setTransform(t);
    }

    public void hide() {
        setTransform(new Transform3D(C.nullRot, new Vector3d(), 0.0));
    }
}
