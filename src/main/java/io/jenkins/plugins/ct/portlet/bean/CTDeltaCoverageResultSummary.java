package io.jenkins.plugins.ct.portlet.bean;

import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.plugins.ct.portlet.CTLoadData;

/**
 * This class encapsulates actual delta coverage of current build.
 * It calculates absolute difference between coverage of last successful build and current build
 */
public class CTDeltaCoverageResultSummary {

    /**
     * Variables to capture delta coverage of current build
     */

    private float branchCoverage;

    private float mcdcCoverage;

    private float statementCoverage;

    private float callCoverage;

    public CTDeltaCoverageResultSummary() {
    }

    // Used to extract coverage result of current and last successful build and encapsulate delta coverage values
    public static CTDeltaCoverageResultSummary build(Run<?,?> run){
        Run<?,?> lastSuccessfulBuild = run.getParent().getLastSuccessfulBuild();
        CTCoverageResultSummary lastBuildCoverage = lastSuccessfulBuild!=null ? CTLoadData.getResult(lastSuccessfulBuild):new CTCoverageResultSummary();
        CTCoverageResultSummary currentBuildCoverage = CTLoadData.getResult(run);

        CTDeltaCoverageResultSummary jacocoDeltaCoverageResultSummary = new CTDeltaCoverageResultSummary();
        jacocoDeltaCoverageResultSummary.branchCoverage = currentBuildCoverage.getBranchCoverage() - lastBuildCoverage.getBranchCoverage();
        jacocoDeltaCoverageResultSummary.mcdcCoverage = currentBuildCoverage.getMCDCCoverage() - lastBuildCoverage.getMCDCCoverage();
        jacocoDeltaCoverageResultSummary.statementCoverage = currentBuildCoverage.getStatementCoverage() - lastBuildCoverage.getStatementCoverage();
        jacocoDeltaCoverageResultSummary.callCoverage = currentBuildCoverage.getCallCoverage() - lastBuildCoverage.getCallCoverage();

        return jacocoDeltaCoverageResultSummary;
    }

    public float getBranchCoverage() {
        return branchCoverage;
    }

    public float getMCDCCoverage() {
        return mcdcCoverage;
    }

    public float getStatementCoverage() {
        return statementCoverage;
    }

    public float getCallCoverage() {
        return callCoverage;
    }

    public void setBranchCoverage(float branchCoverage) {
        this.branchCoverage = branchCoverage;
    }

    public void setMCDCCoverage(float mcdcCoverage) {
        this.mcdcCoverage = mcdcCoverage;
    }

    public void setStatementCoverage(float statementCoverage) {
        this.statementCoverage = statementCoverage;
    }

    public void setCallCoverage(float callCoverage) {
        this.callCoverage = callCoverage;
    }

    @Override
    public String toString() {
        return "JacocoDeltaCoverageResultSummary [" +
                ", branchCoverage=" + branchCoverage +
                ", mcdcCoverage=" + mcdcCoverage +
                ", statementCoverage=" + statementCoverage +
                ", callCoverage=" + callCoverage +
                ']';
    }
}
