package io.jenkins.plugins.ct;

import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Encapsulates the directory structure in $JENKINS_HOME where we store jacoco related files.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTReportDir {
    private final File root;

    public CTReportDir(File rootDir) {
        root = new File(rootDir, "ct");
    }

    /**
     * Where we store *.class files, honoring package names as directories.
     * @return Directory to which we store *.class files, honoring package names as directories.
     */
    public File getClassesDir() {
        return new File(root,"classes");
    }

    public int saveClassesFrom(@NonNull FilePath dir, @NonNull String fileMask) throws IOException, InterruptedException {
        FilePath d = new FilePath(getClassesDir());
        d.mkdirs();
        return dir.copyRecursiveTo(fileMask, d);
    }

    /**
     * Where we store *.java files, honoring package names as directories.
     * @return Directory to which we store *.java files, honoring package names as directories.
     */
    public File getSourcesDir() {
        return new File(root,"sources");
    }

    public int saveSourcesFrom(@NonNull FilePath dir, @NonNull String inclusionMask, @NonNull String exclusionMask) throws IOException, InterruptedException {
        FilePath d = new FilePath(getSourcesDir());
        d.mkdirs();
        return dir.copyRecursiveTo(inclusionMask, exclusionMask, d);
    }

    /**
     * Root directory that stores jacoco.exec files.
     * Each exec file is stored in its own directory.
     * @return Directory that stores jacoco.exec files.
     *
     * @see #getXmlFiles()
     */
    public File getCoverageReportFilesDir() {
        return new File(root,"xmlFiles");
    }

    /**
     * Lists up existing jacoco.exec files.
     * @return List of existing jacoco.exec files.
     */
    public List<File> getXmlFiles() {
        List<File> r = new ArrayList<>();
        int i = 0;
        File root = getCoverageReportFilesDir();
        File checkPath;
        while ((checkPath = new File(root, "xml" + i)).exists()) {
            r.add(new File(checkPath,"coverage.xml"));
            i++;
        }

        return r;
    }

    public void addExecFiles(Iterable<FilePath> execFiles) throws IOException, InterruptedException {
        FilePath root = new FilePath(getCoverageReportFilesDir());
        int i=0;
        for (FilePath file : execFiles) {
            FilePath separateExecDir;
            do {
                separateExecDir = new FilePath(root, "xml"+(i++));
            } while (separateExecDir.exists());

        	FilePath fullExecName = separateExecDir.child("coverage.xml");
        	file.copyTo(fullExecName);
        }
    }

    /**
     * Parses the saved "jacoco.exec" files into an {@link ExecutionFileLoader}.
     * @param includes see {@link ExecutionFileLoader#setIncludes}
     * @param excludes see {@link ExecutionFileLoader#setExcludes}
     * @return the configured {@code ExecutionFileLoader}
     * @throws IOException if any I/O error occurs
     */
    public ExecutionFileLoader parse(String[] includes, String... excludes) throws IOException {
        ExecutionFileLoader efl = new ExecutionFileLoader();
        for (File exec : getXmlFiles()) {
            efl.addExecFile(new FilePath(exec));
        }

        efl.setIncludes(includes);
        efl.setExcludes(excludes);
        efl.setClassDir(new FilePath(getClassesDir()));
        efl.setSrcDir(new FilePath(getSourcesDir()));
        efl.loadBundleCoverage();

        return efl;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
