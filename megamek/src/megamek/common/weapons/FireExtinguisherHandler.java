/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

import java.util.Vector;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class FireExtinguisherHandler extends WeaponHandler {
    private static final long serialVersionUID = -7047033962986081773L;

    public FireExtinguisherHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed) {
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.add(r);
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                r = new Report(3540);
                r.subject = subjectId;
                r.add(target.getPosition().getBoardNum());
                r.indent(3);
                vPhaseReport.add(r);
                game.getBoard().getHex(target.getPosition()).removeTerrain(Terrains.FIRE);
                gameManager.sendChangedHex(target.getPosition());
                game.getBoard().removeInfernoFrom(target.getPosition());
            } else if (target instanceof Entity) {
                if (entityTarget.infernos.isStillBurning()
                        || (target instanceof Tank && ((Tank) target).isOnFire())) {
                    r = new Report(3550);
                    r.subject = subjectId;
                    r.addDesc(entityTarget);
                    r.indent(3);
                    vPhaseReport.add(r);
                }
                entityTarget.infernos.clear();
                if (target instanceof Tank) {
                    for (int i = 0; i < entityTarget.locations(); i++) {
                        ((Tank) target).extinguishAll();
                    }
                }
            }
        }
        return true;
    }
}
