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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.chemsoftware.gaussian.GaussianConstants;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.AtomLabelsGenerator;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.modeling.constraints.ConstraintsGenerator;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.conformation.ConformationalSpace;
import autocompchem.molecule.conformation.ConformationalSpaceGenerator;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixConstants;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlock;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * This object represents a single directive that consists of
 * a name, and may include a list of {@link Keyword}s, 
 * a list of subordinate {@link Directive}s, 
 * and some tabled {@link DirectiveData}. 
 * All fields are optional and may be empty.
 *
 * @author Marco Foscato
 */

public class Directive implements IDirectiveComponent, Cloneable
{
    /**
     * Directive name.
     */
    private String name = "#noname";

    /**
     * List of keywords.
     */
    private List<Keyword> keywords;

    /**
     * List of subordinate directives.
     */
    private List<Directive> subDirectives;

    /**
     * Data attached directly to this directive.
     */
    private List<DirectiveData> dirData;
    
    /**
     * Parameters defining task embedded in this directive.
     */
    private ParameterStorage accTaskParams;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive.
     */

    public Directive()
    {
        this("#noname", new ArrayList<Keyword>(), new ArrayList<Directive>(), 
        		new ArrayList<DirectiveData>());
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive with a name.
     * @param name the name of the directive.
     */

    public Directive(String name)
    {
        this(name, new ArrayList<Keyword>(), new ArrayList<Directive>(), 
        		new ArrayList<DirectiveData>());
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for directive with specific content.
     * @param name the name of the directive.
     * @param keywords the list of keywords.
     * @param subDirectives the list of sub directives.
     * @param dirData the list of data blocks.
     */

    public Directive(String name, ArrayList<Keyword> keywords, 
                                       ArrayList<Directive> subDirectives,
                                         ArrayList<DirectiveData> dirData)
    {
        this.name = name;
        this.keywords = keywords;
        this.subDirectives = subDirectives;
        this.dirData = dirData;

        // We define also the tasks that may have to be performed inside a 
        // directive
        // TODO-gg consider registering workers for each of them
        Task.make("generateBasisSet");
        Task.make("generateConstraints");
        Task.make("generateAtomLabels");
        Task.make("generateAtomTuples");
        Task.make("addFileName");
        Task.make("addGeometry");
        Task.make("addAtomSpecificKeywords");
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name of this directive.
     * @return the name of the directive.
     */

    public String getName()
    {
        return name;
    }
 
//-----------------------------------------------------------------------------

    /**
     * @return the kind of directive component this is.
     */
    
	public DirectiveComponentType getComponentType() 
	{
		return DirectiveComponentType.DIRECTIVE;
	}

//-----------------------------------------------------------------------------

    /**
     * Returns all sub directive.
     * @return the list of sub directives.
     */

    public List<Directive> getAllSubDirectives()
    {
        return subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the first sub {@link Directive} with the given name. 
     * Use this method when you can assume there is only
     * only one {@link DirectiveData} with the given name.
     * @param name the name of the sub directive to get (case-insensitive).
     * @return the sub directive with the given name or null if it doesn't 
     * exist.
     */

    public Directive getFirstDirective(String name)
    {
        for (Directive subDir : subDirectives)
        {
            if (subDir.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return subDir;
            }
        }
        return null;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns all {@link Directive}s with the given name. 
     * Only sub-directives belonging to
     * this directive can be returned. 
     * {@link Directive}s of sub directives are ignored. 
     * @param name the name of the directive to get (case-insensitive).
     * @return the list of directives. Can be empty list.
     */

    public List<Directive> getDirectives(String name)
    {
    	List<Directive> matches = new ArrayList<Directive>();
        for (Directive d : subDirectives)
        {
            if (d.getName().toUpperCase().equals(name.toUpperCase()))
            {
            	matches.add(d);
            }
        }
        return matches;
    }

//-----------------------------------------------------------------------------

    /**
     * Remove a sub directive of this directive.
     * @param dir the directive that has to be removed.
     */

    public void deleteSubDirective(Directive dir)
    {
        subDirectives.remove(dir);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all keywords.
     * @return the list of keywords.
     */

    public List<Keyword> getAllKeywords()
    {
        return keywords;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Reorder the keywords according to the given comparator.
     * @param c the comparator defining the criteria for sorting keywords.
     */
    
    public void sortKeywordsBy(Comparator<Keyword> c)
    {
    	Collections.sort(keywords, c);
    	for (Directive d : getAllSubDirectives())
    	{
    		d.sortKeywordsBy(c);
    	}
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the first {@link Keyword} with the given name. Ignores any other 
     * {@link Keyword}  with this name.
     * Only keyword belonging to
     * this directive can be returned. 
     * {@link Keyword}s of sub directives are ignored. 
     * Use this method when you can assume there is only
     * only one {@link DirectiveData} with the given name.
     * @param name the name of the keyword to get (case-insensitive).
     * @return the keyword with the given name or null if such keyword
     * does not exist.
     */

    public Keyword getFirstKeyword(String name)
    {
        for (Keyword kw : keywords)
        {
            if (kw.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return kw;
            }
        }
        return null;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns all {@link Keyword}s with the given name. 
     * Only keyword belonging to
     * this directive can be returned. 
     * {@link Keyword}s of sub directives are ignored. 
     * @param name the name of the keyword to get (case-insensitive).
     * @return the list of keywords. Can be empty list.
     */

    public List<Keyword> getKeywords(String name)
    {
    	List<Keyword> matches = new ArrayList<Keyword>();
        for (Keyword kw : keywords)
        {
            if (kw.getName().toUpperCase().equals(name.toUpperCase()))
            {
            	matches.add(kw);
            }
        }
        return matches;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all data blocks contained in this directive.
     * @return the list of data blocks.
     */

    public List<DirectiveData> getAllDirectiveDataBlocks()
    {
        return dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the first {@link DirectiveData} having the specified name. 
     * Data of sub
     * directives are ignored. 
     * Use this method when you can assume there is only
     * only one {@link DirectiveData} with the given name.
     * @param name the name of the data block to get (case-insensitive).
     * @return the data blocks having the specified name or null if such data
     * block does not exist.
     */

    public DirectiveData getFirstDirectiveData(String name)
    {
        for (DirectiveData data : dirData)
        {
            if (data.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return data;
            }
        }
        return null;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns all {@link DirectiveData}s with the given name. 
     * Only data belonging to
     * this directive can be returned. 
     * {@link DirectiveData}s of sub directives are ignored. 
     * @param name the name of the data to get (case-insensitive).
     * @return the list of data. Can be empty list.
     */

    public List<DirectiveData> getDirectiveData(String name)
    {
    	List<DirectiveData> matches = new ArrayList<DirectiveData>();
        for (DirectiveData dd : dirData)
        {
            if (dd.getName().toUpperCase().equals(name.toUpperCase()))
            {
            	matches.add(dd);
            }
        }
        return matches;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Searched for a component on the given name and type.
     * @param name the name to search for  (case-insensitive).
     * @param type the type to search for.
     * @return <code>true</code> if such component exists.
     */
    public boolean hasComponent(String name, DirectiveComponentType type)
    {
    	return getComponent(name, type).size()>0;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Deletes the component with the given reference name and type. Also all
     * sub components of that component will be removed.
     * @param comp the component to remove
     */
    
    public void deleteComponent(IDirectiveComponent comp)
    {
    	switch (comp.getComponentType())
    	{
	    	case KEYWORD:
	    	{
	    		keywords.remove(comp);
	    		break;
	    	}
	    	
	    	case DIRECTIVEDATA:
	    	{
	    		dirData.remove(comp);
	    		break;
	    	}
	    	
	    	case DIRECTIVE:
	    	{
	    		subDirectives.remove(comp);
	    		break;
	    	}
	    	
	    	case ANY:
	    	{
	    		keywords.remove(comp);
	    		dirData.remove(comp);
	    		subDirectives.remove(comp);
	    		break;
	    	}
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Deletes the component with the given reference name and type. Also all
     * sub components of that component will be removed.
     * @param typeAndName the component type and name to remove
     */
    
    public void deleteComponent(DirComponentTypeAndName typeAndName)
    {
    	List<IDirectiveComponent> matches = getComponent(typeAndName.name, 
    			typeAndName.type);
    	for (IDirectiveComponent component : matches)
    		deleteComponent(component);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Looks for the components with the given reference name and type.
     * @param name the reference name of the components to find 
     * (case-insensitive).
     * @param type the type of components to find.
     * @return the list of desired components.
     */
    
    public List<IDirectiveComponent> getComponent(String name, 
    		DirectiveComponentType type)
    {
    	List<IDirectiveComponent> list = new ArrayList<IDirectiveComponent>();
    	switch (type)
    	{
	    	case KEYWORD:
	    	{
	    		list.addAll(getKeywords(name));
	    		break;
	    	}
	    	
	    	case DIRECTIVEDATA:
	    	{
	    		list.addAll(getDirectiveData(name));
	    		break;
	    	}
	    	
	    	case DIRECTIVE:
	    	{
	    		list.addAll(getDirectives(name));
	    		break;
	    	}
	    	
	    	case ANY:
	    	{
	    		list.addAll(getKeywords(name));
	    		list.addAll(getDirectiveData(name));
	    		list.addAll(getDirectives(name));
	    	}
    	}
    	return list;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Sets a component in this directive. If a component with the
     * same name already exists the behavior is this:
     * <ul>
     * <li>for {@link Keyword} and {@link DirectiveData}, we set the value to 
     * that of the argument,</li>
     * <li>for {@link Directive}s, we remove any of the components of the 
     * existing {@link Directive} and replace them with with those of the 
     * argument.</li>
     * </ul>
     * 
     * @param component
     */
    
    @SuppressWarnings("incomplete-switch")
	public void setComponent(IDirectiveComponent component)
    {
    	switch (component.getComponentType())
    	{
	    	case KEYWORD:
	    	{
	    		Keyword key = (Keyword) component;
	    		setKeyword(key);
	    		break;
	    	}
	    	
	    	case DIRECTIVEDATA:
	    	{
	    		DirectiveData dd = (DirectiveData) component;
	    		setDirectiveData(dd);
	    		break;
	    	}
	    	
	    	case DIRECTIVE:
	    	{
	    		Directive dir = (Directive) component;
	    		setSubDirective(dir);
	    		break;
	    	}
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Add a component in this directive. The existence of a component with the
     * same type and name has no influence on the result: this
     * method just adds new components without affecting existing components.
     * 
     * @param component
     */
    
    @SuppressWarnings("incomplete-switch")
	public void addComponent(IDirectiveComponent component)
    {
    	switch (component.getComponentType())
    	{
	    	case KEYWORD:
	    	{
	    		Keyword key = (Keyword) component;
	    		addKeyword(key);
	    		break;
	    	}
	    	
	    	case DIRECTIVEDATA:
	    	{
	    		DirectiveData dd = (DirectiveData) component;
	    		addDirectiveData(dd);
	    		break;
	    	}
	    	
	    	case DIRECTIVE:
	    	{
	    		Directive dir = (Directive) component;
	    		addSubDirective(dir);
	    		break;
	    	}
    	}
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add a subordinate directive.
     * @param subDirective the subordinate directive to add.
     */

    public void addSubDirective(Directive subDirective)
    {
        subDirectives.add(subDirective);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a keyword to the list of keywords.
     * @param kw the keyword to add.
     */

    public void addKeyword(Keyword kw)
    {
        keywords.add(kw);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a new block of data to this directive.
     * @param data the block to be added.
     */

    public void addDirectiveData(DirectiveData data)
    {
        dirData.add(data);
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of subordinate directives.
     * @param subDirectives the new list of subordinate directives .
     */

    public void setAllSubDirectives(List<Directive> subDirectives)
    {
        this.subDirectives = subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Adds a single sub directive of this directive. If any sub directive
     * with the given name already exists, then we overwrite its components
     * 
     * This method makes sure the given directive is not a
     * parent directive of <code>this</code> or vice versa. This to prevent 
     * loop-like directive structures.
     * @param dir the new directive.
     */

    public void setSubDirective(Directive dir)
    {
    	if (dir.embeds(this) || this.embeds(dir))
		{
			throw new IllegalArgumentException("Attempt to create loop "
					+ "in directives structure.");
		}
        List<Directive> oldDirs = getDirectives(dir.getName());
        if (oldDirs.size()==0)
        {
            this.addSubDirective(dir);
        } else {
        	for (Directive old : oldDirs)
        	{
            	old.setAllKeywords(dir.getAllKeywords());
                old.setAllSubDirectives(dir.getAllSubDirectives());
                old.setAllDirectiveDataBlocks(dir.getAllDirectiveDataBlocks());
        	}
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Checks if another directive is anywhere embedded in the directives 
     * structure embedded in this directive.
     * @param other
     * @return <code>true</code> if the other directive is either a 
     * sub-directive of this directive, or is further embedded at any level
     * in the directives structure.
     */
    public boolean embeds(Directive other)
    {
    	if (subDirectives.contains(other))
    	{
    		return true;
    	}
    	for (Directive subDir : subDirectives)
    	{
    		if (subDir.embeds(other))
    			return true;
    	}
    	return false;
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of keywords.
     * @param keywords the new list of keywords.
     */

    public void setAllKeywords(List<Keyword> keywords)
    {
        this.keywords = keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single keyword of this directive.
     * @param kw the new keywords.
     */

    public void setKeyword(Keyword kw)
    {
        Keyword oldKw = getFirstKeyword(kw.getName());
        if (oldKw == null)
        {
             addKeyword(kw);
        }
        else
        {
             oldKw.setValue(kw.getValue());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of tabled data.
     * @param dirData the new list of data.
     */

    public void setAllDirectiveDataBlocks(List<DirectiveData> dirData)
    {
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single block of data of this directive.
     * @param dd the new data block.
     */

    public void setDirectiveData(DirectiveData dd)
    {
        DirectiveData oldDd = getFirstDirectiveData(dd.getName());
        if (oldDd == null)
        {
             addDirectiveData(dd);
        }
        else
        {
             oldDd.setValue(dd.getValue());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * @return the parameters defining the ACC task embedded in this directive.
     */
   	public ParameterStorage getTaskParams() 
   	{
   		return accTaskParams;
   	}
   	
//-----------------------------------------------------------------------------

    /**
     * Sets the parameters defining the ACC task embedded in this directive.
     * @param params
     */
   	public void setTaskParams(ParameterStorage params) 
   	{
   		accTaskParams=params;
   	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Checks if there is any ACC task definition within this directive. Does
     * not distinguished whether the task is in this directive or in any of its 
     * components (i.e., {@link keyword}s, {@link DirectiveData} or embedded 
     * {@link Directive}s).
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
    public boolean hasACCTask()
    {
    	if (accTaskParams!=null)
    		return true;
    	
    	for (Keyword k : keywords)
    	{
    		if (k.hasACCTask())
    		{
    			return true;
    		}
    	}
    	for (DirectiveData dd : dirData)
    	{
    		if (dd.hasACCTask())
    		{
    			return true;
    		}
    	}
    	for (Directive d : subDirectives)
    	{
    		if (d.hasACCTask())
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes the ACC tasks from any directive's component.
     */
    public void removeACCTasks()
    {
    	accTaskParams = null;
    	for (Keyword k : keywords)
    	{
    		k.removeACCTasks();
    	}
    	for (DirectiveData dd : dirData)
    	{
    		dd.removeACCTasks();
    	}
    	for (Directive d : subDirectives)
    	{
    		d.removeACCTasks();
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Performs ACC tasks that are defined within this directive. Such
     * tasks are typically dependent on the chemical system at hand, so, by
     * running this method, this job may become system-specific. Moreover,
     * ACC may 
     * modify the content of any of its components, i.e., 
     * keywords, directive data, accordingly to the  
     * needs of the given tasks.
     * Tasks are performed serially, one after the other, according to this
     * ordering scheme:
     * <ol>
     * <li>tasks found in this very {@link Directive},</li>
     * <li>tasks found in {@link Keyword}s,</li>
     * <li>tasks found in {@link DirectiveData}s,</li>
     * <li>recursion into embedded (sub){@link Directive}s,</li>
     * </ol>
     * In each case, when multiple components are present, the ordering of 
     * components is respected when searching and performing tasks.
     * @param mols the chemical system/s given to any system-dependent tasks.
     * @param job the job containing this directive and that required to perform
     * the task.
     * @param masterJob the job that requests the update of the job containing 
     * this directive.
     */
    
    public void performACCTasks(List<IAtomContainer> mols, Job job, Job masterJob)
    {
    	if (accTaskParams!=null)
    	{
    		try {
				performACCTask(mols, accTaskParams, this, job, masterJob);
			} catch (ClassNotFoundException e) {
				throw new Error("Unable to perform ACC task.",e);
			}
    	}
    	
    	for (Keyword k : keywords)
    	{
    		if (k.hasACCTask())
    		{
	    		ParameterStorage ps;
    			if (k.getTaskParams()!=null)
    			{
    				ps = k.getTaskParams();
    			} else {
    				ps = parseACCTaskParams(k.getValueAsLines(), k);
    			}
	    		try {
					performACCTask(mols, ps, k, job, masterJob);
				} catch (ClassNotFoundException e) {
					throw new Error("Unable to perform ACC task.",e);
				}
    		}
    	}
    	for (DirectiveData dd : dirData)
    	{
    		if (dd.hasACCTask())
    		{
	    		ParameterStorage ps;
    			if (dd.getTaskParams()!=null)
    			{
    				ps = dd.getTaskParams();
    			} else {
	    			// This is legacy code: deals with cases where the parameters 
	    			//defining the task are still listed in the 'value' of dd
	    			ArrayList<String> lines = dd.getLines();
	    			
	    			// WARNING! Here we assume that the entire content of the 
	    			// directive data, is about the ACC task. Thus, we add the 
	    			// multiline start/end labels so that the getACCTaskParams
	    			// method will keep read all the lines as one.
	    			if (lines.size()>1)
	    			{
		    			lines.set(0, ChemSoftConstants.JDOPENBLOCK + lines.get(0));
		    			lines.set(lines.size()-1, lines.get(lines.size()-1) 
		    					+ ChemSoftConstants.JDCLOSEBLOCK);
	    			}
	    			ps = parseACCTaskParams(lines, dd);
    			}
				try {
					performACCTask(mols, ps, dd, job, masterJob);
				} catch (ClassNotFoundException e) {
					throw new Error("Unable to perform ACC task.",e);
				}
    		}	
    	}
    	for (Directive d : subDirectives)
    	{
    		//Recursion into nested directives
    		d.performACCTasks(mols, job, masterJob);
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Parses the block of lines as to find parameters defining an ACC tasks.
     * @param lines to parse.
     * @return the list of parameter storage units.
     */
    
    public static ParameterStorage parseACCTaskParams(List<String> lines)
    {	
    	return parseACCTaskParams(lines, null);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Parses the block of lines as to find parameters defining an ACC tasks.
     * @param lines to parse.
     * @param dirComp source of the parameters (used only for logging).
     * @return the list of parameter storage units.
     */
    
    private static ParameterStorage parseACCTaskParams(List<String> lines, 
    		IDirectiveComponent dirComp)
    {	
    	// This takes care of any $START/$END label needed to make all JD lines
    	// fit into a single line for the purpose of making the 
    	// directive component line.
    	List<String> linesPack = TextAnalyzer.readTextWithMultilineBlocks(
    			new ArrayList<String>(lines), ChemSoftConstants.JDCOMMENT, 
    			ChemSoftConstants.JDOPENBLOCK, ChemSoftConstants.JDCLOSEBLOCK);
    	
    	// Then we take away any line that does not contain ACC tasks
    	int numOfLinesWithTask = 0;
    	boolean fixObsoleteSytax = false;
    	String task = "";
    	for (int iLine=0; iLine<linesPack.size(); iLine++)
    	{
    		String line = linesPack.get(iLine);
    		if (line.toUpperCase().contains(ChemSoftConstants.JDLABACCTASK))
    		{
    			numOfLinesWithTask++;
    		} else if (line.toUpperCase().contains(GaussianConstants.LABPARAMS))
        	// Due to legacy code using a different convention (the Gaussian stuff)
        	// We need to check for the possibility of a different keyword, and
    	    // we need to adapt the obsolete syntax.
    		{
    			String lineMod = line.replace(GaussianConstants.LABPARAMS,"");
    			linesPack.set(iLine, lineMod);
    			fixObsoleteSytax=true;
    			if (lineMod.toUpperCase().contains(BasisSetConstants.ATMSPECBS))
    				task = Task.getExisting("generateBasisSet").casedID;
    			else if (lineMod.toUpperCase().contains(
    					Task.getExisting("generateConstraints").ID))
    				task = Task.getExisting("generateConstraints").casedID;
    			numOfLinesWithTask++;
    		}
    	}
    	
    	if (numOfLinesWithTask==0)
		{
    		// Nothing to do
			return new ParameterStorage();
		} 
    	else if (numOfLinesWithTask>1)
    	{
    		Terminator.withMsgAndStatus("ERROR! Unexpected format of "
    				+ "the directive component containing this value: '" 
    				+ lines + "'", -1);
    	}
    	// Warning: because of the above the rest is assuming there is only one
    	// task, even if the return value is a list.
    	
		ArrayList<String> taskSpecificLines = new ArrayList<String>(
				Arrays.asList(linesPack.get(0).split("\\r?\\n|\\r")));
		ParameterStorage ps = new ParameterStorage();
		if (dirComp!=null)
		{
			ps.importParametersFromLines(taskSpecificLines);
		} else {
			ps.importParametersFromLines(taskSpecificLines);
		}
		
		//TODO-gg: much of this will eventually be removed or moved to a dedicated 
		// class for converting job details files
		
		//Another fix of the obsolete syntax
		if (task.toUpperCase().equals(Task.getExisting("generateConstraints").ID))
		{
			taskSpecificLines = new ArrayList<String>(
					Arrays.asList(ps.getParameter(
							Task.getExisting("generateConstraints").ID).getValue()
							.toString().split("\\r?\\n|\\r")));
			ps = new ParameterStorage();
			String smarts = "";
            String atomIDs ="";
            for (String line : taskSpecificLines)
            {
            	//WARNING! this is very hardcoded!!! 
            	// Takes from GaussianInputWriter
            	String key = line.substring(0, line.indexOf(":"));
            	String value = line.substring(line.indexOf(":")+1).trim();
            	switch (key.toUpperCase())
            	{
            		case "SMARTS":
            			if (smarts.isBlank())
            			{
            				smarts = value;
            			} else {
            				smarts = smarts 
            						+ System.getProperty("line.separator") 
            						+ value;
            			}
            			break;
            		case "ATOMIDS":
            			if (atomIDs.isBlank())
            			{
            				atomIDs = value;
            			} else {
            				atomIDs = atomIDs 
            						+ System.getProperty("line.separator") 
            						+ value;
            			}
            			break;
            		case "GENERATECONSTRAINTS":
            			break;
            		default:
            			ps.setParameter(key,
            					line.substring(line.indexOf(":")+1).trim());
            	}
            }
            if (!smarts.isBlank())
			{
            	ps.setParameter("SMARTS",smarts);
			}
            if (!atomIDs.isBlank())
			{
            	ps.setParameter("ATOMIDS",atomIDs);
			}
		}

    	if (fixObsoleteSytax)
    	{
    		ps.setParameter(ChemSoftConstants.JDLABACCTASK, task);
    	}
    	return ps;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs the task that is specified in the given set of parameters. Note
     * the given parameters are all meant to pertain a single task.
     * @param mols the molecular representation used in system-dependent tasks.
     * @param params the collection of parameters defining one single task 
     * (though this can certainly require more than one parameter).
     * @param dirComp the directive component that required performing the task.
     * @param job the job containing the directive component that required 
     * performing the task.
     * @param masterJob the job that requests the update of the job containing 
     * this directive.
     * @throws ClassNotFoundException when a {@link Worker} could not be found 
     * to perform an embedded task.
     */
    
    private void performACCTask(List<IAtomContainer> mols, ParameterStorage params, 
    		IDirectiveComponent dirComp, Job job, Job masterJob) 
    				throws ClassNotFoundException
    {	
    	String task = "none";
    	if (params.contains(ChemSoftConstants.JDLABACCTASK))
        {
    		task = params.getParameter(
            		ChemSoftConstants.JDLABACCTASK).getValueAsString();
        } else if (params.contains(ChemSoftConstants.JDACCTASK))
        {
    		task = params.getParameter(
            		ChemSoftConstants.JDACCTASK).getValueAsString();
        } else {
        	return;
        }
        
    	//NB: this will exit with an error should the string not be good enough
    	Task taskID = Task.getExisting(task, true);
        switch (taskID.ID) 
        {   
	        case "ADDATOMSPECIFICKEYWORDS":
	        {
	        	if (!(dirComp instanceof Directive))
	        	{
	        		throw new IllegalArgumentException("Task " + task 
	        				+ " can be performed only from within a "
	        				+ Directive.class.getSimpleName() + ". "
	        				+ "Not from " + dirComp.getClass().getName() + ".");
	        	}
	        	Directive targetDir = (Directive) dirComp;
	        	
	        	// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
        		
        		// Define atom pointers
        		ParameterStorage labMakerParams = params.clone();
        		
                //TODO-gg use WorkerConstant.TASK
        		labMakerParams.setParameter("TASK", 
        				Task.getExisting("generateAtomLabels").ID);
				AtomLabelsGenerator labGenerator = (AtomLabelsGenerator) 
            			WorkerFactory.createWorker(labMakerParams, masterJob);
        		List<String> pointers = labGenerator.generateAtomLabels(mol);
        		
	        	// Identify specific atoms to work with
        		List<String> atmSpecValues = new ArrayList<String>();
                ParameterStorage cnstrParams = params.clone();
                
                //TODO-gg use WorkerConstant.TASK
                cnstrParams.setParameter("TASK",
                		Task.getExisting("generateAtomTuples").ID);
                cnstrParams.setParameter("VALUEDKEYWORDS", 
                		"options prefix suffix");
                
            	Worker w = WorkerFactory.createWorker(cnstrParams, masterJob);
            	AtomTupleGenerator cnstrg = (AtomTupleGenerator) w;
            	
            	for (AnnotatedAtomTuple tuple : cnstrg.createTuples(mol))
            	{
            		for (Integer id : tuple.getAtomIDs())
            		{
            			String prefix = tuple.getValueOfAttribute("prefix");
            			if (prefix==null)
            				prefix = "";

            			String suffix = tuple.getValueOfAttribute("suffix");
            			if (suffix==null)
            				suffix = "";
            			
	            		atmSpecValues.add(prefix + pointers.get(id) + suffix);
            		}
            	}
	        	
	        	// Make and append atom-specific keywords
	        	String kwName = ""; 
	        	boolean isLoud = false; // By default it's a "mute" keyword
	        	if (params.contains(ChemSoftConstants.KEYWORDNAME))
            	{
            		kwName = params.getParameter(
            				ChemSoftConstants.KEYWORDNAME).getValueAsString();
            	}
	        	if (params.contains(ChemSoftConstants.KWISLOUD))
            	{
            		isLoud = Boolean.parseBoolean(params.getParameter(
            				ChemSoftConstants.KWISLOUD).getValueAsString());
            	}
	        	for (String value : atmSpecValues)
	        	{
	        		targetDir.addKeyword(new Keyword(kwName, isLoud, value));
	        	}
	        	break;
	        }
            case "ADDFILENAME":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
            	
            	String pathname = job.getParameter(
            			ChemSoftConstants.PAROUTFILEROOT)
            			.getValueAsString();
            	
            	if (params.contains(ChemSoftConstants.PARGETFILENAMEROOTSUFFIX))
            	{
            		String suffix = params.getParameter(
            			ChemSoftConstants.PARGETFILENAMEROOTSUFFIX)
            			.getValueAsString();
            		pathname = pathname + suffix;
            	}
            	if (params.contains(ChemSoftConstants.PARGETFILENAMEROOTQUOTE))
            	{
            		String q = params.getParameter(
                			ChemSoftConstants.PARGETFILENAMEROOTQUOTE)
                			.getValueAsString();
            		pathname = q + pathname + q;
            	}
            	
            	((IValueContainer) dirComp).setValue(pathname);
            	break;
            }
            
            case "ADDGEOMETRY":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
            	
            	CoordsType coordsType = CoordsType.XYZ;
            	if (params.contains(ChemSoftConstants.PARCOORDTYPE))
            	{
            		String value = params.getParameter(
            				ChemSoftConstants.PARCOORDTYPE)
            				.getValueAsString();
            	
            		coordsType = CoordsType.valueOf(
            			value.trim().toUpperCase());
            	}
            	int geometryId = 0;
            	if (params.contains(ChemSoftConstants.PARMULTIGEOMID))
            	{
            		geometryId = Integer.parseInt(params.getParameter(
            				ChemSoftConstants.PARMULTIGEOMID).getValueAsString());
            	}
            	
            	boolean useAtomTags = params.contains(ChemSoftConstants.PARUSEATMTAGS);
            	
            	switch (coordsType)
            	{    
                	case ZMAT:
                	{
                		ParameterStorage zmatMakerTask = new ParameterStorage();
                		zmatMakerTask.setParameter(WorkerConstants.PARTASK, 
                				"PRINTZMATRIX");
                		zmatMakerTask.setParameter("MOL", 
                				NamedDataType.IATOMCONTAINER, 
                				mols.get(geometryId));
                        Worker w = WorkerFactory.createWorker(zmatMakerTask,
                        		masterJob);
                        ZMatrixHandler zmh = (ZMatrixHandler) w;
                        //TODO-gg make it use atom tags upon request
                        ZMatrix zmat = zmh.makeZMatrix();
                        if (params.contains(ZMatrixConstants.SELECTORMODE))
                        {
                        	ParameterStorage cnstMakerTask = params.clone();
                        	cnstMakerTask.setParameter(
                        			WorkerConstants.PARTASK, 
                        			Task.getExisting("generateConstraints").ID);

                        	
                            ConstraintsGenerator cnstrg = (ConstraintsGenerator)
                            		WorkerFactory.createWorker(cnstMakerTask,
                            				masterJob);
                        	ConstraintsSet cs = new ConstraintsSet();
                        	try {
            					cs = cnstrg.createConstraints(mols.get(
            							geometryId));
            				} catch (Exception e) {
            					e.printStackTrace();
            					Terminator.withMsgAndStatus("ERROR! "
            							+ "Unable to create constraints. "
            							+ "Exception from the "
            							+ "ConstraintGenerator.", -1);
            				}
                        	String mode = params.getParameterValue(
                        			ZMatrixConstants.SELECTORMODE);
                        	switch (mode.toUpperCase())
                        	{
                        	case ZMatrixConstants.SELECTORMODE_CONSTANT:
                            	zmat.setConstants(cs);
                        		break;

                        	case ZMatrixConstants.SELECTORMODE_VARIABLES:
                            	zmat.setVariables(cs);
                        		break;
                        	}
                        }
                        
                		((IValueContainer) dirComp).setValue(zmat);
                		break;
                	}
                	
                	case XYZ:
                	default:
                	{
                		IAtomContainer mol = mols.get(geometryId);
                		if (useAtomTags)
                		{
                			mol = MolecularUtils.makeSimpleCopyWithAtomTags(mol);
                		}
                		((IValueContainer) dirComp).setValue(mol);
                		break;
                	}
            	}
            	break;
            }
            	
            case "GENERATEBASISSET":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
            	
        		// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
        		
            	//TODO verbosity/logging
                System.out.println("ACC starts creating atom-specific "
                		+ "Basis Set");
                
            	//We require the component to be a DirectiveData
            	if (!dirComp.getComponentType().equals(
            			DirectiveComponentType.DIRECTIVEDATA))
            	{
            		Terminator.withMsgAndStatus("ERROR! Atom-specific "
            				+ "basis set can only be defined with a "
            				+ DirectiveData.class.getName() 
            				+ " object", -1);
            	}
            	
                ParameterStorage bsGenParams = new ParameterStorage();
                bsGenParams.setParameter(params.getParameter(
                		BasisSetConstants.ATMSPECBS));
                //TODO- task from constant
                bsGenParams.setParameter("TASK", task);
            	Worker w = WorkerFactory.createWorker(bsGenParams, masterJob);
                BasisSetGenerator bsg = (BasisSetGenerator) w;
                
                bsg.setAtmIdxAsId(true);
                BasisSet bs = bsg.assignBasisSet(mol);
                
                ((IValueContainer) dirComp).setValue(bs);
            	break;
            }
            
            case "GENERATECONSTRAINTS":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
        	
            	// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
        		
            	//TODO verbosity/logging
                System.out.println("ACC starts creating geometric constraints");
                
                ParameterStorage cnstrParams = params.clone();
                
                //TODO: this should be avoided by using TASK instead of ACCTASK
                //TODO-gg use WorkerConstant.TASK
                cnstrParams.setParameter("TASK", task);
                
            	Worker w = WorkerFactory.createWorker(cnstrParams, masterJob);
            	ConstraintsGenerator cnstrg = (ConstraintsGenerator) w;
            	
            	ConstraintsSet cs = new ConstraintsSet();
            	try {
					cs = cnstrg.createConstraints(mol);
				} catch (Exception e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR! Unable to create "
							+ "constraints. Exception from the "
							+ "ConstraintGenerator.", -1);
				}
            	
            	// Replace value of component that triggered this task
            	((IValueContainer) dirComp).setValue(cs);
                break;
            }
            
            case "GENERATECONFORMATIONALSPACE":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
        	
            	// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
        		
                ParameterStorage csGenParams = params.clone();
                
                //TODO: this should be avoided by using TASK instead of ACCTASK
                //TODO-gg use WorkerConstant.TASK
                csGenParams.setParameter("TASK", task);
                
            	Worker w = WorkerFactory.createWorker(csGenParams, masterJob);
            	ConformationalSpaceGenerator csGen = 
            			(ConformationalSpaceGenerator) w;
            	
            	ConformationalSpace cs = new ConformationalSpace();
            	try {
            		cs = csGen.createConformationalSpace(mol);
				} catch (Exception e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR! Unable to create "
							+ "conformational space. Exception from the "
							+ "ConformationalSpaceGenerator.", -1);
				}
            	
            	// Replace value of component that triggered this task
            	((IValueContainer) dirComp).setValue(cs);
            	break;
            }
            
            case "GENERATEATOMLABELS":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
            	// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
                
                ParameterStorage atmLabelsParams = params.clone();
                
                //TODO: this should be avoided by using TASK instead of ACCTASK
                //TODO-gg use WorkerConstant.TASK
                atmLabelsParams.setParameter("TASK", task);
                
            	Worker w = WorkerFactory.createWorker(atmLabelsParams, 
            			masterJob);
            	AtomLabelsGenerator labelsGenerator = (AtomLabelsGenerator) w;
            	
            	TextBlock labels = new TextBlock(
            			labelsGenerator.generateAtomLabels(mol));
            	
            	// Replace value of component that triggered this task
            	((IValueContainer) dirComp).setValue(labels);
            	break;
            }
            
            case "GENERATEATOMTUPLES":
            {
            	ensureTaskIsInIValueContainer(task, dirComp);
            	// WARNING: uses only the first molecule
        		IAtomContainer mol = mols.get(0);
                
                ParameterStorage atmTuplesParams = params.clone();
                
                //TODO: this should be avoided by using TASK instead of ACCTASK
                //TODO-gg use WorkerConstant.TASK
                atmTuplesParams.setParameter("TASK", task);
                
            	Worker w = WorkerFactory.createWorker(atmTuplesParams, 
            			masterJob);
            	AtomTupleGenerator labelsGenerator = (AtomTupleGenerator) w;
            	
            	AnnotatedAtomTupleList tuples = new AnnotatedAtomTupleList(
            			labelsGenerator.createTuples(mol));
            	
            	// Replace value of component that triggered this task
            	((IValueContainer) dirComp).setValue(tuples);
            	break;
            }
                
            //TODO: add here other atom/molecule specific option
            
                
            default:
                String msg = "WARNING! Task '" + task + "' is not a "
                       + "known task when performing ACC tasks from within "
                       + "a directive in a comp.chem. job.";
                
                //TODO verbosity/logging
                System.out.println(msg);
                break;
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void ensureTaskIsInIValueContainer(String task, 
    		IDirectiveComponent dirComp)
    {
    	if (! (dirComp instanceof IValueContainer))
    	{
    		throw new IllegalArgumentException("Task " + task 
    				+ " can be performed only from within "
    				+ Keyword.class.getSimpleName()
    				+ "or "
    				+ DirectiveData.class.getSimpleName()
    				+ ". Not from " 
    				+ dirComp.getClass().getName() + ".");
    	}
    }
    
//-----------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
    	if ( o== null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	   
 	    Directive other = (Directive) o;
 	   
        if (!this.getName().equals(other.getName()))
        	return false;
        
        if ((this.accTaskParams!=null && other.accTaskParams==null)
     		    || (this.accTaskParams==null && other.accTaskParams!=null))
        {
        	return false;
        }
 	    if (this.accTaskParams!=null && other.accTaskParams!=null)
 	    {
 	    	if (!this.accTaskParams.equals(other.accTaskParams))
 	    		return false;
 	    }
        
        if (this.getAllKeywords().size() != other.getAllKeywords().size())
        	return false;
        for (int i=0; i<this.getAllKeywords().size(); i++)
    	{
    		Keyword tKey = this.getAllKeywords().get(i);
    		Keyword oKey = other.getAllKeywords().get(i);
    		if (!tKey.equals(oKey))
    			return false;
    	}
        
        if (this.getAllSubDirectives().size() 
        		!= other.getAllSubDirectives().size())
        	return false;
        for (int i=0; i<this.getAllSubDirectives().size(); i++)
    	{
        	Directive tDir = this.getAllSubDirectives().get(i);
        	Directive oDir = other.getAllSubDirectives().get(i);
    		if (!tDir.equals(oDir))
    			return false;
    	}
        
        if (this.getAllDirectiveDataBlocks().size() != 
        		other.getAllDirectiveDataBlocks().size())
        	return false;
        for (int i=0; i<this.getAllDirectiveDataBlocks().size(); i++)
    	{
        	DirectiveData tDd = this.getAllDirectiveDataBlocks().get(i);
        	DirectiveData oDd = other.getAllDirectiveDataBlocks().get(i);
    		if (!tDd.equals(oDd))
    			return false;
    	}
        
        return true;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax ACC's job details file.
     * @return the list of lines for a job details file
     */

    public List<String> toLinesJobDetails()
    {
        List<String> lines = new ArrayList<String>();
        String root = ChemSoftConstants.JDLABDIRECTIVE + name + " ";
        
        // Keywords are appended in the same line as the directive's name
        for (Keyword k : keywords)
        {
            lines.add(root + k.toStringJobDetails());
        }
        
        //Then add the data section
        for (DirectiveData data : dirData)
        {
            ArrayList<String> dataLines = data.toLinesJobDetails();
            lines.add(root + dataLines.get(0));
            if (dataLines.size() > 1)
            { 
                for (int i=1; i<dataLines.size(); i++)
                {
                    lines.add(dataLines.get(i));
                }
            }
        }
        
        //Finally add the sub-directives
        for (Directive subDir : subDirectives)
        {
            for (String dirLine : subDir.toLinesJobDetails())
            {
                if (dirLine.startsWith(ChemSoftConstants.JDLABDIRECTIVE))
                {
                    lines.add(root + dirLine);
                }
                else
                {
                    lines.add(dirLine);
                }
            }
        }
        
        //Ensure that also empty directives are reported
        if (lines.size() == 0)
        {
            lines.add(root);
        }

        return lines;
    }

//------------------------------------------------------------------------------

    public static class DirectiveSerializer 
    implements JsonSerializer<Directive>
    {
        @Override
        public JsonElement serialize(Directive dir, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("name", dir.name);
            
            if (dir.keywords!=null && dir.keywords.size()>0)
                jsonObject.add("keywords", context.serialize(dir.keywords));
            
            if (dir.subDirectives!=null && dir.subDirectives.size()>0)
                jsonObject.add("subDirectives", 
                		context.serialize(dir.subDirectives));
            
            if (dir.dirData!=null && dir.dirData.size()>0)
                jsonObject.add("dirData", context.serialize(dir.dirData));
           
			if (dir.getTaskParams()!=null)
				jsonObject.add("accTaskParams", context.serialize(
						dir.getTaskParams()));
			
            return jsonObject;
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Creates a new directive that contains all components of this one (to the
     * extent the components' content is cloneable) and is named as specified
     * in the given argument.
     * @param dirName the name of the directive to create.
     * @return the new directive.
     * @throws CloneNotSupportedException 
     */
    @Override
	public Directive clone() throws CloneNotSupportedException 
	{
		Directive newDir = new Directive(name);

		for (Keyword k : keywords)
		{
			newDir.addKeyword(k.clone());
		}
		for (DirectiveData dd : dirData)
		{
			newDir.addDirectiveData(dd.clone());
		}
		for (Directive d : subDirectives)
		{
			newDir.addSubDirective(d.clone());
		}
		if (accTaskParams!=null)
        {
			newDir.accTaskParams = accTaskParams.clone();
        }
		return newDir;
	}
 
//-----------------------------------------------------------------------------

	
}
