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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.run.Job;
import autocompchem.smarts.SMARTS;
import autocompchem.smarts.SMARTSUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to edit or remove one or more atoms.
 * 
 * @author Marco Foscato
 */


public class AtomEditor extends AtomContainerInputProcessor
{
    /**
     * Name of the output file
     */
    private File outFile;
    
    /**
     * List of atom-matching rules for definition of the atoms to edit and 
     * the relevant attributes.
     */
    private List<AtomEditingRule> rules = new ArrayList<AtomEditingRule>();
    
    /**
     * Unique identifier for rules
     */
	protected AtomicInteger ruleID = new AtomicInteger(0);

    /**
     * List (with string identifier) of smarts queries to identify target atoms.
     */
    private Map<String,List<SMARTS>> smarts = new HashMap<String,List<SMARTS>>();

    /**
     * List (with string identifier) of feature for target bond
     */
    private Map<String,Object> editorObjectives = new HashMap<String,Object>();
    
    /**
     * String defining the task of mutating bonds
     */
    public static final String EDITATOMSTASKNAME = "mutateAtoms";

    /**
     * Task about mutating atoms
     */
    public static final Task EDITATOMSTASK;
    static {
    	EDITATOMSTASK = Task.make(EDITATOMSTASKNAME);
    }
    
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "AtomEditRule-";
   
    /**
     * Keyword used to identify the imposed elemental symbols.
     */
    public static final String KEYELEMENT = "ELEMENT";
   
    /**
     * Value-less keyword used to identify atoms to remove.
     */
    public static final String KEYREMOVE = "REMOVE";
    
	/**
	 * Keywords that expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/AtomEditor.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYELEMENT);

	/**
	 * Keywords that do not expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/AtomEditor.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList(
			KEYREMOVE);

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomEditor()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(EDITATOMSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomEditor();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters set for this worker.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        if (params.contains("SMARTS"))
        {
        	String all = params.getParameter("SMARTS").getValueAsString();
        	parseAtomEditingRules(all);
        	for (AtomEditingRule bmRule : rules)
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
     * Parses the formatted text defining {@link AtomEditingRule} and adds
     * the resulting rules to this instance.
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link AtomEditingRule}s.
     */

    protected void parseAtomEditingRules(String text)
    {
    	// NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        parseAtomEditingRules(new ArrayList<String>(Arrays.asList(arr)));
    }
    
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link AtomEditingRule} and adds
     * the resulting rules to this instance.
     * @param lines the lines of text to be parsed into 
     * {@link AtomEditingRule}s.
     */

    protected void parseAtomEditingRules(List<String> lines)
    {
        for (String line : lines)
        {
            rules.add(new AtomEditingRule(line, ruleID.getAndIncrement()));
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Class adapting the general functionality of {@link AtomTupleMatchingRule}
     * to parse SMARTS-based definition of atom editing task
     */
    private class AtomEditingRule extends AtomTupleMatchingRule
    {

    //--------------------------------------------------------------------------

        /**
         * Constructor for a rule by parsing a formatted string of text. 
         * Default keywords that are interpreted to parse specific input
         * instructions are defined by
         * {@link AtomEditor#DEFAULTVALUEDKEYS} and 
         * {@link AtomEditor#DEFAULTVALUELESSKEYS}.
         * @param txt the string to be parsed
         * @param i a unique integer used to identify the rule. Is used to build
         * the reference name of the generated rule.
         */

        public AtomEditingRule(String txt, int i)
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
        	String elementSymbolObjective = getValueOfAttribute(KEYELEMENT);
        	if (elementSymbolObjective!=null)
        	{
        		return elementSymbolObjective;
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
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
      	if (task.equals(EDITATOMSTASK))
      	{
      		editAtoms(iac, smarts, editorObjectives);
            
            if (exposedOutputCollector != null)
            {
            	String molID = "mol-"+i;
		        exposeOutputData(new NamedData(molID, 
		      		NamedDataType.IATOMCONTAINER, iac));
            }
            
            if (outFile!=null)
            {
				IOtools.writeSDFAppend(outFile, iac, true);
            }
      	} else {
      		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Edit atoms in a container.
     * @param iac the container to edit.
     * @param smarts the SMARTS matching the atoms to edit.
     * @param editorObjectives map defining what to do or edit in the matches 
     * atoms.
     */

    public static void editAtoms(IAtomContainer iac, 
    		Map<String,List<SMARTS>> smarts, 
    		Map<String,Object> editorObjectives)
    {
    	Logger logger = LogManager.getLogger();
    	
    	Map<String, List<List<List<IAtom>>>> matchesPerRule = 
    			SMARTSUtils.identifyAtomTuples(iac, smarts);
    	
    	Set<IAtom> toRemove = new HashSet<IAtom>();
    	for (String key : matchesPerRule.keySet())
  		{
    		if (!editorObjectives.containsKey(key))
    		{
    			// Nothing to do for these matched atoms
    			continue;
    		}
  			Object objective = editorObjectives.get(key);
    		
    		for (List<List<IAtom>> matchObj : matchesPerRule.get(key))
    		{
    			if (objective instanceof String)
    			{
        			// NB: assumption that a string objective means 
        			// either change of elemental symbol,
        			// or removal of atom
    				if (objective.toString().toUpperCase().equals(
    	  					KEYREMOVE.toString().toUpperCase())) {
    	  				for (List<IAtom> atms : matchObj)
    	  					for (IAtom atm : atms)
    	  						toRemove.add(atm);
    				} else {
	  				for (List<IAtom> atms : matchObj)
	  					for (IAtom atm : atms)
	  					{
	  						mutateElement(iac, atm, (String) objective);
	  					}
    				}
	  			} else {
	  				logger.warn("No implementation is available for editing "
	  						+ "task '" + objective + "'. Ignoring task.");
	  			}
    		}
    	}
    	
  		for (IAtom atm : toRemove)
  		{
  			iac.removeAtom(atm);
  		}
    }
    
//------------------------------------------------------------------------------

    /**
     * Changes the elemental features of a specific atom in the given atom 
     * container. Features that will be edited are: elemental symbol, 
     * atomic number, exact mass, mass number, natural abundance.
     * @param iac the atom container that contains the atom to edit.
     * @param originalAtom the atom to edit
     * @param newElSymbol the symbol of the new element to use.
     */
    public static void mutateElement(IAtomContainer iac, 
    		IAtom originalAtom, String newElSymbol)
    {
    	Logger logger = LogManager.getLogger();
    	
        IAtom newAtm = new Atom(newElSymbol);
        try
        {
            newAtm = (IAtom) originalAtom.clone();
            IAtom newEl = new Atom(newElSymbol);
            newAtm.setSymbol(newEl.getSymbol());
            newAtm.setAtomicNumber(newEl.getAtomicNumber());
            newAtm.setExactMass(newEl.getExactMass());
            newAtm.setMassNumber(newEl.getMassNumber());
            newAtm.setNaturalAbundance(newEl.getNaturalAbundance());
        }
        catch (CloneNotSupportedException cnse)
        {
            logger.warn("Cannot clone atom " 
            		+ MolecularUtils.getAtomRef(originalAtom, iac) 
            		+ ". Some atom features will be lost upon creation of a "
            		+ "new atom object.");
        }
        
        AtomContainerManipulator.replaceAtomByAtom(iac, originalAtom, newAtm);
    }

//------------------------------------------------------------------------------

}
