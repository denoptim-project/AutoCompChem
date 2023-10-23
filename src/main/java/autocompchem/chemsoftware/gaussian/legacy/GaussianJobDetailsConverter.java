package autocompchem.chemsoftware.gaussian.legacy;

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
import java.io.File;

import com.google.gson.Gson;

import autocompchem.chemsoftware.gaussian.GaussianJob;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/** 
 * Tool converting the old job details format used for Gaussian jobs into
 * the general JSON-compatible job.
 */


public class GaussianJobDetailsConverter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.CONVERTJOBDETAILS)));
	
    //Files we work with
    private File inFile;
    private File outFile;

    //Reporting flag
    private int verbosity = 0;

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public GaussianJobDetailsConverter()
    {
        super("inputdefinition/GaussianJobDetailsConverter.json");
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {

        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(
            		" Adding parameters to GaussianJobDetailsConverter");

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
       * has been initialised.
       */

      @Override
      public void performTask()
      {
    	  GaussianJob gJob = new GaussianJob(inFile.getAbsolutePath());
    	  Job jJob = gJob.convertToCompChemJob();
    	  Gson writer = ACCJson.getWriter();
    	  if (verbosity > -1)
    	  {    
    		  System.out.println("Writing job with " + jJob.getNumberOfSteps() 
    		  + " steps to '" + outFile + "'.");
    	  }
    	  
    	  IOtools.writeTXTAppend(outFile, writer.toJson(jJob).toString(), false);
      }

//------------------------------------------------------------------------------

}
