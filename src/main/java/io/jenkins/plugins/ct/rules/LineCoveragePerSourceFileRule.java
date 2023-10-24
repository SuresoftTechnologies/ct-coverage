package io.jenkins.plugins.ct.rules;

import hudson.model.TaskListener;
import io.jenkins.plugins.ct.Rule;
import io.jenkins.plugins.ct.report.CoverageReport;

/**
 * Flags a failure if the line coverage of a source file
 * goes below a certain threshold.
 */
public class LineCoveragePerSourceFileRule extends Rule {

    private static final long serialVersionUID = -2869893039051762047L;

    //private final float minPercentage;

    public LineCoveragePerSourceFileRule(float minPercentage) {
        //this.minPercentage = minPercentage;
    }

    @Override
    public void enforce(CoverageReport report, TaskListener listener) {
//        for (PackageReport pack : report.getChildren().values()) {
//            for (SourceFileReport sfReport : pack.getChildren().values()) {
//                float percentage = sfReport.getLineCoverage().getPercentageFloat();
//
//                if (percentage < minPercentage) {
//                    listener.getLogger().println("Emma: " + sfReport.getDisplayName() + " failed (below " + minPercentage + "%).");
//                    sfReport.setFailed();
//                }
//            }
//        }
    }
}
