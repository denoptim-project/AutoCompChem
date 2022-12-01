package autocompchem.chemsoftware;

import java.lang.reflect.Type;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;

/**
 * Object representing an computational chemistry job to be run by an 
 * undefined software tool. The job may include one or more
 * sub jobs/tasks/steps, each of which is an instance of this very class.
 * A CompChemJob is meant to be independent from the comp.chem. tool that is
 * meant to do the actual comp.chem. tasks. 
 * A CompChemJob can be defined as to be independent from the specific 
 * definition of a chemical object (i.e., independent from a molecule). 
 * However, it can become dependent the chemical system upon processing
 * the definition of a chemical object, such as a molecule. For example,
 * the job can define how to generate the basis set from any molecule, and 
 * thus be independent from a specific molecular object. Then, upon processing
 * ACC tasks with the {@Link #processDirectives(IAtomContainer)} method, the
 * same CompChemJob will generate the basis set for the given atom container,
 * thus becoming molecule-specific.
 * 
 *
 * @author Marco Foscato
 */

public class CompChemJob extends Job implements Cloneable
{
	/**
	 * List of settings, data, and keywords for the comp.chem. tool
	 */
	private ArrayList<Directive> directives = new ArrayList<Directive>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty job
     */

    public CompChemJob()
    {
    	super();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a job from a formatted file (i.e., ACC's job details file)
     * containing instructions and parameters that define the calculation.
     * @param inFile formatted job details file to be read.
     */

    public CompChemJob(String inFile)
    {
        this(IOtools.readTXT(inFile));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a job from a formatted text divided in lines. 
     * The format of these lines is expected to adhere to that of
     * job details files.
     * @param lines array of lines to be read.
     */

    public CompChemJob(ArrayList<String> lines)
    {
    	super();

    	// WARNING: for now we are not considering the possibility of having
    	// both directives AND sub jobs.
    	
    	if (lines.toString().contains(ChemSoftConstants.JDLABSTEPSEPARATOR))
    	{
	    	ArrayList<String> newLines = 
	    			TextAnalyzer.readTextWithMultilineBlocks(lines, 
	    			ChemSoftConstants.JDCOMMENT, 
	    			ChemSoftConstants.JDOPENBLOCK, 
	    			ChemSoftConstants.JDCLOSEBLOCK);
	
	    	ArrayList<String> linesOfAStep = new ArrayList<String>();
	        for (int i=0; i<newLines.size(); i++)
	        {
	            String line = newLines.get(i).trim();
	
	            if (line.toUpperCase().equals(
	            		ChemSoftConstants.JDLABSTEPSEPARATOR))
	            {
	                CompChemJob step = new CompChemJob(linesOfAStep);
	                addStep(step);
	                linesOfAStep.clear();
	            } else {
	                linesOfAStep.add(line);
	            }
	        }
	        //Deal with the last step that doesn't have a separator at the end
	        CompChemJob step = new CompChemJob(linesOfAStep);
	        addStep(step);
    	} else {
    		directives = DirectiveFactory.buildAllFromJDText(lines);
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Looks into the existing directives for ACC tasks, and performs them.
     * Depending on the task, some directives may be changed as a result of the
     * ACC tasks. 
     * @param mol the molecular representation given to mol-dependent tasks.
     */
    
    //TODO rename so that it is clear that this makes the job mol-dependent
    
    public void processDirectives(IAtomContainer mol)
    {
    	for (Directive d : directives)
    	{
    		if (!d.hasACCTask())
    		{
    			continue;
    		}
    		d.performACCTasks(mol,this);
    	}
    	for (Job step : steps)
    	{
    		((CompChemJob) step).processDirectives(mol);
    	}
    }

//-----------------------------------------------------------------------------
    
    /**
     * Reorder the directives according to the given comparator
     * @param c the comparator defining the criteria for sorting directives.
     */
    
    public void sortDirectivesBy(Comparator<Directive> c)
    {
    	Collections.sort(directives, c);
    	for (Job step : steps)
    	{
    		((CompChemJob) step).sortDirectivesBy(c);
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Finds and return a specified directive.
     * @param name of the directive to return (case insensitive).
     * @return the directive or null if none is found with that name.
     */
    
    public Directive getDirective(String name)
    {
    	for (Directive d : directives)
    	{
    		if (d.getName().toUpperCase().equals(name.toUpperCase()))
    		{
    			return d;
    		}
    	}
    	return null;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Retrieves the wanted step.
     * @param i the index of the directive
     * @return the directive    
     */
    
    public Directive getDirective(int i)
    {
    	return directives.get(i);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Returns the iterator over directives
     * @return the iterator.   
     */
    
    public Iterator<Directive> directiveIterator()
    {
    	return directives.iterator();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Adds directive to this job or, if a directive with such name already
     * exists, replaces the existing one with the given one.
     * @param d the given directive
     */
    
    public void setDirective(Directive d)
    {
    	setDirective(d, false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds directive to this job or, if a directive with such name already
     * exists, replaces the existing one with the given one.
     * @param d the given directive
     * @param recursive use <code>true</code> to set the directive in all 
     * embedded jobs or steps.
     */
    
    public void setDirective(Directive d, boolean recursive)
    {
    	if (directives.contains(d))
    	{
    		int id = directives.indexOf(d);
    		directives.set(id, d);
    	} else {
    	    directives.add(d);
    	}
    	
    	if (getNumberOfSteps()>0 && recursive)
        {
	        for (int step = 0; step<steps.size(); step++)
	        {
	        	setDirective(d, recursive);
	        }
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Overwrites all directives of this job with the given ones.
     * @param directives the new directives
     */
    
    public void setDirectives(ArrayList<Directive> directives)
    {
    	this.directives = directives;
    }

//------------------------------------------------------------------------------

    /**
     * Clones all information as to produce an independent deep copy of this
     * object. This is achieved by exporting the object to string and 
     * constructing a brand new object from that string.
     * @return a deep copy.
     */
    
    public CompChemJob clone()
    {
    	CompChemJob clone = new CompChemJob(this.toLinesJobDetails());
    	return clone;
    }

//------------------------------------------------------------------------------
    
    /**
     * Produced a text representation of this object following the format of
     * autocompchem's job detail text file.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails()
    {
    	
    	// WARNING: for now we are not considering the possibility of having
    	// both directives AND sub jobs.
    	
        ArrayList<String> lines= new ArrayList<String>();
        if (getNumberOfSteps()>0 && directives.size()==0)
        {
	        for (int step = 0; step<steps.size(); step++)
	        {
	            if (step != 0)
	            {
	                lines.add(ChemSoftConstants.JDLABSTEPSEPARATOR);
	            }
	            lines.addAll(getStep(step).toLinesJobDetails());
	        }
        } else if (getNumberOfSteps()==0 && directives.size()>0) 
        {
        	for (Directive d : directives)
        	{
        		lines.addAll(d.toLinesJobDetails());
        	}
        } else {
        	Terminator.withMsgAndStatus("ERROR! Unable to convert CompChemJob "
        			+ "to JobDetails lines when it has " + directives.size() 
        			+ " directives and " + getNumberOfSteps() + " sub-jobs. "
        			+ "This functionality is not implemented yet. Please, "
        			+ "contact the authors.", -1);
        }
        return lines;
    }
    
//------------------------------------------------------------------------------

    public static class CompChemJobSerializer 
    implements JsonSerializer<CompChemJob>
    {
        @Override
        public JsonElement serialize(CompChemJob job, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONJOVTYPE, job.getClass().getSimpleName());
            
            if (!job.params.isEmpty())
            	jsonObject.add(JSONPARAMS, context.serialize(job.params));
            if (!job.steps.isEmpty())
            	jsonObject.add(JSONSUBJOBS, context.serialize(job.steps));
            if (!job.directives.isEmpty())
            	jsonObject.add("directives", context.serialize(job.directives));
            
            return jsonObject;
        }
    }
//------------------------------------------------------------------------------

}
