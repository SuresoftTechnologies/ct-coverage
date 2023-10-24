package io.jenkins.plugins.ct;

import static org.junit.Assert.*;

import io.jenkins.plugins.ct.model.Coverage;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractJacocoTestBase {
    protected final void assertRatio(Coverage r, float numerator, float denominator) {
        fail("Replaced by assertCoverage()");
    }
    
    protected final void assertCoverage(Coverage coverage, int missed, int covered) {
        assertEquals(missed + "/" + covered, coverage.getMissed() + "/" + coverage.getCovered());
    }
}
