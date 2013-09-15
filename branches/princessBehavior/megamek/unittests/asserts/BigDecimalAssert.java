package asserts;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/15/13 12:20 AM
 */
public class BigDecimalAssert {
    public static void assertEquals(BigDecimal expected, Object actual, int delta) {
        TestCase.assertNotNull(actual);
        TestCase.assertTrue("d2: " + actual.getClass().getName(), actual instanceof BigDecimal);
        BigDecimal roundedExpected = expected.setScale(delta, RoundingMode.FLOOR);
        BigDecimal roundedActual = ((BigDecimal)actual).setScale(delta, RoundingMode.FLOOR);
        TestCase.assertEquals(roundedExpected, roundedActual);
    }
}
