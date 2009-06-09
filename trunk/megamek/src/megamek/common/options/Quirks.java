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
        addOption(posQuirk, "easy_pilot", false); //$NON-NLS-1$
        addOption(posQuirk, "low_profile", false); //$NON-NLS-1$
        addOption(posQuirk, "hyper_actuator", false); //$NON-NLS-1$
     
        // negative quirks
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS); //$NON-NLS-1$
        addOption(negQuirk, "hard_pilot", false); //$NON-NLS-1$
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
