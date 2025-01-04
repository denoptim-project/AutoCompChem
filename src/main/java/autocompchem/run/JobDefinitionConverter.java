package autocompchem.run;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;

import autocompchem.files.ACCFileType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


/**
 * A worker that converts among formats of job definition.
 * 
 * @author Marco Foscato
 */

public class JobDefinitionConverter extends Worker
{
    /**
     * String defining the task of converting job definition
     */
    public static final String CONVERTJOBDEFTASKNAME = "convertJobDefinition";

    /**
     * Task about evaluating any job output
     */
    public static final Task CONVERTJOBDEFTASK;
    static {
    	CONVERTJOBDEFTASK = Task.make(CONVERTJOBDEFTASKNAME);
    }

    /**
     * The input file in any job defining format.
     */
    protected File inFile;
    
    /**
     * The output file in any job defining format.
     */
    protected File outFile;
    
    /**
     * Format of the output file
     */
    protected ACCFileType outFormat = ACCFileType.JSON;
    
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobDefinitionConverter()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
		Set<Task> tmpSet = new HashSet<Task>();
		tmpSet.add(CONVERTJOBDEFTASK);
		return Collections.unmodifiableSet(tmpSet);
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/JobDefinitionConverter.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new JobDefinitionConverter();
    }
    
//------------------------------------------------------------------------------
	
	public void initialize() 
	{   	
    	super.initialize();
    	
    	if (params.contains(WorkerConstants.PARINFILE))
        {
         	String pathname = params.getParameter(WorkerConstants.PARINFILE)
             		.getValueAsString();
            FileUtils.foundAndPermissions(pathname,true,false,false);
            this.inFile = new File(pathname);
        } else {
			Terminator.withMsgAndStatus("ERROR! Missing '" 
					+ WorkerConstants.PARINFILE + "'.", -1);
        }
    	
    	if (params.contains(WorkerConstants.PAROUTFILE))
        {
	        this.outFile = new File(params.getParameter(
	        		WorkerConstants.PAROUTFILE).getValueAsString());
	        FileUtils.mustNotExist(this.outFile);
	        String ext = FileUtils.getFileExtension(this.outFile)
	        		.replaceFirst("\\.","");
	        if (ext != null && EnumUtils.isValidEnum(ACCFileType.class, 
	        		ext.toUpperCase()))
	        {
	        	this.outFormat = ACCFileType.valueOf(ext.toUpperCase());
	        }
        } else {
			Terminator.withMsgAndStatus("ERROR! Missing '" 
					+ WorkerConstants.PAROUTFILE + "'.", -1);
        }
        
        if (params.contains(WorkerConstants.PAROUTFORMAT))
        {
        	this.outFormat = ACCFileType.valueOf(params.getParameter(
        			WorkerConstants.PAROUTFORMAT).getValueAsString()
        			.toUpperCase());
        }
	}

//------------------------------------------------------------------------------
	
	/**
	 * Reads a job from a file and write that same job into another file using
	 * the give format.
	 * @param inFile input file
	 * @param outFile output file
	 * @param outFormat format for output file
	 */
	public static void convertJobDefinitionFile(File inFile, File outFile, 
			ACCFileType outFormat) 
	{	
		Job job = JobFactory.buildFromFile(inFile);
		
    	switch(outFormat)
    	{
	    	case JSON:
	    		IOtools.writeJobToJSON(job, outFile);
	    		break;
    		
    		default:
    			Terminator.withMsgAndStatus("ERROR! Format '" + outFormat 
    					+ "' cannot be used to write Jobs.", -1);
    			break;
    	}
	}

//------------------------------------------------------------------------------
	
	@Override
	public void performTask() 
	{
		convertJobDefinitionFile(inFile, outFile, outFormat);
	}
	
//------------------------------------------------------------------------------

}
