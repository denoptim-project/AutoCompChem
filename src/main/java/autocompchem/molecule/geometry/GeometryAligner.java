package autocompchem.molecule.geometry;

import java.io.File;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.vecmath.Point3d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Atom;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to compare the geometries of {@link IAtomContainer}s and align their
 * geometries.
 * 
 * @author Marco Foscato
 */

public class GeometryAligner extends AtomContainerInputProcessor
{   
    /**
     * Molecular representation of the reference substructure
     */
    private IAtomContainer reference;

    /**
     * String defining the task of sorting molecules
     */
    public static final String ALIGNGEOMETRIESTASKNAME = "alignGeometries";

    /**
     * Task about sorting molecules
     */
    public static final Task ALIGNGEOMETRIESTASK;
    static {
    	ALIGNGEOMETRIESTASK = Task.make(ALIGNGEOMETRIESTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public GeometryAligner()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ALIGNGEOMETRIESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/GeometryAligner.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new GeometryAligner();
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	if (params.contains("REFERENCE"))
        {
	        File refFile = new File(
	        		params.getParameter("REFERENCE").getValueAsString());
	        FileUtils.foundAndPermissions(refFile,true,false,false);
	        List<IAtomContainer> lst = IOtools.readMultiMolFiles(refFile);
	        if (lst.size()>1)
            {
                logger.warn("WARNING: Found " + lst.size() 
                + " reference molecules, but we'll use only the first one.");
            }
	        reference = lst.get(0);
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
    	multiGeomMode = MultiGeomMode.INDEPENDENTJOBS;
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		IAtomContainer result = null;
		if (task.equals(ALIGNGEOMETRIESTASK))
		{
			GeometryAlignment alignment = null;
			try {
				alignment = alignGeometries(reference, iac);
			} catch (IllegalArgumentException | CloneNotSupportedException e) {
				Terminator.withMsgAndStatus("ERROR! Could not align geometries "
						+ "'" + MolecularUtils.getNameOrID(iac) + "' and '" 
						+ MolecularUtils.getNameOrID(reference)+ "'.", -1, e);
			}
			
			result = alignment.getSecondIAC().iac;
			
			if (exposedOutputCollector != null)
	        {
	    	    String molID = "mol-"+i;
		        exposeOutputData(new NamedData(task.ID + molID + "RMSD",
		        		alignment.getRMSD()));
		        exposeOutputData(new NamedData(task.ID + molID + "RMSDIM",
		        		alignment.getRMSDIM()));
		        exposeOutputData(new NamedData(task.ID + molID,
		        		alignment.getSecondIAC()));
	    	}
		}
		return result;
    }
	
//-----------------------------------------------------------------------------
	
    /**
     * Calculates and return the best atom mapping between two structures:
     * a map of which atom in the first structure (reference or query) 
     * best corresponds to an atom in the second structure.
     * @param molA the first molecule
     * @param molB the second molecule
     * @return the atom map where keys are atom in the first molecule
     * and values are atom indexes in the second molecule
     */
    public static Map<Integer,Integer> getBestAtomMappingByGeometry(
    		IAtomContainer molA,
    		IAtomContainer molB)
    {
    	GeometryAlignment alignment = null;
        try
        {
        	alignment = alignGeometries(
        			(IAtomContainer) molA.clone(),
        			(IAtomContainer) molB.clone());
        } catch (Throwable t)
        {
        	String fileName = "AutoCompChem-error_uncloneable.sdf";
            IOtools.writeSDFAppend(new File(fileName),molA,false);
            IOtools.writeSDFAppend(new File(fileName),molB,true);
            Terminator.withMsgAndStatus("ERROR! Cannot clone mols to calculate "
                + "best geometry-aware atom mapping. See " + fileName, -1);
        }
        return alignment.getMappingIndexes();
    }
    
//------------------------------------------------------------------------------

    /**
     * Aligns two structures according to general purpose default settings.
     * @param reference the geometry onto which we align.
     * @param structure the geometry to be aligned to the reference.
     * @return the best alignment as a snapshot of both systems, i.e., it 
     * contains clones of the {@link IAtomContainer}s given as input.
     * @throws IllegalArgumentException if the structures cannot be aligned
     * @throws CloneNotSupportedException if either of the atom containers is 
     * not cloneable.
     */

    public static GeometryAlignment alignGeometries(IAtomContainer reference,
    		IAtomContainer structure) 
    				throws IllegalArgumentException, CloneNotSupportedException
    {
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
        mismatch due to resonance forms and aromaticity notation.
        */

        boolean ignoreStereType = true;
        /*
        Option added to ignore all stereo descriptors
        */

        boolean ringmatch = false;
        boolean stereoMatch = false;
        boolean fragmentMinimization = false;
        boolean energyMinimization = false;

        boolean removeHydrogen = false;
        boolean cleanAndConfigure = false;
        
        boolean useRMSDIntDist = false;

        return alignGeometries(reference, structure, 
        		bondSensitive,
        		ingnoreBondType,
        		ignoreStereType,
        		ringmatch,
        		stereoMatch,
        		fragmentMinimization,
        		energyMinimization,
        		removeHydrogen,
        		cleanAndConfigure,
        		2, //maximum number of re-mapping attempts
        		useRMSDIntDist);
    } 
    
//------------------------------------------------------------------------------

    /**
     * Aligns two molecular systems according to tunable settings. 
     * See the meaning of the boolean flags in 
     * <a href="https://doi.org/10.1186/1758-2946-1-12">Journal of Cheminformatics 2009 1:12</a>.
     * 
     * @param reference the geometry onto which we align.
     * @param structure the geometry to be aligned to the reference.
     * @param bondSensitive see Journal of Cheminformatics 2009 1:12
     * @param ingnoreBondType see Journal of Cheminformatics 2009 1:12
     * @param ignoreStereType see Journal of Cheminformatics 2009 1:12
     * @param ringmatch see Journal of Cheminformatics 2009 1:12
     * @param stereoMatch see Journal of Cheminformatics 2009 1:12
     * @param fragmentMinimization see Journal of Cheminformatics 2009 1:12
     * @param energyMinimization see Journal of Cheminformatics 2009 1:12
     * @param removeHydrogen see Journal of Cheminformatics 2009 1:12
     * @param cleanAndConfigure see Journal of Cheminformatics 2009 1:12
     * @param maxCycles maximum number of attempt to update mapping after
     * alignment of the geometries.
     * @param useRMSDIntDist use <code>true</code> to select the best mapping 
     * based on the RMSD of intermolecular distances.
     * @return the best alignment.
     * @throws IllegalArgumentException if the structures cannot be aligned
     * @throws CloneNotSupportedException if either of the atom containers is 
     * not cloneable.
     */

    public static GeometryAlignment alignGeometries(
            IAtomContainer reference,
    		IAtomContainer structure,
            boolean bondSensitive,
            boolean ingnoreBondType,
            boolean ignoreStereType,
            boolean ringmatch,
            boolean stereoMatch,
            boolean fragmentMinimization,
            boolean energyMinimization,
            boolean removeHydrogen,
            boolean cleanAndConfigure,
            int maxCycles,
            boolean useRMSDIntDist) 
            		throws IllegalArgumentException, CloneNotSupportedException
    {
    	Logger logger = LogManager.getLogger(GeometryAligner.class);
    	
        //Clean bond order and stereo descriptors
        if (ingnoreBondType || ignoreStereType)
        {
            for (IBond bnd : structure.bonds())
            {
                if (ingnoreBondType)
                    bnd.setOrder(IBond.Order.SINGLE);

                if (ignoreStereType)
                    bnd.setStereo(IBond.Stereo.NONE);
            }

            for (IBond bnd : reference.bonds())
            {
                if (ingnoreBondType)
                    bnd.setOrder(IBond.Order.SINGLE);

                if (ignoreStereType)
                    bnd.setStereo(IBond.Stereo.NONE);
            }
        }
        if (ignoreStereType)
        {
            for (IAtom atm : structure.atoms())
            {
                atm.setStereoParity(CDKConstants.STEREO_ATOM_PARITY_UNDEFINED);
            }

            for (IAtom atm : reference.atoms())
            {
                atm.setStereoParity(CDKConstants.STEREO_ATOM_PARITY_UNDEFINED);
            }   
        }
        
        // Check pre-requisite
        if (reference.getAtomCount()>structure.getAtomCount())
        {
        	throw new IllegalArgumentException("We can only align a structure "
        			+ "to a reference that contains at most "
        			+ "as many atoms as the structure to align. Attempt to "
        			+ "align a structure with " + structure.getAtomCount() 
        			+ " atoms to a reference with "
        			+ reference.getAtomCount() + " atoms.");
        }

        //Initialise the tool for MCS (maximum common substructure)
//        Isomorphism im = new Isomorphism(Algorithm.DEFAULT, bondSensitive);
//This algorithm is faster for the kind of task meant here
//but fails sometime
        
        //TODO: consider using  http://github.com/asad/smsd
        @SuppressWarnings("deprecation")
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
            im.init(reference,structure,removeHydrogen,cleanAndConfigure);
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Unable to initialize "
                        + "Isomorphism. Exception returned by Isomorphism.", -1,
                        t );
        }
        im.setChemFilters(stereoMatch,fragmentMinimization,energyMinimization);
        
        //First check for case of no mapping at all
        if (im.getFirstAtomMapping() == null)
        {
            throw new IllegalArgumentException("Isomorphism returned 'null' "
            		+ " mapping.");
        }
        
        //Exclude mappings that do not allow alignement
        List<Map<IAtom, IAtom>>  toRemove = new ArrayList<Map<IAtom, IAtom>>();
        List<Map<IAtom, IAtom>> allAtomMaps = im.getAllAtomMapping();
        for (Map<IAtom, IAtom> mapping : allAtomMaps)
        {
        	if (mapping.size()<3)
        	{
        		logger.trace("Ignoring atom-atom mapping of size " 
        				+ mapping.size());
        		toRemove.add(mapping);
        	}
        }
        allAtomMaps.removeAll(toRemove);
        if (allAtomMaps.size()<1)
        {
        	throw new IllegalArgumentException("No atom mapping suitable for "
        			+ "alignment was found.");
        }

        //Calculate best superposition by looping over all given atom mappings
        double minValue = Double.MAX_VALUE;
        int mapIdx = -1;
        IAtomContainer refFromIM = im.getReactantMolecule();
        IAtomContainer inFromIM = im.getProductMolecule();
        IAtomContainer inMolAlgn = structure;
        IAtomContainer refMolAlgn = reference;
        

        int bestAtomMapping = 0;
        double rmsd = Double.MAX_VALUE;
        double rmsDevIntAtmDist = Double.MAX_VALUE;
        

        GeometryAlignment bestAlignment = null;
        for (Map<IAtom,IAtom> mapping : allAtomMaps)
        {
            mapIdx++;
            double locRMSD = -1.0;
            double locRMSDIAD = -1.0;

            logger.trace("Working on mapping "+mapIdx);
            if (mapping.size()<2)
            {
            	logger.trace("Ignoring mapping with only " + mapping.size() 
            		+ "mapped atoms.");
            	continue;
            }

            //Align structured using the current mapping and get RMSD
            locRMSD = alignMolsWithMapping(refFromIM, inFromIM, mapping);

            //Get RMS deviation of intramolecular Distances
            locRMSDIAD = getRMSDevIntramolecularDistances(mapping);
           
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
                rmsd = locRMSD;
                rmsDevIntAtmDist = locRMSDIAD; 
                inMolAlgn = inFromIM;
                refMolAlgn = refFromIM;

                bestAlignment = new GeometryAlignment(refFromIM, inFromIM, mapping);
                bestAlignment.setRMSD(locRMSD);
                bestAlignment.setRMSDIM(locRMSDIAD);
            }
            
            logger.trace("RSMD from the alignmentt: " + locRMSD + 
            		" RSM deviation of intramolecular distances: " 
            		+ locRMSDIAD);
        } //End loop over atom mapping


        String msg = "Best atom mapping from Isomorphism: " + NL 
        		+ "RMSD = " + rmsd + NL 
        		+ "Map " + bestAtomMapping + " of "+ allAtomMaps.size() 
        		+ ": " +  bestAlignment.getMappingDefinition();
        logger.debug(msg);

        //Try improving mapping and alignment up to convergence
        boolean foundBetterAlignment = false;
        for (int c=0; c<maxCycles; c++)
        {
            //Adjust mapping on the basis of previous alignment
            Map<IAtom, IAtom> newMap = adjustAtomMappingOfAlignedMols(
                                                                refMolAlgn,
                                                                inMolAlgn,
                                                                allAtomMaps,
                                                                logger);
            
            //In case of problems adjusting the atom-atom map
            if (newMap == null)
                continue;

            //Store new Map
            allAtomMaps.add(newMap);

            //Align according to the new atom map
            mapIdx++;
            double locRMSD = alignMolsWithMapping(refMolAlgn, inMolAlgn, newMap);
            double locRMSDIAD = getRMSDevIntramolecularDistances(newMap);
            
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
                rmsd = locRMSD;
                rmsDevIntAtmDist = locRMSDIAD; 
                inMolAlgn = inFromIM;
                refMolAlgn = refFromIM;

                bestAlignment = new GeometryAlignment(refFromIM, inFromIM, newMap);
                bestAlignment.setRMSD(locRMSD);
                bestAlignment.setRMSDIM(locRMSDIAD);
            }
        
            logger.trace("Convergence (iteration " + c + "): "
            		+ "RSMD from the alignment: " + rmsd 
            		+ "; RSM deviation of intramolecular distances: " 
            		+ rmsDevIntAtmDist);
        }

        //Report results
        if (foundBetterAlignment)
        {
            bestAtomMapping = allAtomMaps.size()-1;
            String msg2  = "3D-refined atom mapping ";
            if (useRMSDIntDist)
            {
            	msg2 = msg2 + "(Using RMS Dev. Intr. Dist.)";
            } else {
            	msg2 = msg2 + "(Using RMSD)";
            }
            msg2 = msg2 + "; RSMD from the alignment: " + rmsd 
            		+ "; RSM deviation of intramolecular distances: " 
            		+ rmsDevIntAtmDist + "; Mapping: " 
            		+ System.getProperty("line.separator") 
            		+ bestAlignment.getMappingDefinition();
            logger.debug(msg2);
        }
        return bestAlignment;
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
     * corresponds to which atom in the molecule to be fitted (VALUE), or null
     * if no better matching could be found.
     */

    private static Map<IAtom,IAtom> adjustAtomMappingOfAlignedMols(
    		IAtomContainer refMol, IAtomContainer fitMol,
    		List<Map<IAtom,IAtom>> allAtomMaps, Logger logger)
    {
        logger.trace("Adjusting atom mapping of aligned molecules.");

        //Get the first good seed for search:
        //An atom that matched only one atom in the fitted molecule in all maps
        IAtom seedRef = new Atom();
        IAtom seedFit = new Atom();
        boolean fountGoodPairOfSeeds = false;
        for (IAtom candRef : refMol.atoms())
        {
            IAtom candFit = new Atom();
            int counter = 0;
            for (int nMap=0; nMap<allAtomMaps.size(); nMap++)
            {
                if (!allAtomMaps.get(nMap).containsKey(candRef))
                    continue;

                counter++;

                if (nMap==0)
                {
                    //So, the atom on refMol has one partner in fitMol
                    candFit = allAtomMaps.get(0).get(candRef);
                } else {
                    if(!candFit.equals(allAtomMaps.get(nMap).get(candRef)))
                    {
                        //but, there is another atom in fitMol that matches
                        // so the candidate on refMol is rejected by stopping
                        // the loop (later we check if the loo run all the way
                        break;
                    }
                }
            }

            if (counter == allAtomMaps.size())
            {
                fountGoodPairOfSeeds = true;
                seedRef = candRef;
                seedFit = candFit;
                break;
            }
        }
        if (!fountGoodPairOfSeeds)
        {
            logger.trace("Cannot improve the atom-atom map (a).");
            return null;
        }

        //Get the correspondence
        List<IAtom> lstRef = new ArrayList<IAtom>();
        List<IAtom> lstFit = new ArrayList<IAtom>();
        explorePairOfMols(seedRef, seedFit, refMol, fitMol, lstRef, lstFit, 
        		allAtomMaps, logger);

        //Check correctness of sizes
        if (lstRef.size() == 0 || lstRef.size() != lstFit.size())  
        {
            logger.trace("Cannot improve the atom-atom map (b).");
            return null;
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
     * Explore two molecules starting from a seed atom and following the 
     * connectivity.
     * @param sA seed atom for <code>molA</code>
     * @param sB seed atom for <code>molB</code>
     * @param molA molecule A
     * @param molB molecule B
     * @param lstA list of already visited atoms in <code>molA</code>
     * @param lstB list of already visited atoms in <code>molB</code>
     * @param allAtomMaps collection of atom mappings to search for best
     * mapping.
     */

    private static void explorePairOfMols(IAtom sA, IAtom sB, 
                                   IAtomContainer molA, IAtomContainer molB, 
                                   List<IAtom> lstA, List<IAtom> lstB,
                                   List<Map<IAtom,IAtom>> allAtomMaps, 
                                   Logger logger)
    {
        logger.trace("Exploring mols: pair "
                        + MolecularUtils.getAtomRef(sA,molA) + " - "
                        + MolecularUtils.getAtomRef(sB,molB));
        
        //store seeds
        if (lstA.contains(sA))
        {
            logger.warn("WARNING! LstA already contains qA = "  
                                + MolecularUtils.getAtomRef(sA,molA));
        }
        lstA.add(sA);

        if (lstB.contains(sB))
        {
            logger.warn("WARNING! LstB already contains qB = " 
                                + MolecularUtils.getAtomRef(sB,molB));
        }
        lstB.add(sB);

        //Get connected atoms
        List<IAtom> nbrsA = molA.getConnectedAtomsList(sA);
        List<IAtom> nbrsB = molB.getConnectedAtomsList(sB);
        
        for (int i=0; i<nbrsA.size(); i++)
        {
            IAtom qA = nbrsA.get(i);

            //skip if already done
            if (lstA.contains(qA))
                continue;

            logger.trace("Attempt to find atom in molB "
                                + "corresponding to " 
                                + MolecularUtils.getAtomRef(qA,molA)
                                + " in molA.");

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
                	continue;
                }

                //Connectivity condition
                if (!nbrsB.contains(candidateInB))
                {
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
                String msg = "No good candidate found to proceed in this "
                		+ "direction. Proposed atom of molB were: ";
                for (IAtom a : proposals)
                {
                    if (a == null)
                    	msg = msg + "null;";
                    else
                    	msg = msg + MolecularUtils.getAtomRef(a,molB) + ";";
                }
                logger.trace(msg);
                continue;
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
                    String w = "WARNING! More than one atom satisfy "
                    		+ "the condition! Atoms that map " 
                    		+ MolecularUtils.getAtomRef(qA,molA) + " are "
                    		+ MolecularUtils.getAtomRef(qB,molB) + " and "
                    		+ MolecularUtils.getAtomRef(candidateInB, molB);
                    logger.warn(w);
                } else {
                    qB = candidateInB;
                    alreadyFound = true;
                    break;
                }
            }
            
            //Recursion on the stored atoms
            if (alreadyFound)
            {
                explorePairOfMols(qA, qB, molA, molB, lstA, lstB, allAtomMaps,
                		logger);
            }
        }
    }

//------------------------------------------------------------------------------

      /**
       * Calculates the Root Mean Square Deviation of Intramolecular Distances.
       * We compare two vectors of atoms calculating all 
       * inter-atomic distances (IAD) within each vector and consider the Root
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
      * Calculates the Root Mean Square Deviation of Intramolecular Distances.
      * We compare two vectors of atoms calculating all 
      * inter atomic distances (IAD) within each vector and considering the Root
      * Mean Square of the Distance IAD(a) - IAD(b).
      * @param lstA first vector of atoms 
      * @param lstB second vector of atoms
      * @return the value of Root Mean Square Deviation of Intramolecular 
      *         Distances (RMSDIAD)
      */
      
    public static double getRMSDevIntramolecularDistances(IAtom[] lstA, 
    		  IAtom[] lstB)
    {
        double result = 0.0d;

        //Check if conditions for running this calculation are satisfied
        if (lstA.length != lstB.length)
        {
            Terminator.withMsgAndStatus("ERROR! Attempt to calculate RMSD "
                        + " of structures with different number of atoms",-1);
        }
          
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
     * @return the root mean square distance from <code>KabschAlignment</code>.
     */

    public static double alignMolsWithMapping(IAtomContainer molA,
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
            sa = new KabschAlignment(lstA, lstB);
            sa.align();
        }
        catch (Throwable t)
        {
            Terminator.withMsgAndStatus("ERROR! KabschAlignment failed.", -1, t);
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
    
//-----------------------------------------------------------------------------

}
