package megamek.client.ui.dialogs.advancedsearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RecentFilesStoreTest {

    @Test
    void testConstructWithNullThrowsException() {
        // Constructing the store with a null file should not be allowed
        assertThrows(IllegalArgumentException.class, () -> new RecentFilesStore(null));
    }

    @Test
    void testConstructWithNonNullDoesNotThrowException() {
        // Constructing the store with a non-existent file is valid!! The file and parent directories will be created
        // when a recent entry is "touched"
        final Path nonexistentPath = Path.of("Unobtainium.json");
        assertFalse(Files.exists(nonexistentPath), "this test file should not exist for the test to work");
        assertDoesNotThrow(() -> new RecentFilesStore(nonexistentPath));
    }

    @Test
    void testNonexistentIsEmpty() throws IOException {
        // A non-existent file should not yield entries
        final Path nonexistentPath = Path.of("Unobtainium.json");
        assertFalse(Files.exists(nonexistentPath), "this test file should not exist for the test to work");
        RecentFilesStore store = new RecentFilesStore(nonexistentPath);
        assertTrue(store.getRecentFiles().isEmpty(), "recent files for a nonexistent store should be empty");
    }

    @Test
    void testMalformedThrows() throws URISyntaxException {
        // Bad json should throw an IOException
        Path resourceDir = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("searches")).toURI());
        final Path malformedFile = Path.of(String.valueOf(resourceDir), "malformed-recent-files-store.json");
        assertThrows(IOException.class, () -> new RecentFilesStore(malformedFile));
    }

    @Test
    void testWellformed() throws URISyntaxException, IOException {
        // good json should give results.
        Path resourceDir = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("searches")).toURI());
        final Path wellformedFile = Path.of(String.valueOf(resourceDir), "wellformed-recent-files-store.json");
        RecentFilesStore store = new RecentFilesStore(wellformedFile);
        assertEquals(8, store.getRecentFiles().size(), "should have 8 results");
        // Not testing MAXENTRIES, value is arbitrary and unimportant
        store.touch(store.getRecentFiles().get(0));
        assertEquals(8, store.getRecentFiles().size(), "should still have 8 results");
        Path last =  store.getRecentFiles().get(store.getRecentFiles().size() - 1);
        store.touch(last); // will change the tested file, but it doesn't matter
        assertEquals(last, store.getRecentFiles().get(0), "last entry should have moved up to first");
    }
}
