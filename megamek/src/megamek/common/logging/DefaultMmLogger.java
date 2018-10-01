/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.logging;

/**
 * This class is being phased out.
 * <p/>
 * For your logging needs, please use log4j2 directly.
 */
public class DefaultMmLogger {

    /**
     * @return the MMLogger singleton (which is stateless)
     */
    public static MMLogger getInstance() {
        return MMLogger.INSTANCE;
    }

    private DefaultMmLogger() {
        // no instances: this class only exists for backwards compatibility
    }

}
