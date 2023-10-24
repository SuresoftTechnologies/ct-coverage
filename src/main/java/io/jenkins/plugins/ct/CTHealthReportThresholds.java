package io.jenkins.plugins.ct;

import java.io.Serializable;

import io.jenkins.plugins.ct.model.Coverage;
import io.jenkins.plugins.ct.model.CoverageElement.Type;

/**
 * Holds the configuration details for {@link hudson.model.HealthReport} generation
 *
 * @author Stephen Connolly
 * @since 1.7
 */
public class CTHealthReportThresholds implements Serializable {
    private int minCall;
    private int maxCall;
    private int minStatement;
    private int maxStatement;
    private int minBranch;
    private int maxBranch;
    private int minMCDC;
    private int maxMCDC;

    public CTHealthReportThresholds() {
    }
    
    public CTHealthReportThresholds(
    		int minClass, int maxClass, int minMethod, int maxMethod, int minLine, int maxLine,
    		int minBranch, int maxBranch, int minInstruction, int maxInstruction, int minComplexity, int maxComplexity) {
        this.minCall = minMethod;
        this.maxCall = maxMethod;
        this.minStatement = minLine;
        this.maxStatement = maxLine;
		this.minBranch = minBranch;
		this.maxBranch = maxBranch;
		this.minMCDC = minComplexity;
		this.maxMCDC = maxComplexity;
        ensureValid();
    }

    private int applyRange(int min , int value, int max) {
        if (value < min) {
        	return min;
		}

        if (value > max) {
        	return max;
		}

        return value;
    }

    public enum RESULT {BELOWMINIMUM, BETWEENMINMAX, ABOVEMAXIMUM}
    
    public void ensureValid() {
        maxCall = applyRange(0, maxCall, 100);
        minCall = applyRange(0, minCall, maxCall);
        maxStatement = applyRange(0, maxStatement, 100);
        minStatement = applyRange(0, minStatement, maxStatement);
        maxBranch = applyRange(0, maxBranch, 100);
        minBranch = applyRange(0, minBranch, maxBranch);
        maxMCDC = applyRange(0, maxMCDC, 100);
        minMCDC = applyRange(0, minMCDC, maxMCDC);
    }

    public int getMinCall() {
        return minCall;
    }

    public void setMinCall(int minMethod) {
        this.minCall = minMethod;
    }

    public int getMaxCall() {
        return maxCall;
    }

    public void setMaxCall(int maxMethod) {
        this.maxCall = maxMethod;
    }

    public int getMinStatement() {
        return minStatement;
    }

    public void setMinStatement(int minLine) {
        this.minStatement = minLine;
    }

    public int getMaxStatement() {
        return maxStatement;
    }

    public void setMaxStatement(int maxLine) {
        this.maxStatement = maxLine;
    }

	public int getMinBranch() {
		return minBranch;
	}

	public int getMaxBranch() {
		return maxBranch;
	}

	public int getMinMCDC() {
		return minMCDC;
	}

	public int getMaxMCDC() {
		return maxMCDC;
	}

	public void setMinBranch(int minBranch) {
		this.minBranch = minBranch;
	}

	public void setMaxBranch(int maxBranch) {
		this.maxBranch = maxBranch;
	}

	public void setMinMCDC(int minComplexity) {
		this.minMCDC = minComplexity;
	}

	public void setMaxMCDC(int maxComplexity) {
		this.maxMCDC = maxComplexity;
	}

	public  RESULT getResultByTypeAndRatio(Coverage ratio) {
		RESULT result = RESULT.ABOVEMAXIMUM;
		Type covType = ratio.getType();
		float percentage = ratio.getPercentageFloat();

		if (covType == Type.BRANCH) {
			if (percentage < minBranch) {
				result = RESULT.BELOWMINIMUM;
			} else if (percentage < maxBranch) {
				result = RESULT.BETWEENMINMAX;
			}
		} else if (covType == Type.STATEMENT) {
			if (percentage < minStatement) {
				result = RESULT.BELOWMINIMUM;
			} else if (percentage < maxStatement) {
				result = RESULT.BETWEENMINMAX;
			}
		} else if (covType == Type.MCDC) {
			if (percentage < minMCDC) {
				result = RESULT.BELOWMINIMUM;
			} else if (percentage < maxMCDC) {
				result = RESULT.BETWEENMINMAX;
			}
		} else if (covType == Type.CALL) {
			if (percentage < minCall) {
				result = RESULT.BELOWMINIMUM;
			} else if (percentage < maxCall) {
				result = RESULT.BETWEENMINMAX;
			}
		}
			 
		return result;
	}

	@Override
	public String toString() {
		return "JacocoHealthReportThresholds [minMethod=" + minCall
				+ ", maxMethod=" + maxCall + ", minLine=" + minStatement
				+ ", maxLine=" + maxStatement + ", minBranch=" + minBranch
				+ ", maxBranch=" + maxBranch
				+ ", minComplexity=" + minMCDC + ", maxComplexity="
				+ maxMCDC + "]";
	}
}
