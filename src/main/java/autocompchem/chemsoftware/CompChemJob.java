package autocompchem.chemsoftware;

import java.io.IOException;
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
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.io.ACCJson;
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
 * ACC tasks with the {@link processDirectives} method, the
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
	private List<Directive> directives = new ArrayList<Directive>();

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

    public CompChemJob(List<String> lines)
    {
    	super();

    	// WARNING: for now we are not considering the possibility of having
    	// both directives AND sub jobs.
    	
    	if (lines.toString().contains(ChemSoftConstants.JDLABSTEPSEPARATOR))
    	{
	    	List<String> newLines = 
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
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty job
     * @throws IOException 
     */

    public static CompChemJob fromJSONFile(String pathname) throws IOException
    {
    	CompChemJob ccj = (CompChemJob) IOtools.readJsonFile(pathname, 
    			Job.class);
    	return ccj;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Looks into the existing directives for ACC tasks, and performs them.
     * Depending on the task, some directives may be changed as a result of the
     * ACC tasks. 
     * @param mols the molecular representation given to mol-dependent tasks.
     * @param masterJob the job that requires to process directives of this job.
     */
    
    //TODO rename so that it is clear that this makes the job mol-dependent
    
    public void processDirectives(List<IAtomContainer> mols, Job masterJob)
    {
    	for (Directive d : directives)
    	{
    		if (!d.hasACCTask())
    		{
    			continue;
    		}
    		d.performACCTasks(mols, this, masterJob);
    	}
    	for (Job step : steps)
    	{
    		((CompChemJob) step).processDirectives(mols, masterJob);
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
     * Finds and return directive component that are found in a given location
     * of the directive component's structure.
     * @param address the location of the components to fetch, starting with the
     * outermost directive and ending with the component to fetch.
     * @return the directive components that match the path. 
     * The result can be an empty list.
     */
    
    public List<IDirectiveComponent> getDirectiveComponents(
    		DirComponentAddress address)
    {
    	List<IDirectiveComponent> matches = new ArrayList<IDirectiveComponent>();
    	if (address.size()==0)
    	{
    		return matches;
    	} 
    	if (address.size()==1 &&
    			!(address.get(0).type.equals(DirectiveComponentType.DIRECTIVE)
    				|| address.get(0).type.equals(DirectiveComponentType.ANY)))
    	{
    		//No keyword of directiveData can be found at level 0
			return matches;
    	}

    	List<Directive> candDirectives = new ArrayList<Directive>();
		for (Directive d : directives)
		{
			if (address.get(0).name.equals("*") || d.getName().toUpperCase().equals(
					address.get(0).name.toUpperCase()))
			{
				candDirectives.add(d);
			}
		}
		if (address.size()==1)
		{
			matches.addAll(candDirectives);
			return matches;
		}
    	
    	for (int iLevel=1; iLevel<address.size(); iLevel++)
    	{
    		DirectiveComponentType levType = address.get(iLevel).type;
    		String name = address.get(iLevel).name;
    		List<IDirectiveComponent> matchingComponents = 
					new ArrayList<IDirectiveComponent>();
			for (Directive parentDir : candDirectives)
			{
				List<IDirectiveComponent> candidateComponents = 
						new ArrayList<IDirectiveComponent>();
				switch (levType) 
				{
				case KEYWORD:
					candidateComponents.addAll(parentDir.getAllKeywords());
					break;
				case DIRECTIVEDATA:
					candidateComponents.addAll(
    						parentDir.getAllDirectiveDataBlocks());
					break;
				case DIRECTIVE:
    				candidateComponents.addAll(
    						parentDir.getAllSubDirectives());
    				break;
				case ANY:
					candidateComponents.addAll(parentDir.getAllKeywords());
					candidateComponents.addAll(
    						parentDir.getAllDirectiveDataBlocks());
    				candidateComponents.addAll(
    						parentDir.getAllSubDirectives());
    				break;
				}
    			for (IDirectiveComponent candComp : candidateComponents)
    			{
    				if (address.get(iLevel).name.equals("*") || candComp.getName()
    						.toUpperCase().equals(name.toUpperCase()))
    				{
    					matchingComponents.add(candComp);
    				}
    			}
			}
			candDirectives.clear();
			if (levType==DirectiveComponentType.DIRECTIVE
					|| levType==DirectiveComponentType.ANY)
			{
				for (IDirectiveComponent c : matchingComponents)
				{
					if (c instanceof Directive)
						candDirectives.add((Directive) c);
				}
			}
			
			if (iLevel==(address.size()-1))
			{
				matches.addAll(matchingComponents);
			}
    	}
    	return matches;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Ensures the structure of directives contains the directives involved in
     * the given address. Ignores any directive components that is not a
     * {@link Directive}.
     * @param address the address to create if not found already.
     * @return <code>true</code> if any directive had to be created.
     */
    public boolean ensureDirectiveStructure(DirComponentAddress address)
	{
    	boolean result = false;
    	if (address.size()==0)
		{
			return result;
		} 
		if (address.size()==1 &&
				!(address.get(0).type.equals(DirectiveComponentType.DIRECTIVE)
					|| address.get(0).type.equals(DirectiveComponentType.ANY)))
		{
			return result;
		}
		
		List<Directive> candDirectives = new ArrayList<Directive>();
		String wantedDirName = address.get(0).name;
		if (wantedDirName.equals("*"))
		{
			candDirectives.addAll(directives);
		} else {
			for (Directive d : directives)
			{
				if (d.getName().toUpperCase().equals(
						wantedDirName.toUpperCase()))
				{
					candDirectives.add(d);
				}
			}
		}
		if (candDirectives.size()==0 && !wantedDirName.equals("*"))
		{
			Directive wantedDir = new Directive(wantedDirName);
			addDirective(wantedDir);
			candDirectives.add(wantedDir);
			result = true;
		}
		
		for (int iLevel=1; iLevel<address.size(); iLevel++)
		{
			DirectiveComponentType levType = address.get(iLevel).type;
			if (!(levType==DirectiveComponentType.DIRECTIVE
					|| levType==DirectiveComponentType.ANY))
			{
				break;
			}
			wantedDirName = address.get(iLevel).name;

			List<Directive> nextParDirs = new ArrayList<Directive>();
			for (Directive parentDir : candDirectives)
			{
				if (wantedDirName.equals("*"))
				{
					nextParDirs.addAll(parentDir.getAllSubDirectives());
				} else {
					for (Directive d : parentDir.getAllSubDirectives())
					{
						if (d.getName().toUpperCase().equals(
								wantedDirName.toUpperCase()))
						{
							nextParDirs.add(d);
						}
					}
				}
			}
			
			if (nextParDirs.size()==0 && !wantedDirName.equals("*"))
			{
				if (candDirectives.size()==1)
				{
					Directive wantedDir = new Directive(wantedDirName);
					candDirectives.get(0).addSubDirective(wantedDir);
					nextParDirs.add(wantedDir);
					result = true;
				} else if (candDirectives.size()>1) {
					throw new IllegalArgumentException("Found multiple parent "
							+ "directives at level " + (iLevel-1) 
							+ " of address '" + address + "'. "
							+ "Directive '" + wantedDirName + "' "
							+ "will not be created under more than one parent.");
				}
			}

			candDirectives.clear();
			candDirectives.addAll(nextParDirs);
		}
		return result;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Finds and return a specified directive. This method looks only at the 
     * directives of this very job, not at the content of any embedded job.
     * @param name of the directive to return (case insensitive).
     * @return the directive or null if none is found with that name.
     */
    
    public Directive getDirective(String name)
    {
    	return getDirective(name, false);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Finds and return a specified directive. This method looks also at the 
     * content of any embedded job, if recursion is required. Note that we 
     * return the first-encountered directive with the given name.
     * @param name of the directive to return (case insensitive).
     * @param recursive use <code>true</code> to allow recursion into embedded
     * jobs.
     * @return the directive or null if none is found with that name.
     */
    
    public Directive getDirective(String name, boolean recursive)
    {
    	for (Directive d : directives)
    	{
    		if (d.getName().toUpperCase().equals(name.toUpperCase()))
    		{
    			return d;
    		}
    	}
    	if (recursive)
    	{
    		Directive d = null;
	    	for (Job step : steps)
	    	{
	    		d= ((CompChemJob)step).getDirective(name, true);
	    		if (d!=null)
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
    	if (d==null)
    		throw new IllegalArgumentException("Null directives not allowed!");
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
	        for (Job  step : steps)
	        {
	        	if (step instanceof CompChemJob)
	        		((CompChemJob) step).setDirective(d, recursive);
	        }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds directive to this job regardless of whether another directive with 
     * the same name exists.
     * @param d the given directive
     */
    public void addDirective(Directive d)
    {
    	directives.add(d);
    }
        
//------------------------------------------------------------------------------

    /**
     * Removes the first occurrence of the specified Directive from this list,
     * if it is present. If the list does not contain the Directive, 
     * it is unchanged.
     * @param origiGeomDir
     * @return <code>true</code> if the list of directives changes upon calling
     * this method, i.e., the object has been removed.
     */
  	public boolean removeDirective(Directive d) 
  	{
  		return directives.remove(d);
  	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Sets a {@link Keyword} in a {@link Directive} with the given name
     * if it is not already present. 
     * @param dirName the name of the directive.
     * @param keyName the name of the {@link Keyword}.
     * @param isLoud use <code>true</code> if the keyword should be set to be
     * a loud keyword, meaning that conversion to text used the syntax 
     * <code>key|separator|value</code> (for loud keywords) instead of just
     * <code>value</code> (for non-loud, or silent keywords).
     * @param value the value of the keyword to specify.
     */
    public void setKeywordIfUnset(String dirName, String keyName, 
    		boolean isLoud, String value)
    {
		Directive dir = getDirective(dirName);
		if (dir==null)
		{
			dir = new Directive(dirName);
    		dir.addKeyword(new Keyword(keyName, isLoud, value));
			setDirective(dir);
		} else {
			if (dir.getKeyword(keyName)==null)
				dir.addKeyword(new Keyword(keyName, isLoud, value));
		}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Sets a {@link DirectiveData} in a {@link Directive} with the given name
     * if it is not already present. 
     * @param dirName the name of the directive.
     * @param dirDataName the name of the {@link DirectiveData}.
     * @param dd a source of data. We'll take the value from this instance to
     * make a new {@link directiveData}.
     */
    public void setDirectiveDataIfUnset(String dirName, String dirDataName, 
    		DirectiveData dd)
    {
    	Directive dir = getDirective(dirName);
		if (dir==null)
		{
			dir = new Directive(dirName);
    		dir.addDirectiveData(dd);
			setDirective(dir);
		} else {
			DirectiveData oldDd = dir.getDirectiveData(dirDataName);
			if (oldDd==null)
			{
        		dir.addDirectiveData(dd);
        	} else {
        		oldDd.setValue(dd.getValue());
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
    	for (Directive d : directives)
    		if (d==null)
    			throw new IllegalArgumentException(
    					"Null directives not allowed!");
    	this.directives = directives;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Removes the ACC tasks from any directive's component in this job and in
     * any embedded job.
     */
    public void removeACCTasks()
    {
    	for (Directive d : directives)
    	{
    		d.removeACCTasks();
    	}
    	
    	for (Job j : steps)
    	{
    		((CompChemJob) j).removeACCTasks();
    	}
    }
   
//------------------------------------------------------------------------------

    /**
     * Copies most information from this jobs and its steps and builds a new job
     * out of it. This is achieved by exporting the object to string and 
     * constructing a brand new object from that string.
     * @return a new job.
     */
    
    public CompChemJob clone()
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	CompChemJob clone = (CompChemJob) reader.fromJson(writer.toJson(this), 
    			Job.class);
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
