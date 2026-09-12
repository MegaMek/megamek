package megamek.scratch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.util.SerializationHelper;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.mockito.Mockito;

/** Loads a saved game outside a running server, for investigating bug reports. */
final class SaveProbe {

    private SaveProbe() {}

    static Game load(String path) throws Exception {
        EquipmentType.initializeTypes();
        installStubServer();
        File save = new File(path);
        try (InputStream in = path.endsWith(".gz")
              ? new GZIPInputStream(new FileInputStream(save))
              : new FileInputStream(save)) {
            return (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(in);
        }
    }

    private static void installStubServer() throws Exception {
        Field field = Server.class.getDeclaredField("serverInstance");
        field.setAccessible(true);
        if (field.get(null) != null) {
            return;
        }
        Server stub = Mockito.mock(Server.class);
        Mockito.when(stub.getGameManager()).thenReturn(Mockito.mock(TWGameManager.class));
        field.set(null, stub);
    }
}
