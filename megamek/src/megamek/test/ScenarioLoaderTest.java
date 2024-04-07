package megamek.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.common.Game;
import megamek.common.MechSummaryCache;
import megamek.server.gameManager.GameManager;
import megamek.common.scenario.ScenarioLoader;
import megamek.server.Server;

public class ScenarioLoaderTest {
    private List<String> errCache = new ArrayList<>();
    private PrintStream cachedPs;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    public static void main(String[] args) {
        ScenarioLoaderTest tester = new ScenarioLoaderTest();
        tester.runTests();
        System.exit(0);
    }
    
    public List<String> runTests() {
        List<String> errorAccumulator = new ArrayList<>();
        PrintStream nullPs = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // Output nothing
            }
        });
        originalOut = System.out;
        System.setOut(nullPs);
        cachedPs = new PrintStream(new OutputStream() {
            private StringBuilder line = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String s = line.toString();
                    if (!s.startsWith("MMRandom: generating RNG")) {
                        errCache.add(s);
                    }
                    line.setLength(0);
                } else if (b != '\r') {
                    line.append((char) b);
                }
            }
        });
        originalErr = System.err;
        System.setErr(cachedPs);

        // Wait for MSC (we have to wait anyway, better to do it once if we want to measure)
        MechSummaryCache msc = MechSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
        }
        
        File baseDir = new File("data/scenarios");
        checkScenarioFile(baseDir, errorAccumulator);
        System.setOut(originalOut);
        System.setErr(originalErr);
        cachedPs.close();
        nullPs.close();
        return errorAccumulator;
    }
    
    private void checkScenarioFile(File file, List<String> errorAccumulator) {
        int port = 7770;
        if (null == file) {
            return;
        }
        if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".mms")) {
            ScenarioLoader loader = new ScenarioLoader(file);
            try {
                Game game = loader.createGame();
                GameManager gameManager = new GameManager();
                Server server = new Server("test", port + 1, gameManager);
                server.setGame(game);
                loader.applyDamage(gameManager);
                server.die();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            if (!errCache.isEmpty()) {
                errorAccumulator.add("ERROR in " + file.getPath());
                originalErr.println("ERROR in " + file.getPath());
                for (String line : errCache) {
                    errorAccumulator.add(line);
                    originalErr.println(line);
                }
                errCache.clear();
            }
        } else if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                checkScenarioFile(subFile, errorAccumulator);
            }
        }
    }
}
