/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class FireExtinguisherHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -7047033962986081773L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public FireExtinguisherHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     *      megamek.common.Entity, boolean)
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        if (!bMissed) {
            r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.add(r);
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                r = new Report(3540);
                r.subject = subjectId;
                r.add(target.getPosition().getBoardNum());
                r.indent(3);
                vPhaseReport.add(r);
                game.getBoard().getHex(target.getPosition()).removeTerrain(
                        Terrains.FIRE);
                server.sendChangedHex(target.getPosition());
                game.getBoard().removeInfernoFrom(target.getPosition());
            } else if (target instanceof Entity) {
                if (entityTarget.infernos.isStillBurning()
                        || (target instanceof Tank && ((Tank) target)
                                .isOnFire())) {
                    r = new Report(3550);
                    r.subject = subjectId;
                    r.addDesc(entityTarget);
                    r.newlines = 0;
                    r.indent(3);
                    vPhaseReport.add(r);
                }
                entityTarget.infernos.clear();
                if (target instanceof Tank) {
                    for (int i = 0; i < entityTarget.locations(); i++)
                        ((Tank) target).extinguishAll();
                }
            }
        }
        return true;
    }
}
