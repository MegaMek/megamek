package megamek.common.util;

public class DiscordExportUtil {
    private DiscordExportUtil() {}

    private static final char esc = '\u001b';

    public static enum DiscordFormat {
        GRAY(30),
        RED(31),
        GREEN(32),
        YELLOW(33),
        BLUE(34),
        PINK(35),
        CYAN(36),
        WHITE(37),

        BOLD(1),
        UNDERLINE(4),

        RESET(0);

        private int code;
        DiscordFormat(int code) {
            this.code = code;
        }

        public String format() {
            return esc + "[" + code + 'm';
        }
    }
}
