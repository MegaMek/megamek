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
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.TransformGroup;

abstract class HexModel extends TransformGroup {
    private static final double[] hexVertices = {
        -BoardModel.HEX_SIDE_LENGTH/2,  BoardModel.HEX_DIAMETER/2, 0,
        -BoardModel.HEX_SIDE_LENGTH,    0,              0,
        -BoardModel.HEX_SIDE_LENGTH/2, -BoardModel.HEX_DIAMETER/2, 0,
         BoardModel.HEX_SIDE_LENGTH/2, -BoardModel.HEX_DIAMETER/2, 0,
         BoardModel.HEX_SIDE_LENGTH,    0,              0,
         BoardModel.HEX_SIDE_LENGTH/2,  BoardModel.HEX_DIAMETER/2, 0,
        -BoardModel.HEX_SIDE_LENGTH/2,  BoardModel.HEX_DIAMETER/2, 0,
    };

    static final GeometryArray polygon = makeHex();
    static final GeometryArray border = makeHexOutline();
    static final GeometryArray shaft = makeHexShaft();

    private static final GeometryArray makeHex() {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        float[] texCoords = {
            0.25f, 1.0f,
            0.00f, 0.5f,
            0.25f, 0.0f,
            0.75f, 0.0f,
            1.00f, 0.5f,
            0.75f, 1.0f,
            0.25f, 1.0f,
        };
        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
        };
        
        gi.setCoordinates(hexVertices);
        gi.setStripCounts(new int[] { hexVertices.length/3 });
        gi.setContourCounts(new int[] { 1 });
        gi.setNormals(normals);

        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, texCoords);
        
        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return gi.getGeometryArray();
    }

    private static final GeometryArray makeHexShaft() {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        
        double[] coords = new double[hexVertices.length*2];
        
        int s = 0;
        for (int d = 0; d < coords.length; d += 6, s += 3) {
            coords[d] = hexVertices[s];
            coords[d+1] = hexVertices[s+1];
            coords[d+2] = -0.001;
            coords[d+3] = hexVertices[s];
            coords[d+4] = hexVertices[s+1];
            coords[d+5] = -20*BoardModel.HEX_HEIGHT;
        }
        
        gi.setCoordinates(coords);
        gi.setStripCounts(new int[] { coords.length/3 });
        
        NormalGenerator ng = new NormalGenerator(0);
        ng.generateNormals(gi);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return gi.getGeometryArray();
    }

    private static final GeometryArray makeHexOutline() {
        LineStripArray l = new LineStripArray(hexVertices.length, LineStripArray.COORDINATES, new int[] { hexVertices.length/3 });
        l.setCoordinates(0, hexVertices);
        return l;
    }

}
