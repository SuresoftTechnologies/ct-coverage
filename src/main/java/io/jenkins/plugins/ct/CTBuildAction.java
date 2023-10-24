package io.jenkins.plugins.ct;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import edu.umd.cs.findbugs.annotations.Nullable;

import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerProxy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement;
import io.jenkins.plugins.ct.model.CoverageObject;
import io.jenkins.plugins.ct.model.CoverageElement.Type;
import io.jenkins.plugins.ct.report.CoverageReport;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep.LastBuildAction;

/**
 * Build view extension by JaCoCo plugin.
 *
 * As {@link CoverageObject}, it retains the overall coverage report.
 *
 * @author Kohsuke Kawaguchi
 * @author Jonathan Fuerth
 * @author Ognjen Bubalo
 */
public final class CTBuildAction extends CoverageObject<CTBuildAction> implements HealthReportingAction, StaplerProxy, Serializable, RunAction2, LastBuildAction {
    private static final long serialVersionUID = 1L;

	private transient Run<?,?> owner;
	
	@Deprecated public transient AbstractBuild<?,?> build;
	
	private final transient PrintStream logger;
	@Deprecated private transient ArrayList<?> reports;
	private transient WeakReference<CoverageReport> report;
	private final String[] inclusions;
	private final String[] exclusions;
	private transient CTReportDir layout;
 
	/**
	 * The thresholds that applied when this build was built.
	 * TODO: add ability to trend thresholds on the graph
	 */
	private final CTHealthReportThresholds thresholds;
	private transient CTProjectAction jacocoProjectAction;

	/**
	 * 
	 * @param ratios
	 *            The available coverage ratios in the report. Null is treated
	 *            the same as an empty map.
	 * @param thresholds
	 *            The thresholds that applied when this build was built.
	 * @param listener
	 *            The listener from which we get logger
	 * @param inclusions
	 *            See {@link CTReportDir#parse(String[], String...)}
	 * @param exclusions
	 *            See {@link CTReportDir#parse(String[], String...)}
	 */
	public CTBuildAction(
			Map<CoverageElement.Type, Coverage> ratios, CTReportDir layout,
			CTHealthReportThresholds thresholds, TaskListener listener, String[] inclusions, String[] exclusions) {
		logger = listener.getLogger();
		if (ratios == null) {
			ratios = Collections.emptyMap();
		}
		this.layout = layout;
		this.inclusions = inclusions != null ? Arrays.copyOf(inclusions, inclusions.length) : null;
		this.exclusions = exclusions != null ? Arrays.copyOf(exclusions, exclusions.length) : null;
		this.call = getOrCreateRatio(ratios, CoverageElement.Type.CALL);
		this.statement = getOrCreateRatio(ratios, CoverageElement.Type.STATEMENT);
		this.thresholds = thresholds;
		this.branch = getOrCreateRatio(ratios, CoverageElement.Type.BRANCH);
		this.mcdc = getOrCreateRatio(ratios, CoverageElement.Type.MCDC);
	}

	private Coverage getOrCreateRatio(Map<CoverageElement.Type, Coverage> ratios, CoverageElement.Type type) {
		Coverage r = ratios.get(type);
		if (r == null) {
			r = new Coverage();
		}
		return r;
	}

	public String getDisplayName() {
		return Messages.BuildAction_DisplayName();
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public String getUrlName() {
		return "jacoco";
	}


	/**
	 * Get the coverage {@link hudson.model.HealthReport}.
	 *
	 * @return The health report or <code>null</code> if health reporting is disabled.
	 * @since 1.7
	 */
	public HealthReport getBuildHealth() {
		if (thresholds == null) {
			// no thresholds => no report
			return null;
		}
		thresholds.ensureValid();
		int score = 100;
		float percent;
		ArrayList<Localizable> reports = new ArrayList<>(5);
		
		if (call != null && thresholds.getMaxCall() > 0) {
			percent = call.getPercentageFloat();
			if (percent < thresholds.getMaxCall()) {
				reports.add(Messages._BuildAction_Calls(call, percent));
			}
			score = updateHealthScore(score, thresholds.getMinCall(),
					percent, thresholds.getMaxCall());
		}
		if (statement != null && thresholds.getMaxStatement() > 0) {
			percent = statement.getPercentageFloat();
			if (percent < thresholds.getMaxStatement()) {
				reports.add(Messages._BuildAction_Statements(statement, percent));
			}
			score = updateHealthScore(score, thresholds.getMinStatement(),
					percent, thresholds.getMaxStatement());
		}
		if (branch != null && thresholds.getMaxBranch() > 0) {
			percent = branch.getPercentageFloat();
			if (percent < thresholds.getMaxBranch()) {
				reports.add(Messages._BuildAction_Branches(branch, percent));
			}
			score = updateHealthScore(score, thresholds.getMinBranch(),
					percent, thresholds.getMaxBranch());
		}
		if (score == 100) {
			reports.add(Messages._BuildAction_Perfect());
		}
		// Collect params and replace nulls with empty string
		//throw new RuntimeException("Jebiga");
		Object[] args = reports.toArray(new Object[5]);
		for (int i = 4; i >= 0; i--) {
			if (args[i]==null) {
				args[i] = "";
			} else {
				break;
			}
		}
		return new HealthReport(score, Messages._BuildAction_Description(
				args[0], args[1], args[2], args[3], args[4]));
	}

	public CTHealthReportThresholds getThresholds() {
		return thresholds;
	}

	private static int updateHealthScore(int score, int min, float value, int max) {
		if (value >= max) {
			return score;
		}
		if (value <= min) {
			return 0;
		}
		assert max != min;
		final int scaled = (int) (100.0 * (value - min) / (max - min));
		if (scaled < score) {
			return scaled;
		}
		return score;
	}

	public Object getTarget() {
		return getResult();
	}

	@Override
	public Run<?,?> getBuild() {
		return owner;
	}

    public CTReportDir getJacocoReport() {
        return new CTReportDir(owner.getRootDir());
    }
    
    protected static List<FilePath> getReports(List<File> files) throws IOException, InterruptedException {
    	List<FilePath> paths = new ArrayList<>();
    	for ( File file : files) {
    		paths.add(new FilePath(file));
    	}
		return paths;
	}

	/**
	 * Obtains the detailed {@link CoverageReport} instance.
	 * @return the report, or null if these was a problem
	 */
	public synchronized @Nullable CoverageReport getResult() {

		if(report!=null) {
			final CoverageReport r = report.get();
			if(r!=null) {
				return r;
			}
		}

		try {
			getLogger().println("[Build Action] load report");
			// Get the list of report files stored for this build
			
			List<FilePath> reports = getReports(this.layout.getExecFiles());
            InputStream[] streams = new InputStream[reports.size()];
            for (int i=0; i<reports.size(); i++) {
            	streams[i] = reports.get(i).read();
            }
            
			CoverageReport r = new CoverageReport(this, streams);
			report = new WeakReference<>(r);
			r.setThresholds(thresholds);
			return r;
		} catch (IOException | RuntimeException | InterruptedException e) {
			getLogger().println("Failed to load ");
			e.printStackTrace(getLogger());
			return null;
		}
	}

	@Override
	public CTBuildAction getPreviousResult() {
		return getPreviousResult(owner);
	}

	/**
	 * @return A map which represents coverage objects and their status to show on build status page (summary.jelly).
	 */
	public Map<Coverage,Boolean> getCoverageRatios(){
		CoverageReport result = getResult();
		Map<Coverage,Boolean> ratios = new LinkedHashMap<>();
		if( result != null ) {
			Coverage mcdcScore = result.getMCDCCoverage();
			Coverage branchCoverage = result.getBranchCoverage();
			Coverage statementCoverage = result.getStatementCoverage();
			Coverage callCoverage = result.getCallCoverage();

			mcdcScore.setType(CoverageElement.Type.MCDC);			
			branchCoverage.setType(CoverageElement.Type.BRANCH);			
			statementCoverage.setType(CoverageElement.Type.STATEMENT);
			callCoverage.setType(CoverageElement.Type.CALL);
			
			ratios.put(statementCoverage,CTHealthReportThresholds.RESULT.BELOWMINIMUM == thresholds.getResultByTypeAndRatio(statementCoverage));
			ratios.put(branchCoverage,CTHealthReportThresholds.RESULT.BELOWMINIMUM == thresholds.getResultByTypeAndRatio(branchCoverage));
			ratios.put(mcdcScore,CTHealthReportThresholds.RESULT.BELOWMINIMUM == thresholds.getResultByTypeAndRatio(mcdcScore));
			ratios.put(callCoverage,CTHealthReportThresholds.RESULT.BELOWMINIMUM == thresholds.getResultByTypeAndRatio(callCoverage));
			
		}
		return ratios;
	}
	
	/**
	 * Gets the previous {@link CTBuildAction} of the given build.
	 */
	/*package*/ static CTBuildAction getPreviousResult(Run<?,?> start) {
		Run<?,?> b = start;
		while(true) {
			b = b.getPreviousBuild();
			if(b==null) {
				return null;
			}
			if (b.isBuilding() || b.getResult() == Result.FAILURE || b.getResult() == Result.ABORTED) {
				continue;
			}
			CTBuildAction r = b.getAction(CTBuildAction.class);
			if(r!=null) {
				return r;
			}
		}
	}

	/**
	 * Constructs the object from JaCoCo exec files.
	 * @param thresholds
	 *            The thresholds that applied when this build was built.
	 * @param listener
	 *            The listener from which we get logger
	 * @param layout
	 *             The object parsing the saved "jacoco.exec" files
     * @param includes
     *            See {@link CTReportDir#parse(String[], String...)}
     * @param excludes
     *            See {@link CTReportDir#parse(String[], String...)}
	 * @return new {@code JacocoBuildAction} from JaCoCo exec files
	 * @throws IOException
	 *      if failed to parse the file.
	 */
	public static CTBuildAction load(CTHealthReportThresholds thresholds, TaskListener listener, CTReportDir layout, String[] includes, String[] excludes) throws IOException {
		Map<CoverageElement.Type,Coverage> ratios = loadRatios(layout, includes, excludes);
		return new CTBuildAction(ratios, layout, thresholds, listener, includes, excludes);
	}


	/**
	 * Extracts top-level coverage information from the JaCoCo report document.
	 */
	private static Map<Type, Coverage> loadRatios(CTReportDir layout, String[] includes, String... excludes) throws IOException {
		
		Map<CoverageElement.Type,Coverage> ratios = new LinkedHashMap<>();
        try {
    		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
	        for (File exec : layout.getExecFiles()) {
	
	        	try {
					Document document = builder.parse(exec.getAbsolutePath());
					Element root = document.getDocumentElement();
					var coverages = root.getElementsByTagName("counter");
					for (int k = 0; k < coverages.getLength(); k++) {
						Node node = coverages.item(k);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
		                    Element coverage = (Element) node;
		                    var type = coverage.getAttribute("type");
		                    var covered = coverage.getAttribute("covered");
		                    var missed = coverage.getAttribute("missed");
		                    
		                    Coverage ratio = new Coverage();
		                    ratio.accumulatePP(Integer.parseInt(missed), Integer.parseInt(covered));
		                    if ( type.equals("STATEMENT")) {
		                		ratios.put(CoverageElement.Type.STATEMENT, ratio);
		                    } 
		                    else if (type.equals("BRANCH")) {
		                    	ratios.put(CoverageElement.Type.BRANCH, ratio);
		                    }
		                    else if (type.equals("CALL")) {
		                    	ratios.put(CoverageElement.Type.CALL, ratio);
		                    }
		                    else if (type.equals("MCDC")) {
		                    	ratios.put(CoverageElement.Type.MCDC, ratio);
		                    }
						}
					}
					
					
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	
	        
	    }
	    catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ratios;

	}
	
	//private static final Logger logger = Logger.getLogger(JacocoBuildAction.class.getName());
	public final PrintStream getLogger() {
	    if(logger != null) {
	        return logger;
	    }

	    // use System.out as a fallback if the BuildAction was de-serialized which
	    // does not run the construct and thus leaves the transient variables empty
	    return System.out;
	}

	public Run<?, ?> getOwner() {
		return owner;
	}

	private void setOwner(Run<?, ?> owner) {
		jacocoProjectAction = new CTProjectAction(owner.getParent());
		this.owner = owner;
	}

	@Override
	public void onAttached(Run<?, ?> run) {
		setOwner(run);
	}

	@Override
	public void onLoad(Run<?, ?> run) {
		setOwner(run);
	}

	@Override
	public Collection<? extends Action> getProjectActions() {
		return jacocoProjectAction != null ? Collections.singletonList(jacocoProjectAction) : Collections.emptyList();
	}
}
