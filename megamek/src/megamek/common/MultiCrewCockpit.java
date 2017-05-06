/**
 * 
 */
package megamek.common;

import java.util.Arrays;

/**
 * Crew class for use with Tripod 'Mechs and QuadVees, which track damage separately to pilot
 * and gunner (and tech officer in the case of superheavy tripods).
 * 
 * @author Neoancient
 *
 */
public class MultiCrewCockpit extends Crew {

    /**
     * 
     */
    private static final long serialVersionUID = 1134026973918994259L;
    
    /**
     * This enum only covers multicrew cockpits for units that usually have a single pilot. It is used
     * to initialize crew size and the normal positions for pilot and gunner. For the superheavy
     * tripod and force command console (being the cockpit command console mounted on a heavy/assault Mek),
     * it also gives the position of the crew member that provides a command bonus.
     *
     */
    public enum CockpitType {
        TRIPOD (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 3),
        SUPERHEAVY_TRIPOD (new String[] {"Pilot", "Gunner", "Tech Officer"}, 0, 1, 2, 2, 3),
        QUADVEE (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 3),
        DUAL (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 2),
        COMMAND_CONSOLE (new String[] {"Pilot", "Commander"}, 0, 0, -1, -1, 1),
        FORCE_COMMAND_CONSOLE (new String[] {"Pilot", "Commander"}, 0, 0, 1, -1, 1);
        
        private String[] roleNames;
        private int pilotPos;
        private int gunnerPos;
        private int commanderPos;
        private int techPos;
        private int maxPrimaryTargets;
        
        CockpitType(String[] roleNames, int pilotPos, int gunnerPos, int commanderPos, int techPos,
                int maxPrimaryTargets) {
            this.roleNames = roleNames;
            this.pilotPos = pilotPos;
            this.commanderPos = commanderPos;
            this.techPos = techPos;
            this.maxPrimaryTargets = maxPrimaryTargets;
        }
        
        public int getCrewSize() {
            return roleNames.length;
        }
        
        public String getRoleName(int index) {
            return roleNames[index];
        }
        
        public int getPilotPos() {
            return pilotPos;
        }
        
        public int getGunnerPos() {
            return gunnerPos;
        }
        
        public int getCommanderPos() {
            return commanderPos;
        }
        
        public int getTechPos() {
            return techPos;
        }
        
        public int getMaxPrimaryTargets() {
            return maxPrimaryTargets;
        }
    }
    
    private final CockpitType cockpitType;
    
    //Skills and health for multiple crew members. Note that the equivalent fields in the parent class
    //are not used and we will need to override access methods to return these values instead.
    private final String[] name;
    private final int[] gunnery;
    private final int[] piloting;
    private final int[] gunneryL;
    private final int[] gunneryB;
    private final int[] gunneryM;
    private final int[] artillery;
    private final int[] hits;
    private final boolean[] unconscious;
    private final boolean[] dead;
    private final boolean[] koThisRound;

    //In the event that the pilot or gunner is incapacitated, this records who has taken over
    //the duties.
    private int pilotIndex;
    private int gunnerIndex;
    
    public MultiCrewCockpit(CockpitType cockpitType) {
        this(cockpitType, "Unnamed", 4, 5);        
    }
    
    public MultiCrewCockpit(CockpitType cockpitType, String name, int gunnery, int piloting) {
        this(cockpitType, name, gunnery, gunnery, gunnery, piloting);
    }

    public MultiCrewCockpit(CockpitType cockpitType, String name, int gunneryL, int gunneryM, int gunneryB, int piloting) {
        super(name, cockpitType.getCrewSize(), gunneryL, gunneryM, gunneryB, piloting);
        this.cockpitType = cockpitType;
        int size = cockpitType.getCrewSize();
        
        this.name = new String[size];
        Arrays.fill(this.name, name);
        this.gunnery = new int[size];
        Arrays.fill(this.gunnery, super.getGunnery());
        this.piloting = new int[size];
        Arrays.fill(this.piloting, piloting);
        this.gunneryL = new int[size];
        Arrays.fill(this.gunneryL, gunneryL);
        this.gunneryB = new int[size];
        Arrays.fill(this.gunneryB, gunneryB);
        this.gunneryM = new int[size];
        Arrays.fill(this.gunneryM, gunneryM);
        this.artillery = new int[size];
        Arrays.fill(this.artillery, super.getGunnery());
        this.hits = new int[size];
        this.unconscious = new boolean[size];
        this.dead = new boolean[size];
        this.koThisRound = new boolean[size];
        
        pilotIndex = cockpitType.getPilotPos();
        gunnerIndex = cockpitType.getGunnerPos();
    }
    
    public CockpitType getCockpitType() {
        return cockpitType;
    }
    
    public void setName(String name, int pos) {
        this.name[pos] = name;
        if (pos == 0) {
            super.setName(name);
        }
    }
    
    public String getName(int pos) {
        return name[pos];
    }
        
    public void setGunnery(int gunnery, int pos) {
        this.gunnery[pos] = gunnery;
    }
    
    public void setPiloting(int piloting, int pos) {
        this.piloting[pos] = piloting;
    }
    
    public void setGunneryL(int gunneryL, int pos) {
        this.gunneryL[pos] = gunneryL;
    }
    
    public void setGunneryB(int gunneryB, int pos) {
        this.gunneryB[pos] = gunneryB;
    }
    
    public void setGunneryM(int gunneryM, int pos) {
        this.gunneryM[pos] = gunneryM;
    }

    public void setArtillery(int artillery, int pos) {
        this.artillery[pos] = artillery;
    }

    @Override
    public int getGunnery() {
        selectGunner();
        return gunnery[gunnerIndex];
    }
    
    @Override
    public int getGunneryL() {
        selectGunner();
        return gunneryL[gunnerIndex];
    }
    
    @Override
    public int getGunneryB() {
        selectGunner();
        return gunneryB[gunnerIndex];
    }
    
    @Override
    public int getGunneryM() {
        selectGunner();
        return gunneryM[gunnerIndex];
    }
    
    @Override
    public int getPiloting() {
        selectPilot();
        return piloting[pilotIndex];
    }
    
    //TODO: this bonus should be +2 for a tripod that is designated as the force's command unit
    @Override
    public int getCommandBonus() {
        int bonus = super.getCommandBonus();
        if (isActive(cockpitType.getCommanderPos())) {
            ++bonus;
        }
        return bonus;
    }
    
    /**
     * Since this value is used to determine crew death, we want to return the lowest value. 
     */
    @Override
    public int getHits() {
        int h = DEATH;
        for (int i = 0; i < hits.length; i++) {
            if (hits[i] < h) {
                h = hits[i];
            }
        }
        return h;
    }
    
    public int getHits(int i) {
        return hits[i];
    }
    
    @Override
    public void applyDamage(int damage, boolean ammoExplosion) {
        if (!isEjected()) {
            if (ammoExplosion) {
                if (!isActive(pilotIndex)) {
                    selectPilot();
                }
                hits[pilotIndex] += damage;
            } else {
                for (int i = 0; i < hits.length; i++) {
                    hits[i] += damage;
                }
            }
        }
    }
    
    public void setHits(int hits, int pos) {
        this.hits[pos] = hits;
    }
    
    //The crew as a whole is considered unconscious only if at least one is alive and none are active.
    @Override
    public boolean isUnconscious() {
        return !isActive() && !isDead();
    }
    
    public boolean isUnconsious(int pos) {
        return pos >= 0 && pos < getSize() && unconscious[pos];
    }
    
    /**
     * Sets the unconscious status of a crew member. If this crew member is in the pilot and/or gunner
     * position and is now active, the crew member will resume his or her normal duties.
     * 
     * @param unconscious Whether the crew member is now unconscious
     * @param pos         The crew member index.
     */
    public void setUnconscious(boolean unconscious, int pos) {
        this.unconscious[pos] = unconscious;
        if (isActive(pos)) {
            if (pos == cockpitType.getPilotPos()) {
                pilotIndex = pos;
            }
            if (pos == cockpitType.getGunnerPos()) {
                gunnerIndex = pos;
            }
        }
    }
    
    //The crew as a whole is dead only if all members are dead.
    @Override
    public boolean isDead() {
        for (int i = 0; i < dead.length; i++) {
            if (!dead[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isDead(int pos) {
        return pos >= 0 && pos < getSize() && dead[pos];
    }

    public void setDead(boolean dead, int pos) {
        if (!isEjected()) {
            this.dead[pos] = dead;
            if (dead) {
                hits[pos] = DEATH;
            }
        }
    }

    @Override
    public void setDoomed(boolean doomed) {
        super.setDoomed(doomed);
        if (!isEjected() && doomed) {
            for (int i = 0; i < hits.length; i++) {
                hits[i] = DEATH;
            }
        }
    }

    //The crew as a whole is considered active if any crewmember is active, since any can fill in as pilot and/or gunner.
    @Override
    public boolean isActive() {
        for (int i = 0; i < getSize(); i++) {
            if (isActive(i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActive(int pos) {
        return pos >= 0 && pos < getSize() && !unconscious[pos] && !dead[pos];
    }

    public boolean isKoThisRound(int pos) {
        return pos >= 0 && pos < getSize() && koThisRound[pos];
    }

    public void setKoThisRound(boolean koThisRound, int pos) {
        this.koThisRound[pos] = koThisRound;
    }
    
    public int getCurrentPilotIndex() {
        return pilotIndex;
    }
    
    public int getCurrentGunnerIndex() {
        return gunnerIndex;
    }
    
    public void setCurrentPilot(int pos) {
        pilotIndex = pos;
        if (cockpitType.equals(CockpitType.COMMAND_CONSOLE)
                || cockpitType.equals(CockpitType.FORCE_COMMAND_CONSOLE)) {
            gunnerIndex = pos;
        }
    }
    
    public void setCurrentGunner(int pos) {
        gunnerIndex = pos;
        if (cockpitType.equals(CockpitType.COMMAND_CONSOLE)
                || cockpitType.equals(CockpitType.FORCE_COMMAND_CONSOLE)) {
            pilotIndex = pos;
        }
    }

    /**
     * If the current pilot is incapacitated, we need to find someone to take over the duties.
     */
    private void selectPilot() {
        int pilot = -1;
        //Start by checking whether the default pilot is available.
        if (isActive(cockpitType.getPilotPos())) {
            pilot = cockpitType.getPilotPos();
        } else if (!isActive(pilotIndex)) {
            //If not, look for the crew member with the best piloting skill. If equal, we use the lowest index.
            int skill = MAX_SKILL;
            for (int i = 0; i < piloting.length; i++) {
                if (piloting[i] < skill && isActive(i)) {
                    pilot = i;
                    skill = piloting[i];
                }
            }
            //If none is found with less than the max skill, just take the first active.
            if (pilotIndex < 0) {
                for (int i = 0; i < piloting.length; i++) {
                    if (isActive(i)) {
                        pilot = i;
                        break;
                    }
                }
            }
            //If we still haven't found any, the entire crew is inactive and it doesn't matter who the pilot is.
            if (pilotIndex < 0) {
                pilot = cockpitType.getPilotPos();
            }
        }
        //Make sure that cockpit command consoles use the same crew member for pilot and gunner.
        setCurrentPilot(pilot);
    }

    /**
     * If the current gunner is incapacitated, we need to find someone to take over the duties.
     */
    private void selectGunner() {
        int gunner = -1;
        //Start by checking whether the default gunner is available.
        if (isActive(cockpitType.getGunnerPos())) {
            gunner = cockpitType.getGunnerPos();
        } else if (!isActive(gunnerIndex)) {
            //If not, look for the crew member with the best gunnery skill. If equal, we use the lowest index.
            int skill = MAX_SKILL;
            for (int i = 0; i < gunnery.length; i++) {
                if (gunnery[i] < skill && isActive(i)) {
                    gunner = i;
                    skill = gunnery[i];
                }
            }
            //If none is found with less than the max skill, just take the first active.
            if (gunnerIndex < 0) {
                for (int i = 0; i < gunnery.length; i++) {
                    if (isActive(i)) {
                        gunner = i;
                        break;
                    }
                }
            }
            //If we still haven't found any, the entire crew is inactive and it doesn't matter who the gunner is.
            if (gunnerIndex < 0) {
                gunner = cockpitType.getGunnerPos();
            }
        }
        //Make sure that cockpit command consoles use the same crew member for pilot and gunner.
        setCurrentGunner(gunner);
    }
    
    @Override
    public boolean hasDedicatedPilot() {
        return pilotIndex == cockpitType.getPilotPos()
                && pilotIndex != gunnerIndex;
    }
    
    @Override
    public boolean hasDedicatedGunner() {
        return gunnerIndex == cockpitType.getGunnerPos()
                && pilotIndex != gunnerIndex;
    }
    
    @Override
    public boolean hasActiveTechOfficer() {
        return isActive(cockpitType.getTechPos());
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
