package autocompchem.wiro.acc;


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


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.run.Job;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for AutoCompChem (i.a., ACC).
 *
 * @author Marco Foscato
 */

public class ACCInputWriter extends InputWriter
{
    /**
     * String defining the task of preparing input files for AutoCompChem
     */
    public static final String PREPAREINPUTACCTASKNAME = "prepareInputACC";

    /**
     * Task about preparing input files for AutoCompChem
     */
    public static final Task PREPAREINPUTACCTASK;
    static {
    	PREPAREINPUTACCTASK = Task.make(PREPAREINPUTACCTASKNAME);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor
     */
    public ACCInputWriter() 
    {
		inpExtrension = ChemSoftConstants.JSONJDEXTENSION;
	}
  
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTACCTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ACCInputWriter();
    }
    
//------------------------------------------------------------------------------

	@Override
	public StringBuilder getTextForInput(Job job) 
	{
		StringBuilder sb = new StringBuilder();
		Gson writer = ACCJson.getWriter();
    	return sb.append(writer.toJson(job));
	}
	
//------------------------------------------------------------------------------
	
}
