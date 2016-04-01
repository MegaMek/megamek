package megamek.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.common.IGame;
import megamek.server.ScenarioLoader;
import megamek.server.Server;

public class ScenarioLoadierTest {
    private List<String> errCache = new ArrayList<>();
    private PrintStream cachedPs;
    private PrintStream originalErr;
    
    public static void main(String[] args) {
        ScenarioLoadierTest tester = new ScenarioLoadierTest();
        tester.runTests();
    }
    
    @SuppressWarnings("resource")
    public void runTests() {
        PrintStream nullPs = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // Output nothing
            }
        });
        System.setOut(nullPs);
        cachedPs = new PrintStream(new OutputStream() {
            private StringBuilder line = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                if(b == '\n') {
                    String s = line.toString();
                    if(!s.startsWith("MMRandom: generating RNG")) { //$NON-NLS-1$
                        errCache.add(s);
                    }
                    line.setLength(0);
                } else if(b != '\r') {
                    line.append((char) b);
                }
            }
        });
        originalErr = System.err;
        System.setErr(cachedPs);
        File baseDir = new File("data/scenarios"); //$NON-NLS-1$
        checkScenarioFile(baseDir);
        System.setErr(originalErr);
        cachedPs.close();
        nullPs.close();
    }
    
    private void checkScenarioFile(File file) {
        int port = 7770;
        if(null == file) {
            return;
        }
        if(file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".mms")) { //$NON-NLS-1$
            ScenarioLoader loader = new ScenarioLoader(file);
            try {
                IGame game = loader.createGame();
                Server server = new Server("test", port ++); //$NON-NLS-1$
                server.setGame(game);
                loader.applyDamage(server);
                server.die();
            } catch(Exception e) {
                e.printStackTrace();
            }
            
            if(errCache.size() > 0) {
                originalErr.println("ERROR in " + file.getPath()); //$NON-NLS-1$
                for(String line : errCache) {
                    originalErr.println(line);
                }
                errCache.clear();
            }
        } else if(file.isDirectory()) {
            for(File subFile : file.listFiles()) {
                checkScenarioFile(subFile);
            }
        }
    }
}
