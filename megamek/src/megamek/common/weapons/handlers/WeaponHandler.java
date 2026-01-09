/*
 * Copyright (c) 2004-2005 - Ben Mazur (bmazur@sev.org)
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
import static megamek.common.equipment.AmmoType.INCENDIARY_MOD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.Messages;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.TeleMissileAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.compute.ComputeSideTable;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.SmokeCloud;
import megamek.server.totalWarfare.TWGameManager;

/**
 * A basic, simple attack handler. May or may not work for any particular weapon; must be overloaded to support special
 * rules.
 *
 * @author Andrew Hunter
 */
public class WeaponHandler implements AttackHandler, Serializable {
    private static final MMLogger LOGGER = MMLogger.create(WeaponHandler.class);

    @Serial
    private static final long serialVersionUID = 7137408139594693559L;
    public ToHitData toHit;
    protected HitData hit;
    public WeaponAttackAction weaponAttackAction;
    public Roll roll;
    protected boolean isJammed = false;

    protected Game game;
    protected transient TWGameManager gameManager; // must not save the server
    protected boolean bMissed;
    protected boolean bSalvo = false;
    protected boolean bGlancing = false;
    protected boolean bDirect = false;
    protected boolean bLowProfileGlancing = false;
    protected boolean nukeS2S = false;
    protected WeaponType weaponType;
    protected AmmoType ammoType;
    protected String typeName;
    protected WeaponMounted weapon;
    /**
     * Attacking Entity is the {@link Entity}  where the attack is coming from
     */
    protected Entity attackingEntity;
    /**
     * Weapon Entity is the {@link Entity} that has the {@link WeaponMounted} {@link WeaponHandler#weapon}
     */
    protected Entity weaponEntity;
    protected Targetable target;
    protected int subjectId;
    protected int nRange;
    protected int nDamPerHit;
    protected int attackValue;
    protected boolean throughFront;
    protected boolean underWater;
    protected boolean announcedEntityFiring = false;
    protected boolean missed = false;
    protected DamageType damageType;
    protected int generalDamageType = HitData.DAMAGE_NONE;
    protected Vector<Integer> insertedAttacks = new Vector<>();
    protected int numWeapons; // for capital fighters/fighter squadrons
    protected int numWeaponsHit; // for capital fighters/fighter squadrons
    protected boolean secondShot = false;
    protected int numRapidFireHits;
    protected String sSalvoType = " shot(s) ";
    protected int nSalvoBonus = 0;
    /**
     * Keeps track of whether we are processing the first hit in a series of hits (like for cluster weapons)
     */
    protected boolean firstHit = true;

    /**
     * Boolean flag that determines whether this attack is part of a strafing run.
     */
    protected boolean isStrafing = false;

    /**
     * Boolean flag that determines if this shot was the first one by a particular weapon in a strafing run. Used to
     * ensure that heat is only added once.
     */
    protected boolean isStrafingFirstShot = false;

    // Large Craft Point Defense/AMS Bay Stuff
    protected int CounterAV; // the combined attack value of all point defenses used against this weapon
    // attack
    protected int CapMissileArmor; // the standard scale armor points of a capital missile bay
    protected int CapMissileAMSMod; // the to-hit mod inflicted against a capital missile attack if it isn't
    // completely destroyed
    protected boolean CapMissileMissed = false; // true if the AMS Mod causes a capital missile attack to miss. Used for
    // reporting.
    protected boolean amsBayEngaged = false; // true if one or more AMS bays engages this attack. Used for reporting if
    // this is a standard missile (LRM, MRM, etc) attack.
    protected boolean pdBayEngaged = false; // true if one or more point defense bays engages this attack. Used for
    // reporting if this is a standard missile (LRM, MRM, etc) attack.
    protected boolean pdOverheated = false; // true if counterfire + offensive weapon attacks made this round cause the
    // defending unit to overheat. Used for reporting.
    protected boolean amsBayEngagedCap = false; // true if one or more AMS bays engages this attack. Used for reporting
    // if this is a capital missile attack.
    protected boolean pdBayEngagedCap = false; // true if one or more point defense bays engages this attack. Used for
    // reporting if this is a capital missile attack.
    protected boolean amsBayEngagedMissile = false; // true if one or more AMS bays engages this attack. Used for
    // reporting if this is a single large missile (thunderbolt, etc.)
    // attack.
    protected boolean pdBayEngagedMissile = false; // true if one or more point defense bays engages this attack. Used
    // for reporting if this is a single large missile (thunderbolt, etc.)
    // attack.
    protected boolean advancedPD = false; // true if advanced StratOps game rule is on
    protected WeaponHandler parentBayHandler = null; // Used for weapons bays when Aero Sanity is on
    protected int originalAV = 0; // Used to handle AMS damage to standard missile flights fired by capital
    // fighters

    protected boolean amsEngaged = false;
    protected boolean apdsEngaged = false;

    public int getSalvoBonus() {
        return nSalvoBonus;
    }

    /**
     * Returns the heat generated by a large craft's weapons fire declarations during the round Used to determine
     * whether point defenses can engage.
     *
     * @param entity the entity you wish to get heat data from
     *
     * @see TeleMissileAttackAction which contains a modified version of this to work against a TeleMissile entity in
     *       the physical phase
     */
    protected int getLargeCraftHeat(Entity entity) {
        int totalHeat = 0;

        if (!entity.isLargeCraft()) {
            return totalHeat;
        }

        for (Enumeration<AttackHandler> attack = game.getAttacks(); attack.hasMoreElements(); ) {
            AttackHandler attackHandler = attack.nextElement();
            WeaponAttackAction prevAttack = attackHandler.getWeaponAttackAction();
            if (prevAttack.getEntityId() == entity.getId()) {
                WeaponMounted prevWeapon = (WeaponMounted) entity.getEquipment(prevAttack.getWeaponId());
                if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                    totalHeat += prevWeapon.getHeatByBay();
                } else {
                    boolean rearMount = prevWeapon.isRearMounted();
                    int loc = prevWeapon.getLocation();

                    // create an array of booleans of locations
                    boolean[] usedFrontArc = new boolean[weaponEntity.locations()];
                    boolean[] usedRearArc = new boolean[weaponEntity.locations()];
                    for (int i = 0; i < weaponEntity.locations(); i++) {
                        usedFrontArc[i] = false;
                        usedRearArc[i] = false;
                    }
                    if (!rearMount) {
                        if (!usedFrontArc[loc]) {
                            totalHeat += weaponEntity.getHeatInArc(loc, rearMount);
                            usedFrontArc[loc] = true;
                        }
                    } else {
                        if (!usedRearArc[loc]) {
                            totalHeat += weaponEntity.getHeatInArc(loc, rearMount);
                            usedRearArc[loc] = true;
                        }
                    }
                }
            }
        }
        return totalHeat;
    }

    /**
     * Checks to see if the basic conditions needed for point defenses to work are in place Artillery weapons need to
     * change this slightly See also TeleMissileAttackAction, which contains a modified version of this to work against
     * a TeleMissile entity in the physical phase
     */
    protected boolean checkPDConditions() {
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
        if ((target == null)
              || (target.getTargetType() != Targetable.TYPE_ENTITY)
              || !advancedPD
              // Don't defend against ground fire with bay fire unless attacked by capital
              // missile fire
              // Prevents ammo and heat being used twice for dropships defending here and with
              // getAMSHitsMod()
              || (weaponAttackAction.isGroundToAir(game) && (!(weaponType.isSubCapital() || weaponType.isCapital())))) {
            return false;
        }
        // Prevents a grounded dropship using individual weapons from engaging with
        // AMSBays unless attacked by a dropship or capital fighter
        // You can get some blank missile weapons fire reports due to the attack value /
        // n damage per hit conversion if this isn't done
        return !(target instanceof Dropship)
              || !weaponAttackAction.isAirToGround(game)
              || attackingEntity.usesWeaponBays();
    }

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile This should return true. Only when
     * handling capital missile attacks can this be false. See also TeleMissileAttackAction, which contains a modified
     * version of this to work against a TeleMissile entity in the physical phase
     */
    protected boolean canEngageCapitalMissile(WeaponMounted counter) {
        return true;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is See also
     * TeleMissileAttackAction, which contains a modified version of this to work against a TeleMissile entity in the
     * physical phase
     */
    protected void setAMSBayReportingFlag() {
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is See also
     * TeleMissileAttackAction, which contains a modified version of this to work against a TeleMissile entity in the
     * physical phase
     */
    protected void setPDBayReportingFlag() {
    }

    /**
     * Sets whether this weapon is considered a single, large missile for AMS resolution
     */
    protected boolean isThunderBolt() {
        return false;
    }

    /**
     * Calculates the attack value of point defense weapons used against a missile bay attack This is the main large
     * craft point defense method See also TeleMissileAttackAction, which contains a modified version of this to work
     * against a TeleMissile entity in the physical phase
     */
    protected int calcCounterAV() {
        if (!checkPDConditions()) {
            return 0;
        }
        int counterAV = 0;
        int amsAV;
        double pdAV;
        Entity entityTarget = (Entity) target;
        // any AMS bay attacks by the target?
        List<WeaponMounted> lCounters = weaponAttackAction.getCounterEquipment();
        // We need to know how much heat has been assigned to offensive weapons fire by
        // the defender this round
        int weaponHeat = getLargeCraftHeat(entityTarget) + entityTarget.heatBuildup;
        if (null != lCounters) {
            for (WeaponMounted counter : lCounters) {
                // Point defenses only fire vs attacks against the arc they protect
                Entity pdEnt = counter.getEntity();
                boolean isInArc;
                // If the defending unit is the target, use attacker for arc
                if (entityTarget.equals(pdEnt)) {
                    isInArc = ComputeArc.isInArc(game, pdEnt.getId(), pdEnt.getEquipmentNum(counter), attackingEntity);
                } else { // Otherwise, the attack must pass through an escort unit's hex
                    // TODO: We'll get here, eventually
                    isInArc = ComputeArc.isInArc(game, pdEnt.getId(),
                          pdEnt.getEquipmentNum(counter),
                          entityTarget);
                }

                if (!isInArc) {
                    continue;
                }
                // Point defenses can't fire if they're not ready for any other reason
                if (!counter.isReady() || counter.isMissing()
                      // shutdown means no Point defenses
                      || pdEnt.isShutDown()) {
                    continue;
                }
                // Point defense/AMS bays with less than 2 weapons cannot engage capital
                // missiles
                if (!canEngageCapitalMissile(counter)) {
                    continue;
                }

                // Set up differences between point defense and AMS bays
                boolean isAMSBay = counter.getType().hasFlag(WeaponType.F_AMS_BAY);
                boolean isPDBay = counter.getType().hasFlag(WeaponType.F_PD_BAY);

                // Point defense bays can only fire at one attack per round
                if (isPDBay) {
                    if (counter.isUsedThisRound()) {
                        continue;
                    }
                }

                // Now for heat, damage and ammo we need the individual weapons in the bay
                // First, reset the temporary damage counters
                amsAV = 0;
                pdAV = 0;
                for (WeaponMounted bayW : counter.getBayWeapons()) {
                    AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
                    WeaponType bayWType = bayW.getType();

                    // build up some heat
                    // First Check to see if we have enough heat capacity to fire
                    if ((weaponHeat + bayW.getCurrentHeat()) > pdEnt.getHeatCapacity()) {
                        pdOverheated = true;
                        break;
                    }
                    if (counter.getType().hasFlag(WeaponType.F_HEAT_AS_DICE)) {
                        int heatDice = Compute.d6(bayW
                              .getCurrentHeat());
                        pdEnt.heatBuildup += heatDice;
                        weaponHeat += heatDice;
                    } else {
                        pdEnt.heatBuildup += bayW.getCurrentHeat();
                        weaponHeat += bayW.getCurrentHeat();
                    }

                    // Bays use lots of ammo. Check to make sure we haven't run out
                    if (bayWAmmo != null) {
                        if (bayWAmmo.getBaseShotsLeft() == 0) {
                            continue;
                        }
                        // decrement the ammo
                        bayWAmmo.setShotsLeft(Math.max(0,
                              bayWAmmo.getBaseShotsLeft() - 1));
                    }

                    if (isAMSBay) {
                        // get the attack value
                        amsAV += (int) Math.round(bayWType.getShortAV());
                        // set the ams as having fired, if it did
                        setAMSBayReportingFlag();
                    }

                    if (isPDBay) {
                        // get the attack value
                        pdAV += bayWType.getShortAV();
                        // set the pod bay as having fired, if it was able to
                        counter.setUsedThisRound(true);
                        setPDBayReportingFlag();
                    }
                }
                // non-AMS only add half their damage, rounded up
                counterAV += (int) Math.ceil(pdAV / 2.0);
                // AMS add their full damage
                counterAV += amsAV;
            }
        }

        CounterAV = counterAV;
        return counterAV;
    }

    /**
     * Return the attack value of point defense weapons used against a missile bay attack
     */
    public int getCounterAV() {
        return CounterAV;
    }

    /**
     * Used with Aero Sanity mod Returns the handler for the BayWeapon this individual weapon belongs to
     */
    protected WeaponHandler getParentBayHandler() {
        return parentBayHandler;
    }

    /**
     * Sets the parent handler for each sub-weapon handler called when looping through bay weapons Used with Aero Sanity
     * to pass counterAV through to the individual missile handler from the bay handler
     *
     * @param bh - The <code>AttackHandler</code> for the BayWeapon this individual weapon belongs to
     */
    public void setParentBayHandler(WeaponHandler bh) {
        parentBayHandler = bh;
    }

    /**
     * Calculates the to-hit penalty inflicted on a capital missile attack by point defense fire this should return 0
     * unless this is a capital missile attack (otherwise, reporting and to-hit get screwed up)
     */
    protected int calcCapMissileAMSMod() {
        return 0;
    }

    /**
     * Return the to-hit penalty inflicted on a capital missile attack by point defense fire
     */
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }

    // End of Large Craft Point Defense Methods and Variables

    /**
     * Used to store reports from calls to <code>calcDamagePerHit</code>. This is necessary because the method is called
     * before the report needs to be added.
     */
    protected Vector<Report> calcDmgPerHitReport = new Vector<>();

    /**
     * return the <code>int</code> ID of the attacking <code>Entity</code>
     */
    @Override
    public int getAttackerId() {
        return attackingEntity.getId();
    }

    @Override
    public Entity getAttacker() {
        return attackingEntity;
    }

    /**
     * Do we care about the specified phase?
     */
    @Override
    public boolean cares(GamePhase phase) {
        return phase.isFiring();
    }

    /**
     * @param vPhaseReport - A <code>Vector</code> containing the phase report.
     *
     * @return a <code>boolean</code> value indicating whether the attack misses because of a failed check.
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        return false;
    }

    /**
     * Carries out a check to see if the weapon in question explodes due to the 'ammo feed problem' quirk Not the case
     * for weapons without ammo
     */
    protected boolean doAmmoFeedProblemCheck(Vector<Report> vPhaseReport) {
        return false;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        gameManager = (TWGameManager) Server.getServerInstance().getGameManager();
    }

    protected TargetRoll getFireTNRoll() {
        int targetNumber = (ammoType == null) ? weaponType.getFireTN()
              : Math.min(weaponType.getFireTN(), ammoType.getFireTN());
        return new TargetRoll(targetNumber, weaponType.getName());
    }

    /**
     * @return a <code>boolean</code> value indicating whether this attack needs further calculating, like a missed shot
     *       hitting a building, or an AMS only shooting down some missiles.
     */
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
          IBuilding bldg, Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if ((entityTarget != null)
              && !entityTarget.isAirborne()
              && !entityTarget.isAirborneVTOLorWIGE()
              && ((bldg == null) && (weaponType.getFireTN() != TargetRoll.IMPOSSIBLE
              && (ammoType == null || ammoType.getFireTN() != TargetRoll.IMPOSSIBLE)))) {
            gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
                  getFireTNRoll(), 3,
                  vPhaseReport);
        }

        // shots that miss an entity can also potential cause explosions in a
        // heavy industrial hex
        gameManager.checkExplodeIndustrialZone(target.getPosition(), target.getBoardId(), vPhaseReport);

        // TW, pg. 171 - shots that miss a target in a building don't damage the
        // building, unless the attacker is adjacent
        return bldgDamagedOnMiss
              && (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL);
    }

    /**
     * Calculate the number of hits
     *
     * @param vPhaseReport - the <code>Vector</code> containing the phase report.
     *
     * @return an <code>int</code> containing the number of hits.
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // normal BA attacks (non-swarm, non single-trooper weapons)
        // do more than 1 hit
        if ((attackingEntity instanceof BattleArmor)
              && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
              && !(weapon.isSquadSupportWeapon())
              && !(attackingEntity.getSwarmTargetId() == target.getId())) {
            bSalvo = true;
            int toReturn = allShotsHit() ? ((BattleArmor) attackingEntity)
                  .getShootingStrength()
                  : Compute
                  .missilesHit(((BattleArmor) attackingEntity).getShootingStrength());
            Report r = new Report(3325);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toReturn);
            r.add(" troopers ");
            r.add(toHit.getTableDesc());
            vPhaseReport.add(r);
            return toReturn;
        }
        return 1;
    }

    /**
     * Calculate the clustering of the hits
     *
     * @return a <code>int</code> value saying how much hits are in each cluster of damage.
     */
    protected int calculateNumCluster() {
        return 1;
    }

    protected int calculateNumClusterAero(Entity entityTarget) {
        if (usesClusterTable() && !attackingEntity.isCapitalFighter()
              && (entityTarget != null) && !entityTarget.isCapitalScale()) {
            return 5;
        } else {
            return 1;
        }
    }

    protected int[] calcAeroDamage(Entity entityTarget,
          Vector<Report> vPhaseReport) {
        // Now I need to adjust this for attacks on aerospace because they use
        // attack values and different rules
        // this will work differently for cluster and non-cluster
        // weapons, and differently for capital fighter/fighter
        // squadrons
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            // everything will use the normal hits and clusters for hits weapon
            // unless
            // we have a squadron or capital scale entity
            int reportSize = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);
            int nCluster = calculateNumCluster();
            int AMSHits;
            if (attackingEntity.isCapitalFighter()) {
                Vector<Report> throwAwayReport = new Vector<>();
                // for capital scale fighters, each non-cluster weapon hits a
                // different location
                bSalvo = true;
                hits = 1;
                if (numWeapons > 1) {
                    if (allShotsHit()) {
                        numWeaponsHit = numWeapons;
                    } else {
                        numWeaponsHit = Compute.missilesHit(numWeapons,
                              ((Aero) attackingEntity).getClusterMods());
                    }
                    if (usesClusterTable()) {
                        // remove the last reports because they showed the
                        // number of shots that hit
                        while (vPhaseReport.size() > reportSize) {
                            vPhaseReport.remove(vPhaseReport.size() - 1);
                        }
                        hits = 0;
                        for (int i = 0; i < numWeaponsHit; i++) {
                            hits += calcHits(throwAwayReport);
                        }
                        // Report and apply point defense fire
                        if (pdBayEngaged || amsBayEngaged) {
                            Report r = new Report(3367);
                            r.indent();
                            r.subject = subjectId;
                            r.add(getCounterAV());
                            r.newlines = 0;
                            vPhaseReport.addElement(r);
                            hits -= (CounterAV / nDamPerHit);
                        } else if (amsEngaged) {
                            Report r = new Report(3350);
                            r.subject = entityTarget.getId();
                            r.newlines = 0;
                            vPhaseReport.add(r);
                        }
                        Report r = new Report(3325);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(sSalvoType);
                        r.add(toHit.getTableDesc());
                        r.newlines = 0;
                        vPhaseReport.add(r);
                    } else {
                        // If point defenses engage Large, single missiles
                        if (pdBayEngagedMissile || amsBayEngagedMissile) {
                            // remove the last reports because they showed the
                            // number of shots that hit
                            while (vPhaseReport.size() > reportSize) {
                                vPhaseReport.remove(vPhaseReport.size() - 1);
                            }
                            AMSHits = 0;
                            Report r = new Report(3236);
                            r.subject = subjectId;
                            r.add(numWeaponsHit);
                            vPhaseReport.add(r);
                            r = new Report(3230);
                            r.indent(1);
                            r.subject = subjectId;
                            vPhaseReport.add(r);
                            for (int i = 0; i < numWeaponsHit; i++) {
                                Roll diceRoll = Compute.rollD6(1);

                                if (diceRoll.getIntValue() <= 3) {
                                    r = new Report(3240);
                                    r.subject = subjectId;
                                    r.add("missile");
                                    r.add(diceRoll);
                                    vPhaseReport.add(r);
                                    AMSHits += 1;
                                } else {
                                    r = new Report(3241);
                                    r.add("missile");
                                    r.add(diceRoll);
                                    r.subject = subjectId;
                                    vPhaseReport.add(r);
                                }
                            }
                            numWeaponsHit = numWeaponsHit - AMSHits;
                        } else if (amsEngaged || apdsEngaged) {
                            // remove the last reports because they showed the
                            // number of shots that hit
                            while (vPhaseReport.size() > reportSize) {
                                vPhaseReport.remove(vPhaseReport.size() - 1);
                            }
                            // If you're shooting at a target using single AMS
                            // Too many variables here as far as AMS numbers
                            // Just allow 1 missile to be shot down
                            AMSHits = 0;
                            Report r = new Report(3236);
                            r.subject = subjectId;
                            r.add(numWeaponsHit);
                            vPhaseReport.add(r);
                            if (amsEngaged) {
                                r = new Report(3230);
                                r.indent(1);
                                r.subject = subjectId;
                                vPhaseReport.add(r);
                            }
                            if (apdsEngaged) {
                                r = new Report(3231);
                                r.indent(1);
                                r.subject = subjectId;
                                vPhaseReport.add(r);
                            }
                            Roll diceRoll = Compute.rollD6(1);

                            if (diceRoll.getIntValue() <= 3) {
                                r = new Report(3240);
                                r.subject = subjectId;
                                r.add("missile");
                                r.add(diceRoll);
                                vPhaseReport.add(r);
                                AMSHits = 1;
                            } else {
                                r = new Report(3241);
                                r.add("missile");
                                r.add(diceRoll);
                                r.subject = subjectId;
                                vPhaseReport.add(r);
                            }
                            numWeaponsHit = numWeaponsHit - AMSHits;
                        }
                        nCluster = 1;
                        if (!bMissed) {
                            Report r = new Report(3325);
                            r.subject = subjectId;
                            r.add(numWeaponsHit);
                            r.add(" weapon(s) ");
                            r.add(" ");
                            r.newlines = 0;
                            hits = numWeaponsHit;
                            vPhaseReport.add(r);
                        }
                    }
                }
            }
            int[] results = new int[2];
            results[0] = hits;
            results[1] = nCluster;
            return results;
        } else {
            int hits = 1;
            int nCluster = calculateNumClusterAero(entityTarget);
            if (attackingEntity.isCapitalFighter()) {
                bSalvo = false;
                if (numWeapons > 1) {
                    numWeaponsHit = Compute.missilesHit(numWeapons,
                          ((IAero) attackingEntity).getClusterMods());
                    if (pdBayEngaged || amsBayEngaged) {
                        // Point Defenses engage standard (cluster) missiles
                        int counterAV;
                        counterAV = getCounterAV();
                        nDamPerHit = originalAV * numWeaponsHit - counterAV;
                    } else {
                        // If multiple large missile or non-missile weapons hit
                        Report report = new Report(3325);
                        report.subject = subjectId;
                        report.add(numWeaponsHit);
                        report.add(" weapon(s) ");
                        report.add(" ");
                        report.newlines = 1;
                        vPhaseReport.add(report);
                        nDamPerHit = attackValue * numWeaponsHit;
                    }
                    nCluster = 1;
                }
            } else if (nCluster > 1) {
                bSalvo = true;
                nDamPerHit = 1;
                hits = attackValue;
            } else {
                // If we're not a capital fighter / squadron
                // Point Defenses engage any Large, single missiles
                if (pdBayEngagedMissile || amsBayEngagedMissile) {
                    bSalvo = false;
                    Report report = new Report(3235);
                    report.subject = subjectId;
                    vPhaseReport.add(report);
                    report = new Report(3230);
                    report.indent(1);
                    report.subject = subjectId;
                    vPhaseReport.add(report);
                    for (int i = 0; i < numWeaponsHit; i++) {
                        Roll diceRoll = Compute.rollD6(1);

                        if (diceRoll.getIntValue() <= 3) {
                            report = new Report(3240);
                            report.subject = subjectId;
                            report.add("missile");
                            report.add(diceRoll);
                            vPhaseReport.add(report);
                            hits = 0;
                        } else {
                            report = new Report(3241);
                            report.add("missile");
                            report.add(diceRoll);
                            report.subject = subjectId;
                            vPhaseReport.add(report);
                            hits = 1;
                        }
                    }
                } else {
                    bSalvo = false;
                    nDamPerHit = attackValue;
                    nCluster = 1;
                }
            }
            int[] results = new int[2];
            results[0] = hits;
            results[1] = nCluster;
            return results;
        }
    }

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> returnedReports) {
        if (!cares(phase)) {
            return true;
        }
        Vector<Report> vPhaseReport = new Vector<>();

        boolean heatAdded = false;
        int numAttacks = 1;
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS)
              && ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) || (weaponType
              .getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA_THB))
              && !weapon.curMode().equals("Single")) {
            numAttacks = 2;
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
              entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
              && !(target instanceof Infantry)
              && attackingEntity.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        IBuilding bldg = game.getBuildingAt(target.getBoardLocation()).orElse(null);
        String number = numWeapons > 1 ? " (" + numWeapons + ")" : "";
        for (int i = numAttacks; i > 0; i--) {
            // Skip weapon announcement for spawned attacks (e.g., rapid-fire AC special ammo)
            // The parent handler already announced the weapon
            if (parentBayHandler == null) {
                // Report weapon attack and its to-hit value.
                Report weaponReport = new Report(3115);
                weaponReport.indent();
                weaponReport.newlines = 0;
                weaponReport.subject = subjectId;
                String base = weaponType.isClan() ? " (Clan)" : "";
                weaponReport.add(weaponType.getName() + base + number);
                if (entityTarget != null) {
                    if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                          && (weapon.getLinked() != null)
                          && (weapon.getLinked().getType() instanceof AmmoType)) {
                        if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)
                              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML
                              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX
                              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ATM) {
                            weaponReport.messageId = 3116;
                            weaponReport.add(ammoType.getSubMunitionName());
                        }
                    }
                    weaponReport.addDesc(entityTarget);
                } else {
                    weaponReport.messageId = 3120;
                    weaponReport.add(target.getDisplayName(), true);
                }
                vPhaseReport.addElement(weaponReport);
            }
            Report report;

            // Point Defense fire vs Capital Missiles

            // are we a glancing hit? Check for this here, report it later
            setGlancingBlowFlags(entityTarget);

            // Set Margin of Success/Failure and check for Direct Blows
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            // This has to be up here so that we don't screw up glancing/direct blow reports
            attackValue = calcAttackValue();

            // CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            // Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll.getIntValue() < toHit.getValue()) {
                    CapMissileMissed = true;
                }
            }

            // Report any AMS bay action against Capital missiles that doesn't destroy them
            // all.
            if (amsBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3358);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);

                // Report any PD bay action against Capital missiles that doesn't destroy them
                // all.
            } else if (pdBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3357);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // Report AMS/Point defense failure due to Overheating.
            if (pdOverheated
                  && (!(amsBayEngaged
                  || amsBayEngagedCap
                  || amsBayEngagedMissile
                  || pdBayEngaged
                  || pdBayEngagedCap
                  || pdBayEngagedMissile))) {
                report = new Report(3359);
                report.subject = subjectId;
                report.indent();
                vPhaseReport.addElement(report);
            } else if (pdOverheated) {
                // Report a partial failure
                report = new Report(3361);
                report.subject = subjectId;
                report.indent();
                vPhaseReport.addElement(report);
            }

            // Skip to-hit reporting for spawned attacks - parent already reported the attack
            if (parentBayHandler == null) {
                if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                    report = new Report(3135);
                    report.subject = subjectId;
                    report.add(toHit.getDesc());
                    vPhaseReport.addElement(report);
                    returnedReports.addAll(vPhaseReport);
                    return false;
                } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                    report = new Report(3140);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(toHit.getDesc());
                    vPhaseReport.addElement(report);
                } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                    report = new Report(3145);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(toHit.getDesc());
                    vPhaseReport.addElement(report);
                } else {
                    // roll to hit
                    report = new Report(3150);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(toHit);
                    vPhaseReport.addElement(report);
                }

                // dice have been rolled, thanks
                report = new Report(3155);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(roll);
                vPhaseReport.addElement(report);
            }

            // do we hit?
            bMissed = roll.getIntValue() < toHit.getValue();

            // Report Glancing/Direct Blow here because of Capital Missile weirdness
            // TODO: Can't figure out a good way to make Capital Missile bays report
            // direct/glancing blows
            // when Advanced Point Defense is on, but they work correctly.
            if (!(amsBayEngagedCap || pdBayEngagedCap)) {
                addGlancingBlowReports(vPhaseReport);

                if (bDirect) {
                    report = new Report(3189);
                    report.subject = attackingEntity.getId();
                    report.newlines = 0;
                    vPhaseReport.addElement(report);
                }
            }

            // Do this stuff first, because some weapon's miss report reference
            // the amount of shots fired and stuff.
            nDamPerHit = calcDamagePerHit();
            if (!heatAdded) {
                addHeat();
                heatAdded = true;
            }

            // Report any AMS bay action against standard missiles.
            CounterAV = getCounterAV();
            // use this if counterfire destroys all the missiles
            if (amsBayEngaged && (attackValue <= 0)) {
                report = new Report(3356);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            } else if (amsBayEngaged) {
                report = new Report(3354);
                report.indent();
                report.add(CounterAV);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // use this if AMS counterfire destroys all the Capital missiles
            if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3356);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // Report any Point Defense bay action against standard missiles.
            if (pdBayEngaged && (attackValue <= 0)) {
                report = new Report(3355);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            } else if (pdBayEngaged) {
                report = new Report(3353);
                report.add(CounterAV);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3355);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }

            // Do we need some sort of special resolution (minefields,
            // artillery,
            if (specialResolution(vPhaseReport, entityTarget) && (i < 2)) {
                returnedReports.addAll(vPhaseReport);
                return false;
            }

            if (bMissed && !missReported) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS)
                      && ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) || (weaponType
                      .getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA_THB))
                      && (i == 2)) {
                    reportMiss(vPhaseReport, true);
                } else {
                    reportMiss(vPhaseReport);
                }

                // Works out fire setting, AMS shots, and whether continuation
                // is necessary.
                if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                      vPhaseReport) && (i < 2)) {
                    returnedReports.addAll(vPhaseReport);
                    return false;
                }
            }

            //  handle damage. . different weapons do this in very
            // different
            // ways
            int nCluster = calculateNumCluster();
            int id = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);
            if ((target.isAirborne() && !weaponAttackAction.isGroundToAir(game))
                  || game.isOnSpaceMap(target)
                  || attackingEntity.usesWeaponBays()) {
                // if we added a line to the phase report for calc hits, remove
                // it now
                while (vPhaseReport.size() > id) {
                    vPhaseReport.removeElementAt(vPhaseReport.size() - 1);
                }
                int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
                hits = aeroResults[0];
                // If our capital missile was destroyed, it shouldn't hit
                if ((amsBayEngagedCap || pdBayEngagedCap) && (CapMissileArmor <= 0)) {
                    hits = 0;
                }
                nCluster = aeroResults[1];
            }

            // We have to adjust the reports on a miss, so they line up
            if (bMissed && id != vPhaseReport.size()) {
                vPhaseReport.get(id - 1).newlines--;
                vPhaseReport.get(id).indent(2);
                vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
            }

            if (!bMissed) {
                // Buildings shield all units from a certain amount of damage.
                // Amount is based upon the building's CF at the phase's start.
                int bldgAbsorbs = 0;
                if (targetInBuilding && (bldg != null)
                      && (toHit.getThruBldg() == null)) {
                    bldgAbsorbs = bldg.getAbsorption(target.getPosition());
                }

                // Attacking infantry in buildings from same building
                if (targetInBuilding && (bldg != null)
                      && (toHit.getThruBldg() != null)
                      && (entityTarget instanceof Infantry)) {
                    // If elevation is the same, building doesn't absorb
                    if (attackingEntity.getElevation() != entityTarget.getElevation()) {
                        int dmgClass = weaponType.getInfantryDamageClass();
                        int nDamage;
                        if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                            nDamage = nDamPerHit * Math.min(nCluster, hits);
                        } else {
                            // Need to indicate to handleEntityDamage that the
                            // absorbed damage shouldn't reduce incoming damage,
                            // since the incoming damage was reduced in
                            // Compute.directBlowInfantryDamage
                            nDamage = -weaponType.getDamage(nRange)
                                  * Math.min(nCluster, hits);
                        }
                        bldgAbsorbs = (int) Math.round(nDamage
                              * bldg.getInfDmgFromInside());
                    } else {
                        // Used later to indicate a special report
                        bldgAbsorbs = Integer.MIN_VALUE;
                    }
                }

                // Make sure the player knows when his attack causes no damage.
                if (hits == 0) {
                    report = new Report(3365);
                    report.subject = subjectId;
                    vPhaseReport.addElement(report);
                }

                // for each cluster of hits, do a chunk of damage
                while (hits > 0) {
                    int nDamage;
                    if ((target.getTargetType() == Targetable.TYPE_HEX_TAG)
                          || (target.getTargetType() == Targetable.TYPE_BLDG_TAG)) {
                        TagInfo info = new TagInfo(attackingEntity.getId(), target.getTargetType(), target, false);
                        game.addTagInfo(info);

                        attackingEntity.setSpotting(true);
                        attackingEntity.setSpotTargetId(target.getId());

                        report = new Report(3390);
                        report.subject = subjectId;
                        vPhaseReport.addElement(report);
                        hits = 0;
                        // targeting a hex for igniting
                    } else if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                          || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                        handleIgnitionDamage(vPhaseReport, bldg, hits);
                        hits = 0;
                        // targeting a hex for clearing
                    } else if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                        nDamage = nDamPerHit * hits;
                        handleClearDamage(vPhaseReport, bldg, nDamage);
                        hits = 0;
                        // Targeting a building.
                    } else if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                        // The building takes the full brunt of the attack.
                        nDamage = nDamPerHit * hits;
                        handleBuildingDamage(vPhaseReport, bldg, nDamage, target.getPosition());
                        hits = 0;
                    } else if (entityTarget != null) {
                        handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
                        gameManager.creditKill(entityTarget, attackingEntity);
                        hits -= nCluster;
                        firstHit = false;
                    } else {
                        // we shouldn't be here, but if we get here, let's set hits to 0
                        // to avoid infinite loops
                        hits = 0;
                        LOGGER.error("Unexpected target type: {}", target.getTargetType());
                    }
                } // Handle the next cluster.
            } else { // We missed, but need to handle special miss cases
                // When shooting at a non-infantry unit in a building and the
                // shot misses, the building is damaged instead, TW pg 171
                if (bldgDamagedOnMiss) {
                    report = new Report(6429);
                    report.indent(2);
                    report.subject = attackingEntity.getId();
                    report.newlines--;
                    vPhaseReport.add(report);
                    int nDamage = nDamPerHit * hits;
                    // We want to set bSalvo to true to prevent
                    // handleBuildingDamage from reporting a hit
                    boolean savedSalvo = bSalvo;
                    bSalvo = true;
                    handleBuildingDamage(vPhaseReport, bldg, nDamage,
                          target.getPosition());
                    bSalvo = savedSalvo;
                }
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS)
                  && ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) || (weaponType
                  .getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA_THB))
                  && (i == 2)) {
                // Jammed weapon doesn't get 2nd shot...
                if (isJammed) {
                    report = new Report(9905);
                    report.indent();
                    report.subject = attackingEntity.getId();
                    vPhaseReport.addElement(report);
                    i--;
                } else { // If not jammed, it gets the second shot...
                    report = new Report(9900);
                    report.indent();
                    report.subject = attackingEntity.getId();
                    vPhaseReport.addElement(report);
                    if (null != attackingEntity.getCrew()) {
                        roll = attackingEntity.getCrew().rollGunnerySkill();
                    } else {
                        roll = Compute.rollD6(2);
                    }
                }
            }
        }
        Report.addNewline(vPhaseReport);

        insertAttacks(phase, vPhaseReport);

        returnedReports.addAll(vPhaseReport);
        return false;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage(nRange);

        // Check for BA vs BA weapon effectiveness, if option is on
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BA_VS_BA)
              && (target instanceof BattleArmor)) {
            // We don't check to make sure the attacker is BA, as most weapons
            // will return their normal damage.
            toReturn = Compute.directBlowBADamage(toReturn,
                  weaponType.getBADamageClass(), (BattleArmor) target);
        }

        // we default to direct fire weapons for anti-infantry damage
        if (target.isConventionalInfantry()) {
            // Flechette ammo is treated "as though attack were from infantry unit" (TW p.208)
            // so it should NOT get the non-infantry vs mechanized damage bonus
            boolean isNonInfantryVsMechanized = ((Infantry) target).isMechanized()
                  && !attackingEntity.isConventionalInfantry()
                  && damageType != DamageType.FLECHETTE;
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  isNonInfantryVsMechanized,
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }
        return (int) toReturn;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int av = 0;
        // if we have a ground firing unit, then AV should not be determined by
        // aero range brackets
        if (!attackingEntity.isAirborne() || game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS)) {
            if (usesClusterTable()) {
                // for cluster weapons just use the short range AV
                av = weaponType.getRoundShortAV();
            } else {
                // otherwise just use the full weapon damage by range
                av = weaponType.getDamage(nRange);
            }
        } else {
            // we have an airborne attacker, so we need to use aero range
            // brackets
            int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(),
                  true, false);
            if (range == WeaponType.RANGE_SHORT) {
                av = weaponType.getRoundShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = weaponType.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = weaponType.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = weaponType.getRoundExtAV();
            }
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);

        av = (int) Math.floor(getBracketingMultiplier() * av);

        return av;
    }

    /**
     * * adjustment factor on attack value for fighter squadrons
     */
    protected double getBracketingMultiplier() {
        double multiplier = 1.0;
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            multiplier = 0.8;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            multiplier = 0.6;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            multiplier = 0.4;
        }
        return multiplier;
    }

    /*
     * Return the capital missile target for criticalSlots. Zero if not a capital
     * missile
     */
    protected int getCapMisMod() {
        return 0;
    }

    /**
     * Handles potential damage to partial cover that absorbs a shot. The
     * <code>ToHitData</code> is checked to what if there is any damagable cover
     * to be hit, and if so which cover gets hit (there are two possibilities in some cases, such as 75% partial cover).
     * The method then takes care of assigning damage to the cover. Buildings are damaged directly, while dropships call
     * the <code>handleEntityDamage</code> method.
     *
     * @param entityTarget The target Entity
     */
    protected void handlePartialCoverHit(Entity entityTarget, Vector<Report> vPhaseReport, HitData pcHit,
          IBuilding bldg,
          int hits, int nCluster, int bldgAbsorbs) {

        // Report the hit and table description, if this isn't part of a salvo
        Report r;
        if (!bSalvo) {
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(pcHit));
            vPhaseReport.addElement(r);
            if (weapon.isRapidFire()) {
                r.newlines = 0;
                r = new Report(3225);
                r.subject = subjectId;
                r.add(numRapidFireHits * 3);
                vPhaseReport.add(r);
            }
        } else {
            // Keep spacing consistent
            Report.addNewline(vPhaseReport);
        }

        r = new Report(3460);
        r.subject = subjectId;
        r.add(entityTarget.getShortName());
        r.add(entityTarget.getLocationAbbr(pcHit));
        r.indent(2);
        vPhaseReport.addElement(r);

        int damageableCoverType;
        IBuilding coverBuilding;
        Entity coverDropShip;
        Coords coverLoc;

        // Determine if there is primary and secondary cover,
        // and then determine which one gets hit
        if ((toHit.getCover() == LosEffects.COVER_75RIGHT || toHit.getCover() == LosEffects.COVER_75LEFT)
              ||
              // 75% cover has a primary and secondary
              (toHit.getCover() == LosEffects.COVER_HORIZONTAL && toHit
                    .getDamagableCoverTypeSecondary() != LosEffects.DAMAGABLE_COVER_NONE)) {
            // Horizontal cover provided by two 25%'s, so primary and secondary
            int hitLoc = pcHit.getLocation();
            // Primary stores the left side, from the perspective of the
            // attacker
            if (hitLoc == Mek.LOC_RIGHT_LEG || hitLoc == Mek.LOC_RIGHT_TORSO
                  || hitLoc == Mek.LOC_RIGHT_ARM) {
                // Left side is primary
                damageableCoverType = toHit.getDamagableCoverTypePrimary();
                coverBuilding = toHit.getCoverBuildingPrimary();
                coverDropShip = toHit.getCoverDropshipPrimary();
                coverLoc = toHit.getCoverLocPrimary();
            } else {
                // If not left side, then right side, which is secondary
                damageableCoverType = toHit.getDamagableCoverTypeSecondary();
                coverBuilding = toHit.getCoverBuildingSecondary();
                coverDropShip = toHit.getCoverDropshipSecondary();
                coverLoc = toHit.getCoverLocSecondary();
            }
        } else { // Only primary cover exists
            damageableCoverType = toHit.getDamagableCoverTypePrimary();
            coverBuilding = toHit.getCoverBuildingPrimary();
            coverDropShip = toHit.getCoverDropshipPrimary();
            coverLoc = toHit.getCoverLocPrimary();
        }
        // Check if we need to damage the cover that absorbed the hit.
        if (damageableCoverType == LosEffects.DAMAGABLE_COVER_DROPSHIP) {
            // We need to adjust some state and then restore it later
            // This allows us to make a call to handleEntityDamage
            ToHitData savedToHit = toHit;
            AimingMode savedAimingMode = weaponAttackAction.getAimingMode();
            weaponAttackAction.setAimingMode(AimingMode.NONE);
            int savedAimedLocation = weaponAttackAction.getAimedLocation();
            weaponAttackAction.setAimedLocation(Entity.LOC_NONE);
            boolean savedSalvo = bSalvo;
            bSalvo = true;
            // Create new toHitData
            toHit = new ToHitData(0, "", ToHitData.HIT_NORMAL,
                  ComputeSideTable.sideTable(attackingEntity, coverDropShip));
            // Report cover was damaged
            int sizeBefore = vPhaseReport.size();
            r = new Report(3465);
            r.subject = subjectId;
            r.add(coverDropShip.getShortName());
            vPhaseReport.add(r);
            // Damage the DropShip
            handleEntityDamage(coverDropShip, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            // Remove a blank line in the report list
            if (vPhaseReport.elementAt(sizeBefore).newlines > 0) {
                vPhaseReport.elementAt(sizeBefore).newlines--;
            }
            // Indent reports related to the damage absorption
            while (sizeBefore < vPhaseReport.size()) {
                vPhaseReport.elementAt(sizeBefore).indent(3);
                sizeBefore++;
            }
            // Restore state
            toHit = savedToHit;
            weaponAttackAction.setAimingMode(savedAimingMode);
            weaponAttackAction.setAimedLocation(savedAimedLocation);
            bSalvo = savedSalvo;
            // Damage a building that blocked a shot
        } else if (damageableCoverType == LosEffects.DAMAGABLE_COVER_BUILDING) {
            // Normal damage
            int nDamage = nDamPerHit * Math.min(nCluster, hits);
            Vector<Report> buildingReport = gameManager.damageBuilding(coverBuilding, nDamage,
                  " blocks the shot and takes ", coverLoc);
            for (Report report : buildingReport) {
                report.subject = subjectId;
                report.indent();
            }
            vPhaseReport.addAll(buildingReport);
            // Damage any infantry in the building.
            Vector<Report> infantryReport = gameManager.damageInfantryIn(coverBuilding, nDamage,
                  coverLoc, weaponType.getInfantryDamageClass());
            for (Report report : infantryReport) {
                report.indent(2);
            }
            vPhaseReport.addAll(infantryReport);
        }
        missed = true;
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     */
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        missed = false;

        initHit(entityTarget);

        boolean isIndirect = weapon.hasModes() && weapon.curMode().equals("Indirect");

        Hex targetHex = game.getHexOf(target);
        boolean mechPokingOutOfShallowWater = unitGainsPartialCoverFromWater(targetHex, entityTarget);

        // a very specific situation where a mek is standing in a height 1 building
        // or its upper torso is otherwise somehow poking out of said building
        boolean targetInShortBuilding = WeaponAttackAction.targetInShortCoverBuilding(target);
        boolean legHit = entityTarget.locationIsLeg(hit.getLocation());
        boolean shortBuildingBlocksLegHit = targetInShortBuilding && legHit;

        boolean partialCoverForIndirectFire = isIndirect && (mechPokingOutOfShallowWater || shortBuildingBlocksLegHit);

        // For indirect fire, remove leg hits only if target is in water partial cover
        // Per TW errata for indirect fire
        if ((!isIndirect || partialCoverForIndirectFire)
              && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(),
              ComputeSideTable.sideTable(attackingEntity, entityTarget,
                    weapon.getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                  nCluster, bldgAbsorbs);
            return;
        }

        if (!bSalvo) {
            // Each hit in the salvo gets its own hit location.
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            if (weapon.isRapidFire()) {
                r.newlines = 0;
                r = new Report(3225);
                r.subject = subjectId;
                r.add(numRapidFireHits * 3);
                vPhaseReport.add(r);
            }
        } else {
            Report.addNewline(vPhaseReport);
        }

        // for non-salvo shots, report that the aimed shot was successful
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            Report r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        int nDamage = nDamPerHit * Math.min(nCluster, hits);

        if (bDirect) {
            hit.makeDirectBlow(toHit.getMoS() / 3);
        }

        // Report calcDmgPerHitReports here
        if (!calcDmgPerHitReport.isEmpty()) {
            vPhaseReport.addAll(calcDmgPerHitReport);
            calcDmgPerHitReport.clear();
        }

        // if the target was in partial cover, then we already handled
        // damage absorption by the partial cover, if it had happened
        boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);

        nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs,
              vPhaseReport, bldg, targetStickingOutOfBuilding);

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);
        nDamage = checkLI(nDamage, entityTarget, vPhaseReport);

        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if ((null != bldg) && !targetStickingOutOfBuilding) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            Report r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }

            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
            }

            vPhaseReport.addAll(gameManager.damageEntity(entityTarget, hit, nDamage, false,
                  attackingEntity.getSwarmTargetId() == entityTarget.getId() ? DamageType.IGNORE_PASSENGER : damageType,
                  false, false, throughFront, underWater, nukeS2S));
            if (damageType.equals(DamageType.ANTI_TSM) && (target instanceof Mek)
                  && entityTarget.antiTSMVulnerable()) {
                vPhaseReport.addAll(gameManager.doGreenSmokeDamage(entityTarget));
            }
            // for salvo shots, report that the aimed location was hit after
            // applying damage, because the location is first reported when
            // dealing the damage
            if (hit.hitAimedLocation() && bSalvo) {
                Report r = new Report(3410);
                r.subject = subjectId;
                vPhaseReport.lastElement().newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        // If a BA squad is shooting at infantry, damage may be random and need
        // to be rerolled for the next hit (if any) from the same attack.
        if ((attackingEntity instanceof BattleArmor) && (target instanceof Infantry)) {
            nDamPerHit = calcDamagePerHit();
        }
    }

    /**
     * Worker function - does the entity gain partial cover from shallow water?
     */
    protected boolean unitGainsPartialCoverFromWater(Hex targetHex, Entity entityTarget) {
        return (targetHex != null) &&
              targetHex.containsTerrain(Terrains.WATER) &&
              (entityTarget.relHeight() == targetHex.getLevel());
    }

    /**
     * Worker function - is a part of this unit inside the hex's terrain features, but part sticking out?
     */
    protected boolean unitStickingOutOfBuilding(Hex targetHex, Entity entityTarget) {
        // target needs to be on the board,
        // be tall enough for it to make a difference,
        // target "feet" are below the "ceiling"
        // target "head" is above the "ceiling"
        return (targetHex != null) &&
              (entityTarget.getHeight() > 0) &&
              (entityTarget.getElevation() < targetHex.ceiling()) &&
              (entityTarget.relHeight() >= targetHex.ceiling());
    }

    /**
     * Worker function to (maybe) have a building absorb damage meant for the entity
     */
    protected int absorbBuildingDamage(int nDamage, Entity entityTarget, int bldgAbsorbs,
          Vector<Report> vPhaseReport, IBuilding bldg,
          boolean targetStickingOutOfBuilding) {
        // if the building will absorb some damage and the target is actually
        // entirely inside the building:
        if ((bldgAbsorbs > 0) && !targetStickingOutOfBuilding) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = gameManager.damageBuilding(bldg, toBldg,
                  entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
            // Units on same level, report building absorbs no damage
        } else if (bldgAbsorbs == Integer.MIN_VALUE) {
            Report.addNewline(vPhaseReport);
            Report r = new Report(9976);
            r.subject = attackingEntity.getId();
            r.indent(2);
            vPhaseReport.add(r);
            // Cases where absorbed damage doesn't reduce incoming damage
        } else if ((bldgAbsorbs < 0) && !targetStickingOutOfBuilding) {
            int toBldg = -bldgAbsorbs;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = gameManager.damageBuilding(bldg, toBldg,
                  entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        return nDamage;
    }

    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = getFireTNRoll();
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
                  tn, true, -1, vPhaseReport);
        }
    }

    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        handleClearDamage(vPhaseReport, bldg, nDamage, true);
    }

    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage,
          boolean hitReport) {
        if (!bSalvo && hitReport) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
              && gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
              getFireTNRoll(), 5, vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), target.getBoardId(), nDamage,
              subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    protected void handleBuildingDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage,
          Coords coords) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        Vector<Report> buildingReport = gameManager.damageBuilding(bldg, nDamage, coords);
        for (Report report : buildingReport) {
            report.subject = subjectId;
        }
        vPhaseReport.addAll(buildingReport);

        // Damage any infantry in hex, unless attack between units in same bldg
        if (toHit.getThruBldg() == null) {
            vPhaseReport.addAll(gameManager.damageInfantryIn(bldg, nDamage, coords,
                  weaponType.getInfantryDamageClass()));
        }
    }

    protected boolean allShotsHit() {
        if ((((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) || (target
              .getTargetType() == Targetable.TYPE_BUILDING)) && (nRange <= 1))
              || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            return true;
        }

        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
              && target.getTargetType() == Targetable.TYPE_ENTITY
              && ((Entity) target).isCapitalScale()
              && !((Entity) target).isCapitalFighter()
              && !attackingEntity.isCapitalFighter();
    }

    protected void reportMiss(Vector<Report> vPhaseReport) {
        reportMiss(vPhaseReport, false);
    }

    protected void reportMiss(Vector<Report> vPhaseReport, boolean singleNewline) {
        // Report the miss.
        Report r = new Report(3220);
        r.subject = subjectId;
        if (singleNewline) {
            r.newlines = 1;
        } else {
            r.newlines = 2;
        }
        vPhaseReport.addElement(r);
    }

    protected WeaponHandler() {
        // deserialization only
    }

    // Among other things, basically a refactored Server#preTreatWeaponAttack
    public WeaponHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        damageType = DamageType.NONE;
        toHit = toHitData;
        this.weaponAttackAction = weaponAttackAction;
        this.game = game;

        weaponEntity = this.game.getEntity(this.weaponAttackAction.getEntityId());
        if (weaponEntity == null) {
            throw new EntityLoadingException("Weapon Entity is NULL");
        }

        attackingEntity = weaponEntity.getAttackingEntity();
        if (attackingEntity == null) {
            throw new EntityLoadingException("Attacking Entity is NULL");
        }

        weapon = (WeaponMounted) weaponEntity.getEquipment(this.weaponAttackAction.getWeaponId());
        weaponType = weapon.getType();
        ammoType = (weapon.getLinked() != null && weapon.getLinked().getType() instanceof AmmoType)
              ? (AmmoType) weapon.getLinked().getType()
              : null;
        typeName = weaponType.getInternalName();
        target = this.game.getTarget(this.weaponAttackAction.getTargetType(), this.weaponAttackAction.getTargetId());
        gameManager = twGameManager;
        subjectId = getAttackerId();
        nRange = Compute.effectiveDistance(this.game, attackingEntity, target);
        if (target instanceof Mek) {
            throughFront = Compute.isThroughFrontHex(this.game, attackingEntity.getPosition(), (Entity) target);
        } else {
            throughFront = true;
        }
        // is this an underwater attack on a surface naval vessel?
        underWater = toHit.getHitTable() == ToHitData.HIT_UNDERWATER;
        if (null != attackingEntity.getCrew()) {
            roll = attackingEntity.getCrew().rollGunnerySkill();
        } else {
            roll = Compute.rollD6(2);
        }

        numWeapons = getNumberWeapons();
        numWeaponsHit = 1;
        // use ammo when creating this, so it works when shooting the last shot
        // a unit has and we fire multiple weapons of the same type
        // TODO : need to adjust this for cases where not all the ammo is available
        for (int i = 0; i < numWeapons; i++) {
            useAmmo();
        }

        if (target instanceof Entity) {
            ((Entity) target).addAttackedByThisTurn(weaponAttackAction.getEntityId());
            if (!attackingEntity.isAirborne()) {
                ((Entity) target).addGroundAttackedByThisTurn(weaponAttackAction.getEntityId());
            }
        }
    }

    /**
     * Worker function that initializes the actual hit, including a hit location and various other properties.
     *
     * @param entityTarget Entity being hit.
     */
    protected void initHit(Entity entityTarget) {
        hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
              weaponAttackAction.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(weaponType.isCapital());
        hit.setBoxCars(roll.getIntValue() == 12);
        hit.setCapMisCritMod(getCapMisMod());
        hit.setFirstHit(firstHit);
        hit.setAttackerId(getAttackerId());

        if (weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
    }

    protected void useAmmo() {
        if (weaponType.hasFlag(WeaponType.F_DOUBLE_ONE_SHOT)) {
            ArrayList<Mounted<?>> chain = new ArrayList<>();
            for (Mounted<?> current = weapon.getLinked(); current != null; current = current.getLinked()) {
                chain.add(current);
            }

            if (!chain.isEmpty()) {
                chain.sort((m1, m2) -> Integer.compare(m2.getUsableShotsLeft(), m1.getUsableShotsLeft()));
                weapon.setLinked(chain.get(0));
                for (int i = 0; i < chain.size() - 1; i++) {
                    chain.get(i).setLinked(chain.get(i + 1));
                }
                chain.get(chain.size() - 1).setLinked(null);
                if (weapon.getLinked().getUsableShotsLeft() == 0) {
                    weapon.setFired(true);
                }
            }
        } else if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
            weapon.setFired(true);
        }

        setDone();
    }

    protected void setDone() {
        weapon.setUsedThisRound(true);
    }

    protected void addHeat() {
        // Only add heat for first shot in strafe
        if (isStrafing && !isStrafingFirstShot()) {
            return;
        }
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (attackingEntity.isLargeCraft() && !game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                int loc = weapon.getLocation();
                boolean rearMount = weapon.isRearMounted();
                if (!attackingEntity.hasArcFired(loc, rearMount)) {
                    attackingEntity.heatBuildup += attackingEntity.getHeatInArc(loc, rearMount);
                    attackingEntity.setArcFired(loc, rearMount);
                }
            } else {
                attackingEntity.heatBuildup += (weapon.getHeatByBay());
            }
        }
    }

    /**
     * Does this attack use the cluster hit table? necessary to determine how Aero damage should be applied
     */
    protected boolean usesClusterTable() {
        return false;
    }

    /**
     * special resolution, like minefields and arty
     *
     * @param vPhaseReport - a <code>Vector</code> containing the phase report
     * @param entityTarget - the <code>Entity</code> targeted, or <code>null</code>, if no Entity targeted
     *
     * @return true when done with processing, false when not
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        return false;
    }

    @Override
    public boolean announcedEntityFiring() {
        return announcedEntityFiring;
    }

    @Override
    public void setAnnouncedEntityFiring(boolean announcedEntityFiring) {
        this.announcedEntityFiring = announcedEntityFiring;
    }

    @Override
    public WeaponAttackAction getWeaponAttackAction() {
        return weaponAttackAction;
    }

    public int checkTerrain(int nDamage, Entity entityTarget, Vector<Report> vPhaseReport) {
        if (entityTarget == null) {
            return nDamage;
        }
        Hex hex = game.getHexOf(entityTarget);
        boolean hasWoods = hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE);
        boolean isAboveWoods = (entityTarget.relHeight() + 1 > hex.terrainLevel(Terrains.FOLIAGE_ELEV))
              || entityTarget.isAirborne() || !hasWoods;

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)
              && hasWoods && !isAboveWoods
              && !(entityTarget.getSwarmAttackerId() == attackingEntity.getId())) {
            Terrain woodHex = hex.getTerrain(Terrains.WOODS);
            Terrain jungleHex = hex.getTerrain(Terrains.JUNGLE);
            int treeAbsorbs = 0;
            String hexType = "";
            if (woodHex != null) {
                treeAbsorbs = woodHex.getLevel() * 2;
                hexType = "wooded";
            } else if (jungleHex != null) {
                treeAbsorbs = jungleHex.getLevel() * 2;
                hexType = "jungle";
            }

            // Do not absorb more damage than the weapon can do.
            treeAbsorbs = Math.min(nDamage, treeAbsorbs);

            nDamage = Math.max(0, nDamage - treeAbsorbs);
            gameManager.tryClearHex(entityTarget.getPosition(),
                  entityTarget.getBoardId(),
                  treeAbsorbs,
                  attackingEntity.getId());
            Report.addNewline(vPhaseReport);
            Report terrainReport = new Report(6427);
            terrainReport.subject = entityTarget.getId();
            terrainReport.add(hexType);
            terrainReport.add(treeAbsorbs);
            terrainReport.indent(2);
            terrainReport.newlines = 0;
            vPhaseReport.add(terrainReport);
        }
        return nDamage;
    }

    /**
     * Check for Laser Inhibiting smoke clouds, does not work against PPCs, Plasma Weapons, or Flamers per TacOps:AUE
     * (6th) pg. 168
     */
    public int checkLI(int nDamage, Entity entityTarget, Vector<Report> vPhaseReport) {
        if ((attackingEntity.getPosition() == null) || (entityTarget.getPosition() == null)) {
            return nDamage;
        }

        weapon = weaponAttackAction.getEntity(game).getWeapon(weaponAttackAction.getWeaponId());
        weaponType = weapon.getType();

        if (!weaponType.hasFlag(WeaponType.F_LASER)) {
            return nDamage;
        }

        ArrayList<Coords> coords = Coords.intervening(attackingEntity.getPosition(), entityTarget.getPosition());
        Board board = game.commonEnclosingBoard(attackingEntity, entityTarget).orElse(game.getBoard(attackingEntity));
        int reFrac = 0;
        double travel = 0;
        double range = attackingEntity.getPosition().distance(target.getPosition());
        double atkLev = attackingEntity.relHeight();
        double tarLev = entityTarget.relHeight();
        double levDif = Math.abs(atkLev - tarLev);
        String hexType = "LASER inhibiting smoke";

        // loop through all intervening coords.
        // If you could move this to compute.java, then remove - import
        // java.util.ArrayList;
        for (Coords curr : coords) {
            // skip hexes not actually on the board
            if (!board.contains(curr)) {
                continue;
            }
            Terrain smokeHex = board.getHex(curr).getTerrain(Terrains.SMOKE);
            if (board.getHex(curr).containsTerrain(Terrains.SMOKE)
                  && ((smokeHex.getLevel() == SmokeCloud.SMOKE_LI_LIGHT)
                  || (smokeHex.getLevel() == SmokeCloud.SMOKE_LI_HEAVY))) {

                int levIt = ((board.getHex(curr).getLevel()) + 2);

                // does the hex contain LASER inhibiting smoke?
                if ((tarLev > atkLev)
                      && (levIt >= ((travel * (levDif / range)) + atkLev))) {
                    reFrac++;
                } else if ((atkLev > tarLev)
                      && (levIt >= (((range - travel) * (levDif / range)) + tarLev))) {
                    reFrac++;
                } else if ((atkLev == tarLev) && (levIt >= 0)) {
                    reFrac++;
                }
                travel++;
            }
        }
        if (reFrac != 0) {
            // Damage reduced by 2 for each intervening smoke.
            reFrac = (reFrac * 2);

            // Do not absorb more damage than the weapon can do. (Are both of
            // these really necessary?)
            reFrac = Math.min(nDamage, reFrac);
            nDamage = Math.max(0, (nDamage - reFrac));

            Report.addNewline(vPhaseReport);
            Report fogReport = new Report(6427);
            fogReport.subject = entityTarget.getId();
            fogReport.add(hexType);
            fogReport.add(reFrac);
            fogReport.indent(2);
            fogReport.newlines = 0;
            vPhaseReport.add(fogReport);
        }
        return nDamage;
    }

    /**
     * Insert any additional attacks that should occur before this attack
     */
    protected void insertAttacks(GamePhase phase, Vector<Report> vPhaseReport) {

    }

    /**
     * @return the number of weapons of this type firing (for squadron weapon groups)
     */
    protected int getNumberWeapons() {
        return weapon.getNWeapons();
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = weaponType.getName();
        } else {
            weaponType = (WeaponType) EquipmentType.get(typeName);
        }

        if (weaponType == null) {
            LOGGER.error("Could not restore equipment type \"{}\"", typeName);
        }
    }

    protected int getClusterModifiers(boolean clusterRangePenalty) {
        int nMissilesModifier = getSalvoBonus();

        int[] ranges = weaponType.getRanges(weapon);
        if (clusterRangePenalty && game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CLUSTER_HIT_PEN)) {
            if (nRange <= 1) {
                nMissilesModifier += 1;
            } else if (nRange > ranges[RangeType.RANGE_MEDIUM]) {
                nMissilesModifier -= 1;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > ranges[RangeType.RANGE_LONG])) {
            nMissilesModifier -= 2;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > ranges[RangeType.RANGE_EXTREME])) {
            nMissilesModifier -= 3;
        }

        if (bGlancing) {
            nMissilesModifier -= 4;
        }

        if (bLowProfileGlancing) {
            nMissilesModifier -= 4;
        }

        if (bDirect) {
            nMissilesModifier += (toHit.getMoS() / 3) * 2;
        }

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getEMI().isEMI()) {
            nMissilesModifier -= 2;
        }

        if (null != attackingEntity.getCrew()) {
            if (attackingEntity.hasAbility(OptionsConstants.GUNNERY_SANDBLASTER, weaponType.getName())) {
                if (nRange > ranges[RangeType.RANGE_MEDIUM]) {
                    nMissilesModifier += 2;
                } else if (nRange > ranges[RangeType.RANGE_SHORT]) {
                    nMissilesModifier += 3;
                } else {
                    nMissilesModifier += 4;
                }
            } else if (attackingEntity.hasAbility(OptionsConstants.GUNNERY_CLUSTER_MASTER)) {
                nMissilesModifier += 2;
            } else if (attackingEntity.hasAbility(OptionsConstants.GUNNERY_CLUSTER_HITTER)) {
                nMissilesModifier += 1;
            }
        }
        return nMissilesModifier;
    }

    @Override
    public boolean isStrafing() {
        return isStrafing;
    }

    @Override
    public void setStrafing(boolean isStrafing) {
        this.isStrafing = isStrafing;
    }

    @Override
    public boolean isStrafingFirstShot() {
        return isStrafingFirstShot;
    }

    @Override
    public void setStrafingFirstShot(boolean isStrafingFirstShot) {
        this.isStrafingFirstShot = isStrafingFirstShot;
    }

    /**
     * Determine the "glancing blow" divider. 2 if the shot is "glancing" or "glancing due to low profile" 4 if both int
     * version
     */
    protected int applyGlancingBlowModifier(int initialValue, boolean roundup) {
        return (int) applyGlancingBlowModifier((double) initialValue, roundup);
    }

    /**
     * Determine the "glancing blow" divider. 2 if the shot is "glancing" or "glancing due to low profile" 4 if both
     * double version
     */
    protected double applyGlancingBlowModifier(double initialValue, boolean roundup) {
        // if we're not going to be applying any glancing blow modifiers, just return
        // what we came in with
        if (!bGlancing && !bLowProfileGlancing) {
            return initialValue;
        }

        double divisor = getTotalGlancingBlowFactor();
        double intermediateValue = initialValue / divisor;
        return roundup ? Math.ceil(intermediateValue) : Math.floor(intermediateValue);
    }

    /**
     * Logic to determine the glancing blow multiplier: 1 if no glancing blow 2 if one type of glancing blow (either
     * usual or narrow/low profile) 4 if both types of glancing blow
     */
    protected double getTotalGlancingBlowFactor() {
        return (bGlancing ? 2.0 : 1.0) * (bLowProfileGlancing ? 2.0 : 1.0);
    }

    /**
     * Worker function that sets the glancing blow flags for this attack for the target when appropriate
     */
    protected void setGlancingBlowFlags(Entity entityTarget) {
        // are we a glancing hit? Check for this here, report it later
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS)) {
            bGlancing = roll.getIntValue() == toHit.getValue();
        }

        // low profile glancing blows are triggered on roll = toHit or toHit - 1
        bLowProfileGlancing = isLowProfileGlancingBlow(entityTarget, toHit);
    }

    /**
     * Worker function that determines if the given hit on the given entity is a glancing blow as per narrow/low profile
     * quirk rules
     */
    protected boolean isLowProfileGlancingBlow(Entity entityTarget, ToHitData hitData) {
        return (entityTarget != null) &&
              entityTarget.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE) &&
              ((roll.getIntValue() == hitData.getValue()) || (roll.getIntValue() == hitData.getValue() + 1));
    }

    /**
     * Worker function that adds the 'glancing blow' reports
     */
    protected void addGlancingBlowReports(Vector<Report> vPhaseReport) {
        Report r;

        if (bGlancing) {
            r = new Report(3186);
            r.subject = attackingEntity.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        if (bLowProfileGlancing) {
            r = new Report(9985);
            r.subject = attackingEntity.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
    }

    /**
     * Used by certain artillery handlers to draw drift markers with "hit" graphics if anything is caught in the blast,
     * or "drift" marker if nothing is damaged. No-op for direct hits.
     */
    protected void handleArtilleryDriftMarker(Coords targetPos, Coords finalPos, ArtilleryAttackAction aaa,
          Vector<Integer> hitIds) {
        if (bMissed) {
            String msg = Messages.getString("ArtilleryMessage.drifted") + " " + targetPos.getBoardNum();
            final SpecialHexDisplay shd;
            if (hitIds.isEmpty()) {
                shd = new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_DRIFT,
                      game.getRoundCount(),
                      game.getPlayer(aaa.getPlayerId()),
                      msg);
            } else {
                shd = new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT,
                      game.getRoundCount(),
                      game.getPlayer(aaa.getPlayerId()),
                      msg);
            }
            game.getBoard(aaa.getTarget(game)).addSpecialHexDisplay(finalPos, shd);
        }
    }
}
