/*
 * Copyright (C) 2018 - The MegaMek Team
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
package megamek.common.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechManager;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;

/**
 * @author Neoancient
 *
 */
public class TestProtomech extends TestEntity {
    
    /**
     * Any protomech with a larger mass than this is ultra-heavy
     */
    public static final double MAX_STD_TONNAGE = 9.0;
    
    /**
     * Maximum weight for a protomech
     */
    public static final double MAX_TONNAGE = 15.0;
    
    public enum ProtomechJumpJets {
        JJ_STANDARD ("ProtomechJumpJet", false),
        JJ_EXTENDED ("ExtendedJumpJetSystem", true),
        JJ_UMU ("ProtomechUMU", false);
        
        private final String internalName;
        private final boolean improved;
        
        private ProtomechJumpJets(String internalName, boolean improved) {
            this.internalName = internalName;
            this.improved = improved;
        }
        
        public String getName() {
            return internalName;
        }
        
        public boolean isImproved() {
            return improved;
        }
        
        public static List<EquipmentType> allJJs() {
            return Arrays.stream(values())
                    .map(jj -> EquipmentType.get(jj.internalName))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * All the protomech armor options. Both of the them.
     *
     */
    public static enum ProtomechArmor {
        STANDARD (EquipmentType.T_ARMOR_STANDARD, true, 0),
        EDP (EquipmentType.T_ARMOR_EDP, true, 1);

        private final int type;
        private final boolean isClan;
        private final int torsoSlots;

        ProtomechArmor(int t, boolean c, int slots) {
            type = t;
            isClan = c;
            torsoSlots = slots;
        }

        public static int armorCount() {
            return values().length;
        }

        /**
         * Given an armor type, return the {@link ProtomechArmor} instance that
         * represents that type.
         *
         * @param t   The armor type.
         * @param c   Whether this armor type is Clan or not.
         * @return    The {@link ProtomechArmor} that corresponds to the given type
         *            or null if no match was found.
         */
        public static @Nullable ProtomechArmor getArmor(Protomech proto) {
            return getArmor(proto.getArmorType(Protomech.LOC_TORSO),
                    TechConstants.isClan(proto.getArmorTechLevel(Protomech.LOC_TORSO)));
        }

        /**
         * Given an armor type, return the {@link ProtomechArmor} instance that
         * represents that type.
         *
         * @param t   The armor type.
         * @param c   Whether this armor type is Clan or not.
         * @return    The {@link ProtomechArmor} that corresponds to the given type
         *            or null if no match was found.
         */
        public static @Nullable ProtomechArmor getArmor(int t, boolean c) {
            for (ProtomechArmor a : values()) {
                if ((a.type == t) && (a.isClan == c)) {
                    return a;
                }
            }
            return null;
        }
        
        /**
         * @return The {@link MiscType} for this armor.
         */
        public EquipmentType getArmorEqType() {
            String name = EquipmentType.getArmorTypeName(type, isClan);
            return EquipmentType.get(name);
        }
        
        public int getType() {
            return type;
        }
        
        public boolean isClan() {
            return isClan;
        }
        
        public int getTorsoSlots() {
            return torsoSlots;
        }
        
        public double getWtPerPoint() {
            return EquipmentType.getProtomechArmorWeightPerPoint(type);
        }
    }

    /**
     * Filters all protomech armor according to given tech constraints
     * 
     * @param techManager
     * @return A list of all armors that meet the tech constraints
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (ProtomechArmor armor : ProtomechArmor.values()) {
            final EquipmentType eq = armor.getArmorEqType();
            if ((null != eq) && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
        }
        return retVal;
    }
        
    private final Protomech proto;
    private final String fileString;

    public TestProtomech(Protomech proto, TestEntityOption option, String fileString) {
        super(option, proto.getEngine(), getArmor(proto), null);
        this.proto = proto;
        this.fileString = fileString;
    }

    private static Armor[] getArmor(Protomech proto) {
        Armor[] armor = new Armor[proto.locations()];
        for (int i = 0; i < proto.locations(); i++) {
            int type = proto.getArmorType(i);
            int flag = 0;
            if (proto.isClanArmor(i)) {
                flag |= Armor.CLAN_ARMOR;
            }
            armor[i] = new Armor(type, flag);
        }
        return armor;
    }
    
    @Override
    public Entity getEntity() {
        return proto;
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
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }
    
    @Override
    public boolean isProtomech() {
        return true;
    }
    
    @Override
    public double getWeightStructure() {
        return round(proto.getWeight() * 0.1, Ceil.KILO);
    }
    
    @Override
    public double getWeightEngine() {
        return proto.getEngine().getWeightEngine(proto, Ceil.KILO);
    }

    @Override
    public double getWeightControls() {
        return (proto.getWeight() > MAX_STD_TONNAGE)? 0.75 : 0.5;
    }

    @Override
    public double getWeightMisc() {
        return 0;
    }

    @Override
    public double getWeightHeatSinks() {
        return getCountHeatSinks() * 0.25;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return heatNeutralHSRequirement();
    }
    
    @Override
    public double calculateWeight() {
        // Deal with some floating point precision issues
        return round(super.calculateWeight(), Ceil.KILO);
    }
    
    @Override
    public double getWeightAllocatedArmor() {
        ProtomechArmor armor = ProtomechArmor.getArmor(proto);
        double wtPerPoint = 0.0;
        if (null != armor) {
            wtPerPoint = armor.getWtPerPoint();
        }
        return proto.getTotalArmor() * wtPerPoint;
    }

    @Override
    public String printWeightStructure() {
        return StringUtil.makeLength(
                "Structure: "
                        + Integer.toString(getEntity().getTotalOInternal()), getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Controls:", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightControls()));
        retVal.append("\n");
        return retVal.toString();
    }

    @Override
    public StringBuffer printMiscEquip(StringBuffer buff, int posLoc,
            int posWeight) {
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20)).append(
                    TestEntity.makeWeightString(round(mt.getTonnage(getEntity()), Ceil.KILO)));
            buff.append("\n");
        }
        return buff;
    }

    @Override
    public StringBuffer printWeapon(StringBuffer buff, int posLoc, int posWeight) {
        for (Mounted m : getEntity().getWeaponList()) {
            WeaponType mt = (WeaponType) m.getType();

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20))
                    .append(TestEntity.makeWeightString(round(mt
                            .getTonnage(getEntity()), Ceil.KILO))).append("\n");
        }
        return buff;
    }

    @Override
    public StringBuffer printAmmo(StringBuffer buff, int posLoc, int posWeight) {
        for (Mounted m : getEntity().getAmmo()) {
            AmmoType mt = (AmmoType) m.getType();

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(" ").append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20))
                    .append(TestEntity.makeWeightString(
                            Math.ceil(mt.getKgPerShot() * m.getBaseShotsLeft()) / 1000.0)).append("\n");
        }
        return buff;
    }

    @Override
    public double getWeightCarryingSpace() {
        return 0.0;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        if (skip()) {
            return correct;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        correct = correct && correctMovement(buff);
        return correct;
    }
    
    @Override
    public boolean correctWeight(StringBuffer buff) {
        boolean correct = super.correctWeight(buff);
        if (proto.getWeight() > MAX_TONNAGE) {
            buff.append("Exceeds maximum weight of ").append(MAX_TONNAGE).append("\n");
            correct = false;
        }
        return correct;
    }
    
    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = false;
        Map<Integer, Integer> slotsByLoc = new HashMap<>();
        Map<Integer, Double> weightByLoc = new HashMap<>();
        for (Mounted mount : proto.getEquipment()) {
            if (!requiresSlot(mount.getType())) {
                continue;
            }
            slotsByLoc.merge(mount.getLocation(), 1, Integer::sum);
            weightByLoc.merge(mount.getLocation(),
                    mount.getType().getTonnage(proto, mount.getLocation()), Double::sum);
            if (mount.isRearMounted() && (mount.getLocation() != Protomech.LOC_TORSO)) {
                buff.append("Equipment can only be rear-mounted on the torso\n");
                illegal = true;
            }
            if ((mount.getType() instanceof WeaponType)
                    && !mount.getType().hasFlag(WeaponType.F_PROTO_WEAPON)) {
                buff.append(mount.toString()).append(" is not a legal protomech weapon.\n");
                illegal = true;
            } else if ((mount.getType() instanceof MiscType)
                    && !mount.getType().hasFlag(MiscType.F_PROTOMECH_EQUIPMENT)) {
                buff.append(mount.toString()).append(" is not legal protomech equipment.\n");
                illegal = true;
            }
            if ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                if (proto.isGlider() || proto.isQuad()) {
                    buff.append("Quad and glider protomechs cannot use a magnetic clamp system.\n");
                    illegal = true;
                }
                if (mount.getLocation() != Protomech.LOC_TORSO) {
                    buff.append("The magnetic clamp system must be mounted in the torso.\n");
                    illegal = true;
                }
            }
            if ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_PROTOQMS)) {
                if (!proto.isQuad()) {
                    buff.append(mount.getType().getName() + "can only be used by quad protomechs.\n");
                    illegal = true;
                }
                if (mount.getLocation() != Protomech.LOC_TORSO) {
                    buff.append(mount.getType().getName() + " must be mounted in the torso.\n");
                    illegal = true;
                }
            }
        }
        ProtomechArmor armor = ProtomechArmor.getArmor(proto);
        if (null == armor) {
            buff.append("Does not have legal armor type.\n");
            illegal = true;
        } else {
            slotsByLoc.merge(Protomech.LOC_TORSO, armor.getTorsoSlots(), Integer::sum);
        }
        for (int loc = 0; loc < proto.locations(); loc++) {
            if (slotsByLoc.getOrDefault(loc, 0) > maxSlotsByLocation(loc, proto)) {
                buff.append("Exceeds ").append(maxSlotsByLocation(loc, proto))
                    .append(" slot limit in ").append(proto.getLocationName(loc)).append("\n");
                illegal = true;
            }
            if (weightByLoc.getOrDefault(loc, 0.0) > maxWeightByLocation(loc, proto)) {
                buff.append("Exceeds ").append(maxWeightByLocation(loc, proto) * 1000)
                    .append(" kg limit in ").append(proto.getLocationName(loc)).append("\n");
                illegal = true;
            }
        }
        if (proto.isGlider() && proto.isQuad()) {
            buff.append("Glider protomechs cannot be quads.\n");
            illegal = true;
        }
        
        return illegal;
    }
    
    /**
     * Checks for exceeding the maximum number of armor points for the tonnage.
     * 
     * @param buffer  A string buffer for appending error messages.
     * @return        Whether the number of armor points is legal
     */
    public boolean correctArmor(StringBuffer buffer) {
        if (proto.getTotalArmor() > maxArmorFactor(proto)) {
            buffer.append("Exceeds maximum of ").append(maxArmorFactor(proto)).append(" armor points.\n");
            return false;
        }
        return true;
    }
    
    /**
     * Checks whether the protomech meets the minimum MP requirements for the configuration.
     * 
     * @param buffer A buffer for error messages
     * @return       Whether the MP is legal.
     */
    public boolean correctMovement(StringBuffer buffer) {
        if (proto.isGlider()
                && proto.getOriginalWalkMP() < 4) {
            buffer.append("Glider protomechs have a minimum cruising MP of 4.\n");
            return false;
        } else if (proto.isQuad()
                && proto.getOriginalWalkMP() < 3) {
            buffer.append("Quad protomechs have a minimum walk MP of 3.\n");
            return false;
        }
        return true;
    }
    

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Protomech: ").append(proto.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(proto.getYear());
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
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Protomech: " + proto.getDisplayName();
    }

    @Override
    public double getWeightAmmo() {
        double weight = 0.0;
        for (Mounted m : getEntity().getAmmo()) {
            AmmoType mt = (AmmoType) m.getType();
            weight += Math.ceil(mt.getKgPerShot() * m.getBaseShotsLeft()) / 1000.0;
        }
        return weight;
    }

    @Override
    public double getWeightPowerAmp() {
        return 0;
    }
    
    /**
     * Determine the minimum walk MP for the protomech based on configuration
     * 
     * @param proto The protomech
     * @return      The minimum walk MP
     */
    public int getMinimumWalkMP(Protomech proto) {
        if (proto.isGlider()) {
            return 4;
        } else if (proto.isQuad()) {
            return 3;
        } else {
            return 1;
        }
    }
    
    /**
     * Computes the required engine rating
     * 
     * @param proto The protomech
     * @return      The engine rating required for the weight, speed, and configuration
     */
    public static int calcEngineRating(Protomech proto) {
        int moveFactor = (int) Math.ceil(proto.getOriginalWalkMP() * 1.5);
        // More efficient engine use for gliders and quads
        if (proto.isGlider() || proto.isQuad()) {
            moveFactor -= 2;
        }
        return Math.max(1, (int)(moveFactor * proto.getWeight()));
    }
    
    /**
     * Determines whether a piece of equipment counts toward the slot and weight limits of a location.
     * 
     * @param etype The equipment
     * @return      Whether the equipment takes a slot.
     */
    public static boolean requiresSlot(EquipmentType etype) {
        if (etype instanceof AmmoType) {
            return false;
        }
        if (etype instanceof MiscType) {
            return !(etype.hasFlag(MiscType.F_MASC)
                || etype.hasFlag(MiscType.F_UMU)
                || etype.hasFlag(MiscType.F_JUMP_JET));
        }
        return true;
    }
    
    /**
     * Equipment slot limit by location
     * 
     * @param loc   The Protomech location
     * @param proto The Protomech
     * @return      The number of equipment slots in the location
     */
    public static int maxSlotsByLocation(int loc, Protomech proto) {
        return maxSlotsByLocation(loc, proto.isQuad(), proto.getWeight() > MAX_STD_TONNAGE);
    }

    /**
     * Equipment slot limit by location
     * 
     * @param loc   The Protomech location
     * @param quad  Whether the protomech is a quad
     * @param ultra Whether the protomech is ultraheavy
     * @return      The number of equipment slots in the location
     */
    public static int maxSlotsByLocation(int loc, boolean quad, boolean ultra) {
        switch(loc) {
            case Protomech.LOC_TORSO: {
                int slots = 2;
                if (ultra) {
                    slots++;
                }
                if (quad) {
                    slots += slots;
                }
                return slots;
            }
            case Protomech.LOC_LARM:
            case Protomech.LOC_RARM:
                return quad? 0 : 1;
            case Protomech.LOC_MAINGUN:
                return (quad && ultra)? 2 : 1;
            case Protomech.LOC_HEAD:
            case Protomech.LOC_LEG:
            default:
                return 0;
        }
    }
    
    /**
     * The maximum total weight that can be mounted in a given location.
     * 
     * @param loc   The Protomech location
     * @param proto The Protomech
     * @return      The weight limit for that location, in tons.
     */
    public static double maxWeightByLocation(int loc, Protomech proto) {
        return maxWeightByLocation(loc, proto.isQuad(), proto.getWeight() > MAX_STD_TONNAGE);
    }
    
    /**
     * The maximum total weight that can be mounted in a given location.
     * 
     * @param loc   The Protomech location
     * @param quad  Whether the protomech is a quad
     * @param ultra Whether the protomech is ultraheavy
     * @return      The weight limit for that location, in tons.
     */
    public static double maxWeightByLocation(int loc, boolean quad, boolean ultra) {
        switch(loc) {
            case Protomech.LOC_TORSO:
                if (quad) {
                    return ultra? 8.0 : 5.0;
                } else {
                    return ultra? 4.0 : 2.0;
                }
            case Protomech.LOC_LARM:
            case Protomech.LOC_RARM:
                if (quad) {
                    return 0;
                }
                return ultra? 1.0 : 0.5;
            case Protomech.LOC_MAINGUN:
                return Double.MAX_VALUE;
            case Protomech.LOC_HEAD:
            case Protomech.LOC_LEG:
            default:
                return 0.0;
        }
    }
    
    private static final int MAX_ARMOR_FACTOR[] = { 15, 17, 22, 24, 33, 35, 40, 42, 51, 53, 58, 60, 65, 67 };
    
    /**
     * Calculate the maximum armor factor based on weight and whether there is a main gun location
     * 
     * @param proto   The protomech
     * @return        The maximum total number of armor points
     */
    public static int maxArmorFactor(Protomech proto) {
        return maxArmorFactor(proto.getWeight(), proto.hasMainGun());
    }
    
    /**
     * Calculate the maximum armor factor based on weight and whether there is a main gun location
     * 
     * @param weight  The weight of the protomech in tons
     * @param mainGun Whether the protomech has a main gun location
     * @return        The maximum total number of armor points
     */
    public static int maxArmorFactor(double weight, boolean mainGun) {
        final int weightIndex = Math.max(0, (int) weight - 2);
        int base = MAX_ARMOR_FACTOR[Math.min(weightIndex, MAX_ARMOR_FACTOR.length - 1)];
        if (mainGun) {
            return base + ((weight > MAX_STD_TONNAGE)? 6 : 3);
        }
        return base;
    }
}
