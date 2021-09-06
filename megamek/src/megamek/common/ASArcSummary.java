package megamek.common;

import java.util.EnumMap;

/** 
 * This class holds the AlphaStrike information for one arc of a multi-arc unit such as a 
 * Dropship or Warship or for a turret of a ground unit. It includes a standard damage
 * ASDamageVector that holds the standard damage of the turret or the STD damage of the arc.
 * It further includes a Map of specials that includes everything that can occur in an arc
 * or turret, including MSL, SCAP and CAP damage values for an arc.
 * 
 * @author Simon (Juliez)
 *
 */
public class ASArcSummary {
    
    /** Represents the standard-type, non-special damage of this arc or turret. */
    public ASDamageVector arcDamage;
    
    /** 
     * Contains all specials and special damage types of this arc or turret. This includes MSL,
     * SCAP and CAP damage for spaceship arcs and FLK, LRM, IF, PNT and all the other specials.  
     */
    public EnumMap<BattleForceSPA, Object> arcSpecials = new EnumMap<>(BattleForceSPA.class);

    public ASDamageVector getDamage() {
        return arcDamage;
    }

    public void setArcDamage(ASDamageVector arcDamage) {
        this.arcDamage = arcDamage;
    }

    public EnumMap<BattleForceSPA, Object> getArcSpecials() {
        return arcSpecials;
    }

    public void setArcSpecials(EnumMap<BattleForceSPA, Object> arcSpecials) {
        this.arcSpecials = arcSpecials;
    }
    
    

}
