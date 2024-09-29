package autocompchem.wiro.chem.gaussian.legacy;

import java.io.File;

/*
 *   Copyright (C) 2023  Marco Foscato
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/** 
 * Tool converting the old job details format used for Gaussian jobs into
 * the general JSON-compatible job.
 */


public class GaussianJobDetailsConverter extends Worker
{
	
    //Files we work with
    private File inFile;
    private File outFile;
    
    /**
     * String defining the task of converting job details
     */
    public static final String CONVERTJOBDETAILSTASKNAME = "convertJobDetails";

    /**
     * Task about converting job details
     */
    public static final Task CONVERTJOBDETAILSTASK;
    static {
    	CONVERTJOBDETAILSTASK = Task.make(CONVERTJOBDETAILSTASKNAME);
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public GaussianJobDetailsConverter()
    {
    }
  
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(CONVERTJOBDETAILSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/GaussianJobDetailsConverter.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new GaussianJobDetailsConverter();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

    public void initialize()
    {
    	super.initialize();

        //Get and check the input file (which has to be an SDF file)
        String inFilePathname = params.getParameter("INFILE").getValueAsString();
        this.inFile = new File(inFilePathname);
        FileUtils.foundAndPermissions(inFilePathname,true,false,false);

        String outFilePathname = null;
        if (params.contains("OUTFILE"))
        {
        	outFilePathname = params.getParameter("OUTFILE").getValueAsString();
        } else {
        	outFilePathname = inFilePathname + ".json";
        }
        this.outFile = new File(outFilePathname);
        FileUtils.mustNotExist(this.outFile);
    }
    
//-----------------------------------------------------------------------------

      /**
       * Performs any of the registered tasks according to how this worker
       * has been initialized.
       */

      @Override
      public void performTask()
      {
    	  if (!task.equals(CONVERTJOBDETAILSTASK))
    		  dealWithTaskMismatch();
      	
    	  GaussianJob gJob = new GaussianJob(inFile.getAbsolutePath());
    	  Job jJob = gJob.convertToCompChemJob();
    	  Gson writer = ACCJson.getWriter();
    	  logger.info("Writing job with " + jJob.getNumberOfSteps() 
    		  + " steps to '" + outFile + "'.");
    	  IOtools.writeTXTAppend(outFile, writer.toJson(jJob).toString(), false);
      }

//------------------------------------------------------------------------------

}
