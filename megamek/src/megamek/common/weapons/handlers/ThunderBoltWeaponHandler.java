/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class ThunderBoltWeaponHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6329291710822071023L;

    /**
     *
     */
    public ThunderBoltWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
          TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType ammoType = ammo.getType();
        double toReturn = ammoType.getDamagePerShot();
        int minRange;
        if (attackingEntity.isAirborne()) {
            minRange = weaponType.getATRanges()[RangeType.RANGE_MINIMUM];
        } else {
            minRange = weaponType.getMinimumRange();
        }
        if ((nRange <= minRange) && !weapon.isHotLoaded()) {
            toReturn /= 2;
            toReturn = Math.floor(toReturn);
        }
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }
        return (int) Math.ceil(toReturn);
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        double counterAV = calcCounterAV();
        int armor = weaponType.getMissileArmor();
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);
        if (range == WeaponType.RANGE_SHORT) {
            av = weaponType.getRoundShortAV();
        } else if (range == WeaponType.RANGE_MED) {
            av = weaponType.getRoundMedAV();
        } else if (range == WeaponType.RANGE_LONG) {
            av = weaponType.getRoundLongAV();
        } else if (range == WeaponType.RANGE_EXT) {
            av = weaponType.getRoundExtAV();
        }

        // For squadrons, total the missile armor for the launched volley
        if (attackingEntity.isCapitalFighter()) {
            armor = armor * numWeapons;
        }
        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();

        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);

        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (av);
    }

    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }

    //Thunderbolts apply damage all in one block.
    //This was referenced incorrectly for Aero damage.
    @Override
    protected boolean usesClusterTable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.MissileWeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Activate single AMS
        getAMSHitsMod(vPhaseReport);
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            // Or bay AMS if Aero Sanity is on
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                if (getParentBayHandler() != null) {
                    WeaponHandler bayHandler = getParentBayHandler();
                    amsBayEngagedCap = bayHandler.amsBayEngagedCap;
                    pdBayEngagedCap = bayHandler.pdBayEngagedCap;
                }
            }
        }
        bSalvo = true;
        // Report AMS/Point defense failure due to Overheating.
        if (pdOverheated
              && (!(amsBayEngaged
              || amsBayEngagedCap
              || amsBayEngagedMissile
              || pdBayEngaged
              || pdBayEngagedCap
              || pdBayEngagedMissile))) {
            Report r = new Report(3359);
            r.subject = subjectId;
            r.indent();
            vPhaseReport.addElement(r);
        }
        if (amsEngaged || apdsEngaged) {
            Report r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add("missile");
                r.add(diceRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("missile");
            r.add(diceRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
        return 1;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
    }

    @Override
    // For AntiShip missiles, which behave more like Thunderbolts than capital missiles except for this
    // All other thunderbolt type large missiles should be unable to score a critical hit here
    protected int getCapMisMod() {
        if (weaponType.hasFlag(WeaponType.F_ANTI_SHIP)) {
            return 11;
        } else {
            return 0;
        }
    }

}
