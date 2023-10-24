package io.jenkins.plugins.ct;

import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.ct.CTHealthReportDeltaThresholds;
import io.jenkins.plugins.ct.CTPublisher;
import io.jenkins.plugins.ct.portlet.bean.CTDeltaCoverageResultSummary;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class BuildOverBuildTest {

    private CTDeltaCoverageResultSummary jacocoDeltaCoverageResultSummary_1, jacocoDeltaCoverageResultSummary_2;
    private CTHealthReportDeltaThresholds deltaHealthThresholds;
    //private JacocoHealthReportThresholds healthThresholds;

    private Run run = mock(Run.class);
    private final PrintStream logger = System.out;

    @Before
    public void setUp(){
        jacocoDeltaCoverageResultSummary_1 = new CTDeltaCoverageResultSummary();
        jacocoDeltaCoverageResultSummary_1.setCallCoverage(11.8921f);
        jacocoDeltaCoverageResultSummary_1.setStatementCoverage(21.523f);
        jacocoDeltaCoverageResultSummary_1.setBranchCoverage(0f);
        jacocoDeltaCoverageResultSummary_1.setMCDCCoverage(1.34f);

        jacocoDeltaCoverageResultSummary_2 = new CTDeltaCoverageResultSummary();
        jacocoDeltaCoverageResultSummary_2.setCallCoverage(5.340f);
        jacocoDeltaCoverageResultSummary_2.setStatementCoverage(7.8921f);
        jacocoDeltaCoverageResultSummary_2.setBranchCoverage(0f);
        jacocoDeltaCoverageResultSummary_2.setMCDCCoverage(1.678f);

        deltaHealthThresholds = new CTHealthReportDeltaThresholds("10.556", "0", "2.3434", "9.11457", "8.2525", "1.5556");
        //healthThresholds = new JacocoHealthReportThresholds(88, 100, 85, 100, 75, 90, 100, 100, 83, 95, 86, 92);
    }

    /**
     * [JENKINS-58184] - This test verifies that we are ignoring coverage increase while checking against the thresholds.
     * In this test data, Instruction Coverage has gone down but it is still within the configured threshold limit.
     *                  Method and line coverage has increased and are way above thresholds.
     * The check passes the build as no decrease is more than the configured threshold
     */
    @Test
    public void shouldPassIfNegativeMetricIsWithinThresholdAndOtherMetricesArePositiveAndAboveThreshold(){

        try (MockedStatic<CTDeltaCoverageResultSummary> staticJacocoDeltaCoverageResultSummary =
                mockStatic(CTDeltaCoverageResultSummary.class)) {
            staticJacocoDeltaCoverageResultSummary
                    .when(() -> CTDeltaCoverageResultSummary.build(any()))
                    .thenReturn(jacocoDeltaCoverageResultSummary_1);

            CTPublisher jacocoPublisher = new CTPublisher();
            jacocoPublisher.deltaHealthReport = deltaHealthThresholds;
            Result result = jacocoPublisher.checkBuildOverBuildResult(run, logger);

            Assert.assertEquals("Delta coverage drop is lesser than delta health threshold values", Result.SUCCESS, result);
        }

    }

    // Test if the build with delta coverage < delta threshold will pass
    @Test
    public void checkBuildOverBuildSuccessTest(){

        try (MockedStatic<CTDeltaCoverageResultSummary> staticJacocoDeltaCoverageResultSummary = mockStatic(CTDeltaCoverageResultSummary.class)) {
            staticJacocoDeltaCoverageResultSummary
                    .when(() -> CTDeltaCoverageResultSummary.build(any()))
                    .thenReturn(jacocoDeltaCoverageResultSummary_2)
                    .thenReturn(jacocoDeltaCoverageResultSummary_1);
            CTPublisher jacocoPublisher = new CTPublisher();
            jacocoPublisher.deltaHealthReport = deltaHealthThresholds;
            Result result = jacocoPublisher.checkBuildOverBuildResult(run, logger); // check for first test case: delta coverage < delta threshold

            Assert.assertEquals("Delta coverage is lesser than delta health threshold values", Result.SUCCESS, result);

            result = jacocoPublisher.checkBuildOverBuildResult(run, logger); // check for second test case: delta coverage > delta threshold but overall coverage better than last successful build
            Assert.assertEquals("Delta coverage is greater than delta health threshold values but overall coverage is better than last successful build's coverage", Result.SUCCESS, result);
        }

    }

    /**
     * [JENKINS-58184] - This test verifies that we are still respecting the thresholds and are failing the build
     *                  in case the drop in coverage is more than the configured threshold for any parameter
     * In this test data, drop in complexity coverage is more than the configured limit of 2.3434 and so the build fails
     */
    @Test
    public void shouldFailIfNegativeMetricIsAboveThresholdAndOtherMetricesArePositive(){
        CTDeltaCoverageResultSummary jacocoDeltaCoverageResultSummary = new CTDeltaCoverageResultSummary();
        jacocoDeltaCoverageResultSummary.setCallCoverage(5.340f);
        jacocoDeltaCoverageResultSummary.setStatementCoverage(7.8921f);
        jacocoDeltaCoverageResultSummary.setBranchCoverage(0f);
        jacocoDeltaCoverageResultSummary.setMCDCCoverage(-2.678f);

        try (MockedStatic<CTDeltaCoverageResultSummary> staticJacocoDeltaCoverageResultSummary = mockStatic(CTDeltaCoverageResultSummary.class)) {
            staticJacocoDeltaCoverageResultSummary
                    .when(() -> CTDeltaCoverageResultSummary.build(any()))
                    .thenReturn(jacocoDeltaCoverageResultSummary);

            CTPublisher jacocoPublisher = new CTPublisher();
            jacocoPublisher.deltaHealthReport = deltaHealthThresholds;
            Result result = jacocoPublisher.checkBuildOverBuildResult(run, logger);

            Assert.assertEquals("Delta coverage drop is greater than delta health threshold values", Result.FAILURE, result);
        }
    }
}
