package autocompchem.molecule.conformation;

import java.util.ArrayList;

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

import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.silent.RingSet;

import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.conformation.ConformationalCoordinate.ConformationalCoordType;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;


/**
 * The conformational space as the combination of a list of conformational
 * changes (i.e., the conformational coordinates).
 * 
 * @author Marco Foscato 
 */
//TODO-gg replace by ConformationalSpaceGenerator
public class ConformationalSpaceBuilder
{

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ConformationalSpace
     */

    public ConformationalSpaceBuilder()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Generates the {@link ConformationalSpace} of a specific molecule using
     * the constitutional details of the permitted conformational changes.
     * @param mol the molecular system
     * @param moves the definition of permitted conformational changes
     * @param verbosity verbosity level
     * @return the conformational space
     */

    public static ConformationalSpace makeConfSpace(IAtomContainer mol, 
                                            ConformationalMovesDefinition moves,
                                                                  int verbosity)
    {
        Map<String,String> smarts = moves.getSMARTSQueries();

        //Match all queriea
        ManySMARTSQuery msq = null;
        msq = new ManySMARTSQuery(mol,smarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! Cannot match SMARTS "
                                                        + "queries" + cause,-1);
        }

        return makeConfSpace(mol,moves,msq,verbosity);
    }

//------------------------------------------------------------------------------

    /**
     * Generates the {@link ConformationalSpace} of a specific molecule using
     * the constitutional details of the permitted conformational changes
     * and a pregenerates list of matches for the SMARTS queries contained 
     * in the definition of the <code>moves</code>.
     * @param mol the molecular system
     * @param moves the definition of permitted conformational changes
     * @param msq a SMARTS matcher that, maybe among other things, contains the 
     * matches for the conformational moves' definitions
     * @param verbosity verbosity level
     * @return the conformational space
     */

    public static ConformationalSpace makeConfSpace(IAtomContainer mol,
                                            ConformationalMovesDefinition moves,
                                                            ManySMARTSQuery msq,                                                                  int verbosity)

    {
        //Perceive bonds belonging to cycles
        SpanningTree spanTree = new SpanningTree(mol);
        IRingSet rings = new RingSet();
        try
        {
            rings = spanTree.getAllRings();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Unable to detect all rings for "
                + "molecule '" + MolecularUtils.getNameOrID(mol) + "'. Cause: "
                + t.getCause(),-1);
        }

        //Generate the conformational space
        ConformationalSpace confSpace = new ConformationalSpace();
        for (String refName : moves.refNames())
        {
            if (verbosity > 1)
            {
                System.out.println("Setting conformational moves from SMARTS "
                          + " rule " + moves.getSMARTS(refName)+ ".");
            }

            //Prepare the options for this type of conformational coordinate
            ArrayList<String> opts = moves.getOpts(refName);
            int nFold = 0;
            if (moves.getOpts(refName).size() == 1)
            {
                try
                {
                    nFold = Integer.parseInt(moves.getOpts(refName).get(0));
                }
                catch (Throwable t)
                {
                    nFold = 0;
                }
            }
            

            //TODO: add any further processing of options here
     

            //Put all matches in one list
            if (msq.getNumMatchesOfQuery(refName) == 0)
            {
                if (verbosity > 1)
                {
                    System.out.println("WARNING! No match for conformational "
                          + "move definition " + moves.getSMARTS(refName)+ ".");
                }
                continue;
            }
            List<List<Integer>> matches = msq.getMatchesOfSMARTS(refName);
            for (List<Integer> oneMatch : matches)
            {
                int n = oneMatch.size();
                if (n > 2)
                {
                    Terminator.withMsgAndStatus("ERROR! Found SMARTS matching "
                        + "more than two atoms while defining conformational "
                        + "moves. Check SMARTS '" + moves.getSMARTS(refName)
                        + "'.",-1);
                }

                //Defintition of the coordinante in terms of IAtom
                ArrayList<IAtom> atomDef = new ArrayList<IAtom>();
                for (Integer id : oneMatch)
                {
                    atomDef.add(mol.getAtom(id));
                }
                if (verbosity > 2)
                {
                    String msg = "Looking at ConformationalCoord on atom";
                    if (n > 1)
                    {
                        msg = msg + "s";
                    }
                    msg = msg + ": ";
                    for (IAtom atm : atomDef)
                    {
                        msg = msg + MolecularUtils.getAtomRef(atm,mol) + " ";
                    }
                    System.out.println(msg);
                }

                //Coordination type dependent features (type, value, fold)
                ConformationalCoordType coordType = 
                		ConformationalCoordType.UNDEFINED;
                double coordValue = 0.0;
                boolean skip = false;
                switch (n)
                {
                   case 1:
                        coordType = ConformationalCoordType.FLIP;
                        //For flipping atoms value is either + or -
                        coordValue = 1.0;
                        break;

                   case 2:
                        coordType = ConformationalCoordType.TORSION;
                        IAtom atm0 = atomDef.get(0);
                        IAtom atm1 = atomDef.get(1);
                        IAtom nextTo0 = getNextTo(atomDef,0,mol);
                        IAtom nextTo1 = getNextTo(atomDef,1,mol);
                        if (nextTo0 == null || nextTo1 == null ||
                            !mol.getConnectedAtomsList(atm0).contains(atm1))
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Ignoring terminal bond "
                                    + MolecularUtils.getAtomRef(atm0,mol) + "-"
                                    + MolecularUtils.getAtomRef(atm1,mol));
                            }
                            skip = true;
                        }
                        IBond bnd = mol.getBond(atm0,atm1);
                        IRingSet rs = rings.getRings(bnd);
                        if (rs.getAtomContainerCount() != 0)
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Ignoring cyclic bond "
                                    + MolecularUtils.getAtomRef(atm0,mol) + "-"
                                    + MolecularUtils.getAtomRef(atm1,mol));
                            }
                            skip = true;
                        }
                        coordValue = MolecularUtils.calculateTorsionAngle(
                                                     nextTo0,atm0,atm1,nextTo1);
                        break;
                }

                if (skip)
                {
                    continue;
                }

                String coordName = confSpace.getUnqCoordName();
                ConformationalCoordinate newCoord = 
                                        new ConformationalCoordinate(coordName,
                                                                     coordType,
                                                                       atomDef,
                                              new ArrayList<Integer>(oneMatch),
                                                                    coordValue,
                                                                         nFold);
                if (!confSpace.contains(newCoord))
                {
                    if (verbosity > 2)
                    {
                        System.out.println("Added to ConformationalSpace.");
                    }
                    confSpace.add(newCoord);
                }
                else
                {
                    if (verbosity > 2)
                    {
                        System.out.println("Not a new conformationa coord.");
                    }
                }
            }
        }        
        return confSpace;
    }

//------------------------------------------------------------------------------

    /**
     * Utility method to pick an atom bounded to one in the given pair, but not
     * belonging to the same atom pair. This is done to define a torsion angle
     * along a bond having only the identity of the two central atoms.
     * @param pair the bonded pair of atoms
     * @param id the id of the central atom of which neighbour is to be found
     * @param mol the molecular system
     * @return null if there is no possible solution
     */
    
    private static IAtom getNextTo(List<IAtom> pair, int id, IAtomContainer mol)
    {
        int otherId = 0;
        if (id == 0)
        {
            otherId = 1;
        }
        IAtom result = null;
        List<IAtom> nbrs = mol.getConnectedAtomsList(pair.get(id));
        for (IAtom nbr : nbrs)
        {
            if (nbr != pair.get(otherId))
            {
                result = nbr;
                break;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

}
