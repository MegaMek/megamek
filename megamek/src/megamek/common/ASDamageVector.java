package megamek.common;

/** 
 * Represents a full AlphaStrike damage value combination of S/M/L/E values,
 * all of which may be minimal damage (0*).
 * Minimal Damage is repesented by minimal == true, all other values by damage being
 * their damage value and minimal == false. 
 * Note that this class does not care if the E value is valid or not; if it is not used,
 * it is usually 0.
 */
public class ASDamageVector {
    
    public ASDamage S;
    public ASDamage M;
    public ASDamage L;
    public ASDamage E;
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded normally (i.e. up or down depending on the tenth) 
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createRoundedNormal(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createRoundedNormal(s);
        result.M = ASDamage.createRoundedNormal(m);
        result.L = ASDamage.createRoundedNormal(l);
        result.E = ASDamage.createRoundedNormal(e);
        return result;
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L from the given 
     * double values. The values are rounded normally (i.e. up or down depending on the tenth) 
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createRoundedNormal(double s, double m, double l) {
        return createRoundedNormal(s, m, l, 0);
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded up to the nearest integer and values 
     * between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createRoundedUp(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createRoundedUp(s);
        result.M = ASDamage.createRoundedUp(m);
        result.L = ASDamage.createRoundedUp(l);
        result.E = ASDamage.createRoundedUp(e);
        return result;
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L from the given 
     * double values. The values are rounded up to the nearest integer and values 
     * between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createRoundedUp(double s, double m, double l) {
        return createRoundedUp(s, m, l, 0);
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createDualRoundedUp(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createDualRoundedUp(s);
        result.M = ASDamage.createDualRoundedUp(m);
        result.L = ASDamage.createDualRoundedUp(l);
        result.E = ASDamage.createDualRoundedUp(e);
        return result;
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L from the given 
     * double values. The values are rounded first up to the nearest tenth, then up 
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createDualRoundedNormal(double s, double m, double l) {
        return createDualRoundedNormal(s, m, l, 0);
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createDualRoundedNormal(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createDualRoundedNormal(s);
        result.M = ASDamage.createDualRoundedNormal(m);
        result.L = ASDamage.createDualRoundedNormal(l);
        result.E = ASDamage.createDualRoundedNormal(e);
        return result;
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L from the given 
     * double values. The values are rounded first up to the nearest tenth, then up 
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage. 
     */
    public static ASDamageVector createDualRoundedUp(double s, double m, double l) {
        return createDualRoundedUp(s, m, l, 0);
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded normally (i.e. up or down depending on the tenth) 
     * to the nearest integer. There is no minimal damage, so damage values < 0.5 become 0. 
     */
    public static ASDamageVector createRoundedNormalNoMinimal(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createRoundedNormalNoMinimal(s);
        result.M = ASDamage.createRoundedNormalNoMinimal(m);
        result.L = ASDamage.createRoundedNormalNoMinimal(l);
        result.E = ASDamage.createRoundedNormalNoMinimal(e);
        return result;
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L from the given 
     * double values. The values are rounded normally (i.e. up or down depending on the tenth) 
     * to the nearest integer. There is no minimal damage, so damage values < 0.5 become 0. 
     */
    public static ASDamageVector createRoundedNormalNoMinimal(double s, double m, double l) {
        return createRoundedNormalNoMinimal(s, m, l, 0);
    }
    
    /** 
     * Creates a full AlphaStrike damage value combination of S/M/L/E from the given 
     * double values. The values are rounded first up to the nearest tenth, then normally 
     * (i.e. up or down depending on the tenth) the nearest integer. There is no minimal 
     * damage, so damage values < 0.41 become 0. 
     */
    public static ASDamageVector createDualRoundedNormalNoMinimal(double s, double m, double l, double e) {
        var result = new ASDamageVector();
        result.S = ASDamage.createDualRoundedNormalNoMinimal(s);
        result.M = ASDamage.createDualRoundedNormalNoMinimal(m);
        result.L = ASDamage.createDualRoundedNormalNoMinimal(l);
        result.E = ASDamage.createDualRoundedNormalNoMinimal(e);
        return result;
    }
    
    /** Returns an S/M String representation of this ASDV, e.g. 2/2. The L and E values are ignored. */
    public String getSMString() {
        return "" + S + "/" + M;
    }
    
    /** Returns an S/M/L String representation of this ASDV, e.g. 2/1/0*, disregarding any E value. */
    public String getSMLString() {
        return "" + S + "/" + M + "/" + L;
    }
    
    /** Returns an S/M/L/E String representation of this ASDV, e.g. 2/1/1/0. The E value is always included. */
    public String getSMLEString() {
        return "" + S + "/" + M + "/" + L + "/" + E;
    }
    
    public String getSMLStringWithZero() {
        return "" + S.toStringWithZero() + "/" + M.toStringWithZero() + "/" + L.toStringWithZero();
    }
    
    public String getSMLEStringWithZero() {
        return "" + S.toStringWithZero() + "/" + M.toStringWithZero()
            + "/" + L.toStringWithZero() + "/" + E.toStringWithZero();
    }
    
    @Override
    public String toString() {
        return "" + S + "/" + M + "/" + L + (E.noDamage() ? "" : "/" + E);
    }
    
}