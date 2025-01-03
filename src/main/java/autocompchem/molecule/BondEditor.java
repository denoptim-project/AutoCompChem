package autocompchem.molecule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.run.Job;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.smarts.SMARTSUtils;
import autocompchem.utils.NumberUtils;
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
     * List of atom-matching rules for definition of the bonds to edit and 
     * the relevant attributes.
     */
    private List<BondEditingRule> rules = new ArrayList<BondEditingRule>();
    
    /**
     * Unique identifier for rules
     */
	protected AtomicInteger ruleID = new AtomicInteger(0);

    /**
     * List (with string identifier) of smarts queries to identify target bonds.
     */
    private Map<String,List<SMARTS>> smarts = new HashMap<String,List<SMARTS>>();

    /**
     * List (with string identifier) of feature for target bond
     */
    private Map<String,Object> editorObjectives = new HashMap<String,Object>();
    
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
    
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "BondEditRule-";
   
    /**
     * Keyword used to identify the imposed bond order value.
     */
    public static final String KEYORDER = "ORDER";
   
    /**
     * Keyword used to identify the stereochemistry descriptor value.
     */
    public static final String KEYSTEREO = "STEREO";
   
    /**
     * Value-less keyword used to identify bonds to remove.
     */
    public static final String KEYREMOVE = "REMOVE";
    
	/**
	 * Keywords that expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/BondEditor.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYORDER, KEYSTEREO);

	/**
	 * Keywords that do not expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/BondEditor.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList(
			KEYREMOVE);

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
        	parseBondEditingRules(all);
        	for (BondEditingRule bmRule : rules)
        	{
        		smarts.put(bmRule.getRefName(), bmRule.getSMARTS());
        		Object objective = bmRule.getObjective();
        		if (objective!=null)
        			editorObjectives.put(bmRule.getRefName(), objective);
        	}
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link BondEditingRule} and adds
     * the resulting rules to this instance.
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link BondEditingRule}s.
     */

    protected void parseBondEditingRules(String text)
    {
    	// NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        parseBondEditingRules(new ArrayList<String>(Arrays.asList(arr)));
    }
    
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link BondEditingRule} and adds
     * the resulting rules to this instance.
     * @param lines the lines of text to be parsed into 
     * {@link BondEditingRule}s.
     */

    protected void parseBondEditingRules(List<String> lines)
    {
        for (String line : lines)
        {
            rules.add(new BondEditingRule(line, ruleID.getAndIncrement()));
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Class adapting the general functionality of {@link AtomTupleMatchingRule}
     * to parse SMARTS-based definition of bond editing task
     */
    private class BondEditingRule extends AtomTupleMatchingRule
    {

    //--------------------------------------------------------------------------

        /**
         * Constructor for a rule by parsing a formatted string of text. 
         * Default keywords that are interpreted to parse specific input
         * instructions are defined by
         * {@link BondEditor#DEFAULTVALUEDKEYS} and 
         * {@link BondEditor#DEFAULTVALUELESSKEYS}.
         * @param txt the string to be parsed
         * @param i a unique integer used to identify the rule. Is used to build
         * the reference name of the generated rule.
         */

        public BondEditingRule(String txt, int i)
        {
        	super(txt, BASENAME+i, DEFAULTVALUEDKEYS, DEFAULTVALUELESSKEYS, 
        			true);
        }
        
    //--------------------------------------------------------------------------
        
        /**
         * @return the objective of the editing task, i.e., the intended result 
         * on the bonds matched by this rule, or null, if no known objective is 
         * associated to this rule.
         */
        public Object getObjective()
        {
        	String bndOrderObjective = getValueOfAttribute(KEYORDER);
        	if (bndOrderObjective!=null)
        	{
        		if (NumberUtils.isParsableToInt(bndOrderObjective))
        		{
            		return MolecularUtils.intToBondOrder(
            				Integer.parseInt(bndOrderObjective));
        		} else {
        			return IBond.Order.valueOf(bndOrderObjective.toUpperCase());
        		}
        	}
        	
        	String stereoDscrpObjective = getValueOfAttribute(KEYSTEREO);
        	if (stereoDscrpObjective!=null)
        	{
        		return IBond.Stereo.valueOf(stereoDscrpObjective.toUpperCase());
        	}
        	
        	if (hasValuelessAttribute(KEYREMOVE))
        	{
        		return KEYREMOVE;
        	}
        	
        	return null;
        }

    //--------------------------------------------------------------------------

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
      		editBonds(iac, smarts, editorObjectives);
      	} else {
      		dealWithTaskMismatch();
        }
      	return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Edit bonds in a container.
     * @param iac the container to edit.
     * @param smarts the SMARTS matching the bonds to edit, or the pair of 
     * atoms between which to define a bond (if none is already present).
     * @param editorObjectives map defining what to do or edit in the matches 
     * bonds.
     */

    public static void editBonds(IAtomContainer iac, 
    		Map<String,List<SMARTS>> smarts, 
    		Map<String,Object> editorObjectives)
    {
    	Logger logger = LogManager.getLogger();
    	
    	Map<String,List<SMARTS>> atomPairSmarts = 
    			new HashMap<String,List<SMARTS>>();
    	Map<String,SMARTS> bondSmarts = new HashMap<String,SMARTS>();
    	for (Entry<String, List<SMARTS>> e : smarts.entrySet())
    	{
    		if (e.getValue().size()>1)
    			atomPairSmarts.put(e.getKey(), e.getValue());
    		else
    			bondSmarts.put(e.getKey(), e.getValue().get(0));
    	}

    	// Add new bonds, if not already present
    	if (atomPairSmarts.size()>0)
    	{
	    	Map<String, List<MatchingIdxs>> matchesPerRule = 
	    			SMARTSUtils.identifyAtomIdxTuples(iac, atomPairSmarts);
	    	for (String key : matchesPerRule.keySet())
	  		{
	    		IBond.Order bo = IBond.Order.SINGLE;
	    		if (editorObjectives.containsKey(key) 
	    				&& editorObjectives.get(key) instanceof IBond.Order)
	    		{
	    			bo = (IBond.Order) editorObjectives.get(key);
	    		}
	    		List<MatchingIdxs> matchesThisRule = matchesPerRule.get(key);
	    		if (matchesThisRule.size() != 2)
	    		{
	    			logger.warn("Match of SMARTS tuple '" + key 
	    					+ "' is not a pair (is " + matchesThisRule.size()
	    					+ "-tuple: " + matchesThisRule + "). Skipped.");
	    			continue;
	    		}
	    		for (List<Integer> idxGroupA : matchesThisRule.get(0))
	    		{
	    			int idxA = idxGroupA.get(0);
	    			IAtom atmA = iac.getAtom(idxA);
	    			for (List<Integer> idxGroupB : matchesThisRule.get(1))
		    		{
		    			int idxB = idxGroupB.get(0);
	    				if (idxA==idxB)
	    					continue;
		    			IAtom atmB = iac.getAtom(idxB);
		    			if (!iac.getConnectedAtomsList(atmA).contains(atmB))
		    			{
		    				IBond bnd = new Bond(atmA, atmB, bo);
		    				iac.addBond(bnd);
		    			}
		    		}
	    		}
	  		}
    	}
    	
    	if (bondSmarts.size()>0)
    	{
	    	// Alter previously existing bonds
	    	Map<String,List<IBond>> targets = 
	    			SMARTSUtils.identifyBondsBySMARTS(iac, bondSmarts);
	
			List<IBond> toRemove = new ArrayList<IBond>();
	  		for (String key : targets.keySet())
	  		{
	  			if (!editorObjectives.containsKey(key))
	  				continue;
	  			Object objective = editorObjectives.get(key);
	  			if (objective instanceof IBond.Order) {
	  				for (IBond bnd : targets.get(key))
	  				{
	  					bnd.setOrder((IBond.Order) objective);
	  				}
	  			} else if (objective instanceof IBond.Stereo) {
	  				for (IBond bnd : targets.get(key))
	  				{
	  					bnd.setStereo((IBond.Stereo) objective);
	  				}
	  			} else if (
	  					(objective.toString().toUpperCase().equals("REMOVE")) 
	  					||
	  					(objective.toString().toUpperCase().equals("DELETE"))){
	  				toRemove.addAll(targets.get(key));
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
    }

//------------------------------------------------------------------------------

}
