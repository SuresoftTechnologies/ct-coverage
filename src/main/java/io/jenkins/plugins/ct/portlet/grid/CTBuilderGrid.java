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
package io.jenkins.plugins.ct.portlet.grid;

import java.util.Collection;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.plugins.view.dashboard.DashboardPortlet;
import io.jenkins.plugins.ct.portlet.CTLoadData;
import io.jenkins.plugins.ct.portlet.Messages;
import io.jenkins.plugins.ct.portlet.bean.CTCoverageResultSummary;

/**
 * A portlet for JaCoCo Coverage results - Grid data.
 *
 * See http://wiki.hudson-ci.org/display/HUDSON/Dashboard+View
 */
public class CTBuilderGrid extends DashboardPortlet {

  /**
   * Constructor with grid name as parameter. DataBoundConstructor
   * annotation helps the Stapler class to find which constructor that
   * should be used when automatically copying values from a web form
   * to a class.
   *
   * @param name
   *          grid name
   */
  @DataBoundConstructor
  public CTBuilderGrid(String name) {
    super(name);
  }

  /**
   * This method will be called by portlet.jelly to load data and
   * create the grid.
   *
   * @param jobs
   *          a Collection of Job objects
   * @return JacocoCoverageResultSummary a coverage result summary
   */
  public CTCoverageResultSummary getJaCoCoCoverageResultSummary(Collection<Job<?,?>> jobs) {
    return CTLoadData.getResultSummary(jobs);
  }

  /**
   * Descriptor that will be shown on Dashboard Portlets view.
   */
  @Extension(optional = true)
  public static class JacocoGridDescriptor extends Descriptor<DashboardPortlet> {

    @Override
    public String getDisplayName() {
      return Messages.gridTitle();
    }
  }
}
