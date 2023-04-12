package autocompchem.molecule.geometry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
import java.util.TreeMap;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;

import autocompchem.atom.AtomUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;

/**
 * Tool to compare two molecular geometries
 * 
 * @author Marco Foscato
 */


public class ComparatorOfGeometries
{
    //Storage of descriptors
    private double rmsd;
    private double rmsDevIntAtmDist;
    private boolean useRMSDIntDist = false;


    //Storage of aligned structures
    private IAtomContainer inMolAlgn;
    private IAtomContainer refMolAlgn;

    /**
     * Storage for atom mappings results (as objects)
     */
    private List<Map<IAtom,IAtom>> allAtomMaps = null;

    /**
     * Storage for atom mappings results (as indexes)
     */
    private List<Map<Integer,Integer>> allAtomMapsAsIdx = null;

    /**
     * Index of best atom mapping
     */
    private int bestAtomMapping = -1;

    /**
     * Flag recording that a comparison by superposition has been run
     */
    private boolean runConparisonBySuperposition = false;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty MolecularComparator
     */

    public ComparatorOfGeometries() 
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty MolecularComparator and specify verbosity
     * @param verbosity the verbosity level
     */

    public ComparatorOfGeometries(int verbosity)
    {
        this.verbosity = verbosity;
    }

//------------------------------------------------------------------------------

    /**
     * Calculates and return the best atom mapping between two structures:
     * a map of which atom in the first structure corresponds to an atom in the
     * second structure. The result is reported with 0-based indexes in the atom
     * array of each structure, so that
     * the keys are the indexes of atoms in the second (reference)
     * structure while the values are the indexes in the first structure.
     * @param molA the first molecule
     * @param molB the second molecule
     * @return the atom map where keys are atom indexes in the second molecule
     * and values are atom indexes in the first molecule
     */
    public Map<Integer,Integer> getAtomMapping(IAtomContainer molA,
                                                            IAtomContainer molB)
    {
        try
        {
            compareGeometryBySuperposition((IAtomContainer) molA.clone(),
                                           (IAtomContainer) molB.clone());
        }
        catch (Throwable t)
        {
            IOtools.writeSDFAppend(
            		new File("AutoCompChem-error_uncloneable.sdf"),molA,false);
            IOtools.writeSDFAppend(
            		new File("AutoCompChem-error_uncloneable.sdf"),molB,true);
            Terminator.withMsgAndStatus("ERROR! Cannot clone mols to calculate "
                + "best geometry-aware atom mapping.", -1);
        }

        return allAtomMapsAsIdx.get(bestAtomMapping);
    }

//------------------------------------------------------------------------------

    /**
     * Compare two molecular geometeries by attempting superposition.
     * Results of the comparison are stored within this MoleculeComparator.
     * @param inMol Input molecule to me superposed to the reference one
     * @param refMol reference molecule
     */

    public void compareGeometryBySuperposition(IAtomContainer inMol, 
                                               IAtomContainer refMol)
    {
//TODO: make this tunable

        //See http://www.jcheminf.com/content/pdf/1758-2946-1-12.pdf
        //Paper describing the algorithms in use here.

        boolean bondSensitive = true;
        /*
        True: employed to screen compounds that mimic the substructure of the 
         query molecule(s) based on the MCS
        False: used for atom-atom mapping in a reaction where the focus is on
         bond changes, thus leading to a better understanding of the structural
         changes that emerge during a reaction
        */

        boolean ingnoreBondType = true;
        /* 
        Option added to convert all bonds to single bonds avoiding possible 
        mismatch due to resonance formes and aromaticity notation.
        */

        boolean ignoreStereType = true;
        /*
        Option added to ingore all stereo descriptors
        */

        boolean ringmatch = false;
        boolean stereoMatch = false;
        boolean fragmentMinimization = false;
        boolean energyMinimization = false;

        boolean removeHydrogen = false;
        boolean cleanAndConfigure = false;

        compareGeometryBySuperposition(inMol,refMol,true,true,true,false,false,
                                        false,false,false,false);
    }

//------------------------------------------------------------------------------

    /**
     * Compare two molecular geometeries by attempting superposition.
     * Results of the comparison are stored within this MoleculeComparator.
     * See the meaning of the boolean flags in 
     * <a href="https://doi.org/10.1186/1758-2946-1-12">Journal of Cheminformatics 2009 1:12</a>.
     * @param inMol Input molecule to me superposed to the reference one
     * @param refMol reference molecule
     * @param  bondSensitive see Journal of Cheminformatics 2009 1:12
     * @param  ingnoreBondType see Journal of Cheminformatics 2009 1:12
     * @param  ignoreStereType see Journal of Cheminformatics 2009 1:12
     * @param  ringmatch see Journal of Cheminformatics 2009 1:12
     * @param  stereoMatch see Journal of Cheminformatics 2009 1:12
     * @param  fragmentMinimization see Journal of Cheminformatics 2009 1:12
     * @param  energyMinimization see Journal of Cheminformatics 2009 1:12
     * @param  removeHydrogen see Journal of Cheminformatics 2009 1:12
     * @param  cleanAndConfigure see Journal of Cheminformatics 2009 1:12
     */

    public void compareGeometryBySuperposition(IAtomContainer inMol,
                                               IAtomContainer refMol,
                                               boolean bondSensitive,
                                               boolean ingnoreBondType,
                                               boolean ignoreStereType,
                                               boolean ringmatch,
                                               boolean stereoMatch,
                                               boolean fragmentMinimization,
                                               boolean energyMinimization,
                                               boolean removeHydrogen,
                                               boolean cleanAndConfigure)
    {

        //Clean bond order and stero descriptors
        if (ingnoreBondType || ignoreStereType)
        {
            for (IBond bnd : inMol.bonds())
            {
                if (ingnoreBondType)
                    bnd.setOrder(IBond.Order.SINGLE);

                if (ignoreStereType)
                    bnd.setStereo(IBond.Stereo.NONE);
            }

            for (IBond bnd : refMol.bonds())
            {
                if (ingnoreBondType)
                    bnd.setOrder(IBond.Order.SINGLE);

                if (ignoreStereType)
                    bnd.setStereo(IBond.Stereo.NONE);
            }
        }
        if (ignoreStereType)
        {
            for (IAtom atm : inMol.atoms())
            {
                atm.setStereoParity(CDKConstants.STEREO_ATOM_PARITY_UNDEFINED);
            }

            for (IAtom atm : refMol.atoms())
            {
                atm.setStereoParity(CDKConstants.STEREO_ATOM_PARITY_UNDEFINED);
            }   
        }

        //Initialise the tool for MCS (maximum common substructure)
//        Isomorphism im = new Isomorphism(Algorithm.DEFAULT, bondSensitive);
//This algorithm is faster for the kind of task meant here
//but fails sometime
        Isomorphism im = new Isomorphism(Algorithm.VFLibMCS, bondSensitive);
//
//MCSPlus takes forever...
//        Isomorphism im = new Isomorphism(Algorithm.MCSPlus, bondSensitive);
//
// Fast but makes mistakes
//        Isomorphism im = new Isomorphism(Algorithm.CDKMCS, bondSensitive);
//
//Returns null mapping in some case
//        Isomorphism im = new Isomorphism(Algorithm.SubStructure, bondSensitive);


        try {
            im.init(refMol,inMol,removeHydrogen,cleanAndConfigure);
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Unable to initialize "
                        + "Isomorphism. Exception returned by Isomorphism.",-1);
        }
        im.setChemFilters(stereoMatch,fragmentMinimization,energyMinimization);
        
        //First check for extreme case of no mapping at all
        if (im.getFirstAtomMapping() == null)
        {
            Terminator.withMsgAndStatus("ERROR! Isomorphism returned 'null' "
                                                        + " mapping.", -1);
        }

        //Calculate best superposition by looping over all given atom mappings
        double minValue = Double.MAX_VALUE;
        int mapIdx = -1;
        IAtomContainer refFromIM = im.getReactantMolecule();
        IAtomContainer inFromIM = im.getProductMolecule();
        allAtomMaps = im.getAllAtomMapping();
        allAtomMapsAsIdx = new ArrayList<Map<Integer,Integer>>();

//TODO del
/*
System.out.println("All mappings from IM");
for(Map<IAtom,IAtom> m : allAtomMaps)
{
    printAtomMap(m,inFromIM,refFromIM);
}
IOtools.writeSDFAppend("fromIM.sdf",refFromIM,true);
IOtools.writeSDFAppend("fromIM.sdf",inFromIM,true);
*/

        if (verbosity > 2)
            System.out.println(" Initiating loop over the " + allAtomMaps.size()
                                        + " atom-atom maps");

        for (Map<IAtom,IAtom> mapping : allAtomMaps)
        {
            mapIdx++;
            double locRMSD = -1.0;
            double locRMSDIAD = -1.0;

            if (verbosity > 2)
                System.out.println(" Working on mapping "+mapIdx+": ");

            //Store atom index version of the atom mapping
            storeAtomMappingAsIdx(mapping,inFromIM,refFromIM);

            //Align structured using the current mapping and get RMSD
            locRMSD = alignMolsWithMapping(refFromIM, inFromIM, mapping);

            //Get RMS deviation of itramolecular Distances
            locRMSDIAD = getRMSDevIntramolecularDistances(mapping);

//Only for testing
//System.out.println(" " + fid + " " + locRMSD);
//writeTestFilesForQUATFIT(mapping, refFromIM, inFromIM);

            //Choose the value to be minimized over the mappings
            double localValue = 0.0d;
            if (useRMSDIntDist)
            {
                localValue = locRMSDIAD;
            } else {
                localValue = locRMSD;
            }

            //Compare with previous mappings and, in case, store results
            if (localValue < minValue)
            {
                minValue = localValue;
                bestAtomMapping = mapIdx;
                this.rmsd = locRMSD;
                this.rmsDevIntAtmDist = locRMSDIAD; 
                this.inMolAlgn = inFromIM;
                this.refMolAlgn = refFromIM;
            }

            //Report results
            if (verbosity > 2)
            {
                System.out.println("  (local) RSMD from the alignmentt: " 
                                        + locRMSD);
                System.out.println("  (local) RSM deviation of intramolecular "
                                         + "distances: " + locRMSDIAD);
            }
        } //End loop over atom mapping

        if (verbosity > 1)
        {
            System.out.println(" BEST ATOM MAPPING from Isomorphism: RMSD = "
                                + this.rmsd + " (Map " 
                                + bestAtomMapping + " of " 
                                + allAtomMaps.size() + ")." );
            printAtomMap(allAtomMaps.get(bestAtomMapping),inFromIM,refFromIM);
        }

        //Try improving mapping and alignment up to convergence
//TODO make this parameter user defined
        int maxCycles = 2;
        int numMapBeforeRef = allAtomMaps.size();
        for (int c=0; c<maxCycles; c++)
        {
            //Adjust mapping on the basis of previous alignment
            Map<IAtom,IAtom> newMap = adjustAtomMappingOfAlignedMols(
                                                                refMolAlgn,
                                                                inMolAlgn,
                                                                allAtomMaps);
            //In case of problems adjusting the atom-atom map
            if (newMap == null)
                continue;

            //Store new Map
            allAtomMaps.add(newMap);
            storeAtomMappingAsIdx(newMap,inMolAlgn,refMolAlgn);

            //Align according to the new atom map
            this.rmsd = alignMolsWithMapping(refMolAlgn, inMolAlgn, newMap);
            this.rmsDevIntAtmDist = getRMSDevIntramolecularDistances(newMap);

//Only for testing
//System.out.println(" " + fid + " " + this.rmsd);
//writeTestFilesForQUATFIT(newMap, refFromIM, inFromIM);
        
            //Report
            if (verbosity > 2)
            {
                System.out.println(" Convergence (iteration " + c + ")");
                System.out.println("  RSMD from the alignment: " + rmsd);
                System.out.println("  RSM deviation of intramolecular "
                                        + "distances: " + rmsDevIntAtmDist);
            }
        }

        //Report results
        if (verbosity > 0 && allAtomMaps.size() > numMapBeforeRef)
        {
            bestAtomMapping = allAtomMaps.size()-1;
            if (useRMSDIntDist)
            {
                System.out.println(" 3D-REFINED ATOM MAPPING (Using RMS Dev. )"
                                                              + " Intr. Dist.");
            } else {
                System.out.println(" 3D-REFINED ATOM MAPPING (Using RMSD)");
            }
            System.out.println(" RSMD from the alignment: " + this.rmsd);
            System.out.println(" RSM deviation of intramolecular "
                                       + "distances: " + this.rmsDevIntAtmDist);
            printAtomMap(allAtomMaps.get(bestAtomMapping),inMolAlgn,refMolAlgn);
        }

        //Record that this method has run
        this.runConparisonBySuperposition = true;
    }

//------------------------------------------------------------------------------

    private void printAtomMap(Map<IAtom,IAtom> atomMap, IAtomContainer molA,
                              IAtomContainer molB)
    {
        if (atomMap.size() == 0)
        {
            System.out.println(" Atom map is empty! ");
            return;
        }

        System.out.println(" Atom map: ");
        for (IAtom key : atomMap.keySet())
        {
            System.out.println("Atom-Atom MAP:   "
                    + MolecularUtils.getAtomRef(atomMap.get(key),molA)
                    + " <---> " + MolecularUtils.getAtomRef(key,molB));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Retrun the first molecule (target) aligned to the second molecule 
     * (reference)This is a result of a comparisong of geometry by
     * superposition. Calling this method without having run such comparison
     * will return an error.
     * @return the aligned atom container.
     */

    public IAtomContainer getFirstMolAligned()
    {
        if (!runConparisonBySuperposition)
        {
            Thread.dumpStack();
            Terminator.withMsgAndStatus("ERROR! Cannot return aligned molecule "
                + "before any comparison of geometry by superpostion.",-1);
        }
        return inMolAlgn;
    }

//------------------------------------------------------------------------------

    /**
     * Retrun the second molecule (reference) translated as to superpose to
     * the first. This is a result of a comparisong of geometry by 
     * superposition. Calling this method without having run such comparison
     * will return an error.
     * @return the aligned atom container.
     */

    public IAtomContainer getSecondMolAligned()
    {
        if (!runConparisonBySuperposition)
        {
            Thread.dumpStack();
            Terminator.withMsgAndStatus("ERROR! Cannot return aligned molecule "
                + "before any comparison of geometry by superpostion.",-1);
        }
        return refMolAlgn;
    }

//------------------------------------------------------------------------------

    /**
     * Return the size of the atom-atom map
     * @return the size of the atom-atom map 
     */

    public int getSizeOfMap()
    {
        return  allAtomMapsAsIdx.get(bestAtomMapping).size();
    }

//------------------------------------------------------------------------------

    /**
     * Retrun numericla value representing the agreement of the two geometries.
     * @return the score representing agreement between the geometries
     */

    public double getAlignementScore()
    {
        if (!runConparisonBySuperposition)
        {
            Thread.dumpStack();
            Terminator.withMsgAndStatus("ERROR! Cannot return aligned molecule "
                + "before any comparison of geometry by superpostion.",-1);
        }
        double score = 0.0d;
        if (useRMSDIntDist)
        {
            score = rmsDevIntAtmDist;
        } else {
            score = rmsd;
        }
        return score;
    }

//------------------------------------------------------------------------------

    private void storeAtomMappingAsIdx(Map<IAtom,IAtom> atomMap, 
                                       IAtomContainer molA, IAtomContainer molB)
    {
        Map<Integer,Integer> map = new HashMap<Integer,Integer>();
        for (IAtom atmB : atomMap.keySet())
        {
            int atmNumB = molB.indexOf(atmB);
            int atmNumA = molA.indexOf(atomMap.get(atmB));
            map.put(atmNumB,atmNumA);
        }
        allAtomMapsAsIdx.add(map);
    }

//------------------------------------------------------------------------------

    /**
     * Explore two molecules starting from a seed atom and following the 
     * connectivity.
     * @param sA seed atom for <code>molA</code>
     * @param sB seed atom for <code>molB</code>
     * @param molA molecule A
     * @param molB molecule B
     * @param lstA list of already visited atoms in <code>molA</code>
     * @param lstB list of already visited atoms in <code>molB</code>
     */

    private void explorePairOfMols(IAtom sA, IAtom sB, 
                                   IAtomContainer molA, IAtomContainer molB, 
                                   List<IAtom> lstA, List<IAtom> lstB)
    {
        if (verbosity > 2) 
        {
            System.out.println(" Exploring mols: pair "
                        + MolecularUtils.getAtomRef(sA,molA) + " - "
                        + MolecularUtils.getAtomRef(sB,molB));
        }
        
        //store seeds
        if (lstA.contains(sA))
        {
            System.out.println(" WARNING! LstA already contains qA = "  
                                + MolecularUtils.getAtomRef(sA,molA));
        }
        lstA.add(sA);

        if (lstB.contains(sB))
        {
            System.out.println(" WARNING! LstB already contains qB = " 
                                + MolecularUtils.getAtomRef(sB,molB));
        }
        lstB.add(sB);

        //Get connected atoms
        List<IAtom> nbrsA = molA.getConnectedAtomsList(sA);
        List<IAtom> nbrsB = molB.getConnectedAtomsList(sB);
        
        boolean foundPartner = false;
        for (int i=0; i<nbrsA.size(); i++)
        {
            IAtom qA = nbrsA.get(i);

            //skip if already done
            if (lstA.contains(qA))
                continue;

            if (verbosity > 2)
            {
                System.out.println(" Attempt to find atom in molB "
                                + "corresponding to " 
                                + MolecularUtils.getAtomRef(qA,molA)
                                + " in molA.");
            }

            //get list of candidates
            Map<Double,IAtom> sortedCandidates = new TreeMap<Double,IAtom>();
            List<IAtom> proposals = new ArrayList<IAtom>();
            for (int nMap=0; nMap<allAtomMaps.size(); nMap++)
            {
                IAtom candidateInB = allAtomMaps.get(nMap).get(qA);

                if (!proposals.contains(candidateInB))
                    proposals.add(candidateInB);
                
                //skip null
                if (candidateInB == null)
                    continue;

                //skip already used
                if (lstB.contains(candidateInB))
                {
//System.out.println("  atom "+MolecularUtils.getAtomRef(candidateInB,molB)+" already used");
                    continue;
                }

                //Connectivity condition
                if (!nbrsB.contains(candidateInB))
                {
//System.out.println("  atom "+MolecularUtils.getAtomRef(candidateInB,molB)+" not in nbrs");
                    continue;
                }

                //calculate the distance in the aligned orientation
                Point3d pA = AtomUtils.getCoords3d(qA);
                Point3d pB = AtomUtils.getCoords3d(candidateInB);
                double d = pA.distance(pB);

                sortedCandidates.put(d,candidateInB);
            }

            //If no good candidate give up
            if (sortedCandidates.size() < 1)
            {
                if (verbosity > 3)
                {
                    System.out.println(" No good candidate found to proceed in "
                                        + "this direction");
                    System.out.print(" Proposed atom of molB were: ");
                    for (IAtom a : proposals)
                    {
                        if (a == null)
                            System.out.print("null;");
                        else
                            System.out.print(MolecularUtils.getAtomRef(a,molB) 
                                                + ";");
                    }
                    System.out.print("\n");
                }
                continue;
            }
            if (verbosity > 3)
            {
                System.out.print(" Proposed atom of molB were: ");
                for (IAtom a : proposals)
                {
                    if (a == null)
                        System.out.print("null;");
                    else
                        System.out.print(MolecularUtils.getAtomRef(a,molB)
                                            + ";");
                }
                System.out.print("\n");
            }

            //Evaluate candidates in order of closeness
            IAtom qB = new Atom();
            boolean alreadyFound = false;
            for (Double d : sortedCandidates.keySet())
            {
                IAtom candidateInB = sortedCandidates.get(d);

                //Connectivity condition
                if (!nbrsB.contains(candidateInB))
                    continue;

                //Here we have the closest, compatible, connected atom in B
                //Store it in the list
                if (alreadyFound)
                {
                    System.out.println("WARNING! More than one atom satisfy "
                                                        + "the condition!");
                    System.out.println("Atoms that map " 
                                        + MolecularUtils.getAtomRef(qA,molA));
                    System.out.println(MolecularUtils.getAtomRef(qB,molB));
                    System.out.println(MolecularUtils.getAtomRef(candidateInB,
                                                                        molB));
                    IOtools.pause();
                } else {
                    qB = candidateInB;
                    alreadyFound = true;
                    break;
                }
            }
            
            //Recursion on the stored atoms
            if (alreadyFound)
            {
                explorePairOfMols(qA, qB, molA, molB, lstA, lstB);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Calculates the Root Mean Square Deviation of Intramolecular Distances
     * according to the method used by Yury Minencov (see the paper 
     * TODO: add reference -TODO check if this is right!!!!!).
     * We compare two vectors of atoms calculating all 
     * inter atomic distances (IAD) within each vector and considering the Root
     * Mean Square of the Distance IAD(a) - IAD(b).
     * @param map mapped pairs of atoms
     * @return the value of Root Mean Square Deviation of Intramolecular 
     *         Distances (RMSDIAD)
     */

    public static double getRMSDevIntramolecularDistances(Map<IAtom,IAtom> map)
    {
        //Get Lists of atoms in non-map format
        IAtom[] lstA = new IAtom[map.size()];
        IAtom[] lstB = new IAtom[map.size()];
        int k = 0;
        for (Map.Entry<IAtom,IAtom> pair : map.entrySet())
        {
            lstA[k] = pair.getKey();
            lstB[k] = pair.getValue();
            k++;
        }

        return getRMSDevIntramolecularDistances(lstA, lstB);
    }

//------------------------------------------------------------------------------

    /**
     * Calculates the Root Mean Square Deviation of Intramolecular Distances
     * according to the method used by Yury Minencov (see the paper 
     * TODO: add reference - TODO check if this is right!!!!!).
     * We compare two vectors of atoms calculating all 
     * inter atomic distances (IAD) within each vector and considering the Root
     * Mean Square of the Distance IAD(a) - IAD(b).
     * @param lstA first vector of atoms 
     * @param lstB second vector of atoms
     * @return the value of Root Mean Square Deviation of Intramolecular 
     *         Distances (RMSDIAD)
     */

    public static double getRMSDevIntramolecularDistances(IAtom[] lstA, IAtom[] lstB)
    {
        double result = 0.0d;

        //Check if conditions for running this calculation are satisfied
        if (lstA.length != lstB.length)
        {
            Terminator.withMsgAndStatus("ERROR! Attempt to calculate RMSD "
                        + " of structures with different number of atoms",-1);
        }

//
//TODO: make it calculating Mean Unsigned Deviation and Mean Signed Deviation
//      all in once.
//

        for (int i=0; i<lstA.length; i++)
        {
            //Get first point for both structures
            Point3d pAi = AtomUtils.getCoords3d(lstA[i]);
            Point3d pBi = AtomUtils.getCoords3d(lstB[i]);

            for (int j=i+1; j<lstA.length; j++)
            {
                //Get second point for both structures
                Point3d pAj = AtomUtils.getCoords3d(lstA[j]);
                Point3d pBj = AtomUtils.getCoords3d(lstB[j]);

                //Get distances
                double dAij = pAi.distance(pAj);
                double dBij = pBi.distance(pBj);

                //deviation
                double dev = dAij - dBij;
                double devSquare = (dev) * (dev);

                //sum to others
                result = result + devSquare;
            }
        }

        //Calculate prefactor
        int n = lstA.length;
        double denominator = n * (n - 1);
        double preFactor = 2.0 / denominator;
        result = preFactor * result;
        result = Math.sqrt(result);

        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * Giving a pair of aligned molecules this method tries to improve the atom
     * mapping according to the best superposition of the molecules.
     * Useful to improve atom mapping when symmetry and conformational changes
     * create multiple possible atom correspondences (maps) and the
     * isomorphism's tools, that does not consider 3D coordinates, cannot 
     * appreciate the difference between various atom maps.
     * A new map is created using the PREVIOUS MAPS, CONNECTIVITY and CLOSENESS
     * from 3D coordinates of the aligned molecules) to select an atom map 
     * that can lead to a lower RMSD between the two structures. 
     * 
     * @param refMol the reference molecule
     * @param fitMol the fitted molecule
     * @param mappings ensemble of reasonable atom mappings
     * @return the new atom map defining which atom in the reference (KEY)
     * corresponds to which atom in the molecule to be fitted (VALUE).
     */

    private Map<IAtom,IAtom> adjustAtomMappingOfAlignedMols(
                                                          IAtomContainer refMol,
                                                          IAtomContainer fitMol,
                                                List<Map<IAtom,IAtom>> mappings)
    {
        if (verbosity > 2)
            System.out.println(" Adjusting atom mapping of aligned molecules");

        //Get the first good seed for search:
        //An atom that matched only one atom in the fitted molecule in all maps
        IAtom seedRef = new Atom();
        IAtom seedFit = new Atom();
        boolean fountGoodPairOfSeeds = false;
        for (IAtom candRef : refMol.atoms())
        {
            IAtom candFit = new Atom();
            int counter = 0;
            for (int nMap=0; nMap<mappings.size(); nMap++)
            {
                if (!mappings.get(nMap).containsKey(candRef))
                    continue;

                counter++;

                if (nMap==0)
                {
                    //So, the atom on refMol has one partner in fitMol
                    candFit = mappings.get(0).get(candRef);
                } else {
                    if(!candFit.equals(mappings.get(nMap).get(candRef)))
                    {
                        //but, there is another atom in fitMol that matches
                        // so the candidate on refMol is rejected by stopping
                        // the loop (later we check if the loo run all the way
                        break;
                    }
                }
            }

            if (counter == mappings.size())
            {
                fountGoodPairOfSeeds = true;
                seedRef = candRef;
                seedFit = candFit;
                break;
            }
        }
        if (!fountGoodPairOfSeeds)
        {
            System.out.println(" WARNING! Cannot improve the atom-atom map.");
            return null;
// If we cannot improve the atom-atom map then we keep that one we already have
//            Terminator.withMsgAndStatus("ERROR! Atom mapping in reference and "
//                + "fitted molecule have different size!",-1);
        }

        //Get the correspondence
        List<IAtom> lstRef = new ArrayList<IAtom>();
        List<IAtom> lstFit = new ArrayList<IAtom>();
        explorePairOfMols(seedRef, seedFit, refMol, fitMol, lstRef, lstFit);

        //Check correctness of sizes
        if (lstRef.size() == 0 || lstRef.size() != lstFit.size())  
        {
            System.out.println(" WARNING! Cannot improve the atom-atom map.");
            return null;
// If we cannot improve the atom-atom map then we keep that one we already have
//            Terminator.withMsgAndStatus("ERROR! Atom mapping in reference and "
//                + "fitted molecule have different size!",-1);
        }

        //Prepare output
        Map<IAtom,IAtom> newMap = new HashMap<IAtom,IAtom>();
        for (int i=0; i<lstRef.size(); i++)
        {
            newMap.put(lstRef.get(i),lstFit.get(i));
        }

        return newMap;
    }

//------------------------------------------------------------------------------

    /**
     * Align (or superpose) two molecules as rigid bodies. The RMSD of the
     * superposition is defined by the list of atoms that has to be fitted one 
     * to the other.
     * @param molA the reference molecule 
     * @param molB the molecule to be fitted
     * @param mapping atom mapping defining the correspondence between atoms in
     * the reference molecule (entry KEY) and in the molecule to be fitted 
     * (entry VALUE). 
     * @return the root mean square distance from <code>KabschAlignment</code>
     */

    private double alignMolsWithMapping(IAtomContainer molA,
                                        IAtomContainer molB,
                                        Map<IAtom,IAtom> mapping)
    {
        //Get Lists of atoms in format suitable for KabschAlignment
        IAtom[] lstA = new IAtom[mapping.size()];
        IAtom[] lstB = new IAtom[mapping.size()];
        int k = 0;
        for (Map.Entry<IAtom,IAtom> pair : mapping.entrySet())
        {
            lstA[k] = pair.getKey();
            lstB[k] = pair.getValue();
            k++;
        }

        //Calculate rotational matrix and align
        KabschAlignment sa = null;
        try
        {
            if (verbosity > 2)
            {
                System.out.println("  Running KabschAlignment");
            }
            sa = new KabschAlignment(lstA, lstB);
            sa.align();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            System.out.println(" KabschAlignment failed with atom map (size: "+
                                                           mapping.size()+")");
            printAtomMap(mapping,molA,molB);
            Terminator.withMsgAndStatus("ERROR! KabschAlignment returns "
                                                         + "an exception.",-1);
        }

        //Translation of molecule A (ref)
        Point3d cm = sa.getCenterOfMass();
        for (int ia = 0; ia < molA.getAtomCount(); ia++)
        {
            IAtom a = molA.getAtom(ia);
            Point3d oldPlace = AtomUtils.getCoords3d(a);
            Point3d newPlace = new Point3d(oldPlace.x - cm.x,
                                               oldPlace.y - cm.y,
                                               oldPlace.z - cm.z);
            a.setPoint3d(newPlace);
        }

        //Rototranslation of molecule B
        sa.rotateAtomContainer(molB);

        //Get RMSD for structure superposition
        double locRMSD = sa.getRMSD();

        return locRMSD;
    }

//------------------------------------------------------------------------------

}
