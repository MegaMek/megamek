/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.debug;

/**
 * A set of assert methods.
 */
public class Assert {

    /**
     * Don't allow creating instances, since it's static class
     */
    protected Assert() {
    }

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * AssertionFailedError.
     */
    public static void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * AssertionFailedError with the given message.
     */
    public static void assertTrue(boolean condition, String message) {
        if (!condition)
            fail(message);
    }

    /**
     * Fails with no message.
     */
    public static void fail() {
        fail(null);
    }

    /**
     * Fails with the given message.
     */
    public static void fail(String message) {
        throw new AssertionFailedError(message);
    }

}
