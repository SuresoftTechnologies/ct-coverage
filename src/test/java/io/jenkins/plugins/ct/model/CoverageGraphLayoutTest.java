package io.jenkins.plugins.ct.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType;

import java.util.Locale;

import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType.BRANCH;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType.CALL;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType.MCDC;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageType.STATEMENT;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageValue.COVERED;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageValue.MISSED;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageValue.PERCENTAGE;
import static io.jenkins.plugins.ct.model.CoverageGraphLayout.CoverageValue.values;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.junit.Assert.assertEquals;

public class CoverageGraphLayoutTest {

    private Locale localeBackup;

    @Before
    public void setUp() {
        localeBackup = Locale.getDefault();
    }

    @After
    public void tearDown() {
        Locale.setDefault(localeBackup);
    }

    @Test
    public void type() {
        Locale.setDefault(ENGLISH);

        assertEquals("New Coverage Types", 4, CoverageType.values().length);
        
        assertEquals("line", STATEMENT.getMessage());
        assertEquals("branch", BRANCH.getMessage());
        assertEquals("method", CALL.getMessage());
        assertEquals("complexity", MCDC.getMessage());

        Locale.setDefault(GERMAN);
        assertEquals("Zeilen", STATEMENT.getMessage());
        assertEquals("Branch", BRANCH.getMessage());
        assertEquals("Methoden", CALL.getMessage());
        
        // JDK 8 has a problem with encoding here, JDK 11 works, so we
        // need to check for both until we do not use JDK 8 any more
        assertEquals("Had: " + MCDC.getMessage(),
                "Komplexität", MCDC.getMessage());
    }

    @Test
    public void value() {
        Locale.setDefault(ENGLISH);
        assertEquals("New Coverage Value", 3, values().length);
        assertEquals("line covered", COVERED.getMessage(STATEMENT));
        assertEquals("line missed", MISSED.getMessage(STATEMENT));
        assertEquals("line", PERCENTAGE.getMessage(STATEMENT));

        Locale.setDefault(GERMAN);
        assertEquals("Zeilen abgedeckt", COVERED.getMessage(STATEMENT));
        assertEquals("Zeilen nicht abgedeckt", MISSED.getMessage(STATEMENT));
        assertEquals("Zeilen", PERCENTAGE.getMessage(STATEMENT));
    }
}