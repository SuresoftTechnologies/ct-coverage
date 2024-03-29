package io.jenkins.plugins.ct;

import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.portlet.utils.Utils;

import org.junit.Test;

import java.math.RoundingMode;

import static org.junit.Assert.*;

/**
 * JUnit test for {@link Coverage}
 */
public class CoverageTest extends AbstractJacocoTestBase {

	@Test
    public void testPercentageCalculation() throws Exception {
        Coverage c = new Coverage(1, 2);
        assertEquals(67, c.getPercentage());
    }

	@Test
    public void testUninitialized() throws Exception {
        Coverage c = new Coverage();
        assertFalse(c.isInitialized());
    }

	@Test
    public void testAccumulateInitializes() throws Exception {
        Coverage c = new Coverage();
        c.accumulate(3, 2);
        assertTrue(c.isInitialized());
    }

	@Test
    public void testNormalConstructorInitializes() throws Exception {
        Coverage c = new Coverage(1, 2);
        assertTrue(c.isInitialized());
    }

	@Test
    public void testVacuousCoverage() throws Exception {
        final Coverage c = new Coverage(0, 0);
        assertEquals(100, c.getPercentage());
    }

	@Test
    public void testHalfCoverage() throws Exception {
        Coverage c = new Coverage(1, 1);
        assertEquals(50, c.getPercentage());

        c = new Coverage(12, 12);
        assertEquals(50, c.getPercentage());
    }

	@Test
    public void testFullCoverage() throws Exception {
        Coverage c = new Coverage(0, 1);
        assertEquals(100, c.getPercentage());

        c = new Coverage(0, 12);
        assertEquals(100, c.getPercentage());
    }

    @Test
    public void testStringRepresentation() {
	    int coverageInt = 67;
	    float coverageDouble = 2f/3;
	    assertEquals(0.666666667, coverageDouble, 0.000001);

        //noinspection StringBufferReplaceableByString
        StringBuilder buf = new StringBuilder();
	    buf.append(coverageInt).append("/")
                .append(coverageDouble).append("/")
                .append(Utils.roundFloat(6, RoundingMode.HALF_EVEN, 100*coverageDouble));
	    assertEquals("67/0.6666667/66.66667", buf.toString());
    }
}