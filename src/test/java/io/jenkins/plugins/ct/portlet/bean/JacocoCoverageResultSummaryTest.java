package io.jenkins.plugins.ct.portlet.bean;

import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.plugins.ct.portlet.bean.CTCoverageResultSummary;

import org.junit.Test;

import java.util.Collections;
import java.util.SortedMap;

import static org.junit.Assert.*;

public class JacocoCoverageResultSummaryTest {
    @Test
    public void testCoverageSetterGetter() throws Exception {
        CTCoverageResultSummary summary = new CTCoverageResultSummary();
        summary.setBranchCoverage(23.4f);
        summary.setComplexityScore(23.6f);
        summary.setLineCoverage(23.8f);
        summary.setMethodCoverage(23.9f);

        assertEquals(23.4f, summary.getBranchCoverage(), 0.01);
        assertEquals(23.6f, summary.getMCDCCoverage(), 0.01);
        assertEquals(23.8f, summary.getStatementCoverage(), 0.01);
        assertEquals(23.9f, summary.getCallCoverage(), 0.01);

        assertEquals(0.0f, summary.getTotalBranchCoverage(), 0.01);
        assertEquals(0.0f, summary.getTotalMCDCCOverage(), 0.01);
        assertEquals(0.0f, summary.getTotalStatementCoverage(), 0.01);
        assertEquals(0.0f, summary.getTotalCallCoverage(), 0.01);

        assertTrue(summary.getCTCoverageResults().isEmpty());
        assertNull(summary.getJob());
        //noinspection unchecked
        Job job = new Job(null, "job") {
            @Override
            public boolean isBuildable() {
                return false;
            }

            @Override
            protected SortedMap<Integer, ? extends Run> _getRuns() {
                return null;
            }

            @Override
            protected void removeRun(Run run) {

            }
        };
        summary.setJob(job);
        assertNotNull(summary.getJob());
    }

    @Test
    public void constructor() throws Exception {
        CTCoverageResultSummary summary = new CTCoverageResultSummary(null, 23.4f, 23.5f,
                23.7f, 23.9f);

        assertEquals(23.7f, summary.getBranchCoverage(), 0.01);
        assertEquals(23.9f, summary.getMCDCCoverage(), 0.01);
        assertEquals(23.4f, summary.getStatementCoverage(), 0.01);
        assertEquals(23.5f, summary.getCallCoverage(), 0.01);

        assertEquals(0.0f, summary.getTotalBranchCoverage(), 0.01);
        assertEquals(0.0f, summary.getTotalMCDCCOverage(), 0.01);
        assertEquals(0.0f, summary.getTotalStatementCoverage(), 0.01);
        assertEquals(0.0f, summary.getTotalCallCoverage(), 0.01);

        assertTrue(summary.getCTCoverageResults().isEmpty());
    }
}