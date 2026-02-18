package autocompchem.molecule.connectivity;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerComparator;

import autocompchem.atom.AtomUtils;
import autocompchem.constants.ACCConstants;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.SeedAtom;
import autocompchem.molecule.SeedAtomComparator;

/** 
 * Toolbox for molecular connectivity
 *          
 * @author Marco Foscato
 */

public class ConnectivityUtils
{

//------------------------------------------------------------------------------

    /**
     * Given a pair of elements' symbols returns the minimum interatomic 
     * distance for the two to be non-bonded.
     * @param elA element of atom a
     * @param elB element of atom b
     * @param tolerance the as percentage of the sum of their v.d.W. radii
     * @return return the lowest distance
     */

    public static double getMinNonBondedDistance(String elA, String elB, 
                                                               double tolerance)
    {
        double rA = AtomUtils.getVdwRradius(elA);
        double rB = AtomUtils.getVdwRradius(elB);

        double refDist = rA + rB;
        refDist = refDist - (refDist * tolerance);

        return refDist;
    }

//------------------------------------------------------------------------------

    /**
     * Add bond to an atom container according to the connectivity table of a 
     * reference atom container.
     * @param mol the atom container under evaluation
     * @param ref the reference atom container
     * @return <code>true</code> if the operation was possible or 
     * <code>false</code> if <code>mol</code> has too few atoms.
     */

    static public boolean importConnectivityFromReference(IAtomContainer mol, 
                                                          IAtomContainer ref)
    {
        if (mol.getAtomCount() < ref.getAtomCount())
        {
            return false;
        }
        
        mol.removeAllElectronContainers();
        mol.removeAllBonds();
       
        for (IBond bndRef : ref.bonds())
        {
            IBond bndMol = new Bond(
                mol.getAtom(ref.indexOf(bndRef.getAtom(0))),
                mol.getAtom(ref.indexOf(bndRef.getAtom(1))),
                bndRef.getOrder());
            mol.addBond(bndMol);
        }
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Compares given molecule against a reference one by mean of their 
     * connectivity matrices.
     * @param mol the atom container under evaluation
     * @param ref the reference atom container 
     * @param logger logging tool.
     * @return <code>true</code> if the two connectivity matrices are equal
     */

    public static boolean compareWithReference(IAtomContainer mol, 
    		IAtomContainer ref, Logger logger)
    {
        if (mol.getAtomCount() == ref.getAtomCount()) 
        {
            if (mol.getBondCount() == ref.getBondCount())
            {
                for (IAtom sMol : mol.atoms())
                {
                    for (IAtom sRef : ref.atoms())
                    {
                        if (compatibleAtoms(mol,sMol,ref,sRef))
                        {
                            List<IAtom> emptyDoneMol = new ArrayList<IAtom>();
                            List<IAtom> emptyDoneRef = new ArrayList<IAtom>();
                            boolean compTrees = exploreConnectivity(0, mol, 
                            		sMol, emptyDoneMol, ref, sRef, 
                            		emptyDoneRef);
                            if (compTrees)
                            {
                                return true;
                            }
                        }
                    }
                }
                logger.info("No compatible atom tree found!");
            } else {
            	logger.info("Different number of Bonds!");
            }
        } else {
        	logger.info("Different number of atoms!");
        }
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Compares the bond distances within an atom container with the 
     * corresponding ones of a reference container. The connectivity of the
     * reference defines what are the pair of bonded atoms.
     * @param mol the atom container under evaluation.
     * @param ref the reference atom container .
     * @param tolerance the tolerance applied when comparing interatomic 
     * distances.
     * @param logger logging tool.
     * @return <code>true</code> if the two interatomic distances are compatible
     * with the given connectivity matrix (within the given tolerance).
     */

    public static boolean compareBondDistancesWithReference(IAtomContainer mol, 
    		IAtomContainer ref, double tolerance, Logger logger)
    {
    	return compareBondDistancesWithReference(mol, ref, tolerance, logger, 
    			new StringBuffer());
    }
    
//------------------------------------------------------------------------------

    /**
     * Compares the bond distances within an atom container with the 
     * corresponding ones of a reference container. The connectivity of the
     * reference defines what are the pair of bonded atoms.
     * @param mol the atom container under evaluation.
     * @param ref the reference atom container .
     * @param tolerance the tolerance applied when comparing interatomic 
     * distances. The value is a factor applied to the reference distance.
     * @param logger logging tool.
     * @param log a string with the log from this analysis.
     * @return <code>true</code> if the two interatomic distances are compatible
     * with the given connectivity matrix (within the given tolerance).
     */
    
    public static boolean compareBondDistancesWithReference(IAtomContainer mol, 
    		IAtomContainer ref, double tolerance, Logger logger, 
    		StringBuffer log)
    {  
    	String largest = "";
    	double maxDelta = -10000.0;
        if (mol.getAtomCount() == ref.getAtomCount()) 
        {
        	logger.debug("Compatison of bond distances:");
            for (IBond refBnd : ref.bonds())
            {
            	int iA = ref.indexOf(refBnd.getAtom(0));
            	int iB = -1;
            	if (refBnd.getAtomCount() > 1)
            	{
            		iB = ref.indexOf(refBnd.getAtom(1));
            	}
            	if (refBnd.getAtomCount() != 2)
            	{
            		String msg = "WARNING! Comparison of bond distanced "
            				+ "assumes two atoms are involved, but a bond was "
            				+ "found that involves a different number of atoms."
            				+ " Ignoring bond involving "
            				+ MolecularUtils.getAtomRef(refBnd.getAtom(0), ref);
            		for (int i=1; i<refBnd.getAtomCount(); i++)
            		{
            			msg = msg + "-"
            				+ MolecularUtils.getAtomRef(refBnd.getAtom(i), ref);
            		}
            		logger.warn(msg);
            		continue;
            	}
            	double refDist = MolecularUtils.calculateInteratomicDistance(
            			ref.getAtom(iA), ref.getAtom(iB));
            	double molDist = MolecularUtils.calculateInteratomicDistance(
            			mol.getAtom(iA), mol.getAtom(iB));
            	double delta = Math.abs(refDist - molDist);
            	String line = MolecularUtils.getAtomRef(refBnd.getAtom(0),ref)
        				+ "-" 
        				+ MolecularUtils.getAtomRef(refBnd.getAtom(1),ref)
        				+ ": |" + refDist + "-" + molDist + "| = " + delta;
            	logger.debug(line);
            	if (delta > maxDelta)
            	{
            		maxDelta = delta;
            		largest = line;
            	}
            	double maxAllowed = Math.abs(refDist*tolerance);
            	if (delta > maxAllowed)
            	{
            		String msg = "Unacceptable bond distance "
        					+ "deviation: "+ largest + " > " + maxAllowed;
            		logger.info(msg);
            		log.append(msg);
            		return false;
            	}
            }
        } else {
        	String msg = "Different number of atoms!";
        	logger.info(msg);
        	log.append(msg);
        	return false;
        }

        logger.info("Largest deviation in bond distance: " + largest);
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluate compatibility of two atoms according to their
     * element symbol and connected environment (size and element symbols)
     * @param molA first molecule 
     * @param atmA the candidate atom in <code>molA</code>
     * @param molB second molecule
     * @param atmB the candidate atom in <code>molB</code>
     * @return <code>true</code> if the two atoms are compatible
     */

    private static boolean compatibleAtoms(IAtomContainer molA, IAtom atmA, 
                                    IAtomContainer molB, IAtom atmB)
    {
        boolean result = false;

        ArrayList<String> nbrsRefA = new ArrayList<String>();
        ArrayList<String> nbrsRefB = new ArrayList<String>();

        int numFound = 0;
        
        if (atmA.getSymbol().equals(atmB.getSymbol()))
        {
            //Check compatible environment
            List<IAtom> nbrsA = molA.getConnectedAtomsList(atmA);
            List<IAtom> nbrsB = molB.getConnectedAtomsList(atmB);
            if (nbrsA.size() == nbrsB.size())
            {
                ArrayList<IAtom> alreadyUsed = new ArrayList<IAtom>();
                for (IAtom nA : nbrsA)
                {
                    nbrsRefA.add(MolecularUtils.getAtomRef(nA,molA));
                    boolean found = false;
                    for (IAtom nB : nbrsB)
                    {
                        if (alreadyUsed.contains(nB))
                            continue;

                        if (nA.getSymbol().equals(nB.getSymbol()))
                        {
                            numFound++;
                            found = true;
                            alreadyUsed.add(nB);
                            nbrsRefB.add(MolecularUtils.getAtomRef(nB,molB));
                            break;
                        }
                    }
                    if (!found)
                        break;
                }
            }
            if (numFound == nbrsA.size())
            {
                result = true;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    /**
     * Method for exploring the connectivity recursively
     */

    private static boolean exploreConnectivity(int recNum,
                        IAtomContainer mol, IAtom seedMol, List<IAtom> doneMol, 
                        IAtomContainer ref, IAtom seedRef, List<IAtom> doneRef)
    {

        //set string for reporting and debugging
        String recFlag = "";
        for (int ri = 0; ri < recNum; ri++)
             recFlag = recFlag+"-";

        int numCompBranches = 0;
        int numAlreadyDone = 0;
        int numBranches = mol.getConnectedBondsCount(seedMol);

        List<IAtom> nbrsMol = mol.getConnectedAtomsList(seedMol);
        List<IAtom> nbrsRef = ref.getConnectedAtomsList(seedRef);

        //change order or seed atoms
        List<SeedAtom> salist = new ArrayList<SeedAtom>();
        for (IAtom a : nbrsMol)
        {
            SeedAtom sa = new SeedAtom(a,mol);
            salist.add(sa);
        }

        Collections.sort(salist, new SeedAtomComparator());

        List<IAtom> nbrsMolOrdered = new ArrayList<IAtom>();
        for (SeedAtom sa : salist)
        {
            nbrsMolOrdered.add(sa.getAtom());
        }

        for (IAtom tMol : nbrsMolOrdered)
        {
            if (doneMol.contains(tMol))
            {
                numAlreadyDone++;
                continue;
            }

            for (IAtom tRef : nbrsRef)
            {
                if (doneRef.contains(tRef))
                    continue;
                
                if (!compatibleAtoms(mol,tMol,ref,tRef))
                    continue;

                //If you arrive here, tMol and tRef are seen as equivalent

                //Prepare lists of atoms already found
//                Set<IAtom> childDoneMol = new HashSet<IAtom>();
                List<IAtom> childDoneMol = new ArrayList<IAtom>();
                childDoneMol.addAll(doneMol);
                childDoneMol.add(tMol);
//                Set<IAtom> childDoneRef = new HashSet<IAtom>();
                List<IAtom> childDoneRef = new ArrayList<IAtom>();
                childDoneRef.addAll(doneRef);
                childDoneRef.add(tRef);
                
                //Try to go on with another recursion = evaluate unvisited branches
                if (mol.getConnectedBondsCount(tMol) > 1)
                {
                    boolean branchOntMolIsCompatible = exploreConnectivity(
                    		recNum+1, mol, tMol, childDoneMol, ref, tRef, 
                    		childDoneRef);
                    if (branchOntMolIsCompatible)
                    {
                        //Update list of successfully visited atoms
                        for (IAtom va : childDoneMol)
                        {
                            if (!doneMol.contains(va))
                                doneMol.add(va);
                        }
                        for (IAtom va : childDoneRef)
                        {
                            if (!doneRef.contains(va))
                                doneRef.add(va);
                        }

                        //Report good result
                        numCompBranches++;
                        break;
                    }
                } else {
                    doneMol.add(tMol);
                    doneRef.add(tRef);
                    numCompBranches++;
                    break;
                }
            }
        }
        
        boolean foundCompatibleTree = false;
        if ((numCompBranches + numAlreadyDone) == numBranches)
        {
            foundCompatibleTree = true;
        }
        return foundCompatibleTree;
    }

//------------------------------------------------------------------------------

    /**
     * Explore the container system following the connectivity.
     * This method labels
     * every atom according to the continuously connected graph that atom 
     * belongs
     * and the order of the visits. Note that the index for the fragment is
     * 1-based, while that of the order of visit is 0-based.
     * @param mol the molecular system under analysis
     * @param sources the atoms from which explorations are to be attempted
     * @param fragLabel the name of the CDK atom property field used to label
     * atoms of the same fragment
     * @param orderLabel the name of the CDK atom property field used to
     * record the order
     * @param priority the comparator defining the priority rules for
     * the exploration of the connectivity
     */

    public static void exploreContinuoslyConnectedFromSource(IAtomContainer mol,
    		List<IAtom> sources,
    		String fragLabel,
    		String orderLabel,
    		Comparator<SeedAtom> priority)
    {
        int fragLabValue = 0;
        for (IAtom atm : sources)
        {
            if (atm.getProperty(fragLabel) != null)
            {
                continue;
            }
            fragLabValue++;
            atm.setProperty(fragLabel,fragLabValue);
            int ordLabValue = 0;
            atm.setProperty(orderLabel,ordLabValue);
            ordLabValue = exploreConnectedToAtm(atm, 
                                                mol, 
                                                fragLabValue, 
                                                fragLabel, 
                                                ordLabValue, 
                                                orderLabel, 
                                                priority);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Explore the molecular system following the connectivity and labels
     * every atom according to the continuously connected graph it belongs.
     * The label can be used to identify totally disconnected fragments
     * in the IAtomContainer. Note that the index for the fragment is
     * 1-based.
     * @param mol the molecular system under analysis
     * @param label the name of the CDK atom property field used to label the 
     * atoms
     */

    public static void identifyContinuoslyConnected(IAtomContainer mol, 
    		String label)
    {
        ArrayList<IAtom> atoms = new ArrayList<IAtom>();
        for (IAtom atm : mol.atoms())
        {
            atoms.add(atm);
        }
        exploreContinuoslyConnectedFromSource(mol,
                                              atoms,
                                              label,
                                              "#",
                                              null);
    }

//------------------------------------------------------------------------------

    /**
     * Explore the molecular system by recursion following the connectivity 
     */

    private static int exploreConnectedToAtm(IAtom atm, IAtomContainer mol, 
                                             int frgLab, String frgLabName, 
                                             int ordLab, String ordLabName, 
                                             Comparator<SeedAtom> priority)
    {
        ArrayList<IAtom> seeds = new ArrayList<IAtom>();
        if (priority == null)
        {
            seeds.addAll(mol.getConnectedAtomsList(atm));
        }
        else
        {
            List<SeedAtom> salist = new ArrayList<SeedAtom>();
            for (IAtom a : mol.getConnectedAtomsList(atm))
            {
                SeedAtom sa = new SeedAtom(a,mol);
                salist.add(sa);
            }
            Collections.sort(salist, priority);
            for (SeedAtom sa : salist)
            {
                seeds.add(sa.getAtom());
            }
        }

        for (IAtom connectedAtom : seeds)
        {
            Object labVal = connectedAtom.getProperty(frgLabName);
            if (labVal == null)
            {
                // extend exploration to connectedAtom
                connectedAtom.setProperty(frgLabName,frgLab);
                ordLab++;
                connectedAtom.setProperty(ordLabName,ordLab);
                ordLab = exploreConnectedToAtm(connectedAtom, 
                                               mol, 
                                               frgLab, 
                                               frgLabName, 
                                               ordLab, 
                                               ordLabName, 
                                               priority);
            }
        }

        return ordLab;
    }

//------------------------------------------------------------------------------

    /**
     * Splits the atom container into fragments according to connectivity. All
     * atoms that are reachable by following the connectivity will fall into
     * the same fragment.
     * @param iac the initial atom container
     * @return a list of fragments
     */

    public static List<IAtomContainer> getConnectedFrags(IAtomContainer iac)
    {
        return getConnectedFrags(iac, 1);
    }

//------------------------------------------------------------------------------

    /**
     * Splits the atom container into fragments according to connectivity. All
     * atoms that are reachable by following the connectivity will fall into
     * the same fragment.
     * @param iac the initial atom container
     * @param minSize the minimum size of fragments to consider
     * @return a list of fragments (sorted from bigger to smaller according
     * to AtomContainerComparator).
     */

    public static List<IAtomContainer> getConnectedFrags(IAtomContainer iac, 
    		int minSize)
    {
        //Label atoms with the original index in the original atom list
        for (int i=0; i<iac.getAtomCount(); i++)
        {
            IAtom atm = iac.getAtom(i);
            atm.setProperty(ACCConstants.ATMIDPROP,i);
        }

        //Identify and isolate fragments
        ArrayList<IAtomContainer> frags = new ArrayList<IAtomContainer>();
        Map<Integer,List<IAtom>> fragsMap = identifyConnectedFrags(iac);
        for (Integer fragId : fragsMap.keySet())
        {
            IAtomContainer newIAC = new AtomContainer();
            try
            {
                newIAC = (IAtomContainer) iac.clone();
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Cannot clone molecule to "
                    + "extract fragments.", t);
            }
            ArrayList<IAtom> atmsToDel = new ArrayList<IAtom>();
            for (int i=0; i<iac.getAtomCount(); i++)
            {
                IAtom atmToKeep = iac.getAtom(i);
                if (!fragsMap.get(fragId).contains(atmToKeep))        
                {
                    atmsToDel.add(newIAC.getAtom(i));
                }
            }
            for (IAtom atm : atmsToDel)
            {
                newIAC.removeAtom(atm);
            }
            if (newIAC.getAtomCount() > minSize)
            {
                frags.add(newIAC);
            }
        }
        Collections.sort(frags, new AtomContainerComparator());
        Collections.reverse(frags);
        return frags;
    }

//------------------------------------------------------------------------------

    /**
     * Identify fragments according to connectivity. All
     * atoms that are reachable by following the connectivity will fall into
     * the same fragment.
     * @param iac the initial atom container
     * @return a map of fragments as lists of connected atoms
     */

    public static Map<Integer,List<IAtom>> identifyConnectedFrags(
    		IAtomContainer iac)
    {
        String prop = "FRAGMENTERBYCONNMTRX";
        identifyContinuoslyConnected(iac,prop);
        Map<Integer,List<IAtom>> fragsMap = new HashMap<Integer,List<IAtom>>();
        for (IAtom atm : iac.atoms())
        {
            int frgId = Integer.parseInt(atm.getProperty(prop).toString());
            if (fragsMap.keySet().contains(frgId))
            {
                fragsMap.get(frgId).add(atm);
            }
            else
            {
                List<IAtom> newFrag = new ArrayList<IAtom>();
                newFrag.add(atm);
                fragsMap.put(frgId,newFrag);
            }
        }

        return fragsMap;
    }

//------------------------------------------------------------------------------

}
