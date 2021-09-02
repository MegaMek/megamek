package megamek.common;

/** 
 * Represents a single AlphaStrike damage value that may be minimal damage (0*).
 * Minimal Damage is repesented by minimal == true, all other values by damage being
 * their damage value and minimal == false. 
 */
public class ASDamage {
    
    public final int damage;
    public final boolean minimal;
    
    public ASDamage(int dmg, boolean minim) {
        damage = dmg;
        minimal = minim;
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is rounded "normally" (i.e. up or down depending on the tenth) to the nearest
     * integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamage createRoundedNormal(double dmg) {
        return new ASDamage((int)Math.round(dmg), (dmg > 0) && (dmg < 0.5));
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is first rounded up to the nearest tenth, then assigned minimal damage if < 0.5,  
     * otherwise rounded up (i.e. up or down depending on the tenth) to the nearest integer.
     */
    public static ASDamage createDualRoundedNormal(double dmg) {
        double intermediate = AlphaStrikeConverter.roundUpToTenth(dmg);
        if (intermediate < 0.5) {
            return new ASDamage(0, intermediate > 0);
        } else {
            return new ASDamage((int)Math.round(intermediate), false);
        }
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is rounded "up" (i.e. up or down depending on the tenth) to the nearest integer
     * except for values smaller than 0.5 and values between 0 and 0.5 excl. end up as 
     * minimal damage. 
     */
    public static ASDamage createRoundedUp(double dmg) {
        if (dmg < 0.5) {
            return new ASDamage(0, dmg > 0);
        } else {
            return new ASDamage((int)Math.ceil(dmg), false);
        }
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is first rounded up to the nearest tenth, then assigned minimal damage if < 0.5,  
     * otherwise rounded up (i.e. up or down depending on the tenth) to the nearest integer.
     */
    public static ASDamage createDualRoundedUp(double dmg) {
        double intermediate = AlphaStrikeConverter.roundUpToTenth(dmg);
        if (intermediate < 0.5) {
            return new ASDamage(0, intermediate > 0);
        } else {
            return new ASDamage((int)Math.round(intermediate + 0.4), false);
        }
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is rounded "normally" (i.e. up or down depending on the tenth) to the nearest
     * integer. There is no minimal damage, i.e. dmg < 0.5 becomes 0. 
     */
    public static ASDamage createRoundedNormalNoMinimal(double dmg) {
        return new ASDamage((int)Math.round(dmg), false);
    }
    
    /** 
     * Creates an AlphaStrike single damage value from the given double value. The value
     * is first rounded up to the nearest tenth, then rounded "normally" (i.e. up or 
     * down depending on the tenth) to the nearest integer. There is no minimal damage, 
     * i.e. dmg < 0.41 becomes 0. 
     */
    public static ASDamage createDualRoundedNormalNoMinimal(double dmg) {
        double intermediate = AlphaStrikeConverter.roundUpToTenth(dmg);
        return new ASDamage((int)Math.round(intermediate), false);
    }
    
    /** Returns true if this ASDamage represents no damage, not even minimal. */ 
    public boolean noDamage() {
        return (damage == 0) && !minimal;
    }
    
    @Override
    public String toString() {
        if (minimal) {
            return "0*";
        } else if (damage == 0) {
            return "-";
        } else {
            return "" + damage;
        }
    }
    
    public String toStringWithZero() {
        if (minimal) {
            return "0*";
        } else {
            return "" + damage;
        }
    }
}
