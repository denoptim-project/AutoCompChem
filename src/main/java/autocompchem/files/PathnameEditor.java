package autocompchem.files;

import java.io.File;

/*   
 *   Copyright (C) 2024  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.AtomLabelsGenerator.AtomLabelMode;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.constraints.ConstraintDefinition;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Tool to edit pathnames.
 * 
 * @author Marco Foscato
 */


public class PathnameEditor extends Worker
{
    /**
     * The input pathname to work with
     */
	private File input;
	
	/**
	 * String meant to be pre-pended (i.e., added before, no separator)
	 * to the processed pathname
	 */
	private String prefix = "";
	
	/**
	 * String meant to be appended (i.e., no separator)
	 * to the processed pathname
	 */
	private String suffix = "";
	
	/**
	 * String meant to be added both before and after 
	 * to the processed pathname, like quotation marks.
	 */
	private String quotationMark = "";
	
    /**
     * String defining the task of getting and possibly editing a pathname.
     */
    public static final String GETPATHNAMETASKNAME = "getPathName";

    /**
     * Task about getting and possibly editing a pathname.
     */
    public static final Task GETPATHNAMETASK;
    static {
    	GETPATHNAMETASK = Task.make(GETPATHNAMETASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public PathnameEditor()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GETPATHNAMETASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/PathnameEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new PathnameEditor();
    }
    
//-----------------------------------------------------------------------------

	@Override
	public void initialize() {
		
        // Define verbosity
        if (params.contains("VERBOSITY"))
        {
            String v = params.getParameter("VERBOSITY").getValueAsString();
            this.verbosity = Integer.parseInt(v);
        }

        // Multiple ways to define the input pathname are here processes from
        // the lowest priority to the highest. This here we implicitly define
        // the priority order.
        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            this.input = new File(
            		params.getParameter(ChemSoftConstants.PAROUTFILEROOT)
            		.getValueAsString());
        }
        if (params.contains(ChemSoftConstants.PARPATHNAMEROOT))
        {
            this.input = new File(
            		params.getParameter(ChemSoftConstants.PARPATHNAMEROOT)
            		.getValueAsString());
        }
        if (params.contains("PATHNAME"))
        {
            this.input = new File(
            		params.getParameter("PATHNAME").getValueAsString());
        }

        if (params.contains("PREFIX"))
        {
            this.prefix = params.getParameter("PREFIX").getValueAsString();
        }
        
        if (params.contains("SUFFIX"))
        {
            this.suffix = params.getParameter("SUFFIX").getValueAsString();
        }
        
        if (params.contains("QUOTATION"))
        {
            this.quotationMark = 
            		params.getParameter("QUOTATION").getValueAsString();
        }
	}
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialized.
     */

    @Override
    public void performTask()
    {
    	if (task.equals(GETPATHNAMETASK))
    	{
    		getPathName();
    	} else {
    		dealWithTaskMistMatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Processes the input given upon initialization to produce a processed
     * pathname.
     */

    public String getPathName()
    {
        if (input==null)
        {
            Terminator.withMsgAndStatus("ERROR! Missing input pathname. "
                + "Nothing to do",-1);
        }
        
        String processedPathname = processPathname(input.getPath());
        
        String result = quotationMark
        		+ prefix 
        		+ processedPathname 
        		+ suffix
        		+ quotationMark;
        
        if (exposedOutputCollector != null)
    	{
	        exposeOutputData(new NamedData(GETPATHNAMETASK.ID, 
	        		NamedDataType.STRING, result));
    	}
        
        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * NB: this method is a placeholder of pathname processing functionality,
     * which is, however, not yet available.
     * 
     * @param original
     * @return the processed string
     */
    public String processPathname(String original)
    {
    	// NB: no processing is currently implemented
    	return original;
    }
    
//-----------------------------------------------------------------------------

}
