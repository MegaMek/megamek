package megamek.common.loaders;

import megamek.common.MechSummaryCache;
import megamek.common.util.fileUtils.MegaMekFile;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class CacheRebuildTest {
    /**
     * Tests that every single unit can load successfully
     */
    @Test
    public void testCacheRebuild() {
        File file = new MegaMekFile(MechSummaryCache.getUnitCacheDir(),
            MechSummaryCache.FILENAME_UNITS_CACHE).getFile();
        if (file.exists()) {
            // Ensure the cache really is cleared
            assertTrue(file.delete());
        }
        MechSummaryCache instance = MechSummaryCache.getInstance(true);
        // Make sure no units failed loading
        assertTrue(instance.getFailedFiles().isEmpty());
        // Sanity check to make sure the loader thread didn't fail outright
        assertTrue(instance.getAllMechs().length > 100);
    }
}
