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
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import megamek.common.Coords;
import megamek.common.IHex;

import com.sun.j3d.utils.behaviors.keyboard.KeyNavigatorBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.universe.MultiTransformGroup;
import com.sun.j3d.utils.universe.SimpleUniverse;


abstract class ViewTransform {
    static final int MAX_TRANSFORMS = 7;

    abstract String getName();
    abstract void reset();
    abstract void centerOnHex(Coords c, IHex hex);
    abstract Node makeViewRelative(Node obj, double centerDistance);
    abstract void zoom(int steps);
    void remove() {
        if (controllers != null) {
            controllers.detach();
        }
    }

    static ViewTransform create(int index, SimpleUniverse universe) {
        try {
            ViewTransform v = (ViewTransform)transforms[index].newInstance();
            v.universe = universe;
            v.controllers = new BranchGroup();
            v.controllers.setCapability(BranchGroup.ALLOW_DETACH);

            MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

            for (int i = 0; i < mtg.getNumTransforms(); i++) {
                mtg.getTransformGroup(i).setTransform(identity);
            }
            v.setup();
            v.controllers.compile();
            universe.addBranchGraph(v.controllers);
            return v;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    abstract protected void setup();

    protected SimpleUniverse universe;
    protected BranchGroup controllers;

    private static final Transform3D identity = new Transform3D();
    private static final Class<?>[] transforms = { PlayerViewTransform.class, MapViewTransform.class };

}

/**
 *  FIXME: This is WAY too complicated, as none of the core behaviors quite match what
 *  I want. Each one has its own little quirk. It should be easier to simply write a custom
 *  behavior, but this code grew step by step -- and works correctly.
 */
class PlayerViewTransform extends ViewTransform implements MouseBehaviorCallback {
    final static Quat4d rotfix = C.mkquat(1, 0, 0, Math.PI/2);

    public PlayerViewTransform() {}

    @Override
    String getName() { return "Player View"; }

    @Override
    public void centerOnHex(Coords c, IHex hex) {
        if ((c == null) || (hex == null)) {
            return;
        }

        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

        mtg.getTransformGroup(0).setTransform(
            new Transform3D(rotfix, new Vector3d(BoardModel.getHexLocation(c, hex.getElevation())), 1.0)
        );
    }

    @Override
    void reset() {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

        TransformGroup rotateTrans = mtg.getTransformGroup(1);
        Transform3D rtrans = new Transform3D();
        rtrans.rotX(-Math.PI/2);
        rotateTrans.setTransform(rtrans);

        TransformGroup tiltTrans = mtg.getTransformGroup(3);
        Transform3D ttrans = new Transform3D();
        ttrans.rotX(Math.PI/3);
        tiltTrans.setTransform(ttrans);

        TransformGroup zoomTrans = mtg.getTransformGroup(4);
        Transform3D ztrans = new Transform3D();
        ztrans.setTranslation(new Vector3d(0.0, 0.0, 20*BoardModel.HEX_DIAMETER));
        zoomTrans.setTransform(ztrans);
    }

    /**
     * Create transformation chain and mouse behaviors for a "human player" perspective
     */
    @Override
    protected void setup() {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();
        View view = universe.getViewer().getView();
        view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        view.setScreenScalePolicy(View.SCALE_SCREEN_SIZE);

        TransformGroup rotateTrans = mtg.getTransformGroup(1);
        Transform3D rtrans = new Transform3D();
        rtrans.rotX(-Math.PI/2);
        rotateTrans.setTransform(rtrans);

        TransformGroup panTrans = mtg.getTransformGroup(2);
        Transform3D ptrans = new Transform3D();
        panTrans.setTransform(ptrans);

        TransformGroup tiltTrans = mtg.getTransformGroup(3);
        Transform3D ttrans = new Transform3D();
        ttrans.rotX(Math.PI/3);
        tiltTrans.setTransform(ttrans);

        TransformGroup zoomTrans = mtg.getTransformGroup(4);
        zoomTrans.setTransform(new Transform3D(new Quat4d(), new Vector3d(0.0, 0.0, 20*BoardModel.HEX_DIAMETER), 1.0));

        Transform3D etrans = new Transform3D();
        etrans.lookAt(new Point3d(0, 0, 0), new Point3d(0, 0, -1), new Vector3d(0, 1, 0));
        etrans.invert();
        mtg.getTransformGroup(5).setTransform(etrans);

        MouseWheelZoom behavior0 = new MouseWheelZoom(zoomTrans);
        behavior0.setFactor(-BoardModel.HEX_DIAMETER/3);
        controllers.addChild(behavior0);
        behavior0.setSchedulingBounds(BoardModel.bounds);

        MouseRotate behavior1 = new MouseRotate(tiltTrans);
        behavior1.setFactor(0.0, -0.002);
        controllers.addChild(behavior1);
        behavior1.setSchedulingBounds(BoardModel.bounds);

        MouseRotate behavior2 = new MouseRotate(rotateTrans);
        behavior2.setFactor(-0.002, 0.0);
        controllers.addChild(behavior2);
        behavior2.setSchedulingBounds(BoardModel.bounds);

        MouseTranslate behavior3 = new MouseTranslate(panTrans);
        behavior3.setFactor(-0.5);
        controllers.addChild(behavior3);
        behavior3.setSchedulingBounds(BoardModel.bounds);
        behavior3.setupCallback(this);

        KeyNavigatorBehavior behavior4 = new KeyNavigatorBehavior(panTrans);
        controllers.addChild(behavior4);
        behavior4.setSchedulingBounds(BoardModel.bounds);
    }

    @Override
    void zoom(int steps) {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();
        TransformGroup zoomTrans = mtg.getTransformGroup(4);

        Transform3D trans = new Transform3D();
        zoomTrans.getTransform(trans);

        Vector3d t = new Vector3d();
        trans.get(t);
        t.z -= steps*4*BoardModel.HEX_DIAMETER;
        trans.setTranslation(t);

        zoomTrans.setTransform(trans);
    }

    @Override
    Node makeViewRelative(Node obj, double centerDistance) {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

        Transform3D rtrans = new Transform3D();
        mtg.getTransformGroup(1).getTransform(rtrans);
        Transform3D ttrans = new Transform3D();
        mtg.getTransformGroup(3).getTransform(ttrans);
        Transform3D ptrans = new Transform3D();
        Transform3D ftrans = new Transform3D();
        ftrans.rotX(Math.PI/2);
        ptrans.setTranslation(new Vector3d(0.0, -centerDistance, 1.0));

        TransformGroup t = new TransformGroup(ttrans);
        t.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TransformGroup p = new TransformGroup(ptrans);
        TransformGroup r = new TransformGroup(rtrans);
        r.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TransformGroup f = new TransformGroup(ftrans);

        f.addChild(r);
        r.addChild(p);
        p.addChild(t);
        t.addChild(obj);

        BranchGroup bg = new BranchGroup();
        MouseRotate behavior1 = new MouseRotate(t);
        behavior1.setFactor(0.0, -0.002);
        bg.addChild(behavior1);
        behavior1.setSchedulingBounds(BoardModel.bounds);

        MouseRotate behavior2 = new MouseRotate(r);
        behavior2.setFactor(-0.002, 0.0);
        bg.addChild(behavior2);
        behavior2.setSchedulingBounds(BoardModel.bounds);

        bg.addChild(f);

        return bg;
    }

    public void transformChanged(int type, Transform3D trans) {
        if (type != TRANSLATE) {
            return;
        }

        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();
        Transform3D old = new Transform3D();
        mtg.getTransformGroup(0).getTransform(old);
        Transform3D rot = new Transform3D();
        mtg.getTransformGroup(1).getTransform(rot);
        Vector3d transv = new Vector3d();
        trans.get(transv);
        Point3d transp = new Point3d(transv);
        rot.transform(transp);
        old.transform(transp);
        old.setTranslation(new Vector3d(transp));
        mtg.getTransformGroup(0).setTransform(old);
        mtg.getTransformGroup(2).setTransform(new Transform3D());
    }

}


/**
 *  FIXME: this doesn't yet work exactly as intended. Either rewrite using a
 *  custom behaviour, or adapt PlayerViewTransform, which is now correct
 *  (but ugly).
 */
class MapViewTransform extends ViewTransform implements MouseBehaviorCallback {
    public MapViewTransform() {}

    TransformGroup wheel = new TransformGroup();

    @Override
    String getName() { return "Map View"; }

    @Override
    public void centerOnHex(Coords c, IHex hex) {
        if ((c == null) || (hex == null)) {
            return;
        }

        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

        mtg.getTransformGroup(0).setTransform(
            new Transform3D(C.nullRot, new Vector3d(BoardModel.getHexLocation(c, hex.getElevation())), 1.0)
        );

        center();
    }

    void center() {
        // TODO: smooth reset
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();

        TransformGroup panTrans = mtg.getTransformGroup(1);
        Transform3D ptrans = new Transform3D();
        ptrans.rotX(Math.PI/2);
        panTrans.setTransform(ptrans);
    }

    @Override
    void zoom(int steps) {
        View view = universe.getViewer().getView();

        double scale = view.getScreenScale();

        scale *= Math.pow(1.5, steps);

        view.setScreenScale(scale);

        // seems like this is needed to counter too much J3D optimization
        TransformGroup panTrans = universe.getViewingPlatform().getMultiTransformGroup().getTransformGroup(1);
        Transform3D ptrans = new Transform3D();
        panTrans.getTransform(ptrans);
        panTrans.setTransform(ptrans);
    }

    @Override
    void reset() {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();
        View view = universe.getViewer().getView();
        view.setScreenScale(.001);

        TransformGroup rotateTrans = mtg.getTransformGroup(2);
        Transform3D rtrans = new Transform3D();
        rtrans.rotX(-Math.PI/2);
        rotateTrans.setTransform(rtrans);
    }

    /**
     * Create transformation chain and mouse behaviors for a top-down map perspective
     */
    @Override
    protected void setup() {
        MultiTransformGroup mtg = universe.getViewingPlatform().getMultiTransformGroup();
        View view = universe.getViewer().getView();

        view.setProjectionPolicy(View.PARALLEL_PROJECTION);
        view.setScreenScalePolicy(View.SCALE_EXPLICIT);
        view.setScreenScale(.001);

        TransformGroup panTrans = mtg.getTransformGroup(1);
        Transform3D ptrans = new Transform3D();
        ptrans.rotX(Math.PI/2);
        panTrans.setTransform(ptrans);

        TransformGroup rotateTrans = mtg.getTransformGroup(2);
        Transform3D rtrans = new Transform3D();
        rtrans.rotX(-Math.PI/2);
        rotateTrans.setTransform(rtrans);

        Transform3D etrans = new Transform3D();
        etrans.lookAt(new Point3d(0.0, 0.0, 20*BoardModel.HEX_DIAMETER), new Point3d(0, 0, 0), new Vector3d(0, 1, 0));
        etrans.invert();
        mtg.getTransformGroup(5).setTransform(etrans);

        MouseWheelZoom behavior0 = new MouseWheelZoom(wheel);
        behavior0.setFactor(1.0/3);
        behavior0.setupCallback(this);
        controllers.addChild(behavior0);
        behavior0.setSchedulingBounds(BoardModel.bounds);

        MouseRotate behavior2 = new MouseRotate(rotateTrans);
        behavior2.setFactor(-0.002, 0.0);
        controllers.addChild(behavior2);
        behavior2.setSchedulingBounds(BoardModel.bounds);

        // FIXME: rotate pan direction with rotateTrans
        MouseTranslate behavior3 = new MouseTranslate(panTrans);
        behavior3.setFactor(-0.5);
        controllers.addChild(behavior3);
        behavior3.setSchedulingBounds(BoardModel.bounds);
    }

    @Override
    Node makeViewRelative(Node obj, double centerDistance) {
        // TODO.
        return obj;
    }

    public void transformChanged(int type, Transform3D trans) {
        if (type != ZOOM) {
            return;
        }

        Vector3d t = new Vector3d();
        trans.get(t);

        zoom((int)Math.round(t.z));

        trans.set(new Vector3d());
        wheel.setTransform(trans);
    }

}
