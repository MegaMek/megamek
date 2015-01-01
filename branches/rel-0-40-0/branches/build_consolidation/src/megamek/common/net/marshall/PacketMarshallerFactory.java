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

package megamek.common.net.marshall;

public class PacketMarshallerFactory {

    private static PacketMarshallerFactory instance = new PacketMarshallerFactory();

    private NativeSerializationMarshaller nativeSerializationMarshaller;
    private XMLMarshaller XMLMarshaller;

    private PacketMarshallerFactory() {
    }

    public static PacketMarshallerFactory getInstance() {
        return instance;
    }

    public PacketMarshaller getMarshaller(int marshallingType) {
        switch (marshallingType) {
            case PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING:
                if (nativeSerializationMarshaller == null) {
                    nativeSerializationMarshaller = new NativeSerializationMarshaller();
                }
                return nativeSerializationMarshaller;
            case PacketMarshaller.XML_MARSHALING:
                if (XMLMarshaller == null) {
                    XMLMarshaller = new XMLMarshaller();
                }
                return XMLMarshaller;
            default:
                return null;
        }
    }

}
