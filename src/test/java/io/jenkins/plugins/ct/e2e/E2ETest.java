package io.jenkins.plugins.ct.e2e;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RealJenkinsRule;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import io.jenkins.plugins.ct.CTBuildAction;
import io.jenkins.plugins.ct.CTPublisher;
import io.jenkins.plugins.ct.model.Coverage;

import static io.jenkins.plugins.ct.e2e.E2ETest.CoverageMatcher.withCoverage;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class E2ETest {

    @Rule
    public RealJenkinsRule rjr = new RealJenkinsRule();
   
    @Test
    public void simpleTest() throws Throwable {rjr.then(r -> {
            FreeStyleProject project = r.createFreeStyleProject();
            project.getBuildersList().addAll(createJacocoSampleXmlCopy());
            project.getPublishersList().add(new CTPublisher());

            FreeStyleBuild build = r.buildAndAssertSuccess(project);
            
            assertThat("plugin collected data", build.getLog(), containsString("Collecting CT coverage data"));
            
            CTBuildAction action = build.getAction(CTBuildAction.class);
            assertThat("incorrect branch coverage reported", action.getBranchCoverage(), withCoverage(422, 268, 690));
            assertThat("incorrect statement coverage reported", action.getStatementCoverage(), withCoverage(956, 1033, 1989));
            assertThat("incorrect mcdc coverage reported", action.getCallCoverage(), withCoverage(916, 334, 1250));
            assertThat("incorrect call coverage reported", action.getMCDCCoverage(), withCoverage(229, 181, 410));
            build.run();
        }
        );
        
    }
    
    private static List<Builder> createJacocoSampleXmlCopy() {
    	String sampleFile = Paths.get(io.jenkins.plugins.ct.CoverageReportTest.class.getResource("sample.xml").getFile().substring(1)).toString();
        String[] commands = { String.format("copy \"%s\" \"%s\" /Y", sampleFile, "%WORKSPACE%") };
        List<Builder> builders = new ArrayList<>();
        if (Functions.isWindows()) {
            for (String command : commands) {
                builders.add(new BatchFile(command));
            }
        } else {
            for (String command : commands) {
                builders.add(new Shell(command));
            }
        }
        return builders;
    }

    public static class CoverageMatcher extends TypeSafeDiagnosingMatcher<Coverage> {

        private final int covered;
        private final int missed;
        private final int total;

        private CoverageMatcher(int covered, int missed, int total) {
            this.covered = covered;
            this.missed = missed;
            this.total = total;
        }
        @Override
        public void describeTo(Description description) {
            description.appendText(" with covered="+ covered);
            description.appendText(" and missed="+ missed);
            description.appendText(" and total="+ total);

        }

        @Override
        protected boolean matchesSafely(Coverage coverage, Description mismatchDescription) {
            mismatchDescription.appendText("Coverage with covered="+ coverage.getCovered());
            mismatchDescription.appendText(" and missed="+ coverage.getMissed());
            mismatchDescription.appendText(" and total="+ coverage.getTotal());

            return coverage.getCovered() == covered &&
                    coverage.getMissed() == missed &&
                    coverage.getTotal() == total;
        }

        public static CoverageMatcher withCoverage(int covered, int missed, int total) {
            return new CoverageMatcher(covered, missed, total);
        }
    }
}
