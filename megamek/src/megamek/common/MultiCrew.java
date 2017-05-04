/**
 * 
 */
package megamek.common;

/**
 * Crew class for use with Tripod 'Mechs and QuadVees, which track damage separately to pilot
 * and gunner (and tech officer in the case of superheavy tripods).
 * 
 * @author Neoancient
 *
 */
public class MultiCrew extends Crew {

    /**
     * 
     */
    private static final long serialVersionUID = 1134026973918994259L;
    
    //In case of incapacity, other crew can fill in so we'll need skill values for them as well.
    private int pilotGunnery = 4;
    private int gunnerPiloting = 5;
    private int techGunnery = 8;
    private int techPiloting = 8;
    
    private int pilotGunneryL = 4;
    private int pilotGunneryB = 4;
    private int pilotGunneryM = 4;
    private int techGunneryL = 4;
    private int techGunneryB = 4;
    private int techGunneryM = 4;
    
    //We'll use the values from the parent class for the gunner.
    private int pilotHits;
    private boolean pilotUnconscious;
    private boolean pilotDoomed;
    private boolean pilotDead;
    private boolean pilotKoThisRound;

    private int techHits;
    private boolean techUnconscious;
    private boolean techDoomed;
    private boolean techDead;
    private boolean techKoThisRound;

    public MultiCrew(int size) {
        super(size);
        if (size > 2) {
            techGunnery = 4;
            techPiloting = 5;
        }
    }
    
    public void setPilotGunnery(int gunnery) {
        pilotGunnery = gunnery;
    }
    
    public void setTechGunner(int gunnery) {
        techGunnery = gunnery;
    }
    
    public void setGunnerPiloting(int piloting) {
        gunnerPiloting = piloting;
    }
    
    public void setTechPiloting(int piloting) {
        techPiloting = piloting;
    }
    
    public void setPilotGunneryL(int gunnery) {
        pilotGunneryL = gunnery;
    }
    
    public void setPilotGunneryB(int gunnery) {
        pilotGunneryB = gunnery;
    }
    
    public void setPilotGunneryM(int gunnery) {
        pilotGunneryM = gunnery;
    }
    
    public void setTechGunneryL(int gunnery) {
        techGunneryL = gunnery;
    }
    
    public void setTechGunneryB(int gunnery) {
        techGunneryB = gunnery;
    }
    
    public void setTechGunneryM(int gunnery) {
        techGunneryM = gunnery;
    }

    @Override
    public int getGunnery() {
        if (isGunnerActive()) {
            return super.getGunnery();
        } else {
            return Math.min(pilotGunnery, techGunnery);
        }
    }
    
    @Override
    public int getGunneryL() {
        if (isGunnerActive()) {
            return super.getGunneryL();
        } else {
            return Math.min(pilotGunneryL, techGunneryL);
        }
    }
    
    @Override
    public int getGunneryB() {
        if (isGunnerActive()) {
            return super.getGunneryB();
        } else {
            return Math.min(pilotGunneryB, techGunneryB);
        }
    }
    
    @Override
    public int getGunneryM() {
        if (isGunnerActive()) {
            return super.getGunneryM();
        } else {
            return Math.min(pilotGunneryM, techGunneryM);
        }
    }
    
    @Override
    public int getPiloting() {
        if (isPilotActive()) {
            return super.getPiloting();
        } else {
            return Math.min(gunnerPiloting, techPiloting);
        }
    }
    
    //TODO: this bonus should be +2 for a tripod that is designated as the force's command unit
    @Override
    public int getCommandBonus() {
        int bonus = super.getCommandBonus();
        if (isTechOfficerActive()) {
            ++bonus;
        }
        return bonus;
    }
    
    /**
     * Since this value is used to determine crew death, we want to return the lowest value. 
     */
    @Override
    public int getHits() {
        int hits = Math.min(super.getHits(), pilotHits);
        if (getSize() > 2) {
            hits = Math.min(hits, techHits);
        }
        return hits;
    }
    
    public int getGunnerHits() {
        return super.getHits();
    }
    
    public int getPilotHits() {
        return pilotHits;
    }
    
    public int getTechHits() {
        return techHits;
    }
    
    @Override
    public void applyDamage(int damage, boolean ammoExplosion) {
        if (!isEjected()) {
            if (ammoExplosion) {
                if (isPilotActive()) {
                    pilotHits += damage;
                    //If the pilot is out, who's driving? We'll favor the gunner, unless there
                    //is an active tech officer with a better piloting skill (or the gunner is out).
                } else if (isTechOfficerActive() && (!isGunnerActive() || techPiloting < gunnerPiloting)) {
                    techHits += damage;
                } else {
                    super.applyDamage(damage, ammoExplosion);
                }
            } else {
                super.applyDamage(damage, ammoExplosion);
                pilotHits += damage;
                techHits += damage;
            }
        }
    }
    
    public void setGunnerHits(int hits) {
        super.setHits(hits);
    }
    
    public void setPilotHits(int hits) {
        if (!isEjected()) {
            pilotHits = hits;
        }
    }
    
    public void setTechHits(int hits) {
        if (!isEjected()) {
            techHits = hits;
        }
    }
    
    //The crew as a whole is considered unconscious only if at least one is alive and none are active.
    @Override
    public boolean isUnconscious() {
        return !isActive() && !isDead();
    }

    public boolean isGunnerUnconscious() {
        return super.isUnconscious();
    }
    
    public boolean isPilotUnconscious() {
        return pilotUnconscious;
    }

    public boolean isTechUnconscious() {
        return techUnconscious;
    }
    
    public void setGunnerUnconscious(boolean unconscious) {
        super.setUnconscious(unconscious);
    }
    
    public void setPilotUnconcious(boolean unconscious) {
        pilotUnconscious = unconscious;
    }
    
    public void setTechUnconcious(boolean unconscious) {
        techUnconscious = unconscious;
    }
    
    //The crew as a whole is dead only if all members are dead.
    @Override
    public boolean isDead() {
        return isGunnerDead() && isPilotDead()
                && (getSize() < 3 || isTechDead());
    }

    public boolean isGunnerDead() {
        return super.isDead();
    }

    public boolean isPilotDead() {
        return pilotDead;
    }

    public boolean isTechDead() {
        return techDead;
    }

    public void setGunnerDead(boolean dead) {
        super.setDead(dead);
    }

    public void setPilotDead(boolean dead) {
        // Ejected pilots stop taking hits.
        if (!isEjected()) {
            pilotDead = dead;
            if (dead) {
                pilotHits = 6;
            }
        }
    }

    public void setTechDead(boolean dead) {
        // Ejected pilots stop taking hits.
        if (!isEjected()) {
            techDead = dead;
            if (dead) {
                techHits = 6;
            }
        }
    }
    
    @Override
    public boolean isDoomed() {
        return isGunnerDoomed() && isPilotDoomed()
                && (getSize() < 3 || isTechDoomed());
    }

    public boolean isGunnerDoomed() {
        return super.isDoomed();
    }

    public boolean isPilotDoomed() {
        return pilotDoomed;
    }

    public boolean isTechDoomed() {
        return techDoomed;
    }

    public void setGunnerDoomed(boolean b) {
        super.setDoomed(b);
    }

    public void setPilotDoomed(boolean b) {
        // Ejected pilots stop taking hits.
        if (!isEjected()) {
            pilotDoomed = b;
            if (b) {
                pilotHits = 6;
            }
        }
    }

    public void setTechDoomed(boolean b) {
        // Ejected pilots stop taking hits.
        if (!isEjected()) {
            techDoomed = b;
            if (b) {
                techHits = 6;
            }
        }
    }

    //The crew as a whole is considered active if any crewmember is active, since any can fill in as pilot and/or gunner.
    @Override
    public boolean isActive() {
        return isGunnerActive() || isPilotActive() || isTechOfficerActive();
    }

    @Override
    public boolean isPilotActive() {
        return !pilotUnconscious && !pilotDead;
    }

    @Override
    public boolean isTechOfficerActive() {
        return getSize() > 2 && !techUnconscious && !techDead;
    }

    public boolean isGunnerKoThisRound() {
        return super.isKoThisRound();
    }

    public boolean isPilotKoThisRound() {
        return pilotKoThisRound;
    }

    public boolean isTechKoThisRound() {
        return techKoThisRound;
    }

    public void setGunnerKoThisRound(boolean koThisRound) {
        super.setKoThisRound(koThisRound);;
    }

    public void setPilotKoThisRound(boolean koThisRound) {
        pilotKoThisRound = koThisRound;
    }

    public void setTechKoThisRound(boolean koThisRound) {
        techKoThisRound = koThisRound;
    }
/*
    public String getDesc() {
        String s = new String(name);
        if (hits > 0) {
            s += " (" + hits + " hit(s)";
            if (isUnconscious()) {
                s += " [ko]";
            } else if (isDead()) {
                s += " [dead]";
            }
            s += ")";
        } else if (isUnconscious()) {
            s += " [ko]";
        } else if (isDead()) {
            s += " [dead]";
        }
        return s;
    }

    public Vector<Report> getDescVector(boolean gunneryOnly) {
        Vector<Report> vDesc = new Vector<Report>();
        Report r;

        r = new Report();
        r.type = Report.PUBLIC;
        r.add(name);
        if (gunneryOnly) {
            r.messageId = 7050;
            r.add(getGunnery());
        } else {
            r.messageId = 7045;
            r.add(getGunnery());
            r.add(getPiloting());
        }

        if ((hits > 0) || isUnconscious() || isDead()) {
            Report r2 = new Report();
            r2.type = Report.PUBLIC;
            if (hits > 0) {
                r2.messageId = 7055;
                r2.add(hits);
                if (isUnconscious()) {
                    r2.messageId = 7060;
                    r2.choose(true);
                } else if (isDead()) {
                    r2.messageId = 7060;
                    r2.choose(false);
                }
            } else if (isUnconscious()) {
                r2.messageId = 7065;
                r2.choose(true);
            } else if (isDead()) {
                r2.messageId = 7065;
                r2.choose(false);
            }
            r.newlines = 0;
            vDesc.addElement(r);
            vDesc.addElement(r2);
        } else {
            vDesc.addElement(r);
        }
        return vDesc;
    }
*/
}
