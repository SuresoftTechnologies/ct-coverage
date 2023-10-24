package io.jenkins.plugins.ct.portlet.chart;

import org.junit.Test;

import io.jenkins.plugins.ct.portlet.chart.CTBuilderTrendChart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JacocoBuilderTrendChartTest {
    @Test
    public void construct() throws Exception {
        CTBuilderTrendChart chart = new CTBuilderTrendChart("chart", "200", "500", "3");
        assertEquals(3, chart.getDaysNumber());
        assertEquals(200, chart.getWidth());
        assertEquals(500, chart.getHeight());

        // causes an NPE because Stapler.getCurrentRequest() returns null: assertNotNull(chart.getSummaryGraph());
    }

    @Test
    public void descriptor() throws Exception {
        CTBuilderTrendChart.DescriptorImpl descriptor = new CTBuilderTrendChart.DescriptorImpl();
        assertNotNull(descriptor.getDisplayName());
    }
}