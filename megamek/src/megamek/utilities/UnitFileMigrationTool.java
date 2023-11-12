package megamek.utilities;

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitRoleHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class UnitFileMigrationTool {

    public static void main(String... args) throws IOException {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);
        MechSummary[] units = cache.getAllMechs();
        for (MechSummary unit : units) {
            File file = unit.getSourceFile();
            if (file.toString().toLowerCase().endsWith(".mtf")) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int line = 0;
                for (; line < lines.size(); line++) {
                    if (lines.get(line).toLowerCase().startsWith("rules level")) {
                        break;
                    }
                }

                lines.add(line+1, "role:"+ UnitRoleHandler.getRoleFor(unit));
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            }



        }
    }
}