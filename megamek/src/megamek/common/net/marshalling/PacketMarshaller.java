/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.net.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import megamek.common.annotations.Nullable;
import megamek.common.net.packets.Packet;
import megamek.logging.MMLogger;

/**
 * Generic marshaller that [un]marshalls the <code>Packet</code>
 */
public abstract class PacketMarshaller {
    private static final MMLogger logger = MMLogger.create(PacketMarshaller.class);

    /**
     * Java native serialization marshalling
     */
    public static final int NATIVE_SERIALIZATION_MARSHALING = 0;

    /**
     * Marshalls the packet data into the <code>byte[]</code>
     *
     * @param packet packet to marshall
     *
     * @return marshalled representation of the given <code>Packet</code>
     */
    public @Nullable byte[] marshall(Packet packet) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            marshall(packet, bo);
            bo.flush();
            return bo.toByteArray();
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }

    /**
     * Marshalls the packet data into the given <code>OutputStream</code>
     *
     * @param packet packet to marshall
     * @param stream <code>OutputStream</code> to marshall the
     *               <code>Packet</code> to
     *
     * @throws Exception
     */
    public abstract void marshall(Packet packet, OutputStream stream) throws Exception;

    /**
     * Unmarshalls the packet data from the given <code>byte[]</code> array
     *
     * @param data <code>byte[]</code> array to unmarshall the packet from
     *
     * @return the new <code>Packet</code>unmarshalled from the given
     *       <code>byte[]</code> array
     */
    public @Nullable Packet unmarshall(byte... data) {
        try {
            return unmarshall(new ByteArrayInputStream(data));
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }

    /**
     * Unmarshalls the packet data from the given <code>InputStream</code>
     *
     * @param stream <code>InputStream</code> to unmarshall the packet from
     *
     * @return the new <code>Packet</code>unmarshalled from the given
     *       <code>InputStream</code>
     *
     * @throws Exception
     */
    public abstract Packet unmarshall(InputStream stream) throws Exception;
}
