/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.options;

import java.io.Serializable;
import java.util.Vector;

/**
 * Contains the options determining abilities of the pilot
 * 
 * @author Cord
 */
public class PilotOptions extends AbstractOptions implements Serializable {
    private static final long serialVersionUID = 6628080570425023949L;
    public static final String LVL3_ADVANTAGES = "lvl3Advantages"; //$NON-NLS-1$
    public static final String MD_ADVANTAGES = "MDAdvantages"; //$NON-NLS-1$

    public PilotOptions() {
        super();
    }

    public void initialize() {
        IBasicOptionGroup adv = addGroup("adv", LVL3_ADVANTAGES); //$NON-NLS-1$
        addOption(adv, "dodge_maneuver", false); //$NON-NLS-1$
        addOption(adv, "maneuvering_ace", false); //$NON-NLS-1$
        addOption(adv, "melee_specialist", false); //$NON-NLS-1$
        addOption(adv, "pain_resistance", false); //$NON-NLS-1$
        addOption(adv, "tactical_genius", false); //$NON-NLS-1$
        addOption(adv, "weapon_specialist", new Vector<String>()); //$NON-NLS-1$
        addOption(adv, "gunnery_laser", false); //$NON-NLS-1$
        addOption(adv, "gunnery_missile", false); //$NON-NLS-1$
        addOption(adv, "gunnery_ballistic", false); //$NON-NLS-1$
        addOption(adv, "iron_man", false); //$NON-NLS-1$
        addOption(adv, "ei_implant", false); //$NON-NLS-1$
        addOption(adv, "clan_pilot_training", false); //$NON-NLS-1$
        addOption(adv, "edge", 0); //$NON-NLS-1$

        /* different edge triggers */
        addOption(adv, "edge_when_headhit", false); //$NON-NLS-1$
        addOption(adv, "edge_when_tac", false); //$NON-NLS-1$
        addOption(adv, "edge_when_ko", false); //$NON-NLS-1$
        addOption(adv, "edge_when_explosion", false); //$NON-NLS-1$

        // manei domini
        IBasicOptionGroup md = addGroup("md", MD_ADVANTAGES); //$NON-NLS-1$
        addOption(md, "vdni", false); //$NON-NLS-1$
        addOption(md, "bvdni", false); //$NON-NLS-1$
        addOption(md, "pain_shunt", false); //$NON-NLS-1$
        addOption(md, "grappler", false); //$NON-NLS-1$
        addOption(md, "pl_masc", false); //NON-NLS-1$
        //TODO: Need to add Active Probe as sensor 
        //Forum query pending at http://www.classicbattletech.com/forums/index.php/topic,47577.0.html
        addOption(md, "cyber_eye_im", false); //$NON-NLS-1$
        addOption(md, "cyber_eye_tele", false); //$NON-NLS-1$
        addOption(md, "mm_eye_im", false); //$NON-NLS-1$
        addOption(md, "comm_implant", false); //$NON-NLS-1$
        addOption(md, "boost_comm_implant", false); //$NON-NLS-1$
        addOption(md, "dermal_armor", false); //$NON-NLS-1$
        //addOption(md, "tsm_implant", false); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PilotOptionsInfo.getInstance();
    }

    private static class PilotOptionsInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new PilotOptionsInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected PilotOptionsInfo() {
            super("PilotOptionsInfo"); //$NON-NLS-1$
        }
    }
}
