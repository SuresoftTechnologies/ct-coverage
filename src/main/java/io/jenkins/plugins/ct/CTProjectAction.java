package io.jenkins.plugins.ct;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.ct.Messages;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Project view extension by JaCoCo plugin.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class CTProjectAction implements Action {
    public final Job<?,?> project;

    public CTProjectAction(Job<?,?> project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getDisplayName() {
        return Messages.ProjectAction_DisplayName();
    }

    public String getUrlName() {
        return "jacoco";
    }

    /**
     * Gets the most recent {@link CTBuildAction} object.
     * @return the most recent jacoco coverage report
     */
    public CTBuildAction getLastResult() {
        for (Run<?, ?> b = project.getLastBuild(); b != null; b = b.getPreviousBuild()) {
            if (b.isBuilding() || b.getResult() == Result.FAILURE || b.getResult() == Result.ABORTED)
                continue;
            CTBuildAction r = b.getAction(CTBuildAction.class);
            if (r != null)
                return r;
        }
        return null;
    }

    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
       if (getLastResult() != null)
          getLastResult().doGraph(req,rsp);
    }

    //private static final Logger logger = Logger.getLogger(JacocoBuildAction.class.getName());
}
