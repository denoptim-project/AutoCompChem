package autocompchem.molecule;


/*   
 *   Copyright (C) 2024  Marco Foscato 
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.molecule.connectivity.BondEditingRule;
import autocompchem.run.Job;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.smarts.SMARTSUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to add, modify, or remove one or more bonds.
 * 
 * @author Marco Foscato
 */


public class BondEditor extends AtomContainerInputProcessor
{
    
    /**
     * Unique identifier for bond-matching rules
     */
	protected AtomicInteger ruleID = new AtomicInteger(0);

    /**
     * List (with string identifier) of rules defining hoe to edit bonds.
     */
    private Map<String,BondEditingRule> bondEditingrules = 
    		new HashMap<String,BondEditingRule>();
    
    /**
     * String defining the task of editing bonds
     */
    public static final String EDITBONDSTASKNAME = "editBonds";

    /**
     * Task about editing bonds
     */
    public static final Task EDITBONDSTASK;
    static {
    	EDITBONDSTASK = Task.make(EDITBONDSTASKNAME);
    }


//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public BondEditor()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(EDITBONDSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/BondEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new BondEditor();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters set for this worker.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        //Get the list of SMARTS to be matched
        if (params.contains("SMARTS"))
        {
        	String all = params.getParameter("SMARTS").getValueAsString();
        	parseRules(all);
        }
        
        //Get the list of bonds by atom Index
        if (params.contains("ATOMIDS"))
        {
        	String all = params.getParameter("ATOMIDS").getValueAsString();
        	parseRules(all);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link BondEditingRuled} and adds
     * the resulting rules to this instance.
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link BondEditingRuled}s.
     */

    protected void parseRules(String text)
    {
    	// NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        parseRules(new ArrayList<String>(Arrays.asList(arr)));
    }
    
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link BondEditingRuled} and adds
     * the resulting rules to this instance.
     * @param lines the lines of text to be parsed into 
     * {@link BondEditingRuled}s.
     */

    protected void parseRules(List<String> lines)
    {
        for (String line : lines)
        {
        	BondEditingRule bmRule = new BondEditingRule(line, 
        			ruleID.getAndIncrement());
        	bondEditingrules.put(bmRule.getRefName(), bmRule);
        }
    }
    
//------------------------------------------------------------------------------

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
      	if (task.equals(EDITBONDSTASK))
      	{
      		editBonds(iac, bondEditingrules);
      	} else {
      		dealWithTaskMismatch();
        }
      	return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Edit bonds in a container.
     * @param iac the container to edit.
     * @param bondEditingrules the list of rules defining how to edit bonds.
     */

    public static void editBonds(IAtomContainer iac, 
    		Map<String,BondEditingRule> bondEditingrules)
    {
    	Logger logger = LogManager.getLogger();
    	
    	// Atom indexed may result both from explicit atom IDs or from SMARTS
    	Map<String, List<MatchingIdxs>> atmIDMatchesPerRule = 
    			new HashMap<String, List<MatchingIdxs>>();
    	
    	// SMARTS may be for atoms that are disconnected or for bonds
    	Map<String,List<SMARTS>> atomPairSmarts = 
    			new HashMap<String,List<SMARTS>>();
    	Map<String,SMARTS> bondSmarts = new HashMap<String,SMARTS>();
    	for (Entry<String,BondEditingRule> e : bondEditingrules.entrySet())
    	{
    		String bmRuleName = e.getKey();
    		BondEditingRule bmRule = e.getValue();
    		List<SMARTS> smarts = bmRule.getSMARTS();
    		if (smarts!=null)
    		{
    			// This is  a SMARTS-based rule
	    		if (smarts.size()>1)
	    			atomPairSmarts.put(bmRuleName, smarts);
	    		else
	    			bondSmarts.put(bmRuleName, smarts.get(0));
    		} else {
    			// This is a rule based on atom indexes
    			List<MatchingIdxs> pair = new ArrayList<MatchingIdxs>();
    			for (Integer id : bmRule.getAtomIDs())
    			{
    				// each atom as a single substructure
        			MatchingIdxs ids = new MatchingIdxs();
    				ids.add(Arrays.asList(id));
    				pair.add(ids);
    			}
    			atmIDMatchesPerRule.put(bmRuleName, pair);
    		}
    	}
    			
    	// Get atom IDs from SMARTS
    	if (atomPairSmarts.size()>0)
    	{
    		atmIDMatchesPerRule.putAll(
	    			SMARTSUtils.identifyAtomIdxTuples(iac, atomPairSmarts));
    	}
    	
    	// Get existing bonds batched by SMARTS
    	Map<String,List<IBond>> targetBonds = new HashMap<String,List<IBond>>();
    	if (bondSmarts.size()>0)
    	{
	    	targetBonds = SMARTSUtils.identifyBondsBySMARTS(iac, bondSmarts);
    	}
    	
    	// Add any bond that should be added
    	for (String bmRuleName : atmIDMatchesPerRule.keySet())
  		{
    		IBond.Order bo = IBond.Order.SINGLE;
    		if (bondEditingrules.containsKey(bmRuleName) 
    				&& bondEditingrules.get(bmRuleName).getObjective() 
    				instanceof IBond.Order)
    		{
    			bo = (IBond.Order) bondEditingrules.get(bmRuleName).getObjective();
    		}
    		List<MatchingIdxs> matchesThisRule = atmIDMatchesPerRule.get(bmRuleName);
    		if (matchesThisRule.size() != 2)
    		{
    			logger.warn("Match of bond editing rule '" + bmRuleName 
    					+ "' is not a pair (is " + matchesThisRule.size()
    					+ "-tuple: " + matchesThisRule + "). Skipped.");
    			continue;
    		}
    		for (List<Integer> idxGroupA : matchesThisRule.get(0))
    		{
    			int idxA = idxGroupA.get(0);
    			if (idxA<0 || idxA>(iac.getAtomCount()-1))
    			{
    				logger.warn("WARNING: Ignoring atom index out of range (" 
    						+ idxA+ ")");
    				continue;
    			}
    			IAtom atmA = iac.getAtom(idxA);
    			for (List<Integer> idxGroupB : matchesThisRule.get(1))
	    		{
	    			int idxB = idxGroupB.get(0);
    				if (idxA==idxB)
    					continue;
        			if (idxB<0 || idxB>(iac.getAtomCount()-1))
        			{
        				logger.warn("WARNING: Ignoring atom index out of range "
        						+ "(" + idxB+ ")");
        				continue;
        			}
	    			IAtom atmB = iac.getAtom(idxB);
	    			if (!iac.getConnectedAtomsList(atmA).contains(atmB))
	    			{
	    				// if the bond does not exist we make it
	    				IBond bnd = new Bond(atmA, atmB, bo);
	    				iac.addBond(bnd);
	    			} else {
	    				// if it exist we add it to the list of bonds to process
	    				if (targetBonds.containsKey(bmRuleName))
	    				{
	    					targetBonds.get(bmRuleName).add(iac.getBond(atmA, atmB));
	    				} else {
	    					List<IBond> bnds = Arrays.asList(iac.getBond(atmA, atmB));
	    					targetBonds.put(bmRuleName, bnds);
	    				}
	    			}
	    		}
    		}
  		}
    	
		List<IBond> toRemove = new ArrayList<IBond>();
  		for (String key : targetBonds.keySet())
  		{
  			if (!bondEditingrules.containsKey(key))
  				continue;
  			Object objective = bondEditingrules.get(key).getObjective();
  			if (objective instanceof IBond.Order) {
  				for (IBond bnd : targetBonds.get(key))
  				{
  					bnd.setOrder((IBond.Order) objective);
  				}
  			} else if (objective instanceof IBond.Stereo) {
  				for (IBond bnd : targetBonds.get(key))
  				{
  					bnd.setStereo((IBond.Stereo) objective);
  				}
  			} else if (
  					(objective.toString().toUpperCase().equals("REMOVE")) 
  					||
  					(objective.toString().toUpperCase().equals("DELETE"))){
  				toRemove.addAll(targetBonds.get(key));
  			} else {
  				logger.warn("No implementation is available for editing "
  						+ "bond feature to '" + objective + "'.");
  			}
  		}
  		
  		for (IBond bnd : toRemove)
  		{
  			iac.removeBond(bnd);
  		}
    }

//------------------------------------------------------------------------------

}
