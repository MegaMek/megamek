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

import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColorInterpolator;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Material;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;

class BoardModel extends BranchGroup {
    // 3D projection uses metric coordinates according to rulebook
    static final double HEX_DIAMETER = 30.0;
    static final double HEX_HEIGHT = 6.0;
    static final double HEX_SIDE_LENGTH = Math.tan(Math.PI/6)*HEX_DIAMETER;
    static final double UNIT_SIZE = 12.0;
    static final double WRECK_HEIGHT = 1.0;
    static final double INFANTRY_HEIGHT = 2.0;
    static final double BATTLEARMOR_HEIGHT = 3.0;
    static final BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), Double.POSITIVE_INFINITY);
    static final Color3f SKY = new Color3f(.33f, .5f, 1.0f);
    static final Color3f LAND = new Color3f(.66f, .66f, .5f);

    TileTextureManager tileManager;
    SimpleUniverse universe;
    IGame game;
    boolean isDeploying;
    int w;
    int h;
    Material highlight = new Material(C.white, C.black, C.white, C.black, 1.0f);
    ColorInterpolator highlightEffect = new ColorInterpolator(C.dblAlpha, highlight, C.grey75, C.white);
    SharedGroup shared[];
    
    static final int KEEP = 2;
    public BoardModel(TileTextureManager t, SimpleUniverse u, IGame g) {
        tileManager = t;
        universe = u;
        game = g;
        shared = BoardHexModel.mkShared();
        setCapability(BranchGroup.ALLOW_DETACH);
        highlight.setCapability(Material.ALLOW_COMPONENT_READ);
        highlight.setCapability(Material.ALLOW_COMPONENT_WRITE);
        highlightEffect.setSchedulingBounds(BoardModel.bounds);
        addChild(highlightEffect);
        BranchGroup back = new BranchGroup();
        Appearance sapp = new Appearance();
        sapp.setColoringAttributes(new ColoringAttributes(SKY, ColoringAttributes.SHADE_FLAT));
        sapp.setPolygonAttributes(C.noCull);
        sapp.setTexture(tileManager.getTexture("data/images/misc/clouds.jpg"));
        TransformGroup tg = new TransformGroup(new Transform3D(C.mkquat(1, 0, 0, Math.PI/2), new Vector3d(), 1.0));
        tg.addChild(new Sphere(1f, Primitive.GENERATE_TEXTURE_COORDS, 100, sapp));
        back.addChild(tg);
        Background bg = new Background(back);
        bg.setApplicationBounds(bounds);
        addChild(bg);
    }
    
    static final Point3d getHexLocation(Coords c, int level) {
        return new Point3d((c.x*BoardModel.HEX_SIDE_LENGTH*1.5), -(c.y*(BoardModel.HEX_DIAMETER) + ((c.x & 1) != 0? BoardModel.HEX_DIAMETER/2 : 0)), level*BoardModel.HEX_HEIGHT);
    }

    // TODO: optimization, changing single hexes, connecting hexes

    private final BoardHexModel hexAt(int x, int y) {
        return ((BoardHexModel)getChild(y*w+x+KEEP));
    }
    
    void update(Player player) {
        IBoard gboard = game.getBoard();
        detach();

        if (gboard.getWidth() != w || gboard.getHeight() != h) {
            System.out.println("BoardModel: full rebuild");
            for (int i = this.numChildren()-1; i >= KEEP; i--) removeChild(i);

            w = gboard.getWidth();
            h = gboard.getHeight();

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Coords c = new Coords(x, y);
                    BoardHexModel b = new BoardHexModel(game, c, gboard.getHex(c), tileManager, shared);
                    addChild(b);
                }
            }

            DirectionalLight pl = new DirectionalLight(
                true,
                new Color3f(0.5f, 0.45f, 0.4f),
                new Vector3f(-1, 1, -1)
            );
            pl.setInfluencingBounds(bounds);
            addChild(pl);
            pl = new DirectionalLight(
                true,
                new Color3f(0.5f, 0.45f, 0.4f),
                new Vector3f(-1, -0.5f, -2)
            );
            pl.setInfluencingBounds(bounds);
            addChild(pl);

            AmbientLight al = new AmbientLight(new Color3f(0.4f, 0.4f, 0.4f));
            al.setInfluencingBounds(bounds);
            addChild(al);
        } else {
            System.out.println("BoardModel: full update");
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                hexAt(x, y).update(gboard.getHex(x, y), tileManager, player);
            }
        }
        if (!isDeploying) resetBoard();

        universe.addBranchGraph(this);
    }
    
    void update(Coords c, IHex hex, Player player) {
        detach();
        hexAt(c.x, c.y).update(hex, tileManager, player);
        universe.addBranchGraph(this);
    }
    
    public void showDeployment(Player deployer) {
        if (deployer == null) {
            isDeploying = false;
            resetBoard();
            return;
        }
        isDeploying = true;
        IBoard gboard = game.getBoard();

        detach();
        Coords c = new Coords(0,0);
        for (c.y = 0; c.y < h; c.y++) {
            for (c.x = 0; c.x < w; c.x++) {
                if (!gboard.isLegalDeployment(c, deployer)) {
                    hexAt(c.x, c.y).darken();
                } else {
                    hexAt(c.x, c.y).setEffect(highlight);
                }
            }
        }
        universe.addBranchGraph(this);
    }

    public void resetBoard() {
        boolean night = GUIPreferences.getInstance().getBoolean(GUIPreferences.ADVANCED_DARKEN_MAP_AT_NIGHT) && 
            game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DAY;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                BoardHexModel bhm = hexAt(x, y);
                
                if (night && !game.isPositionIlluminated((Coords)bhm.getUserData())) {
                   bhm.night(); 
                } else {
                    bhm.reset();
                }
            }
        }
    }

}
