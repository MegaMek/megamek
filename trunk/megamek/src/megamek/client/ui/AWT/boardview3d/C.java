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

import javax.media.j3d.Alpha;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.*;

/**
 * generic reusable 3D constants
 */
class C {
    static final boolean ANTIALIAS = true;
    static final PolygonAttributes noCull = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0.0f, true);
    static final LineAttributes defLine = new LineAttributes(2.0f, LineAttributes.PATTERN_SOLID, ANTIALIAS);
    static final TransparencyAttributes alphaTexture = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.0f);
    static final TextureAttributes materialModulate = new TextureAttributes(TextureAttributes.MODULATE, new Transform3D(), new Color4f(), TextureAttributes.NICEST);
    static final Quat4d nullRot = new Quat4d();
    static final Color3f black  = new Color3f(0.0f, 0.0f, 0.0f);
    static final Color3f white  = new Color3f(1.0f, 1.0f, 1.0f);
    static final Color3f grey90 = new Color3f(0.9f, 0.9f, 0.9f);
    static final Color3f grey75 = new Color3f(0.75f, 0.75f, 0.75f);
    static final Color3f grey50 = new Color3f(0.5f, 0.5f, 0.5f);
    static final Color3f grey25 = new Color3f(0.25f, 0.25f, 0.25f);
    static final Color3f grey10 = new Color3f(0.1f, 0.1f, 0.1f);
    static final Color3f red    = new Color3f(1.0f, 0.0f, 0.0f);
    static final Color3f green  = new Color3f(0.0f, 1.0f, 0.0f);
    static final Color3f blue   = new Color3f(0.0f, 0.0f, 1.0f);
    static final Color3f cyan  = new Color3f(0.0f, 1.0f, 1.0f);
    static final Color3f magenta  = new Color3f(1.0f, 0.0f, 1.0f);
    static final Color3f yellow  = new Color3f(1.0f, 1.0f, 0.0f);
    static final Color3f plain = new Color3f(5/8f/2, 1/2f/2, 3/8f/2);
    static final Color3f water = new Color3f(1/4f/2, 3/8f/2, 5/8f/2);
    static final Alpha defAlpha = new Alpha(-1, 4000);
    static final Alpha dblAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE|Alpha.DECREASING_ENABLE, 0, 0, 500, 0, 0, 500, 0, 0);
    static final Alpha halfAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE|Alpha.DECREASING_ENABLE, 0, 0, 4000, 0, 0, 4000, 0, 0);
    
    /**
     * Create a quaternion that describes a rotation of alpha around the given axis.
     * The axis must describe a unit vector.
     */
    static final Quat4d mkquat(double x, double y, double z, double alpha) {
        double s = Math.sin(alpha/2);
        return new Quat4d(x*s, y*s, z*s, Math.cos(alpha/2));
    }

    private C() {
    }
}
