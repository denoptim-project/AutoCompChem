package autocompchem.wiro;

import java.io.File;
import java.io.IOException;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.ACCFileType;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.worker.Worker;

/**
 * Core components of any worker writing input files for any software 
 * packages that does mainly read a chemical system as input. For the latter,
 * see {@link ChemSoftInputWriter}.
 *
 * @author Marco Foscato
 */

public abstract class InputWriter extends Worker implements ITextualInputWriter
{
    /**
     * Pathname root for output files (i.e., the input for the other software).
     */
    protected String outFileNameRoot;
    
    /**
     * Output name (input for the other software).
     */
    protected File outFile;
    
    /**
     * Flag deciding if we write the specific job-details file or not.
     */
    private boolean writeJobSpecificJDOutput = true;
    
    /**
     * The job we want to prepare the input for.
     */
    protected Job jobToInput;
    
    /**
     * Default extension of the software input file
     */
    protected String inpExtrension;

    /**
     * Default extension of the software output file
     */
    protected String outExtension;
    

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public InputWriter()
    {}
    
//------------------------------------------------------------------------------

    @Override
	public String getKnownInputDefinition() {
		return "inputdefinition/InputWriter.json";
	}
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    public void initialize()
    {
    	super.initialize();
       
    	if (params.contains(ChemSoftConstants.PARJOBDETAILSFILE))
        {
            File jdFile = new File(params.getParameter(
                    ChemSoftConstants.PARJOBDETAILSFILE).getValueAsString());
            logger.debug("Job details from JD file '" + jdFile + "'.");
            
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.jobToInput = JobFactory.buildFromFile(jdFile);
        } else if (params.contains(ChemSoftConstants.PARJOBDETAILSOBJ)) {
        	this.jobToInput = (Job) params.getParameter(
                    ChemSoftConstants.PARJOBDETAILSOBJ).getValue();
        } else {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                    + "Neither '" + ChemSoftConstants.PARJOBDETAILSFILE
                    + "' nor '" + ChemSoftConstants.PARJOBDETAILSOBJ 
                    + "'found in parameters.",-1);
        }

        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileNameRoot = params.getParameter(
                    ChemSoftConstants.PAROUTFILEROOT).getValueAsString();
            outFile = new File(outFileNameRoot + inpExtrension);
        } else if (params.contains(ChemSoftConstants.PAROUTFILE))
        {
        	outFile = new File(params.getParameter(
        			ChemSoftConstants.PAROUTFILE).getValueAsString());
            outFileNameRoot = FileUtils.getRootOfFileName(outFile);
        } else {
    		outFileNameRoot = "softiutput";
            logger.debug("Neither '" 
        				 + ChemSoftConstants.PAROUTFILE + "' nor '" 
        				 + ChemSoftConstants.PAROUTFILEROOT + "' found. " + NL
                         + "Root of any output file name set to '" 
                         + outFileNameRoot + "'.");
        	outFile = new File(outFileNameRoot + inpExtrension);
        }
        
        if (params.contains(ChemSoftConstants.PARNOJSONOUTPUT))
        {
        	writeJobSpecificJDOutput = false;
        }
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Produced the text that will be printed in the job's main input file, 
     * the one defining what kind of task and setting the software should use
     * to do its job.
     */
    public abstract StringBuilder getTextForInput(Job job);
    
//------------------------------------------------------------------------------

	@Override
	public void performTask() {
		IOtools.writeTXTAppend(outFile, getTextForInput(jobToInput).toString(),
				false);
		if (writeJobSpecificJDOutput)
		{
			IOtools.writeJobToJSON(jobToInput, new File(
					outFileNameRoot + ChemSoftConstants.JSONJDEXTENSION));
		}
	}
	
//------------------------------------------------------------------------------

}
