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
package megamek.client.ui.swing.util;

import megamek.common.annotations.Nullable;

/**
 * Interface for a data verification object. Implementing classes should evaluate the value passed
 * into the {@link #verify} method to make sure it is valid according to the needs of the implementer.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/14/14 1:11 PM
 */
public interface DataVerifier {

    /**
     * Performs a verification of the given value. If the value is good, a null is returned.
     * Otherwise, a {@link String} explaining how the verification failed will be returned.
     *
     * @param value The value to be evaluated.
     * @return NULL if the value is good, otherwise a description of how the evaluation failed.
     */
    @Nullable String verify(Object value);
}
