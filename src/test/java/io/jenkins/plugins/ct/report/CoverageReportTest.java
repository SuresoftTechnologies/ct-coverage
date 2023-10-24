package io.jenkins.plugins.ct.report;

import static org.junit.Assert.*;

import hudson.util.StreamTaskListener;
import io.jenkins.plugins.ct.CTBuildAction;
import io.jenkins.plugins.ct.CTHealthReportThresholds;
import io.jenkins.plugins.ct.ExecutionFileLoader;
import io.jenkins.plugins.ct.report.CoverageReport;

import org.junit.Test;


public class CoverageReportTest {
    @Test
    public void testGetBuild() {
        CoverageReport report = new CoverageReport(action, new ExecutionFileLoader());
        assertNull(report.getBuild());
    }

    @Test
    public void testName() {
        CoverageReport report = new CoverageReport(action, new ExecutionFileLoader());
        assertEquals("Jacoco", report.getName());

        report.setName("myname/&:<>2%;");
        assertEquals("myname/____2__", report.getName());
        assertEquals("myname/____2__", report.getDisplayName());
    }

    @Test
    public void testDoJaCoCoExec() {
        CoverageReport report = new CoverageReport(action, new ExecutionFileLoader());
        assertNotNull(report);
        // TODO: how to simulate JaCoCoBuildAction without full Jenkins test-framework?
        // report.doJacocoExec();
    }

    @Test
    public void testThresholds() {
        CoverageReport report = new CoverageReport(action, new ExecutionFileLoader());
        report.setThresholds(new CTHealthReportThresholds());
    }

    private CTBuildAction action = new CTBuildAction(null, null, null, StreamTaskListener.fromStdout(), null, null);
}
