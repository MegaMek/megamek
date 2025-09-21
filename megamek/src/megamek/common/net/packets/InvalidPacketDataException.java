/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.net.packets;

import java.io.Serial;

/**
 * This exception is thrown whenever a packet data object does not match the expected type(s) and cannot be read
 */
public class InvalidPacketDataException extends Exception {
    @Serial
    private static final long serialVersionUID = -4472736483205970853L;

    /**
     * Creates new <code>InvalidPacketDataException</code> without detail message.
     */
    public InvalidPacketDataException() {

    }

    /**
     * Constructs an <code>InvalidPacketDataException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public InvalidPacketDataException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidPacketDataException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new exception with desired object type, the object in question, and its specified index
     * @param desiredType String naming the type that was expected but not found.
     * @param objectionable Object we didn't like.
     * @param index int containing the object's position within the packet's data.
     */
    public InvalidPacketDataException(String desiredType, Object objectionable, int index) {
        this(buildMessage(desiredType, objectionable, index));
    }

    /**
     * Construct a new exception with desired object type, the object in question, and its specified index
     * @param desiredType String naming the type that was expected but not found.
     * @param objectionable Object we didn't like.
     * @param index int containing the object's position within the packet's data.
     * @param cause Throwable that initiated the exception
     */
    public InvalidPacketDataException(String desiredType, Object objectionable, int index, final Throwable cause) {
        this(buildMessage(desiredType, objectionable, index), cause);
    }

    /**
     * Construct a new message with desired object type, the object in question, and its specified index
     * @param desiredType String naming the type that was expected but not found.
     * @param objectionable Object we didn't like.
     * @param index int containing the object's position within the packet's data.
     */
    private static String buildMessage(String desiredType, Object objectionable, int index) {
        return String.format(
              "Value not %s: %s (%s)",
              desiredType,
              (objectionable == null) ? "Null object at index " + index : objectionable,
              (objectionable == null) ? "Unknown" : objectionable.getClass()
        );
    }
}
