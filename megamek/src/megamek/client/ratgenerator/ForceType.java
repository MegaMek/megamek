/**
 * 
 */
package megamek.client.ratgenerator;

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

}
