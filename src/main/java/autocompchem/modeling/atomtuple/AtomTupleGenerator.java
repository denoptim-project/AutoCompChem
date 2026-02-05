package autocompchem.modeling.atomtuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.modeling.AtomLabelsGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule.RuleType;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Job;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.smarts.SMARTSUtils;
import autocompchem.utils.ListOfListsCombinations;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool to generate tuples (i.e., ordered lists) of atom pointers/identifiers 
 * decorated by attributes,
 * i.e., {@link AnnotatedAtomTuple}.
 * The rules on which atoms to collect, and in which order, and on what 
 * attributes to associate with the tuple of pointers are all defined in 
 * {@link AtomTupleMatchingRule}s.
 * 
 * @author Marco Foscato
 */

public class AtomTupleGenerator extends AtomContainerInputProcessor
{
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

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String GENERATEATOMSETSTASKNAME = "generateAtomSets";

    /**
     * Task about generating tuples of atoms
     */
    public static final Task GENERATEATOMSETSTASK;
    static {
    	GENERATEATOMSETSTASK = Task.make(GENERATEATOMSETSTASKNAME);
    }
    
    public enum Mode {
    	TUPLES,
    	SETS
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
             Arrays.asList(GENERATEATOMTUPLESTASK,
            		 GENERATEATOMSETSTASK)));
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

    public void initialize()
    {
		super.initialize();
        
        if (params.contains(AtomTupleConstants.KEYRULENAMEROOT))
        {
        	ruleRoot = params.getParameter(AtomTupleConstants.KEYRULENAMEROOT)
        			.getValueAsString();
        }
        
        if (params.contains(AtomTupleConstants.KEYVALUEDKEYWORDS))
        {
        	valuedKeywords.addAll(Arrays.asList(
        			params.getParameter(AtomTupleConstants.KEYVALUEDKEYWORDS)
        			.getValueAsString()
        			.trim().toUpperCase().split("\\s+")));
        }

        if (params.contains(AtomTupleConstants.KEYBOOLEANKEYWORDS))
        {
        	valuelessKeywords.addAll(Arrays.asList(
        			params.getParameter(AtomTupleConstants.KEYBOOLEANKEYWORDS)
        			.getValueAsString()
        			.trim().toUpperCase().split("\\s+")));
        }

        if (params.contains(AtomTupleConstants.KEYRULETYPESMARTS))
        {
        	String all = params.getParameter(AtomTupleConstants.KEYRULETYPESMARTS)
        			.getValueAsString();
        	parseAtomTupleMatchingRules(all);
        }
        
        if (params.contains(AtomTupleConstants.KEYRULETYPEATOMIDS))
        {
        	String all = params.getParameter(AtomTupleConstants.KEYRULETYPEATOMIDS)
        			.getValueAsString();
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
    	processInput();
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

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GENERATEATOMTUPLESTASK) ||
    			task.equals(GENERATEATOMSETSTASK))
    	{
	        // If needed, generate atom labels
	    	List<String> labels = null;
	        for (AtomTupleMatchingRule r : rules)
	        {
	    		if (r.hasValuelessAttribute(AtomTupleConstants.KEYGETATOMLABELS))
	    		{
	    	    	ParameterStorage labMakerParams = params.copy();
	    			labels = generateAtomLabels(iac, labMakerParams);
	    			break;
	    		}
	        }
	        
	        Mode mode = Mode.TUPLES;
	        if (task.equals(GENERATEATOMSETSTASK))
	        {
	        	mode = Mode.SETS;
	        }
	        
	        // Now generate the tuple/sets, possibly passing the atom labels data
	        List<AnnotatedAtomTuple> tuples = createTuples(iac, rules, labels,
	        		mode);
	        logger.debug(StringUtils.mergeListToString(tuples, 
	        			System.getProperty("line.separator")));
	        AnnotatedAtomTupleList aatl = new AnnotatedAtomTupleList();
	        if (tuples.size()>0)
	        {
		        aatl= new AnnotatedAtomTupleList(tuples);
	        }
	        
            if (outFile!=null)
            {
            	outFileAlreadyUsed = true;
            	StringBuilder sb = new StringBuilder();
	    		int jj = 0;
	    		for (AnnotatedAtomTuple aat : aatl)
	    		{
	    			jj++;
	    			sb.append("mol-").append(i).append("_")
	    				.append(mode.toString().toLowerCase())
	    				.append("-").append(jj)
	    				.append(": ").append(aat)
	    				.append(System.getProperty("line.separator"));
	    		}
            	IOtools.writeTXTAppend(outFile, sb.toString(), true);
            }
	        
	        if (exposedOutputCollector != null)
	    	{
				String molID = "mol-"+i;
		        exposeOutputData(new NamedData(task.ID + "_" + molID, aatl));
	    	}
		} else {
			dealWithTaskMismatch();
	    }
    	return iac;
    }
        
//------------------------------------------------------------------------------
    
	/**
	 * Runs a child task for generating the atom labels according
	 * to the given parameters.
	 * @param iac the container of atoms for which to generate the labels.
	 * @param labMakerParams parameters controlling the generation of the 
	 * labels. Note that these will be edited to force the {@link Task} to be 
	 * {@link AtomLabelsGenerator#GENERATEATOMLABELSTASK}.
	 * @return the list of labels, one per atom, ordered according to the list 
	 * of atoms in the container.
	 */
    protected static List<String> generateAtomLabels(IAtomContainer iac, 
    		ParameterStorage labMakerParams)
    {
		labMakerParams.setParameter(WorkerConstants.PARTASK, 
				AtomLabelsGenerator.GENERATEATOMLABELSTASK.ID);
		labMakerParams.removeData(WorkerConstants.PAROUTFILE);
		labMakerParams.setParameter(WorkerConstants.PARNOOUTFILEMODE);
		AtomLabelsGenerator labGenerator;
		try {
			labGenerator = (AtomLabelsGenerator) 
					WorkerFactory.createWorker(labMakerParams, null);
		} catch (ClassNotFoundException e) {
			// Cannot happen... unless there is very serious bug!
			throw new IllegalStateException("Unable to create worker for "
					+ "atom labels generation task.", e);
		}
		return labGenerator.generateAtomLabels(iac);
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
    	return createTuples(mol, rules, null, Mode.TUPLES);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define annotated atom tuples for the given atom container using the given  
     * set of tuple-defining rules.
     * @param mol the atom container for which we want to create atom tuples.
     * @param rules the tuple-defining rules to apply.
     * @param labels atom labels. Ignored if <code>null</code>.
     * @param mode defines how to combine atoms to generated tuples or sets.
     * In TUPLES mode, the atoms are collected in combinations of 
     * ordered and fixed-size lists.
     * In SETS mode, the atoms are collapsed into a single list that does not 
     * include duplicates.
     * @return the list of tuples.
     */
    public static List<AnnotatedAtomTuple> createTuples(IAtomContainer mol,
    		List<AtomTupleMatchingRule> rules, List<String> labels, Mode mode)
    {
		// If needed, identify continuously connected sets of atoms, i.e., molcules
		// withing the atom container
		Map<Integer,List<IAtom>> molecules = null;
		if (rules.stream().anyMatch(r -> r.hasValuelessAttribute(
			AtomTupleConstants.KEYONLYINTERMOLECULAR)))
		{
			molecules = ConnectivityUtils.identifyConnectedFrags(mol);
		}

    	List<AnnotatedAtomTuple> result = new ArrayList<AnnotatedAtomTuple>();
    	
    	//Here we collect all atom tuples (without annotation) by the name of
    	// the matching AtomTupleMatchingRule (below called MR for brevity)
        Map<String,List<MatchingIdxs>> allIDsForEachMR =
                new HashMap<String,List<MatchingIdxs>>();
    	
        //Handling differs for SMARTS- and ID-based rules
    	Set<String> sortedKeys = new TreeSet<String>();
    	Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();
    	Map<String,List<SMARTS>> mergedSmarts = 
    			new HashMap<String,List<SMARTS>>();
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
            		smarts.put(refName, s);
            	}
            	mergedSmarts.put(r.getRefName(), r.getSMARTS());
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
        	Map<String,List<MatchingIdxs>> matchesFromMR = 
        			SMARTSUtils.identifyAtomIdxTuples(mol, mergedSmarts);
        	allIDsForEachMR.putAll(matchesFromMR);
        }
        
        //Define annotated tuples/sets according to the matched atom
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
            
            // The behavior in presence of a single multi-atom SMARTS is 
            // that of an ordered list of single-atom SMARTS
            boolean useMultiAtomMatches = false;
            if (atmIdxsForMR.size()==1)
            {
	            for (MatchingIdxs mIdxs : atmIdxsForMR)
	            {
	            	if (mIdxs.hasMultiCenterMatches())
	            	{
	            		useMultiAtomMatches = true;
	            		break;
	            	}
	            }
            }
            
            // Then we choose what to iterate over when making atom tuples
            Iterator<List<IAtom>> iter = null;
            switch (mode) {
	            case TUPLES:
	            {
	            	if (useMultiAtomMatches)
	                {
	                    List<List<IAtom>> atmsForMR = new ArrayList<List<IAtom>>();
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
	                    List<List<IAtom>> atmsForMR = new ArrayList<List<IAtom>>();
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
	            	break;
	            }
	            
	            case SETS:
	            {
	            	// We will iterate over a single item, hence the wrapper
	            	List<List<IAtom>> wrapper = new ArrayList<List<IAtom>>();
	            	List<IAtom> allMatchedAtoms = new ArrayList<IAtom>();
    	            for (MatchingIdxs mIdxs : atmIdxsForMR)
    	            {
    	            	for (List<Integer> lst : mIdxs)
    	            	{
    	            		for (Integer idx : lst)
    	            		{
    	            			IAtom atm = mol.getAtom(idx);
    	            			if (!allMatchedAtoms.contains(atm))
    	            				allMatchedAtoms.add(atm);
    	            		}
    	            	}
    	            }
	            	wrapper.add(allMatchedAtoms);
    	            iter = wrapper.iterator();
    	            break;
	            }
	            
	            default:
	                throw new IllegalStateException(
	                		"Unrecognized mode of action '" + mode + "'.");
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

				if (r.hasValuelessAttribute(AtomTupleConstants.KEYONLYINTERMOLECULAR))
				{
					if (molecules != null && belongToSameList(atoms, molecules.values()))
					{
						continue;
					}
				}
        		
        		if (labels !=null && r.hasValuelessAttribute(
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

    
//------------------------------------------------------------------------------

	private static boolean belongToSameList(List<IAtom> atms, 
		Collection<List<IAtom>> lists)
	{
		for (List<IAtom> lst : lists)
		{
			if (lst.containsAll(atms))
				return true;
		}
		return false;
	}

//-----------------------------------------------------------------------------

}
