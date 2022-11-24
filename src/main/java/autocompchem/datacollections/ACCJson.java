package autocompchem.datacollections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.Directive.DirectiveSerializer;
import autocompchem.run.ACCJob;
import autocompchem.run.Job;
import autocompchem.run.Job.JobSerializer;

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


/**
 * Reader/Writer for JSON files with AutoCompChem objects.
 */

public class ACCJson 
{
	
	private static ACCJson instance = null;
	
	Gson reader;

    Gson writer;
   
//------------------------------------------------------------------------------

    /**
     * Construct the singleton instance. This is run only once
     */
    private ACCJson()
    {
    	writer = new GsonBuilder()
    			.setPrettyPrinting()
    	        .registerTypeAdapter(Job.class, new JobSerializer())
    	        .registerTypeAdapter(ACCJob.class, new JobSerializer())
    	       //TODO-gg del .registerTypeAdapter(Directive.class, new DirectiveSerializer())
    			.create();
    	
    	reader = new GsonBuilder()
    			.setPrettyPrinting()
    			.create();
    }

//------------------------------------------------------------------------------

    /**
     * Gets the only implementation of this class.
     * @return the singleton instance
     */
    private static ACCJson getInstance()
    {
        if (instance == null)
            instance = new ACCJson();
        return instance;
    }

//------------------------------------------------------------------------------

    public static Gson getReader() 
    {
        return getInstance().reader;
    }

//------------------------------------------------------------------------------

    public static Gson getWriter() 
    {
        return getInstance().writer;
    }

//------------------------------------------------------------------------------

}
