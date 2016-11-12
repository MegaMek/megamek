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

        // addOption(adv, OptionsConstants.PILOT_ANIMAL_MIMIC, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_CROSS_COUNTRY, false);
        // //$NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_DODGE_MANEUVER, false); // $NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_DUST_OFF, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_HVY_LIFTER, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_HOPPER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_HOPPING_JACK, false); // $NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_HOT_DOG, false); // $NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_JUMPING_JACK, false); // $NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_MANEUVERING_ACE, false); // $NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_MELEE_MASTER, false); // $NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_MELEE_SPECIALIST, false); // $NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_NATURAL_GRACE, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_RIDE_WASH, false);
        // //$NON-NLS-1$
        addOption(adv, OptionsConstants.PILOT_SHAKY_STICK, false); // $NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_SLUGGER, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_SPEED_DEMON, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_STAND_ASIDE, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_SWORDSMAN, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_TM_, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_WIND_WALKER, false);
        // //$NON-NLS-1$
        // addOption(adv, OptionsConstants.PILOT_ZWEIHANDER, false);
        // //$NON-NLS-1$

        // Gunnery Abilities
        // addOption(adv, OptionsConstants.GUNNERY_BLOOD_STALKER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_HITTER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_MASTER, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.GUNNERY_FIST_FIRE, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_GOLDEN_GOOSE, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.GUNNERY_GROUND_HUGGER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_SPECIALIST, new Vector<String>()); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.GUNNERY_MARKSMAN, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_MULTI_TASKER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_RANGE_MASTER,  new Vector<String>()); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_SANDBLASTER, false); //$NON-NLS-1$
        // addOption(adv, OptionsConstants.GUNNERY_SHARPSHOOTER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_SNIPER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.GUNNERY_WEAPON_SPECIALIST, new Vector<String>()); //$NON-NLS-1$

         // Misc Abilities
         // addOption(adv, OptionsConstants.MISC_ANTAGONIZER, false); //$NON-NLS-1$
         // addOption(adv, OptionsConstants.MISC_COMBAT_INTUITION, false); //$NON-NLS-1$
         // addOption(adv, OptionsConstants.MISC_DEMORALIZER, false); //$NON-NLS-1$
         addOption(adv, OptionsConstants.MISC_EAGLE_EYES, false); //$NON-NLS-1$
         // addOption(adv, OptionsConstants.MISC_ENV_SPECIALIST, false); //$NON-NLS-1$
         addOption(adv, OptionsConstants.MISC_FORWARD_OBSERVER, false); //$NON-NLS-1$
         // addOption(adv, OptionsConstants.MISC_HUMAN_TRO, false); //$NON-NLS-1$
         addOption(adv, OptionsConstants.MISC_IRON_MAN, false); //$NON-NLS-1$
         addOption(adv, OptionsConstants.MISC_PAIN_RESISTANCE, false); //$NON-NLS-1$
         addOption(adv, OptionsConstants.MISC_TACTICAL_GENIUS, false); //$NON-NLS-1$

        // Infantry abilities - Only one until beast mounts are implemented
        addOption(adv, OptionsConstants.INFANTRY_FOOT_CAV, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.INFANTRY_URBAN_GUERRILLA, false); //$NON-NLS-1$

        // Unofficial      
        addOption(adv, OptionsConstants.UNOFF_EI_IMPLANT, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_LASER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_MISSILE, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_BALLISTIC, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_CLAN_PILOT_TRAINING, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_SOME_LIKE_IT_HOT, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_WEATHERED, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_ALLWEATHER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_BLIND_FIGHTER, false); //$NON-NLS-1$
        addOption(adv, OptionsConstants.UNOFF_SENSOR_GEEK, false); //$NON-NLS-1$

        IBasicOptionGroup edge = addGroup("edge", EDGE_ADVANTAGES); //$NON-NLS-1$
        addOption(edge, "edge", 0); //$NON-NLS-1$
        /* different edge triggers */
        addOption(edge, "edge_when_headhit", false); //$NON-NLS-1$
        addOption(edge, "edge_when_tac", false); //$NON-NLS-1$
        addOption(edge, "edge_when_ko", false); //$NON-NLS-1$
        addOption(edge, "edge_when_explosion", false); //$NON-NLS-1$
        addOption(edge, "edge_when_masc_fails", false); //$NON-NLS-1$
        
        addOption(edge, OptionsConstants.EDGE, 0); //$NON-NLS-1$
        //different edge triggers 
        addOption(edge, OptionsConstants.EDGE_WHEN_HEADHIT, false); //$NON-NLS-1$
        addOption(edge, OptionsConstants.EDGE_WHEN_TAC, false); //$NON-NLS-1$
        addOption(edge, OptionsConstants.EDGE_WHEN_KO, false); //$NON-NLS-1$
        addOption(edge, OptionsConstants.EDGE_WHEN_EXPLOSION, false); //$NON-NLS-1$
        addOption(edge, OptionsConstants.EDGE_WHEN_MASC_FAILS, false); //$NON-NLS-1$

        // manei domini
        IBasicOptionGroup md = addGroup("md", MD_ADVANTAGES); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_PAIN_SHUNT, false); // $NON-NLS-1$
        addOption(md, OptionsConstants.MD_COMM_IMPLANT, false); // $NON-NLS-1$
        //TODO - -1 bonus when spotting for LRMs and moving through mines.
        addOption(md, OptionsConstants.MD_BOOST_COMM_IMPLANT, false); // $NON-NLS-1$
        //TODO - -1 bonus when spotting for LRMs and moving through mines. 

        addOption(md, OptionsConstants.MD_CYBER_IMP_AUDIO, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_CYBER_IMP_VISUAL, false); //$NON-NLS-1$

        addOption(md, OptionsConstants.MD_CYBER_IMP_LASER, false); //$NON-NLS-1$
  
        addOption(md, OptionsConstants.MD_MM_IMPLANTS, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_ENH_MM_IMPLANTS, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_FILTRATION, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_GAS_EFFUSER_PHERO, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_GAS_EFFUSER_TOXIN, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_DERMAL_ARMOR, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_DERMAL_CAMO_ARMOR, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_TSM_IMPLANT, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, false); //$NON-NLS-1$  
        addOption(md, OptionsConstants.MD_VDNI, false); // $NON-NLS-1$
        addOption(md, OptionsConstants.MD_BVDNI, false); // $NON-NLS-1$
        //Prosthetic Limbs (not MD Exclusive)
        addOption(md, OptionsConstants.MD_PL_ENHANCED, false); //$NON-NLS-1$
        addOption(md, OptionsConstants.MD_PL_MASC, false); // NON-NLS-1$
        //TODO - Prototype DNI IO pg 83
        //SUICIDE CHARGE IO pg 83

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
