package autocompchem.molecule;

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

import javax.vecmath.Point3d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import autocompchem.atom.AtomUtils;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.run.Job;
import autocompchem.utils.NumberUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to add, edit, or remove one or more atoms.
 * 
 * @author Marco Foscato
 */

public class AtomEditor extends AtomTupleGenerator
{
    /**
     * String defining the task of mutating bonds.
     */
    public static final String EDITATOMSTASKNAME = "editAtoms";

    /**
     * Task about mutating atoms
     */
    public static final Task EDITATOMSTASK;
    static {
    	EDITATOMSTASK = Task.make(EDITATOMSTASKNAME);
    }

    /**
     * String defining the task of mutating bonds.
     * Redundant, but defined to ensure visibility of the task.
     */
    public static final String REMOVEATOMSTASKNAME = "removeAtoms";

    /**
     * Task about removing atoms
     */
    public static final Task REMOVEATOMSTASK;
    static {
    	REMOVEATOMSTASK = Task.make(REMOVEATOMSTASKNAME);
    }

    /**
     * String defining the task of adding an atom
     * Redundant, but defined to ensure visibility of the task.
     */
    public static final String ADDATOMSTASKNAME = "addAtoms";

    /**
     * Task about adding an atom
     */
    public static final Task ADDATOMSTASK;
    static {
    	ADDATOMSTASK = Task.make(ADDATOMSTASKNAME);
    }

	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "AtomEditRule-";

    /**
     * Keyword used to define how to add an atom
     */
    public static final String KEYADDATOM = "ADDATOM";
   
    /**
     * Keyword used to identify the imposed elemental symbols.
     */
    public static final String KEYELEMENT = "ELEMENT";
   
    /**
     * Value-less keyword used to identify atoms to remove.
     */
    public static final String KEYREMOVE = "REMOVE";

    /**
     * List of mutually exclusive attributes.
     */
    public static final List<String> MUTUALLYEXCLUSIVEATTRIBUTES = Arrays.asList(
        KEYELEMENT, KEYADDATOM, KEYREMOVE);

    /**
     * Keyword used to define the position of the new atom 
     * from internal coordinates of the reference atoms
     */
    public static final String KEYATOMATIC = "INTERNALCOORDS";

    /**
     * Keyword used to define the position of the new atom at the centroid
     * of a set of atoms.
     */
    public static final String KEYATOMATCENTROID = "CENTROID";

    /**
     * List of keywords that can be used to define the position of the new atom.
     */
    public static final List<String> KEYWORDSFORATOMPOSITION = Arrays.asList(
        KEYATOMATIC, KEYATOMATCENTROID);

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomEditor()
    {
        ruleRoot = BASENAME;
        valuedKeywords.add(KEYELEMENT);
        valuedKeywords.add(KEYADDATOM);
        valuelessKeywords.add(KEYREMOVE);
    }

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(EDITATOMSTASK, ADDATOMSTASK, REMOVEATOMSTASK)));
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
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
        List<AnnotatedAtomTuple> tuples = createTuples(iac, rules, null,
            AtomTupleGenerator.Mode.TUPLES);

        // Redundancy of task name is meant to ensure visibility of all tasks
        // in the list AutoCompChem tasks.
      	if (task.equals(EDITATOMSTASK) 
            || task.equals(ADDATOMSTASK) 
            || task.equals(REMOVEATOMSTASK))
      	{
      		editAtoms(iac, tuples);
      	} else {
      		dealWithTaskMismatch();
        }
      	return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Edit atoms in a container.
     * @param iac the container to edit.
     * @param tuples the tuples of atoms that define what and where to edit atoms. 
     * The attributes of the tuples define the details in this way: 
     * - if the attribute KEYELEMENT is present, the elemental symbol of the all
     * atoms in the tuple is changed to the value of the attribute.
     * - if the attribute KEYREMOVE is present, all the atoms in the tuple are 
     * removed.
     * - if the attribute KEYADDATOM is present, a new atom is added at the 
     * position of the tuple. The value of the attribute is the elemental symbol 
     * of the new atom.
     * The three cases are mutually exclusive. An exception is thrown if more 
     * than one of the three attributes is present.
     */

    public static void editAtoms(IAtomContainer iac, List<AnnotatedAtomTuple> tuples)
    {
    	Logger logger = LogManager.getLogger();
    	
    	Set<IAtom> toRemove = new HashSet<IAtom>();
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
            int numExclusiveAttributes = 0;
            for (String attribute : MUTUALLYEXCLUSIVEATTRIBUTES)
            {
                if (tuple.hasValuelessAttribute(attribute))
                {
                    numExclusiveAttributes++;
                }
            }
            if (numExclusiveAttributes > 1)
            {
                throw new IllegalArgumentException("More than one mutually "
                    + "exclusive attribute is present in tuple '" 
                    + tuple.toString() + "'. This is not allowed.");
            }

    		if (tuple.hasValuelessAttribute(KEYREMOVE))
    		{
                for (int atmIdx : tuple.getAtomIDs())
                {
                    // NB: we remove al last operation to avoid messing with the atom indices.
                    toRemove.add(iac.getAtom(atmIdx));
                }
    		} else if (tuple.hasValuedAttribute(KEYELEMENT)) 
            {
                String newElSymbol = tuple.getValueOfAttribute(KEYELEMENT);
                for (int atmIdx : tuple.getAtomIDs())
                {
                    IAtom atm = iac.getAtom(atmIdx);
                    String msg = "Mutated atom " + MolecularUtils.getAtomRef(atm, iac)
                        + " from " + atm.getSymbol() + " to " + newElSymbol;
                    mutateElement(iac, atm, newElSymbol);
                    logger.debug(msg);
                }
            } else if (tuple.hasValuedAttribute(KEYADDATOM))
            {
                addAtom(iac, tuple);
            } else {
                throw new IllegalArgumentException("Expecting one of '"
                    + MUTUALLYEXCLUSIVEATTRIBUTES.toString() 
                    + "' but found none in tuple '" 
                    + tuple.toString() + "'. This is not allowed.");
            }
    	}
    	
  		for (IAtom atm : toRemove)
  		{
  			iac.removeAtom(atm);

            String msg = "Removed atom " + MolecularUtils.getAtomRef(atm, iac);
            logger.debug(msg);
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

    /**
     * Adds an atom to the given atom container.
     * @param iac the atom container to add the atom to.
     * @param tuple the tuple of atoms that define the atom to add.
     */
    public static void addAtom(IAtomContainer iac, AnnotatedAtomTuple tuple)
    {
        // Get the reference atoms used to define the position of the new atom.
        IAtom[] referenceAtoms = new IAtom[tuple.getNumberOfIDs()];
        for (int i=0; i<tuple.getNumberOfIDs(); i++)
        {
            referenceAtoms[i] = iac.getAtom(tuple.getAtomIDs().get(i));
        }

        String valueOfKeyword = tuple.getValueOfAttribute(KEYADDATOM);
        String[] words = valueOfKeyword.trim().split("\\s+");  

        if (words.length < 2)
        {
            throw new IllegalArgumentException("Value of '" + KEYADDATOM 
                + "' keyword must start with the elemental symbol "
                + "followed by one among " 
                + KEYWORDSFORATOMPOSITION.toString()
                + ". Found '" + valueOfKeyword + "'.");
        }
        
        String newSymbol = words[0];

        Map<IAtom,Object[]> bondedAtoms = new HashMap<IAtom,Object[]>();
        switch (words[1].toUpperCase()) {
            case KEYATOMATIC:
                InternalCoord[] internalCoords = new InternalCoord[referenceAtoms.length];
                if (words.length != 4 && words.length != 6 && words.length != 8)
                {
                    throw new IllegalArgumentException("Value of '" + KEYATOMATIC 
                        + "' keyword must be followed by up to 3 pairs of index and value'."
                        + ". Found only " + (words.length - 2) + " words instead of 6.");
                }
                int numPairs = (words.length - 2) / 2;
                int[] indexes = new int[numPairs];
                double[] values = new double[numPairs];
                for (int iPair=0; iPair<numPairs; iPair++)
                {
                    int i = 2 + iPair * 2;
                    if (!NumberUtils.isParsableToInt(words[i]))
                    {
                        throw new IllegalArgumentException("Value of '" + KEYATOMATIC 
                            + "' keyword must be followed by space-separated pairs 'index value'."
                            + ". Could not parse '" + words[i] + "' as index.");
                    }
                    indexes[iPair] = Integer.parseInt(words[i]);
                    if (!NumberUtils.isParsableToDouble(words[i+1]))
                    {
                        throw new IllegalArgumentException("Value of '" + KEYATOMATIC 
                            + "' keyword must be followed by space-separated pairs 'index value'."
                            + ". Could not parse '" + words[i+1] + "' as value.");
                    }
                    values[iPair] = Double.parseDouble(words[i+1]);
                }
                internalCoords[0] = new InternalCoord("distance", values[0], 
                    new ArrayList<Integer>(Arrays.asList(-1, indexes[0])));
                if (numPairs > 1)
                {
                    internalCoords[1] = new InternalCoord("angle", values[1], 
                        new ArrayList<Integer>(Arrays.asList(-1, indexes[0], indexes[1])));
                    if (numPairs > 2)
                    {
                        internalCoords[2] = new InternalCoord("torsion", values[2], 
                            new ArrayList<Integer>(Arrays.asList(-1, indexes[0], indexes[1], indexes[2])));
                    }
                }

                addAtom(iac, referenceAtoms, internalCoords, newSymbol, bondedAtoms);
                break;

            case KEYATOMATCENTROID:
                IAtom[] centroidAtoms = new IAtom[4];
                if (words.length > 2)
                {
                    centroidAtoms = new IAtom[words.length - 2];
                    for (int i=2; i<words.length; i++)
                    {
                        if (!NumberUtils.isParsableToInt(words[i]))
                        {
                            throw new IllegalArgumentException("Value of '" + KEYATOMATCENTROID 
                                + "' keyword must be followed by a space-separated list of atom indexes."
                                + ". Could not parse '" + words[i] + "' as index.");
                        }
                        centroidAtoms[i-2] = referenceAtoms[Integer.parseInt(words[i])];
                    }
                } else {
                    centroidAtoms = referenceAtoms;
                }
                Point3d centroid = MolecularUtils.calculateCentroid(centroidAtoms);
                addAtom(iac, newSymbol, centroid, bondedAtoms);
                break;
            default:
                throw new IllegalArgumentException("Value of '" + KEYADDATOM 
                + "' keyword must start with one among " 
                + KEYWORDSFORATOMPOSITION.toString()
                + ". Found '" + words[1] + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Adds an atom to the given atom container.
     * @param iac the atom container to add the atom to.
     * @param referenceAtoms the reference atoms used to define the position of the new atom.
     * @param internalCoords the internal coordinate templates used to calculate 
     * the position of the new atom. The index of the new atom is expected to the
     * '-1' and be the first position in the list of indexes.
     * @param newElSymbol the elemental symbol of the new atom.
     * @param bondedAtoms the map of atoms bonded to the new atom and the 
     * properties of the new bond to be created, namely {@Link IBond.Order} and 
     * {@Link IBond.Stereo}.
     */
    public static void addAtom(IAtomContainer iac, IAtom[] referenceAtoms, 
        InternalCoord[] internalCoords, String newElSymbol, 
        Map<IAtom,Object[]> bondedAtoms)
    {
        Point3d newAtomPosition = MolecularUtils.calculateAtomPosition(
            referenceAtoms, internalCoords);
        addAtom(iac, newElSymbol, newAtomPosition, bondedAtoms);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an atom to the given atom container.
     * @param iac the atom container to add the atom to.
     * @param newElSymbol the elemental symbol of the new atom.
     * @param newAtomPosition the position of the new atom.
     * @param bondedAtoms the map of atoms bonded to the new atom and the 
     * properties of the new bond to be created, namely {@Link IBond.Order} and 
     * {@Link IBond.Stereo}.
     */
    public static void addAtom(IAtomContainer iac, String newElSymbol, 
        Point3d newAtomPosition, Map<IAtom,Object[]> bondedAtoms)
    {
    	Logger logger = LogManager.getLogger();
        IAtom newAtm = AtomUtils.makeIAtom(newElSymbol, newAtomPosition);
        iac.addAtom(newAtm);

        String msg = "Added atom " + MolecularUtils.getAtomRef(newAtm, iac)
            + " at " + newAtomPosition.toString();
        logger.debug(msg);
        
        for (IAtom bondedAtm : bondedAtoms.keySet())
        {
            Object[] bondProperties = bondedAtoms.get(bondedAtm);
            if (bondProperties.length != 2)
            {
                throw new IllegalArgumentException("Bond properties must "
                    + "be an array of length 2. Found " 
                    + bondProperties.length + ".");
            }
            if (!(bondProperties[0] instanceof IBond.Order))
            {
                throw new IllegalArgumentException("Bond property 0 must "
                    + "be an instance of IBond.Order. Found " 
                    + bondProperties[0].getClass().getName() + ".");
            }
            if (!(bondProperties[1] instanceof IBond.Stereo))
            {
                throw new IllegalArgumentException("Bond property 1 must "
                    + "be an instance of IBond.Stereo. Found " 
                    + bondProperties[1].getClass().getName() + ".");
            }
            IBond newBnd = new Bond(newAtm, bondedAtm, 
                (IBond.Order) bondProperties[0],
                (IBond.Stereo) bondProperties[1]);
            iac.addBond(newBnd);

            String msg2 = "Added bond between " + MolecularUtils.getAtomRef(newAtm, iac)
                + " and " + MolecularUtils.getAtomRef(bondedAtm, iac)
                + " with order " + bondProperties[0].toString()
                + " and stereo " + bondProperties[1].toString();
            logger.debug(msg2);
        }
    }

//------------------------------------------------------------------------------

}
