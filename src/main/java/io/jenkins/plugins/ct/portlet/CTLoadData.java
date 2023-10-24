/*
 *  The MIT License
 *
 *  Copyright 2010 Sony Ericsson Mobile Communications. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

/*
 * @author Allyn Pierre (Allyn.GreyDeAlmeidaLimaPierre@sonyericsson.com)
 * @author Eduardo Palazzo (Eduardo.Palazzo@sonyericsson.com)
 * @author Mauro Durante (Mauro.DuranteJunior@sonyericsson.com)
 */
package io.jenkins.plugins.ct.portlet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.plugins.ct.CTBuildAction;
import io.jenkins.plugins.ct.portlet.bean.CTCoverageResultSummary;
import io.jenkins.plugins.ct.portlet.utils.Utils;

/**
 * Load data of JaCoCo coverage results used by chart or grid.
 */
public final class CTLoadData {

  /**
   * Private constructor avoiding this class to be used in a non-static way.
   */
  private CTLoadData() {
  }

  /**
   * Get JaCoCo coverage results of all jobs and store into a sorted
   * HashMap by date.
   *
   * @param jobs
   *        jobs of Dashboard view
   * @param daysNumber
   *          number of days
   * @return Map The sorted summaries
   */
  public static Map<String, CTCoverageResultSummary> loadChartDataWithinRange(List<Job<?,?>> jobs, int daysNumber) {

    Map<String, CTCoverageResultSummary> summaries = new HashMap<>();

    // Get the last build (last date) of the all jobs
    Calendar firstDate = Utils.getLastDate(jobs);

    // No builds
    if (firstDate == null) {
      return null;
    }

    // Adjust for the given date range
    firstDate.add(Calendar.DAY_OF_MONTH, -daysNumber);

    // For each job, get JaCoCo coverage results according with
    // date range (last build date minus number of days)
    for (Job<?,?> job : jobs) {

      Run<?,?> run = job.getLastCompletedBuild();

      if (null != run) {
          Calendar runDate = run.getTimestamp();

        while (runDate.after(firstDate)) {

          summarize(summaries, run, runDate, job);

          run = run.getPreviousBuild();
          while (run != null && run.isBuilding()) {
            run = run.getPreviousBuild();
          }

          if (null == run) {
            break;
          }

          runDate = run.getTimestamp();
        }
      }
    }

    // Sorting by date, ascending order
    return new TreeMap<>(summaries);

  }

  /**
   * Summarize JaCoCo coverage results.
   *
   * @param summaries
   *          a Map of JacocoCoverageResultSummary objects indexed by
   *          dates
   * @param run
   *          the build which will provide information about the
   *          coverage result
   * @param runDate
   *          the date on which the build was performed
   * @param job
   *          job from the DashBoard Portlet view
   */
  private static void summarize(Map<String, CTCoverageResultSummary> summaries, Run<?,?> run, Calendar runDate, Job<?,?> job) {

    CTCoverageResultSummary jacocoCoverageResult = getResult(run);
	
	String date = new SimpleDateFormat("yyyy-MM-dd").format(runDate.getTime());

    // Retrieve JaCoCo information for informed date
    CTCoverageResultSummary jacocoCoverageResultSummary = summaries.get(date);

    // Consider the last result of each
    // job date (if there are many builds for the same date). If not
    // exists, the JaCoCo coverage data must be added. If exists
    // JaCoCo coverage data for the same date but it belongs to other
    // job, sum the values.
    if (jacocoCoverageResultSummary == null) {
      jacocoCoverageResultSummary = new CTCoverageResultSummary();
      jacocoCoverageResultSummary.addCoverageResult(jacocoCoverageResult);
      jacocoCoverageResultSummary.setJob(job);
    } else {

      // Check if exists JaCoCo data for same date and job
      List<CTCoverageResultSummary> listResults = jacocoCoverageResultSummary.getCTCoverageResults();
      boolean found = false;

      for (CTCoverageResultSummary item : listResults) {
        if ((null != item.getJob()) && (null != item.getJob().getName()) && (null != job)) {
          if (item.getJob().getName().equals(job.getName())) {
            found = true;
            break;
          }
        }
      }

      if (!found) {
        jacocoCoverageResultSummary.addCoverageResult(jacocoCoverageResult);
        jacocoCoverageResultSummary.setJob(job);
      }
    }

    summaries.put(date, jacocoCoverageResultSummary);
  }

  /**
   * Get the JaCoCo coverage result for a specific run.
   *
   * @param run
   *          a job execution
   * @return JaCoCoCoverageTestResult the coverage result
   */
  public static CTCoverageResultSummary getResult(Run<?,?> run) {
    CTBuildAction jacocoAction = run.getAction(CTBuildAction.class);

    float statementCoverage = 0.0f;
    float callCoverage = 0.0f;
    float branchCoverage = 0.0f;
    float mcdcScore = 0.0f;

    if (jacocoAction != null) {
      
      if (null != jacocoAction.getStatementCoverage()) {
        statementCoverage = jacocoAction.getStatementCoverage().getPercentageFloat();
      }
      if (null != jacocoAction.getCallCoverage()) {
        callCoverage = jacocoAction.getCallCoverage().getPercentageFloat();
      }
      if (null != jacocoAction.getBranchCoverage()) {
        branchCoverage = jacocoAction.getBranchCoverage().getPercentageFloat();
      }
      if (null != jacocoAction.getMCDCCoverage()) {
        mcdcScore = jacocoAction.getMCDCCoverage().getPercentageFloat();
      }
    }
    return new CTCoverageResultSummary(
        run.getParent(), statementCoverage, callCoverage, 
        branchCoverage, mcdcScore);
  }

  /**
   * Summarize the last coverage results of all jobs. If a job doesn't
   * include any coverage, add zero.
   *
   * @param jobs
   *          a final Collection of Job objects
   * @return JacocoCoverageResultSummary the result summary
   */
  public static CTCoverageResultSummary getResultSummary(final Collection<Job<?,?>> jobs) {
    CTCoverageResultSummary summary = new CTCoverageResultSummary();

    for (Job<?,?> job : jobs) {

      float lineCoverage = 0.0f;
      float callCoverage = 0.0f;
      float branchCoverage = 0.0f;
      float mcdcScore = 0.0f;

      Run<?,?> run = job.getLastSuccessfulBuild();

      if (run != null) {

        CTBuildAction jacocoAction = job.getLastSuccessfulBuild().getAction(CTBuildAction.class);

        if (null != jacocoAction) {
          if (null != jacocoAction.getStatementCoverage()) {
            lineCoverage = jacocoAction.getStatementCoverage().getPercentageFloat();
            BigDecimal bigLineCoverage = new BigDecimal(lineCoverage);
            bigLineCoverage = bigLineCoverage.setScale(1, RoundingMode.HALF_EVEN);
            lineCoverage = bigLineCoverage.floatValue();
          }

          if (null != jacocoAction.getCallCoverage()) {
            callCoverage = jacocoAction.getCallCoverage().getPercentageFloat();
            BigDecimal bigMethodCoverage = new BigDecimal(callCoverage);
            bigMethodCoverage = bigMethodCoverage.setScale(1, RoundingMode.HALF_EVEN);
            callCoverage = bigMethodCoverage.floatValue();
          }

          if (null != jacocoAction.getBranchCoverage()) {
            branchCoverage = jacocoAction.getBranchCoverage().getPercentageFloat();
            BigDecimal bigBranchCoverage = new BigDecimal(branchCoverage);
            bigBranchCoverage = bigBranchCoverage.setScale(1, RoundingMode.HALF_EVEN);
            branchCoverage = bigBranchCoverage.floatValue();
          }

          if (null != jacocoAction.getMCDCCoverage()) {
            mcdcScore = jacocoAction.getMCDCCoverage().getPercentageFloat();
            BigDecimal bigComplexityCoverage = new BigDecimal(mcdcScore);
            bigComplexityCoverage = bigComplexityCoverage.setScale(1, RoundingMode.HALF_EVEN);
            mcdcScore = bigComplexityCoverage.floatValue();
          }
        }
      }
      summary.addCoverageResult(new CTCoverageResultSummary(
          job, lineCoverage, callCoverage, branchCoverage, mcdcScore));
    }
    return summary;
  }
}
