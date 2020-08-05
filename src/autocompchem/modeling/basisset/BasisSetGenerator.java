package autocompchem.modeling.basisset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;


/**
 * BasisSetGenerator implements the facility to generate basis sets for 
 * specific atom types according to a
 * given list of matching rules. The type of implemented rules and their
 * syntax is defined in the {@link BSMatchingRule} documentation page.
 * Parameters:
 * <ul>
 * <li>
 * <b>{@value autocompchem.modeling.basisset.BasisSetConstants#ATMSPECBS}</b> 
 * is used to define the 
 * atom-matching rules for assigning a specific basis set to one or more
 * atoms in the system. One rule for each line. 
 * The syntax of each line is defined in the
 * {@link autocompchem.modeling.basisset.BSMatchingRule}, and that for defining
 * a multi line {@link autocompchem.datacollections.Parameter} in the
 * {@link autocompchem.datacollections.ParameterStorage}.
 * </li>
 * <li> (optional)
 * <b>{@value autocompchem.modeling.basisset.BasisSetConstants#ALLOWPARTIALMATCH}</b>
 * allows to tolerate partial basis set that do not cover all atoms in the
 * molecular structure. Any value other than <code>true</code> 
 * will forbid partial matches.
 * </li>
 * <li>
 * (optional) <b>VERBOSITY</b> verbosity level.
 * </li>
 * <li>
 * (optional) <b>INFILE</b> name of a molecular structure file containing
 * molecule/s to be processed.
 * </li>
 * <li>
 * (optional) <b>OUTFILE</b> name of the output file on which the basis set
 * will be written.
 * </li>
 * <li>
 * (optional) <b>FORMAT</b> is used to specify the format used when
 * returning or writing the resulting basis set.
 * </li>
 * </ul>
 * @author Marco Foscato
 */


public class BasisSetGenerator extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.GENERATEBASISSET)));
    
    /**
     * The name of the input file (molecular structure files)
     */
    private String inFile = "noInFile";

    /**
     * The name of the output file, if any
     */
    private String outFile = "";

    /**
     * Format for reporting basis set (Default: Gaussian)
     */
    private String format = "GAUSSIAN";

    /**
     * List of atom-matching rules for assignation of the basis sets
     */
    private Map<String,BSMatchingRule> rules = 
                                           new HashMap<String,BSMatchingRule>();

    /**
     * Storage of imported basis sets 
     */
    private Map<String,BasisSet> importedBSs = new HashMap<String,BasisSet>();

    /**
     * Flag setting use of atom index as ID
     */
    private boolean atmIdxAsId = false;    

    /**
     * Flag setting tolerance for partial matches
     */
    private boolean allowPartial = false;

    /**
     * Verbosity level
     */
    private int verbosity = 0;


//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
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
            this.inFile = params.getParameter("INFILE").getValue().toString();
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
            this.outFile = 
                        params.getParameter("OUTFILE").getValue().toString();
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
     * has been initialised.
     */

    @SuppressWarnings("incomplete-switch")
    @Override
    public void performTask()
    {
        switch (task)
          {
          case GENERATEBASISSET:
        	  assignBasisSetToAllMolsInFile();
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

//------------------------------------------------------------------------------

    /**
     * Sets the use of atom index (1-based position in the list of atoms) as
     * ID of centers (i.e., atoms).
     * @param atmIdxAsId the new value of the flag: if <code>true</code>
     * makes the generator use atom indexes as IDs.
     */
 
    public void setAtmIdxAsId(boolean atmIdxAsId)
    {
        this.atmIdxAsId = atmIdxAsId;
    } 

//------------------------------------------------------------------------------

    /**
     * Sets the rules for assigning the basis sets to each atom/element
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link BSMatchingRule}s.
     */

    public void setBSMatchingRules(String text)
    {
        String[] arr = text.split(System.getProperty("line.separator"));
        setBSMatchingRules(new ArrayList<String>(Arrays.asList(arr)));
    }

//------------------------------------------------------------------------------

    /**
     * Sets the rules for assigning the basis sets to each atom/element
     * @param lines the lines of text to be parsed into {@link BSMatchingRule}s
     */

    public void setBSMatchingRules(ArrayList<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            BSMatchingRule bsr = new BSMatchingRule(lines.get(i),i);
            this.rules.put(bsr.getRefName(),bsr);
        }
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
                BasisSetUtils.writeFormattedBS(bs,format,outFile);
                //writeBSRefNamesToOut(mol);

            } //end loop over molecules

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
            System.out.println(" SMARTS for basis set assignation: " + smarts);
            System.out.println(" Elemental symbols for basis set assignation: "
                                                                      + elmnts);
        }

        // Apply SMARTS-bases ruled 
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
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
                importBasisSetFromFile(rulRef,rules.get(rulRef).getSource());
            }

            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(rulRef);
            for (List<Integer> innerList : allMatches)
            {
                for (Integer iAtm : innerList)
                {
                    IAtom atm = mol.getAtom(iAtm);
                    addBSAssignationRuleReferenceToAtom(atm,rulRef,mol);
                }
            }
        }

        //Verify that all atoms have got a basis set
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
        // Elemental symbol-based rules as wildcards
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
                        importBasisSetFromFile(rulRef,rule.getSource());
                    }
                    BasisSet impBS = importedBSs.get(rulRef);
    
                    if (rule.getKey().equals("*"))
                    {
                        for (CenterBasisSet c : impBS.getAllCenterBSs())
                        {        
                            // NB: the center Id is case insensitive: use upper!
                            if (MolecularUtils.containsElement(mol,
                                                               c.getCenterId()))
                            {
                                globBS.getCenterBasisSetForCenter(
                                           c.getCenterId()).appendComponents(c);
                            }
                        }
                    }
                    else
                    {
                        // NB: the center Id is case insensitive: use upper!
                        if (!impBS.hasCenter(elSymb.toUpperCase()))
                        {
                            String msg = "ERROR! Basis set file imported for "
                                         + "rule '" + rulRef 
                                         + "' does not contains element '"
                                         + elSymb + "'. Exiting.";
                        Terminator.withMsgAndStatus(msg,-1);
                        }
    
                        // NB: the center Id is case insensitive: use upper!
                        CenterBasisSet cbs = impBS.getCenterBasisSetForCenter(
                                                          elSymb.toUpperCase());
                        globBS.getCenterBasisSetForCenter(
                                       cbs.getCenterId()).appendComponents(cbs);
                    }
                    break;

                case BasisSetConstants.BSSOURCENAME:
                    CenterBasisSet cbs = new CenterBasisSet(elSymb);
                    cbs.addNamedComponent(rule.getSource());
                    globBS.getCenterBasisSetForCenter(
                                       cbs.getCenterId()).appendComponents(cbs);
                    break;
            }
        }

        // Now add all atom-specific components from SMARTS-based rules
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            // Generate atom tag
            String elSymb = atm.getSymbol();
            String atmId = elSymb + (i+1);
            if (atmIdxAsId)
            {
                atmId = String.valueOf(i+1);
            }
            // Use previously set atom tag, if any
            if (AtomUtils.hasProperty(atm,ACCConstants.ATMTAGPROP))
            {
                atmId = 
                       atm.getProperty(ACCConstants.ATMTAGPROP).toString();
            }

            CenterBasisSet globCBS = new CenterBasisSet();
            if (atm.getProperty(BasisSetConstants.BSATMPROP+"0") != null)
            {
                globCBS = globBS.getCenterBasisSetForCenter(atmId);
            }

            Map<Object,Object> allProps = atm.getProperties();
            for (Object key : allProps.keySet())
            {
                String ks = key.toString();
                if (!ks.toUpperCase().startsWith(BasisSetConstants.BSATMPROP))
                {
                    continue;
                }
                String rulRef = allProps.get(key).toString();
                switch (rules.get(rulRef).getSourceType().toUpperCase())
                {
                    case BasisSetConstants.BSSOURCELINK:
                        BasisSet impBS = importedBSs.get(rulRef);
                        // NB: the center Id is case insensitive: use upper!
                        if (!impBS.hasCenter(elSymb.toUpperCase()))
                        {
                            String msg = "ERROR! Basis set file imported for "
                                         + "rule '" + rulRef 
                                         + "' does not contains element '"
                                         + elSymb + "'. Exiting.";
                            Terminator.withMsgAndStatus(msg,-1);
                        }
                        CenterBasisSet locCBS = 
                         impBS.getCenterBasisSetForCenter(elSymb.toUpperCase());
                        globCBS.appendComponents(locCBS);
                        break;

                    case BasisSetConstants.BSSOURCENAME:
                        globCBS.addNamedComponent(rules.get(
                                                           rulRef).getSource());
                        break;
                }
            }
        }

        return globBS;
    }

//------------------------------------------------------------------------------

    /**
     * Stored the basis set reference (i.e., the name of the atom mathing rule)
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

    private void importBasisSetFromFile(String bsName, String src)
    {
        //TODO for now only GCB files can be imported
        BasisSet bs = BasisSetUtils.importBasisSetFromGBSFile(src,verbosity);
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
