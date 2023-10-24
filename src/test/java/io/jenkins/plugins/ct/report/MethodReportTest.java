package io.jenkins.plugins.ct.report;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

import io.jenkins.plugins.ct.report.MethodReport;
import io.jenkins.plugins.ct.report.SourceFileReport;


public class MethodReportTest {

    @Test
    public void testPrint() {
        MethodReport report = new MethodReport();
        assertNotNull(report.printFourCoverageColumns());
    }

    @Test
    public void testChildren() {
        MethodReport report = new MethodReport();
        report.setName("pkg");

        assertEquals(0, report.getChildren().size());
        SourceFileReport child = new SourceFileReport();
        child.setName("testname");
        report.add(child);
        assertEquals("testname", child.getName());
        assertEquals(1, report.getChildren().size());
        assertEquals("testname", report.getChildren().values().iterator().next().getName());
    }
}
