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

import com.sun.j3d.utils.behaviors.interpolators.*;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import java.awt.Color;
import java.util.Vector;
import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Material;
import javax.media.j3d.ScaleInterpolator;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TransparencyInterpolator;
import javax.vecmath.*;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.common.BattleArmor;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.UnitLocation;

/**
 *
 * @author jwalt
 */
class EntityModel extends BranchGroup {
    
    Alpha alpha;
    TransformGroup facing, label, mech;

    static final Vector3d location(IHex hex, Coords c, int elevation, double height) {
        Vector3d v = new Vector3d(BoardModel.getHexLocation(c, hex.getElevation() + elevation));
        v.z += height/2 + .1;
        return v;
    }

    static final Vector3d labelLocation(IHex hex, Coords c, int elevation) {
        return new Vector3d(BoardModel.getHexLocation(c, hex.getElevation() + (elevation < 0 ? 0 : elevation)));
    }

    static final double facing(int dir) {
        return -Math.PI/3 * dir;
    }
    
    static final double pitch(Entity entity) {
        if (entity.isProne() && entity instanceof Mech) return -Math.PI/2;
        return 0.0;
    }
    
    static final Vector3d scale(Entity entity) {
        if (entity.isProne() && entity instanceof Mech) {
            return new Vector3d(BoardModel.UNIT_SIZE, height(entity), BoardModel.HEX_HEIGHT*2);
        } else {
            return new Vector3d(BoardModel.UNIT_SIZE, BoardModel.UNIT_SIZE, height(entity));
        }
    }

    static final double height(Entity entity) {
        double height = BoardModel.HEX_HEIGHT*(entity.height()+1);
        if (entity instanceof Infantry) height = BoardModel.INFANTRY_HEIGHT;
        if (entity instanceof BattleArmor) height = BoardModel.BATTLEARMOR_HEIGHT;
        if (entity.isDestroyed()) height = BoardModel.WRECK_HEIGHT;

        return height;
    }

    public EntityModel(Entity entity, TileTextureManager tilesetManager, ViewTransform view, IGame game) {
        Coords c = entity.getPosition();
        IHex hex = game.getBoard().getHex(c);

        int elevation = entity.getElevation();
        int dir = entity.getFacing();
        if (dir == -1) dir = 0;
        int sdir = entity.getSecondaryFacing();
        if (sdir == -1) sdir = dir;

        Color tint = PlayerColors.getColor(entity.getOwner().getColorIndex());
        Color3f c50 = new Color3f(tint);
        c50.scale(0.5f);
        Color3f c30 = new Color3f(tint);
        c30.scale(0.3f);

        Vector3d scale = scale(entity);
        // TODO: use this as fallback, but load real 3D models if available
        if (true) {
            Shape3D sh = tilesetManager.getModel(entity);

            Transform3D t = new Transform3D();

            t.rotX(pitch(entity));
            t.setScale(scale);

            mech = new TransformGroup(t);
            mech.addChild(sh);
        }
        mech.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Transform3D ftrans = new Transform3D();
        ftrans.rotZ(facing(dir));
        ftrans.setTranslation(location(hex, c, elevation, height(entity)));
        facing = new TransformGroup(ftrans);
        facing.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        facing.addChild(mech);

        if (entity.getECMRange() != Entity.NONE) {
            int range = entity.getECMRange();
            Appearance eapp = new Appearance();
            eapp.setColoringAttributes(new ColoringAttributes(c50, ColoringAttributes.SHADE_FLAT));
            TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.90f);
            ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
            eapp.setTransparencyAttributes(ta);
            TransparencyInterpolator ti = new TransparencyInterpolator(C.halfAlpha, ta, .90f, .97f);
            ti.setSchedulingBounds(BoardModel.bounds);
            addChild(ti);
            eapp.setPolygonAttributes(C.noCull);
            Shape3D ecm = makeECMArea(range);
            ecm.setAppearance(eapp);
            ecm.setPickable(false);
            facing.addChild(ecm);
            ecm = makeECMBorder(range);
            ecm.setAppearance(eapp);
            ecm.setPickable(false);
            facing.addChild(ecm);
            eapp = new Appearance();
            eapp.setColoringAttributes(new ColoringAttributes(c50, ColoringAttributes.SHADE_FLAT));
            eapp.setLineAttributes(C.defLine);
            ecm = makeECMOutline(range);
            ecm.setAppearance(eapp);
            ecm.setPickable(false);
            facing.addChild(ecm);
        }
        
        Appearance sapp = new Appearance();
        sapp.setMaterial(new Material(new Color3f(tint), C.black, c50, C.white, 64.0f));

        Cone co = new Cone();
        co.setAppearance(sapp);

        if (dir != sdir) {
            TransformGroup fscale = new TransformGroup();
            fscale.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            ScaleInterpolator si = new ScaleInterpolator(C.dblAlpha, fscale);
            si.setSchedulingBounds(BoardModel.bounds);
            si.setMinimumScale(.8f);
            si.setMaximumScale(1.25f);
            fscale.addChild(co);
            Transform3D sftrans = new Transform3D();
            sftrans.rotZ(facing(sdir-dir));
            Vector3d sv = new Vector3d(0.0, 5*BoardModel.HEX_DIAMETER/12, 0.0);
            sftrans.transform(sv);
            sftrans.setTranslation(sv);
            TransformGroup sfacing = new TransformGroup(sftrans);
            sfacing.addChild(fscale);
            sfacing.addChild(si);
            facing.addChild(sfacing);
        }

        label = new TransformGroup(new Transform3D(C.nullRot, labelLocation(hex, c, elevation), 1.0));
        label.addChild(view.makeViewRelative(new LabelModel(entity.getShortName(), C.white, c50, LabelModel.BOLD), BoardModel.HEX_DIAMETER*.45));
        label.setPickable(false);
        label.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        addChild(facing);
        addChild(label);
        setCapability(BranchGroup.ALLOW_DETACH);
        setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        setUserData(entity);
    }

    void move(final Entity entity, Vector<UnitLocation> movePath, IBoard gboard) {
        final int knots = movePath.size();
        KBKeyFrame[] keyframes1 = new KBKeyFrame[knots+1];
        KBKeyFrame[] keyframes2 = new KBKeyFrame[knots+1];
        KBKeyFrame[] keyframes3 = new KBKeyFrame[knots+1];

        int pos = 0;
        Point3f scale = new Point3f(1.0f, 1.0f, 1.0f);
        Point3f zero = new Point3f();
        final Entity prev = (Entity)getUserData();
        final int mpos = (prev.isProne()?1:knots);
        final float pitchA = (float)-pitch(prev), pitchB = (float)-pitch(entity);
        final Point3f scaleA = new Point3f(scale(prev)), scaleB = new Point3f(scale(entity));
        IHex hex = gboard.getHex(prev.getPosition());
        int dir = prev.getFacing();
        if (dir == -1) dir = 0;
        keyframes1[pos] = new KBKeyFrame(
            pos*1.0f/knots,
            0,
            new Point3f(location(hex, prev.getPosition(), prev.getElevation(), height(pos < mpos?prev:entity))),
            0.0f, 0.0f, (float)facing(dir),
            scale,
            -1.0f, -0.7f, 0.2f
        );
        keyframes2[pos] = new KBKeyFrame(
            pos*1.0f/knots, 0,
            new Point3f(labelLocation(hex, prev.getPosition(), prev.getElevation())),
            0.0f, 0.0f, 0.0f, scale, -1.0f, -0.7f, 0.2f
        );
        keyframes3[pos] = new KBKeyFrame(
            pos*1.0f/knots, 0,
            zero,
            0.0f, (pos < mpos?pitchA:pitchB), 0.0f,
            (pos < mpos?scaleA:scaleB), -1.0f, -0.7f, 0.2f
        );
        pos++;
        for (UnitLocation step : movePath) {
            hex = gboard.getHex(step.getCoords());
            dir = step.getFacing();
            if (dir == -1) dir = 0;
            keyframes1[pos] = new KBKeyFrame(
                pos*1.0f/knots,
                0,
                new Point3f(location(hex, step.getCoords(), step.getElevation(), height(pos < mpos?prev:entity))),
                0.0f, 0.0f, (float)facing(dir),
                scale,
                -1.0f, -0.7f, 0.2f
            );
            keyframes2[pos] = new KBKeyFrame(
                pos*1.0f/knots, 0,
                new Point3f(labelLocation(hex, step.getCoords(), step.getElevation())),
                0.0f, 0.0f, 0.0f, scale, -1.0f, -0.7f, 0.2f
            );
            keyframes3[pos] = new KBKeyFrame(
                pos*1.0f/knots, 0,
                zero,
                0.0f, (pos < mpos?pitchA:pitchB), 0.0f,
                (pos < mpos?scaleA:scaleB), -1.0f, -0.7f, 0.2f
            );
            pos++;
        }
        
        setUserData(entity);

        alpha = new Alpha(1, 500*movePath.size());
        KBRotPosScaleSplinePathInterpolator interpolator1 = 
            new KBRotPosScaleSplinePathInterpolator(alpha, facing, new Transform3D(), keyframes1) {
                @Override
                public void computeTransform(float alphaValue, Transform3D transform) {
                    super.computeTransform(alphaValue, transform);
                    Vector3d trans = new Vector3d();
                    transform.get(trans);
                    Point3d pos = new Point3d(trans);
                    double h = height(((int)(knots*alphaValue)) < mpos?prev:entity);
                    pos.z = pos.z - h/2 + (h-BoardModel.HEX_HEIGHT) + BoardModel.HEX_HEIGHT/2;
                    EntityGroup g = ((EntityGroup)getParent().getParent().getParent());
                    if (alphaValue < 0.999f) {
                        g.removeC3LinksFor(entity);
                        g.addC3LinksFor(entity, pos);
                    } else {
                        g.update(entity);
                    }
                }
            };

        KBRotPosScaleSplinePathInterpolator interpolator2 = 
            new KBRotPosScaleSplinePathInterpolator(alpha, label, new Transform3D(), keyframes2);
        
        // as of 2008-06-01, this does not handle scaling because KBRotPosScale is lacking non-uniform scale support
        // too bad, but visually acceptable
        // since the scale value at alpha==1.0 could be wrong, override it.
        KBRotPosScaleSplinePathInterpolator interpolator3 = 
            new KBRotPosScaleSplinePathInterpolator(alpha, mech, new Transform3D(), keyframes3) {
                @Override
                public void computeTransform(float alphaValue, Transform3D transform) {
                    if (alphaValue < 0.999f) {
                        super.computeTransform(alphaValue, transform);
                    } else {
                        transform.rotX(pitch(entity));
                        transform.setScale(new Vector3d(scaleB));
                    }
                }
            };

        interpolator1.setSchedulingBounds(BoardModel.bounds);
        interpolator2.setSchedulingBounds(BoardModel.bounds);
        interpolator3.setSchedulingBounds(BoardModel.bounds);

        BranchGroup bg = new BranchGroup();
        bg.addChild(interpolator1);
        bg.addChild(interpolator2);
        bg.addChild(interpolator3);
        addChild(bg);

        alpha.setStartTime(System.currentTimeMillis()+1000);
    }
    
    private static final double[] makeECMCoords(int range) {
        double[] top = new double[3*6*(2*range+1)+3];
        int coord = 0;
        double[] pos = new double[]{
            -range*1.5*BoardModel.HEX_SIDE_LENGTH - BoardModel.HEX_SIDE_LENGTH,
            range*BoardModel.HEX_DIAMETER/2,
            10.0*BoardModel.HEX_HEIGHT
        };
        coord = makeECMPart(range, coord, top, pos,
            -BoardModel.HEX_SIDE_LENGTH/2, BoardModel.HEX_SIDE_LENGTH/2,
            -BoardModel.HEX_DIAMETER/2, -BoardModel.HEX_DIAMETER/2);
        coord = makeECMPart(range, coord, top, pos,
            BoardModel.HEX_SIDE_LENGTH/2, BoardModel.HEX_SIDE_LENGTH,
            -BoardModel.HEX_DIAMETER/2, 0);
        coord = makeECMPart(range, coord, top, pos,
            BoardModel.HEX_SIDE_LENGTH, BoardModel.HEX_SIDE_LENGTH/2,
            0, BoardModel.HEX_DIAMETER/2);
        coord = makeECMPart(range, coord, top, pos,
            BoardModel.HEX_SIDE_LENGTH/2, -BoardModel.HEX_SIDE_LENGTH/2,
            BoardModel.HEX_DIAMETER/2, BoardModel.HEX_DIAMETER/2);
        coord = makeECMPart(range, coord, top, pos,
            -BoardModel.HEX_SIDE_LENGTH/2, -BoardModel.HEX_SIDE_LENGTH,
            BoardModel.HEX_DIAMETER/2, 0);
        coord = makeECMPart(range, coord, top, pos,
            -BoardModel.HEX_SIDE_LENGTH, -BoardModel.HEX_SIDE_LENGTH/2,
            0, -BoardModel.HEX_DIAMETER/2);
        top[coord++] = top[0];
        top[coord++] = top[1];
        top[coord++] = top[2];
        
        return top;
    }

    private static final Shape3D makeECMArea(int range) {
        double[] top = makeECMCoords(range);

        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        gi.setCoordinates(top);
        gi.setStripCounts(new int[] { top.length/3 });
        gi.setContourCounts(new int[] { 1 });

        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return new Shape3D(gi.getGeometryArray());
    }
    
    private static final Shape3D makeECMBorder(int range) {
        double[] top = makeECMCoords(range);
        double[] border = new double[top.length*2];
        
        int j = 0;
        for (int i = 0; i < top.length; i += 3) {
            border[j++] = top[i];
            border[j++] = top[i+1];
            border[j++] = top[i+2];

            border[j++] = top[i];
            border[j++] = top[i+1];
            border[j++] = -top[i+2];
        }

        GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        gi.setCoordinates(border);
        gi.setStripCounts(new int[] { border.length/3 });

        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);

        Stripifier st = new Stripifier();
        st.stripify(gi);
        
        return new Shape3D(gi.getGeometryArray());
    }
    
    private static final Shape3D makeECMOutline(int range) {
        double[] outline = makeECMCoords(range);

        LineStripArray l = new LineStripArray(outline.length, LineStripArray.COORDINATES, new int[] { outline.length/3 });
        l.setCoordinates(0, outline);

        return new Shape3D(l);
    }
    
    private static final int makeECMPart(int range, int coord, double[] dest, double[] pos, double dx1, double dx2, double dy1, double dy2) {
        dest[coord++] = pos[0];
        pos[0] += dx2;
        dest[coord++] = pos[1];
        pos[1] += dy2;
        dest[coord++] = pos[2];

        for (int i = 0; i < range; i++) {
            dest[coord++] = pos[0];
            pos[0] += dx1;
            dest[coord++] = pos[1];
            pos[1] += dy1;
            dest[coord++] = pos[2];

            dest[coord++] = pos[0];
            pos[0] += dx2;
            dest[coord++] = pos[1];
            pos[1] += dy2;
            dest[coord++] = pos[2];
        }
        return coord;
    }

    boolean isMoving() {
        return alpha != null && !alpha.finished();
    }

}
