package io.jenkins.plugins.ctcoveragecolumn;

import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.Run;
import hudson.search.QuickSilver;
import hudson.util.StreamTaskListener;
import io.jenkins.plugins.ct.CTBuildAction;
import io.jenkins.plugins.ct.CTReportDir;
import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement.Type;
import io.jenkins.plugins.ctcoveragecolumn.CTColumn;
import net.sf.json.JSONObject;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.export.Exported;

import javax.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.*;

public class JaCoCoColumnTest {
	private CTColumn jaCoCoColumn;

    @Before
	public void setUp() {
		jaCoCoColumn = new CTColumn();
	}

	@Test
	public void testGetPercentWithBuildAndAction() {
		ServletContext context = EasyMock.createNiceMock(ServletContext.class);
		
		EasyMock.replay(context);

		final Job<?, ?> mockJob = new MyJob("externaljob") {
			@Override
			@Exported
			@QuickSilver
			@SuppressWarnings("deprecation") // avoid TransientActionFactory
			public MyRun getLastSuccessfulBuild() {
				try {
				    MyRun newBuild = newBuild();
					Map<Type, Coverage> ratios = new HashMap<>();
					ratios.put(Type.STATEMENT, new Coverage(200, 100));
					newBuild.addAction(new CTBuildAction(ratios, null, null, StreamTaskListener.fromStdout(), null, null));
					assertEquals(1, newBuild.getActions().size());
					return newBuild;
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			protected synchronized void saveNextBuildNumber() {
			}
		};
		assertTrue(jaCoCoColumn.hasCoverage(mockJob));
		assertEquals("33.33", jaCoCoColumn.getPercent(mockJob));
		assertEquals(new BigDecimal("33.33"), jaCoCoColumn.getCoverage(mockJob));

		EasyMock.verify(context);
	}

	@Test
	public void testDescriptor() throws FormException {
		assertNotNull(jaCoCoColumn.getDescriptor());
		assertNotNull(
				jaCoCoColumn.getDescriptor().newInstance(null, JSONObject.fromObject("{\"key\":\"value\"}")));
		assertNotNull(jaCoCoColumn.getDescriptor().getDisplayName());
	}

	private class MyJob extends Job<MyJob,MyRun> {

        public MyJob(String name) {
            super(null, name);
        }

        @Override
        public boolean isBuildable() {
            return false;
        }

        @Override
        protected SortedMap<Integer, MyRun> _getRuns() {
            return null;
        }

        @Override
        protected void removeRun(MyRun run) {
        }

        protected synchronized MyRun newBuild() throws IOException {
            return new MyRun(this);
        }
	}
	
	private class MyRun extends Run<MyJob,MyRun> {

        public MyRun(MyJob job) throws IOException {
            super(job);
        }
	}
}
