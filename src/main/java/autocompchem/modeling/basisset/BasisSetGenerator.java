package autocompchem.modeling.basisset;

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

import autocompchem.atom.AtomUtils;
import autocompchem.constants.ACCConstants;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * BasisSetGenerator implements the facility to generate basis sets for 
 * specific atom types according to a
 * given list of matching rules. The type of implemented rules and their
 * syntax is defined in the {@link BSMatchingRule} documentation page.
 * @author Marco Foscato
 */


public class BasisSetGenerator extends Worker
{
    
    /**
     * The input file (molecular structure files)
     */
    private File inFile;

    /**
     * The output file, if any
     */
    private File outFile;

    /**
     * Format for reporting basis set (Default: Gaussian)
     */
    private String format = "GAUSSIAN";

    /**
     * List of atom-matching rules for assigning the basis sets
     */
    private Map<String,BSMatchingRule> rules = 
    		new HashMap<String,BSMatchingRule>();

    /**
     * Storage of imported basis sets 
     */
    private Map<String,BasisSet> importedBSs = new HashMap<String,BasisSet>();  

    /**
     * Flag setting tolerance for partial matches
     */
    private boolean allowPartial = false;
    
    /**
     * String defining the task of generating a basis set
     */
    public static final String GENERATEBASISSETTASKNAME = "generateBasisSet";
    
    /**v
     * Task about generation of basis set
     */
    public static final Task GENERATEBASISSETTASK;
    static {
    	GENERATEBASISSETTASK = Task.make(GENERATEBASISSETTASKNAME);
    }
    
    /**
     * Verbosity level
     */
    private int verbosity = 0;


//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public BasisSetGenerator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATEBASISSETTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/BasisSetGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new BasisSetGenerator();
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
            String v = params.getParameter("VERBOSITY").getValue().toString();
            this.verbosity = Integer.parseInt(v);
        }

        if (verbosity > 0)
            System.out.println(" Adding parameters to BasisSetGenerator");

        // Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            this.inFile = new File(
            		params.getParameter("INFILE").getValue().toString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        // Read the atom type matching rules
        setBSMatchingRules(params.getParameter(
                            BasisSetConstants.ATMSPECBS).getValue().toString());

        // Allow partial matches?
        if (params.contains(BasisSetConstants.ALLOWPARTIALMATCH))
        {
            String v = params.getParameter(
                     BasisSetConstants.ALLOWPARTIALMATCH).getValue().toString();
            if (v.trim().toUpperCase().equals("TRUE"))
            {
                this.allowPartial = true;
            }
        }

        // Name of output file
        if (params.contains("OUTFILE"))
        {
            //Get and check output file
            this.outFile = new File(
                        params.getParameter("OUTFILE").getValue().toString());
            FileUtils.mustNotExist(this.outFile);
        }

        // Format with which to report basis sets
        if (params.contains("FORMAT"))
        {
            this.format = params.getParameter("FORMAT").getValue().toString();
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
    	if (task.equals(GENERATEBASISSETTASK))
    	{
    		assignBasisSetToAllMolsInFile();
    	//} else if (task.equals(Task.getExisting(?)))
        } else {
        	Terminator.withMsgAndStatus("ERROR! Task '" + task + "' is not "
        			+ "linked to any method in " 
        			+ this.getClass().getSimpleName() + ".", -1);
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

//------------------------------------------------------------------------------

    /**
     * Sets the rules for assigning the basis sets to each atom/element
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link BSMatchingRule}s.
     */

    public void setBSMatchingRules(String text)
    {
        // NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        setBSMatchingRules(new ArrayList<String>(Arrays.asList(arr)));
    }

//------------------------------------------------------------------------------

    /**
     * Sets the rules for assigning the basis sets to each atom/element
     * @param lines the lines of text to be parsed into {@link BSMatchingRule}s
     */

    public void setBSMatchingRules(List<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            BSMatchingRule bsr = new BSMatchingRule(lines.get(i),i);
            this.rules.put(bsr.getRefName(), bsr);
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Adds a center-matching rule to assign a basis set to centers matching
     * the given rule.
     * @param bsr the rule to add.
     */
    public void addBSMatchingRule(BSMatchingRule bsr)
    {
    	this.rules.put(bsr.getRefName(), bsr);
    }

//------------------------------------------------------------------------------

    /**
     * Assign basis set to all atoms in all structures found in the input and
     * output according to the parameters given to constructor. This
     * method is meant for working on structures taken from an input file.
     */

    public void assignBasisSetToAllMolsInFile()
    {
        if (inFile.equals("noInFile"))
        {
            Terminator.withMsgAndStatus("ERROR! Missing input file parameter. "
                + " Cannot generate basis set.",-1);
        }

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Assign Basis Set
                BasisSet bs = assignBasisSet(mol);

                //Write to output
                BasisSetUtils.writeFormattedBS(bs, format, outFile);
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
     * Assign basis set to all atoms in the molecule
     * @param mol the molecular system to work with
     * @return the basis set
     */

    public BasisSet assignBasisSet(IAtomContainer mol)
    {
    	// Reset any previous basis set assigned to atoms on this atom container
    	for (IAtom atm : mol.atoms())
    	{
    		Set<Object> keysToRemove = new HashSet<Object>();
    		for (Object k : atm.getProperties().keySet())
    		{
    			if (k.toString().toUpperCase().startsWith(
    					BasisSetConstants.BSATMPROP))
    			{
    				keysToRemove.add(k);
    			}
    		}
    		for (Object k : keysToRemove)
    		{
    			atm.removeProperty(k);
    		}
    	}
    	
        //Split atom matching rules based on type
        Map<String,String> smarts = new HashMap<String,String>();
        Map<String,String> elmnts = new HashMap<String,String>();
        for (String refName : rules.keySet())
        {
            BSMatchingRule r = rules.get(refName);
            if (r.getType().toUpperCase().equals(
                                            BasisSetConstants.ATMMATCHBYSMARTS))
            {
                smarts.put(refName,r.getKey());
            }
            else if (r.getType().toUpperCase().equals(
                                            BasisSetConstants.ATMMATCHBYSYMBOL))
            {
                elmnts.put(refName,r.getKey());
            }
            else 
            {
                Terminator.withMsgAndStatus("ERROR! Unknown basis set "
                                 + "assignation rule '" + r.getType() + ".",-1);
            }
        }
        if (verbosity > 0)
        {
            System.out.println(" SMARTS rules for basis set assignation: " 
            		+ smarts);
            System.out.println(" Elemental symbols rules for basis set "
            		+ "assignation: " + elmnts);
        }

        // Apply SMARTS-bases ruled 
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts, verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
        }
        for (String rulRef : smarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(rulRef) == 0)
            {
                continue;
            }

            if (rules.get(rulRef).getSourceType().toUpperCase().equals(
                                                BasisSetConstants.BSSOURCELINK))
            {
                importBasisSetFromFile(rulRef, new File(
                		rules.get(rulRef).getSource()));
            }
            
            //Get matches for this SMARTS query
            MatchingIdxs matches =  msq.getMatchingIdxsOfSMARTS(rulRef);
            for (List<Integer> innerList : matches)
            {
            	for (Integer iAtm : innerList)
                {
                    IAtom atm = mol.getAtom(iAtm);
                    addBSAssignationRuleReferenceToAtom(atm, rulRef, mol);
                }
            }
        }

        // Verify that all atoms have got a basis set based on SMARTS, if an
        // atom does not have one, then try to use elemental symbol rules.
        for (IAtom atm : mol.atoms())
        {
            boolean hasAtmSpecBS = false;
            Map<Object,Object> allProps = atm.getProperties();
            for (Object k : allProps.keySet())
            {
                String kStr = k.toString();
                if (kStr.toUpperCase().startsWith(BasisSetConstants.BSATMPROP))
                {
                    hasAtmSpecBS = true;
                    break;
                }
            }
            boolean matchedByElSpecBS = false;
            if (!hasAtmSpecBS)
            {
                for (String rulRef : elmnts.keySet())
                {
                    BSMatchingRule rule = rules.get(rulRef);
                    String elSymb = rule.getKey();
                    if (atm.getSymbol().equals(elSymb) || elSymb.equals("*"))
                    {
                        matchedByElSpecBS = true;
                        break;
                    }
                }
            }
            if (!hasAtmSpecBS && !matchedByElSpecBS && !this.allowPartial)
            {
                String msg = "ERROR! Atom "
                              + MolecularUtils.getAtomRef(atm,mol) + " was not "
                              + "matched by any basis set assignation rule! ";
                Terminator.withMsgAndStatus(msg,-1);
            }
        }

        //Build the global basis set by combining all atm-specific pieces
        BasisSet globBS = new BasisSet();
        
        // Elemental symbol-based rules
        for (String rulRef : elmnts.keySet())
        {
            if (verbosity > 1)
            {
                System.out.println(" Setting basis set from rule '"
                              + rulRef + "' (Element-based rule).");
            }
            BSMatchingRule rule = rules.get(rulRef);
            String elSymb = rule.getKey();
            switch (rule.getSourceType().toUpperCase())
            {
                case BasisSetConstants.BSSOURCELINK:
                    if (!importedBSs.keySet().contains(rulRef))
                    {
                        importBasisSetFromFile(rulRef, new File(rule.getSource()));
                    }
                    BasisSet impBS = importedBSs.get(rulRef);
    
                    if (elSymb.equals("*"))
                    {
                        for (CenterBasisSet c : impBS.getAllCenterBSs())
                        {
                            // NB: the center Id is case insensitive: use upper!
                            if (MolecularUtils.containsElement(mol, c.getElement()))
                            {
                                globBS.getCenterBasisSetForElement(
                                		c.getElement().toUpperCase())
                                	.appendComponents(c);
                            }
                        }
                    } else {
                        if (!impBS.hasElement(elSymb))
                        {
                            String msg = "ERROR! Basis set file imported for "
                                         + "rule '" + rulRef 
                                         + "' does not contains element '"
                                         + elSymb + "'. Exiting.";
                            Terminator.withMsgAndStatus(msg,-1);
                        }
    
                        CenterBasisSet cbs = impBS.getCenterBasisSetForElement(
                        		elSymb.toUpperCase());
                     // NB: the center Id is case insensitive: use upper!
                        globBS.getCenterBasisSetForElement(
                        		elSymb.toUpperCase()).appendComponents(cbs);
                    }
                    break;

                case BasisSetConstants.BSSOURCENAME:
                    CenterBasisSet cbs = new CenterBasisSet(null, null, elSymb);
                    cbs.addNamedComponent(rule.getSource());
                    globBS.getCenterBasisSetForElement(
                    		elSymb).appendComponents(cbs);
                    break;
            }
        }

        // Now add all atom-specific components from SMARTS-based rules
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            // Generate atom tag
            String elSymb = atm.getSymbol();
            int atmId = i;
            String tag = null;
            
            // Use previously set atom tag, if any
            if (AtomUtils.hasProperty(atm, ACCConstants.ATMTAGPROP))
            {
            	tag = atm.getProperty(ACCConstants.ATMTAGPROP).toString();
            }

            CenterBasisSet globCBS = new CenterBasisSet();
            if (atm.getProperty(BasisSetConstants.BSATMPROP+"0") != null)
            {
            	if (tag!=null)
                	globCBS = globBS.getCenterBasisSetForCenter(tag, null, null);
            	else
            		globCBS = globBS.getCenterBasisSetForCenter(null, atmId, elSymb);
            }

            Map<Object,Object> allProps = atm.getProperties();
            for (Object key : allProps.keySet())
            {
                String ks = key.toString();
                if (!ks.toUpperCase().startsWith(BasisSetConstants.BSATMPROP))
                {
                	// this property is not one that defines basis set
                    continue;
                }
                String rulRef = allProps.get(key).toString();
                switch (rules.get(rulRef).getSourceType().toUpperCase())
                {
                    case BasisSetConstants.BSSOURCELINK:
                        BasisSet impBS = importedBSs.get(rulRef);
                        // NB: the center Id is case insensitive: use upper!
                        if (!impBS.hasElement(elSymb))
                        {
                            String msg = "ERROR! Basis set file imported for "
                                         + "rule '" + rulRef 
                                         + "' does not contains element '"
                                         + elSymb + "'. Exiting.";
                            Terminator.withMsgAndStatus(msg,-1);
                        }
                        CenterBasisSet locCBS = impBS.getCenterBasisSetForElement(
                        		elSymb.toUpperCase());
                        globCBS.appendComponents(locCBS);
                        break;

                    case BasisSetConstants.BSSOURCENAME:
                        globCBS.addNamedComponent(rules.get(rulRef).getSource());
                        break;
                }
            }
        }

        return globBS;
    }

//------------------------------------------------------------------------------

    /**
     * Stored the basis set reference (i.e., the name of the atom matching rule)
     * as a property into the given atom
     * @param atm the atom to work with
     * @param rulRef the reference name identifying which basis set component 
     * is to be used for the atom
     * @param mol the molecule
     */

    public void addBSAssignationRuleReferenceToAtom(IAtom atm, String rulRef,
                                                             IAtomContainer mol)
    {
        int iBS = 0;
        Map<Object,Object> allProps = atm.getProperties();
        for (Object key : allProps.keySet())
        {
            String keyStr = key.toString();
            if (keyStr.toUpperCase().startsWith(BasisSetConstants.BSATMPROP))
            {
                if (verbosity > 0)
                {
                    System.out.println(" WARNING! Atom "
                            + MolecularUtils.getAtomRef(atm,mol) 
                            + " matches both SMARTS-based rules '"
                            + rulRef + "' and '"
                            + allProps.get(key) + "'.");
                }
                iBS++;
            }
        }
        String propName = BasisSetConstants.BSATMPROP + iBS;
        atm.setProperty(propName,rulRef);
        if (verbosity > 1)
        {
            System.out.println(" Setting basis set from rule '" 
                              + rulRef + "' to atom "
                              + MolecularUtils.getAtomRef(atm,mol));
        } 
    }

//-----------------------------------------------------------------------------

    /**
     * Imports a basis set from a file and stores it for further use
     * @param bsName the reference name for the imported basis set. Most likely
     * it the name of the rule used to identify the atoms to which this basis
     * set is to be assigned.
     * @param src the pathname of the file from which to import the basis set
     */

    private void importBasisSetFromFile(String bsName, File src)
    {
        //TODO for now only GBS files can be imported
        BasisSet bs = BasisSetUtils.importBasisSetFromGBSFile(src, verbosity);
        importedBSs.put(bsName,bs);
    }

//-----------------------------------------------------------------------------

    /**
     * Writes an output file where for each atom the basis set reference name is
     * reported.
     * @param mol the molecule for which the output table is to be added to the
     * output file
     */

    @SuppressWarnings("unused")
        private void writeBSRefNamesToOut(IAtomContainer mol)
    {
        String allMolStr = "";
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            String s = i + " " + MolecularUtils.getAtomRef(mol.getAtom(i),mol)
                       + " BS-components: ";
            Map<Object,Object> allProps = atm.getProperties();
            int l = s.length();
            String indent = "";
            for (int j=0; j<l; j++)
            {
                indent = indent + " " ;
            }
            boolean first = true;
            for (Object key : allProps.keySet())
            {
                String keyStr = key.toString();
                if (keyStr.toUpperCase().startsWith(
                                                   BasisSetConstants.BSATMPROP))
                {
                    if (first)
                    {
                        first = false;
                        s = s + allProps.get(key).toString();
                    }
                    else
                    {
                        s = s + System.getProperty("line.separator")
                            + indent + allProps.get(key).toString();
                    }
                }
            }
            allMolStr = allMolStr + System.getProperty("line.separator") + s;
        }

        IOtools.writeTXTAppend(outFile,allMolStr,true);
    }

//-----------------------------------------------------------------------------

    /**
     * Return the currently loaded Basis Set assignation rules.
     * @return the list of currently loaded basis set assignation rules.
     */

    public Map<String,BSMatchingRule> getBSMatchingRules()
    {
        return rules;
    }

//-----------------------------------------------------------------------------

}
