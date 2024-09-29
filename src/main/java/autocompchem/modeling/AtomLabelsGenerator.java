package autocompchem.modeling;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomConstants;
import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Tool to generate atom labels for all atoms in an atom container.
 * 
 * @author Marco Foscato
 */


public class AtomLabelsGenerator extends AtomContainerInputProcessor
{
    /**
     * The name of the output file, if any
     */
    private File outFile;
    
    /**
     * Specified which policy to use when generating atom labels.
     */
    private AtomLabelMode mode = AtomLabelMode.IndexBased;
    
    /**
     * Policies for generating atom labels.
     */
    public enum AtomLabelMode {ElementBased, IndexBased, AtomicNumber, 
    	IndexOnly};
    
    /**
     * Flag defining if we use 0- or 1-based indexing. 
     * By default, we use 0-based.
     */
    private boolean zeroBased = true;
    
    /**
     * Separator used when reporting the list of atom labels to log or output 
     * file.
     */
    private String labelsSeparator = ", ";
    
    /**
     * String defining the task of generating atom labels
     */
    public static final String GENERATEATOMLABELSTASKNAME = "generateAtomLabels";

    /**
     * Task about generating atom labels
     */
    public static final Task GENERATEATOMLABELSTASK;
    static {
    	GENERATEATOMLABELSTASK = Task.make(GENERATEATOMLABELSTASKNAME);
    }


//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomLabelsGenerator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATEATOMLABELSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomLabelsGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomLabelsGenerator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        // Name of output file
        if (params.contains("OUTFILE"))
        {
            //Get and check output file
            this.outFile = new File(
                        params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        if (params.contains(ChemSoftConstants.PARATMLABELTYPE))
        {
        	this.mode = EnumUtils.getEnumIgnoreCase(AtomLabelMode.class, 
            		params.getParameter(ChemSoftConstants.PARATMLABELTYPE)
            		.getValueAsString());
        }
        
        if (params.contains("SEPARATOR"))
        {
            this.labelsSeparator = 
            		params.getParameter("SEPARATOR").getValueAsString();
        }
        
        if (params.contains("ZEROBASED"))
        {
            this.zeroBased = Boolean.valueOf(
            		params.getParameter("ZEROBASED").getValueAsString());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialized.
     */

    @Override
    public void performTask()
    {
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GENERATEATOMLABELSTASK))
    	{
    		List<String> lst = generateAtomLabels(iac);
            String txt = StringUtils.mergeListToString(lst, labelsSeparator);
            
            logger.info(txt);
            
            if (outFile!=null)
            {
            	IOtools.writeTXTAppend(outFile, 
            			txt + System.getProperty("line.separator"),
            			true);
            }
            
    		if (exposedOutputCollector != null)
        	{

                TextBlock tb = new TextBlock(lst);
    			String molID = "mol-" + i;
  		        exposeOutputData(new NamedData(
  		        		GENERATEATOMLABELSTASK.ID + "_" + molID, 
  		        		NamedDataType.TEXTBLOCK, tb));
        	}
    	} else {
    		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Define atom labels for a atom container using the currently loaded 
     * settings (e.g., {@link AtomLabelMode}).
     * @param mol the molecular system we create atom label for.
     * @return the list of labels.
     */

    public List<String> generateAtomLabels(IAtomContainer mol) 
    {
    	List<String> labels = null;
    	switch (mode)
    	{
			case ElementBased:
				labels = generateElementBasedLabels(mol, zeroBased);
				break;
				
			case IndexBased:
				labels = generateIndexBasedLabels(mol, zeroBased);
				break;
				
			case IndexOnly:
				labels = generateIndexOnlyLabels(mol, zeroBased);
				break;
				
			case AtomicNumber:
				labels = generateAtomicNumberLabels(mol);
				break;
    	}
    	
    	return labels;
    }

//------------------------------------------------------------------------------

    /**
     * Generate the atom labels using the {@link AtomLabelMode#ElementBased} 
     * strategy, i.e., each
     * atom is indexed based on its elemental symbol. 
     * For example, molecule CCOOH
     * will generate labels "C0", "C1", "O0", "O1", and "H0".
     * @param iac the container to work on.
     * @param zeroBased use <code>false</code> to use 1-based indexing. 
     * By default we work with 0-based indexing.
     * @return the list of atom labels.
     */
    public static List<String> generateElementBasedLabels(IAtomContainer iac,
			boolean zeroBased) 
	{
		List<String> labels = new ArrayList<String>();
        Map<String,Integer> counters = new HashMap<String,Integer>();
        for (IAtom atm : iac.atoms())
        {
            String el = AtomUtils.getSymbolOrLabel(atm);
            String label = "notSet";
            if (counters.keySet().contains(el))
            {
                int elCount = counters.get(el);
                label = el + (elCount+1);
                counters.put(el,elCount+1);
            } else {
            	int base = 0;
            	if (!zeroBased)
                {
            		base = 1;
                }
                label = el + base;
                counters.put(el, base);
            }
            atm.setProperty(AtomConstants.ATMLABEL, label);
            labels.add(label);
        }
		return labels;
	}
	
//------------------------------------------------------------------------------

    /**
     * Generate the atom labels using the {@link AtomLabelMode#IndexBased} 
     * strategy, i.e., each
     * atom is indexed based on its elemental symbol. 
     * For example, molecule CCOOH
     * will generate labels "C0", "C1", "O2", "O3", and "H4".
     * @param iac the container to work on.
     * @param zeroBased use <code>false</code> to use 1-based indexing. 
     * By default we work with 0-based indexing.
     * @return the list of atom labels.
     */
	public static List<String> generateIndexBasedLabels(IAtomContainer iac,
			boolean zeroBased) 
	{
		List<String> labels = new ArrayList<String>();
		for (int i=0; i<iac.getAtomCount(); i++)
        {
			IAtom atm = iac.getAtom(i);
            String el = AtomUtils.getSymbolOrLabel(atm);
            String label = "notSet";
            if (zeroBased)
            {
                label = el + i;
            } else {
                label = el + (i+1);
            }
            atm.setProperty(AtomConstants.ATMLABEL, label);
            labels.add(label);
        }
		return labels;
	}
	
//------------------------------------------------------------------------------

    /**
     * Generate the atom labels using the {@link AtomLabelMode#IndexOnly} 
     * strategy, i.e., each
     * atom is indexed based on its elemental symbol. 
     * For example, molecule CCOOH
     * will generate labels "0", "1", "2", "3", and "4".
     * @param iac the container to work on.
     * @param zeroBased use <code>false</code> to use 1-based indexing. 
     * By default we work with 0-based indexing.
     * @return the list of atom labels.
     */
	public static List<String> generateIndexOnlyLabels(IAtomContainer iac,
			boolean zeroBased) 
	{
		List<String> labels = new ArrayList<String>();
		for (int i=0; i<iac.getAtomCount(); i++)
        {
			IAtom atm = iac.getAtom(i);
            String label = "notSet";
            if (zeroBased)
            {
                label = "" + i;
            } else {
                label = "" + (i+1);
            }
            atm.setProperty(AtomConstants.ATMLABEL, label);
            labels.add(label);
        }
		return labels;
	}
	
//------------------------------------------------------------------------------

    /**
     * Generate the atom labels using the {@link AtomLabelMode#AtomicNumber} 
     * strategy, i.e., each
     * atom is defined by its atomic number
     * @param iac the container to work on.
     * @return the list of atom labels.
     */
	public static List<String> generateAtomicNumberLabels(IAtomContainer iac)
	{
		List<String> labels = new ArrayList<String>();
		for (int i=0; i<iac.getAtomCount(); i++)
        {
			IAtom atm = iac.getAtom(i);
			String label = atm.getAtomicNumber().toString();
            atm.setProperty(AtomConstants.ATMLABEL, label);
            labels.add(label);
        }
		return labels;
	}

//-----------------------------------------------------------------------------

}
