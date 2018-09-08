package megamek.common.logging;

import java.net.URL;
import java.util.NoSuchElementException;

/**
 * A collection of utility methods dealing with java technicalities
 */
public class Technicalities {

    private Technicalities() {
        // no instances
    }

    /**
     * Determines whether the given {@linkplain Class} has been loaded from a
     * <tt>.jar</tt> file. Will always return {@code true} when called with
     * classes loaded by the system classloader.
     */
    public static boolean wasLoadedFromJar(Class<?> c) {

        // An arguably more elegant solution would be to use:
        //
        //      c.getProtectionDomain().getCodeSource()
        //
        // but, depending on our permissions, c.getProtectionDomain() could
        // return null.
        //
        // Since we are a standalone app, we shouldn't run into such problems,
        // but it seems nonetheless more reasonable to go the naive way instead.

        ClassLoader cl = c.getClassLoader();
        if (cl == null) {
            // Then it was the system classloader - let's just return true.
            return true;
        } else {
            String name = c.getName().replace('.', '/') + ".class"; //$NON-NLS-1$
            URL url = cl.getResource(name);
            assert url != null;
            return "jar".equals(url.getProtocol()); //$NON-NLS-1$
        }
    }

    /**
     * Returns the {@linkplain StackTraceElement} that is one level below the
     * caller in the stack trace.
     *
     * @throws NoSuchElementException
     *         if called from a function at the bottom of the stack
     *         (eg: from {@code public static void main(String[] args)})
     */
    public static StackTraceElement getCaller() throws NoSuchElementException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0 is getStackTrace(), 1 is us, 2 is our caller
        if (stackTrace.length < 4) {
            throw new NoSuchElementException();
        }
        return stackTrace[3];
    }

    /**
     * Returns the first {@linkplain StackTraceElement} in the stack trace
     * that is outside the caller's class.
     *
     * @throws NoSuchElementException
     *         if there is none (eg: if this method is called from the same
     *         class where {@code public static void main(String[] args)}) is
     */
    public static StackTraceElement getFirstCallerOutsideClass() throws IllegalStateException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0 is getStackTrace(), 1 is us, 2 is our caller
        if (stackTrace.length < 4) {
            throw new NoSuchElementException();
        }
        String callerClassName = stackTrace[2].getClassName();
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement ste = stackTrace[i];
            if (!callerClassName.equals(ste.getClassName())) {
                return ste;
            }
        }
        throw new NoSuchElementException();
    }

}
