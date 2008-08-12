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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.vecmath.Point3d;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.UnitLocation;

/**
 *
 * @author jwalt
 */
class EntityGroup extends BranchGroup {
    IGame game;
    ViewTransform currentView;
    TileTextureManager tileManager;
    HashMap<C3LinkModel, C3LinkModel> c3links = new HashMap<C3LinkModel, C3LinkModel>();

    public EntityGroup(IGame g, TileTextureManager t, ViewTransform v) {
        game = g;
        tileManager = t;
        currentView = v;
        setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        compile();
    }

    private EntityModel find(Entity entity) {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements(); ) {
            Node n = (Node)e.nextElement();
            if (n instanceof EntityModel && entity.equals(n.getUserData())) {
                return (EntityModel)n;
            }
        }
        return null;
    }
    
    public void move(Entity entity, Vector<UnitLocation> movePath) {
        EntityModel em = find(entity);

        if (movePath.size() < 1 || em == null) {
            update(entity, em);
            return;
        }

        em.move(entity, movePath, game.getBoard());

        Coords c = movePath.elementAt(0).getCoords();
        currentView.centerOnHex(c, game.getBoard().getHex(c));
    }

    public boolean isMoving() {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof EntityModel && ((EntityModel)o).isMoving()) return true;
        }
        return false;
    }

    public void remove(Entity entity) {
        EntityModel em = find(entity);
        if (em != null) em.detach();
    }

    public void clear() {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            ((BranchGroup)e.nextElement()).detach();
        }
    }

    public void update(Entity entity) {
        EntityModel em = find(entity);
        update(entity, em);
    }

    void removeC3LinksFor(Entity entity) {
        Vector<C3LinkModel> remove = new Vector<C3LinkModel>();
        for (C3LinkModel m : c3links.values()) {
            if (m.src.getId() == entity.getId() || m.dst.getId() == entity.getId()) {
                remove.add(m);
                m.detach();
            }
        }
        for (C3LinkModel m : remove) c3links.remove(m);
    }

    private void update(Entity entity, EntityModel em) {
        if (em != null) em.detach();
        IHex hex = game.getBoard().getHex(entity.getPosition());
        if (hex != null) {
            removeC3LinksFor(entity);
            addChild(new EntityModel(entity, tileManager, currentView, game));
            if (entity.hasC3() || entity.hasC3i()) addC3LinksFor(entity, null);
        }
    }

    public void update() {
        clear();
        
        final IBoard gboard = game.getBoard();
        if (gboard == null) return;

        for (Enumeration<?> i = game.getEntities(); i.hasMoreElements();) {
            update((Entity)i.nextElement(), null);
        }

        if (GUIPreferences.getInstance().getShowWrecks()) {
            for (Enumeration<Entity> e = game.getWreckedEntities(); e.hasMoreElements();) {
                Entity entity = e.nextElement();
                if (!(entity instanceof Infantry)) {
                    update(entity, null);
                }
            }
        }
    }
    
    void addC3LinksFor(Entity e, Point3d pos) {
        if (e.getPosition() == null)
            return;

        if (e.hasC3i()) {
            for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                final Entity fe = i.nextElement();
                if (fe.getPosition() != null && e.onSameC3NetworkAs(fe) && !fe.equals(e) &&
                        !Compute.isAffectedByECM(e, e.getPosition(), fe.getPosition())) {
                    C3LinkModel m = new C3LinkModel(e, fe, game, pos);
                    if (!c3links.containsKey(m)) {
                        addChild(m);
                        c3links.put(m, m);
                    }
                }
            }
        } else if (e.getC3Master() != null) {
            Entity eMaster = e.getC3Master();
            if (eMaster.getPosition() == null) return;

            if (!Compute.isAffectedByECM(e, e.getPosition(), eMaster.getPosition())
                    && !Compute.isAffectedByECM(eMaster, eMaster.getPosition(), eMaster.getPosition())) {
                C3LinkModel m = new C3LinkModel(e, eMaster, game, pos);
                if (!c3links.containsKey(m)) {
                    addChild(m);
                    c3links.put(m, m);
                }
            }
        }
    }

    void setView(ViewTransform v) {
        currentView = v;
        update();
    }
}
