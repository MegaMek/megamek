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

/*
 * Author: Jay Lawson (Taharqa)
 */

package megamek.common.verifier;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.weapons.infantry.InfantryWeapon;

public class TestBattleArmor extends TestEntity {

    /**
     * Keeps track of the number of free MP a Bipedal BA gets.
     */
    public static int BIPED_FREE_MP = 1;

    /**
     * Keeps track of the number of free MP a Quad BA gets.
     */
    public static int QUAD_FREE_MP = 2;

    /**
     * BattleArmor can have a variable number of shots per slot of ammo, this
     * variable defines the maximum number of shots per slot they can have.
     */
    public static int NUM_SHOTS_PER_CRIT = 4;

    /**
     * BA Tube Artillery gets to be special and has 8 shots per-crit and comes
     * in 2-shot clips.
     */
    public static int NUM_SHOTS_PER_CRIT_TA = 8;

    /**
     * An enumeration that keeps track of the legal armors for BattleArmor. Each
     * entry consists of the type, which corresponds to the types defined in
     * <code>EquipmentType</code>.
     *
     * @author arlith
     *
     */
    public static enum BAArmor {
        STANDARD(EquipmentType.T_ARMOR_BA_STANDARD,0,false),
        CLAN_STANDARD(EquipmentType.T_ARMOR_BA_STANDARD,0,true),
        STANDARD_PROTOTYPE(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE,4,false),
        STANDARD_ADVANCED(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED,5,false),
        STEALTH_BASIC(EquipmentType.T_ARMOR_BA_STEALTH_BASIC,3,false),
        CLAN_STEALTH_BASIC(EquipmentType.T_ARMOR_BA_STEALTH_BASIC,3,true),
        STEALTH(EquipmentType.T_ARMOR_BA_STEALTH,4,false),
        CLAN_STEALTH(EquipmentType.T_ARMOR_BA_STEALTH,4,true),
        STEALTH_IMPROVED(EquipmentType.T_ARMOR_BA_STEALTH_IMP,5,false),
        CLAN_STEALTH_IMPROVED(EquipmentType.T_ARMOR_BA_STEALTH_IMP,5,true),
        STEALTH_PROTOTYPE(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE,4,false),
        FIRE_RESISTANT(EquipmentType.T_ARMOR_BA_FIRE_RESIST,5,false),
        CLAN_FIRE_RESISTANT(EquipmentType.T_ARMOR_BA_FIRE_RESIST,5,true),
        MIMETIC(EquipmentType.T_ARMOR_BA_MIMETIC,7,false),
        CLAN_MIMETIC(EquipmentType.T_ARMOR_BA_MIMETIC,7,true),
        REFLECTIVE(EquipmentType.T_ARMOR_BA_REFLECTIVE,7,false),
        CLAN_REFLECTIVE(EquipmentType.T_ARMOR_BA_REFLECTIVE,7,true),
        REACTIVE(EquipmentType.T_ARMOR_BA_REACTIVE,7,false),
        CLAN_REACTIVE(EquipmentType.T_ARMOR_BA_REACTIVE,7,true);

        /**
         * The type, corresponding to types defined in
         * <code>EquipmentType</code>.
         */
        public int type;

        /**
         * The number of spaces occupied by the armor type.
         */
        public int space;

        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;

        BAArmor(int t, int s, boolean c) {
            type = t;
            space = s;
            isClan = c;
        }

        public static int getNumBAArmors() {
            return values().length;
        }

        /**
         * Given an armor type, return the <code>BAArmor</code> instance that
         * represents that type.
         *
         * @param t
         *            The armor type.
         * @param c
         *            Whether this armor type is Clan or not.
         * @return The <code>BAArmor</code> that correspondes to the given type
         *         or null if no match was found.
         */
        public static BAArmor getArmor(int t, boolean c) {
            for (BAArmor a : values()) {
                if ((a.type == t) && (a.isClan == c)) {
                    return a;
                }
            }
            return null;
        }
    }

    /**
     * An enumeration that keeps track of the legal manipulators for
     * BattleArmor.
     *
     * @author arlith
     *
     */
    public static enum BAManipulator {
        NONE(BattleArmor.MANIPULATOR_NONE,false),
        ARMORED_GLOVE(BattleArmor.MANIPULATOR_ARMORED_GLOVE,false),
        BASIC(BattleArmor.MANIPULATOR_BASIC,false),
        BASIC_MINE_CLEARANCE(BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE,true),
        BATTLE(BattleArmor.MANIPULATOR_BATTLE,false),
        BATTLE_MAGNET(BattleArmor.MANIPULATOR_BATTLE_MAGNET,true),
        BATTLE_VIBRO(BattleArmor.MANIPULATOR_BATTLE_VIBRO,false),
        CARGO_LIFTER(BattleArmor.MANIPULATOR_CARGO_LIFTER,true),
        HEAVY_BATTLE(BattleArmor.MANIPULATOR_HEAVY_BATTLE,false),
        HEAVY_BATTLE_MAGNET(BattleArmor.MANIPULATOR_HEAVY_BATTLE_MAGNET,true),
        HEAVY_BATTLE_VIBRO(BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO,false),
        SALVAGE_ARM(BattleArmor.MANIPULATOR_SALVAGE_ARM,false),
        DRILL(BattleArmor.MANIPULATOR_INDUSTRIAL_DRILL,false);

        /**
         * The type, corresponding to types defined in
         * <code>EquipmentType</code>.
         */
        public int type;

        /**
         * The name of this manipulator
         */
        public String internalName;

        public String displayName;

        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean pairMounted;

        BAManipulator(int t, boolean p) {
            type = t;
            internalName = BattleArmor.MANIPULATOR_TYPE_STRINGS[t];
            displayName = BattleArmor.MANIPULATOR_NAME_STRINGS[t];
            pairMounted = p;
        }

        public static int getNumBAArmors() {
            return values().length;
        }

        /**
         * Given an manipulator internal name, return the
         * <code>BAManipulator</code> instance that represents that internal
         * name.
         *
         * @param name
         *            The internal name.
         * @return The <code>BAManipulator</code> that correspondes to the given
         *         internal name or null if no match was found.
         */
        public static BAManipulator getManipulator(String name) {
            for (BAManipulator m : values()) {
                if (m.internalName.equals(name) || m.displayName.equals(name)) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * Checks to see if the supplied <code>Mounted</code> is valid to be mounted
     * in the given location on the supplied <code>BattleArmor</code>.
     *
     * This method will check that there is available space in the given
     * location make sure that weapon mounting restrictions hold.
     *
     * @param ba
     * @param newMount
     * @param loc
     * @return
     */
    public static boolean isMountLegal(BattleArmor ba, Mounted newMount, int loc) {
        return isMountLegal(ba, newMount, loc, BattleArmor.LOC_SQUAD);
    }

    /**
     * Checks to see if the supplied <code>Mounted</code> is valid to be mounted
     * in the given location on the supplied <code>BattleArmor</code> for the
     * specified suit in the squad.
     *
     * This method will check that there is available space in the given
     * location make sure that weapon mounting restrictions hold.
     *
     * @param ba
     * @param newMount
     * @param loc
     * @param trooper
     * @return
     */
    public static boolean isMountLegal(BattleArmor ba, Mounted newMount,
            int loc, int trooper) {
        int numUsedCrits = 0;
        int numAntiMechWeapons = 0;
        int numAntiPersonnelWeapons = 0;
        for (Mounted m : ba.getEquipment()) {
            // Manipulators don't take up slots in BA
            if (m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                continue;
            }
            
            // AP weapons don't take up slots in BA (the AP Mount does)
            if (m.getType().hasFlag(WeaponType.F_INFANTRY)) {
                continue;
            }
            
            if (m.getBaMountLoc() == loc 
                    && (m.getLocation() == trooper 
                        || m.getLocation() == BattleArmor.LOC_SQUAD)) {
                
                if ((m.getType() instanceof WeaponType) 
                        && !(m.getType() instanceof InfantryWeapon)) {
                    numAntiMechWeapons++;
                }
                if (m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                    numAntiPersonnelWeapons++;
                }             
                if (m.getType().isSpreadable()) {
                    numUsedCrits++;
                } else {
                    numUsedCrits += m.getType().getCriticals(ba);
                }
            }
        }

        // Do we have free space to mount this equipment?
        int newCrits;
        if (newMount.getType().isSpreadable()) {
            newCrits = 1;
        } else {
            newCrits = newMount.getType().getCriticals(ba);
        }
        if ((numUsedCrits + newCrits) <= ba.getNumCrits(loc)) {
            // Weapons require extra criticism
            if (newMount.getType() instanceof WeaponType) {
                if ((numAntiMechWeapons + 1) <= 
                        ba.getNumAllowedAntiMechWeapons(loc)) {
                    return true;
                } else {
                    return false;
                }
            } else if (newMount.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                if ((numAntiPersonnelWeapons + 1) <= 
                        ba.getNumAllowedAntiPersonnelWeapons(loc,trooper)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private BattleArmor ba;

    public TestBattleArmor(BattleArmor armor, TestEntityOption option,
            String fileString) {
        super(option, null, null, null);
        ba = armor;
        this.fileString = fileString;
    }

    @Override
    public Entity getEntity() {
        return ba;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMech() {
        return false;
    }

    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public float getWeightControls() {
        return 0;
    }

    @Override
    public float getWeightMisc() {
        return 0;
    }

    @Override
    public float getWeightHeatSinks() {
        return 0;
    }

    @Override
    public float getWeightEngine() {
        return 0;
    }

    @Override
    public float getWeightStructure() {
        float tons = 0;

        int walkMP = ba.getOriginalWalkMP();
        if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD) {
            walkMP -= QUAD_FREE_MP;
        } else {
            walkMP -= BIPED_FREE_MP;
        }
        int jumpMP = ba.getOriginalJumpMP();
        switch (ba.getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                if (ba.isClan() && ba.isClanExoWithoutHarjel()) {
                    tons += 0.13;
                } else {
                    tons += 0.08;
                }
                tons += walkMP * .025;
                if (ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                    tons += jumpMP * .045;
                } else if (ba.getMovementMode() == EntityMovementMode.VTOL) {
                    tons += jumpMP * .03;
                } else {
                    tons += jumpMP * .025;
                }
                break;
            case EntityWeightClass.WEIGHT_LIGHT:
                if (ba.isClan()) {
                    tons += 0.15;
                } else {
                    tons += 0.1;
                }
                tons += walkMP * .03;
                if (ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                    tons += jumpMP * .045;
                } else if (ba.getMovementMode() == EntityMovementMode.VTOL) {
                    tons += jumpMP * .04;
                } else {
                    tons += jumpMP * .025;
                }
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                if (ba.isClan()) {
                    tons += 0.25;
                } else {
                    tons += 0.175;
                }
                tons += walkMP * .04;
                if (ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                    tons += jumpMP * .085;
                } else if (ba.getMovementMode() == EntityMovementMode.VTOL) {
                    tons += jumpMP * .06;
                } else {
                    tons += jumpMP * .05;
                }
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                if (ba.isClan()) {
                    tons += 0.4;
                } else {
                    tons += 0.3;
                }
                tons += walkMP * .08;
                if (ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                    tons += jumpMP * .16;
                } else {
                    tons += jumpMP * .125;
                }
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                if (ba.isClan()) {
                    tons += 0.7;
                } else {
                    tons += 0.55;
                }
                tons += walkMP * .16;
                tons += jumpMP * .25;
                break;
        }
        return tons;
    }

    @Override
    public float getWeightArmor() {
        return ba.getOArmor(1)
                * EquipmentType.getBaArmorWeightPerPoint(ba.getArmorType(1),
                        TechConstants.isClan(ba.getArmorTechLevel(1)));
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return 0;
    }

    @Override
    public float getWeight() {
        return ba.getTrooperWeight() * ba.getTroopers();
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        return "";
    }

    @Override
    public String printWeightStructure() {
        return StringUtil.makeLength("Structure: ", getPrintSize() + 9)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }

    @Override
    public String printWeightArmor() {
        String armorName = EquipmentType.getArmorTypeName(ba
                .getArmorType(BattleArmor.LOC_SQUAD), TechConstants.isClan(ba
                .getArmorTechLevel(BattleArmor.LOC_SQUAD)));

        return StringUtil.makeLength(
                "Armor: " + Integer.toString(getTotalOArmor()) + " "
                        + armorName, getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightArmor()) + "\n";
    }

    @Override
    public String printArmorPlacement() {
        StringBuffer buff = new StringBuffer();
        buff.append("Armor Placement:\n");
        for (int loc = 1; loc < getEntity().locations(); loc++) {
            buff.append(printArmorLocation(loc)).append("\n");
        }
        return buff.toString();
    }

    @Override
    public String printArmorLocation(int loc) {
        return StringUtil
                .makeLength(getEntity().getLocationAbbr(loc) + ":", 10)
                + StringUtil.makeLength(getEntity().getOInternal(loc), 4)
                + StringUtil.makeLength(getEntity().getOArmor(loc), 6) + "  ";
    }

    /**
     * Checks to see if this unit has valid armor assignment.
     *
     * @param buff
     * @return
     */
    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        int maxArmorPoints = ba.getMaximumArmorPoints();
        for (int loc = 0; loc < ba.locations(); loc++) {
            if (ba.getOArmor(loc) > maxArmorPoints) {
                buff.append(printArmorLocation(loc))
                        .append(printArmorLocProp(loc, maxArmorPoints))
                        .append("\n");
                correct = false;
            }
        }
        return correct;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + Integer.toString(wert) + "!";
    }

    public boolean correctMovement(StringBuffer buff) {
        if (ba.getOriginalWalkMP() > ba.getMaximumWalkMP()) {
            buff.append("Walk MP is " + ba.getOriginalWalkMP()
                    + " but maximum is " + ba.getMaximumWalkMP() + "!");
            return false;
        }

        if (ba.getOriginalJumpMP() > ba.getMaximumJumpMP()) {
            buff.append("Jump MP is " + ba.getOriginalWalkMP()
                    + " but maximum is " + ba.getMaximumWalkMP() + "!");
            return false;
        }

        if (ba.hasWorkingMisc(MiscType.F_JUMP_BOOSTER)
                && ((ba.getMovementMode() != EntityMovementMode.INF_JUMP) || (ba
                        .getJumpMP() < 1))) {
            buff.append("BattleArmor with jump boosters "
                    + "must have jump jets with a least 1MP!");
            return false;
        }

        if (ba.hasWorkingMisc(MiscType.F_PARTIAL_WING)
                && ((ba.getMovementMode() != EntityMovementMode.INF_JUMP) || (ba
                        .getJumpMP() < 1))) {
            buff.append("BattleArmor with a partial wing "
                    + "must have jump jets with a least 1MP!");
            return false;
        }

        if (ba.hasWorkingMisc(MiscType.F_JUMP_BOOSTER)
                && ba.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            buff.append("BattleArmor may not mount a jump booster "
                    + "and a partial wing!");
            return false;
        }

        if (ba.hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)
                && ba.hasMyomerBooster()) {
            buff.append("BattleArmor may not mount a mechanical jump booster "
                    + "and a myomer booster!");
            return false;
        }

        if (ba.hasMyomerBooster()
                && (ba.getArmorType(BattleArmor.LOC_SQUAD) == EquipmentType.T_ARMOR_BA_MIMETIC)) {
            buff.append("BattleArmor may not mount a myomer booster "
                    + "and mimetic armor!");
            return false;
        }

        if (ba.hasMyomerBooster()
                && ((ba.getArmorType(BattleArmor.LOC_SQUAD) == EquipmentType.T_ARMOR_BA_STEALTH)
                        || (ba.getArmorType(BattleArmor.LOC_SQUAD) == EquipmentType.T_ARMOR_BA_STEALTH_BASIC)
                        || (ba.getArmorType(BattleArmor.LOC_SQUAD) == EquipmentType.T_ARMOR_BA_STEALTH_IMP) || (ba
                        .getArmorType(BattleArmor.LOC_SQUAD) == EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE))) {
            buff.append("BattleArmor may not mount a myomer booster "
                    + "and stealth armor!");
            return false;
        }

        if (ba.countWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER) > 1) {
            buff.append("BattleArmor may only mount 1 "
                    + "mechanical jump booster!");
            return false;
        }
        EquipmentType boosterType = EquipmentType.get("CLBAMyomerBooster");
        if (ba.countWorkingMisc(MiscType.F_MASC) > boosterType.getCriticals(ba)) {
            buff.append("BattleArmor may only mount 1 " + "myomer booster!");
            return false;
        }

        return true;
    }

    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        getUnallocatedEquipment(ba, unallocated);
        boolean correct = true;

        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }

        int critsUsed[][] = new int[ba.getTroopers() + 1]
                [BattleArmor.MOUNT_NUM_LOCS];
        int numAPWeapons[][] = new int[ba.getTroopers() + 1]
                [BattleArmor.MOUNT_NUM_LOCS];
        int numAMWeapons[][] = new int[ba.getTroopers() + 1]
                [BattleArmor.MOUNT_NUM_LOCS];
        int numSSWMs = 0;
        int numGloveMountedAPWeapons = 0;
        Mounted squadSupportWeapon = null;
        // Count used crits, AM/AP weaps for each squad member and location
        for (Mounted m : ba.getEquipment()) {
            // BA Tasers should be mounted individually
            if (m.getType().hasFlag(WeaponType.F_TASER)
                    && m.getLocation() == BattleArmor.LOC_SQUAD){
                buff.append("BA Tasers should be mounted individually " +
                        "instead of as a squad weapon!");
                return false;
            }
            
            // BA NARC should be mounted individually
            if ((m.getType() instanceof WeaponType)
                    && ((WeaponType)m.getType()).getAmmoType() 
                        == AmmoType.T_NARC
                    && m.getLocation() == BattleArmor.LOC_SQUAD){
                buff.append("BA NARC should be mounted individually " +
                        "instead of as a squad weapon!");
                return false;
            }
            
            // Ignore unmounted equipment, we'll deal with that elsewhere
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE) {
                continue;
            }

            int critSize;
            if (m.getType().isSpreadable()) {
                critSize = 1;
            } else {
                critSize = m.getType().getCriticals(ba);
            }
            
            // Manipulators don't take up slots in BA
            if (m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                critSize = 0;
            }

            // AP Weapons that are mounted in an AP Mount don't take up slots
            if (m.isAPMMounted() && (m.getLinkedBy() != null)
                    && m.getLinkedBy().getType().hasFlag(MiscType.F_AP_MOUNT)) {
                critSize = 0;
            }
            
            if ((m.getType() instanceof WeaponType) 
                    && m.isSquadSupportWeapon()) {
                numSSWMs++;
                squadSupportWeapon = m;
            }

            // Check for valid BA weapon
            if ((m.getType() instanceof WeaponType)
                    && !m.getType().hasFlag(WeaponType.F_BA_WEAPON)
                    && !m.getType().hasFlag(WeaponType.F_INFANTRY)) {
                buff.append(m.getName() + " is not a BattleArmor weapon!\n");
                correct = false;
            }

            // Special considerations for AP weapons
            if ((m.getType() instanceof WeaponType) 
                    && m.getType().hasFlag(WeaponType.F_INFANTRY)) {
                Mounted link = m.getLinkedBy();
                if (link == null) {
                    correct = false;
                    buff.append(m.getName() + " is an infantry weapon but " +
                            "has no mount point!\n");
                    continue;
                }
                // No AP melee weapons
                if (m.getType().hasFlag(WeaponType.F_INF_POINT_BLANK)) {
                    buff.append(m.getName() + " is a melee AP weapon and " +
                            "BattleArmor cannot mount melee AP weapons!\n");
                    correct = false;   
                }
                // Special considerations for AP mounts
                if (link.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                    if (m.getType().hasFlag(WeaponType.F_INF_SUPPORT)) {
                        buff.append(m.getName() + " is a support weapon and " +
                            "BattleArmor AP mounts cannot mount " +
                            "support weapons!\n");
                        correct = false; 
                    }
                // Special considerations for armored gloves
                } else if (link.getType().hasFlag(MiscType.F_ARMORED_GLOVE)) {
                    if (m.getType().hasFlag(WeaponType.F_INF_SUPPORT)){
                        if ((m.getType() instanceof InfantryWeapon)) {
                            int crew = ((InfantryWeapon)m.getType()).getCrew();
                            if (crew > 1) {
                                buff.append(m.getName()
                                        + " has a crew size of " + crew
                                        + " but size can be no greater "
                                        + "than 1E!\n");
                                correct = false;
                            }
                        } else if (!(m.getType() instanceof InfantryWeapon)) {
                            buff.append(m.getName() + " has the INF_SUPPORT " +
                                    "flag but is not an InfantryWeapon!\n");
                            correct = false;   
                        }
                    }
                }
            }

            // Check for valid BA equipment
            if ((m.getType() instanceof MiscType)
                    && !m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                buff.append(m.getName() + " is not BattleArmor equipment!\n");
                correct = false;
            }

            if ((m.getType() instanceof AmmoType)
                    && (m.getBaseShotsLeft() > NUM_SHOTS_PER_CRIT)) {
                buff.append(m.getName() + "has " + m.getBaseShotsLeft()
                        + " shots, but BattleArmor may only have at most "
                        + NUM_SHOTS_PER_CRIT + " shots per slot.\n");
                correct = false;
            }

            // Ensure that jump boosters are mounted in the body
            if (m.getType().hasFlag(MiscType.F_JUMP_BOOSTER)
                    && (m.getBaMountLoc() != BattleArmor.MOUNT_LOC_BODY)) {
                buff.append("Jump Boosters must be mounted in the body!\n");
            }

            // Ensure partial wing are mounted in the body
            if (m.getType().hasFlag(MiscType.F_PARTIAL_WING)
                    && (m.getBaMountLoc() != BattleArmor.MOUNT_LOC_BODY)) {
                buff.append("Partial wing must be mounted in the body!\n");
            }

            if (m.getLocation() != BattleArmor.LOC_SQUAD) {
                critsUsed[m.getLocation()][m.getBaMountLoc()] += critSize;
                if ((m.getType() instanceof WeaponType)){
                    numAMWeapons[m.getLocation()][m.getBaMountLoc()]++;
                }
                if (m.getType().hasFlag(MiscType.F_AP_MOUNT)){
                    numAPWeapons[m.getLocation()][m.getBaMountLoc()]++;
                }
            } else {
                for (int t = 0; t <= ba.getTroopers(); t++) {
                    critsUsed[t][m.getBaMountLoc()] += critSize;
                    if ((m.getType() instanceof WeaponType) 
                            && !(m.getType() instanceof InfantryWeapon)) {
                        numAMWeapons[t][m.getBaMountLoc()]++;
                    }
                    if (m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                        numAPWeapons[t][m.getBaMountLoc()]++;
                    }
                }
            }
            
            if (m.getType().hasFlag(MiscType.F_ARMORED_GLOVE)) {
                if ((m.getLinked() != null) 
                        && (m.getLinked().getType() instanceof InfantryWeapon)) {
                    numGloveMountedAPWeapons++;
                }
            }
        }
        
        if (numSSWMs > 1){
            buff.append("Squad has " + numSSWMs + " squad support " +
                    "weapon mounts, but only 1 is allowed!\n");
            correct = false;
        }
        
        if (numSSWMs > 0 
                && ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD){
            buff.append("Quad BattleArmor cannot use squad support " +
                    "weapon mounts!\n");
            correct = false;
        }
        
        if (numGloveMountedAPWeapons > 1) {
            buff.append("Batle Armor with armored gloves may only mount 1 " +
                    "additional AP weapon, but " + numGloveMountedAPWeapons 
                    + " are mounted!\n");
            correct = false;
        }
        
        if (squadSupportWeapon != null){
            WeaponType sswType = (WeaponType)squadSupportWeapon.getType();
            for (Mounted ammo : ba.getAmmo()){
                if (ammo.isSquadSupportWeapon() && 
                        !AmmoType.isAmmoValid(ammo, sswType)){
                    buff.append(ammo.getName() + " is squad support weapon " +
                            "mounted, but it is not a legal ammo type for " +
                            "the squad support weapon!\n");
                    correct = false;
                }
            }
        }

        // Now check to make sure the counts are valid
        for (int t = 0; t <= ba.getTroopers(); t++) {
            for (int loc = 0; loc < BattleArmor.MOUNT_NUM_LOCS; loc++) {
                if (critsUsed[t][loc] > ba.getNumCrits(loc)) {
                    buff.append(ba.getBaMountLocAbbr(loc) + " of "
                            + ba.getLocationAbbr(t) + " has "
                            + critsUsed[t][loc]
                            + " used criticals, but only has "
                            + ba.getNumCrits(loc) + " available criticsl!\n");
                    correct = false;
                }
                if (numAMWeapons[t][loc] > 
                        ba.getNumAllowedAntiMechWeapons(loc)) {
                    buff.append(ba.getBaMountLocAbbr(loc) + " of "
                            + ba.getLocationAbbr(t) + " has "
                            + numAMWeapons[t][loc]
                            + " anti-mech weapons, but only "
                            + ba.getNumAllowedAntiMechWeapons(loc)
                            + " are allowed!\n");
                    correct = false;
                }
                if (numAPWeapons[t][loc] > ba
                        .getNumAllowedAntiPersonnelWeapons(loc, t)) {
                    buff.append(ba.getBaMountLocAbbr(loc) + " of "
                            + ba.getLocationAbbr(t) + " has "
                            + numAPWeapons[t][loc]
                            + " anti-personnel weapons, but only "
                            + ba.getNumAllowedAntiPersonnelWeapons(loc, t)
                            + " are allowed!\n");
                    correct = false;
                }
            }
        }

        return correct;
    }

    public void getUnallocatedEquipment(Entity entity,
            Vector<Mounted> unallocated) {
        for (Mounted m : entity.getEquipment()) {
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE) {
                // OS-launcher ammo doesn't take up a slot
                if ((m.getType() instanceof AmmoType)
                        && (m.getLinkedBy() != null)
                        && m.getLinkedBy().getType()
                                .hasFlag(WeaponType.F_ONESHOT)) {
                    continue;
                }

                // Equipment taking up no slots doesn't need to be mounted
                if ((m.getType().getCriticals(entity) == 0) 
                        && !((m.getType() instanceof InfantryWeapon) 
                                && !m.isAPMMounted())) {
                    continue;
                }

                // Weapons mounted in a DWP don't get assigned a location
                if (((m.getLinked() != null) && m.getLinked().getType()
                        .hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))
                        || ((m.getLinkedBy() != null) && m.getLinkedBy()
                                .getType()
                                .hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))) {
                    continue;
                }
                
                // Ammo mounted in a DWP doesn't get assigned a location
                if ((m.getType() instanceof AmmoType) 
                        && (m.getLinkedBy() != null) 
                        && m.getLinkedBy().isDWPMounted() ) {
                    continue;
                }

                // Anything else is unassigned equipment
                unallocated.addElement(m);
            }
        }
    }

    public boolean correctManipulators(StringBuffer buff) {
        boolean correct = true;
        int numLAManipulators = 0;
        int numRAManipulators = 0;
        BAManipulator laManipType = BAManipulator.NONE;
        BAManipulator raManipType = BAManipulator.NONE;
        for (Mounted m : ba.getEquipment()) {
            if (m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                    numLAManipulators++;
                    laManipType = BAManipulator.getManipulator(m.getName());
                } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                    numRAManipulators++;
                    raManipType = BAManipulator.getManipulator(m.getName());
                } else {
                    if (m.getBaMountLoc() != BattleArmor.MOUNT_LOC_NONE) {
                        buff.append(m.getName()
                                + "mounted in "
                                + BattleArmor.MOUNT_LOC_NAMES[m.getBaMountLoc()]
                                + ", but manipulators must be mounted in arms!");
                    } else {
                        buff.append(m.getName()
                                + " not allocated!\n");  
                    }
                    correct = false;
                }
            }
        }

        if (numLAManipulators > 1) {
            buff.append("Found more than 1 manipulator in the left arm!");
            correct = false;
        }

        if (numRAManipulators > 1) {
            buff.append("Found more than 1 manipulator in the right arm!");
            correct = false;
        }

        if ((laManipType.pairMounted || raManipType.pairMounted)
                && (laManipType.type != raManipType.type)) {
            if (laManipType.pairMounted) {
                buff.append("Left Arm manipulator must be mounted as a "
                        + "pair, but the right arm manipulator doesn't match! ");
            } else {
                buff.append("Right Arm manipulator must be mounted as a "
                        + "pair, but the left arm manipulator doesn't match! ");
            }
            correct = false;
        }
        return correct;
    }

    @Override
    public boolean correctWeight(StringBuffer buff, boolean showO, boolean showU) {
        float weightSum = calculateWeight();
        float weight = getWeight();
        boolean correct = true;
        String baDesig = ba.getLocationAbbr(BattleArmor.LOC_SQUAD);
        if (showO && ((weight + getMaxOverweight()) < weightSum)) {
            buff.append(baDesig + "Weight: ").append(calculateWeight())
                    .append(" is greater than ").append(getWeight())
                    .append("\n");
            correct = false;
        }
        if (showU && ((weight - getMinUnderweight()) > weightSum)) {
            buff.append("Weight: ").append(calculateWeight())
                    .append(" is less than ").append(getWeight()).append("\n");
            correct = false;
        }

        for (int t = 1; t < ba.getTroopers(); t++) {
            float trooperWeight = calculateWeight(t);
            if (trooperWeight > ba.getTrooperWeight()) {
                buff.append("Trooper " + t + " Weight: " + trooperWeight
                        + " is greater than " + ba.getTrooperWeight() + "\n");
                correct = false;
            }
        }
        return correct;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }

        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showCorrectCritical() && !correctCriticals(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }

        correct &= correctManipulators(buff);

        correct &= correctMovement(buff);

        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("BattleArmor: ").append(ba.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (")
                    .append(calculateWeight()).append(")\n");
        }
        buff.append(printWeightCalculation()).append("\n");
        buff.append(printArmorPlacement());
        correctArmor(buff);
        buff.append(printLocations());
        correctCriticals(buff);
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String printWeightCalculation() {
        return printWeightStructure() + printWeightArmor() + "Equipment:\n"
                + printWeapon() + printAmmo() + printMiscEquip();
    }

    @Override
    public String getName() {
        return "Battle Armor: " + ba.getDisplayName();
    }

    @Override
    public float getWeightPowerAmp() {
        return 0;
    }

    /**
     * Performs the same functionality as <code>TestEntity.getWeightMiscEquip
     * </code> but only considers equipment mounted on the specified trooper.
     * That is, only misc equipment that is squad mounted or on the specific
     * trooper is added.
     *
     * @param trooper
     * @return
     */
    public float getWeightMiscEquip(int trooper) {
        float weightSum = 0.0f;
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();
            // If this equipment isn't mounted on the squad or this particular
            // trooper, skip it
            if ((m.getLocation() != BattleArmor.LOC_SQUAD)
                    && ((m.getLocation() != trooper) || (trooper == BattleArmor.LOC_SQUAD))) {
                continue;
            }

            // Equipment assigned to this trooper but not mounted shouldn't be
            // counted, unless it's squad-level equipment
            if ((m.getLocation() == trooper) && (trooper != BattleArmor.LOC_SQUAD)
                    && (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE)) {
                continue;
            }

            if (mt.hasFlag(MiscType.F_ENDO_STEEL)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_ENDO_STEEL_PROTO)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_COMPOSITE)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)
                    || mt.hasFlag(MiscType.F_REINFORCED)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)
                    || mt.hasFlag(MiscType.F_FERRO_LAMELLOR)
                    || mt.hasFlag(MiscType.F_LIGHT_FERRO)
                    || mt.hasFlag(MiscType.F_HEAVY_FERRO)
                    || mt.hasFlag(MiscType.F_REACTIVE)
                    || mt.hasFlag(MiscType.F_REFLECTIVE)
                    || mt.hasFlag(MiscType.F_HARDENED_ARMOR)
                    || mt.hasFlag(MiscType.F_PRIMITIVE_ARMOR)
                    || mt.hasFlag(MiscType.F_COMMERCIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_HEAVY_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_ANTI_PENETRATIVE_ABLATIVE)
                    || mt.hasFlag(MiscType.F_HEAT_DISSIPATING)
                    || mt.hasFlag(MiscType.F_IMPACT_RESISTANT)
                    || mt.hasFlag(MiscType.F_BALLISTIC_REINFORCED)
                    || mt.hasFlag(MiscType.F_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                continue;
            }
            weightSum += mt.getTonnage(getEntity(), m.getLocation());
        }
        return weightSum;
    }

    public float getWeightWeapon(int trooper) {
        float weight = 0.0f;
        for (Mounted m : getEntity().getWeaponList()) {
            // If this equipment isn't mounted on the squad or this particular
            // trooper, skip it
            if ((m.getLocation() != BattleArmor.LOC_SQUAD)
                    && ((m.getLocation() != trooper) || (trooper == BattleArmor.LOC_SQUAD))) {
                continue;
            }

            // Equipment assigned to this trooper but not mounted shouldn't be
            // counted, unless it's squad-level equipment
            if ((m.getLocation() == trooper) && (trooper != BattleArmor.LOC_SQUAD)
                    && (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE)) {
                continue;
            }

            WeaponType wt = (WeaponType) m.getType();
            if (m.isDWPMounted()) {
                weight += wt.getTonnage(getEntity()) * 0.75;
            } else if (m.isSquadSupportWeapon()){
                if (ba.isClan()){
                    weight += wt.getTonnage(getEntity()) * 0.4;
                } else {
                    weight += wt.getTonnage(getEntity()) * 0.5;
                }
            } else {
                weight += wt.getTonnage(getEntity());
            }
        }
        // Round weight to prevent odd behavior
        return Math.round(weight*1000) / 1000.0f;
    }

    public float getWeightAmmo(int trooper) {
        float weight = 0.0f;
        for (Mounted m : getEntity().getAmmo()) {

            // If this equipment isn't mounted on the squad or this particular
            // trooper, skip it
            if ((m.getLocation() != BattleArmor.LOC_SQUAD)
                    && ((m.getLocation() != trooper) 
                            || (trooper == BattleArmor.LOC_SQUAD))) {
                continue;
            }

            // Equipment assigned to this trooper but not mounted shouldn't be
            // counted, unless it's squad-level equipment
            if ((m.getLocation() == trooper) 
                    && (trooper != BattleArmor.LOC_SQUAD)
                    && (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE)) {
                continue;
            }
            float modifier = 1;
            // Ammo mounted in a DWP has its weight reduced by 25%
            if (m.getLinkedBy() != null && m.getLinkedBy().isDWPMounted()){
                modifier = 0.75f;
            } else if (m.isSquadSupportWeapon()) {
                if (ba.isClan()){
                    modifier = 0.4f;
                } else {
                    modifier = 0.5f;
                }
            }
            AmmoType mt = (AmmoType) m.getType();
            weight += (mt.getKgPerShot() * m.getBaseShotsLeft()) / 1000.0
                    * modifier;
        }
        return weight;
    }

    /**
     * There are some cases where we need to know the weight of an individual
     * trooper in the BattleArmor squad, this method provides that.
     *
     * @param trooper
     * @return
     */
    public float calculateWeight(int trooper) {
        float weight = 0;
        weight += getWeightStructure();
        weight += getWeightArmor();

        weight += getWeightMiscEquip(trooper);
        weight += getWeightWeapon(trooper);
        weight += getWeightAmmo(trooper);

        // Round weight to prevent odd behavior
        return Math.round(weight*1000) / 1000.0f;
    }

    @Override
    public float calculateWeight() {
        float totalWeight = 0.0f;
        for (int i = 1; i < ba.getTroopers(); i++) {
            totalWeight += calculateWeight(i);
        }
        return totalWeight;
    }

}
