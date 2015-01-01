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

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Stripifier;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

abstract class ArrowModel extends BranchGroup {
    
    TransformGroup anim = new TransformGroup();
    
    ArrowModel() {
        setCapability(ALLOW_DETACH);
        anim.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        RotationInterpolator ri = new RotationInterpolator(C.defAlpha, anim, new Transform3D(), 0.0f, 2*(float)Math.PI);
        ri.setSchedulingBounds(BoardModel.bounds);
        anim.addChild(ri);
    }

    private static final double[] arrowVertices = {
         BoardModel.HEX_SIDE_LENGTH/18,  BoardModel.HEX_DIAMETER/18, 0,
         BoardModel.HEX_SIDE_LENGTH/6,   BoardModel.HEX_DIAMETER/18, 0,
         0,                               BoardModel.HEX_DIAMETER/6, 0,
        -BoardModel.HEX_SIDE_LENGTH/6,   BoardModel.HEX_DIAMETER/18, 0,
        -BoardModel.HEX_SIDE_LENGTH/18,  BoardModel.HEX_DIAMETER/18, 0,
        -BoardModel.HEX_SIDE_LENGTH/18, -BoardModel.HEX_DIAMETER/9, 0,
         BoardModel.HEX_SIDE_LENGTH/18, -BoardModel.HEX_DIAMETER/9, 0,
         BoardModel.HEX_SIDE_LENGTH/18,  BoardModel.HEX_DIAMETER/18, 0,
    };
    private static final int[] arrowStrips = { 8 };

    static final GeometryArray polygon = makeArrow();
    static final GeometryArray border = makeArrowOutline();
    
    private static final GeometryArray makeArrow() {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        int[] contours = { 1 };
        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
        };
        
        gi.setCoordinates(arrowVertices);
        gi.setStripCounts(arrowStrips);
        gi.setContourCounts(contours);
        gi.setNormals(normals);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return gi.getGeometryArray();
    }

    private static final GeometryArray makeArrowOutline() {
        LineStripArray l = new LineStripArray(arrowVertices.length, LineStripArray.COORDINATES, arrowStrips);
        l.setCoordinates(0, arrowVertices);
        return l;
    }

    protected static final GeometryArray makeArrow(double length) {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        int[] contours = { 1 };
        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
        };
        double[] vertices = new double[arrowVertices.length];
        System.arraycopy(arrowVertices, 0, vertices, 0, vertices.length);
        
        length -= BoardModel.HEX_DIAMETER/2;
        for (int i = 0; i < 5; i++) vertices[3*i+1] += length;
        vertices[3*5+1] += BoardModel.HEX_DIAMETER/2;
        vertices[3*6+1] += BoardModel.HEX_DIAMETER/2;
        vertices[3*7+1] += length;
        
        gi.setCoordinates(vertices);
        gi.setStripCounts(arrowStrips);
        gi.setContourCounts(contours);
        gi.setNormals(normals);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return gi.getGeometryArray();
    }

    static final GeometryArray makeArrowOutline(double length) {
        LineStripArray l = new LineStripArray(arrowVertices.length, LineStripArray.COORDINATES, arrowStrips);
        double[] vertices = new double[arrowVertices.length];
        System.arraycopy(arrowVertices, 0, vertices, 0, vertices.length);
        
        length -= BoardModel.HEX_DIAMETER/2;
        for (int i = 0; i < 5; i++) vertices[3*i+1] += length;
        vertices[3*5+1] += BoardModel.HEX_DIAMETER/2;
        vertices[3*6+1] += BoardModel.HEX_DIAMETER/2;
        vertices[3*7+1] += length;
        
        l.setCoordinates(0, vertices);
        return l;
    }

}
