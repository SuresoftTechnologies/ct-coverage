package io.jenkins.plugins.ct;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import io.jenkins.plugins.ct.Messages;
import io.jenkins.plugins.ct.portlet.bean.CTDeltaCoverageResultSummary;
import io.jenkins.plugins.ct.portlet.utils.Utils;
import io.jenkins.plugins.ct.report.CoverageReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * {@link Publisher} that captures jacoco coverage reports.
 *
 * @author Kohsuke Kawaguchi
 * @author Jonathan Fuerth
 * @author Ognjen Bubalo
 *
 */
public class CTPublisher extends Recorder implements SimpleBuildStep {

    /**
     * Rule to be enforced. Can be null.
     * <p>
     * TODO: define a configuration mechanism.
     */
    public Rule rule;
    @Deprecated
    public transient String includes;
    @Deprecated
    public transient int moduleNum;
    /**
     * {@link hudson.model.HealthReport} thresholds to apply.
     */
    public CTHealthReportThresholds healthReports;

    // Delta coverage thresholds to apply
    public CTHealthReportDeltaThresholds deltaHealthReport;


    /**
     * Variables containing the configuration set by the user.
     */
    private String execPattern;
    private String sourcePattern;
    private String sourceInclusionPattern;
    private String sourceExclusionPattern;
    private boolean skipCopyOfSrcFiles; // Added for enabling/disabling copy of source files

    private String minimumInstructionCoverage;
    private String minimumBranchCoverage;
    private String minimumComplexityCoverage;
    private String minimumLineCoverage;
    private String minimumMethodCoverage;
    private String minimumClassCoverage;
    private String maximumInstructionCoverage;
    private String maximumBranchCoverage;
    private String maximumComplexityCoverage;
    private String maximumLineCoverage;
    private String maximumMethodCoverage;
    private String maximumClassCoverage;
    private boolean changeBuildStatus;
    private boolean runAlways; // Added to always run even if build has FAILED or ABORTED

    /**
     * Following variables contain delta coverage thresholds as configured by the user
     * Delta coverage = | Last Successful Coverage - Current Coverage |
     */
    private String deltaInstructionCoverage;
    private String deltaBranchCoverage;
    private String deltaComplexityCoverage;
    private String deltaLineCoverage;
    private String deltaMethodCoverage;
    private String deltaClassCoverage;
    private boolean buildOverBuild;

	private static final String DIR_SEP = "\\s*,\\s*";

    private static final Integer THRESHOLD_DEFAULT = 0;

    @DataBoundConstructor
    public CTPublisher() {
        this.execPattern = "**/**.xml";
        this.sourcePattern = "**/src/main/java";
        this.sourceInclusionPattern = "**/*.java,**/*.groovy,**/*.kt,**/*.kts";
        this.sourceExclusionPattern = "";
        this.skipCopyOfSrcFiles = false;
        this.minimumInstructionCoverage = "0";
        this.minimumBranchCoverage = "0";
        this.minimumComplexityCoverage = "0";
        this.minimumLineCoverage = "0";
        this.minimumMethodCoverage = "0";
        this.minimumClassCoverage = "0";
        this.maximumInstructionCoverage = "0";
        this.maximumBranchCoverage = "0";
        this.maximumComplexityCoverage = "0";
        this.maximumLineCoverage = "0";
        this.maximumMethodCoverage = "0";
        this.maximumClassCoverage = "0";
        this.changeBuildStatus = false;
        this.runAlways = false;
        this.deltaInstructionCoverage = "0";
        this.deltaBranchCoverage = "0";
        this.deltaComplexityCoverage = "0";
        this.deltaLineCoverage = "0";
        this.deltaMethodCoverage = "0";
        this.deltaClassCoverage = "0";
        this.buildOverBuild = false;
    }

	/**
     * Loads the configuration set by user.
	 * @param execPattern deprecated
	 * @param classPattern deprecated
	 * @param sourcePattern deprecated
	 * @param inclusionPattern deprecated
	 * @param exclusionPattern deprecated
	 * @param skipCopyOfSrcFiles deprecated
	 * @param maximumInstructionCoverage deprecated
	 * @param maximumBranchCoverage deprecated
	 * @param maximumComplexityCoverage deprecated
	 * @param maximumLineCoverage deprecated
	 * @param maximumMethodCoverage deprecated
	 * @param maximumClassCoverage deprecated
	 * @param minimumInstructionCoverage deprecated
	 * @param minimumBranchCoverage deprecated
	 * @param minimumComplexityCoverage deprecated
	 * @param minimumLineCoverage deprecated
	 * @param minimumMethodCoverage deprecated
	 * @param minimumClassCoverage deprecated
	 * @param changeBuildStatus deprecated
	 * @param runAlways deprecated
	 * @param deltaInstructionCoverage deprecated
	 * @param deltaBranchCoverage deprecated
	 * @param deltaComplexityCoverage deprecated
	 * @param deltaLineCoverage deprecated
	 * @param deltaMethodCoverage deprecated
	 * @param deltaClassCoverage deprecated
	 * @param buildOverBuild deprecated
     */

    private Integer convertThresholdInputToInteger(String input, EnvVars env) {
    	if ((input == null) || ("".equals(input))) {
    		return THRESHOLD_DEFAULT;
    	}
    	try {
    		String expandedInput = env.expand(input);
    		return Integer.parseInt(expandedInput);
    	} catch (NumberFormatException e) {
    		return THRESHOLD_DEFAULT;
    	}
    }


	@Override
	public String toString() {
		return "CTPublisher [execPattern=" + execPattern
				+ ", sourcePattern=" + sourcePattern
				+ ", sourceExclusionPattern=" + sourceExclusionPattern
				+ ", sourceInclusionPattern=" + sourceInclusionPattern
				+ ", minimumInstructionCoverage=" + minimumInstructionCoverage
				+ ", minimumBranchCoverage=" + minimumBranchCoverage
				+ ", minimumComplexityCoverage=" + minimumComplexityCoverage
				+ ", minimumLineCoverage=" + minimumLineCoverage
				+ ", minimumMethodCoverage=" + minimumMethodCoverage
				+ ", minimumClassCoverage=" + minimumClassCoverage
				+ ", maximumInstructionCoverage=" + maximumInstructionCoverage
				+ ", maximumBranchCoverage=" + maximumBranchCoverage
				+ ", maximumComplexityCoverage=" + maximumComplexityCoverage
				+ ", maximumLineCoverage=" + maximumLineCoverage
				+ ", maximumMethodCoverage=" + maximumMethodCoverage
				+ ", maximumClassCoverage=" + maximumClassCoverage
                + ", runAlways=" + runAlways
                + ", deltaInstructionCoverage=" + deltaInstructionCoverage
                + ", deltaBranchCoverage=" + deltaBranchCoverage
                + ", deltaComplexityCoverage=" + deltaComplexityCoverage
                + ", deltaLineCoverage=" + deltaLineCoverage
                + ", deltaMethodCoverage=" + deltaMethodCoverage
                + ", deltaClassCoverage=" + deltaClassCoverage
                + "]";
	}



	public String getExecPattern() {
		return execPattern;
	}

	public String getSourcePattern() {
		return sourcePattern;
	}

	public String getSourceExclusionPattern() {
		return sourceExclusionPattern;
	}
	public String getSourceInclusionPattern() {
		return sourceInclusionPattern;
	}

	public String getInclusionPattern() {
		return StringUtils.EMPTY;
	}

	public String getExclusionPattern() {
		return StringUtils.EMPTY;
	}

    public boolean isSkipCopyOfSrcFiles() {
        return skipCopyOfSrcFiles;
    }

	public String getMinimumInstructionCoverage() {
		return minimumInstructionCoverage;
	}



	public String getMinimumBranchCoverage() {
		return minimumBranchCoverage;
	}



	public String getMinimumComplexityCoverage() {
		return minimumComplexityCoverage;
	}



	public String getMinimumLineCoverage() {
		return minimumLineCoverage;
	}



	public String getMinimumMethodCoverage() {
		return minimumMethodCoverage;
	}



	public String getMinimumClassCoverage() {
		return minimumClassCoverage;
	}



	public String getMaximumInstructionCoverage() {
		return maximumInstructionCoverage;
	}



	public String getMaximumBranchCoverage() {
		return maximumBranchCoverage;
	}



	public String getMaximumComplexityCoverage() {
		return maximumComplexityCoverage;
	}



	public String getMaximumLineCoverage() {
		return maximumLineCoverage;
	}



	public String getMaximumMethodCoverage() {
		return maximumMethodCoverage;
	}



	public String getMaximumClassCoverage() {
		return maximumClassCoverage;
	}


	public boolean isChangeBuildStatus() {
		return changeBuildStatus;
	}

    public boolean getChangeBuildStatus() {
		return changeBuildStatus;
	}

    public boolean isRunAlways() {
        return runAlways;
    }

    // Getter methods for delta coverage thresholds and build over build flag
    public String getDeltaInstructionCoverage() {
        return deltaInstructionCoverage;
    }

    public String getDeltaBranchCoverage() {
        return deltaBranchCoverage;
    }

    public String getDeltaComplexityCoverage() {
        return deltaComplexityCoverage;
    }

    public String getDeltaLineCoverage() {
        return deltaLineCoverage;
    }

    public String getDeltaMethodCoverage() {
        return deltaMethodCoverage;
    }

    public String getDeltaClassCoverage() {
        return deltaClassCoverage;
    }

    public boolean isBuildOverBuild() {
        return buildOverBuild;
    }

    @DataBoundSetter
    public void setExecPattern(String execPattern) {
        this.execPattern = execPattern;
    }

    @DataBoundSetter
    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    @DataBoundSetter
    public void setSourceInclusionPattern(String sourceInclusionPattern) {
        this.sourceInclusionPattern = sourceInclusionPattern;
    }

    @DataBoundSetter
    public void setSourceExclusionPattern(String sourceExclusionPattern) {
        this.sourceExclusionPattern = sourceExclusionPattern;
    }

    @DataBoundSetter
    public void setSkipCopyOfSrcFiles(boolean skipCopyOfSrcFiles) {
        this.skipCopyOfSrcFiles = skipCopyOfSrcFiles;
    }

    @DataBoundSetter
    public void setMinimumInstructionCoverage(String minimumInstructionCoverage) {
        this.minimumInstructionCoverage = minimumInstructionCoverage;
    }

    @DataBoundSetter
    public void setMinimumBranchCoverage(String minimumBranchCoverage) {
        this.minimumBranchCoverage = minimumBranchCoverage;
    }

    @DataBoundSetter
    public void setMinimumComplexityCoverage(String minimumComplexityCoverage) {
        this.minimumComplexityCoverage = minimumComplexityCoverage;
    }

    @DataBoundSetter
    public void setMinimumLineCoverage(String minimumLineCoverage) {
        this.minimumLineCoverage = minimumLineCoverage;
    }

    @DataBoundSetter
    public void setMinimumMethodCoverage(String minimumMethodCoverage) {
        this.minimumMethodCoverage = minimumMethodCoverage;
    }

    @DataBoundSetter
    public void setMinimumClassCoverage(String minimumClassCoverage) {
        this.minimumClassCoverage = minimumClassCoverage;
    }

    @DataBoundSetter
    public void setMaximumInstructionCoverage(String maximumInstructionCoverage) {
        this.maximumInstructionCoverage = maximumInstructionCoverage;
    }

    @DataBoundSetter
    public void setMaximumBranchCoverage(String maximumBranchCoverage) {
        this.maximumBranchCoverage = maximumBranchCoverage;
    }

    @DataBoundSetter
    public void setMaximumComplexityCoverage(String maximumComplexityCoverage) {
        this.maximumComplexityCoverage = maximumComplexityCoverage;
    }

    @DataBoundSetter
    public void setMaximumLineCoverage(String maximumLineCoverage) {
        this.maximumLineCoverage = maximumLineCoverage;
    }

    @DataBoundSetter
    public void setMaximumMethodCoverage(String maximumMethodCoverage) {
        this.maximumMethodCoverage = maximumMethodCoverage;
    }

    @DataBoundSetter
    public void setMaximumClassCoverage(String maximumClassCoverage) {
        this.maximumClassCoverage = maximumClassCoverage;
    }

    @DataBoundSetter
    public void setChangeBuildStatus(boolean changeBuildStatus) {
        this.changeBuildStatus = changeBuildStatus;
    }

    @DataBoundSetter
    public void setRunAlways(boolean runAlways) {
        this.runAlways = runAlways;
    }

    // Setter methods for delta coverage thresholds and build over build flag
    @DataBoundSetter
    public void setDeltaInstructionCoverage(String deltaInstructionCoverage) {
        this.deltaInstructionCoverage = deltaInstructionCoverage;
    }

    @DataBoundSetter
    public void setDeltaBranchCoverage(String deltaBranchCoverage) {
        this.deltaBranchCoverage = deltaBranchCoverage;
    }

    @DataBoundSetter
    public void setDeltaComplexityCoverage(String deltaComplexityCoverage) {
        this.deltaComplexityCoverage = deltaComplexityCoverage;
    }

    @DataBoundSetter
    public void setDeltaLineCoverage(String deltaLineCoverage) {
        this.deltaLineCoverage = deltaLineCoverage;
    }

    @DataBoundSetter
    public void setDeltaMethodCoverage(String deltaMethodCoverage) {
        this.deltaMethodCoverage = deltaMethodCoverage;
    }

    @DataBoundSetter
    public void setDeltaClassCoverage(String deltaClassCoverage) {
        this.deltaClassCoverage = deltaClassCoverage;
    }

    @DataBoundSetter
    public void setBuildOverBuild(boolean buildOverBuild) {
        this.buildOverBuild = buildOverBuild;
    }

	protected static void saveCoverageReports(FilePath destFolder, FilePath sourceFolder) throws IOException, InterruptedException {
		destFolder.mkdirs();

		sourceFolder.copyRecursiveTo(destFolder);
	}

    protected String resolveFilePaths(Run<?, ?> build, TaskListener listener, String input, Map<String, String> env)
            throws InterruptedException, IOException {
        try {
            final EnvVars environment = build.getEnvironment(listener);
            environment.overrideAll(env);
            return environment.expand(input);
        } catch (IOException e) {
            throw new IOException("Failed to resolve parameters in string \""+
                    input+"\"", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to resolve parameters in string \""+
                    input+"\"", e);
        }
    }

    protected String resolveFilePaths(AbstractBuild<?, ?> build, TaskListener listener, String input)
            throws InterruptedException, IOException {
        try {
            final EnvVars environment = build.getEnvironment(listener);
            environment.overrideAll(build.getBuildVariables());
            return environment.expand(input);
        } catch (IOException e) {
            throw new IOException("Failed to resolve parameters in string \""+
                    input+"\"", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to resolve parameters in string \""+
                    input+"\"", e);
        }
    }

    protected static FilePath[] resolveDirPaths(FilePath workspace, TaskListener listener, final String input)
            throws InterruptedException, IOException {
		return workspace.act(new ResolveDirPaths(input));
	}


    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath filePath, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener taskListener) throws InterruptedException, IOException {
        Map<String, String> envs = run instanceof AbstractBuild ? ((AbstractBuild<?,?>) run).getBuildVariables() : Collections.emptyMap();
        env.overrideAll(envs);

        healthReports = createCTHealthReportThresholds(env);

        // Initialize delta health report with user-configured threshold values
        deltaHealthReport = createCTDeltaHealthReportThresholds();

        if ((run.getResult() == Result.FAILURE || run.getResult() == Result.ABORTED) && !runAlways) {
            return;
        }

        final PrintStream logger = taskListener.getLogger();
        logger.println("[CT plugin] Collecting CT coverage data...");
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            Plugin plugin = instance.getPlugin("jacoco");
            if (plugin != null) {
                logger.println("[CT plugin] Version: " + plugin.getWrapper().getVersion());
            }
        }

        if ((execPattern==null) || (sourcePattern==null)) {
            if(run.getResult() != null && run.getResult().isWorseThan(Result.UNSTABLE) && !runAlways) {
                return;
            }

            logger.println("[CT plugin] ERROR: Missing configuration!");
            run.setResult(Result.FAILURE);
            return;
        }

        logger.println("[CT plugin] " + execPattern + ";" + sourcePattern + ";" + " locations are configured");

        CTReportDir reportDir = new CTReportDir(run.getRootDir());

        if (run instanceof AbstractBuild) {
            execPattern = resolveFilePaths((AbstractBuild<?,?>) run, taskListener, execPattern);
        }

        List<FilePath> matchedExecFiles = Arrays.asList(filePath.list(resolveFilePaths(run, taskListener, execPattern, env)));
        logger.println("[CT plugin] Number of found exec files for pattern " + execPattern + ": " + matchedExecFiles.size());
        logger.print("[CT plugin] Saving matched execfiles: ");
        reportDir.addExecFiles(matchedExecFiles);
        logger.print(" " + matchedExecFiles.stream().map(Object::toString).collect(Collectors.joining(" ")));
        final String warning = "\n[CT plugin] WARNING: You are using directory patterns with trailing /, /* or /** . This will most likely" +
                " multiply the copied files in your build directory. Check the list below and ignore this warning if you know what you are doing.";

        // Use skipCopyOfSrcFiles flag to determine if the source files should be copied or skipped. If skipped display appropriate logger message.
        if(!this.skipCopyOfSrcFiles) {
            FilePath[] matchedSrcDirs = resolveDirPaths(filePath, taskListener, sourcePattern);
            logger.print("\n[CT plugin] Saving matched source directories for source-pattern: " + sourcePattern + ": ");
            logger.print("\n[CT plugin] Source Inclusions: " + sourceInclusionPattern);
            logger.print("\n[CT plugin] Source Exclusions: " + sourceExclusionPattern);

            if (hasSubDirectories(sourcePattern)) {
                logger.print(warning);
            }

            for (FilePath dir : matchedSrcDirs) {
                int copied = reportDir.saveSourcesFrom(dir, sourceInclusionPattern, sourceExclusionPattern);
                logger.print("\n[CT plugin] - " + dir + " " + copied + " files");
            }
        }
        else{
            logger.print("\n[CT plugin] Skipping save of matched source directories for source-pattern: " + sourcePattern);
        }

        logger.println("\n[CT plugin] Loading inclusions files..");
        String[] includes = {};
        String[] excludes = {};
        
        final CTBuildAction action = CTBuildAction.load(healthReports, taskListener, reportDir, includes, excludes);
        action.getThresholds().ensureValid();
        logger.println("[CT plugin] Thresholds: " + action.getThresholds());
        run.addAction(action);

        logger.println("[CT plugin] Publishing the results..");
        final CoverageReport result = action.getResult();

        if (result == null) {
            logger.println("[CT plugin] Could not parse coverage results. Setting Build to failure.");
            run.setResult(Result.FAILURE);
        } else {
            logger.println("[CT plugin] Overall coverage: " 
                    + "call: " + result.getCallCoverage().getPercentageFloat()
                    + ", statement: " + result.getStatementCoverage().getPercentageFloat()
                    + ", branch: " + result.getBranchCoverage().getPercentageFloat()
                    + ", mcdc: " + result.getMCDCCoverage().getPercentageFloat());
            
            logger.println(String.format("[CT plugin] method: %d / %d", result.getCallCoverage().getCovered(), result.getCallCoverage().getMissed()));
            logger.println(String.format("[CT plugin] line: %d / %d", result.getStatementCoverage().getCovered(), result.getStatementCoverage().getMissed()));
            logger.println(String.format("[CT plugin] branch: %d / %d", result.getBranchCoverage().getCovered(), result.getBranchCoverage().getMissed()));
            
            //getLogger().println("[CT plugin] lineCoverage" + JacocoHealthReportThresholds.RESULT.BELOWMINIMUM.toString() + "==" + thresholds.getResultByTypeAndRatio(lineCoverage) );
            result.setThresholds(healthReports);

            // Calculate final result of the current build according to the state of two flags: changeBuildStatus and buildOverBuild
            // Final result is the logical AND of two operation results
            // Initializing individual operation result as SUCCESS to eliminate impact if the corresponding flag is not set
            Result applyMinMaxTh = Result.SUCCESS, applyDeltaTh = Result.SUCCESS;
            if (changeBuildStatus) {
                applyMinMaxTh = checkResult(action); // Compare current coverage with minimum and maximum coverage thresholds
                logger.println("[CT plugin] Health thresholds: "+ healthReports.toString());
                logger.println("[CT plugin] Apply Min/Max thresholds result: "+ applyMinMaxTh.toString());
            }
            if(buildOverBuild){
                applyDeltaTh = checkBuildOverBuildResult(run, logger); // Compute delta coverage of current build and compare with delta thresholds
                logger.println("[CT plugin] Delta thresholds: " + deltaHealthReport.toString());
                logger.println("[CT plugin] Results of delta thresholds check: "+ applyDeltaTh.toString());
            }
            if(changeBuildStatus || buildOverBuild) {
                run.setResult(Utils.applyLogicalAnd(applyMinMaxTh, applyDeltaTh));
            }
        }
    }

    private boolean hasSubDirectories(String pattern) {
        for (String dir : pattern.split(DIR_SEP)) {
            if (dir.endsWith("\\") || dir.endsWith("/") ||
                    dir.endsWith("\\*") || dir.endsWith("/*") ||
                    dir.endsWith("\\**") || dir.endsWith("/**")
                    ) {
                return true;
            }
        }
        return false;
    }

    private CTHealthReportThresholds createCTHealthReportThresholds(EnvVars env) {
        try {
            return healthReports = new CTHealthReportThresholds(
                    convertThresholdInputToInteger(minimumClassCoverage, env),
                    convertThresholdInputToInteger(maximumClassCoverage, env),
                    convertThresholdInputToInteger(minimumMethodCoverage, env),
                    convertThresholdInputToInteger(maximumMethodCoverage, env),
                    convertThresholdInputToInteger(minimumLineCoverage, env),
                    convertThresholdInputToInteger(maximumLineCoverage, env),
                    convertThresholdInputToInteger(minimumBranchCoverage, env),
                    convertThresholdInputToInteger(maximumBranchCoverage, env),
                    convertThresholdInputToInteger(minimumInstructionCoverage, env),
                    convertThresholdInputToInteger(maximumInstructionCoverage, env),
                    convertThresholdInputToInteger(minimumComplexityCoverage, env),
                    convertThresholdInputToInteger(maximumComplexityCoverage, env)
                );
        } catch (NumberFormatException e) {
            return healthReports = new CTHealthReportThresholds(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Creates JacocoHealthReportDeltaThresholds object to encapsulate user configured delta threshold values.
     * The values entered by the user are validated to be in range of [0, 100] percentage
     */
    private CTHealthReportDeltaThresholds createCTDeltaHealthReportThresholds(){
        return new CTHealthReportDeltaThresholds(this.deltaInstructionCoverage, this.deltaBranchCoverage, this.deltaComplexityCoverage, this.deltaLineCoverage, this.deltaMethodCoverage, this.deltaClassCoverage);
    }

    public static Result checkResult(CTBuildAction action) {
		if ((action.getBranchCoverage().getPercentageFloat() < action.getThresholds().getMinBranch()) || (action.getStatementCoverage().getPercentageFloat() < action.getThresholds().getMinStatement())  || (action.getMCDCCoverage().getPercentageFloat() < action.getThresholds().getMinMCDC())  || (action.getCallCoverage().getPercentageFloat() < action.getThresholds().getMinCall())) {
			return Result.FAILURE;
		}
		if ((action.getBranchCoverage().getPercentageFloat() < action.getThresholds().getMaxBranch()) || (action.getStatementCoverage().getPercentageFloat() < action.getThresholds().getMaxStatement())  || (action.getMCDCCoverage().getPercentageFloat() < action.getThresholds().getMaxMCDC())  || (action.getCallCoverage().getPercentageFloat() < action.getThresholds().getMaxCall())) {
			return Result.UNSTABLE;
		}
		return Result.SUCCESS;
	}

    // Calculates actual delta coverage of the current build by subtracting it's coverage from coverage of last successful build
    // and compares if the delta coverage is less than or equal to user-configured delta thresholds
    // Returns success (if delta coverage is equal to or less than delta thresholds) OR (if delta coverage is bigger than delta thresholds AND current coverage is bigger than last successful coverage)
    public Result checkBuildOverBuildResult(Run<?,?> run, PrintStream logger){

        CTDeltaCoverageResultSummary deltaCoverageResultSummary = CTDeltaCoverageResultSummary.build(run);
        logger.println("[CT plugin] Delta coverage: "
                + ", method: " + deltaCoverageResultSummary.getCallCoverage()
                + ", statement: " + deltaCoverageResultSummary.getStatementCoverage()
                + ", branch: " + deltaCoverageResultSummary.getBranchCoverage()
                + ", complexity: " + deltaCoverageResultSummary.getMCDCCoverage());

        /*
         * Coverage thresholds will not be checked for any parameter for which the coverage has increased.
         * Only if the coverage for a particular parameter has decreased, we will check the configured threshold for that parameter.
         * [JENKINS-58184] - This fix ensures that build will never fail in case coverage reduction is within the threshold limits.
         */
        if(( deltaCoverageResultSummary.getBranchCoverage() > 0 || Math.abs(deltaCoverageResultSummary.getBranchCoverage()) <= deltaHealthReport.getDeltaBranch()) &&
                ( deltaCoverageResultSummary.getMCDCCoverage() > 0 || Math.abs(deltaCoverageResultSummary.getMCDCCoverage()) <= deltaHealthReport.getDeltaComplexity()) &&
                ( deltaCoverageResultSummary.getStatementCoverage() > 0 || Math.abs(deltaCoverageResultSummary.getStatementCoverage()) <= deltaHealthReport.getDeltaLine()) &&
                ( deltaCoverageResultSummary.getCallCoverage() > 0 || Math.abs(deltaCoverageResultSummary.getCallCoverage()) <= deltaHealthReport.getDeltaMethod()) )
            return Result.SUCCESS;
        else
            return Result.FAILURE;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return (BuildStepDescriptor<Publisher>)super.getDescriptor();
    }

    /**
     * @deprecated
     *      use injection via {@link Jenkins#getInjector()}
     */
    public static /*final*/ BuildStepDescriptor<Publisher> DESCRIPTOR;

    private static void setDescriptor(BuildStepDescriptor<Publisher> descriptor) {
        DESCRIPTOR = descriptor;
    }

    @Extension @Symbol("ct")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(CTPublisher.class);
            setDescriptor(this);
        }

		@NonNull
        @Override
        public String getDisplayName() {
            return Messages.CTPublisher_DisplayName();
        }

		@Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

		/*@Override
        public Publisher newInstance(StaplerRequest req, JSONObject json) throws FormException {
            JacocoPublisher pub = new JacocoPublisher();
            req.bindParameters(pub, "jacoco.");
            req.bindParameters(pub.healthReports, "jacocoHealthReports.");
            // start ugly hack
            //@TODO remove ugly hack
            // the default converter for integer values used by req.bindParameters
            // defaults an empty value to 0. This happens even if the type is Integer
            // and not int.  We want to change the default values, so we use this hack.
            //
            // If you know a better way, please fix.
            if ("".equals(req.getParameter("jacocoHealthReports.maxClass"))) {
                pub.healthReports.setMaxClass(100);
            }
            if ("".equals(req.getParameter("jacocoHealthReports.maxMethod"))) {
                pub.healthReports.setMaxMethod(70);
            }
            if ("".equals(req.getParameter("jacocoHealthReports.maxLine"))) {
                pub.healthReports.setMaxLine(70);
            }
            if ("".equals(req.getParameter("jacocoHealthReports.maxBranch"))) {
                pub.healthReports.setMaxBranch(70);
            }
            if ("".equals(req.getParameter("jacocoHealthReports.maxInstruction"))) {
                pub.healthReports.setMaxInstruction(70);
            }
            if ("".equals(req.getParameter("jacocoHealthReports.maxComplexity"))) {
                pub.healthReports.setMaxComplexity(70);
            }
            // end ugly hack
            return pub;
        }*/

    }

    private static class ResolveDirPaths extends MasterToSlaveFileCallable<FilePath[]> {
        static final long serialVersionUID = 1552178457453558870L;
        private final String input;

        public ResolveDirPaths(String input) {
            this.input = input;
        }

        public FilePath[] invoke(File f, VirtualChannel channel) {
            FilePath base = new FilePath(f);
            ArrayList<FilePath> localDirectoryPaths= new ArrayList<>();
            String[] includes = input.split(DIR_SEP);
            DirectoryScanner ds = new DirectoryScanner();

            ds.setIncludes(includes);
            ds.setCaseSensitive(false);
            ds.setBasedir(f);
            ds.scan();
            String[] dirs = ds.getIncludedDirectories();

            for (String dir : dirs) {
                localDirectoryPaths.add(base.child(dir));
            }
            FilePath[] lfp = {};//trick to have an empty array as a parameter, so the returned array will contain the elements
            return localDirectoryPaths.toArray(lfp);
        }

    }
    
    //private static final Logger logger = Logger.getLogger(JacocoPublisher.class.getName());

	public String getClassPattern() {
		return StringUtils.EMPTY;
	}
}
