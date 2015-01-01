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

import java.util.List;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.*;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.Terrains;

class BoardHexModel extends HexModel {
    private static final Material normal = new Material(C.grey90, C.black, C.grey90, C.black, 1.0f);
    private static final Material normalWater = new Material(C.grey75, C.black, C.grey75, C.white, 128.0f);
    private static final Material night = new Material(C.grey25, C.black, C.grey25, C.black, 1.0f);
    private static final Material dark = new Material(C.grey10, C.black, C.grey10, C.black, 1.0f);
    
    private static final Material side = new Material(C.plain, C.black, C.plain, C.black, 1.0f);
    private static final Material sideWater = new Material(C.water, C.black, C.water, C.black, 1.0f);

    private static final int KEEP = 3;
    
    Shape3D floor;
    Shape3D surface;
    IHex hex;
    Material current = normal;
    IGame game;
    
    public static SharedGroup[] mkShared() {
        SharedGroup[] shafts = new SharedGroup[2];
        // sides of hex
        Appearance sapp;
        Shape3D sh;
        sapp = new Appearance();
        sapp.setMaterial(side);
        sh = new Shape3D(shaft, sapp);
        sh.setPickable(false);
        shafts[0] = new SharedGroup();
        shafts[0].addChild(sh);

        sapp = new Appearance();
        sapp.setMaterial(sideWater);
        sh = new Shape3D(shaft, sapp);
        shafts[1] = new SharedGroup();
        shafts[1].addChild(sh);
        
        return shafts;
    }
    

    
    public BoardHexModel(IGame g, Coords c, IHex h, TileTextureManager tileManager, SharedGroup shafts[]) {
        game = g;
        hex = h;
        Appearance base = new Appearance();
        base.setMaterial(normal);
        floor = new Shape3D(polygon, base);
        floor.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        floor.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        floor.setPickable(true);
        addChild(floor);

        addChild(new Link(hex.depth() > 0?shafts[1]:shafts[0]));
        
        setUserData(new Coords(c));
        ypos = 0;
        addText(""+c.getBoardNum(), new Color3f(GUIPreferences.getInstance().getMapTextColor()));
        final Point3d hexLoc = BoardModel.getHexLocation(c, h.floor());
        setTransform(new Transform3D(C.nullRot, new Vector3d(hexLoc), 1.0));

        // Water surface
        if (surface == null && hex.depth() > 0) {
            TransformGroup sTrans = new TransformGroup(new Transform3D(C.nullRot, new Vector3d(0.0, 0.0, hex.depth()*BoardModel.HEX_HEIGHT), 1.0));
            surface = new Shape3D(polygon);
            surface.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
            surface.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
            surface.setPickable(true);
            setSurfaceEffect(current, tileManager.getTexture(h, 2/3f));
            sTrans.addChild(surface);
            addChild(sTrans);
        }
    }

    public void update(IHex h, TileTextureManager tileManager, Player localPlayer) {
        hex = h;

        Color3f col = new Color3f(GUIPreferences.getInstance().getMapTextColor());
        ypos = 1;

        // This is a bit convoluted in order to reduce work done on a full update,
        // which happens fairly often.
        Texture tex = tileManager.getTexture(hex, 1.0f);
        if (tex != floor.getAppearance().getTexture()) {
            setEffect(current, tex);
        }

        int keep = (surface == null?KEEP:KEEP+1);
        for (int i = numChildren()-1; i >= keep; i--) removeChild(i);

        
        // mine fields
        if (game.containsMinefield((Coords)getUserData())){
            Coords c = (Coords)getUserData();
            Minefield mf = game.getMinefields(c).elementAt(0);

            addChild(tileManager.getMinefieldSign());

            int nbrMfs = game.getNbrMinefields(c);
            if (nbrMfs > 1) {
                addText(Messages.getString("BoardView1.Multiple"), col); //$NON-NLS-1$
            } else if (nbrMfs == 1) {
                switch (mf.getType()) {
                case Minefield.TYPE_CONVENTIONAL:
                    addText(Messages.getString("BoardView1.Conventional"), col); //$NON-NLS-1$
                    break;
                case Minefield.TYPE_INFERNO:
                    addText(Messages.getString("BoardView1.Thunder-Inf") + mf.getDensity() + ")",  col); //$NON-NLS-1$ //$NON-NLS-2$
                    break;
                case Minefield.TYPE_ACTIVE:
                    addText(Messages.getString("BoardView1.Thunder-Actv") + mf.getDensity() + ")", col); //$NON-NLS-1$ //$NON-NLS-2$
                    break;
                case Minefield.TYPE_COMMAND_DETONATED:
                    addText(Messages.getString("BoardView1.detonated"), col); //$NON-NLS-1$
                    addText(Messages.getString("BoardView1.Command-"), col); //$NON-NLS-1$
                    break;
                case Minefield.TYPE_VIBRABOMB:
                    if (localPlayer != null && mf.getPlayerId() == localPlayer.getId())
                        addText(Messages.getString("BoardView1.Vibrabomb")+" (" + mf.getSetting() + ")", col); //$NON-NLS-1$ //$NON-NLS-2$
                    else
                        addText(Messages.getString("BoardView1.Vibrabomb"), col); //$NON-NLS-1$
                    break;
                }
            }
        }

        // smoke, fire, buildings
        // FIXME: this needs much more detailed processing... but it is good enough for now
        int ih = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV), hex.terrainLevel(Terrains.BRIDGE_ELEV));
        double height = Math.max(ih, Math.max(hex.terrainLevel(Terrains.SMOKE), hex.terrainLevel(Terrains.FIRE)))*BoardModel.HEX_HEIGHT;
        if (height < 1.0) height = 1.0;
        height += (hex.surface() - hex.floor()) * BoardModel.HEX_HEIGHT;
        Transform3D supert = new Transform3D();
        supert.setScale(new Vector3d(2*BoardModel.HEX_SIDE_LENGTH, BoardModel.HEX_DIAMETER, height));
        supert.setTranslation(new Vector3d(0.0, 0.0, height/2));
        TransformGroup superTrans = new TransformGroup(supert);
        List<Shape3D> supers = tileManager.getModels(hex);
        for (Shape3D sup : supers) {
            superTrans.addChild(sup);
        }
        
        superTrans.setPickable(false);
        addChild(superTrans);

        // wood
        if (Math.max(hex.terrainLevel(Terrains.WOODS), hex.terrainLevel(Terrains.JUNGLE)) > 0) {
            supert = new Transform3D();
            supert.setScale(new Vector3d(2*BoardModel.HEX_SIDE_LENGTH, BoardModel.HEX_DIAMETER, 2*BoardModel.HEX_HEIGHT));
            supert.setTranslation(new Vector3d(0.0, 0.0, BoardModel.HEX_HEIGHT));
            superTrans = new TransformGroup(supert);
            superTrans.addChild(tileManager.getModel(hex));

            superTrans.setPickable(false);
            addChild(superTrans);
        }

        setBounds(new BoundingBox(
            new Point3d(-BoardModel.HEX_SIDE_LENGTH, -BoardModel.HEX_DIAMETER/2, BoardModel.HEX_HEIGHT*hex.floor()),
            new Point3d(BoardModel.HEX_SIDE_LENGTH, BoardModel.HEX_DIAMETER/2, height)
        ));

        if (hex.getElevation() != 0) addText(Messages.getString("BoardView1.LEVEL") + hex.getElevation(), col);
        if (hex.depth() != 0) addText(Messages.getString("BoardView1.DEPTH") + hex.depth(), col);
        if (ih > 0) addText(Messages.getString("BoardView1.HEIGHT") + ih, 
                new Color3f(GUIPreferences.getInstance().getColor("AdvancedBuildingTextColor")));

    }

    private void setSurfaceEffect(Material mat, Texture tex) {
        Appearance app = new Appearance();
        app.setTexture(tex);
        app.setMaterial((mat == normal?normalWater:mat));
        app.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.0f));
        app.setPolygonAttributes(C.noCull);
        app.setTextureAttributes(C.materialModulate);
        surface.setAppearance(app);
    }

    private void setEffect(Material mat, Texture tex) {
        current = mat;
        Appearance app = new Appearance();
        app.setTexture(tex);
        app.setMaterial(current);
        app.setTextureAttributes(new TextureAttributes(TextureAttributes.MODULATE, new Transform3D(), new Color4f(), TextureAttributes.NICEST));
        floor.setAppearance(app);
        
        if (surface != null) setSurfaceEffect(mat, surface.getAppearance().getTexture());
    }
    
    void setEffect(Material mat) {
        setEffect(mat, floor.getAppearance().getTexture());
    }
    
    public void darken() {
        setEffect(dark);
    }
    
    public void night() {
        setEffect(night);
    }

    public void reset() {
        setEffect(normal);
    }

    private int ypos = 0;
    private final void addText(String s, Color3f col) {
        Transform3D t = new Transform3D();
        t.setTranslation(new Vector3d(0.0, -BoardModel.HEX_DIAMETER/3-3.5+ypos*2.75, hex.depth()*BoardModel.HEX_HEIGHT));
        TransformGroup tg = new TransformGroup(t);
        tg.addChild(new LabelModel(s, col, null, LabelModel.BIG));
        addChild(tg);
        ypos++;
    }

}
