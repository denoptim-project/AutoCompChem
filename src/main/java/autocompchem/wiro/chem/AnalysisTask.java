package autocompchem.wiro.chem;

import autocompchem.datacollections.ParameterStorage;

/**
 * A class representing the request of a kind of analysis and some settings
 * associated with the analysis task.
 * 
 * @author Marco Foscato
 */
public class AnalysisTask 
{
    
	/**
	 * The kind of analysis to perform
	 */
	private AnalysisKind kind;
	
    /**
     * Possible kinds of analysis that can be asked for jobs/steps
     */
    public enum AnalysisKind {
    	SCFENERGY,
    	SAVELASTGEOMETRY,
    	SAVEALLGEOM,
    	SAVEVIBMODE,
    	CRITICALPOINTKIND,
    	QHTHERMOCHEMISTRY,
    	BLVSCONNECTIVITY}
    
    /**
     * Settings that control how the analysis is performed
     */
    private ParameterStorage params = new ParameterStorage();
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor 
     * @param kind the kind of analysis to perform
     */
    public AnalysisTask(AnalysisKind kind)
    {
    	this.kind = kind;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor 
     * @param kind the kind of analysis to perform
     * @param params settings for the analysis
     */
    public AnalysisTask(AnalysisKind kind, ParameterStorage params)
    {
    	this.kind = kind;
    	this.params = params;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Set the parameters associated with this analysis task.
     * @param params settings for the analysis
     */
    public void setParams(ParameterStorage params)
    {
    	this.params = params;
    }

//-----------------------------------------------------------------------------
    
    /**
     * @param kind the kind of analysis represented by this object
     */
    public void setKind(AnalysisKind kind)
    {
    	this.kind = kind;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return the kind of analysis represented by this object
     */
    public AnalysisKind getKind()
    {
    	return kind;
    }
     
//-----------------------------------------------------------------------------
    
    /**
     * Returns the parameters meant to control the analysis
     * @return the parameters meant to control the analysis
     */
    public ParameterStorage getParams()
    {
    	return params;
    }
    
//-----------------------------------------------------------------------------

    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("[AnalysisTask: ").append(kind).append(" ");
    	sb.append(params.toLinesParametersFileFormat()).append("]");
    	return sb.toString();
    }
    
//-----------------------------------------------------------------------------
    
}
