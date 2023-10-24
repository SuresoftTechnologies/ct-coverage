package io.jenkins.plugins.ctcoveragecolumn;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.ListViewColumn;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * View column that shows the code line coverage percentage
 *
 */
public class CTColumn extends AbstractJaCoCoCoverageColumn {

	@DataBoundConstructor
	public CTColumn() {
	}

	@Override
	protected Float getPercentageFloat(final Run<?, ?> lastSuccessfulBuild) {
		return getPercentageFloat(lastSuccessfulBuild,
				(a) -> a.getStatementCoverage().getPercentageFloat());
	}

	@Extension
	public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

	@Override
	public Descriptor<ListViewColumn> getDescriptor() {
		return DESCRIPTOR;
	}

	private static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public ListViewColumn newInstance(final StaplerRequest req,
										  @NonNull final JSONObject formData) {
			return new CTColumn();
		}

		@Override
		public boolean shownByDefault() {
			return false;
		}

		@NonNull
		@Override
		public String getDisplayName() {
			return "JaCoCo Line Coverage";
		}
	}
}
