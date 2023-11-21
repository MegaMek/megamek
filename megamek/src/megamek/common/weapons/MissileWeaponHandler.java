/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author Sebastian Brocks
 */
public class MissileWeaponHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -4801130911083653548L;
    boolean advancedAMS = false;
    boolean advancedPD = false;
    boolean multiAMS = false;

    public MissileWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
        generalDamageType = HitData.DAMAGE_MISSILE;
        advancedAMS = g.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMS);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
        multiAMS = g.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_MULTI_USE_AMS);
        sSalvoType = " missile(s) ";
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.subject = subjectId;
                int shootingStrength = 1;
                if ((weapon.getLocation() == BattleArmor.LOC_SQUAD)
                        && !(weapon.isSquadSupportWeapon())) {
                    shootingStrength = ((BattleArmor) ae).getShootingStrength();
                }
                r.add(wtype.getRackSize() * shootingStrength);
                r.add(sSalvoType);
                r.add(" ");
                vPhaseReport.add(r);
                return shootingStrength;
            }
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(wtype.getRackSize());
            r.add(sSalvoType);
            r.add(" ");
            vPhaseReport.add(r);
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(true);

        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mech) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }
        Mounted mLinker = weapon.getLinkedBy();
        AmmoType atype = (AmmoType) ammo.getType();

        // is any hex in the flight path of the missile ECM affected?
        boolean bECMAffected = false;
        // if the attacker is affected by ECM or the target is protected by ECM
        // then act as if affected.
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            bECMAffected = true;
        }

        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 2;
            }
        } else if (((mLinker != null)
                && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 1;
            }
        } else if (((mLinker != null)
                && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 3;
            }
        } else if (((mLinker != null)
                && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_APOLLO))
                && (atype.getAmmoType() == AmmoType.T_MRM)) {
            nMissilesModifier -= 1;
        } else if (atype.getAmmoType() == AmmoType.T_ATM) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 2;
            }
        } else if ((entityTarget != null)
                && (entityTarget.isNarcedBy(ae.getOwner().getTeam())
                        || entityTarget.isINarcedBy(ae.getOwner().getTeam()))) {
            // only apply Narc bonus if we're not suffering ECM effect
            // and we are using narc ammo, and we're not firing indirectly.
            // narc capable missiles are only affected if the narc pod, which
            // sits on the target, is ECM affected
            boolean bTargetECMAffected = false;
            bTargetECMAffected = ComputeECM.isAffectedByECM(ae, target.getPosition(), target.getPosition());
            if (((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_SRM)
                    || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))
                    && ((weapon.curMode() == null) || !weapon.curMode().equals("Indirect"))) {
                if (bTargetECMAffected) {
                    // ECM prevents bonus
                    Report r = new Report(3330);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else {
                    nMissilesModifier += 2;
                }
            }
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && entityTarget != null && entityTarget.isLargeCraft()) {
            nMissilesModifier -= getAeroSanityAMSHitsMod();
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(wtype.getRackSize(), nMissilesModifier,
                    weapon.isHotLoaded(), true, isAdvancedAMS());
        } else {
            if (ae instanceof BattleArmor) {
                int shootingStrength = 1;
                if ((weapon.getLocation() == BattleArmor.LOC_SQUAD)
                        && !weapon.isSquadSupportWeapon()) {
                    shootingStrength = ((BattleArmor) ae).getShootingStrength();
                }
                missilesHit = Compute.missilesHit(wtype.getRackSize() * shootingStrength,
                        nMissilesModifier, weapon.isHotLoaded(), false, isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(wtype.getRackSize(), nMissilesModifier,
                        weapon.isHotLoaded(), false, isAdvancedAMS());
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
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    @Override
    protected int calcnCluster() {
        return 5;
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            toReturn = applyGlancingBlowModifier(toReturn, false);
            return (int) toReturn;
        }
        return 1;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);
        if (range == WeaponType.RANGE_SHORT) {
            av = wtype.getRoundShortAV();
        } else if (range == WeaponType.RANGE_MED) {
            av = wtype.getRoundMedAV();
        } else if (range == WeaponType.RANGE_LONG) {
            av = wtype.getRoundLongAV();
        } else if (range == WeaponType.RANGE_EXT) {
            av = wtype.getRoundExtAV();
        }
        Mounted mLinker = weapon.getLinkedBy();
        AmmoType atype = (AmmoType) ammo.getType();
        int bonus = 0;
        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
            // MML3 gets no bonus from Artemis IV (how sad)
            if (atype.getRackSize() > 3) {
                bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
                if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP))  {
                    bonus = 2;
                }
            }
        }

        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
            // MML3 gets no bonus from Artemis IV (how sad)
            if (atype.getRackSize() > 3) {
                bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
                if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP)) {
                    bonus = 1;
                }
            }
        }

        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
            // MML3 WOULD get a bonus from Artemis V, if you were crazy enough
            // to cross-tech it
            bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
            if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP)) {
                bonus = 2;
            }
        }
        av = av + bonus;
        if ((atype.getAmmoType() == AmmoType.T_MML) && !atype.hasFlag(AmmoType.F_MML_LRM)) {
            av = av * 2;
        }
        // Set the Capital Fighter AV here. We'll apply counterAV to this later
        originalAV = av;

        // Point Defenses engage the missiles still aimed at us
        if (ae.usesWeaponBays() || ae.isCapitalFighter()) {
            av = av - calcCounterAV();
        }

        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);
        return (int) Math.floor(getBracketingMultiplier() * av);
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngaged = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngaged = true;
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
                                        Building bldg, Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if ((entityTarget != null)
                && !entityTarget.isAirborne()
                && !entityTarget.isAirborneVTOLorWIGE()
                && ((bldg == null) && (wtype.getFireTN() != TargetRoll.IMPOSSIBLE))) {
            gameManager.tryIgniteHex(target.getPosition(), subjectId, false, false,
                    new TargetRoll(wtype.getFireTN(), wtype.getName()), 3, vPhaseReport);
        }

        // shots that miss an entity can also potential cause explosions in a
        // heavy industrial hex
        gameManager.checkExplodeIndustrialZone(target.getPosition(), vPhaseReport);

        // Report any AMS action.
        if (amsEngaged) {
            Report r = new Report(3230);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        // Report any APDS action.
        if (apdsEngaged) {
            Report r = new Report(3231);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        // TW, pg. 171 - shots that miss a target in a building don't damage the
        // building, unless the attacker is adjacent
        if (!bldgDamagedOnMiss
                || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
            return false;
        }

        return true;
    }

    // Aero sanity reduces effectiveness of AMS bays with default cluster mods.
    // This attempts to account for that, but might need some balancing...
    protected double getAeroSanityAMSHitsMod() {
        if (getParentBayHandler() != null) {
            WeaponHandler bayHandler = getParentBayHandler();
            double counterAVMod = bayHandler.getCounterAV() / bayHandler.weapon.getBayWeapons().size();
            // use this if point defenses engage the missiles
            if (bayHandler.pdOverheated) {
                // Halve the effectiveness
                counterAVMod /= 2.0;
            }
            // Now report and apply the effect, if any
            if (bayHandler.amsBayEngaged || bayHandler.pdBayEngaged) {
                // Let's try to mimic reduced AMS effectiveness against higher munition attack values
                // Set a minimum -4 (default AMS mod)
                return Math.max((counterAVMod / calcDamagePerHit()), 2);
            }
        } else if (getCounterAV() > 0) {
            // Good for squadron missile fire. This may get divided up against too many missile racks to produce a result.
            // Set a minimum -4 (default AMS mod)
            return Math.max((getCounterAV() / nweaponsHit), 4);
        }
        return 0;
    }

    protected int getAMSHitsMod(Vector<Report> vPhaseReport) {
        if ((target == null)
                || (target.getTargetType() != Targetable.TYPE_ENTITY)
                || CounterAV > 0) {
            return 0;
        }
        int apdsMod = 0;
        int amsMod = 0;
        Entity entityTarget = (Entity) target;
        // any AMS attacks by the target?
        ArrayList<Mounted> lCounters = waa.getCounterEquipment();
        if (null != lCounters) {
            // resolve AMS counter-fire
            for (Mounted counter : lCounters) {
                // Set up differences between different types of AMS
                boolean isAMS = counter.getType().hasFlag(WeaponType.F_AMS);
                boolean isAMSBay = counter.getType().hasFlag(WeaponType.F_AMSBAY);
                boolean isAPDS = counter.isAPDS();

                // Only one AMS and one APDS can engage each missile attack
                if (isAMS && amsEngaged) {
                    continue;
                } else if (isAPDS && apdsEngaged) {
                    continue;
                }

                // Check the firing arc, even though this was done when the AMS was assigned
                Entity pdEnt = counter.getEntity();
                boolean isInArc;
                // If the defending unit is the target, use attacker for arc
                if (entityTarget.equals(pdEnt)) {
                    isInArc = Compute.isInArc(game, entityTarget.getId(),
                            entityTarget.getEquipmentNum(counter), ae);
                } else {
                    // Otherwise, the attack target must be in arc
                    isInArc = Compute.isInArc(game, pdEnt.getId(), pdEnt.getEquipmentNum(counter),
                            entityTarget);
                }

                if (!isInArc) {
                    continue;
                }

                // Point defenses can't fire if they're not ready for any other reason
                if (!(counter.getType() instanceof WeaponType)
                        || !counter.isReady() || counter.isMissing()
                        // no AMS when a shield in the AMS location
                        || (pdEnt.hasShield() && pdEnt.hasActiveShield(counter.getLocation(), false))
                        // shutdown means no AMS
                        || pdEnt.isShutDown()) {
                    continue;
                }

                // If we're an AMSBay, heat and ammo must be calculated differently
                if (isAMSBay) {
                    for (int wId : counter.getBayWeapons()) {
                        Mounted bayW = entityTarget.getEquipment(wId);
                        Mounted bayWAmmo = bayW.getLinked();
                        // For AMS bays, stop the loop if an AMS in the bay has engaged this attack
                        if (amsEngaged) {
                            break;
                        }
                        // For AMS bays, continue until we find an individual AMS that hasn't shot yet
                        if (bayW.isUsedThisRound()) {
                            continue;
                        }

                        // build up some heat (assume target is ams owner)
                        if (bayW.getType().hasFlag(WeaponType.F_HEATASDICE)) {
                            pdEnt.heatBuildup += Compute.d6(bayW.getCurrentHeat());
                        } else {
                            pdEnt.heatBuildup += bayW.getCurrentHeat();
                        }

                        // decrement the ammo
                        if (bayWAmmo != null) {
                            bayWAmmo.setShotsLeft(Math.max(0, bayWAmmo.getBaseShotsLeft() - 1));
                        }

                        //Optional rule to allow multiple AMS shots per round
                        if (!multiAMS) {
                            // set the ams as having fired, which is checked by isReady()
                            bayW.setUsedThisRound(true);
                        }
                        amsEngaged = true;
                    }
                } else {
                    // build up some heat
                    if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
                        pdEnt.heatBuildup += Compute.d6(counter.getCurrentHeat());
                    } else {
                        pdEnt.heatBuildup += counter.getCurrentHeat();
                    }

                    // decrement the ammo
                    Mounted mAmmo = counter.getLinked();
                    if (mAmmo != null) {
                        mAmmo.setShotsLeft(Math.max(0, mAmmo.getBaseShotsLeft() - 1));
                    }

                    // Optional rule to allow multiple AMS shots per round
                    if (!multiAMS) {
                        // set the ams as having fired
                        counter.setUsedThisRound(true);
                    }

                    if (isAMS) {
                        amsEngaged = true;
                    }

                    if (isAPDS) {
                        apdsEngaged = true;
                    }
                }
                // Determine APDS mod
                if (apdsEngaged) {
                    int dist = target.getPosition().distance(pdEnt.getPosition());
                    int minApdsMod = -4;
                    if (pdEnt instanceof BattleArmor) {
                        int numTroopers = ((BattleArmor) pdEnt).getNumberActiverTroopers();
                        switch (numTroopers) {
                            case 1:
                                minApdsMod = -2;
                                break;
                            case 2:
                            case 3:
                                minApdsMod = -3;
                                break;
                            default: // 4+
                                minApdsMod = -4;
                                break;
                        }
                    }
                    apdsMod = Math.min(minApdsMod + dist, 0);
                }
            }
            // Determine AMS modifier and report
            if (amsEngaged) {
                Report r = new Report(3350);
                r.subject = entityTarget.getId();
                r.newlines = 0;
                vPhaseReport.add(r);
                amsMod = -4;
            }

            // Report APDS fire. Effect relies on internal variables and must be separated above
            if (apdsEngaged) {
                Report r = new Report(3351);
                r.subject = entityTarget.getId();
                r.add(apdsMod);
                r.newlines = 0;
                vPhaseReport.add(r);
            }
        }
        return apdsMod + amsMod;
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game, entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
                && !(target instanceof Infantry)
                && ae.getPosition().distance(target.getPosition()) <= 1;
        boolean bNemesisConfusable = isNemesisConfusable();

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
            ae.setLastTargetDisplayName(entityTarget.getDisplayName());
        }

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";
        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName() + number);
        if (entityTarget != null) {
            if (wtype.getAmmoType() != AmmoType.T_NA) {
                AmmoType atype = (AmmoType) ammo.getType();
                if (!atype.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                    r.messageId = 3116;
                    r.add(atype.getSubMunitionName());
                }
            }
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);
        // check for nemesis
        boolean shotAtNemesisTarget = false;
        if (bNemesisConfusable && !waa.isNemesisConfused()) {
            // loop through nemesis targets
            for (Enumeration<Entity> e = game.getNemesisTargets(ae, target.getPosition());
                 e.hasMoreElements(); ) {
                Entity entity = e.nextElement();
                // friendly unit with attached iNarc Nemesis pod standing in the way
                r = new Report(3125);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                weapon.setUsedThisRound(false);
                WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                        entity.getId(), waa.getWeaponId());
                newWaa.setNemesisConfused(true);
                Mounted m = ae.getEquipment(waa.getWeaponId());
                Weapon w = (Weapon) m.getType();
                AttackHandler ah = w.fire(newWaa, game, gameManager);
                // increase ammo by one, because we just incorrectly used one up
                weapon.getLinked().setShotsLeft(weapon.getLinked().getBaseShotsLeft() + 1);
                // if the new attack has an impossible to-hit, go on to next entity
                if (ah == null) {
                    continue;
                }
                WeaponHandler wh = (WeaponHandler) ah;
                // attack the new target, and if we hit it, return;
                wh.handle(phase, vPhaseReport);
                // if the new attack hit, we are finished.
                if (!wh.bMissed) {
                    return false;
                }
                shotAtNemesisTarget = true;
            }

            if (shotAtNemesisTarget) {
                // back to original target
                r = new Report(3130);
                r.subject = subjectId;
                r.newlines = 0;
                r.indent();
                vPhaseReport.addElement(r);
            }
        }

        attackValue = calcAttackValue();
        CounterAV = getCounterAV();

        // CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();

        // Only do this if a flight of large missiles wasn't destroyed
        if ((CapMissileAMSMod > 0) && (CapMissileArmor > 0)) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }

        if (amsBayEngagedCap && (CapMissileArmor > 0)) {
            // Report any AMS bay action against Large missiles that doesn't destroy them all.
            r = new Report(3358);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else if (pdBayEngagedCap && (CapMissileArmor > 0)) {
            // Report any PD bay action against Large missiles that doesn't destroy them all.
            r = new Report(3357);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

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
            r.add(toHit);
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

        // are we a glancing hit?
        setGlancingBlowFlags(entityTarget);
        addGlancingBlowReports(vPhaseReport);

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!shotAtNemesisTarget) {
            addHeat();
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);

        //This is for firing ATM/LRM/MML/MRM/SRMs at a DropShip, but is ignored for ground-to-air fire
        //It's also rare but possible for two hostile grounded DropShips to shoot at each other with individual weapons
        //with this handler. They'll use the cluster table too.
        //Don't use this if Aero Sanity is on...
        if (entityTarget != null
                && entityTarget.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && (waa.isAirToAir(game) || (waa.isAirToGround(game) && !ae.usesWeaponBays()))) {
            nDamPerHit = attackValue;
        } else {
            //This is for all other targets in atmosphere
            nDamPerHit = calcDamagePerHit();
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }

        // yeech. handle damage... different weapons do this in very different ways
        int nCluster = calcnCluster();
        int id = vPhaseReport.size();
        int hits;
        if (game.getBoard().inSpace() || waa.isAirToAir(game) || waa.isAirToGround(game)) {
            // Ensures single AMS state is properly updated
            getAMSHitsMod(new Vector<>());
            int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
            hits = aeroResults[0];
            nCluster = aeroResults[1];
            // Report AMS/Pointdefense failure due to Overheating.
            if (pdOverheated
                    && (!(amsBayEngaged
                            || amsBayEngagedCap
                            || amsBayEngagedMissile
                            || pdBayEngaged
                            || pdBayEngagedCap
                            || pdBayEngagedMissile))) {
                r = new Report (3359);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
            } else if (pdOverheated) {
                // Report a partial failure
                r = new Report (3361);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
            }

            if (!bMissed && amsEngaged && isTbolt() && !ae.isCapitalFighter()) {
                // Thunderbolts are destroyed by AMS 50% of the time whether Aero Sanity is on or not
                hits = calcHits(vPhaseReport);
            } else if (!bMissed && nweaponsHit == 1)  {
                r = new Report(3390);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            // This is for aero attacks as attack value. Does not apply if Aero Sanity is on
            if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                if (!bMissed && amsEngaged && !isTbolt() && !ae.isCapitalFighter()) {
                    // handle single AMS action against standard missiles
                    int amsRoll = Compute.d6();
                    r = new Report(3352);
                    r.subject = subjectId;
                    r.add(amsRoll);
                    vPhaseReport.add(r);
                    hits = Math.max(0, hits - amsRoll);
                }
                // Report any AMS bay action against standard missiles.
                if (amsBayEngaged && (originalAV <= 0)) {
                    //use this if AMS counterfire destroys all the missiles
                    r = new Report(3356);
                    r.indent();
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);
                } else if (amsBayEngaged) {
                    // use this if AMS counterfire destroys some of the missiles
                    CounterAV = getCounterAV();
                    r = new Report(3354);
                    r.indent();
                    r.add(CounterAV);
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);

                // Report any Point Defense bay action against standard missiles.

                } else if (pdBayEngaged && (originalAV <= 0)) {
                    // use this if PD counterfire destroys all the missiles
                    r = new Report(3355);
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);
                } else if (pdBayEngaged) {
                    // use this if PD counterfire destroys some of the missiles
                    r = new Report(3353);
                    r.add(CounterAV);
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);
                }
            }
        } else {
            // If none of the above apply
            hits = calcHits(vPhaseReport);
        }

        // We have to adjust the reports on a miss, so they line up
        if (bMissed && (id != vPhaseReport.size())) {
            vPhaseReport.get(id-1).newlines--;
            vPhaseReport.get(id).indent(2);
            vPhaseReport.get(vPhaseReport.size()-1).newlines++;
        }

        if (!bMissed) {
            // Buildings shield all units from a certain amount of damage.
            // Amount is based upon the building's CF at the phase's start.
            int bldgAbsorbs = 0;
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() == null)) {
                bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
            }

            // Attacking infantry in buildings from same building
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() != null)
                    && (entityTarget instanceof Infantry)) {
                // If elevation is the same, building doesn't absorb
                if (ae.getElevation() != entityTarget.getElevation()) {
                    int dmgClass = wtype.getInfantryDamageClass();
                    int nDamage;
                    if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                        nDamage = nDamPerHit * Math.min(nCluster, hits);
                    } else {
                        // Need to indicate to handleEntityDamage that the
                        // absorbed damage shouldn't reduce incoming damage,
                        // since the incoming damage was reduced in
                        // Compute.directBlowInfantryDamage
                        nDamage = -wtype.getDamage(nRange) * Math.min(nCluster, hits);
                    }
                    bldgAbsorbs = (int) Math.round(nDamage * bldg.getInfDmgFromInside());
                } else {
                    // Used later to indicate a special report
                    bldgAbsorbs = Integer.MIN_VALUE;
                }
            }

            // Make sure the player knows when his attack causes no damage.
            if (hits == 0) {
                r = new Report(3365);
                r.subject = subjectId;
                if (target.isAirborne() || game.getBoard().inSpace()) {
                    r.indent(2);
                }
                vPhaseReport.addElement(r);
            }

            // for each cluster of hits, do a chunk of damage
            while (hits > 0) {
                int nDamage;
                // targeting a hex for igniting
                if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                        || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                    handleIgnitionDamage(vPhaseReport, bldg, hits);
                    return false;
                }
                // targeting a hex for clearing
                if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                    nDamage = nDamPerHit * hits;
                    handleClearDamage(vPhaseReport, bldg, nDamage);
                    return false;
                }
                // Targeting a building.
                if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                    // The building takes the full brunt of the attack.
                    nDamage = nDamPerHit * hits;
                    handleBuildingDamage(vPhaseReport, bldg, nDamage, target.getPosition());
                    // And we're done!
                    return false;
                }

                if (entityTarget != null) {
                    handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
                    gameManager.creditKill(entityTarget, ae);
                    hits -= nCluster;
                    firstHit = false;
                }
            }
        } else {
            // We missed, but need to handle special miss cases
            // When shooting at a non-infantry unit in a building and the
            // shot misses, the building is damaged instead, TW pg 171
            if (bldgDamagedOnMiss) {
                r = new Report(6429);
                r.indent(2);
                r.subject = ae.getId();
                r.newlines--;
                vPhaseReport.add(r);
                int nDamage = nDamPerHit * hits;
                // We want to set bSalvo to true to prevent
                //  handleBuildingDamage from reporting a hit
                boolean savedSalvo = bSalvo;
                bSalvo = true;
                handleBuildingDamage(vPhaseReport, bldg, nDamage,
                        target.getPosition());
                bSalvo = savedSalvo;
                hits = 0;
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    protected boolean isNemesisConfusable() {
        // Are we iNarc Nemesis Confusable?
        boolean isNemesisConfusable = false;
        AmmoType atype = (AmmoType) ammo.getType();
        Mounted mLinker = weapon.getLinkedBy();
        if ((wtype.getAmmoType() == AmmoType.T_ATM)
                || ((mLinker != null)
                        && (mLinker.getType() instanceof MiscType)
                        && !mLinker.isDestroyed() && !mLinker.isMissing()
                        && !mLinker.isBreached() && (mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS) || mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS_V) || mLinker.getType().hasFlag(
                                MiscType.F_ARTEMIS_PROTO)))) {
            if ((!weapon.hasModes() || !weapon.curMode().equals("Indirect"))
                    && (((atype.getAmmoType() == AmmoType.T_ATM) &&
                            ((atype.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE))))
                            || ((((atype.getAmmoType() == AmmoType.T_LRM)
                            || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (atype.getAmmoType() == AmmoType.T_SRM)
                            || (atype.getAmmoType() == AmmoType.T_SRM_IMP)) &&
                                    (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)))
                                    || (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))))) {
                isNemesisConfusable = true;
            }
        } else if ((wtype.getAmmoType() == AmmoType.T_LRM)
                || (wtype.getAmmoType() == AmmoType.T_LRM_IMP)
                || (wtype.getAmmoType() == AmmoType.T_SRM)
                || (wtype.getAmmoType() == AmmoType.T_SRM_IMP)) {
            if ((atype.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))
                    || (atype.getMunitionType().contains(AmmoType.Munitions.M_LISTEN_KILL))) {
                isNemesisConfusable = true;
            }
        }
        return isNemesisConfusable;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    protected boolean isAdvancedAMS() {
        // Cluster hits calculation in Compute needs this to be on
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && getParentBayHandler() != null) {
            WeaponHandler bayHandler = getParentBayHandler();
            return advancedPD && (bayHandler.amsBayEngaged || bayHandler.pdBayEngaged);
        }
        return advancedAMS && (amsEngaged || apdsEngaged);
    }

    // Check for Thunderbolt. We'll use this for single AMS resolution
    @Override
    protected boolean isTbolt() {
        return wtype.hasFlag(WeaponType.F_LARGEMISSILE);
    }
}
