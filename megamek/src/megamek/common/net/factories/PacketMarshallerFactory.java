/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.net.factories;

import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketMarshallerMethod;
import megamek.common.net.marshalling.AbstractPacketMarshaller;
import megamek.common.net.marshalling.NativeSerializationMarshaller;

public class PacketMarshallerFactory {
    //region Variable Declarations
    private static PacketMarshallerFactory instance = new PacketMarshallerFactory();
    private NativeSerializationMarshaller nativeSerializationMarshaller;
    //endregion Variable Declarations

    //region Constructors
    private PacketMarshallerFactory() {

    }
    //endregion Constructors

    public static PacketMarshallerFactory getInstance() {
        return instance;
    }

    public @Nullable
    AbstractPacketMarshaller getMarshaller(final PacketMarshallerMethod marshallingMethod) {
        switch (marshallingMethod) {
            case NATIVE_SERIALIZATION_MARSHALLING:
                if (nativeSerializationMarshaller == null) {
                    nativeSerializationMarshaller = new NativeSerializationMarshaller();
                }
                return nativeSerializationMarshaller;
            default:
                return null;
        }
    }
}
