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

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomConstants;
import autocompchem.atom.AtomUtils;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;


/**
 * Tool to generate atom labels for all atoms in an atom container.
 * 
 * @author Marco Foscato
 */


public class AtomLabelsGenerator extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.GENERATEATOMLABELS)));
    
    /**
     * The name of the input file (molecular structure files)
     */
    private File inFile;

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
    public enum AtomLabelMode {ElementBased, IndexBased};
    
    /**
     * Flag defining if we use 0- or 1-based indexing. 
     * By default, we use 0-based.
     */
    private boolean zeroBased = false;
    
    /**
     * Separator used when reporting the list of atom labels to log or output 
     * file.
     */
    private String labelsSeparator = ", ";

    /**
     * Verbosity level
     */
    private int verbosity = 0;


//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomLabelsGenerator()
    {
        super("inputdefinition/AtomLabelsGenerator.json");
    }

//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        // Define verbosity
        if (params.contains("VERBOSITY"))
        {
            String v = params.getParameter("VERBOSITY").getValueAsString();
            this.verbosity = Integer.parseInt(v);
        }

        // Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            this.inFile = new File(
            		params.getParameter("INFILE").getValueAsString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        // Name of output file
        if (params.contains("OUTFILE"))
        {
            //Get and check output file
            this.outFile = new File(
                        params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        if (params.contains("LABELTYPE"))
        {
            this.mode = AtomLabelMode.valueOf(
            		params.getParameter("LABELTYPE").getValueAsString());
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

    @SuppressWarnings("incomplete-switch")
    @Override
    public void performTask()
    {
        switch (task)
        {
            case GENERATEATOMLABELS:
                generateAtomLabels();
                break;
        }

        if (exposedOutputCollector != null)
        {
/*
//TODO
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Define atom labels for all structures found in the structures from
     * the input file.
     */
    
    public void generateAtomLabels()
    {
        if (inFile.equals("noInFile"))
        {
            Terminator.withMsgAndStatus("ERROR! Missing input file parameter. "
                + " Cannot generate atom labels.", -1);
        }

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                IAtomContainer mol = sdfItr.next();
                List<String> txt = generateAtomLabels(mol);
                if (verbosity > 1)
                {
                	System.out.println(StringUtils.mergeListToString(txt, 
                			labelsSeparator));
                }
                if (outFile!=null)
                {
                	IOtools.writeTXTAppend(outFile, 
                			StringUtils.mergeListToString(txt, labelsSeparator)
                				+ System.getProperty("line.separator"),
                			true);
                }
            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
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
	private static List<String> generateElementBasedLabels(IAtomContainer iac,
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
	private static List<String> generateIndexBasedLabels(IAtomContainer iac,
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

//-----------------------------------------------------------------------------

}
