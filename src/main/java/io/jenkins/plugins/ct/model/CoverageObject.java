package io.jenkins.plugins.ct.model;

import hudson.Util;
import hudson.model.Api;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import io.jenkins.plugins.ct.CTHealthReportThresholds;
import io.jenkins.plugins.ct.Rule;
import io.jenkins.plugins.ct.model.CoverageGraphLayout.Axis;
import io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType;
import io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageValue;
import io.jenkins.plugins.ct.model.CoverageGraphLayout.Plot;
import io.jenkins.plugins.ct.report.AggregatedReport;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.jacoco.core.analysis.ICoverageNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;


/**
 * Base class of all coverage objects.
 *
 * @author Kohsuke Kawaguchi
 * @author Martin Heinzerling
 * @param <SELF> self-type
 */
@ExportedBean
public abstract class CoverageObject<SELF extends CoverageObject<SELF>> {
    //private static final Logger logger = Logger.getLogger(CoverageObject.class.getName());

	public Coverage call = new Coverage();	//method
	public Coverage statement = new Coverage();	//line
	public Coverage mcdc = new Coverage();	// complexity
	public Coverage branch = new Coverage();



	/**
	 * Variables used to store which child has to highest coverage for each coverage type.
	 */
	public int maxCall=1;
	public int maxStatement=1;
	public int maxMcdc=1;
	public int maxBranch=1;

	private volatile boolean failed = false;
	private int kind = 0;

    /**
     * @return the maxMethod
     */
    public int getMaxCall() {
        return maxCall;
    }

    /**
     * @param maxCall the maxMethod to set
     */
    public void setMaxCall(int maxCall) {
        this.maxCall = maxCall;
    }

    /**
     * @return the maxLine
     */
    public int getMaxStatement() {
        return maxStatement;
    }

    /**
     * @param maxStatement the maxLine to set
     */
    public void setMaxStatement(int maxStatement) {
        this.maxStatement = maxStatement;
    }

    /**
     * @return the maxComplexity
     */
    public int getMaxMCDC() {
        return maxMcdc;
    }

    /**
     * @param maxComplexity the maxComplexity to set
     */
    public void setMaxMCDC(int maxMCDC) {
        this.maxMcdc = maxMCDC;
    }

    /**
     * @return the maxBranch
     */
    public int getMaxBranch() {
        return maxBranch;
    }

    /**
     * @param maxBranch the maxBranch to set
     */
    public void setMaxBranch(int maxBranch) {
        this.maxBranch = maxBranch;
    }

    public boolean isFailed() {
		return failed;
	}

	/**
	 * Marks this coverage object as failed.
	 * @see Rule
	 */
	public void setFailed() {
		failed = true;
	}
	

	public int getKind() {
		return kind;
	}

	public void setKind(int kind) {
		this.kind = kind;
	}

	@Exported(inline=true)
	public Coverage getCallCoverage() {
		return call;
	}

	@Exported(inline=true)
	public Coverage getMCDCCoverage() {
		return mcdc;
	}

	@Exported(inline=true)
	public Coverage getBranchCoverage() {
		return branch;
	}

	/**
	 * Line coverage. Can be null if this information is not collected.
	 * @return Line coverage.
	 */
	@Exported(inline=true)
	public Coverage getStatementCoverage() {
		return statement;
	}

	/**
	 * Gets the build object that owns the whole coverage report tree.
	 * @return the build object that owns the whole coverage report tree.
	 */
	public abstract Run<?,?> getBuild();

	/**
	 * Gets the corresponding coverage report object in the previous
	 * run that has the record.
	 *
	 * @return
	 *      null if no earlier record was found.
	 */
	@Exported
	public abstract SELF getPreviousResult();
	
	public CoverageObject<?> getParent() {return null;}

	/**
	 * Used in the view to print out four table columns with the coverage info.
	 * @return HTML code.
	 */
	public String printFourCoverageColumns() {
		StringBuilder buf = new StringBuilder();
		mcdc.setType(CoverageElement.Type.MCDC);
		branch.setType(CoverageElement.Type.BRANCH);
		statement.setType(CoverageElement.Type.STATEMENT);
		call.setType(CoverageElement.Type.CALL);

		printRatioCell(isFailed(), statement, buf);
		printRatioCell(isFailed(), branch, buf);
		printRatioCell(isFailed(), mcdc, buf);
		printRatioCell(isFailed(), call, buf);
		return buf.toString();
	}

	public boolean hasStatementCoverage() {
		return statement.isInitialized();
	}

	public boolean hasClassCoverage() {
		return false;
	}


	static NumberFormat dataFormat = new DecimalFormat("000.00", new DecimalFormatSymbols(Locale.US));
	static NumberFormat percentFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
	static NumberFormat intFormat = new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));

	protected void printRatioCell(boolean failed, Coverage ratio, StringBuilder buf) {
		if (ratio != null && ratio.isInitialized()) {
			String bgColor = "#FFFFFF";
			
			buf.append("<td bgcolor='").append(bgColor).append("'");
			buf.append(" data='").append(dataFormat.format(ratio.getPercentageFloat()));
			buf.append("'>\n");
			printRatioTable(ratio, buf);
			buf.append("</td>\n");
		}
	}

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
	
	protected <ReportLevel extends AggregatedReport<?,?,?> > void setAllCovTypes( ReportLevel reportToSet, ICoverageNode covReport) {
		
		Coverage tempCov = new Coverage();
		
		tempCov.accumulate(covReport.getBranchCounter().getMissedCount(), covReport.getBranchCounter().getCoveredCount());
		reportToSet.branch = tempCov;
		
		tempCov = new Coverage();
		tempCov.accumulate(covReport.getLineCounter().getMissedCount(), covReport.getLineCounter().getCoveredCount());
		reportToSet.statement = tempCov;
				
		tempCov = new Coverage();
		tempCov.accumulate(covReport.getMethodCounter().getMissedCount(), covReport.getMethodCounter().getCoveredCount());
		reportToSet.call = tempCov;
		
		tempCov = new Coverage();
		tempCov.accumulate(covReport.getComplexityCounter().getMissedCount(), covReport.getComplexityCounter().getCoveredCount());
		reportToSet.mcdc = tempCov;
		
	}
	
	public  < ReportLevel extends AggregatedReport<?,?,?> > void setCoverage( ReportLevel reportToSet, ICoverageNode covReport) {
		
		setAllCovTypes(reportToSet, covReport);
		
		if (this.maxBranch < reportToSet.branch.getTotal()) {
			this.maxBranch = reportToSet.branch.getTotal();
		}

		if (this.maxStatement < reportToSet.statement.getTotal()) {
			this.maxStatement = reportToSet.statement.getTotal();
		}
		
		if (this.maxCall < reportToSet.call.getTotal()) {
			this.maxCall = reportToSet.call.getTotal();
		}

		if (this.maxMcdc < reportToSet.mcdc.getTotal()) {
			this.maxMcdc = reportToSet.mcdc.getTotal();
		}
	}

	/**
	 * Generates the graph that shows the coverage trend up to this report.
	 * @param req Stapler request from which context, graph width and graph height are read
	 * @param rsp Stapler response to which is sent the graph
	 * @throws IOException if any I/O error occurs
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		if(ChartUtil.awtProblemCause != null) {
			// not available. send out error message
			rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
			return;
		}

		Run<?,?> build = getBuild();
		Calendar t = build.getTimestamp();

		String w = Util.fixEmptyAndTrim(req.getParameter("width"));
		String h = Util.fixEmptyAndTrim(req.getParameter("height"));
		int width = (w != null) ? Integer.parseInt(w) : 500;
		int height = (h != null) ? Integer.parseInt(h) : 200;

		CoverageGraphLayout layout = new CoverageGraphLayout()
				.baseStroke(4f)
				.axis()
				.plot().type(CoverageType.STATEMENT).value(CoverageValue.PERCENTAGE).color(Color.GREEN)
				.plot().type(CoverageType.MCDC).value(CoverageValue.PERCENTAGE).color(Color.DARK_GRAY)
				.plot().type(CoverageType.CALL).value(CoverageValue.PERCENTAGE).color(Color.BLUE)
				.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.RED);

		createGraph(t, width, height,layout).doPng(req, rsp);
	}

	GraphImpl createGraph(final Calendar t, final int width, final int height, final CoverageGraphLayout layout) throws IOException
	{
		return new GraphImpl(this, t, width, height, layout)
		{
			@Override
			protected Map<Axis, DataSetBuilder<String, NumberOnlyBuildLabel>> createDataSetBuilder(CoverageObject<SELF> obj)
			{
				Map<Axis, DataSetBuilder<String, NumberOnlyBuildLabel>> builders = new LinkedHashMap<>();
				for (Axis axis : layout.getAxes())
				{
					builders.put(axis, new DataSetBuilder<String, NumberOnlyBuildLabel>());
					if (axis.isCrop()) bounds.put(axis, new Bounds());
				}

				Map<Plot, Number> last = new HashMap<>();
				for (CoverageObject<SELF> a = obj; a != null; a = a.getPreviousResult())
				{
					NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(a.getBuild());
					for (Plot plot : layout.getPlots())
					{
						Number value = plot.getValue(a);
						Axis axis = plot.getAxis();
						if (axis.isSkipZero() && (value == null || value.floatValue() == 0f)) value = null;
						if (value != null)
						{
							if (axis.isCrop()) bounds.get(axis).update(value);
							last.put(plot, value);
						}
						else
						{
							value = last.get(plot);
						}
						builders.get(axis).add(value, plot.getMessage(), label);
					}
				}
				return builders;
			}
		};
	}

	public Api getApi() {
		return new Api(this);
	}

	abstract class GraphImpl extends Graph {

		private CoverageObject<SELF> obj;
		private CoverageGraphLayout layout;
		protected Map<Axis,Bounds> bounds = new HashMap<>();

		protected class Bounds
		{
			float min=Float.MAX_VALUE;
			float max=Float.MIN_VALUE;

			public void update(Number value)
			{
				float v=value.floatValue();
				if (min>v) min=v;
				if (max<v) max=v+1;
			}
		}

		public GraphImpl(CoverageObject<SELF> obj, Calendar timestamp, int defaultW, int defaultH, CoverageGraphLayout layout) {
			super(timestamp, defaultW, defaultH);
			this.obj = obj;
			this.layout =layout;
		}

		protected abstract Map<Axis, DataSetBuilder<String, NumberOnlyBuildLabel>> createDataSetBuilder(CoverageObject<SELF> obj);

		public JFreeChart getGraph( )
		{
			return createGraph();
		}

		@Override
		protected JFreeChart createGraph() {
			Map<Axis, CategoryDataset> dataSets = new LinkedHashMap<>();
			Map<Axis, DataSetBuilder<String, NumberOnlyBuildLabel>> dataSetBuilders = createDataSetBuilder(obj);
			for (Entry<Axis, DataSetBuilder<String, NumberOnlyBuildLabel>> e : dataSetBuilders.entrySet())
			{
				dataSets.put(e.getKey(), e.getValue().build());
			}
			List<Axis> axes = new ArrayList<>(dataSets.keySet());
			boolean onlyOneBuild = dataSets.entrySet().iterator().next().getValue().getColumnCount() < 2;

			final JFreeChart chart = ChartFactory.createLineChart(
					null, // chart title
					null, // unused
					null, // range axis label
					dataSets.get(axes.get(0)), // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltips
					false // urls
			);

			final CategoryPlot plot = chart.getCategoryPlot();

			CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
			plot.setDomainAxis(domainAxis);
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
			domainAxis.setLowerMargin(onlyOneBuild ? 0.5 : 0.0);
			domainAxis.setUpperMargin(0.0);
			domainAxis.setCategoryMargin(0.0);

			int axisId = 0;
			for (Axis axis : axes)
			{
				int di = axisId;
				plot.setDataset(di, dataSets.get(axis));
				plot.mapDatasetToRangeAxis(di, axisId);
				NumberAxis numberAxis = new NumberAxis(axis.getLabel());
				plot.setRangeAxis(axisId, numberAxis);
				numberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); //TODO
				setBounds(axis, numberAxis);
				axisId++;
			}

			layout.apply(chart, onlyOneBuild);
			return chart;
		}

		private void setBounds(Axis a, ValueAxis axis)
		{
			if (!a.isCrop()) return;
			Bounds bounds = this.bounds.get(a);
			float border = (bounds.max - bounds.min) / 100 * a.getCrop();
			axis.setUpperBound(bounds.max + border);
			axis.setLowerBound(Math.max(0, bounds.min - border));
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":"
				+ " branch=" + branch
				+ " complexity=" + mcdc
				+ " line=" + statement
				+ " method=" + call;
	}
}
