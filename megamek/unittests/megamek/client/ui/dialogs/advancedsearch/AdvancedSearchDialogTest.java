package megamek.client.ui.dialogs.advancedsearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedSearchDialogTest {

    @Test
    void testSearchesShouldParseWithoutException() throws IOException, URISyntaxException {
        // Tests the searches in the testresources/searches folder
        Path resourceDir = Path.of(getClass().getClassLoader().getResource("searches").toURI());

        try (Stream<Path> files = Files.walk(resourceDir)) {
            files.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().endsWith(".json"))
                  .forEach(p ->
                        assertDoesNotThrow(
                              () -> AdvancedSearchDialog.load(p.toFile()),
                              () -> "Failed for JSON file: " + p
                        )
                  );
        }
    }
}



