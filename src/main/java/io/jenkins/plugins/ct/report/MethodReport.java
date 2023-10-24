package io.jenkins.plugins.ct.report;

import java.io.File;
import java.io.Writer;

import org.jacoco.core.analysis.IMethodCoverage;

import io.jenkins.plugins.ct.model.CoverageElement;

/**
 * @author Kohsuke Kawaguchi
 * @author David Carver
 * @author Ognjen Bubalo
 */
//AggregatedReport<PackageReport,ClassReport,MethodReport>  -  AbstractReport<ClassReport,MethodReport>
public class MethodReport extends AggregatedReport<CoverageReport,MethodReport, SourceFileReport> {

	private IMethodCoverage methodCov;
	private String source;
	private String name;
	private String desc;
	private String line;

	@Override
	public String printFourCoverageColumns() {
        StringBuilder buf = new StringBuilder();
        mcdc.setType(CoverageElement.Type.MCDC);
        branch.setType(CoverageElement.Type.BRANCH);
        statement.setType(CoverageElement.Type.STATEMENT);
        call.setType(CoverageElement.Type.CALL);
        
        printRatioCell(isFailed(), this.statement, buf);
        printRatioCell(isFailed(), this.branch, buf);
		printRatioCell(isFailed(), this.mcdc, buf);
        printRatioCell(isFailed(), this.call, buf);
        //logger.log(Level.INFO, "Printing Ratio cells within MethodReport.");
		return buf.toString();
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	@Override
	public void add(SourceFileReport child) {
		getChildren().put(Integer.toString(child.getNr()), child);
        //logger.log(Level.INFO, "SourceFileReport");
    }

    @Override
    public boolean hasClassCoverage() {
        return false;
    }

	public void setSrcFileInfo(IMethodCoverage methodCov) {
		this.methodCov = methodCov;
	}

    public void printHighlightedSrcFile(Writer output) {
    	new SourceAnnotator(new File(this.source)).printHighlightedSrcFile(methodCov,output);
   	}
}
