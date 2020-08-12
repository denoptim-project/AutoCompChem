package autocompchem.chemsoftware;


/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.worker.Worker;
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

public class Directive implements IDirectiveComponent
{
    /**
     * Directive name.
     */
    private String name = "#noname";

    /**
     * List of keywords.
     */
    private ArrayList<Keyword> keywords;

    /**
     * List of subordinate directives.
     */
    private ArrayList<Directive> subDirectives;

    /**
     * Data attached directly to this directive.
     */
    private ArrayList<DirectiveData> dirData;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive.
     */

    public Directive()
    {
        keywords = new ArrayList<Keyword>();
        subDirectives = new ArrayList<Directive>();        
        dirData = new ArrayList<DirectiveData>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive with a name.
     * @param name the name if the directive.
     */

    public Directive(String name)
    {
        this.name = name;
        keywords = new ArrayList<Keyword>();
        subDirectives = new ArrayList<Directive>();
        dirData = new ArrayList<DirectiveData>();
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
    
	public DirectiveComponent getComponentType() 
	{
		return DirectiveComponent.DIRECTIVE;
	}

//-----------------------------------------------------------------------------

    /**
     * Returns all sub directive.
     * @return the list of sub directives.
     */

    public ArrayList<Directive> getAllSubDirectives()
    {
        return subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the sub directive with the given name. 
     * @param name the name of the sub directive to get.
     * @return the sub directive with the given name or null it it doesn't 
     * exist.
     */

    public Directive getSubDirective(String name)
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

    public ArrayList<Keyword> getAllKeywords()
    {
        return keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the keyword with the given name. Only keyword belonging to
     * this directive can be returned. Keywords of sub directives are ignored. 
     * @param name the name of the keyword to get.
     * @return the keyword with the given name or null if such keyword
     * does not exist.
     */

    public Keyword getKeyword(String name)
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
     * Returns all data blocks contained in this directive.
     * @return the list of data blocks.
     */

    public ArrayList<DirectiveData> getAllDirectiveDataBlocks()
    {
        return dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the data block having the specified name. Data of subordinate
     * directives are ignored.
     * @param name the name of the data block to get.
     * @return the data blocks having the specified name or null if such data
     * block does not exist.
     */

    public DirectiveData getDirectiveData(String name)
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

    public void setAllSubDirectives(ArrayList<Directive> subDirectives)
    {
        this.subDirectives = subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single sub directive of this directive. Methods
     * calling this method must make sure the new sub directive is not a
     * parent directive of <code>this</code>, otherwise a loop is created.
     * @param dir the new directive.
     * @param owKeys if <code>true</code> makes this method overwrite the
     * keywords of the existing directive. 
     * @param owSubDirs if <code>true</code> makes this method overwrite the
     * sub-directives of the existing directive.
     * @param owData if <code>true</code> makes this method overwrite the
     * data of the existing directive. 
     */

    public void setSubDirective(Directive dir, boolean owKeys, 
                                              boolean owSubDirs, boolean owData)
    {
        Directive oldDir = getSubDirective(dir.getName());
        if (oldDir == null)
        {
            this.addSubDirective(dir);
        }
        else
        {
            if (owKeys)
            {
                oldDir.setAllKeywords(dir.getAllKeywords());
            }
            if (owSubDirs)
            {
                oldDir.setAllSubDirectives(dir.getAllSubDirectives());
            }
            if (owData)
            {
                oldDir.setAllDirectiveDataBlocks(
                                               dir.getAllDirectiveDataBlocks());
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of keywords.
     * @param keywords the new list of keywords.
     */

    public void setAllKeywords(ArrayList<Keyword> keywords)
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
        Keyword oldKw = getKeyword(kw.getName());
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

    public void setAllDirectiveDataBlocks(ArrayList<DirectiveData> dirData)
    {
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single block of data of this directive.
     * @param dd the new data block.
     */

    public void setDataDirective(DirectiveData dd)
    {
        DirectiveData oldDd = getDirectiveData(dd.getName());
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
     * Checks if there is any ACC task definition within this directive.
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
    public boolean hasACCTask()
    {
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
     * Performs and ACC tasks that are defined within this directive. Such
     * tasks are typically dependent on a specific atom container, and, by
     * running this method, this job becomes molecule-specific. Moreover,
     * ACC may 
     * modify the content of this directive (or any of its components: 
     * keywords, directive data, and sub directives) accordingly to the  
     * needs of the given tasks.
     * Tasks are performed serially, one after the other, according to this
     * ordering scheme:
     * <ol>
     * <li>tasks found in Keywords,</li>
     * <li>tasks found in DirectiveData,</li>
     * <li>tasks found in sub Directives,</li>
     * </ol>
     * In each case, when multiple components are present, the ordering of 
     * components is respected when searching and performing tasks.
     * @param mol the molecular representation given to mol-dependent tasks.
     * @param job the job that called this method.
     */
    
    public void performACCTasks(IAtomContainer mol, Job job)
    {
    	for (Keyword k : keywords)
    	{
    		ArrayList<ParameterStorage> psLst = 
    				getACCTaskParams(k.getValue(), k);
    		for (ParameterStorage ps : psLst)
    		{
        		performACCTask(mol,ps,k);
    		}
    	}
    	for (DirectiveData dd : dirData)
    	{
    		ArrayList<ParameterStorage> psLst = 
    				getACCTaskParams(dd.getLines(), dd);
    		for (ParameterStorage ps : psLst)
    		{
        		performACCTask(mol,ps,dd);
    		}
    	}
    	for (Directive d : subDirectives)
    	{
    		// This is effectively a recursion into nested directives
    		// Also note that ACC tasks are effectively defined only in 
    		// Keywords and DirectiveData.
    		d.performACCTasks(mol, job);
    	}
    }

//-----------------------------------------------------------------------------
    
    /**
     * Parses the block of lines as to find parameters defining the ACC tasks.
     * @param lines to parse.
     * @return the list of parameter storage units.
     */
    
    private ArrayList<ParameterStorage> getACCTaskParams(
    		ArrayList<String> lines, IDirectiveComponent dirComp)
    {
    	ArrayList<String> linesPack = TextAnalyzer.readTextWithMultilineBlocks(
    			lines, ChemSoftConstants.JDCOMMENT, 
    			ChemSoftConstants.JDOPENBLOCK, ChemSoftConstants.JDCLOSEBLOCK);
    	
    	ArrayList<ParameterStorage> psList = new ArrayList<ParameterStorage>();
    	for (String line : linesPack)
    	{
    		if (line.startsWith(ChemSoftConstants.JDLABACCTASK))
    		{
    			line = line.replace(ChemSoftConstants.JDLABACCTASK, "");
    			ParameterStorage ps = new ParameterStorage();
    			ps.importParametersFromLines("Directive " 
		    			+ dirComp.getComponentType() + " " + dirComp.getName(),
		    			new ArrayList<String>(Arrays.asList(line)));
    			psList.add(ps);
    		}
    	}
		return psList;
    }
    
//-----------------------------------------------------------------------------

    /**
     * performs the task that is specified in the given set of parameters.
     * @param mol the molecular representation given to mol-dependent tasks.
     * @param ps the parameters defining the task.
     * @param dirComp the component that contained the definition of the task.
     */
    
    private void performACCTask(IAtomContainer mol, ParameterStorage params, 
    		IDirectiveComponent dirComp)
    {
        for (String task : params.getRefNamesSet())
        {
            switch (task) 
            {
                case ChemSoftConstants.TESTONLY_ACCTASK:
                {
                	DirectiveData dd = new DirectiveData();
                    dd.setReference(dirComp.getName());
                    dd.setValue(ChemSoftConstants.TESTONLY_NEWTEXT);
                    setDataDirective(dd);
                    break;
                }
                	
                case BasisSetConstants.ATMSPECBS:
                {
                	//We require the component to be a DirectiveData
                	if (!dirComp.getComponentType().equals(
                			DirectiveComponent.DIRECTIVEDATA))
                	{
                		Terminator.withMsgAndStatus("ERROR! Atom-specific "
                				+ "basis set can only be defined with a "
                				+ DirectiveData.class.getName() 
                				+ " object", -1);
                	}
                	
                    ParameterStorage bsGenParams = new ParameterStorage();
                    bsGenParams.setParameter(params.getParameter(task));
                    
                    bsGenParams.setParameter(new Parameter("TASK",
                		NamedDataType.STRING, "GENERATEBASISSET"));
                	Worker w = WorkerFactory.createWorker(bsGenParams);
                    BasisSetGenerator bsg = (BasisSetGenerator) w;
                    
                    bsg.setAtmIdxAsId(true);
                    BasisSet bs = bsg.assignBasisSet(mol);
                    
                    //Replace DirectiveData with one with the BS object
                    DirectiveData dd = new DirectiveData();
                    dd.setReference(dirComp.getName());
                    dd.setValue(bs);
                    setDataDirective(dd);
                    break;
                }
                	
                    
                //TODO: add here other atom/molecule specific option

                    
                default:
                    String msg = "WARNING! Task '" + task + "' is not a "
                           + "known task when updating atom/molecule-specific "
                           + "directives in a comp.chem. job.";
                    
                    //TODO verbosity/logging
                    System.out.println(msg);
                    break;
            }
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Custom equality method. Only checks the name of the directive and the
     * size of the lists of keywords, sub directives, and data blocks.
     * @param other the directive to compare with this one
     * @return <code>true</code> if the two objects are equal
     */

    @Override
    public boolean equals(Object other)
    {
        boolean res = false;
        if (other instanceof Directive)
        {
            if (this.getName().equals(((Directive) other).getName()) 
                && this.getAllKeywords().size() 
                           == ((Directive) other).getAllKeywords().size()
                && this.getAllSubDirectives().size()
                      == ((Directive) other).getAllSubDirectives().size()
                && this.getAllDirectiveDataBlocks().size() 
                == ((Directive) other).getAllDirectiveDataBlocks().size())
            {
                res = true;
            }
        }
        return res;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax ACC's job details file.
     * @return the list of lines for a job details file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines = new ArrayList<String>();
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

//-----------------------------------------------------------------------------
 
}
