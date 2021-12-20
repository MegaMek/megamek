package megamek.server;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

public class Messages {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.server.messages",
        PreferenceManager.getClientPreferences().getLocale(), new EncodeControl());

    private Messages() {}

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LogManager.getLogger().error("Missing i18n entry with key " + key);
            return '!' + key + '!';
        }
    }
}
