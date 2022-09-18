/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.AmmoType.*;

import java.util.*;

/**
 * Utility class for obtaining BV Skill Multipliers (TM p.315).
 */
public abstract class BVCalculator {

    protected final Entity entity;
    protected double defensiveValue;
    protected double offensiveValue;
    protected double baseBV;
    protected double adjustedBV;
    protected CalculationReport bvReport;
    protected boolean ignoreC3;
    protected boolean ignoreSkill;
    protected Map<String, Double> ammo = new HashMap<>();
    protected List<String> keys = new ArrayList<>();
    protected Map<String, String> names = new HashMap<>();
    protected Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();

    BVCalculator(Entity entity) {
        this.entity = entity;
    }


    public static BVCalculator getBVCalculator(Entity entity) {
        if (entity instanceof Mech) {
            return new MekBVCalculator(entity);
//        } else if (entity instanceof Protomech) {
//            return new ProtoMekBVCalculator();
//        } else if (entity instanceof BattleArmor) {
//            return new BattleArmorBVCalculator();
//        } else if (entity instanceof Infantry) {
//            return new InfantryBVCalculator();
//        } else if (entity instanceof Jumpship) {
//            return new JumpShipBVCalculator();
//        } else if (entity instanceof SmallCraft) {
//            return new DropShipBVCalculator();
//        } else if (entity instanceof Aero) {
//            return new AeroBVCalculator();
        } else {//  if (entity instanceof GunEmplacement) {
            return new GunEmplacementBVCalculator(entity);
//        } else if (entity instanceof Tank) {
//            return new CombatVehicleBVCalculator();
        }
    }

    public int getBV(boolean ignoreC3, boolean ignoreSkill) {
        return getBV(ignoreC3, ignoreSkill, new DummyCalculationReport());
    }

    public int getBV(boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        this.ignoreC3 = ignoreC3;
        this.ignoreSkill = ignoreSkill;
        getBaseBV(bvReport);
        adjustBV();
        return (int) Math.round(adjustedBV);
    }

    public int getBaseBV() {
        getBaseBV(new DummyCalculationReport());
        return (int) Math.round(baseBV);
    }

    public int getBaseBV(CalculationReport bvReport) {
        this.bvReport = bvReport;
        processBaseBV();
        return (int) Math.round(baseBV);
    }

    protected void processBaseBV() {
        reset();
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(entity.getChassis() + " " + entity.getModel());
        assembleAmmo();
        bvReport.addSubHeader("Defensive Battle Rating:");
        processDefensiveValue();
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Offensive Battle Rating:");
        processOffensiveValue();
        processSummarize();
    }

    protected void reset() {
        defensiveValue = 0;
        offensiveValue = 0;
        baseBV = 0;
        adjustedBV = 0;
        ammo.clear();
        keys.clear();
    }

    protected abstract void processDefensiveValue();

    protected abstract void processOffensiveValue();

    protected void processAmmoValue() {
        for (String key : keys) {
            // They dont exist in either hash then dont bother adding nulls.
            if (!ammo.containsKey(key) || !weaponsForExcessiveAmmo.containsKey(key)) {
                continue;
            }
            if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                offensiveValue += weaponsForExcessiveAmmo.get(key);
                bvReport.addLine(names.get(key),
                        "+ " + formatForReport(weaponsForExcessiveAmmo.get(key)) + " (Excessive BV)",
                        "= " + formatForReport(offensiveValue));
            } else {
                offensiveValue += ammo.get(key);
                bvReport.addLine(names.get(key), "+ " + formatForReport(ammo.get(key)),
                        "= " + formatForReport(offensiveValue));
            }

        }
    }

    protected boolean ammoCounts(Mounted ammo) {
        AmmoType ammoType = (AmmoType) ammo.getType();
        return (ammo.getUsableShotsLeft() > 0)
                && (ammoType.getAmmoType() != AmmoType.T_AMS)
                && (ammoType.getAmmoType() != AmmoType.T_APDS)
                && !ammo.isOneShotAmmo();
    }

    protected void processSummarize() {
        baseBV = defensiveValue + offensiveValue;
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Battle Value:");
        bvReport.addLine("--- Base Unit BV:",
                formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue) + ", rn",
                "= " + (int) Math.round(baseBV));
    }

    protected void assembleAmmo() {
        for (Mounted ammo : entity.getAmmo()) {
            AmmoType atype = (AmmoType) ammo.getType();

            // don't count depleted ammo, AMS and oneshot ammo
            if (ammoCounts(ammo)) {
                String key = atype.getAmmoType() + ":" + atype.getRackSize();
                if (!keys.contains(key)) {
                    keys.add(key);
                    names.put(key, ammo.getDesc());
                }
                if (!this.ammo.containsKey(key)) {
                    this.ammo.put(key, atype.getBV(entity));
                } else {
                    this.ammo.put(key, atype.getBV(entity) + this.ammo.get(key));
                }
            }
        }

        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(entity);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }
    }

    /**
     * Adjust the BV with force bonuses (TAG, C3) and pilot skill.
     */
    protected void adjustBV() {
        adjustedBV = (int) Math.round(baseBV);
        double tagBonus = BVCalculator.bvTagBonus(entity);
        double c3Bonus = ignoreC3 ? 0 : entity.getExtraC3BV((int) Math.round(adjustedBV));
        double pilotFactor = ignoreSkill ? 1 : BVCalculator.bvMultiplier(entity);
        if ((tagBonus == 0) && (c3Bonus == 0) && (pilotFactor == 1)) {
            return;
        }

//        bvReport.addEmptyLine();
//        bvReport.addSubHeader("Force Adjustment:");
        if (tagBonus > 0) {
            adjustedBV += tagBonus;
            bvReport.addLine("Force Bonus (TAG):",
                    "+ " + formatForReport(tagBonus), "= " + formatForReport(adjustedBV));
        }

        if (c3Bonus > 0) {
            adjustedBV += c3Bonus;
            bvReport.addLine("Force Bonus (C3):",
                    "+ " + formatForReport(c3Bonus), "= " + formatForReport(adjustedBV));
        }

        if (pilotFactor != 1) {
            bvReport.addLine("Pilot Modifier:",
                    formatForReport(adjustedBV) + " x " + formatForReport(pilotFactor),
                    "= " + formatForReport(adjustedBV * pilotFactor));
            adjustedBV *= pilotFactor;
        }

//        bvReport.addEmptyLine();
//        bvReport.addSubHeader("--- Adjusted BV:");
        bvReport.addLine("--- Adjusted BV:", formatForReport(adjustedBV) + ", rn",
                "= " + (int) Math.round(adjustedBV));
    }

    private static final double[][] bvMultipliers = new double[][]{
            {2.42, 2.31, 2.21, 2.10, 1.93, 1.75, 1.68, 1.59, 1.50},
            {2.21, 2.11, 2.02, 1.92, 1.76, 1.60, 1.54, 1.46, 1.38},
            {1.93, 1.85, 1.76, 1.68, 1.54, 1.40, 1.35, 1.28, 1.21},
            {1.66, 1.58, 1.51, 1.44, 1.32, 1.20, 1.16, 1.10, 1.04},
            {1.38, 1.32, 1.26, 1.20, 1.10, 1.00, 0.95, 0.90, 0.85},
            {1.31, 1.19, 1.13, 1.08, 0.99, 0.90, 0.86, 0.81, 0.77},
            {1.24, 1.12, 1.07, 1.02, 0.94, 0.85, 0.81, 0.77, 0.72},
            {1.17, 1.06, 1.01, 0.96, 0.88, 0.80, 0.76, 0.72, 0.68},
            {1.10, 0.99, 0.95, 0.90, 0.83, 0.75, 0.71, 0.68, 0.64},
    };

    /**
     * Returns the BV multiplier for the gunnery/piloting of the given entity's pilot (TM p.315) as well as MD
     * implants of the pilot.
     * Returns 1 if the given entity's crew is null. Special treatment is given to infantry units where
     * units unable to make anti-mek attacks use 5 as their anti-mek (piloting) value as well as LAM pilots that
     * use the average of their aero and mek values.
     *
     * @param entity The entity to get the skill modifier for
     * @return The BV multiplier for the given entity's pilot
     */
    public static double bvMultiplier(Entity entity) {
        if (entity.getCrew() == null) {
            return 1;
        }
        int gunnery = entity.getCrew().getGunnery();
        int piloting = entity.getCrew().getPiloting();

        if ((entity instanceof Infantry) && (!((Infantry) entity).canMakeAntiMekAttacks())) {
            piloting = 5;
        } else if (entity.getCrew() instanceof LAMPilot) {
            LAMPilot lamPilot = (LAMPilot) entity.getCrew();
            gunnery = (lamPilot.getGunneryMech() + lamPilot.getGunneryAero()) / 2;
            piloting = (lamPilot.getPilotingMech() + lamPilot.getPilotingAero()) / 2;
        }
        return bvImplantMultiplier(entity) * bvSkillMultiplier(gunnery, piloting);
    }

    /**
     * Returns the BV multiplier for the given gunnery and piloting values. Returns 1 for the neutral
     * values 4/5.
     *
     * @param gunnery  the gunnery skill of a pilot
     * @param piloting the piloting skill of a pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double bvSkillMultiplier(int gunnery, int piloting) {
        return bvMultipliers[MathUtility.clamp(gunnery, 0, 8)][MathUtility.clamp(piloting, 0, 8)];
    }

    /**
     * Returns the BV multiplier for any MD implants that the crew of the given entity has. When the crew
     * doesn't have any relevant MD implants, returns 1.
     *
     * @param entity The entity to get the skill modifier for
     * @return a multiplier to the BV of the given entity
     */
    public static double bvImplantMultiplier(Entity entity) {
        int level = 1;
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
            level = 2;
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)) {
            level = 3;
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            level = 5;
        }
        return level / 4.0 + 0.75;
    }

    /**
     * Returns the BV bonus that a unit with TAG, LTAG or C3M gets for friendly units that have semi-guided
     * or Arrow IV homing ammunition
     * (TO:AUE p.198, https://bg.battletech.com/forums/tactical-operations/tagguided-munitions-and-bv/)
     *
     * @param entity The entity to get the skill modifier for
     * @return A BV bonus for the given entity
     */
    public static double bvTagBonus(Entity entity) {
        long tagCount = workingTAGCount(entity);
        if ((tagCount == 0) || (entity.getGame() == null)) {
            return 0;
        }
        double bvBonus = 0;
        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            if ((otherEntity == entity) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            for (Mounted mounted : otherEntity.getAmmo()) {
                AmmoType atype = (AmmoType) mounted.getType();
                long munitionType = atype.getMunitionType();
                if ((mounted.getUsableShotsLeft() > 0)
                        && ((munitionType == M_SEMIGUIDED) || (munitionType == M_HOMING))) {
                    bvBonus += atype.getBV(otherEntity);
                }
            }
        }
        return bvBonus * tagCount;
    }

    private static long workingTAGCount(Entity entity) {
        return entity.getWeaponList().stream()
                .filter(m -> !m.isMissing() && !m.isDestroyed())
                .map(Mounted::getType)
                .filter(Objects::nonNull)
                .filter(t -> t.hasFlag(WeaponType.F_TAG))
                .count();
    }


}
