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
