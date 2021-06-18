package megamek.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Board;
import megamek.common.Configuration;

/** 
 * Scans all boards and applies automated tags to them.
 * Examples: Woods (Auto), DenseUrban (Auto). Deletes its own tags (and only those) 
 * so if the rules for applying tags are changed, it can also remove them. Checks
 * tags to not apply any twice, so this utility can be run as often as needed. 
 * 
 * @author Simon (Juliez)
 *
 */
public class BoardsTagger {
    
    private int numBoardErrors = 0;
    private boolean isVerbose;
    
    /**
     * Sets a value indicating whether a full listing of errors
     * should be printed when validating a board.
     * @param b {@code true} if the specific errors for each board
     *          should be printed, otherwise {@code false} for just
     *          the file name.
     */
    public void setIsVerbose(boolean b) {
        isVerbose = b;
    }

    /**
     * Recursively scans the supplied File for any boards and validates them.  If the supplied File is a directory, then
     * all files in that directory are recursively scanned.
     *
     * @param file
     * @throws IOException
     */
    private void scanForBoards(File file) throws IOException {
        if (file.isDirectory()) {
            String fileList[] = file.list();
            for (String filename : fileList) {
                File filepath = new File(file, filename);
                if (filepath.isDirectory()) {
                    scanForBoards(new File(file, filename));
                } else {
                    tagBoard(filepath);
                }
            }
        } else {
            tagBoard(file);
        }
    }

    /**
     * Check whether the supplied file is a valid board file or not.  Ignores files that don't end in .board.  
     * Any errors are logged to System.out.
     * 
     * @param boardFile
     * @throws FileNotFoundException
     */
    private void tagBoard(File boardFile) throws FileNotFoundException, IOException {
        // If this isn't a board, ignore it
        if (!boardFile.toString().endsWith(".board")) {
            return;
        }
        
        try (InputStream is = new FileInputStream(boardFile)) {
            StringBuffer errBuff = new StringBuffer();
            Board b = new Board();

            try {
                b.load(is, errBuff, false);
            } catch (Exception e) {
                errBuff.append(e.toString());
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                errBuff.append(writer.toString());
            }

            if (errBuff.length() > 0) {
                numBoardErrors++;
                System.out.println("Found Invalid Board! Board: " + boardFile);
                if (isVerbose) {
                    System.out.println(errBuff);
                }
            }
        }
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        Args a = Args.parse(args);
        if (a.showHelp) {
            System.out.println("Usage: java -cp MegaMek.jar megamek.utils.BoardsValidator [OPTIONS] [paths]");
            System.out.println();
            System.out.println("    -q, --quiet       Only print invalid file names.");
            System.out.println("    -?, -h, --help    Show this message and quit.");
            System.out.println();
            System.out.println("Examples:");
            System.out.println();
            System.out.println("Validate every board in the ./data subdirectory of the");
            System.out.println("current working directory:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator");
            System.out.println();
            System.out.println("Validate a given board:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator SomeFile.board");
            System.out.println();
            System.out.println("Validate a directory of boards:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator SomeFiles");
            System.out.println();
            System.exit(2);
            return;
        }

        var validator = new BoardsTagger();
        validator.setIsVerbose(!a.isQuiet);

        try {
            if (a.paths.size() == 0) {
                File boardDir = Configuration.boardsDir();
                validator.scanForBoards(boardDir);
            } else {
                for (String path : a.paths) {
                    validator.scanForBoards(new File(path));
                }
            }

            System.out.println("Found " + validator.numBoardErrors + " boards with errors!");
            System.exit(validator.numBoardErrors > 0 ? 1 : 0);
        } catch (IOException e) {
            System.out.println("IOException!");
            e.printStackTrace();
            System.exit(64);
        }
    }

    private static class Args {
        public boolean showHelp;
        public boolean isQuiet;
        public List<String> paths = new ArrayList<>();

        private static Args parse(String[] args) {
            Args a = new Args();
            for (String arg : args) {
                if ("-?".equals(arg) || "-h".equals(arg) || "--help".equals(arg)) {
                    a.showHelp = true;
                } else if ("-q".equals(arg) || "--quiet".equals(arg)) {
                    a.isQuiet = true;
                } else {
                    a.paths.add(arg);
                }
            }
            return a;
        }
    }

}
