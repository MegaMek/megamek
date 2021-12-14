package megamek.client.ui.swing;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a UI Theme for Swing.
 */
public class UITheme {
    private final String name;
    private final String className;

    /**
     * Creates a new {@code UITheme} class.
     * @param className The class name to load the UI Theme.
     */
    public UITheme(String className) {
        this(className, className);
    }

    /**
     * Creates a new {@code UITheme} class.
     * @param name The optional name of the UI Theme.
     * @param className The class name to load the UI Theme.
     */
    public UITheme(String className, String name) {
        this.className = Objects.requireNonNull(className);
        this.name = Optional.ofNullable(name).orElse(className);
    }

    /**
     * Gets the name of the theme.
     * @return The name of the theme.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the class name to load the theme with.
     * @return The class name of the theme.
     */
    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || other.getClass() != getClass()) {
            return false;
        } else {
            return getClassName().equals(((UITheme) other).getClassName());
        }
    }
}
