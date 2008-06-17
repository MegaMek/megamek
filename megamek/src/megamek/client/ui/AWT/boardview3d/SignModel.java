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
import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture2D;

class SignModel extends Shape3D {
    static final GeometryArray polygon = makeSign();

    private static final GeometryArray makeSign() {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
        final double top = Math.sqrt(BoardModel.HEX_SIDE_LENGTH/2*BoardModel.HEX_SIDE_LENGTH/2-BoardModel.HEX_DIAMETER/6*BoardModel.HEX_DIAMETER/6);
        double[] coords = {
            -BoardModel.HEX_SIDE_LENGTH/4, BoardModel.HEX_DIAMETER/12, 0.0,
            -BoardModel.HEX_SIDE_LENGTH/4, 0, top,
             BoardModel.HEX_SIDE_LENGTH/4, 0, top,
             BoardModel.HEX_SIDE_LENGTH/4, BoardModel.HEX_DIAMETER/12, 0.0,

             0.0, -BoardModel.HEX_DIAMETER/6, 0.0,
             BoardModel.HEX_SIDE_LENGTH/8, -BoardModel.HEX_DIAMETER/8, top,
            -BoardModel.HEX_SIDE_LENGTH/8, BoardModel.HEX_DIAMETER/8, top,
            -BoardModel.HEX_SIDE_LENGTH/4, BoardModel.HEX_DIAMETER/12, 0.0,

             BoardModel.HEX_SIDE_LENGTH/4, BoardModel.HEX_DIAMETER/12, 0.0,
             BoardModel.HEX_SIDE_LENGTH/8, BoardModel.HEX_DIAMETER/8, top,
            -BoardModel.HEX_SIDE_LENGTH/8, -BoardModel.HEX_DIAMETER/8, top,
             0.0, -BoardModel.HEX_DIAMETER/6, 0.0,
        };
        float[] tcoords = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

        };
        gi.setCoordinates(coords);
        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, tcoords);
        
        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);

        Stripifier st = new Stripifier();
        st.stripify(gi);

        return gi.getGeometryArray();
    }

    private static final Appearance makeAppearance(Texture2D tex) {
        Appearance app = new Appearance();
        app.setTexture(tex);
        app.setTransparencyAttributes(C.alphaTexture);
        return app;
    }

    public SignModel(Texture2D tex) {
        super(polygon, makeAppearance(tex));
    }
}
