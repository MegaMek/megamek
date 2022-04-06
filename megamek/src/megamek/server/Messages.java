package megamek.server;

import megamek.MegaMek;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.server.messages",
        MegaMek.getMMOptions().getLocale(), new EncodeControl());

    private Messages() {
        
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LogManager.getLogger().error("Missing i18n entry with key " + key);
            return '!' + key + '!';
        }
    }
}
