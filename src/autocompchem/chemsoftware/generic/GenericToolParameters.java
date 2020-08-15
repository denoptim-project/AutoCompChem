package autocompchem.chemsoftware.generic;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;

/**
 * Storage of user-defined parameters for GenericTool related tools.
 * With GenericTool we refer to any tools that can be executed automatically
 * and is capable of producing an output or log file with information 
 * about the outcome of the job performed with such tool.
 *
 * @author Marco Foscato
 */

public class GenericToolParameters extends ParameterStorage
{

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public GenericToolParameters()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from ParameterStorage
     * @param ps the storage of parameters to expand
     */

    public GenericToolParameters(ParameterStorage ps)
    {
        super();
        for (String k : ps.getRefNamesSet())
        {
        	this.setParameter(ps.getParameter(k));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the string identifying the beginning of a single job step of 
     * a GeneticTool
     * @return the string identifying the beginning of a single job step
     */

    public String getOutputInitialMsg()
    {
        Parameter par  =  getParameterOrDefault(
                                            GenericToolConstants.DEFINITMSGKEY,
                                            NamedDataType.STRING,
                                            GenericToolConstants.DEFINITMSG);
        return par.getValue().toString();
    }

//------------------------------------------------------------------------------

    /**
     * Returns the string identifying the normal termination of single job step
     * of a GeneticTool
     * @return the string identifying the normal termination of a single job
     * step
     */

    public String getOutputNormalEndMsg()
    {
        Parameter par  =  getParameterOrDefault(
                                          GenericToolConstants.DEFNORMENDMSGKEY,
                                          NamedDataType.STRING,
                                          GenericToolConstants.DEFNORMENDMSG);
        return par.getValue().toString();
    }

//------------------------------------------------------------------------------

}
