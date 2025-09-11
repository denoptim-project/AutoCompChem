package autocompchem.molecule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Bond;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import autocompchem.atom.AtomUtils;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.run.Terminator;

/**
 * Toolbox for molecular objects
 * 
 * @author Marco Foscato
 */

public class MolecularUtils
{

    //TODO move to constants
    private static String duSymbol = "Du";
    //TODO move to constants
    @SuppressWarnings("unused")
        private static double linearBendThld = 175.0;
    
	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
	private static Logger logger = LogManager.getLogger(MolecularUtils.class);


//------------------------------------------------------------------------------

    /**
     * Counts the number of atoms for each element in the system and return
     * a map of ElementSymbol:NumberOfAtoms
     * @param mol the molecule to analyse
     * @return the map of ElementSymbol:NumberOfAtoms
     */

    public static  Map<String,Integer> getMolecularFormulaOfAtomContainer(
                                                        IAtomContainer mol)
    {
        Map<String,Integer> formula = new HashMap<String,Integer>();
        for (IAtom atm : mol.atoms())
        {
            String elSymbol = atm.getSymbol();
            if (formula.keySet().contains(elSymbol))
            {
                int num = formula.get(elSymbol) + 1;
                formula.put(elSymbol,num);
            } else {
                formula.put(elSymbol,1);
            }
        }
        return formula;
    }

//------------------------------------------------------------------------------

      /**
       * Get the string version of a molecular formula using CDK engine.
       * @param mol the molecule to analyse
       * @return the molecular formula
       */

      public static String getMolecularFormula(IAtomContainer mol)
      {
    	  return MolecularFormulaManipulator.getString(
			MolecularFormulaManipulator.getMolecularFormula(mol));
      }	

//------------------------------------------------------------------------------

    /**
     * Returns a string with the element symbol and the atom number (1-n)
     * of the given atom
     * @param atm the atom for which to produce the id-string
     * @param mol the molecule containing <code>atm</code>
     * @return the string for identifying the atom in the molecule
     */

    public static String getAtomRef(IAtom atm, IAtomContainer mol)
    {
        String ref = atm.getSymbol() + (mol.indexOf(atm) +1);
        return ref;
    }

//------------------------------------------------------------------------------

    /**
     * Tool for detecting the smallest ring involving two atoms
     * @param atmS first atom involved (seed of the search)
     * @param atmT second atom involved 
     * @param mol the molecular object
     * @return the size of the ring or -1 if no ring has been found
     */

    public static int getSmallestRing(IAtom atmS, IAtom atmT, 
                                                        IAtomContainer mol)
    {
    	logger.trace("Looking for ring involving "
                        +getAtomRef(atmS, mol)
                        +" and "
                        +getAtomRef(atmT, mol));
        int size=-1;

        boolean moreTrials = true;
        boolean first = true;
        int level = 0;

        Map<IAtom,Integer> visited = new HashMap<IAtom,Integer>();
        visited.put(atmS,level);
        while (moreTrials)
        {
            // Get atoms from previous level
            List<IAtom> inLevel = new ArrayList<IAtom>();
            for (IAtom a : visited.keySet())
                if (visited.get(a) == level)
                    inLevel.add(a);

            // Get atoms in next level
            level++;
            List<IAtom> nextLevel = new ArrayList<IAtom>();
            for (IAtom sfpl : inLevel)  //sfpl: seed from previous level
            {
                List<IAtom> neighbours = mol.getConnectedAtomsList(sfpl);
                //in case it's a dummy ingore and move on
                for (IAtom nbr : neighbours)
                {
                    if (nbr.getSymbol().equals(duSymbol))
                    {
                        List<IAtom> neighboursOfDu = 
                                                mol.getConnectedAtomsList(nbr);
                        visited.put(nbr,-1);
                        nextLevel.remove(nbr);
                        //remove possible duplicates
                        nextLevel.removeAll(neighboursOfDu);
                        //add new candidates
                        nextLevel.addAll(neighboursOfDu);
                    }
                }
                nextLevel.removeAll(neighbours); //remove possible duplicates
                nextLevel.addAll(neighbours); //add new candidates
            }
            nextLevel.removeAll(visited.keySet());
            if (nextLevel.size() == 0 )
            {
                moreTrials = false;
            } else {
                for (IAtom nbr : nextLevel)
                {
                    if (nbr == atmT)
                    {
                        if (!first)
                        {
                            size = level + 1;
                            moreTrials = false;
                            logger.trace("Found target: "
                                        + getAtomRef(nbr,mol)
                                        + " - Ring size: " + size);
                        } else {
                            first = false;
                        }
                    } else {
                        visited.put(nbr,level);
                    }
                }
            }
        }

        return size;
    }
//------------------------------------------------------------------------------

    /**
     * Evaluates the need of changing the bond orders to match aromaticity
     * @param mol the molecule to analyse
     * @return the cause of the errors if any, or an empty string
     */

    public static String missmatchingAromaticity(IAtomContainer mol)
    {
        String cause = "";
        for (IAtom atm : mol.atoms())
        {
            //Check of carbons with or without aromatic flags
            if (atm.getSymbol().equals("C")) 
            {
                if (atm.getFormalCharge() == 0)
                {
                    if (mol.getConnectedBondsCount(atm) == 3)
                    {
                        if (atm.getFlag(CDKConstants.ISAROMATIC))
                        {
                            int n = numOfBondsWithBO(atm,mol,"DOUBLE");
                            if (n == 0)
                            {
                                cause = "Aromatic atom " + 
                                        getAtomRef(atm,mol) + 
                                        " has 3 connected atoms but " +
                                        "no double bonds";
                                return cause;
                            }
                        } else {
                            for (IAtom nbr : mol.getConnectedAtomsList(atm))
                            {
                                if (nbr.getSymbol().equals("C"))
                                {
                                    if (nbr.getFormalCharge() == 0)
                                    {
                                        if (mol.getConnectedBondsCount(nbr) ==3)
                                        {
                                            int nNbr = numOfBondsWithBO(nbr,
                                                                mol,"SINGLE");
                                            int nAtm = numOfBondsWithBO(atm,
                                                                mol,"SINGLE");
                                            if ((nNbr == 3) && (nAtm == 3))
                                            {
                                                cause = "Connected atoms " +
                                                        getAtomRef(atm,mol) +
                                                        " " + 
                                                        getAtomRef(nbr,mol) + 
                                                        " have 3 connected " +
                                                        "atoms but no double "+
                                                        "bond. They are " +
                                                        "likely to be " +
                                                        "aromatic but no " +
                                                        "aromaticity has " +
                                                        "been reported";
                                                return cause;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return cause;
    }

//------------------------------------------------------------------------------
   
    /**
     * Returns the number of bonds, with a certain bond order, surrounding an 
     * atom
     * @param atm the target atom
     * @param mol the molecule containing <code>atm</code>
     * @param ord bond order to be searched
     * @return the number of bonds
     */

    public static int numOfBondsWithBO(IAtom atm, IAtomContainer mol, 
                                                                     String ord)
    {
        int res = -1;
        int n = 0;
        for (IBond bnd : mol.getConnectedBondsList(atm))
        {
            if (bnd.getOrder() == IBond.Order.valueOf(ord))
                n++;
        }

        res = n;
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Get a vector from a source atom to a target atom
     * @param srcAtom the source atom
     * @param trgAtom the target atom
     * @return the vector 
     */

    public static Vector3d getVectorFromTo(IAtom srcAtom, IAtom trgAtom)
    {
        Point3d ps = AtomUtils.getCoords3d(srcAtom);
        Point3d pt = AtomUtils.getCoords3d(trgAtom);
        Vector3d v3d = new Vector3d(ps.x-pt.x, ps.y-pt.y, ps.z-pt.z);
        return v3d;
    }

//-----------------------------------------------------------------------------

    /**
     * Determines the dimensionality of the chemical object submitted
     * @param mol input molecular/chemical object
     * @return dimensionality of this object (2 or 3) or -1
     */

    public static int getDimensions(IAtomContainer mol)
    {
        final int is2D = 2;
        final int is3D = 3;
        final int not2or3D = -1;

        int numOf2D = 0;
        int numOf3D = 0;

        for (IAtom atm : mol.atoms())
        {
            Point2d p2d = new Point2d();
            Point3d p3d = new Point3d();
            p2d = atm.getPoint2d();
            boolean have2D = true;
            if (p2d == null)
            {
                have2D = false;
                p3d = atm.getPoint3d();
                if (p3d == null)
                {
                    return not2or3D;
                }
            }
            ArrayList<Double> pointer = new ArrayList<Double>();
            try {
                if (have2D)
                {
                      pointer.add(p2d.x);
                    pointer.add(p2d.y);
                    numOf2D++;
                } else {
                    pointer.add(p3d.x);
                    pointer.add(p3d.y);
                    pointer.add(p3d.z);
                    numOf3D++;
                }
            } catch (Throwable t) {
                return not2or3D;
            }
        }

        if (numOf2D == mol.getAtomCount())
            return is2D;
        else if (numOf3D == mol.getAtomCount())
            return is3D;
        else
            return not2or3D;
    }

//------------------------------------------------------------------------------

    /**
     * Check the IAtomContainer has a property and the value is not null
     * @param mol the molecule
     * @param propName the name of the property to be checked
     * @return <code>true</code> if the property is not null
     */

    public static boolean hasProperty(IAtomContainer mol, String propName)
    {
        Object propValue = null;
        try {
            propValue = mol.getProperty(propName);
        } catch (Throwable tsm) {
            return false;
        }

        if (propValue == null)
            return false;

        return true;
    }

  //-----------------------------------------------------------------------------

    /**
     * Looks for a property referring to the name or ID of the molecule.
     * Recognized cdk:Title,ChEBI ID,TTD DRUGID.
     * @param mol molecule
     * @return the name or ID if any. Otherwise <code>null</code>.
     */

    public static String getNameIDOrNull(IAtomContainer mol)
    {
    	return getNameOrID(mol, null);
    }
    
 //-----------------------------------------------------------------------------

    /**
     * Looks for a property referring to the name or ID of the molecule.
     * Recognized cdk:Title,ChEBI ID,TTD DRUGID.
     * @param mol molecule
     * @return the name or ID if any. Otherwise, 'noname'.
     */

    public static String getNameOrID(IAtomContainer mol)
    {
    	return getNameOrID(mol, "noname");
    }
    
//-----------------------------------------------------------------------------

    /**
     * Looks for a property referring to the name or ID of the molecule.
     * Recognized cdk:Title,ChEBI ID,TTD DRUGID.
     * @param mol molecule
     * @param defaultName the default value to give to the result.
     * @return the name or ID if any common way to name the atom container is
     * used. Otherwise, the value given as default will
     * be returned.
     */

    public static String getNameOrID(IAtomContainer mol, String defaultName)
    {
    	String name = mol.getTitle();
    	if (name==null || name.equals("") || name.isBlank())
    	{
    		name = defaultName;
	        //ChEBI
	        try {
	            name = mol.getProperty("ChEBI ID").toString();
	        } catch (Throwable t1) {
	            //TTD
	            try {
	                name = mol.getProperty("DRUGID").toString();
	            } catch (Throwable t2) {
	                //CDK
	                try {
	                    name = mol.getProperty("cdk:Title").toString();
	                } catch (Throwable t3) {
	                    //General case using title
	                    try {
	                        name = mol.getProperty(CDKConstants.TITLE).toString();
	                    } catch (Throwable t) {
	                        logger.trace("Molecule name not found. Set to '" 
	                        		+ name + "'.");
	                    }
	                }
	            }
	        }
        }
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate Interatomic distance 
     * @param atmA the first atom
     * @param atmB the second atom
     * @return distance between atmA and atmB
     */

    public static double calculateInteratomicDistance(IAtom atmA, IAtom atmB)
    {
        Point3d pa = AtomUtils.getCoords3d(atmA);
        Point3d pb = AtomUtils.getCoords3d(atmB);
        double dx = pa.x - pb.x;
        double dy = pa.y - pb.y;
        double dz = pa.z - pb.z;
        double dist = Math.sqrt((Math.pow(dx,2.0D)) +
                                (Math.pow(dy,2.0D)) +
                                (Math.pow(dz,2.0D)));
        return dist;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the bond angle given the coordinates
     * @param atomLeft the first atom
     * @param atomCentre the central atom
     * @param atomRight the third atom
     * @return the bond angle in deg
     */

    public static double calculateBondAngle(IAtom atomLeft, IAtom atomCentre, 
                                                                IAtom atomRight)
    {
        double angle = 0.0;
        Point3d l3d = AtomUtils.getCoords3d(atomLeft);
        Point3d c3d = AtomUtils.getCoords3d(atomCentre);
        Point3d r3d = AtomUtils.getCoords3d(atomRight);

        double xab = l3d.x - c3d.x;
        double yab = l3d.y - c3d.y;
        double zab = l3d.z - c3d.z;
        double xcb = r3d.x - c3d.x;
        double ycb = r3d.y - c3d.y;
        double zcb = r3d.z - c3d.z;
        double rab2 = xab*xab + yab*yab + zab*zab;
        double rcb2 = xcb*xcb + ycb*ycb + zcb*zcb;
        double rabc = Math.sqrt(rab2 * rcb2);
        if (rabc != 0.0)
        {
            double cosine = (xab*xcb + yab*ycb + zab*zcb) / rabc;
            cosine = Math.min(1.0, Math.max(-1.0, cosine));
            angle = 57.29577951308232088 * Math.acos(cosine);
        }
        return angle;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the dihedral angle A-B-C-D given the atoms
     *
     * @param atmA atom in position A
     * @param atmB atom in position B
     * @param atmC atom in position C
     * @param atmD atom in position D
     * @return the dihedral angle
     */
    public static double calculateTorsionAngle(IAtom atmA, IAtom atmB, 
                                                        IAtom atmC, IAtom atmD)
    {
        Point3d pA = AtomUtils.getCoords3d(atmA);
        Point3d pB = AtomUtils.getCoords3d(atmB);
        Point3d pC = AtomUtils.getCoords3d(atmC);
        Point3d pD = AtomUtils.getCoords3d(atmD);

        double[] A = new double[] {pA.x,pA.y,pA.z};
        double[] B = new double[] {pB.x,pB.y,pB.z};
        double[] C = new double[] {pC.x,pC.y,pC.z};
        double[] D = new double[] {pD.x,pD.y,pD.z};
        double angle = 0.0D;
        double xba = B[0] - A[0];
        double yba = B[1] - A[1];
        double zba = B[2] - A[2];
        double xcb = C[0] - B[0];
        double ycb = C[1] - B[1];
        double zcb = C[2] - B[2];
        double xdc = D[0] - C[0];
        double ydc = D[1] - C[1];
        double zdc = D[2] - C[2];
        double xt = yba * zcb - ycb * zba;
        double yt = xcb * zba - xba * zcb;
        double zt = xba * ycb - xcb * yba;
        double xu = ycb * zdc - ydc * zcb;
        double yu = xdc * zcb - xcb * zdc;
        double zu = xcb * ydc - xdc * ycb;
        double rt2 = xt * xt + yt * yt + zt * zt;
        double ru2 = xu * xu + yu * yu + zu * zu;
        double rtru = Math.sqrt(rt2 * ru2);
        if (rtru != 0.0)
        {
            double cosine = (xt * xu + yt * yu + zt * zu) / rtru;
            cosine = Math.min(1.0, Math.max(-1.0, cosine));
            angle = 57.29577951308232088 * Math.acos(cosine);
            double sign = xba * xu + yba * yu + zba * zu;
            if (sign < 0.0)
            {
                angle = -angle;
            }
        }
        return angle;
    }

//-----------------------------------------------------------------------------

    /**
     * Translate integer to a CDK bond order
     * @param bndOrder number defining the bond order
     * @return <code>IBond.Order</code> corresponding to integer bond order
     */

    public static Order intToBondOrder(int bndOrder)
    {
        if (bndOrder == 1)
            return IBond.Order.valueOf("SINGLE");
        else if (bndOrder == 2)
            return IBond.Order.valueOf("DOUBLE");
        else if (bndOrder == 3)
            return IBond.Order.valueOf("TRIPLE");
        else if (bndOrder == 4)
            return IBond.Order.valueOf("QUADRUPLE");
        else if (bndOrder == 0)
            return IBond.Order.valueOf("UNSET");
        else {
            Terminator.withMsgAndStatus("ERROR! Integer '" + bndOrder 
                              + "' is not recognized ad a valid bond order",-1);
            return IBond.Order.valueOf("UNSET");
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Translate a CDK bond order to an integer number
     * @param bndOrder string-like CDK bond order
     * @return integer corresponding to CDK bond order
     */

    public static int bondorderToint(IBond.Order bndOrder)
    {
        if (bndOrder.equals(IBond.Order.valueOf("SINGLE")))
            return 1;
        else if (bndOrder.equals(IBond.Order.valueOf("DOUBLE")))
            return 2;
        else if (bndOrder.equals(IBond.Order.valueOf("TRIPLE")))
            return 3;
        else if (bndOrder.equals(IBond.Order.valueOf("QUADRUPLE")))
            return 4;
        else if (bndOrder.equals(IBond.Order.valueOf("UNSET")))
            return 0;
        else {
            Terminator.withMsgAndStatus("ERROR! Integer '" + bndOrder
                              + "' is not recognized ad a valid bond order",-1);
            return -1;
        }
   }

//-----------------------------------------------------------------------------

    /**
     * Analysis of CDK Flags over the whole AtomContainer
     * @param mol molecular system to check
     * @param flagid number identifying the flag to check
     * @return <code>true</code> if at least one flag in <code>false</code>
     **/

    public static boolean containsFalseFlag(IAtomContainer mol, int flagid)
    {
        for (IAtom a : mol.atoms())
        {
            if (!a.getFlag(flagid))
                return true;
        }
        return false;
   }

//------------------------------------------------------------------------------


    /**
     * Checks if a molecule contains the given element. Does not consider 
     * isotopes.
     * @param mol the molecule
     * @param elSymb the elemental symbol
     * @return <code>true</code> is the molecule contains one or more atoms of 
     * the given the element.
     */

    public static boolean containsElement(IAtomContainer mol, String elSymb)
    {
        boolean res = false;
        for (IAtom atm : mol.atoms())
        {
            if (atm.getSymbol().toUpperCase().equals(elSymb.toUpperCase()))
            {
                res = true;
                break;
            }
        }
        return res;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * returns the interatomic distance matrix for the given atom container
     * @param mol the atom container to analyse
     * @return the distance matrix
     */
    
    public static DistanceMatrix getInteratormicDistanceMatrix(IAtomContainer mol)
    {
        DistanceMatrix dm = new DistanceMatrix();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
        	IAtom atmI = mol.getAtom(i);
        	for (int j=i+1; j<mol.getAtomCount(); j++)
            {
            	IAtom atmJ = mol.getAtom(j);
            	double dist = calculateInteratomicDistance(atmI, atmJ);
                dm.put(i,j,dist);
            }
        }
        return dm;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Find the shortest inter-elemental distance in the given atom container.
     * @param mol the atom container
     * @param elA first elemental symbol
     * @param elBsecond elemental symbol
     * @return the shortest interatomic distance between atoms of such elements
     */
    
    public static double getMinInterelementalDistance(IAtomContainer mol, 
    		String elA, String elB)
    {
    	ArrayList<Double> vals = new ArrayList<Double>();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
        	IAtom atmI = mol.getAtom(i);
        	if (atmI.getSymbol().equals(elA))
        	{
	        	for (int j=i+1; j<mol.getAtomCount(); j++)
	            {
	            	IAtom atmJ = mol.getAtom(j);
	            	if (atmJ.getSymbol().equals(elB))
	            	{
	            		double dist = calculateInteratomicDistance(atmI, atmJ);
	            		vals.add(dist);
	            	}
	            }
        	}
        }
        Collections.sort(vals);
    	return vals.get(0);
    }

//------------------------------------------------------------------------------

    public static Set<String> getElementalSymbols(IAtomContainer mol,
    		boolean ingoreDummy)
    {
    	Set<String> list = new HashSet<String>();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
        	IAtom atm = mol.getAtom(i);
        	if (ingoreDummy && !AtomUtils.isElement(atm.getSymbol()))
        	{
        		continue;
        	}
        	else
        	{
        		list.add(atm.getSymbol());
        	}
        }
    	return list;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets all implicit atom count to zero.
     * @param mol
     */
    public static void setZeroImplicitHydrogensToAllAtoms(IAtomContainer mol) 
    {
    	for (IAtom atm : mol.atoms()) {
    		atm.setImplicitHydrogenCount(0);
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Makes sure each bond order is NOT {@link IBond.Order#UNSET}.
     * @param mol
     */
	public static void ensureNoUnsetBondOrders(IAtomContainer mol) 
	{
		try {
			Kekulization.kekulize(mol);
		} catch (CDKException e) {
			e.printStackTrace();
			Terminator.withMsgAndStatus("ERROR! Kekulization failes. "
					+ "To by-pass "
					+ "this problem, generate input structures with "
					+ "connectivity tables that do NOT use aromatic bond "
					+ "type.",-1);
		}
		
		for (IBond bnd : mol.bonds())
		{
			if (bnd.getOrder().equals(IBond.Order.UNSET)) 
			{
				String msg = "ERROR! Kekulization could not "
						+ "assign all bond orders. In particular, the bond "
						+ "order of bond involving ";
				for (IAtom atm : bnd.atoms())
				{
					msg = msg + " " + MolecularUtils.getAtomRef(atm, mol);
				}
				msg = msg + " is still UNSET. To by-pass "
						+ "this problem, generate input structures with "
						+ "connectivity tables that do NOT use aromatic bond "
						+ "type.";
				Terminator.withMsgAndStatus(msg, -1);
			}
		}
	}
	
//------------------------------------------------------------------------------

	/**
	 * Makes a simplified copy of the given {@link IAtomContainer} considering
	 * only the list of atoms as elements/labels and points in 3D, set of bonds,
	 * and container title/name, 
	 * without any further property of atoms, bonds, or of the container, and
	 * sets the atom label to be an atom tag, i.e., a string that allows to 
	 * identify the atom (e.g., "C3"). The tag is generated by concatenating
	 * the elemental symbol (or initial {@link PseudoAtom} label) with the
	 * 1-based index of the atom in the given container.
	 * @param iac the original atom container to copy and decorate with atom 
	 * tags.
	 * @return the copy of the original container with the atom tags.
	 */
	public static IAtomContainer makeSimpleCopyWithAtomTags(IAtomContainer iac)
	{
		List<String> labels = new ArrayList<String>();
		for (int iAtm=0; iAtm<iac.getAtomCount(); iAtm++)
    	{
    		labels.add(AtomUtils.getSymbolOrLabel(iac.getAtom(iAtm))+(iAtm+1));
    	}
		return makeSimpleCopyWithAtomTags(iac, labels);
	}
		
//------------------------------------------------------------------------------

	/**
	 * Makes a simplified copy of the given {@link IAtomContainer} considering
	 * only the list of atoms as points in 3D, set of bonds,
	 * and container title/name, 
	 * without any further property of atoms, bonds, or of the container, and
	 * sets the atom label to be the string found in the given list of labels.
	 * All atoms are changed into {@link PseudoAtom}.
	 * @param iac the original atom container to copy and decorate with atom 
	 * tags.
	 * @param labels the
	 * @return the copy of the original container with the atom tags.
	 */
	public static IAtomContainer makeSimpleCopyWithAtomTags(IAtomContainer iac,
			List<String> labels)
	{
		IAtomContainer iacWithTags = chemBuilder.newAtomContainer();
		for (int iAtm=0; iAtm<iac.getAtomCount(); iAtm++)
    	{
    		IAtom origAtm = iac.getAtom(iAtm);
    		String atomTag = labels.get(iAtm);
    		IAtom atmWithTag = new PseudoAtom(atomTag);
	    	Point3d p3d = AtomUtils.getCoords3d(origAtm);
	    	atmWithTag.setPoint3d(new Point3d(p3d.x, p3d.y, p3d.z));
	    	iacWithTags.addAtom(atmWithTag);
    	}
		for (IBond origBnd : iac.bonds())
		{
			IAtom[] atomsInNewBond = new IAtom[origBnd.getAtomCount()];
			for (int i=0; i<origBnd.getAtomCount(); i++)
			{
				atomsInNewBond[i] = iacWithTags.getAtom(
						iac.indexOf(origBnd.getAtom(i)));
			}
			IBond newBond = new Bond(atomsInNewBond, origBnd.getOrder());
			iacWithTags.addBond(newBond);
		}
		iacWithTags.setTitle(MolecularUtils.getNameIDOrNull(iac));
		return iacWithTags;
	}
	
//------------------------------------------------------------------------------

}
