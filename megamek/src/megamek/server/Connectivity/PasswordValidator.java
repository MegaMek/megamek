package megamek.server.Connectivity;

import megamek.common.annotations.Nullable;

public class PasswordValidator {
    /**
     *
     * @return valid password or null if no password or password is blank string
     */
    @Nullable
    public static String validatePassword(@Nullable String password) {
        if ((password == null) || password.isBlank()) {
            return null;
        }
        return password.trim();
    }
}
