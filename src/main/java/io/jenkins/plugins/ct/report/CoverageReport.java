package io.jenkins.plugins.ct.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;

import hudson.model.Run;

import org.apache.commons.digester3.Digester;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.internal.analysis.CounterImpl;
import org.jacoco.core.internal.analysis.MethodCoverageImpl;
import org.jacoco.core.tools.ExecFileLoader;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.objectweb.asm.Type;
import org.xml.sax.SAXException;

import hudson.util.HttpResponses;
import io.jenkins.plugins.ct.CTBuildAction;
import io.jenkins.plugins.ct.CTHealthReportThresholds;
import io.jenkins.plugins.ct.ExecutionFileLoader;
import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement;

/**
 * Root object of the coverage report.
 * 
 * @author Kohsuke Kawaguchi
 * @author Ognjen Bubalo
 */
public final class CoverageReport extends AggregatedReport<CoverageReport/*dummy*/,CoverageReport, MethodReport> {
	private final CTBuildAction action;

	private CoverageReport(CTBuildAction action) {
		this.action = action;
		setName("Jacoco");
	}
	
//	private String instructionColor;
//	private String classColor;
//	private String branchColor;
//	private String complexityColor;
//	private String lineColor;
//	private String methodColor;
	public CTHealthReportThresholds healthReports;

	/**
	 * Loads the exec files using JaCoCo API. Creates the reporting objects and the report tree.
	 * 
	 * @param action Jacoco build action
	 * @param executionFileLoader execution file loader owning bundle coverage
	 */
	public CoverageReport(CTBuildAction action, @NonNull ExecutionFileLoader executionFileLoader ) {
		this(action);
		action.getLogger().println("[JaCoCo plugin] Loading packages..");

		action.getLogger().println("[JaCoCo plugin] Done.");
	}
	
	public CoverageReport(CTBuildAction action, InputStream... xmlReports) throws IOException {
		this(action);
		//action.getLogger().println("[JaCoCo plugin] Loading packages..");
        for (InputStream is: xmlReports) {
          try {
            createDigester(!Boolean.getBoolean(this.getClass().getName() + ".UNSAFE")).parse(is);
          } catch (SAXException e) {
              throw new IOException("Failed to parse XML",e);
          }
        }
        setParent(null);
        //action.getLogger().println("[JaCoCo plugin] Done.");
    }
	
	public CoverageReport(CTBuildAction action, File xmlReport) throws IOException {
        this(action);
        action.getLogger().println("[JaCoCo plugin] Loading packages..");
        try {
            createDigester(!Boolean.getBoolean(this.getClass().getName() + ".UNSAFE")).parse(xmlReport);
        } catch (SAXException e) {
            throw new IOException("Failed to parse "+xmlReport,e);
        }
        setParent(null);
        action.getLogger().println("[JaCoCo plugin] Done.");
    }

    /**
     * From Jacoco: Checks if a class name is anonymous or not.
     * 
     * @param vmname
     * @return
     */
    private boolean isAnonymous(final String vmname) {
        final int dollarPosition = vmname.lastIndexOf('$');
        if (dollarPosition == -1) {
            return false;
        }
        final int internalPosition = dollarPosition + 1;
        if (internalPosition == vmname.length()) {
            // shouldn't happen for classes compiled from Java source
            return false;
        }
        // assume non-identifier start character for anonymous classes
        final char start = vmname.charAt(internalPosition);
        return !Character.isJavaIdentifierStart(start);
    }
    


    /**
     * Returns a method name for the method, including possible parameter names.
     * 
     * @param classCov
     *            Coverage Information about the Class
     * @param methodCov
     *            Coverage Information about the Method
     * @return method name
     */
    private String getMethodName(IClassCoverage classCov, IMethodCoverage methodCov) {
        if ("<clinit>".equals(methodCov.getName()))
            return "static {...}";

        StringBuilder sb = new StringBuilder();
        if ("<init>".equals(methodCov.getName())) {
            if (isAnonymous(classCov.getName())) {
                return "{...}";
            }
            
            int pos = classCov.getName().lastIndexOf('/');
            String name = pos == -1 ? classCov.getName() : classCov.getName().substring(pos + 1);
            sb.append(name.replace('$', '.'));
        } else {
            sb.append(methodCov.getName());
        }
        
        sb.append('(');
        final Type[] arguments = Type.getArgumentTypes(methodCov.getDesc());
        boolean comma = false;
        for(final Type arg : arguments) {
            if(comma) {
                sb.append(", ");
            } else {
                comma = true;
            }
            
            String name = arg.getClassName();
            int pos = name.lastIndexOf('.');
            String shortname = pos == -1 ? name : name.substring(pos + 1);
            sb.append(shortname.replace('$', '.'));
        }
        sb.append(')');

        return sb.toString();
    }

    static final NumberFormat dataFormat = new DecimalFormat("000.00", new DecimalFormatSymbols(Locale.US));
    static final NumberFormat percentFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
	
	@Override
	protected void printRatioCell(boolean failed, Coverage ratio, StringBuilder buf) {
		if (ratio != null && ratio.isInitialized()) {
			String bgColor = "#FFFFFF";
			
			if (CTHealthReportThresholds.RESULT.BETWEENMINMAX == healthReports.getResultByTypeAndRatio(ratio)) {
				bgColor = "#FF8000";
			} else if (CTHealthReportThresholds.RESULT.BELOWMINIMUM == healthReports.getResultByTypeAndRatio(ratio)) {
				bgColor = "#FF0000";
			}
			buf.append("<td bgcolor='").append(bgColor).append("'");
			buf.append(" data='").append(dataFormat.format(ratio.getPercentageFloat()));
			buf.append("'>\n");
			printRatioTable(ratio, buf);
			buf.append("</td>\n");
		}
	}
	
	@Override
	protected void printRatioTable(Coverage ratio, StringBuilder buf){
		buf.append("<table class='percentgraph' cellpadding='0' cellspacing='0'><tr class='percentgraph'>")
		.append("<td style='width:40px' class='data'>").append(ratio.getPercentage()).append("%</td>")
		.append("<td class='percentgraph'>")
		.append("<div class='percentgraph' style='width:100px'>")
		.append("<div class='redbar' style='width:")
		.append(100 - ratio.getPercentage()).append("px'>")
		.append("</div></div></td></tr><tr><td colspan='2'>")
		.append("<span class='text'><b>M:</b> ").append(ratio.getMissed())
		.append(" <b>C:</b> ").append(ratio.getCovered()).append("</span></td></tr></table>\n");
	}

	@Override
	public CoverageReport getPreviousResult() {
		CTBuildAction prev = action.getPreviousResult();
		if(prev!=null) {
			return prev.getResult();
		}
		
		return null;
	}

	@Override
	public Run<?,?> getBuild() {
		return action.getOwner();
	}

    /**
     * Serves a single jacoco.exec file that merges all that have been recorded.
     * @return HTTP response serving a single jacoco.exec file, or error 404 if nothing has been recorded. 
     * @throws IOException if any I/O error occurs
     */
    @WebMethod(name="jacoco.exec")
    public HttpResponse doJacocoExec() throws IOException {
        final List<File> files = action.getJacocoReport().getExecFiles();

        switch (files.size()) {
        case 0:
            return HttpResponses.error(404, "No jacoco.exec file recorded");
        case 1:
            return HttpResponses.staticResource(files.get(0));
        default:
            // TODO: perhaps we want to cache the merged result?
            return new HttpResponse() {
                public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                    ExecFileLoader loader = new ExecFileLoader();
                    for (File exec : files) {
                        loader.load(exec);
                    }
                    rsp.setContentType("application/octet-stream");
                    final ExecutionDataWriter dataWriter = new ExecutionDataWriter(rsp.getOutputStream());
                    loader.getSessionInfoStore().accept(dataWriter);
                    loader.getExecutionDataStore().accept(dataWriter);
                }
            };
        }
    }
    
    private Digester createDigester(boolean secure) throws SAXException {
        Digester digester = new Digester();
        if (secure) {
            digester.setXIncludeAware(false);
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException ex) {
                throw new SAXException("Failed to securely configure xml digester parser", ex);
            }
        }
        digester.setClassLoader(getClass().getClassLoader());

        digester.push(this);
                
        digester.addObjectCreate( "*/method", MethodReport.class);
        digester.addSetNext(      "*/method","add");
        digester.addSetProperties("*/method");

        digester.addObjectCreate("*/counter", CoverageElement.class);
        digester.addSetProperties("*/counter");
        digester.addSetNext(      "*/counter","addCoverage");
        
        digester.addObjectCreate("*/line", SourceFileReport.class);
        digester.addSetProperties("*/line");
        digester.addSetNext(      "*/line","add");

        return digester;
    }


	public void setThresholds(CTHealthReportThresholds healthReports) {
		this.healthReports = healthReports;
		/*if (healthReports.getMaxBranch() < branch.getPercentage()) {
			branchColor = "#000000";
		} else if (healthReports.getMinBranch() < branch.getPercentage()) {
			branchColor = "#FF8000";
		} else {
			branchColor = "#FF0000";
		}
		*/
	}

    @Override
    public void add(MethodReport child) {
    	MethodCoverageImpl coverageInfo = new MethodCoverageImpl(child.getName(), child.getDesc(), child.getDisplayName());
    	
    	for (var line : child.getChildren().values()) {
    		coverageInfo.increment(CounterImpl.getInstance(line.getMs(), line.getCs()), CounterImpl.getInstance(line.getMb(), line.getCb()), line.getNr());
    	}
    	coverageInfo.incrementMethodCounter();
    	child.setSrcFileInfo(coverageInfo);
        this.getChildren().put(child.getName(), child);
        //logger.log(Level.INFO, "PackageReport");
    }

}
