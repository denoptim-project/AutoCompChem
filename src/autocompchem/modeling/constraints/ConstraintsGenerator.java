package autocompchem.modeling.constraints;

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
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.constraints.ConstrainDefinition.RuleType;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;


/**
 * Facility to generate geometric constraints
 * given list of matching rules.
 * 
 * @author Marco Foscato
 */


public class ConstraintsGenerator extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.GENERATECONSTRAINTS)));
    
    /**
     * The name of the input file (molecular structure files)
     */
    private String inFile = "noInFile";

    /**
     * List of atom-matching rules for definition of the constraints
     */
    private ArrayList<ConstrainDefinition> rules = 
    		new ArrayList<ConstrainDefinition>();
    
    /**
     * Results
     */
    private ArrayList<ConstraintsSet> output = new ArrayList<ConstraintsSet>();

    /**
     * Verbosity level
     */
    private int verbosity = 0;

    /**
     * Unique identifier for rules
     */
	private AtomicInteger ruleID = new AtomicInteger(0);


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
            String v = params.getParameter("VERBOSITY").getValueAsString();
            this.verbosity = Integer.parseInt(v);
        }

        if (verbosity > 0)
            System.out.println(" Adding parameters to ConstraintstGenerator");

        // Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            this.inFile = params.getParameter("INFILE").getValueAsString();
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        if (params.contains("SMARTS"))
        {
        	String all = params.getParameter("SMARTS").getValueAsString();
        	setConstrainDefinitions(all);
        }
        
        if (params.contains("ATOMIDS"))
        {
        	String all = params.getParameter("ATOMIDS").getValueAsString();
        	setConstrainDefinitions(all);
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
          case GENERATECONSTRAINTS:
        	  createConstrains();
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
     * Sets the rules for defining constraints to each atom/element
     * @param text the text (i.e., multiple lines) to be parsed into 
     * {@link ConstrainDefinition}s.
     */

    public void setConstrainDefinitions(String text)
    {
        String[] arr = text.split(System.getProperty("line.separator"));
        setConstrainDefinitions(new ArrayList<String>(Arrays.asList(arr)));
    }

//------------------------------------------------------------------------------

    /**
     * Sets the rules for defining constraints to each atom/element
     * @param lines the lines of text to be parsed into 
     * {@link ConstrainDefinition}s
     */

    public void setConstrainDefinitions(ArrayList<String> lines)
    {
        for (String line : lines)
        {
            rules.add(new ConstrainDefinition(line,ruleID.getAndIncrement()));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Define constraints for all structures found in the input structures feed.
     * method is meant for working on structures taken from an input file.
     */

    public void createConstrains()
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
                ConstraintsSet cs = createConstraints(mol);
                
                if (verbosity > 1)
                {
                	cs.printAll();
                }
                output.add(cs);
                
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
     * Define constraints in a given molecule and using the currently loaded 
     * constraint defining rules.
     * @param mol the molecular system we create constraints for.
     * @return the set of constraints.
     * @throws Exception  
     */

    public ConstraintsSet createConstraints(IAtomContainer mol) throws Exception 
    {
    	ConstraintsSet cLst = new ConstraintsSet();
    	
        //Collect all SMARTS queries
    	Set<String> sortedKeys = new TreeSet<String>();
    	Map<String,String> smarts = new HashMap<String,String>();
        for (ConstrainDefinition r : rules)
        {
            if (r.getType() == RuleType.SMARTS)
            {
            	for (int i=0; i<r.getSMARTS().size(); i++)
            	{
            		SMARTS s = r.getSMARTS().get(i);
            		
            		//NB: this format is assumed here and elsewhere
            		String refName = r.getRefName()+"_"+i;
            		sortedKeys.add(r.getRefName());
            		smarts.put(refName,s.getString());
            	}
            }
            else if (r.getType() == RuleType.ID)
            {
            	cLst.add(r.makeConstraint());
            }
        }
        
        //Get groups of atom IDs according to the SMARTS-based matching rules
        // defined in the ConstraintDefinition (below called CD for brevity)
        Map<String,ArrayList<ArrayList<IAtom>>> allIDsForEachCD =
                new HashMap<String,ArrayList<ArrayList<IAtom>>>();
        if (smarts.keySet().size()>0)
        {
        	//First apply all SMARTS in once, for the sake of efficiency
	        ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
	        if (msq.hasProblems())
	        {
	            String cause = msq.getMessage();
	            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
	        }
	        
	        //Get matches grouped by the ref names of SMARTS queries
	        Map<String,ArrayList<IAtom>> groupedByRule = 
                    new HashMap<String,ArrayList<IAtom>>();
	        for (String rulRef : smarts.keySet())
	        {
	            if (msq.getNumMatchesOfQuery(rulRef) == 0)
	            {
	                continue;
	            }
	
	            ArrayList<IAtom> atomsMatched = new ArrayList<IAtom>();
	            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(rulRef);
	            for (List<Integer> innerList : allMatches)
	            {
	                for (Integer iAtm : innerList)
	                {
                        IAtom targetAtm = mol.getAtom(iAtm);
                        atomsMatched.add(targetAtm);
	                }
	            }
	            groupedByRule.put(rulRef,atomsMatched);
	        }

            // Collect matches that belong to same ConstraintDefinition (CD)
            for (String key : sortedKeys)
            {
                ArrayList<String> smartsRefNamesForCD = new ArrayList<String>();
                for (String k2 : groupedByRule.keySet())
                {
                    if (k2.toUpperCase().startsWith(key.toUpperCase()))
                    {
                        smartsRefNamesForCD.add(k2);
                    }
                }
                ArrayList<ArrayList<IAtom>> atmsForCD =
                                new ArrayList<ArrayList<IAtom>>();
                for (int ig = 0; ig<smartsRefNamesForCD.size(); ig++)
                {
                	//NB: here we assume the format of the SMARTS ref names
                    String k2qry = key + "_" + Integer.toString(ig);
                    atmsForCD.add(groupedByRule.get(k2qry));
                }
                allIDsForEachCD.put(key,atmsForCD);
            }
           
            if (verbosity>1)
	        {
	            System.out.println("Matches for each CD: ");
	            for (String key : sortedKeys)
	            {
	            	if (!allIDsForEachCD.containsKey(key))
	            		continue;
	            	
	            	String str = " -> "+key+":";
	            	for (ArrayList<IAtom> lst : allIDsForEachCD.get(key))
	            	{
	            		str = str + " [";
	            		boolean first = true;
	            		for (IAtom atm : lst)
	            		{
	            			if (first)
	            			{
	            				str = str + MolecularUtils.getAtomRef(atm, mol);
	            				first = false;
	            			} else {
	
	            				str = str + "," + MolecularUtils.getAtomRef(atm,
	            						mol);
	            			}
	            		}
	            		str = str + "]";
	            	}
	            	System.out.println(str);
	            }
            }
            
            //Define constraints according to the matched atom IDs
            for (ConstrainDefinition r : rules)
            {
            	String key = r.getRefName();
            	
            	if (!allIDsForEachCD.containsKey(key))
            		continue;
            	
                ArrayList<ArrayList<IAtom>> atmsForCD =allIDsForEachCD.get(key);

                if (atmsForCD.size() == 0)
                {
                	continue;
                }
                
                for (IAtom atmA : atmsForCD.get(0))
                {
                	if (atmsForCD.size() == 1)
                	{
                		cLst.add(r.makeConstraintFromIDs(new ArrayList<Integer>(
                				Arrays.asList(mol.indexOf(atmA)))));
                		continue;
                	}
                	
                    for (IAtom atmB : atmsForCD.get(1))
                    {
                        if (atmA.equals(atmB))
                            continue;

                        if (r.limitToBonded() 
                        		&& !mol.getConnectedAtomsList(atmA)
                        		.contains(atmB))
                        {
                            continue;
                        }
                        
                        if (atmsForCD.size() == 2)
                    	{
                    		cLst.add(r.makeConstraintFromIDs(
                    				new ArrayList<Integer>(Arrays.asList(
                    						mol.indexOf(atmA),
                    						mol.indexOf(atmB)))));
                    		continue;
                    	}
                        
                        for (IAtom atmC : atmsForCD.get(2))
                        {
                            if (atmB.equals(atmC))
                                continue;

                            if (atmA.equals(atmC))
                                continue;

                            if (r.limitToBonded() 
                            		&& !mol.getConnectedAtomsList(atmB)
                            		.contains(atmC))
                            {
                                continue;
                            }

                            if (atmsForCD.size() == 3)
                        	{
                        		cLst.add(r.makeConstraintFromIDs(
                        				new ArrayList<Integer>(Arrays.asList(
                        						mol.indexOf(atmA),
                        						mol.indexOf(atmB),
                        						mol.indexOf(atmC)))));
                        		continue;
                        	}
                            
                            for (IAtom atmD : atmsForCD.get(3))
                            {
                                if (atmC.equals(atmD))
                                    continue;
                                if (atmB.equals(atmD))
                                    continue;
                                if (atmA.equals(atmD))
                                    continue;

                                if (r.limitToBonded() 
                                		&& !mol.getConnectedAtomsList(atmC)
                                		.contains(atmD))
                                {
                                    continue;
                                }

                                if (atmsForCD.size() == 4)
                            	{
                            		cLst.add(r.makeConstraintFromIDs(
                            				new ArrayList<Integer>(
                            						Arrays.asList(
                            						mol.indexOf(atmA),
                            						mol.indexOf(atmB),
                            						mol.indexOf(atmC),
                            						mol.indexOf(atmD)))));
                            		continue;
                            	} else {
                            		throw new Exception("Unexpectedly long "
                            				+ "list of atom IDs");
                            	}
                            }
                        }
                    }
                }
            }
        }

        return cLst;
    }

//-----------------------------------------------------------------------------

}
