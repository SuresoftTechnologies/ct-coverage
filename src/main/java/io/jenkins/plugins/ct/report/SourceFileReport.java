package io.jenkins.plugins.ct.report;


/**
 * @author Kohsuke Kawaguchi
 */
public final class SourceFileReport extends AbstractReport<MethodReport,SourceFileReport> {
	private int nr;
	private int ms;
	private int cs;
	private int mb;
	private int cb;
	
	@Override
    public void setName(String name) {
        super.setName(name.replace('/', '.'));
    	//logger.log(Level.INFO, "SourceFileReport");
    }
		
	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}


	public int getMs() {
		return ms;
	}


	public void setMs(int ms) {
		this.ms = ms;
	}


	public int getCs() {
		return cs;
	}


	public void setCs(int cs) {
		this.cs = cs;
	}


	public int getMb() {
		return mb;
	}


	public void setMb(int mb) {
		this.mb = mb;
	}


	public int getCb() {
		return cb;
	}


	public void setCb(int cb) {
		this.cb = cb;
	}
	//private static final Logger logger = Logger.getLogger(SourceFileReport.class.getName());
}
