package io.jenkins.plugins.ct;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import io.jenkins.plugins.ct.CTPublisher.DescriptorImpl;

public class JacocoPublisherTest  {
    private final TaskListener taskListener = niceMock(TaskListener.class);
    private final Launcher launcher = niceMock(Launcher.class);
	private StringBuilder logContent;

    @Before
    public void setUp() {
		logContent = new StringBuilder();
		expect(taskListener.getLogger()).andReturn(new PrintStream(System.out) {
													   @Override
													   public void print(String s) {
														   super.print(s);
														   logContent.append(s);
													   }
												   }
		).anyTimes();
	}

	public static void main(String[] args) {
		
	}


}
