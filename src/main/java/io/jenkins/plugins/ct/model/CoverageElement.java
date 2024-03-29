package io.jenkins.plugins.ct.model;

import java.io.IOException;

import io.jenkins.plugins.ct.report.AbstractReport;

/**
 * This is a transitive object used during the parsing, but not a part of
 * the final tree built. 
 *
 * @author Kohsuke Kawaguchi
 */
public final class CoverageElement {

    /**
     * Enumeration of coverage types that appear in a JaCoCo report.
     * 
     * @author Jonathan Fuerth &lt;jfuerth@gmail.com&gt;
     */
    public enum Type {
      
      BRANCH {
        @Override
        public Coverage getAssociatedRatio(AbstractReport<?, ?> from) {
          return from.branch;
        }
      },
      STATEMENT {
          @Override
          public Coverage getAssociatedRatio(AbstractReport<?, ?> from) {
            return from.statement;
          }
        },
      CALL {
            @Override
            public Coverage getAssociatedRatio(AbstractReport<?, ?> from) {
              return from.call;
            }
          },
      MCDC {
              @Override
              public Coverage getAssociatedRatio(AbstractReport<?, ?> from) {
                return from.mcdc;
              }
           }
      ;
      
      /**
       * Returns the ratio object on the given report that tracks this type of coverage.
       * 
       * @param from The report to return the appropriate Coverage object from. Not null.
       * @return the ratio object on the given report that tracks this type of coverage.
       */
      public abstract Coverage getAssociatedRatio(AbstractReport<?,?> from);
    }

    private Type type;
    private int missed;
    private int covered;

    /**
     * Returns the enum constant that says what type of coverage this bean
     * represents.
     * <p>
     * Warning: don't call this method getType() because that confuses the
     * Digester.
     * @return the enum constant that says what type of coverage this bean represents.
     */
    public Type getTypeAsEnum() {
        return type;
    }
    
    // set by attributes
    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    // set by attributes
    public void setMissed(int missed) {
      this.missed = missed;
    }

    // set by attributes
    public void setCovered(int covered) {
      this.covered = covered;
    }

    public void addTo(AbstractReport<?,?> report) throws IOException {
        type.getAssociatedRatio(report).accumulate(missed, covered);
    }
}
