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
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DistanceLOD;
import javax.media.j3d.Font3D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;

class LabelModel extends TransformGroup {
    private final static double tolerance = 10.0;
    public static final Font3D PLAIN = new Font3D(new Font("sans-serif", Font.PLAIN, 2), tolerance, null);
    public static final Font3D BOLD = new Font3D(new Font("sans-serif", Font.BOLD, 2), tolerance, null);
    public static final Font3D BIG = new Font3D(new Font("sans-serif", Font.PLAIN, 3), tolerance, null);
    public static final Font3D BIGBOLD = new Font3D(new Font("sans-serif", Font.BOLD, 3), tolerance, null);
    

    public LabelModel(String text, Color3f fg, Color3f bg, Font3D font) {
        Text3D geom = new Text3D(font, text, new Point3f(0.0f, 0.0f, 0.0f), Text3D.ALIGN_CENTER, Text3D.PATH_RIGHT);
        BoundingBox b = new BoundingBox();
        geom.getBoundingBox(b);
        Point3d upper = new Point3d(), lower = new Point3d();
        b.getLower(lower);
        b.getUpper(upper);
        
        // Workaround for a bounding box bug -- assumes that M and ( have same ascent
        GlyphVector fix = font.getFont().createGlyphVector(new FontRenderContext(null, false, false), "M(");
        Point3d fixu = new Point3d(), fixl = new Point3d();
        font.getBoundingBox(fix.getGlyphCode(0), b);
        b.getUpper(fixu);
        b.getLower(fixl);
        double h1 = fixu.y-fixl.y;
        font.getBoundingBox(fix.getGlyphCode(1), b);
        b.getUpper(fixu);
        b.getLower(fixl);
        double h2 = fixu.y-fixl.y;
        upper.y = h1;
        lower.y = h1-h2;

        Switch sw = new Switch(Switch.CHILD_MASK);
        sw.setCapability(Switch.ALLOW_SWITCH_WRITE);
        DistanceLOD lod = new DistanceLOD(new float[] { font.getFont().getSize()*10*(float)BoardModel.HEX_DIAMETER });
        lod.addSwitch(sw);
        lod.setSchedulingBounds(BoardModel.bounds);

        Appearance app = new Appearance();
        app.setColoringAttributes(new ColoringAttributes(fg, ColoringAttributes.SHADE_FLAT));
        sw.addChild(new Shape3D(geom, app));

        app = new Appearance();
        Color3f lodfg = new Color3f(fg);
        lodfg.scale(2/3f);
        lodfg.add(C.grey10);
        app.setColoringAttributes(new ColoringAttributes(lodfg, ColoringAttributes.SHADE_FLAT));
        sw.addChild(makeQuad(lower.x, lower.y+(upper.y-lower.y)/4, upper.x-lower.x, (upper.y-lower.y)/2, 0, app));

        addChild(sw);
        addChild(lod);

        setTransform(new Transform3D(C.nullRot, new Vector3d(0,0,0.1), 1.0));

        if (bg != null) {
            app = new Appearance();
            app.setColoringAttributes(new ColoringAttributes(bg, ColoringAttributes.SHADE_FLAT));
            app.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 10.0f));
            addChild(makeQuad(lower.x-0.5, lower.y-0.1, upper.x-lower.x+1, upper.y-lower.y+.6, -.1, app));
        }
    }

    static final Shape3D makeQuad(double orgX, double orgY, double width, double height, double z, Appearance app) {
        GeometryInfo gi = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
        double[] vertices = {
            orgX,       orgY, z,
            orgX+width, orgY, z,
            orgX+width, orgY+height, z,
            orgX,       orgY+height, z,
        };
        float[] texCoords = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
        };
        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
        };
        
        gi.setCoordinates(vertices);
        gi.setNormals(normals);

        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, texCoords);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return new Shape3D(gi.getGeometryArray(), app);
    }
}
