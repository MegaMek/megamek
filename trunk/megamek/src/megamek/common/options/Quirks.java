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
 * Contains the options determining quirks of the unit
 * 
 * @author Taharqa (Jay Lawson)
 */
public class Quirks extends AbstractOptions {
    private static final long serialVersionUID = 7618380522964885740L;
    public static final String POS_QUIRKS = "PosQuirks"; //$NON-NLS-1$
    public static final String NEG_QUIRKS = "NegQuirks"; //$NON-NLS-1$
  
    public Quirks() {
        super();
    }

    @Override
    public void initialize() {
        //positive quirks
        IBasicOptionGroup posQuirk = addGroup("pos_quirks", POS_QUIRKS); //$NON-NLS-1$
        addOption(posQuirk, "anti_air", false); //$NON-NLS-1$
        addOption(posQuirk, "atmo_flyer", false); //$NON-NLS-1$
        addOption(posQuirk, "battle_computer", false); //$NON-NLS-1$
        addOption(posQuirk, "combat_computer", false); //$NON-NLS-1$
        addOption(posQuirk, "command_mech", false); //$NON-NLS-1$
        addOption(posQuirk, "easy_pilot", false); //$NON-NLS-1$
        addOption(posQuirk, "ext_twist", false); //$NON-NLS-1$
        addOption(posQuirk, "low_profile", false); //$NON-NLS-1$
        addOption(posQuirk, "hyper_actuator", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_life_support", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_short", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_med", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_long", false); //$NON-NLS-1$
        addOption(posQuirk, "pro_actuator", false); //$NON-NLS-1$
        addOption(posQuirk, "stable", false); //$NON-NLS-1$
     
        // negative quirks
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS); //$NON-NLS-1$
        addOption(negQuirk, "atmo_instability", false); //$NON-NLS-1$
        addOption(negQuirk, "cramped_cockpit", false); //$NON-NLS-1$
        addOption(negQuirk, "difficult_eject", false); //$NON-NLS-1$
        addOption(negQuirk, "exp_actuator", false); //$NON-NLS-1$
        addOption(negQuirk, "fragile_fuel", false); //$NON-NLS-1$
        addOption(negQuirk, "hard_pilot", false); //$NON-NLS-1$
        addOption(negQuirk, "no_arms", false); //$NON-NLS-1$
        addOption(negQuirk, "no_eject", false); //$NON-NLS-1$
        addOption(negQuirk, "no_twist", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_life_support", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_short", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_med", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_long", false); //$NON-NLS-1$
        addOption(negQuirk, "unbalanced", false); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return QuirksInfo.getInstance();
    }

    private static class QuirksInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new QuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected QuirksInfo() {
            super("QuirksInfo"); //$NON-NLS-1$
        }
    }
}
