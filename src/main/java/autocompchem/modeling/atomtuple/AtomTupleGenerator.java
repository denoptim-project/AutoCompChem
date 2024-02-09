package autocompchem.modeling.atomtuple;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.IValueContainer;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.AtomLabelsGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule.RuleType;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.ListOfListsCombinations;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool to generate list of atom pointers (i.e., tuple) decorated by attributes,
 * i.e., {@link AnnotatedAtomTuple}.
 * The rules on which atoms to collect, and in which order, and on what 
 * attributes to associate with the tuple of pointers are all defined in 
 * {@link AtomTupleMatchingRule}s.
 * 
 * @author Marco Foscato
 */

public class AtomTupleGenerator extends Worker
{
    
    /**
     * The input file (molecular structure files)
     */
    protected File inFile;

    /**
     * The input molecules
     */
    protected List<IAtomContainer> inMols;

    /**
     * List of atom-matching rules for definition of the tuples and of their 
     * attributes.
     */
    protected List<AtomTupleMatchingRule> rules = 
    		new ArrayList<AtomTupleMatchingRule>();
    
    /**
     * String used as root for any atom tuple matching rule defined in this 
     * instance.
     */
    protected String ruleRoot = "AtmMatcher-";
    
	/**
	 * Keywords that expect values and are used to annotate tuples.
	 */
	protected List<String> valuedKeywords = new ArrayList<String>();

	/**
	 * Keywords that do not expect values and are used to annotate tuples.
	 */
	protected List<String> valuelessKeywords = new ArrayList<String>();
    
    /**
     * Verbosity level
     */
    protected int verbosity = 0;

    /**
     * Unique identifier for rules
     */
	protected AtomicInteger ruleID = new AtomicInteger(0);

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String GENERATEATOMTUPLESTASKNAME = "generateAtomTuples";

    /**
     * Task about generating tuples of atoms
     */
    public static final Task GENERATEATOMTUPLESTASK;
    static {
    	GENERATEATOMTUPLESTASK = Task.make(GENERATEATOMTUPLESTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public AtomTupleGenerator()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATEATOMTUPLESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomTupleGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomTupleGenerator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

	@Override
    public void initialize()
    {
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
        if (params.contains(ChemSoftConstants.PARGEOM))
        {
            this.inMols = (List<IAtomContainer>) params.getParameter(
            		ChemSoftConstants.PARGEOM).getValue();
            if (params.contains("INFILE"))
            {
            	//TODO: logging
            	System.out.println("WARNING: found both INFILE and "
            			+ ChemSoftConstants.PARGEOM + ". Using geometries from "
            			+ ChemSoftConstants.PARGEOM + " as input for "
            			+ this.getClass().getSimpleName() + ".");
            	this.inFile = null;
            }
        }
        
        if (params.contains("RULENAMEROOT"))
        {
        	ruleRoot = params.getParameter("RULENAMEROOT").getValueAsString();
        }
        
        if (params.contains("VALUEDKEYWORDS"))
        {
        	valuedKeywords.addAll(Arrays.asList(
        			params.getParameter("VALUEDKEYWORDS").getValueAsString()
        			.trim().toUpperCase().split("\\s+")));
        }

        if (params.contains("BOOLEANKEYWORDS"))
        {
        	valuelessKeywords.addAll(Arrays.asList(
        			params.getParameter("BOOLEANKEYWORDS").getValueAsString()
        			.trim().toUpperCase().split("\\s+")));
        }

        if (params.contains("SMARTS"))
        {
        	String all = params.getParameter("SMARTS").getValueAsString();
        	parseAtomTupleMatchingRules(all);
        }
        
        if (params.contains("ATOMIDS"))
        {
        	String all = params.getParameter("ATOMIDS").getValueAsString();
        	parseAtomTupleMatchingRules(all);
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
    	if (task.equals(GENERATEATOMTUPLESTASK))
    	{
    		createTuples();
    	} else {
    		dealWithTaskMistMatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link AtomTupleMatchingRule} and adds
     * the resulting rules to this instance of atom tuple generator. Uses the
     * list of keywords defined in this instance.
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link AtomTupleMatchingRule}s.
     */

    public void parseAtomTupleMatchingRules(String text)
    {
    	// NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        parseAtomTupleMatchingRules(new ArrayList<String>(Arrays.asList(arr)));
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link AtomTupleMatchingRule} and adds
     * the resulting rules to this instance of atom tuple generator. Uses the
     * list of keywords defined in this instance.
     * @param lines the lines of text to be parsed into 
     * {@link AtomTupleMatchingRule}s.
     */

    public void parseAtomTupleMatchingRules(List<String> lines)
    {
        for (String line : lines)
        {
            rules.add(new AtomTupleMatchingRule(line, ruleRoot 
            		+ ruleID.getAndIncrement(), valuedKeywords, 
            		valuelessKeywords));
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the rules used by this instance to generate. Ignores the list of
     * keywords defined in this instance. WARNING! This allows to create
     * inconsistency between this class and the atom tuple matching rules is
     * contains! Use with caution.
     * {@link AnnotatedAtomTuple}s. Replaces previously set rules.
     * @param rules the rules to import.
     */
    public void setAtomTupleMatchingRules(List<AtomTupleMatchingRule> rules)
    {
    	this.rules = rules;
    }

//------------------------------------------------------------------------------

    /**
     * Define {@link AnnotatedAtomTuple}s for all structures found in the input.
     * Uses tuple-defining rules available in this instance (i.e., either
     * created upon initialization or set by 
     * {@link #setAtomTupleMatchingRules(List)}.
     */
    public void createTuples()
    {
        if (inFile==null && inMols==null)
        {
            Terminator.withMsgAndStatus("ERROR! Missing parameter defining the "
            		+ "input geometries (" + ChemSoftConstants.PARGEOM + ") or "
            		+ "an input file to read geometries from. "
            		+ "Cannot generate atom tuple.",-1);
        }

        List<AnnotatedAtomTupleList> output = 
        		new ArrayList<AnnotatedAtomTupleList>();
        
        if (inFile!=null)
        {
	        try {
	            SDFIterator sdfItr = new SDFIterator(inFile);
	            while (sdfItr.hasNext())
	            {
	                IAtomContainer mol = sdfItr.next();
	        		processOneAtomContainer(mol, output);
	            }
	            sdfItr.close();
	        } catch (Throwable t) {
	            t.printStackTrace();
	            Terminator.withMsgAndStatus("ERROR! Exception returned by "
	                + "SDFIterator while reading " + inFile, -1);
	        }
        } else {
        	for (IAtomContainer mol : inMols)
        	{
        		processOneAtomContainer(mol, output);
        	}
        } 

        if (exposedOutputCollector != null)
    	{
	    	int ii = 0;
	    	for (AnnotatedAtomTupleList tuples : output)
	    	{
	    		ii++;
	    		if (tuples != null)
	    		{
	    			String molID = "mol-"+ii;
	  		        exposeOutputData(new NamedData(
	  		        		GENERATEATOMTUPLESTASK.ID + "_" + molID, 
	  		        		NamedDataType.ANNOTATEDATOMTUPLELIST, tuples));
	    		}
	    	}
    	}
    }
    
//------------------------------------------------------------------------------
    
    private void processOneAtomContainer(IAtomContainer iac,
    		List<AnnotatedAtomTupleList> output)
    {
        // If needed, generate atom labels
    	List<String> labels = null;
        for (AtomTupleMatchingRule r : rules)
        {
    		if (r.hasValuelessAttribute(AtomTupleConstants.KEYGETATOMLABELS))
    		{
    			labels = generateAtomLabels(iac);
    			break;
    		}
        }
        
        // Now generate the tuple, possibly passing the atom labels data
        List<AnnotatedAtomTuple> tuples = createTuples(iac, rules, labels);
        if (verbosity > 0)
        {
        	System.out.println("# " + MolecularUtils.getNameOrID(iac));
        	System.out.println(StringUtils.mergeListToString(tuples, 
        			System.getProperty("line.separator")));
        }
        AnnotatedAtomTupleList aatl= new AnnotatedAtomTupleList(tuples);
        output.add(aatl);
    }
        
//------------------------------------------------------------------------------
    
    private List<String> generateAtomLabels(IAtomContainer iac)
    {
    	ParameterStorage labMakerParams = params.clone();
		labMakerParams.setParameter(WorkerConstants.PARTASK, 
				Task.getExisting("generateAtomLabels").ID);
		AtomLabelsGenerator labGenerator = null;
		try {
			labGenerator = (AtomLabelsGenerator) 
					WorkerFactory.createWorker(labMakerParams, myJob);
		} catch (ClassNotFoundException e) {
			// Cannot happen... unless there is very serious bug!
			e.printStackTrace();
		}
		return labGenerator.generateAtomLabels(iac);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define annotated atom tuples for the given atom container using the
     *  tuple-defining rules present in this instance.
     * @param mol the atom container for which we want to create atom tuples.
     * @return the list of tuples
     */
    public List<AnnotatedAtomTuple> createTuples(IAtomContainer mol)
    {
    	//TODO-gg get rid of this method! it's obsolete
    	return createTuples(mol, rules, null);
    }

//------------------------------------------------------------------------------

    /**
     * Define annotated atom tuples for the given atom container using the given  
     * set of tuple-defining rules.
     * @param mol the atom container for which we want to create atom tuples.
     * @param rules the tuple-defining rules to apply.
     * @return the list of tuples.
     */
    public static List<AnnotatedAtomTuple> createTuples(IAtomContainer mol,
    		List<AtomTupleMatchingRule> rules)
    {
    	return createTuples(mol, rules, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define annotated atom tuples for the given atom container using the given  
     * set of tuple-defining rules.
     * @param mol the atom container for which we want to create atom tuples.
     * @param rules the tuple-defining rules to apply.
     * @param labels atom labels. Ignored if <code>null</code>.
     * @return the list of tuples.
     */
    public static List<AnnotatedAtomTuple> createTuples(IAtomContainer mol,
    		List<AtomTupleMatchingRule> rules, List<String> labels)
    {
    	List<AnnotatedAtomTuple> result = new ArrayList<AnnotatedAtomTuple>();
    	
    	//Here we collect all atom tuples (without annotation) by the name of
    	// the matching AtomTupleMatchingRule (below called MR for brevity)
        Map<String,List<MatchingIdxs>> allIDsForEachMR =
                new HashMap<String,List<MatchingIdxs>>();
    	
        //Handling differs for SMARTS- and ID-based rules
    	Set<String> sortedKeys = new TreeSet<String>();
    	Map<String,String> smarts = new HashMap<String,String>();
        for (AtomTupleMatchingRule r : rules)
        {
        	// For SMARTS-based we need to inspect the structure
            if (r.getType() == RuleType.SMARTS)
            {
            	for (int i=0; i<r.getSMARTS().size(); i++)
            	{
            		SMARTS s = r.getSMARTS().get(i);
            		
            		//NB: this format is assumed here and elsewhere
            		String refName = r.getRefName()+"_"+i;
            		sortedKeys.add(r.getRefName());
            		smarts.put(refName, s.getString());
            	}
            }
            // For ID-based we just collect the atoms
            else if (r.getType() == RuleType.ID)
            {
            	List<MatchingIdxs> nestedList = new ArrayList<MatchingIdxs>();
            	for (Integer atmId : r.getAtomIDs())
            	{
            		MatchingIdxs mps = new MatchingIdxs();
            		mps.add(Arrays.asList(atmId));
            		nestedList.add(mps);
            	}
            	allIDsForEachMR.put(r.getRefName(), nestedList);
            }
        }
        
        //Add non-annotated atom tuples according to the SMARTS queries
        // present in each AtomTupleMatchingRule (below called MR for brevity)
        if (smarts.keySet().size()>0)
        {
        	//First apply all SMARTS in once, for the sake of efficiency
	        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts, 0);
	        if (msq.hasProblems())
	        {
	            String cause = msq.getMessage();
	            Terminator.withMsgAndStatus("ERROR! " +cause, -1);
	        }
	        
	        //Get matches grouped by the ref names of SMARTS queries
	        Map<String,MatchingIdxs> groupedByRule = new HashMap<String,MatchingIdxs>();
	        for (String rulRef : smarts.keySet())
	        {
	            if (msq.getNumMatchesOfQuery(rulRef) == 0)
	            {
	                continue;
	            }
	            groupedByRule.put(rulRef, msq.getMatchingIdxsOfSMARTS(rulRef));
	        }

            // Collect matches that belong to same AtomTupleMatchingRule (MR)
            for (String key : sortedKeys)
            {
                List<String> smartsRefNamesForMR = new ArrayList<String>();
                for (String k2 : groupedByRule.keySet())
                {
                    if (k2.toUpperCase().startsWith(key.toUpperCase()))
                    {
                    	smartsRefNamesForMR.add(k2);
                    }
                }
                boolean allComponentsMatched = true;;
                List<MatchingIdxs> atmsForMR = new ArrayList<MatchingIdxs>();
                for (int ig = 0; ig<smartsRefNamesForMR.size(); ig++)
                {
                	//NB: here we assume the format of the SMARTS ref names
                    String k2qry = key + "_" + Integer.toString(ig);
                    if (groupedByRule.containsKey(k2qry))
                    {
                    	atmsForMR.add(groupedByRule.get(k2qry));
                    } else {
                    	allComponentsMatched = false;
                    }
                }
                if (allComponentsMatched)
                {
                	allIDsForEachMR.put(key, atmsForMR);
                }
            }
        }
        
        //Define annotated tuples according to the matched atom
        for (AtomTupleMatchingRule r : rules)
        {
        	String key = r.getRefName();
        	if (!allIDsForEachMR.containsKey(key))
        		continue;
        	
            List<MatchingIdxs> atmIdxsForMR = allIDsForEachMR.get(key);
            if (atmIdxsForMR.size() == 0)
            {
            	continue;
            }
            if (r.getType() == RuleType.SMARTS &&
            		r.getSMARTS().size() != atmIdxsForMR.size())
            {
            	//There are SMARTS that were not matched
            	continue;
            }
            
            // Allow multi-atom SMARTS to behave as an ordered list of 
            // single-atom SMARTS. However, we cannot mix the two approaches.
            boolean useMultiAtomMAtches = false;
            for (MatchingIdxs mIdxs : atmIdxsForMR)
            {
            	if (mIdxs.hasMultiCenterMatches())
            	{
            		useMultiAtomMAtches = true;
            		break;
            	}
            }
            // Then we choose what to iterate over according to whether we 
            // have single-/multi-atom matches
            Iterator<List<IAtom>> iter = null;
            List<List<IAtom>> atmsForMR = new ArrayList<List<IAtom>>();
            if (useMultiAtomMAtches)
            {
            	if (atmIdxsForMR.size()>1)
            	{
            		throw new IllegalArgumentException("ERROR! Only one "
            				+ "multi-atom SMARTS can be used. Found multiple"
            				+ "ones in rule '" + key + "'.");
            	}
            	for (List<Integer> multiAtomLst : atmIdxsForMR.get(0))
            	{
            		List<IAtom> atoms = new ArrayList<IAtom>();
            		for (Integer idx : multiAtomLst)
            		{
            			atoms.add(mol.getAtom(idx));
            		}
	            	atmsForMR.add(atoms);
            	}
            	iter = atmsForMR.iterator();
            } else {
	            for (MatchingIdxs mIdxs : atmIdxsForMR)
	            {
	            	List<IAtom> atoms = new ArrayList<IAtom>();
	            	for (List<Integer> lst : mIdxs)
	            	{
	            		for (Integer idx : lst)
	            			atoms.add(mol.getAtom(idx));
	            	}
	            	atmsForMR.add(atoms);
	            }
	            iter = new ListOfListsCombinations<IAtom>(atmsForMR);
            }
        	while (iter.hasNext())
        	{
        		List<IAtom> atoms = iter.next();
        		
        		Set<IAtom> uniqueAtoms = new HashSet<IAtom>(atoms);
        		if (atoms.size() != uniqueAtoms.size())
        			continue;
        		
        		AnnotatedAtomTuple tuple = r.makeAtomTupleFromIDs(atoms, mol);
        		
        		if (r.hasValuelessAttribute(AtomTupleConstants.KEYONLYBONDED))
        		{
        			boolean isLinearlyConnected = true;
        			for (int iAtm=1; iAtm<atoms.size(); iAtm++)
        			{
        				IBond bndToPrevious = mol.getBond(atoms.get(iAtm-1), 
        						atoms.get(iAtm));
        				if (bndToPrevious==null)
        				{
        					isLinearlyConnected = false;
        					break;
        				}
        			}
        			if (!isLinearlyConnected)
        				continue;
        		}
        		
        		if (r.hasValuelessAttribute(
        				AtomTupleConstants.KEYGETATOMLABELS))
        		{
        			List<String> atmLabels = new ArrayList<String>();
        			for (IAtom atm : atoms)
        			{
        				atmLabels.add(labels.get(mol.indexOf(atm)));
        			}
        			tuple.setAtmLabels(atmLabels);
        		}
        		
        		if (r.hasValuelessAttribute(
        				AtomTupleConstants.KEYUSECURRENTVALUE))
        		{
            		Double value = null;
        			switch (atoms.size())
        			{
        			case 2:
        				value = MolecularUtils.calculateInteratomicDistance(
        						atoms.get(0), atoms.get(1));
        				break;
        			case 3:
        				value = MolecularUtils.calculateBondAngle(
        						atoms.get(0), atoms.get(1), atoms.get(2));
        				break;
        			case 4:
        				value = MolecularUtils.calculateTorsionAngle(
        						atoms.get(0), atoms.get(1), atoms.get(2),
        						atoms.get(3));
        				break;
        			}
        			if (value!=null)
        			{
        				tuple.setValueOfAttribute(
        						AtomTupleConstants.KEYCURRENTVALUE, 
        						value.toString());
        			}
        		}
        		
        		result.add(tuple);
        	}
        }
        return result;
    }

//-----------------------------------------------------------------------------

}
