/**
 * 
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.EntityWeightClass;

/**
 * Campaign Operations rules for force generation.
 * 
 * @author Neoancient
 *
 */
public class ForceType {
    
    public enum UnitRole {
        UNDETERMINED (false),
        AMBUSHER (true),
        BRAWLER (true),
        JUGGERNAUT (true),
        MISSILE_BOAT (true),
        SCOUT (true),
        SKIRMISHER (true),
        SNIPER (true),
        STRIKER (true),
        ATTACK_FIGHTER (false),
        DOGFIGHTER (false),
        FAST_DOGFIGHTER (false),
        FIRE_SUPPORT (false),
        INTERCEPTOR (false),
        TRANSPORT (false);
        
        private boolean ground;
        
        UnitRole(boolean ground) {
            this.ground = ground;
        }
        
        public boolean isGroundRole() {
            return ground;
        }
        
        public static UnitRole parseRole(String role) {
            switch (role.toLowerCase()) {
            case "ambusher":
                return AMBUSHER;
            case "brawler":
                return BRAWLER;
            case "juggernaut":
                return JUGGERNAUT;
            case "missile_boat":
            case "missile boat":
                return MISSILE_BOAT;
            case "scout":
                return SCOUT;
            case "skirmisher":
                return SKIRMISHER;
            case "sniper":
                return SNIPER;
            case "striker":
                return STRIKER;
            case "attack_fighter":
            case "attack figher":
            case "attack":
                return ATTACK_FIGHTER;
            case "dogfighter":
                return DOGFIGHTER;
            case "fast_dogfighter":
            case "fast dogfighter":
                return FAST_DOGFIGHTER;
            case "fire_support":
            case "fire support":
            case "fire-support":
                return FIRE_SUPPORT;
            case "interceptor":
                return INTERCEPTOR;
            case "transport":
                return TRANSPORT;
            default:
                System.err.println("Could not parse AS Role " + role);
                return UNDETERMINED;
            }
        }        
    };
    
    private static HashMap<String,ForceType> allForceTypes = new HashMap<>();
    public static ForceType getForceType(String key) {
        return allForceTypes.get(key);
    }
    
    private String name = "Support";
    private UnitRole idealRole = UnitRole.UNDETERMINED;
    private int minWeightClass = 0;
    private int maxWeightClass = EntityWeightClass.SIZE;
    private List<Predicate<ModelRecord>> mainCriteria = new ArrayList<>();
    private List<Constraint> otherCriteria = new ArrayList<>();
    
    public String getName() {
        return name;
    }
    
    public UnitRole getIdealRole() {
        return idealRole;
    }

    public int getMinWeightClass() {
        return minWeightClass;
    }

    public int getMaxWeightClass() {
        return maxWeightClass;
    }

    public List<Predicate<ModelRecord>> getMainCriteria() {
        return mainCriteria;
    }

    public List<Constraint> getOtherCriteria() {
        return otherCriteria;
    }
    
    public static void createForceTypes() {
        allForceTypes.clear();
        createAssaultLance();
    }
    
    private static void createAssaultLance() {
        ForceType ft = new ForceType();
        ft.name = "Assault Lance";
        ft.idealRole = UnitRole.JUGGERNAUT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria.add(mr -> mr.getMechSummary().getTotalArmor() >= 135);
        ft.otherCriteria.add(new CountConstraint(3,
                mr -> mr.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY));
        ft.otherCriteria.add(new PercentConstraint(0.75,
                mr -> mr.damageAtRange(7) >= 25));
        //FIXME: The actual requirement is one juggernaut or two snipers; there needs to be
        // a way to combine constraints with ||.
        ft.otherCriteria.add(new CountConstraint(2,
                mr -> mr.getUnitRole().equals(UnitRole.JUGGERNAUT)
                    || mr.getUnitRole().equals(UnitRole.SNIPER)));
    }

    /**
     * base class for limitations on force type 
     */
    public static abstract class Constraint {
        double minFraction = 0.0;
        double maxFraction = 1.0;
        
        Predicate<ModelRecord> criterion;
        
        Constraint(Predicate<ModelRecord> criterion) {
            this.criterion = criterion;
        }
        
        public abstract int getMinimum(int unitSize);
        public abstract int getMaximum(int unitSize);
        
        public boolean fits(ModelRecord mRec) {
            return criterion.test(mRec);
        }
    }
    
    public static class CountConstraint extends Constraint {
        int minCount;
        int maxCount;
        
        public CountConstraint(int min, int max, Predicate<ModelRecord> criterion) {
            super(criterion);
            minCount = min;
            maxCount = max;
        }
        
        public CountConstraint(int min, Predicate<ModelRecord> criterion) {
            this(min, Integer.MAX_VALUE, criterion);
        }
        
        @Override
        public int getMinimum(int unitSize) {
            return minCount;
        }
        
        @Override
        public int getMaximum(int unitSize) {
            return maxCount;
        }
    }
    
    public static class PercentConstraint extends Constraint {
        double minPct;
        double maxPct;
        
        public PercentConstraint(double min, double max, Predicate<ModelRecord> criterion) {
            super(criterion);
            minPct = min;
            maxPct = max;
        }
        
        public PercentConstraint(double min, Predicate<ModelRecord> criterion) {
            this(min, 1.0, criterion);
        }
        
        @Override
        public int getMinimum(int unitSize) {
            return (int)(minPct * unitSize + 0.5);
        }
        
        @Override
        public int getMaximum(int unitSize) {
            return (int)(maxPct * unitSize + 0.5);
        }
    }
}
