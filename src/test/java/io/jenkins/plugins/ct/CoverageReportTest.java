package io.jenkins.plugins.ct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import io.jenkins.plugins.ct.CTHealthReportThresholds;
import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement;
import io.jenkins.plugins.ct.report.CoverageReport;

/**
 * @author Kohsuke Kawaguchi
 * @author David Carver - Refactored for cleaner seperation of tests
 */
public class CoverageReportTest   {
	@Test
	public void printAllCoverages() throws Exception {
        CoverageReport r = new CoverageReport(null, getClass().getResourceAsStream("sample.xml"));
        r.setThresholds(new CTHealthReportThresholds(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        String result = r.printFourCoverageColumns();
        for(var p : r.getChildren().values() ) {
        	p.printFourCoverageColumns();
        }
        
        assertTrue(result.contains("M:</b> 1033 <b>C:</b> 956"));
        assertTrue(result.contains("M:</b> 268 <b>C:</b> 422"));
        assertTrue(result.contains("M:</b> 181 <b>C:</b> 229"));
        assertTrue(result.contains("M:</b> 334 <b>C:</b> 916"));
    }

}
