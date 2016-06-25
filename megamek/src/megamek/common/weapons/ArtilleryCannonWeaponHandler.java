/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Numien, based work by Sebastian Brocks
 */
public class ArtilleryCannonWeaponHandler extends AmmoWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    boolean handledAmmoAndReport = false;

    /**
     * This consructor may only be used for deserialization.
     */
    protected ArtilleryCannonWeaponHandler() {
        super();
    }

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryCannonWeaponHandler(ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        Coords targetPos = target.getPosition();
        boolean isFlak = (target instanceof VTOL) || (target instanceof Aero);
        boolean asfFlak = target instanceof Aero;
        if (ae == null) {
            System.err.println("Artillery Entity is null!");
            return true;
        }
        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType atype = ammoUsed == null ? null : (AmmoType) ammoUsed
                .getType();

        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (wtype != null) {
            r.add(wtype.getName());
        } else {
            r.add("Error: From Nowhwere");
        }

        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledAmmoAndReport) {
            addHeat();
        }
        if (!bMissed) {
            if (!isFlak) {
                r = new Report(3190);
            } else {
                r = new Report(3191);
            }
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            targetPos = Compute.scatter(targetPos,
                    (Math.abs(toHit.getMoS()) + 1) / 2);
            if (game.getBoard().contains(targetPos)) {
                // misses and scatters to another hex
                if (!isFlak) {
                    r = new Report(3195);
                } else {
                    r = new Report(3192);
                }
                r.subject = subjectId;
                r.add(targetPos.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                if (isFlak) {
                    r = new Report(3193);
                } else {
                    r = new Report(3200);
                }
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        // According to TacOps eratta, artillery cannons can only fire standard
        // rounds.
        // But, they're still in as unofficial tech, because they're fun. :)
        if (atype.getMunitionType() == AmmoType.M_FLARE) {
            int radius;
            if (atype.getAmmoType() == AmmoType.T_LONG_TOM) {
                radius = 3;
            } else if (atype.getAmmoType() == AmmoType.T_SNIPER) {
                radius = 2;
            } else {
                radius = 1;
            }
            server.deliverArtilleryFlare(targetPos, radius);
            return false;
        }
        if (atype.getMunitionType() == AmmoType.M_DAVY_CROCKETT_M) {
            // The appropriate term here is "Bwahahahahaha..."
            server.doNuclearExplosion(targetPos, 1, vPhaseReport);
            return false;
        }
        if (atype.getMunitionType() == AmmoType.M_FASCAM) {
            server.deliverFASCAMMinefield(targetPos, ae.getOwner().getId(),
                    atype.getRackSize(), ae.getId());
            return false;
        }
        if (atype.getMunitionType() == AmmoType.M_SMOKE) {
            server.deliverArtillerySmoke(targetPos, vPhaseReport);
            return false;
        }
        int altitude = 0;
        if (isFlak) {
            altitude = target.getElevation();
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        // TODO: Does this apply to arty cannons?
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        if (mineClear && game.containsMinefield(targetPos) && !isFlak
                && !bMissed) {
            r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

            Enumeration<Minefield> minefields = game.getMinefields(targetPos)
                    .elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<Minefield>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (server.clearMinefield(mf, ae,
                        Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                server.removeMinefield(mf);
            }
        }

        server.artilleryDamageArea(targetPos, ae.getPosition(), atype,
                subjectId, ae, isFlak, altitude, mineClear, vPhaseReport,
                asfFlak, -1);

        // artillery may unintentionally clear minefields, but only if it wasn't
        // trying to
        // TODO: Does this apply to arty cannons?
        if (!mineClear && game.containsMinefield(targetPos)) {
            Enumeration<Minefield> minefields = game.getMinefields(targetPos)
                    .elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<Minefield>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (server.clearMinefield(mf, ae, 10, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                server.removeMinefield(mf);
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        float toReturn = wtype.getDamage();
        // area effect damage is double
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn /= 0.5;
        }

        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        // System.err.println("Attack is doing " + toReturn + " damage.");

        return (int) Math.ceil(toReturn);
    }
}
