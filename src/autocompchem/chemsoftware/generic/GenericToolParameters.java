package autocompchem.chemsoftware.generic;

/*   
 *   Copyright (C) 2014  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import autocompchem.parameters.Parameter;
import autocompchem.parameters.ParameterStorage;

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

    /**
     * Default class hosding constant values
     */
    private String cls = "autocompchem.chemsoftware.generic.GenericToolConstants";


//------------------------------------------------------------------------------

    /**
     * Constructor from ParameterStorage
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
	super(ps.getAllParameters());
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
			                                                   cls);
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
                                                                           cls);
        return par.getValue().toString();
    }

//------------------------------------------------------------------------------

}