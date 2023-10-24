package io.jenkins.plugins.ct.portlet;

import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.plugins.ct.portlet.CTLoadData;
import io.jenkins.plugins.ct.portlet.bean.CTCoverageResultSummary;
import io.jenkins.plugins.ct.portlet.bean.CTDeltaCoverageResultSummary;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class JacocoDeltaCoverageResultSummaryTest {

    private final Run run = mock(Run.class);
    private final Job job = mock(Job.class);

    CTCoverageResultSummary lastSuccessfulBuildCoverage, currentBuildwithMoreCoverage, currentBuildWithLesserCoverage;

    @Before
    public void setUp(){
        lastSuccessfulBuildCoverage = new CTCoverageResultSummary(job, 88.9090f
                , 95.5055f,
                98.889f,
                60.05623f);

        currentBuildwithMoreCoverage = new CTCoverageResultSummary(job, 89.231f
                , 95.750f,
                98.999f,
                61.232f);

        currentBuildWithLesserCoverage = new CTCoverageResultSummary(job, 85.556f
                , 95.5055f,
                99.0909f,
                61.234f);
    }

    // Test delta coverage summary when current build has better coverage than previous successful build
    @Test
    public void deltaCoverageSummaryForBetterBuildTest(){

        when(run.getParent()).thenReturn(job);
        when(job.getLastSuccessfulBuild()).thenReturn(run);

        try (MockedStatic<CTLoadData> staticJacocoLoadData = mockStatic(CTLoadData.class)) {
            staticJacocoLoadData
                    .when(() -> CTLoadData.getResult(any()))
                    .thenReturn(lastSuccessfulBuildCoverage)
                    .thenReturn(currentBuildwithMoreCoverage);

            CTDeltaCoverageResultSummary deltaCoverageSummary = CTDeltaCoverageResultSummary.build(run);

            assertEquals("Absolute difference in branch coverage",
                currentBuildwithMoreCoverage.getBranchCoverage() - lastSuccessfulBuildCoverage.getBranchCoverage(), deltaCoverageSummary.getBranchCoverage(), 0.00001);
            assertEquals("Absolute difference in complexity coverage",
                currentBuildwithMoreCoverage.getMCDCCoverage() - lastSuccessfulBuildCoverage.getMCDCCoverage(), deltaCoverageSummary.getMCDCCoverage(), 0.00001);
            assertEquals("Absolute difference in line coverage",
                currentBuildwithMoreCoverage.getStatementCoverage() - lastSuccessfulBuildCoverage.getStatementCoverage(), deltaCoverageSummary.getStatementCoverage(), 0.00001);
            assertEquals("Absolute difference in method coverage",
                currentBuildwithMoreCoverage.getCallCoverage() - lastSuccessfulBuildCoverage.getCallCoverage(), deltaCoverageSummary.getCallCoverage(), 0.00001);
        }
    }

    // Test delta coverage summary when current build has worse coverage than previous successful build
    @Test
    public void deltaCoverageSummaryForWorseBuildTest(){

        when(run.getParent()).thenReturn(job);
        when(job.getLastSuccessfulBuild()).thenReturn(run);

        try (MockedStatic<CTLoadData> staticJacocoLoadData = mockStatic(CTLoadData.class)) {
            staticJacocoLoadData
                    .when(() -> CTLoadData.getResult(any()))
                    .thenReturn(lastSuccessfulBuildCoverage)
                    .thenReturn(currentBuildWithLesserCoverage);

            CTDeltaCoverageResultSummary deltaCoverageSummary = CTDeltaCoverageResultSummary.build(run);

            assertEquals("Absolute difference in branch coverage",
                currentBuildWithLesserCoverage.getBranchCoverage() - lastSuccessfulBuildCoverage.getBranchCoverage(), deltaCoverageSummary.getBranchCoverage(), 0.00001);
            assertEquals("Absolute difference in complexity coverage",
                currentBuildWithLesserCoverage.getMCDCCoverage() - lastSuccessfulBuildCoverage.getMCDCCoverage(), deltaCoverageSummary.getMCDCCoverage(), 0.00001);
            assertEquals("Absolute difference in line coverage",
                currentBuildWithLesserCoverage.getStatementCoverage() - lastSuccessfulBuildCoverage.getStatementCoverage(), deltaCoverageSummary.getStatementCoverage(), 0.00001);
            assertEquals("Absolute difference in method coverage",
                currentBuildWithLesserCoverage.getCallCoverage() - lastSuccessfulBuildCoverage.getCallCoverage(), deltaCoverageSummary.getCallCoverage(), 0.00001);

        }
    }
}
