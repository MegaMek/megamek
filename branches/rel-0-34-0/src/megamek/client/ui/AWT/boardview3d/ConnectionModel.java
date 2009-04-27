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
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TransparencyInterpolator;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import megamek.common.Coords;

import com.sun.j3d.utils.geometry.Cylinder;

/**
 *
 * @author jwalt
 */
class ConnectionModel extends BranchGroup {
    public ConnectionModel(Coords sc, Coords dc, int selev, int delev, Point3d source, Color3f color, float alpha) {
        if (source == null) {
            source = BoardModel.getHexLocation(sc, selev);
            source.z += BoardModel.HEX_HEIGHT/2;
        }

        Point3d destination = BoardModel.getHexLocation(dc, delev);
        destination.z += BoardModel.HEX_HEIGHT/2;

        Appearance base = new Appearance();
        base.setMaterial(new Material(color, C.black, color, C.white, 64.0f));
        base.setPolygonAttributes(C.noCull);
        TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.BLENDED, alpha);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        base.setTransparencyAttributes(ta);
        TransparencyInterpolator ti = new TransparencyInterpolator(C.halfAlpha, ta, alpha, .97f);
        ti.setSchedulingBounds(BoardModel.bounds);
        addChild(ti);

        Cylinder link = new Cylinder(1.5f, (float)source.distance(destination));
        link.setAppearance(base);

        Vector3d destination0 = new Vector3d(destination);
        destination0.negate();
        destination0.add(source);
        Vector3d diff = new Vector3d(destination0);
        destination0.y = -destination0.y;
        double angle = destination0.angle(new Vector3d(0.0, -1.0, 0.0));
        destination0.cross(destination0, new Vector3d(0.0, -1.0, 0.0));
        Quat4d rot = new Quat4d();
        rot.set(new AxisAngle4d(destination0, angle));
        diff.scale(.5);
        destination.add(diff);

        TransformGroup tg = new TransformGroup(new Transform3D(rot, new Vector3d(destination), 1.0));
        tg.addChild(link);
        addChild(tg);
        setCapability(ALLOW_DETACH);
        setPickable(false);
    }
}
