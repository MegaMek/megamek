package megamek.common;

/**
 * A class that keeps track of information related to an ECM field.
 * @author arlith
 *
 */
public class ECMInfo implements Comparable {
    
    /**
     * The radius of the field.
     */
    private int range;
    
    /**
     * The center location of the field.
     */
    private Coords pos;
    
    /**
     * The id of the owning player, used to determine if this ECMInfo belongs to
     * an enemy or not.
     */
    private IPlayer owner;
    
    /**
     * The strength of the ECM field, counted in number of fields.
     */
    private double strength = 0;
    
    /**
     * The strength of the Angel ECM field, counted in number of fields.  This
     * is necessay as Angel ECM fields are inherently stronger than those from 
     * other ECM.
     */
    private int angelStrength = 0;
    
    /**
     * The strength of the ECCM field, counted in number of fields.
     */
    private int eccmStrength = 0;
    
    /**
     * The strength of the Angel ECCM field, counted in number of fields.  This
     * is necessay as Angel ECCM fields are inherently stronger than those from 
     * other ECCM.
     */    
    private int angelECCMStrength = 0;
    
    public ECMInfo() {
    }
    
    public ECMInfo(int range, double strength, IPlayer o, Coords p) {
        owner = o;
        pos = p;        
        this.range = range;
        this.strength = strength;
    }
    
    public ECMInfo(int range, double strength, Entity e) {
        owner = e.getOwner();
        pos = e.getPosition();        
        this.range = range;
        this.strength = strength;
    }
    
    public ECMInfo(int range, Coords pos, IPlayer owner, double strength,
            int angelStrength) {

        this.range = range;
        this.pos = pos;
        this.owner = owner;
        this.strength = strength;
        this.angelStrength = angelStrength;
    }
    
    public boolean isAngel() {
        return angelStrength > 0;
    }
    /**
     * 
     * @param r  
     * @param p
     * @param s
     */
    public ECMInfo(int r, Coords p, double s) {
        range = r;
        pos = p;
        strength = s;
    }
    
    /**
     * Returns true if this ECMInfo is considered to be ECCM;  that is, if the
     * strength of ECCM is greater than the strength of ECCM.
     * @return
     */
    public boolean isECCM() {
        return (angelECCMStrength > angelStrength) 
                || (angelStrength == 0 && eccmStrength > strength);
    }
    
    public boolean isAngelECCM() {
        return (angelECCMStrength > angelStrength);
    }
    
    /**
     * True if the number of ECM fields is greater than the number of ECCM
     * fields (which default to 0).
     * @return
     */
    public boolean isECM() {
        return (angelStrength > angelECCMStrength) 
                || (angelECCMStrength == angelStrength 
                        && strength > eccmStrength);
    }
    
    public boolean isAngelECM() {
        return (angelStrength > angelECCMStrength);
    }
    
    
    
    /**
     * Compute the ECMInfo from another instance into this one.  All enemy ECM
     * strength is added, and all allied ECCM strength is added.
     * @param other
     */
    public void addECMEffects(ECMInfo other) {
        // Enemy ECM (ECM without an owner is always considered an enemy)
        if (((other.owner == null) || owner.isEnemyOf(other.owner))) {
            strength += other.strength;
            angelStrength += other.angelStrength;
        // Allied ECCM
        } else if ((other.owner != null) && !owner.isEnemyOf(other.owner)) {
            eccmStrength += other.eccmStrength;
            angelECCMStrength += other.angelECCMStrength;
        }        
    }
    
    public String toString() {
        String ownerString;
        String strengthString = "";
        String eccmString = "";
        
        if (owner != null) {
            ownerString = owner.getName();
        } else {
            ownerString = "none";
        }
        if (angelStrength != 0) {
            strengthString = ", aS: " + angelStrength;
        } else if (strength != 0){
            strengthString = ", s: " + strength;
        }
        
        if (angelECCMStrength != 0) {
            eccmString = ", cAS: " + angelECCMStrength;
        } else if (eccmStrength != 0){
            eccmString = ", cS: " + eccmStrength;
        }
        return "(" + pos.toString() + ", " + ownerString + ", r:" + range
                + strengthString +  eccmString + ")";
    }

    /**
     * Returns true if the supplied ECMInfo is opposed to this one.
     * @param other
     * @return
     */
    public boolean isOpposed(ECMInfo other) {
        return (owner == null) || (other.getOwner() == null) 
                || owner.isEnemyOf(other.getOwner());
    }
    /**
     * Equality is based on whether position, owner, range and all strengths
     * match.
     * 
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o){
       if (!(o instanceof ECMInfo)) {
           return false;
       }
       ECMInfo other = (ECMInfo)o;
       boolean ownersMatch = ((owner == null && other.owner == null) 
               || owner.equals(other.owner));
       boolean posMatch = pos.equals(other.pos);
       boolean strMatch = (strength == other.strength) 
               && (angelStrength == other.angelStrength) 
               && (eccmStrength == other.eccmStrength) 
               && (angelECCMStrength == other.angelECCMStrength); 
       boolean rangeMatch = range == other.range;
       return ownersMatch && posMatch && strMatch && rangeMatch;
    }
    
    /**
     * Compares two ECMInfo objects; ordering is based on strength, with Angel
     * strength trumping regular strength. 
     */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof ECMInfo)) {
            return 1;
        }
        ECMInfo other = (ECMInfo)o;
        // Compare two ECCMs
        if (isECCM() && other.isECCM()) {
            if (other.angelECCMStrength > angelECCMStrength) {
                return -1;
            } else if (other.angelECCMStrength < angelECCMStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (other.eccmStrength > eccmStrength) {
                    return -1;
                } else if (other.eccmStrength < eccmStrength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return 0;
        // Compare ECCM to ECM
        } else if (isECCM() && !other.isECCM()) {
            if (other.angelStrength > angelECCMStrength) {
                return -1;
            } else if (other.angelStrength < angelECCMStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (other.strength > eccmStrength) {
                    return -1;
                } else if (other.strength < eccmStrength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return 0;
        // Compare ECM to ECCM    
        } else if (!isECCM() && other.isECCM()) {
            if (other.angelECCMStrength > angelStrength) {
                return -1;
            } else if (other.angelECCMStrength < angelStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (other.eccmStrength > strength) {
                    return -1;
                } else if (other.eccmStrength < strength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return 0;
        } else { // Compare two ECMs
            if (other.angelStrength > angelStrength) {
                return -1;
            } else if (other.angelStrength < angelStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (other.strength > strength) {
                    return -1;
                } else if (other.strength < strength) {
                    return 1;
                }
            }
            // Both angel and regular strength are equal
            return 0;
        }
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = Math.max(0, range);
    }

    public Coords getPos() {
        return pos;
    }

    public void setPos(Coords pos) {
        this.pos = pos;
    }

    public IPlayer getOwner() {
        return owner;
    }

    public void setOwner(IPlayer owner) {
        this.owner = owner;
    }

    public double getECMStrength() {
        return strength;
    }

    public void setECMStrength(double strength) {
        this.strength = Math.max(0, strength);
    }

    public int getAngelECMStrength() {
        return angelStrength;
    }

    public void setAngelECMStrength(int angelStrength) {
        this.angelStrength = Math.max(0, angelStrength);
    }

    public int getECCMStrength() {
        return eccmStrength;
    }

    public void setECCMStrength(int eccmStrength) {
        this.eccmStrength = Math.max(0, eccmStrength);
    }

    public int getAngelECCMStrength() {
        return angelECCMStrength;
    }

    public void setAngelECCMStrength(int angelECCMStrength) {
        this.angelECCMStrength = Math.max(0, angelECCMStrength);
    }
}