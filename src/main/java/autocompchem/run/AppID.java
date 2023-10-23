package autocompchem.run;

/**
 * Identifier of applications of various kinds that can perform a job. Some
 * applications can be run from within AutoCompChem. Others are known to the
 * extent we can define a job for such apps., but we cannot run them directly.
 */


// TODO: consider setting the way to make jobs of external apps (Gaussian etc.)
// by reading a configuration file that allows to define how to set a ShellJob
// that acts as interface between ACC and the external app.
// This would allow to define settings once, and use them every time a job
// of the configured app is found.

public enum AppID 
{
    UNDEFINED,
    
    ACC,
    SHELL,
    
    GAUSSIAN,
    NWCHEM,
    ORCA,
    XTB,
    SPARTAN;

	private boolean isRunnableByACC = false;
	
	static {
		SHELL.isRunnableByACC = true;
		ACC.isRunnableByACC = true;
	}
	
//------------------------------------------------------------------------------
	
	public boolean isRunnableByACC()
	{
		return isRunnableByACC ? true : false;
	}

//------------------------------------------------------------------------------
}