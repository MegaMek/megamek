/**
 * 
 */
package megamek.common.verifier;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechManager;
import megamek.common.Mounted;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.util.StringUtil;

/**
 * Class for testing and validating instantiations for Small Craft and Dropships.
 *
 * @author Neoancient
 *
 */
public class TestSmallCraft extends TestAero {
    
    // Indices used to specify firing arcs with aliases for aerodyne and spheroid
    public static final int ARC_NOSE = SmallCraft.LOC_NOSE;
    public static final int ARC_LWING = SmallCraft.LOC_LWING;
    public static final int ARC_RWING = SmallCraft.LOC_RWING;
    public static final int ARC_AFT = SmallCraft.LOC_AFT;
    public static final int ARC_FWD_LEFT = SmallCraft.LOC_LWING;
    public static final int ARC_FWD_RIGHT = SmallCraft.LOC_RWING;
    public static final int ARC_AFT_LEFT = SmallCraft.LOC_LWING + 3;
    public static final int ARC_AFT_RIGHT = SmallCraft.LOC_RWING + 3;
    
    private final SmallCraft smallCraft;

    public static enum AerospaceArmor{
        STANDARD(EquipmentType.T_ARMOR_AEROSPACE, false),   
        CLAN_STANDARD(EquipmentType.T_ARMOR_AEROSPACE, true),
        IS_FERRO_ALUM(EquipmentType.T_ARMOR_ALUM, false),
        CLAN_FERRO_ALUM(EquipmentType.T_ARMOR_ALUM, true),
        FERRO_PROTO(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO, false),        
        HEAVY_FERRO_ALUM(EquipmentType.T_ARMOR_HEAVY_ALUM, false),
        LIGHT_FERRO_ALUM(EquipmentType.T_ARMOR_LIGHT_ALUM, false),
        PRIMITIVE(EquipmentType.T_ARMOR_LC_PRIMITIVE_AERO, false);        

        /**
         * The type, corresponding to types defined in 
         * <code>EquipmentType</code>.
         */
        public int type;
                
        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;
        
        AerospaceArmor(int t, boolean c){
            type = t;
            isClan = c;
        }
        
        /**
         * Given an armor type, return the <code>AerospaceArmor</code> instance that
         * represents that type.
         * 
         * @param t  The armor type.
         * @param c  Whether this armor type is Clan or not.
         * @return   The <code>AeroArmor</code> that corresponds to the given 
         *              type or null if no match was found.
         */
        public static AerospaceArmor getArmor(int t, boolean c){
            for (AerospaceArmor a : values()){
                if (a.type == t && a.isClan == c){
                    return a;
                }
            }
            return null;
        }
        
        /**
         * Calculates and returns the points per ton of the armor type given the
         * weight and shape of a small craft/dropship
         * 
         * @param sc The small craft/dropship
         * @return   The number of points of armor per ton
         */
        public double pointsPerTon(SmallCraft sc) {
            return SmallCraft.armorPointsPerTon(sc.getWeight(), sc.isSpheroid(), type, isClan);
        }
        
        /**
         * @return The <code>MiscType</code> for this armor.
         */
        public EquipmentType getArmorEqType() {
            String name = EquipmentType.getArmorTypeName(type, isClan);
            return EquipmentType.get(name);
        }
    }

    /**
     * Filters all small craft/dropship armor according to given tech constraints
     * 
     * @param techManager
     * @return A list of all armors that meet the tech constraints
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (AerospaceArmor armor : AerospaceArmor.values()) {
            final EquipmentType eq = armor.getArmorEqType();
            if ((null != eq) && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
        }
        return retVal;
    }
    
    /**
     * Defines how many spaces each arc has for weapons. More can be added by increasing weight
     * of master fire control systems.
     */
    public static int SLOTS_PER_ARC = 12;
    
    public static int maxArmorPoints(SmallCraft sc) {
        AerospaceArmor a = AerospaceArmor.getArmor(sc.getArmorType(0),
                TechConstants.isClan(sc.getArmorTechLevel(0)));
        if (null != a) {
            return (int)Math.floor(a.pointsPerTon(sc) * maxArmorWeight(sc))
                    + sc.get0SI() * 4;
        } else {
            return 0;
        }
    }
    
    /**
     *  Computes the maximum number armor level in tons
     *   
     */
    public static double maxArmorWeight(SmallCraft smallCraft){
        if (smallCraft.isSpheroid()) {
            return floor(smallCraft.get0SI() * 3.6, Ceil.HALFTON);
        } else {
            return floor(smallCraft.get0SI() * 4.5, Ceil.HALFTON);
        }
    }
    
    /**
     * Computes the amount of weight required for fire control systems and power distribution
     * systems for exceeding the base limit of weapons per firing arc.
     * 
     * Spheroid aft side arcs are implemented as rear-mounted; the return value uses the index
     * of forward side + 3 for the aft side arcs.
     * 
     * @param sc The small craft/dropship in question
     * @return   Returns a <code>double</code> array, where each element corresponds to a 
     *           location and the value is the extra tonnage required by exceeding the base
     *           allotment
     */
    public static double[] extraSlotCost(SmallCraft sc) {
        int arcs = sc.isSpheroid()? 6 : 4;
        int weaponsPerArc[] = new int[arcs];
        double weaponTonnage[] = new double[arcs];

        for (Mounted m : sc.getEquipment()) {
            if (usesWeaponSlot(m.getType())) {
                int arc = m.getLocation();
                if (arc < 0) {
                    continue;
                }
                if (sc.isSpheroid() && m.isRearMounted()) {
                    arc += 3;
                }
                weaponsPerArc[arc]++;
                weaponTonnage[arc] += m.getType().getTonnage(sc);
            }
        }
        double retVal[] = new double[arcs];
        for (int arc = 0; arc < arcs; arc++) {
            int excess = (weaponsPerArc[arc] - 1) / SLOTS_PER_ARC;
            if (excess > 0) {
                retVal[arc] = ceil(excess * weaponTonnage[arc] / 10.0, Ceil.HALFTON);
            }
        }
        return retVal;
    }
    
    /**
     * Computes the weight of the engine.
     * 
     * @param clan          Whether the unit is a Clan design
     * @param tonnage       The weight of the unit
     * @param desiredSafeThrust  The safe thrust value
     * @param dropship      Whether the unit is a dropship (only relevant for primitives)
     * @param year          The original construction year (only relevant for primitives)
     * @return              The weight of the engine in tons
     */
    public static double calculateEngineTonnage(boolean clan, double tonnage, 
            int desiredSafeThrust, boolean dropship, int year) {
        double multiplier = clan? 0.061 : 0.065;
        return ceil(tonnage * desiredSafeThrust * multiplier, Ceil.HALFTON);
    }
    
    public static int weightFreeHeatSinks(SmallCraft sc) {
        double engineTonnage = calculateEngineTonnage(sc.isClan(), sc.getWeight(), sc.getWalkMP(),
                sc.hasETypeFlag(Entity.ETYPE_DROPSHIP), sc.getOriginalBuildYear());
        if (sc.isSpheroid()) {
            if (sc.isPrimitive()) {
                return (int)Math.floor(Math.sqrt(engineTonnage * 1.3));
            } else if (sc.getDesignType() == SmallCraft.MILITARY) {
                return (int)Math.floor(Math.sqrt(engineTonnage * 6.8));
            } else {
                return (int)Math.floor(Math.sqrt(engineTonnage * 1.6));
            }
        } else {
            if (sc.isPrimitive()) {
                return (int)Math.floor(engineTonnage / 75.0);
            } else if (sc.getDesignType() == SmallCraft.MILITARY) {
                return (int)Math.floor(engineTonnage / 20.0);
            } else {
                return (int)Math.floor(engineTonnage / 60.0);
            }
        }
    }
    
    public static double SmallCraftEngineMultiplier(int year) {
        if (year >= 2500) {
            return 0.065;
        } else if (year >= 2400) {
            return 0.078;
        } else if (year >= 2300) {
            return 0.091;
        } else if (year >= 2251) {
            return 0.0975;
        } else if (year >= 2201) {
            return 0.1105;
        } else if (year >= 2151) {
            return 0.1235;
        } else {
            return 0.143;
        }
    }
    
    public static double SmallCraftControlMultiplier(int year) {
        if (year >= 2500) {
            return 0.0075;
        } else if (year >= 2400) {
            return 0.00825;
        } else if (year >= 2300) {
            return 0.00975;
        } else if (year >= 2251) {
            return 0.01125;
        } else if (year >= 2201) {
            return 0.01275;
        } else if (year >= 2151) {
            return 0.01245;
        } else {
            return 0.01575;
        }
    }
    
    public static double DropshipEngineMultiplier(int year) {
        if (year >= 2500) {
            return 0.065;
        } else if (year >= 2400) {
            return 0.0715;
        } else if (year >= 2351) {
            return 0.0845;
        } else if (year >= 2251) {
            return 0.091;
        } else if (year >= 2201) {
            return 0.1104;
        } else if (year >= 2151) {
            return 0.117;
        } else {
            return 0.13;
        }
    }
    
    public static double DropshipControlMultiplier(int year) {
        if (year >= 2500) {
            return 0.0075;
        } else if (year >= 2400) {
            return 0.009;
        } else if (year >= 2351) {
            return 0.00975;
        } else if (year >= 2251) {
            return 0.0105;
        } else if (year >= 2201) {
            return 0.0120;
        } else if (year >= 2151) {
            return 0.0135;
        } else {
            return 0.015;
        }
    }
    
    public TestSmallCraft(SmallCraft sc, TestEntityOption option, String fs) {
        super(sc, option, fs);
        
        smallCraft = sc;
    }

    @Override
    public Entity getEntity() {
        return smallCraft;
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
        return true;
    }
    
    @Override
    public boolean isSmallCraft() {
        return true;
    }

    @Override
    public double getWeightControls() {
        // Non primitives use the multiplier for 2500+ even if they were built before that date
        int year = smallCraft.isPrimitive()? smallCraft.getOriginalBuildYear() : 2500;
        // Small craft round up to the half ton and dropships to the full ton
        if (smallCraft.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            return ceil(smallCraft.getWeight()
                    * DropshipControlMultiplier(year), Ceil.TON);
        } else {
            return ceil(smallCraft.getWeight()
                    * SmallCraftControlMultiplier(year), Ceil.HALFTON);
        }
    }

    public double getWeightEngine() {
        return calculateEngineTonnage(smallCraft.isClan(), smallCraft.getWeight(),
                smallCraft.getOriginalWalkMP(), smallCraft.hasETypeFlag(Entity.ETYPE_DROPSHIP),
                smallCraft.getOriginalBuildYear());
    }
    
    public double getWeightFuel() {
        // Add 2% for pumps and round up to the half ton
        return ceil(smallCraft.getFuelTonnage() * 1.02, Ceil.HALFTON);
    }

    @Override
    public int getCountHeatSinks() {
        return smallCraft.getHeatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        return Math.max(smallCraft.getHeatSinks() - weightFreeHeatSinks(smallCraft), 0);        
    }

    // Bays can store multiple tons of ammo in a single slot.
    @Override
    public double getWeightAmmo() {
        double weight = 0.0;
        for (Mounted m : getEntity().getAmmo()) {

            // One Shot Ammo
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            AmmoType mt = (AmmoType) m.getType();
            int slots = (int)Math.ceil(m.getBaseShotsLeft() / mt.getShots());
            weight += mt.getTonnage(getEntity()) * slots;
        }
        return weight;
    }

    @Override
    public double getWeightMisc() {
        double weight = 0.0;
        // Add in extra fire control system weight for exceeding base slot limit
        for (double extra : extraSlotCost(smallCraft)) {
            weight += extra;
        }
        return weight;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return smallCraft.getHeatType() == Aero.HEAT_DOUBLE;
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Control Systems:", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightControls()));
        retVal.append("\n");
        return retVal.toString();
    }
        
    public String printWeightFuel() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Fuel: ", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightFuel()));
        retVal.append("\n");
        return retVal.toString();
    }

    public Aero getAero() {
        return smallCraft;
    }
    
    public SmallCraft getSmallCraft() {
        return smallCraft;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + Integer.toString(wert) + "!";
    }

    /**
     * Checks to see if this unit has valid armor assignment.
     * 
     * @param buff
     * @return
     */
    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        double maxArmor = maxArmorWeight(smallCraft);
        if (smallCraft.getLabArmorTonnage() > maxArmor) {
            buff.append("Total armor," + smallCraft.getLabArmorTonnage() + 
                    " tons, is greater than the maximum: " + maxArmor);
            correct = false;
        }

        return correct ;
    }
    
    /**
     * Checks that the heatsink type is a legal value.
     * 
     * @param buff
     * @return
     */
    public boolean correctHeatSinks(StringBuffer buff) {
        if ((smallCraft.getHeatType() != Aero.HEAT_SINGLE) 
                && (smallCraft.getHeatType() != Aero.HEAT_DOUBLE)) {
            buff.append("Invalid heatsink type!  Valid types are "
                    + Aero.HEAT_SINGLE + " and " + Aero.HEAT_DOUBLE
                    + ".  Found " + smallCraft.getHeatType() + ".");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, smallCraft.getTechLevel());
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
        if (getCountHeatSinks() < weightFreeHeatSinks(smallCraft)) {
            buff.append("Heat Sinks:\n");
            buff.append(" Total     " + getCountHeatSinks() + "\n");
            buff.append(" Required  " + weightFreeHeatSinks(smallCraft)
                    + "\n");
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
        
        correct &= correctControlSystems(buff);
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        correct &= !hasIllegalEquipmentCombinations(buff);
        correct &= correctHeatSinks(buff);
        
        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Small Craft/Dropship: ").append(smallCraft.getDisplayName()).append("\n");
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

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }
    
    @Override
    public double calculateWeight() {
        double weight = 0;
        weight += getWeightStructure();
        weight += getWeightEngine();
        weight += getWeightControls();
        weight += getWeightFuel();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightMisc();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();

        weight += getWeightCarryingSpace();

        return weight;
    }

    @Override
    public String printWeightCalculation() {
        return printWeightEngine()
                + printWeightControls() + printWeightFuel() 
                + printWeightHeatSinks()
                + printWeightArmor() + printWeightMisc()
                + printWeightCarryingSpace() + "Equipment:\n"
                + printMiscEquip() + printWeapon() + printAmmo();
    }
    
    @Override
    public String printLocations() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < getEntity().locations(); i++) {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName + ":");
            buff.append("\n");
            for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null) {
                    j = getEntity().getNumberOfCriticals(i);                    
                } else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                        buff.append(Integer.toString(j)
                                + ". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    EquipmentType e = getEntity().getEquipmentType(slot);
                    buff.append(Integer.toString(j) + ". "
                            + e.getInternalName());
                    buff.append("\n");
                }
            }
        }
        return buff.toString();
    }
    

    @Override
    public String getName() {
        if (smallCraft.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            return "Dropship: " + smallCraft.getDisplayName();
        } else {
            return "Small Craft: " + smallCraft.getDisplayName();
        }
    }


}
