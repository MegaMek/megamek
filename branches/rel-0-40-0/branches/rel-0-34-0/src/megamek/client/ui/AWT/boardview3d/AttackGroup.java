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
import javax.media.j3d.BranchGroup;
import megamek.client.ui.AWT.TilesetManager;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.*;

/**
 *
 * @author jwalt
 */
class AttackGroup extends BranchGroup {
    IGame game;
    ViewTransform currentView;
    TileTextureManager tileManager;

    Entity selectedEntity;
    Mounted selectedWeapon;
    

    public AttackGroup(IGame g, TileTextureManager t, ViewTransform v) {
        game = g;
        currentView = v;
        tileManager = t;
        setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        setPickable(false);
    }
    
    public void add(AttackAction aa) {
        if (aa instanceof ArtilleryAttackAction) {
            add((ArtilleryAttackAction)aa);
            return;
        }
        Entity ae = game.getEntity(aa.getEntityId());
        Targetable t = game.getTarget(aa.getTargetType(), aa.getTargetId());
        if (ae == null || t == null 
                || t.getTargetType() == Targetable.TYPE_INARC_POD 
                || t.getPosition() == null
                || ae.getPosition() == null
                || !game.getBoard().contains(ae.getPosition())
                || !game.getBoard().contains(t.getPosition())) {
            return;
        }
        
        AttackModel attack = null;

        attack = new AttackModel(aa, ae, t, game);
        
        attack.add(aa, currentView);

        addChild(attack);
    }
    
    public void add(ArtilleryAttackAction aaa) {
        addChild(new ArtilleryAttackModel(TilesetManager.ARTILLERY_INCOMING, aaa, game, tileManager));
    }

    public void remove(Entity entity) {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            BranchGroup bg = (BranchGroup)e.nextElement();
            AttackAction a = (AttackAction)bg.getUserData();
            if (a != null && a.getEntityId() == entity.getId()) bg.detach();
        }
    }

    public void clear() {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            ((BranchGroup)e.nextElement()).detach();
        }
    }

    public void update() {
        clear();
        for (EntityAction ea : game.getActionsVector()) {
            if (ea instanceof AttackAction) {
                add((AttackAction)ea);
            }
        }
        for (AttackAction ea : game.getChargesVector()) {
            if (ea instanceof PhysicalAttackAction) {
                add(ea);
            }
        }

        if (selectedWeapon != null && selectedEntity != null) {
            IBoard gboard = game.getBoard();
            Coords c = new Coords();
            for (c.y = 0; c.y < gboard.getHeight(); c.y++) {
                for (c.x = 0; c.x < gboard.getWidth(); c.x++) {
                    if (selectedEntity.getOwner().getArtyAutoHitHexes().contains(c)) {
                        addChild(new ArtilleryAttackModel(TilesetManager.ARTILLERY_AUTOHIT, c, game, tileManager));
                    } else if (selectedEntity.aTracker.getModifier(selectedWeapon, c) != 0) {
                        addChild(new ArtilleryAttackModel(TilesetManager.ARTILLERY_ADJUSTED, c, game, tileManager));
                    }
                }
            }
        }

        for (Enumeration<ArtilleryAttackAction> attacks=game.getArtilleryAttacks(); attacks.hasMoreElements();) {
            add(attacks.nextElement());
        }
    }

    void setView(ViewTransform v) {
        currentView = v;
        update();
    }

    void setSelected(Entity e, Mounted w, Player p) {
        if (w == null || e == null || !e.getOwner().equals(p) || e.getEquipmentNum(w) == -1 ||
                !(w.getType() instanceof WeaponType) || !w.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            w = null;
            e = null;
        }
        if ((e == null) != (selectedEntity == null) || (e != null && e.getId() != selectedEntity.getId()) || w != selectedWeapon) {
            selectedEntity = e;
            selectedWeapon = w;
            update();
        }
    }
}
