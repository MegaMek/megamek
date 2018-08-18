/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;

public class TestUtilities {

    private TestUtilities() {
        // no instances
    }

    public static void checkSerializable(Object o) {

        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(o);
            }

            serialized = baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Serialization failed", e); //$NON-NLS-1$
        }

        Object clone;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            clone = ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new AssertionError("Deserialization failed", e); //$NON-NLS-1$
        }

        Assert.assertEquals(o, clone);
    }

}
