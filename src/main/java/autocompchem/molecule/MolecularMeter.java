package autocompchem.molecule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.geometry.GeomDescriptor;
import autocompchem.molecule.geometry.GeomDescriptorDefinition;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * MolecularMeter collects measuring tools for molecular quantities
 * such as inter atomic distances, angles, and dihedral angles. Quantities
 * can be defined from SMARTS queries or atom indexes.
 * 
 * @author Marco Foscato
 */


public class MolecularMeter extends AtomTupleGenerator
{   
    /**
     * File to the file where to write the descriptors computed
     */
    protected File outTxtFile = null;
    
    /**
     * Flag controlling whether we save the descriptor values into atom 
     * container properties
     */
    private boolean saveDescriptorsAsProperties = false;
    
    /**
     * String defining the task of measuring geometric descriptors
     */
    public static final String MEASUREGEOMDESCRIPTORSTASKNAME = 
    		"measureGeomDescriptors";

    /**
     * Task about measuring geometric descriptors
     */
    public static final Task MEASUREGEOMDESCRIPTORSTASK;
    static {
    	MEASUREGEOMDESCRIPTORSTASK = Task.make(MEASUREGEOMDESCRIPTORSTASKNAME);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public MolecularMeter()
    {}
    	
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(MEASUREGEOMDESCRIPTORSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularMeter.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularMeter();
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link GeomDescriptorDefinition} and adds
     * the resulting rules to this instance of atom tuple generator.
     * @param lines the lines of text to be parsed into 
     * {@link GeomDescriptorDefinition}s.
     */

    @Override
    public void parseAtomTupleMatchingRules(List<String> lines)
    {
        for (String line : lines)
        {
            rules.add(new GeomDescriptorDefinition(line, ruleID.getAndIncrement()));
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */
    
	@Override
	public void initialize() 
	{
		// Can ignore OUTFILE as it is normally not needed, but may be used
		params.setParameter(WorkerConstants.PARNOOUTFILEMODE);
		super.initialize();
        
        if (params.contains("SAVEASPROPERTIES"))
        {
        	String value = (String) params.getParameter("SAVEASPROPERTIES").getValue();
        	System.out.println("value: "+value);
        	saveDescriptorsAsProperties = StringUtils.parseBoolean(value, true);
        }
        
        if (params.contains(WorkerConstants.PAROUTDATAFILE))
        {
	        this.outTxtFile = getNewFile(params.getParameter(
	        		WorkerConstants.PAROUTDATAFILE).getValueAsString());
	        FileUtils.mustNotExist(this.outTxtFile);
        }
    }
    
//-----------------------------------------------------------------------------
  	
    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */
    
	@Override
  	public void performTask() 
    {
		processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(MEASUREGEOMDESCRIPTORSTASK))
    	{
    		Map<String,List<Double>> descriptors = measureAllQuantities(iac, i, rules);
    		
    		if (saveDescriptorsAsProperties)
    		{
    			for (Map.Entry<String,List<Double>> e : descriptors.entrySet())
	    		{
            		iac.setProperty(e.getKey(), StringUtils.mergeListToString(
            				e.getValue(), " "));
	    		}
    		}
    		
    		if (outTxtFile!=null)
            {
            	StringBuilder sb = new StringBuilder();
            	for (String descRef : descriptors.keySet())
	    		{
            		sb.append("mol-").append(i).append("_").append(descRef)
    					.append(": ").append(descriptors.get(descRef))
	    				.append(System.getProperty("line.separator"));
	    		}
            	IOtools.writeTXTAppend(outTxtFile, sb.toString(), true);
            }
    		
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
	    		for (String descRef : descriptors.keySet())
	    		{
	    			String reference = molID + "_" + descRef;
	  		        exposeOutputData(new NamedData(reference,
	  		        		new ListOfDoubles(descriptors.get(descRef))));
	    		}
        	}
    	} else {
    		dealWithTaskMismatch();
        }
    	return iac;
  	}
    
//------------------------------------------------------------------------------

    /**
     * Measure all geometric descriptors in a given molecule and using a given
     * set of geometric descriptor defining rules.
     * @param mol the molecular system we measure geometric descriptors for.
     * @param rules the set of geometric descriptor defining rules to apply.
     * @return the map of geometric descriptors and their values.
     */

    public static Map<String,List<Double>> measureAllQuantities(
    		IAtomContainer iac, int i, List<AtomTupleMatchingRule> rules)
    {
    	Logger logger = LogManager.getLogger(MolecularMeter.class);
    	
    	String molName = MolecularUtils.getNameOrID(iac);
    	
        Map<String,List<Double>> results = new HashMap<String,List<Double>>();

        List<AnnotatedAtomTuple> tuples = createTuples(iac, rules);
        for (AnnotatedAtomTuple tuple : tuples)
        {
            GeomDescriptor descriptor = new GeomDescriptor(tuple);

            List<Integer> ids = descriptor.getAtomIDs();
            String type = "none";
            if (ids.size() == 2)
            {
                type = "Dst.";
            }
            else if (ids.size() == 3)
            {
                type = "Ang.";
            }
            else if (ids.size() == 4)
            {
                type = "Dih.";
            } else {
                Terminator.withMsgAndStatus("ERROR! Unexpected number of atoms ("
                    + ids.size() + ") for quantity '" + descriptor.getName() 
                    + "'.", -1);
            }
            
            String strRes = "Mol." + i + " "
                + molName + " " + type;
            if (descriptor.getName()!=null)
            	strRes += descriptor.getName();
            strRes += " ";
            for (int j=0; j<ids.size(); j++)
            {
            	if (j>0)
            		strRes += ":";
                strRes += MolecularUtils.getAtomRef(iac.getAtom(ids.get(j)),iac);
            }
            strRes += " = " + descriptor.getValue();

            logger.info(strRes);

            if (results.containsKey(descriptor.getName()))
            {
                results.get(descriptor.getName()).add(descriptor.getValue());
            }
            else
            {
            	List<Double> values = new ArrayList<Double>();
            	values.add(descriptor.getValue());
                results.put(descriptor.getName(), values);
            }
        }
        return results;
    }
    
//------------------------------------------------------------------------------
	
}
