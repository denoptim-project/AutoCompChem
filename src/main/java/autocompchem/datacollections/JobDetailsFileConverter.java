package autocompchem.datacollections;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveFactory;
import autocompchem.chemsoftware.gaussian.GaussianJob;
import autocompchem.chemsoftware.nwchem.NWChemJob;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.run.JobFactory;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * MolecularSorter is a tool to sort molecules.
 * 
 * @author Marco Foscato
 */


public class JobDetailsFileConverter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.CONVERTJOBDETAILS)));
    
    //Filenames
    private String inFile;
    private String inFormat = "JOB";
    private String outFile;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobDetailsFileConverter()
    {
        super("inputdefinition/JobDetailsFileConverter.json");
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Get and check the input file (which has to be an SDF file)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);
        
        if (params.contains("INPUTFORMAT"))
        {
	        this.inFormat = params.getParameter("INPUTFORMAT").getValue()
	        		.toString().toUpperCase();
	        FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }
        
        //Get and check the output file name 
        this.outFile = params.getParameter("OUTFILE").getValue().toString();
        FileUtils.mustNotExist(this.outFile);
    }

//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @SuppressWarnings("unchecked")
	@Override
    public void performTask()
    {
    	Object obj = null;
    	switch (inFormat)
    	{
    	case "CCJ":
    	{
    		obj = new CompChemJob(inFile);
    		break;
    	}
    	
    	case "DIRECTIVES":
    	{
    		ArrayList<Directive> dirs = DirectiveFactory.buildAllFromJDText(
    				IOtools.readTXT(inFile));
    		obj = new CompChemJob();
    		((CompChemJob) obj).setDirectives(dirs);
    		break;
    	}
    	
    	case "GAUSSIANJD":
    	{
    		GaussianJob gJob = new GaussianJob(inFile);
    		obj = gJob.convertToCompChemJob();
    		break;
    	}
    	
    	case "NWCHEMJD":
    	{
    		NWChemJob nwcJob = new NWChemJob(inFile);
    		obj = nwcJob.convertToCompChemJob();
    		break;
    	}
    	
    	default:
    		obj = JobFactory.buildFromFile(inFile);
    	}
    	
    	Gson writer = ACCJson.getWriter();
    	
    	IOtools.writeTXTAppend(outFile, writer.toJson(obj), false);
    	
    	//TODO-log
    	System.out.println("Writing file '" + outFile + "'");
    }

//-----------------------------------------------------------------------------

}
