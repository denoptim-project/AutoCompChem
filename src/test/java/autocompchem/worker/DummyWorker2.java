package autocompchem.worker;

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

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.run.Job;


/**
 * A dummy worker that is meant only for testing purposes. this is meant to be 
 * a different class than {@link DummyWorker}.
 * 
 * @author Marco Foscato
 */

public class DummyWorker2 extends Worker
{
	public static final String DATAREF = "duDataB";
	public static final String DATAVALUE = "Output data B";
	
	protected String infile = "empty";
	
	
//-----------------------------------------------------------------------------

	public DummyWorker2()
	{}
	
//------------------------------------------------------------------------------

    @Override
    public Set<TaskID> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<TaskID>(
             Arrays.asList(TaskID.DUMMYTASK2)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/DummyWorker.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new DummyWorker2();
    }
    
//-----------------------------------------------------------------------------
	
	@Override
	public void initialize() 
	{
		inputCollector = new NamedDataCollector();
		
		// These dummy data are used only for testing
		// Do not modify them unless you change also the 
		// WorkerFactoryTest class
		inputCollector.putNamedData(new NamedData(DATAREF, 
				NamedDataType.STRING, DATAVALUE));
	}
	
//-----------------------------------------------------------------------------

	@Override
	public void performTask() 
	{
		// The only task here is reporting something in the output collector
		exposeOutputData(inputCollector.getNamedData(DATAREF));
		
		if (params.contains("VERBOSITY")) 
		{
			if (Integer.parseInt(params.getParameter("VERBOSITY").getValue()
					.toString()) > 2) {
				// In some tests we might want to check that the input has been 
				// read
				// properly, so we echo the content of the param. storage
				System.out.println(params.toLinesJobDetails());
			} 
		}
	}
	
//-----------------------------------------------------------------------------	

}
