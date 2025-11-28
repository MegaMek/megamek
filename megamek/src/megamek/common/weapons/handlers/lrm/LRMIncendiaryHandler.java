/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.lrm;

import java.io.Serial;
import java.util.Vector;

import megamek.common.units.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.common.weapons.lrms.ExtendedLRMWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handler for Incendiary LRM ammunition.
 * Per TO:AUE pg 181:
 * - 1 in 5 missiles are incendiary (minimum 1), reducing effective rack size by 20%
 * - Fire TN modifier of -4 against terrain/structures (handled by AmmoType.getFireTN())
 * - +1 damage per 5 missiles against infantry
 * - Cannot set units on fire or raise heat level (unlike Inferno)
 */
public class LRMIncendiaryHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = -3352114767356927227L;

    public LRMIncendiaryHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
            throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " incendiary missile(s) ";
    }

    /**
     * Returns the effective rack size after removing incendiary missiles.
     * Per TO:AUE, 1 in 5 missiles are incendiary (minimum 1).
     * Formula: effectiveSize = rackSize - ceil(rackSize / 5)
     */
    protected int getEffectiveRackSize() {
        int baseRackSize = weaponType.getRackSize();
        int incendiaryCount = (int) Math.ceil(baseRackSize / 5.0);
        return Math.max(1, baseRackSize - incendiaryCount);
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int rackSize = getEffectiveRackSize();

        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.subject = subjectId;
                r.add(rackSize * ((BattleArmor) attackingEntity).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.add(r);
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            Report r = new Report(3326);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(rackSize);
            r.add(sSalvoType);
            vPhaseReport.add(r);
            return 1;
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target : null;
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(false);

        // Incendiary LRM is NOT compatible with Artemis (requires Artemis-capable ammo)
        // Check for NARC bonus only
        AmmoType ammoType = ammo.getType();

        if (!weapon.curMode().equals("Indirect")) {
            // NARC bonus check
            if ((entityTarget != null)
                    && (entityTarget.isNarcedBy(attackingEntity.getOwner().getTeam())
                    || entityTarget.isINarcedBy(attackingEntity.getOwner().getTeam()))) {
                boolean bTargetECMAffected = ComputeECM.isAffectedByECM(attackingEntity,
                        target.getPosition(),
                        target.getPosition());
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE)) {
                    if (bTargetECMAffected) {
                        Report r = new Report(3330);
                        r.subject = subjectId;
                        r.newlines = 0;
                        vPhaseReport.addElement(r);
                    } else {
                        nMissilesModifier += 2;
                    }
                }
            }
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
                && entityTarget != null && entityTarget.isLargeCraft()) {
            nMissilesModifier -= (int) Math.floor(getAeroSanityAMSHitsMod());
        }

        boolean minRangeELRMAttack = false;

        // ELRM handling (unlikely to combine with Incendiary, but preserve behavior)
        if (!game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            if (weaponType instanceof ExtendedLRMWeapon
                    && !game.getBoard().isSpace()
                    && (nRange <= weaponType.getMinimumRange())) {
                rackSize = rackSize / 2 + rackSize % 2;
                minRangeELRMAttack = true;
            }
        }

        if (allShotsHit()) {
            missilesHit = Compute.missilesHit(rackSize,
                    nMissilesModifier, weapon.isHotLoaded(), true,
                    isAdvancedAMS());
        } else {
            if (attackingEntity instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(rackSize
                                * ((BattleArmor) attackingEntity).getShootingStrength(),
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(rackSize,
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        isAdvancedAMS());
            }
        }

        if (missilesHit > 0) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMissilesModifier != 0) {
                if (nMissilesModifier > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            if (minRangeELRMAttack) {
                r = new Report(3342);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    @Override
    protected int calcDamagePerHit() {
        // For infantry targets, apply +1 damage per 5 missiles bonus
        if (target.isConventionalInfantry()) {
            int effectiveRack = getEffectiveRackSize();
            // Calculate bonus: +1 per 5 missiles (round up)
            int bonusDamage = (int) Math.ceil(effectiveRack / 5.0);

            // Get base infantry damage
            double toReturn = Compute.directBlowInfantryDamage(
                    effectiveRack, bDirect ? toHit.getMoS() / 3 : 0,
                    weaponType.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);

            // Add incendiary bonus damage
            toReturn += bonusDamage;

            toReturn = applyGlancingBlowModifier(toReturn, false);
            return (int) toReturn;
        }

        // Battle Armor also gets bonus damage
        if (target instanceof BattleArmor) {
            int effectiveRack = getEffectiveRackSize();
            int bonusDamage = (int) Math.ceil(effectiveRack / 5.0);
            return 1 + bonusDamage;
        }

        // Standard 1 damage per missile for other targets
        return 1;
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        // Fire checks already use lower TN via AmmoType.getFireTN() returning 5
        // Standard handling otherwise
        super.handleClearDamage(vPhaseReport, bldg, nDamage);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
            IBuilding bldg, int hits, int nCluster, int bldgAbsorbs) {
        // Incendiary LRMs do NOT add heat to targets (unlike Inferno missiles)
        // They also cannot set units on fire
        // Standard damage handling only
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
    }
}
