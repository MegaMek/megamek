/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.enums.Gender;
import megamek.common.util.CrewSkillSummaryUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Crew class for LAMs which tracks separate skills for 'Mech and Fighter modes,
 * and chooses the correct one based on the LAM's current movement mode.
 *
 * @author Neoancient
 */
public class LAMPilot extends Crew {
    private static final long serialVersionUID = -5081079779376940577L;

    final private LandAirMech lam;
    private int gunneryAero;
    private int gunneryAeroB;
    private int gunneryAeroL;
    private int gunneryAeroM;
    private int pilotingAero;

    public LAMPilot(LandAirMech lam) {
        this(lam, RandomNameGenerator.UNNAMED_FULL_NAME, 4, 5,
                4, 5, Gender.RANDOMIZE, false, null);
    }

    public LAMPilot(LandAirMech lam, String name, int gunneryMech, int pilotingMech,
                    int gunneryAero, int pilotingAero, Gender gender, boolean clanPilot,
                    Map<Integer, Map<String, String>> extraData) {
        super(CrewType.SINGLE, name, 1, gunneryMech, pilotingMech, gender, clanPilot, extraData);
        this.lam = lam;
        this.gunneryAero = gunneryAero;
        this.pilotingAero = pilotingAero;
        this.gunneryAeroB = gunneryAero;
        this.gunneryAeroL = gunneryAero;
        this.gunneryAeroM = gunneryAero;
    }

    /**
     * Used by LandAirMech.setCrew to convert a <code>Crew</code> instance into
     * a <code>LAMPilot</code> instance.
     *
     * @param lam
     *            The LAM that is piloted by this crew.
     * @param crew
     *            The crew to convert to LAMPilot.
     * @return An instance of <code>LAMPilot</code> that has the same values as
     *         the crew argument.
     */
    public static LAMPilot convertToLAMPilot(LandAirMech lam, Crew crew) {
        Map<Integer, Map<String, String>> extraData = new HashMap<>();
        extraData.put(0, crew.getExtraDataForCrewMember(0));
        LAMPilot pilot = new LAMPilot(lam, crew.getName(), crew.getGunnery(), crew.getPiloting(),
                crew.getGunnery(), crew.getPiloting(), crew.getGender(), crew.isClanPilot(), extraData);
        pilot.setNickname(crew.getNickname(), 0);
        pilot.setPortrait(crew.getPortrait(0).clone(), 0);
        pilot.setGunneryL(crew.getGunneryL(), 0);
        pilot.setGunneryB(crew.getGunneryB(), 0);
        pilot.setGunneryM(crew.getGunneryM(), 0);
        pilot.setGunneryAeroL(crew.getGunneryL());
        pilot.setGunneryAeroB(crew.getGunneryB());
        pilot.setGunneryAeroM(crew.getGunneryM());
        pilot.setHits(crew.getHits(0), 0);
        pilot.setUnconscious(crew.isUnconscious(0), 0);
        pilot.setDead(crew.isDead(0), 0);
        pilot.setDoomed(crew.isDoomed());
        pilot.setEjected(crew.isEjected());
        pilot.setFatigue(crew.getFatigue());
        pilot.setArtillery(crew.getArtillery(), 0);
        pilot.setInitBonus(crew.getInitBonus());
        pilot.setCommandBonus(crew.getCommandBonus());
        pilot.setToughness(crew.getToughness(0), 0);
        pilot.setOptions(crew.getOptions());

        pilot.setExternalIdAsString(crew.getExternalIdAsString(0), 0);

        return pilot;
    }

    public int getGunneryMech() {
        return super.getGunnery(0);
    }

    public void setGunneryMech(int gunnery) {
        super.setGunnery(gunnery, 0);
    }

    public int getGunneryMechB() {
        return super.getGunneryB(0);
    }

    public void setGunneryMechB(int gunnery) {
        super.setGunneryB(gunnery, 0);
    }

    public int getGunneryMechM() {
        return super.getGunneryM(0);
    }

    public void setGunneryMechM(int gunnery) {
        super.setGunneryM(gunnery, 0);
    }

    public int getGunneryMechL() {
        return super.getGunneryL(0);
    }

    public void setGunneryMechL(int gunnery) {
        super.setGunneryL(gunnery, 0);
    }

    public int getPilotingMech() {
        return super.getPiloting(0);
    }

    public void setPilotingMech(int piloting) {
        super.setPiloting(piloting, 0);
    }

    public int getGunneryAero() {
        return gunneryAero;
    }

    public void setGunneryAero(int gunnery) {
        gunneryAero = gunnery;
    }

    public int getGunneryAeroB() {
        return gunneryAeroB;
    }

    public void setGunneryAeroB(int gunnery) {
        gunneryAeroB = gunnery;
    }

    public int getGunneryAeroL() {
        return gunneryAeroL;
    }

    public void setGunneryAeroL(int gunnery) {
        gunneryAeroL = gunnery;
    }

    public int getGunneryAeroM() {
        return gunneryAeroM;
    }

    public void setGunneryAeroM(int gunnery) {
        gunneryAeroM = gunnery;
    }

    public int getPilotingAero() {
        return pilotingAero;
    }

    public void setPilotingAero(int piloting) {
        pilotingAero = piloting;
    }

    private boolean useAeroGunnery() {
        if (lam.isConvertingNow()) {
            return lam.getPreviousConversionMode() == LandAirMech.CONV_MODE_FIGHTER;
        } else {
            return lam.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER;
        }
    }

    @Override
    public int getGunnery() {
        return useAeroGunnery() ? getGunneryAero() : getGunneryMech();
    }

    @Override
    public int getGunneryB() {
        return useAeroGunnery() ? getGunneryAeroB() : getGunneryMechB();
    }

    @Override
    public int getGunneryL() {
        return useAeroGunnery() ? getGunneryAeroL() : getGunneryMechL();
    }

    @Override
    public int getGunneryM() {
        return useAeroGunnery() ? getGunneryAeroM() : getGunneryMechM();
    }

    @Override
    public int getPiloting() {
        if (lam.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER
                || (lam.getConversionMode() == LandAirMech.CONV_MODE_AIRMECH && lam.isAirborneVTOLorWIGE())) {
            return pilotingAero;
        } else {
            return super.getPiloting();
        }
    }

    @Override
    public int getPiloting(EntityMovementType moveType) {
        if (lam.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER || (lam
                .getConversionMode() == LandAirMech.CONV_MODE_AIRMECH
                && (moveType == EntityMovementType.MOVE_VTOL_WALK || moveType == EntityMovementType.MOVE_VTOL_RUN))) {
            return pilotingAero;
        } else {
            return super.getPiloting();
        }
    }

    @Override
    public String getSkillsAsString(boolean showPiloting, boolean rpgSkills) {
        return getSkillsAsString(0, showPiloting, rpgSkills);
    }

    /**
     * @return a String showing the overall skills in the format gunnery
     *         (Mech)/piloting (Mech)/gunnery (Aero)/piloting (Aero)
     */
    @Override
    public String getSkillsAsString(int pos, boolean showPiloting, boolean rpgSkills) {
        if (showPiloting) {
            return CrewSkillSummaryUtil.getLAMPilotSkillSummary(
                    getGunnery(pos),
                    getGunneryL(pos),
                    getGunneryM(pos),
                    getGunneryB(pos),
                    getPiloting(pos),
                    getGunneryAero(),
                    getGunneryAeroL(),
                    getGunneryAeroM(),
                    getGunneryAeroB(),
                    getPilotingAero(),
                    rpgSkills);
        } else {
            return CrewSkillSummaryUtil.getLAMGunnerySkillSummary(
                    getGunnery(pos),
                    getGunneryL(pos),
                    getGunneryM(pos),
                    getGunneryB(pos),
                    getGunneryAero(),
                    getGunneryAeroL(),
                    getGunneryAeroM(),
                    getGunneryAeroB(),
                    rpgSkills);
        }
    }

    /**
     * Crew summary report used for victory phase.
     *
     * @param gunneryOnly
     *            Do not show the piloting skill
     */
    @Override
    public Vector<Report> getDescVector(boolean gunneryOnly) {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report();
        r.type = Report.PUBLIC;
        r.add(getName(0));
        if (getSlotCount() > 1) {
            r.add(" (" + getCrewType().getRoleName(0) + ")");
        }
        r.messageId = 7045;
        r.add(getGunneryMech() + "/" + getGunneryAero());
        r.add(getPilotingMech() + "/" + getPilotingAero());

        if (getHits(0) > 0 || isUnconscious(0) || isDead(0)) {
            Report r2 = new Report();
            r2.type = Report.PUBLIC;
            if (getHits(0) > 0) {
                r2.messageId = 7055;
                r2.add(getHits(0));
                if (isUnconscious(0)) {
                    r2.messageId = 7060;
                    r2.choose(true);
                } else if (isDead(0)) {
                    r2.messageId = 7060;
                    r2.choose(false);
                }
            } else if (isUnconscious(0)) {
                r2.messageId = 7065;
                r2.choose(true);
            } else if (isDead(0)) {
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

    /**
     * Returns whether this pilot has non-standard piloting or gunnery values
     */
    @Override
    public boolean isCustom() {
        return getGunneryMech() != 4 || getGunneryAero() != 4 || getPilotingMech() != 5 || getPilotingAero() != 5;
    }
}