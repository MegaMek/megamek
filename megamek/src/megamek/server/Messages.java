package megamek.server;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.EncodeControl;

public class Messages {
    private static final String BUNDLE_NAME = "megamek.server.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME,
        PreferenceManager.getClientPreferences().getLocale(), new EncodeControl());

    private Messages() {}

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch(MissingResourceException e) {
            System.out.println("Missing i18n entry: " + key); //$NON-NLS-1$
            return '!' + key + '!';
        }
    }
}
