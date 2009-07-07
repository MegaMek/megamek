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
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.*;

class AttackModel extends ArrowModel {
    IGame game;
    Vector3d labelLocation;
    Color3f color;
    Entity src;
    Targetable trg;

    public AttackModel(AttackAction aa, Entity ae, Targetable t, IGame game) {
        this.game = game;
        src = ae;
        trg = t;

        IHex shex = game.getBoard().getHex(ae.getPosition());
        IHex thex = game.getBoard().getHex(t.getPosition());
        Point3d source =  BoardModel.getHexLocation(ae.getPosition(), shex.surface()+ae.height());
        source.z += BoardModel.HEX_HEIGHT/2;
        Point3d target = BoardModel.getHexLocation(t.getPosition(), thex.surface()+t.getHeight());
        target.z += BoardModel.HEX_HEIGHT/2;
        
        setCapability(ALLOW_DETACH);
        setUserData(aa);

        color = new Color3f(PlayerColors.getColor(ae.getOwner().getColorIndex()));
        color.scale(.5f);

        Appearance base = new Appearance();
        base.setMaterial(new Material(color, C.black, color, C.white, 64.0f));
        base.setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.SHADE_FLAT));
        base.setPolygonAttributes(C.noCull);
        base.setLineAttributes(C.defLine);

        Shape3D arrow = new Shape3D(makeArrow(source.distance(target)), base);
        Shape3D outline = new Shape3D(makeArrowOutline(source.distance(target)), base);

        anim.addChild(arrow);
        anim.addChild(outline);

        Vector3d target0 = new Vector3d(source);
        target0.negate();
        target0.add(target);
        target0.y = -target0.y;
        double angle = target0.angle(new Vector3d(0.0, -1.0, 0.0));
        target0.cross(target0, new Vector3d(0.0, -1.0, 0.0));
        Quat4d rot = new Quat4d();
        rot.set(new AxisAngle4d(target0, angle));

        TransformGroup tg = new TransformGroup(new Transform3D(rot, new Vector3d(source), 1.0));
        tg.addChild(anim);
        
        labelLocation = new Vector3d(target);
        labelLocation.negate();
        labelLocation.add(source);
        labelLocation.normalize();
        labelLocation.scale(BoardModel.HEX_DIAMETER/2);
        labelLocation.add(target);
        labelLocation.z += BoardModel.HEX_HEIGHT + .1;

        addChild(tg);
    }
    
    void add(AttackAction aa, ViewTransform currentView) {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            Node tg = (Node)e.nextElement();
            if (aa.equals(tg.getUserData())) return;
        }
        
        String label = "?";
        
        if (aa instanceof WeaponAttackAction) {
            WeaponAttackAction action = (WeaponAttackAction)aa;
            final WeaponType wtype = (WeaponType)game.getEntity(aa.getEntityId()).getEquipment(action.getWeaponId()).getType();
            label = wtype.getName()+Messages.getString("BoardView1.needs")+
                action.toHit(game).getValueAsString()+" "+action.toHit(game).getTableDesc();
        } else if (aa instanceof KickAttackAction) {
            KickAttackAction action = (KickAttackAction)aa;
            switch (action.getLeg()) {
            case KickAttackAction.BOTH:
                label = Messages.getString("BoardView1.kickBoth", new Object[] {
                    KickAttackAction.toHit(game, aa.getEntityId(), trg, KickAttackAction.LEFT).getValueAsString(),
                    KickAttackAction.toHit(game, aa.getEntityId(), trg, KickAttackAction.RIGHT).getValueAsString(),
                });
                break;
            case KickAttackAction.LEFT:
                label = Messages.getString("BoardView1.kickLeft", new Object[] {
                    KickAttackAction.toHit(game, aa.getEntityId(), trg, KickAttackAction.LEFT).getValueAsString(),
                });
                break;
            case KickAttackAction.RIGHT:
                label = Messages.getString("BoardView1.kickRight", new Object[] {
                    KickAttackAction.toHit(game, aa.getEntityId(), trg, KickAttackAction.RIGHT).getValueAsString(),
                });
                break;
            }
        } else if (aa instanceof PunchAttackAction) {
            PunchAttackAction action = (PunchAttackAction)aa;
            switch (action.getArm()) {
            case PunchAttackAction.BOTH:
                label = Messages.getString("BoardView1.punchBoth", new Object[] {
                    PunchAttackAction.toHit(game, aa.getEntityId(), trg, PunchAttackAction.LEFT).getValueAsString(),
                    PunchAttackAction.toHit(game, aa.getEntityId(), trg, PunchAttackAction.RIGHT).getValueAsString(),
                });
                break;
            case PunchAttackAction.LEFT:
                label = Messages.getString("BoardView1.punchLeft", new Object[] {
                    PunchAttackAction.toHit(game, aa.getEntityId(), trg, PunchAttackAction.LEFT).getValueAsString(),
                });
                break;
            case PunchAttackAction.RIGHT:
                label = Messages.getString("BoardView1.punchRight", new Object[] {
                    PunchAttackAction.toHit(game, aa.getEntityId(), trg, PunchAttackAction.RIGHT).getValueAsString(),
                });
                break;
            }
        } else if (aa instanceof PushAttackAction) {
            label = Messages.getString("BoardView1.push", new Object[] {
                ((PushAttackAction)aa).toHit(game).getValueAsString()
            });
        } else if (aa instanceof ClubAttackAction) {
            label = Messages.getString("BoardView1.hit", new Object[] {
                ((ClubAttackAction)aa).getClub().getName(),
                ((ClubAttackAction)aa).toHit(game).getValueAsString(),
            });
        } else if (aa instanceof ChargeAttackAction) {
            label = Messages.getString("BoardView1.charge", new Object[] {
                ((ChargeAttackAction)aa).toHit(game).getValueAsString()
            });
        } else if (aa instanceof DfaAttackAction) {
            label = Messages.getString("BoardView1.DFA", new Object[] {
                ((DfaAttackAction)aa).toHit(game).getValueAsString()
            });
        } else if (aa instanceof ProtomechPhysicalAttackAction) {
            label = Messages.getString("BoardView1.proto", new Object[] {
                ((ProtomechPhysicalAttackAction)aa).toHit(game).getValueAsString()
            });
        } else if (aa instanceof SearchlightAttackAction) {
            label = Messages.getString("BoardView1.Searchlight");
        }
        
        TransformGroup l = new TransformGroup(new Transform3D(C.nullRot, labelLocation, 1.0));
        labelLocation.z += 3;
        labelLocation.y += 3;
        l.addChild(currentView.makeViewRelative(new LabelModel(label, C.black, color, LabelModel.BIGBOLD), 0.0));
        l.setUserData(aa);

        addChild(l);
    }

}
