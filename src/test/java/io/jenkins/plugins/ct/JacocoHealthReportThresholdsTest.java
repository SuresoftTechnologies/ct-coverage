package io.jenkins.plugins.ct;

import io.jenkins.plugins.ct.CTHealthReportThresholds;
import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement;

import org.junit.Test;

import static org.junit.Assert.*;

public class JacocoHealthReportThresholdsTest {

    @Test
    public void ensureValidWithAllZero() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0 ,0);
        assertEquals(0, th.getMinCall());
        assertEquals(0, th.getMaxCall());
        assertEquals(0, th.getMinStatement());
        assertEquals(0, th.getMaxStatement());
        assertEquals(0, th.getMinBranch());
        assertEquals(0, th.getMaxBranch());
        assertEquals(0, th.getMinMCDC());
        assertEquals(0, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void ensureValidWithMaxZero() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                1, 0,
                2, 0,
                3, 0,
                4, 0,
                5, 0,
                6 ,0);

        // currently all zero because "min <= max" is enforced
        assertEquals(0, th.getMinCall());
        assertEquals(0, th.getMaxCall());
        assertEquals(0, th.getMinStatement());
        assertEquals(0, th.getMaxStatement());
        assertEquals(0, th.getMinBranch());
        assertEquals(0, th.getMaxBranch());
        assertEquals(0, th.getMinMCDC());
        assertEquals(0, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void ensureValidWithValues() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                1, 2,
                3, 4,
                5, 6,
                7, 8,
                9, 10,
                11 ,12);
        assertEquals(3, th.getMinCall());
        assertEquals(4, th.getMaxCall());
        assertEquals(5, th.getMinStatement());
        assertEquals(6, th.getMaxStatement());
        assertEquals(7, th.getMinBranch());
        assertEquals(8, th.getMaxBranch());
        assertEquals(11, th.getMinMCDC());
        assertEquals(12, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void ensureValidValuesTooSmall() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                -1, -2,
                -3, -4,
                -5, -6,
                -7, -8,
                -9, -10,
                -11 ,-12);
        assertEquals(0, th.getMinCall());
        assertEquals(0, th.getMaxCall());
        assertEquals(0, th.getMinStatement());
        assertEquals(0, th.getMaxStatement());
        assertEquals(0, th.getMinBranch());
        assertEquals(0, th.getMaxBranch());
        assertEquals(0, th.getMinMCDC());
        assertEquals(0, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void ensureValidValuesTooLarge() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                101, 102,
                103, 104,
                105, 106,
                107, 108,
                109, 110,
                111 ,112);
        assertEquals(100, th.getMinCall());
        assertEquals(100, th.getMaxCall());
        assertEquals(100, th.getMinStatement());
        assertEquals(100, th.getMaxStatement());
        assertEquals(100, th.getMinBranch());
        assertEquals(100, th.getMaxBranch());
        assertEquals(100, th.getMinMCDC());
        assertEquals(100, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void ensureValidWithMinValuesTooHigh() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                21, 2,
                23, 4,
                25, 6,
                27, 8,
                29, 10,
                211 ,12);
        assertEquals(4, th.getMinCall());
        assertEquals(4, th.getMaxCall());
        assertEquals(6, th.getMinStatement());
        assertEquals(6, th.getMaxStatement());
        assertEquals(8, th.getMinBranch());
        assertEquals(8, th.getMaxBranch());
        assertEquals(12, th.getMinMCDC());
        assertEquals(12, th.getMaxMCDC());

        assertNotNull(th.toString());
    }

    @Test
    public void testSetters() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0 ,0);

        th.setMinCall(3);
        th.setMaxCall(4);
        th.setMinStatement(5);
        th.setMaxStatement(6);
        th.setMinBranch(7);
        th.setMaxBranch(8);
        th.setMinMCDC(11);
        th.setMaxMCDC(12);

        assertEquals(3, th.getMinCall());
        assertEquals(4, th.getMaxCall());
        assertEquals(5, th.getMinStatement());
        assertEquals(6, th.getMaxStatement());
        assertEquals(7, th.getMinBranch());
        assertEquals(8, th.getMaxBranch());
        assertEquals(11, th.getMinMCDC());
        assertEquals(12, th.getMaxMCDC());
    }

    @Test
    public void testGetResultByTypeAndRatioBetween() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                1, 2,
                1, 2,
                1, 2,
                1, 2,
                1, 2,
                1, 2);

        Coverage ratio = new Coverage(99, 1);
        ratio.setType(CoverageElement.Type.CALL);
        assertEquals(CTHealthReportThresholds.RESULT.BETWEENMINMAX, th.getResultByTypeAndRatio(ratio));
        ratio.setType(CoverageElement.Type.STATEMENT);
        assertEquals(CTHealthReportThresholds.RESULT.BETWEENMINMAX, th.getResultByTypeAndRatio(ratio));
        ratio.setType(CoverageElement.Type.BRANCH);
        assertEquals(CTHealthReportThresholds.RESULT.BETWEENMINMAX, th.getResultByTypeAndRatio(ratio));
       
        ratio.setType(CoverageElement.Type.MCDC);
        assertEquals(CTHealthReportThresholds.RESULT.BETWEENMINMAX, th.getResultByTypeAndRatio(ratio));
    }


    @Test
    public void testGetResultByTypeAndRatioAbove() {
        CTHealthReportThresholds th = new CTHealthReportThresholds(
                1, 2,
                1, 2,
                1, 2,
                1, 2,
                1, 2,
                1, 2);

        Coverage ratio = new Coverage(0, 100);
        ratio.setType(CoverageElement.Type.CALL);
        assertEquals(CTHealthReportThresholds.RESULT.ABOVEMAXIMUM, th.getResultByTypeAndRatio(ratio));
        ratio.setType(CoverageElement.Type.STATEMENT);
        assertEquals(CTHealthReportThresholds.RESULT.ABOVEMAXIMUM, th.getResultByTypeAndRatio(ratio));
        ratio.setType(CoverageElement.Type.BRANCH);
        assertEquals(CTHealthReportThresholds.RESULT.ABOVEMAXIMUM, th.getResultByTypeAndRatio(ratio));
        ratio.setType(CoverageElement.Type.MCDC);
        assertEquals(CTHealthReportThresholds.RESULT.ABOVEMAXIMUM, th.getResultByTypeAndRatio(ratio));
    }
}
