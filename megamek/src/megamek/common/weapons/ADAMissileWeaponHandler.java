/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;

import java.util.Vector;

/**
 * @author Sebastian Brocks
 */
public class ADAMissileWeaponHandler extends MissileWeaponHandler {
    private static final long serialVersionUID = 6329291710822071023L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public ADAMissileWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
                                   GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 20;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }

    @Override
    protected int calcnCluster() {
        return 1;
    }
}
