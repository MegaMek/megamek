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

import java.util.Vector;

/**
 * Contains the options determining abilities of the pilot
 *
 * @author Cord
 */
public class PilotOptions extends AbstractOptions {
    private static final long serialVersionUID = 6628080570425023949L;
    public static final String LVL3_ADVANTAGES = "lvl3Advantages"; //$NON-NLS-1$
    public static final String EDGE_ADVANTAGES = "edgeAdvantages"; //$NON-NLS-1$
    public static final String MD_ADVANTAGES = "MDAdvantages"; //$NON-NLS-1$

    public PilotOptions() {
        super();
    }

    @Override
    public void initialize() {
        IBasicOptionGroup adv = addGroup("adv", LVL3_ADVANTAGES); //$NON-NLS-1$
        
        // Piloting Abilities - Just uncomment and run with it
        // addOption(adv, "animal_mimic", false); //$NON-NLS-1$
        // addOption(adv, "cross_country", false); //$NON-NLS-1$
        addOption(adv, "dodge_maneuver", false); //$NON-NLS-1$
        // addOption(adv, "dust_off", false); //$NON-NLS-1$
        // addOption(adv, "hvy_lifter", false); //$NON-NLS-1$
        // addOption(adv, "hopper", false); //$NON-NLS-1$
        addOption(adv, "hopping_jack", false); //$NON-NLS-1$
        addOption(adv, "hot_dog", false); //$NON-NLS-1$
        addOption(adv, "jumping_jack", false); //$NON-NLS-1$
        addOption(adv, "maneuvering_ace", false); //$NON-NLS-1$
        addOption(adv, "melee_master", false); //$NON-NLS-1$
        addOption(adv, "melee_specialist", false); //$NON-NLS-1$
        // addOption(adv, "natural_grace", false); //$NON-NLS-1$
        // addOption(adv, "ride_wash", false); //$NON-NLS-1$
        addOption(adv, "shaky_stick", false); //$NON-NLS-1$
        // addOption(adv, "slugger", false); //$NON-NLS-1$
        // addOption(adv, "speed_demon", false); //$NON-NLS-1$
        // addOption(adv, "stand_aside", false); //$NON-NLS-1$
        // addOption(adv, "swordsman", false); //$NON-NLS-1$
        // addOption(adv, "tm_", false); //$NON-NLS-1$
        // addOption(adv, "wind_walker", false); //$NON-NLS-1$
        // addOption(adv, "zweihander", false); //$NON-NLS-1$
        
        // Gunnery Abilities
        // addOption(adv, "blood_stalker", false); //$NON-NLS-1$
        addOption(adv, "cluster_hitter", false); //$NON-NLS-1$
        addOption(adv, "cluster_master", false); //$NON-NLS-1$
        // addOption(adv, "fist_fire", false); //$NON-NLS-1$
        addOption(adv, "golden_goose", false); //$NON-NLS-1$
        // addOption(adv, "ground_hugger", false); //$NON-NLS-1$
        addOption(adv, "specialist", new Vector<String>()); //$NON-NLS-1$
        // addOption(adv, "marksman", false); //$NON-NLS-1$
        addOption(adv, "multi_tasker", false); //$NON-NLS-1$
        addOption(adv, "oblique_artillery", false); //$NON-NLS-1$
        addOption(adv, "oblique_attacker", false); //$NON-NLS-1$
        // addOption(adv, "range_master", false); //$NON-NLS-1$
        addOption(adv, "sandblaster", false); //$NON-NLS-1$
        // addOption(adv, "sharpshooter", false); //$NON-NLS-1$
        addOption(adv, "sniper", false); //$NON-NLS-1$
        addOption(adv, "weapon_specialist", new Vector<String>()); //$NON-NLS-1$
        
        // Misc Abilities
        // addOption(adv, "antagonizer", false); //$NON-NLS-1$
        // addOption(adv, "combat_intuition", false); //$NON-NLS-1$
        // addOption(adv, "demoralizer", false); //$NON-NLS-1$
        addOption(adv, "eagle_eyes", false); //$NON-NLS-1$
        // addOption(adv, "env_specialist", false); //$NON-NLS-1$
        addOption(adv, "forward_observer", false); //$NON-NLS-1$
        // addOption(adv, "human_tro", false); //$NON-NLS-1$
        addOption(adv, "iron_man", false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_APTITUDE_GUNNERY, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_APTITUDE_PILOTING, false); //$NON-NLS-1$
        addOption(adv, "pain_resistance", false); //$NON-NLS-1$
        addOption(adv, "tactical_genius", false); //$NON-NLS-1$

        // Infantry abilities - Only one until beast mounts are implemented
        addOption(adv, "foot_cav", false); //$NON-NLS-1$
        addOption(adv, "urban_guerrilla", false); //$NON-NLS-1$

        // Unofficial
        addOption(adv, "ei_implant", false); //$NON-NLS-1$
        addOption(adv, "gunnery_laser", false); //$NON-NLS-1$
        addOption(adv, "gunnery_missile", false); //$NON-NLS-1$
        addOption(adv, "gunnery_ballistic", false); //$NON-NLS-1$
        addOption(adv, "clan_pilot_training", false); //$NON-NLS-1$
        addOption(adv, "some_like_it_hot", false); //$NON-NLS-1$
        addOption(adv, "weathered", false); //$NON-NLS-1$
        addOption(adv, "allweather", false); //$NON-NLS-1$
        addOption(adv, "blind_fighter", false); //$NON-NLS-1$
        addOption(adv, "sensor_geek", false); //$NON-NLS-1$


        IBasicOptionGroup edge = addGroup("edge", EDGE_ADVANTAGES); //$NON-NLS-1$
        addOption(edge, "edge", 0); //$NON-NLS-1$
        /* different edge triggers */
        addOption(edge, "edge_when_headhit", false); //$NON-NLS-1$
        addOption(edge, "edge_when_tac", false); //$NON-NLS-1$
        addOption(edge, "edge_when_ko", false); //$NON-NLS-1$
        addOption(edge, "edge_when_explosion", false); //$NON-NLS-1$
        addOption(edge, "edge_when_masc_fails", false); //$NON-NLS-1$

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
        addOption(md, "tsm_implant", false); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PilotOptionsInfo.getInstance();
    }

    private static class PilotOptionsInfo extends AbstractOptionsInfo {
        private static boolean initliazed = false;
        private static AbstractOptionsInfo instance = new PilotOptionsInfo();

        public static AbstractOptionsInfo getInstance() {
            if (!initliazed) {
                initliazed = true;
                // Create a new dummy PilotOptions; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new PilotOptions();
            }
            return instance;
        }

        protected PilotOptionsInfo() {
            super("PilotOptionsInfo"); //$NON-NLS-1$
        }
    }
}
