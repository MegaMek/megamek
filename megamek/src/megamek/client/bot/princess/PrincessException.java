/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since: 8/17/13 11:03 PM
 */
public class PrincessException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -657543995772098106L;

    public PrincessException() {
        super();
    }

    public PrincessException(Throwable cause) {
        super(cause);
    }

    public PrincessException(String message) {
        super(message);
    }

    public PrincessException(String message, Throwable cause) {
        super(message, cause);
    }
}
