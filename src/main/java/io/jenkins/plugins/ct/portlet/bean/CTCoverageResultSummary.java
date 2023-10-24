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
package io.jenkins.plugins.ct.portlet.bean;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import hudson.model.Job;
import io.jenkins.plugins.ct.portlet.utils.Utils;

/**
 * Summary of the Jacoco Coverage result.
 */
public class CTCoverageResultSummary {

  /**
   * The related job.
   */
  private Job<?,?> job;

  /**
   * Line coverage percentage.
   */
  private float statementCoverage;

  /**
   * Call coverage percentage.
   */
  private float callCoverage;

  /**
   * Block coverage percentage.
   */
  private float branchCoverage;

  /**
   * Complexity score (not a percentage).
   */
  private float mcdcCoverage;

  private List<CTCoverageResultSummary> coverageResults = new ArrayList<>();

  /**
   * Default Constructor.
   */
  public CTCoverageResultSummary() {
  }

  /**
   * Constructor with parameters.
   *
   * @param job
   *          the related Job
   * @param statementCoverage
   *          line coverage percentage
   * @param callCoverage
   *          method coverage percentage
   * @param classCoverage
   *          class coverage percentage
   * @param branchCoverage
   *          branch coverage percentage
   * @param instructionCoverage 
   *          instruction coverage percentage
   * @param mcdcCoverage 
   *          complexity score (not a percentage)
   */
  public CTCoverageResultSummary(Job<?,?> job, float statementCoverage, float callCoverage,
    float branchCoverage, float mcdcCoverage) {
    this.job = job;
    this.statementCoverage = statementCoverage;
    this.callCoverage = callCoverage;
    this.branchCoverage = branchCoverage;
    this.mcdcCoverage = mcdcCoverage;
  }

  /**
   * Add a coverage result.
   *
   * @param coverageResult
   *          a coverage result
   * @return JacocoCoverageResultSummary summary of the Jacoco coverage
   *         result
   */
  public CTCoverageResultSummary addCoverageResult(CTCoverageResultSummary coverageResult) {

    this.setLineCoverage(this.getStatementCoverage() + coverageResult.getStatementCoverage());
    this.setMethodCoverage(this.getCallCoverage() + coverageResult.getCallCoverage());
    this.setBranchCoverage(this.getBranchCoverage() + coverageResult.getBranchCoverage());
    this.setComplexityScore(this.getMCDCCoverage() + coverageResult.getMCDCCoverage());

    getCoverageResults().add(coverageResult);

    return this;
  }

  /**
   * Get list of JacocoCoverageResult objects.
   *
   * @return List a List of JacocoCoverageResult objects
   */
  public List<CTCoverageResultSummary> getCTCoverageResults() {
    return this.getCoverageResults();
  }

  /**
   * Getter of the total of block coverage.
   *
   * @return float the total of block coverage.
   */
  public float getTotalBranchCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } 

    float totalBranch = this.getBranchCoverage() / this.getCoverageResults().size();
    totalBranch = Utils.roundFloat(1, RoundingMode.HALF_EVEN, totalBranch);

    return totalBranch;
  }


  /**
   * Getter of the total of block coverage.
   *
   * @return float the total of block coverage.
   */
  public float getTotalMCDCCOverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    }

    float totalComplex = this.getMCDCCoverage() / this.getCoverageResults().size();
    totalComplex = Utils.roundFloat(1, RoundingMode.HALF_EVEN, totalComplex);

    return totalComplex;
  }

  /**
   * Getter of the total of line coverage.
   *
   * @return float the total of line coverage.
   */
  public float getTotalStatementCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } 

    float totalLine = this.getStatementCoverage() / this.getCoverageResults().size();
    totalLine = Utils.roundFloat(1, RoundingMode.HALF_EVEN, totalLine);

    return totalLine;
  }

  /**
   * Getter of the total of method coverage.
   *
   * @return float the total of method coverage.
   */
  public float getTotalCallCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    }

    float totalMethod = this.getCallCoverage() / this.getCoverageResults().size();
    totalMethod = Utils.roundFloat(1, RoundingMode.HALF_EVEN, totalMethod);

    return totalMethod;
  }

  /**
   * @return Job a job
   */
  public Job<?,?> getJob() {
    return job;
  }

  public float getBranchCoverage() {
    return branchCoverage;
  }

  public float getMCDCCoverage() {
    return mcdcCoverage;
  }

  /**
   * @return the lineCoverage
   */
  public float getStatementCoverage() {
    return statementCoverage;
  }

  /**
   * @return the methodCoverage
   */
  public float getCallCoverage() {
    return callCoverage;
  }

  /**
   * @param job
   *          the job to set
   */
  public void setJob(Job<?,?> job) {
    this.job = job;
  }

  public void setBranchCoverage(float branchCoverage) {
    this.branchCoverage = branchCoverage;
  }

  public void setComplexityScore(float mcdcCoverage) {
    this.mcdcCoverage = mcdcCoverage;
  }

  /**
   * @param statementCoverage
   *          the lineCoverage to set
   */
  public void setLineCoverage(float statementCoverage) {
    this.statementCoverage = statementCoverage;
  }

  /**
   * @param callCoverage
   *          the methodCoverage to set
   */
  public void setMethodCoverage(float callCoverage) {
    this.callCoverage = callCoverage;
  }

  /**
   * @return a list of coverage results
   */
  public List<CTCoverageResultSummary> getCoverageResults() {
    return coverageResults;
  }

  /**
   * @param coverageResults
   *          the list of coverage results to set
   */
  public void setCoverageResults(List<CTCoverageResultSummary> coverageResults) {
    this.coverageResults = coverageResults;
  }
}
