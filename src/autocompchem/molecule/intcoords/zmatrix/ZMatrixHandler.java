package autocompchem.molecule.intcoords.zmatrix;

import java.util.ArrayList;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.files.FilesManager;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.parameters.ParameterStorage;
import autocompchem.run.Terminator;


/**
 * Tool to create and handle the Z-Matrix (internal coordinates) of a chemical 
 * entity.
 *
 * @author Marco Foscato
 */ 

public class ZMatrixHandler
{
    /**
     * The CDK representation of the chemical entity
     */
    private IAtomContainer iac = new AtomContainer();

    /**
     * Name of the input file
     */
    private String inFile;

    /**
     * Name of the second input file
     */
    private String inFile2;

    /**
     * Name of the output file
     */
    private String outFile;

    /**
     * Template ZMatrix
     */
    private ZMatrix tmplZMat;

    /**
     * Flag controlling use of template
     */
    private boolean useTmpl = false;

    /**
     * Flag selection of third intrinsic coordinate
     */
    private boolean onlyTors = false;

    /**
     * The atom index map
     */
    private Map<Integer,Integer> idIacToZmat = new HashMap<Integer,Integer>();

    /**
     * List of internal coordinates used in the Zmatrix
     */
    private ArrayList<InternalCoord> intCoords = new ArrayList<InternalCoord>();

    /**
     * Property used to stamp visited bonds
     */
    private final String DONEFLAG = "ZMHVISITED"; 

    /**
     * Unique integer for naming distance-type internal coords
     */
    private final AtomicInteger distCounter = new AtomicInteger(1);

    /**
     * Unique integer for naming angle-type internal coords
     */
    private final AtomicInteger angCounter = new AtomicInteger(1);

    /**
     * Unique integer for naming torsion-type internal coords
     */
    private final AtomicInteger torCounter = new AtomicInteger(1);

    /**
     * Root for distance-type IC names
     */
    private final String DISTROOT = "dst";

    /**
     * Root for angle-type IC names
     */
    private final String ANGROOT = "ang";

    /**
     * Root for torsion-type IC names
     */
    private final String TORROOT = "tor";

    /**
     * Verbosity level
     */
    private int verbosity = 0;


//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixHandler specifying the chemical entity this object 
     * deals with.
     * @param iac the CDK representation of the chemical entity
     * @param verbosity the verbosity level
     */

    public ZMatrixHandler(IAtomContainer iac, int verbosity)
    {
        this.iac = iac;        
        this.verbosity = verbosity;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixHandler specifying the chemical entity this object
     * deals with.
     * @param iac the CDK representation of the chemical entity
     * @param verbosity the verbosity level
     * @param tmplZMat the template ZMatrix 
     */

    public ZMatrixHandler(IAtomContainer iac, int verbosity, ZMatrix tmplZMat)
    {
        this.iac = iac;
        this.verbosity = verbosity;
        this.useTmpl = true;
        this.tmplZMat = tmplZMat;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a ZMatrixHandler from parameter storage.
     * Parameters controlling the handler
     * <ul>
     * <li>
     * <b>VERBOSITY</b>  verbosity level
     * </li>
     * <li>
     * (optional) <b>INFILE</b>: pathname of the input file containig a the 
     * chemical entity to work with (SDF or ACC's ZMatrix)
     * </li>
     * <li>
     * (optional) <b>INFILE2</b>: pathname of the input file containig a the
     * second chemical entity to work with (SDF or ACC's ZMatrix)
     * </li>
     * <li>
     * (optional) <b>TEMPLATEZMAT</b>: ACC´s ZMatrix used as template when
     * generating ZMAtrix for other, consistent molecular systems.
     * </li>
     * <li>
     * (optional) <b>OUTFILE</b>: pathname of the output file.
     * </li>
     * </ul>
     * @param params the parameters collected in an parameter storage object
     */

    public ZMatrixHandler(ParameterStorage params)
    {
        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to ZMatrixHandler");

        //Get and check the input file (which has to be an SDF file)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FilesManager.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE2"))
	{
            this.inFile2 = params.getParameter("INFILE2").getValue().toString();
            FilesManager.foundAndPermissions(this.inFile2,true,false,false);
	}
       
        //Get the template ZMatrix
        if (params.contains("TEMPLATEZMAT"))
        {
            this.useTmpl = true;
            String tmplZMatFile = 
                      params.getParameter("TEMPLATEZMAT").getValue().toString();
            this.tmplZMat = new ZMatrix(IOtools.readTXT(tmplZMatFile));
        }

        //Get the template ZMatrix
        if (params.contains("TORSIONONLY"))
        {
            this.onlyTors = true;
	    if (this.useTmpl)
	    {
                Terminator.withMsgAndStatus("ERROR! Inconsistent request to "
                             + "use only torsions AND a template ZMatrix.", -1);
		
	    }
        }

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = params.getParameter("OUTFILE").getValue().toString();
            FilesManager.mustNotExist(this.outFile);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Prints the ZMatrix of the loaded chemical system/s, if any.
     * This method will NOT try to reorder the atom list as to make a proper
     * ZMatrix.
     */
   
    public void printZMatrix()
    {
        if (null == outFile)
        {
            if (verbosity > 0)
            {
                System.out.println("No 'OUTFILE' keyword, so using default "
                                                   + "filename 'output.zmat'.");
            }
            outFile = "output.zmat";
        }
        if (iac.getAtomCount() == 0 && null != inFile)
        {
            try 
            {
                int i = 0;
                SDFIterator sdfItr = new SDFIterator(inFile);
                while (sdfItr.hasNext())
                {
                    // Get the molecule
                    i++;
                    iac = sdfItr.next();
                    String molName = MolecularUtils.getNameOrID(iac);
                    System.out.println(" Working on mol ("+i+"): "+molName);
                    ZMatrix zmat = makeZMatrix();
                    IOtools.writeZMatAppend(outFile,zmat,true);
                }
                if (i==0 && verbosity > 1)
                {
                    System.out.println("No molecule found in "+inFile);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                Terminator.withMsgAndStatus("ERROR! Exception returned by "
                    + "SDFIterator while reading " + inFile, -1);
            }
        }
        else
        {
            IOtools.writeTXTAppend(outFile,"No chemical entity in ZMatrix "
                                                            + "handler!",false);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Calculates and prints the difference between pairs of ZMatrices. 
     * Uses the parameters given to the constructor.
     */

    public void subtractZMatrices()
    {
        if (null == outFile)
        {
            if (verbosity > 0)
            {
                System.out.println("No 'OUTFILE' keyword, so using default "
                                                   + "filename 'output.zmat'.");
            }
            outFile = "output.zmat";
        }

        ArrayList<ZMatrix> firstZMats = new ArrayList<ZMatrix>();
        ArrayList<ZMatrix> secondZMats = new ArrayList<ZMatrix>();
        if (iac.getAtomCount() == 0 && null != inFile && null != inFile2)
        {
            if (inFile.endsWith(".sdf"))
            {
                try
                {
                    int i = 0;
                    SDFIterator sdfItr = new SDFIterator(inFile);
                    while (sdfItr.hasNext())
                    {
                        // Get the molecule
                        i++;
                        iac = sdfItr.next();
                        String molName = MolecularUtils.getNameOrID(iac);
                        System.out.println(" Working on mol ("+i+"): "+molName);
                        ZMatrix zmat = makeZMatrix();
                        firstZMats.add(zmat);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Terminator.withMsgAndStatus("ERROR! Exception returned by "
                        + "SDFIterator while reading " + inFile, -1);
                }
            }
            else if (inFile.endsWith(".zmat"))
            {
                firstZMats = IOtools.readZMatrixFile(inFile);
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Only .sdf or .zmat files "
                                    + "can be read in by ZMatrix handler. "
				    + "Check file '" + inFile + "'.", -1);
            }

            if (inFile2.endsWith(".sdf"))
            {
                try
                {
                    int i = 0;
                    SDFIterator sdfItr = new SDFIterator(inFile2);
                    while (sdfItr.hasNext())
                    {
                        // Get the molecule
                        i++;
                        iac = sdfItr.next();
                        String molName = MolecularUtils.getNameOrID(iac);
                        System.out.println(" Working on mol ("+i+"): "+molName);
                        ZMatrix zmat = makeZMatrix();
                        secondZMats.add(zmat);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Terminator.withMsgAndStatus("ERROR! Exception returned by "
                        + "SDFIterator while reading " + inFile, -1);
                }
	    }
	    else if (inFile2.endsWith(".zmat"))
            {
		secondZMats = IOtools.readZMatrixFile(inFile2);
	    }
	    else
	    {
		Terminator.withMsgAndStatus("ERROR! Only .sdf or .zmat files "
			            + "can be read in by ZMatrix handler.", -1);
	    }

            if (firstZMats.size() != secondZMats.size())
            {
                Terminator.withMsgAndStatus("ERROR! Different number of "
                                        + "ZMatrices in '" + inFile 
                                        + "' and '" + inFile2 + "'.", -1);
            }
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Not enough chemical entity in "
                                                       + "ZMatrix handler!",-1);
        }

	for (int i=0; i<firstZMats.size(); i++)
	{
            ZMatrix zmatRes = subtractZMatrices(firstZMats.get(i),
						secondZMats.get(i));
	    IOtools.writeZMatAppend(outFile,zmatRes,true);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Calculates the difference between two ZMatrices
     * @param zmatA the first ZMatrix
     * @param zmatB the second ZMatrix
     * @return the resulting ZMatrix
     */

    public ZMatrix subtractZMatrices(ZMatrix zmatA, ZMatrix zmatB)
    {
        ZMatrix zmatRes = modifyZMatrix(zmatA, zmatB, -1.0);
        return zmatRes;
    }

//------------------------------------------------------------------------------

    /**
     * Creates the Z-Matrix and the corresponding atom index map.
     * @return the resulting ZMatrix
     */

    public ZMatrix makeZMatrix()
    {
        ZMatrix zmat = new ZMatrix();

//TODO: May need to reorder atoms OR change connectivity according to atm list
        if (iac.getAtomCount() == 0)
        {
	    if (null != inFile)
	    {
                try
                {
                    SDFIterator sdfItr = new SDFIterator(inFile);
                    if (sdfItr.hasNext())
		    {
                        iac = sdfItr.next();
                        if (sdfItr.hasNext())
                        {
			    Terminator.withMsgAndStatus("ERROR! Expecting only "
				+ "one molecule in file '"+ inFile +"'.",-1);
			}
                    }
		    else
                    {
                        Terminator.withMsgAndStatus("ERROR! No molecule found "
						             + "in "+inFile,-1);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Terminator.withMsgAndStatus("ERROR! Exception returned by "
                    + "SDFIterator while reading " + inFile, -1);
                }
	    }
	    else
	    {
                IOtools.writeTXTAppend(outFile,"No chemical entity in ZMatrix "
                                                            + "handler!",false);
	    }
        }
        IAtomContainer mol = iac;


//TODO: if atom order changes, change the map
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            idIacToZmat.put(i,i);
        }

        // Ensure consistency with template
        if (useTmpl && mol.getAtomCount() != tmplZMat.getZAtomCount())
        {
            Terminator.withMsgAndStatus("ZMatrix template (" 
                + tmplZMat.getZAtomCount() + ") and current molecule ("
                + mol.getAtomCount() + ") have different size.",-1); 
        }

        // Fill the Z-matrix
        for (int idC=0; idC<mol.getAtomCount(); idC++)
        {
            if (verbosity > 2)
            {
                System.out.println("Working on atom "+idC);
            }
            int idI = -1;
            int idJ = -1;
            int idK = -1;

            IAtom atmI = null;
            IAtom atmJ = null;
            IAtom atmK = null;

            String typK = "0";
            String strNameK = "#";

            double intCrdI = 0.0;
            double intCrdJ = 0.0;
            double intCrdK = 0.0;

            InternalCoord icI = null;
            InternalCoord icJ = null;
            InternalCoord icK = null;

            // The Current atom
            IAtom atmC = mol.getAtom(idC);

            // First internal coordinate of atmC: distance from atmI
            if (idC>0)
            {
                idI = chooseFirstRefAtom(atmC,mol);
                atmI = mol.getAtom(idI);
                intCrdI = atmC.getPoint3d().distance(atmI.getPoint3d());
                List<IAtom> bondedToAtmC = mol.getConnectedAtomsList(atmC);
                if (bondedToAtmC.contains(atmI))
                {
                    mol.getBond(atmC,atmI).setProperty(DONEFLAG,"T");
                }
                else
                {
                    zmat.addPointerToNonBonded(idC,idI);
                }
                icI = new InternalCoord(getUnqDistName(),intCrdI,
                                new ArrayList<Integer>(Arrays.asList(idC,idI)));
                intCoords.add(icI);
            }

            // define the bond angle
            if (idC>1)
            {
                idJ = chooseSecondRefAtom(atmC,atmI,mol);
                atmJ = mol.getAtom(idJ);
                intCrdJ = MolecularUtils.calculateBondAngle(atmC,atmI,atmJ);
                icJ = new InternalCoord(getUnqAngName(),intCrdJ,
                            new ArrayList<Integer>(Arrays.asList(idC,idI,idJ)));
                intCoords.add(icJ);
            }

            // decide on dihedral or second angle and get value
            ArrayList<Integer> arrK = new ArrayList<Integer>();
            if (idC>2)
            {
                Object[] pair = chooseThirdRefAtom(atmC,atmI,atmJ,mol,zmat);
                idK = (int) pair[0];
                atmK = mol.getAtom(idK);
                typK = (String) pair[1];
                if (typK.equals("0"))
                {
                    intCrdK = MolecularUtils.calculateTorsionAngle(atmC,atmI,
                                                                     atmJ,atmK);
                    arrK.add(idC);
                    arrK.add(idI);
                    arrK.add(idJ);
                    arrK.add(idK);
                    strNameK = getUnqTorName();
                }
                else
                {
                    intCrdK = MolecularUtils.calculateBondAngle(atmC,atmI,atmK);
                    arrK.add(idC);
                    arrK.add(idI);
                    arrK.add(idK);
                    strNameK = getUnqAngName();
                }

                icK = new InternalCoord(strNameK,intCrdK,arrK,typK);
                intCoords.add(icK);
            }

            // build object and store it
            ZMatrixAtom zatm = new ZMatrixAtom(atmC.getSymbol(), idI, idJ, idK,
                                                                 icI, icJ, icK);
            zmat.addZMatrixAtom(zatm);
        }

        // Add bonds not visited
        for (IBond b : mol.bonds())
        {
            if (b.getProperty(DONEFLAG) == null)
            {
                zmat.addPointerToBonded(mol.getAtomNumber(b.getAtom(0)),
                                        mol.getAtomNumber(b.getAtom(1)));
            }
        }

        return zmat;
    }

//----------------------------------------------------------------------------

    /**
     * Alter the internal coordinates of the ZMatrixAtoms according to 
     * a given set of changes. The changes are collected into a ZMatrix that
     * must contain the same set of internal coordinates, but each and every 
     * value in the zmtMove's internal coordinate is the change to be applied
     * into the corresponding zmat's internal coordinate. 
     * @param zmat the ZMatrix to modify
     * @param zmtMove the changes to each ZAtom collected into a ZMatrix
     * @param scale the scale factor to be applied to every zmtMove value
     * @return the modified copy of the ZMatrix
     */

    public ZMatrix modifyZMatrix(ZMatrix zmat, ZMatrix zmtMove, double scale)
    {
        ZMatrix modZMat = new ZMatrix(zmat.toLinesOfText(false,false));
        if (modZMat.getZAtomCount() != zmtMove.getZAtomCount())
        {
            Terminator.withMsgAndStatus("ERROR! ZMatrixMove and ZMatrix have "
                + "different number of atoms.",-1);
        }
        for (int i=0; i<modZMat.getZAtomCount(); i++)
        {
            ZMatrixAtom zatm = modZMat.getZAtom(i);
            ZMatrixAtom zmov = zmtMove.getZAtom(i);
            if (verbosity > 3)
            {
                System.out.println("ZATOM: "+zatm.toZMatrixLine(false,false));
                System.out.println("ZMOVE: "+zmov.toZMatrixLine(false,false));
            }

            if (!zatm.sameIDsAs(zmov))
            {
                Terminator.withMsgAndStatus("ERROR! Different set of atom IDs "
                        + "in ZMatrixAtom (" 
                        + zatm.toZMatrixLine(false,false) 
                        +  ") and ZMatrixMove (" 
                        + zmov.toZMatrixLine(false,false) + ")",-1);
            }
            for (int j=0; j<zatm.getICsCount(); j++)
            {
                InternalCoord zmatIC = zatm.getIC(j);
                double change = zmov.getIC(j).getValue();
                change = change * scale;
                if (verbosity > 3)
                {
                    System.out.println("j:"+j);
                    System.out.println(" zmatIC:"+zmatIC);
                    System.out.println(" zmovIC:"+zmov.getIC(j));
                    System.out.println(" Change:"+change);
                }
                else
                {
                    if (Math.abs(change)<0.0000001)
                    {
                        continue;
                    }
                }
                double tmpVal = zmatIC.getValue()+change;
                double s = Math.signum(tmpVal);
                switch (zmatIC.getIDsCount())
                {
                    case 2:
                        zmatIC.setValue(tmpVal);
                        break;

                    case 3:
                        tmpVal = tmpVal % 360.0;
                        if (Math.abs(tmpVal) > 180.0)
                        {
                            tmpVal = (-1)*s*(360.0 - Math.abs(tmpVal));
                        }
                        zmatIC.setValue(tmpVal);
                        break;

                    case 4:
//TODO del
/*System.out.println("zmatIC.getValue(): "+zmatIC.getValue());
System.out.println("tmpVal:            "+tmpVal);

System.out.println("Math.sin(zmatIC.getValue()): "+Math.sin(Math.toRadians(zmatIC.getValue())));
System.out.println("Math.sin(tmpVal):            "+Math.sin(Math.toRadians(tmpVal)));
*/
			double signChange = Math.sin(
					     Math.toRadians(zmatIC.getValue())) 
					    * Math.sin(Math.toRadians(tmpVal)); 
                        tmpVal = tmpVal % 360.0;
                        if (Math.abs(tmpVal) > 180.0)
                        {
                            tmpVal = (-1)*s*(360.0 - Math.abs(tmpVal));
                        }
			if (0.0 > signChange)
			{
			    switch (zmatIC.getType())
			    {
				case "0":
				    break;
                                case "-1":
				    zmatIC.setType("1");
                                    break;
                                case "1":
				    zmatIC.setType("-1");
                                    break;
                                default:
				     Terminator.withMsgAndStatus("ERROR! Type "
					+ "of 3rd internal coordinate ('"
					+ zmatIC.getType() +"') not known.",-1);
			    }
			}
/*
                        if ("0".equals(zmatIC.getType()))
                        {
                            // IC is a dihedral
                        }
                        else
                        {
                            // IC is a second angle
                        }
*/
                        zmatIC.setValue(tmpVal);
                        break;
                }
            }
            if (verbosity > 3)
            {
                System.out.println("ZATM-OUT:"+zatm.toZMatrixLine(false,false));
                IOtools.pause();
            }
        }
        if (verbosity > 1)
        {
            System.out.println(" Modified ZMatrix: ");
            ArrayList<String> txt = modZMat.toLinesOfText(false,false);
            for (int i=0; i<txt.size(); i++)
            {
                System.out.println("  Line-" + i + ": " + txt.get(i));
            }
        }
        return modZMat;
    } 

//----------------------------------------------------------------------------

    /**
     * Convert ZMatrix from file and writes it into an SDF. All settings and
     * input from constructor
     */

    public void convertZMatrixToSDF()
    {
        if (null == outFile)
        {
            if (verbosity > 0)
            {
                System.out.println("No 'OUTFILE' keyword, so using default "
                                                    + "filename 'output.sdf'.");
            }
            outFile = "output.sdf";
        }
        if (null != inFile)
        {
            ArrayList<ZMatrix> zmats = IOtools.readZMatrixFile(inFile);
        int i = 0;
            for (ZMatrix zmat : zmats)
            {
        i++;
        if (verbosity > 0)
        {
            System.out.println("Converting ZMatrix "+i);
        }
        try
        {
            IAtomContainer iac = convertZMatrixToIAC(zmat);
            iac.setProperty(CDKConstants.TITLE,zmat.getTitle());
            IOtools.writeSDFAppend(outFile,iac,true);
        }
        catch (Throwable t)
        {
            Terminator.withMsgAndStatus("ERROR! Exception while "
            + "converting ZMatrix " + i + ". You might need dummy "
            + "atoms to define an healthier ZMatrix. Cause of the "
            + "exception: " + t.getMessage(), -1);
        }
            }
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Expecting an input ZMatrix "
                                     + "file. Please use 'INFILE' keyord.", -1);
        }
    }

//----------------------------------------------------------------------------

    /**
     * Convert ZMatrix into chemical object with Cartesian coordinates
     * @param zmat the ZMatrix to convert
     * @return the chemical object
     * @throws Throwable when there are issues converting the ZMatrix and it 
     * is preferable to redefine the ZMatrix, possibly adding dummy atoms
     */

    public IAtomContainer convertZMatrixToIAC(ZMatrix zmat) throws Throwable
    {
        IAtomContainer dummy = null;
        return convertZMatrixToIAC(zmat,dummy);
    }

//----------------------------------------------------------------------------

    /**
     * Convert ZMatrix into chemical object with Cartesian coordinates
     * @param zmat the ZMatrix to convert
     * @param oldMol a template CDK representation (used for connectivity)
     * @return the chemical object
     * @throws Throwable when there are issues converting the ZMatrix and it 
     * is preferable to redefine the ZMatrix, possibly adding dummy atoms
     */

    public IAtomContainer convertZMatrixToIAC(ZMatrix zmat, 
                                         IAtomContainer oldMol) throws Throwable
    {
        //TODO del code meant to stepwise debug
        boolean debug = false;

        IAtomContainer mol = new AtomContainer();
        for (int i=0; i<zmat.getZAtomCount(); i++)
        {
            //TODO del
            if (debug)
            {
                System.out.println("-----------------------------------------");
                System.out.println("Atom "+i);
            }

            ZMatrixAtom zatm = zmat.getZAtom(i);
            String el = zatm.getName();
            Point3d pt = new Point3d();
            switch (i)
            {
                case 0:
                {
                    checkICs(zatm,0);
                    break;
                }

                case 1:
                {
                    checkICs(zatm,1);
                    InternalCoord icI = zatm.getIC(0);
                    IAtom atmI = mol.getAtom(zatm.getIdRef(0));
                    Point3d pI = atmI.getPoint3d();
                    pt = new Point3d(pI.x, pI.y, pI.z+icI.getValue());
                    break;
                }

                case 2:
                {
                    checkICs(zatm,2);
                    InternalCoord icI = zatm.getIC(0);
                    InternalCoord icJ = zatm.getIC(1);
                    IAtom atmI = mol.getAtom(zatm.getIdRef(0));
                    IAtom atmJ = mol.getAtom(zatm.getIdRef(1));
                    Point3d pI = atmI.getPoint3d();
                    Point3d pJ = atmJ.getPoint3d();
                    double rIJ = pI.distance(pJ);
                    Point3d dIJ = new Point3d(pI.x-pJ.x,pI.y-pJ.y,pI.z-pJ.z); 
                    dIJ.scale(1.0/rIJ);
                    double cosb = dIJ.z;
                    double sinb = Math.sqrt(
                                         Math.pow(dIJ.x,2) + Math.pow(dIJ.y,2));
                    double cosg = 1.0;
                    double sing = 0.0;
                    if (sinb != 0.0)
                    {
                        cosg = dIJ.y / sinb;
                        sing = dIJ.x / sinb;
                    }
                    double vA = icI.getValue() * Math.sin(
                                                Math.toRadians(icJ.getValue()));
                    double vB = rIJ - icI.getValue() * Math.cos(
                                                Math.toRadians(icJ.getValue()));
                    pt = new Point3d(pJ.x + vA*cosg + vB*sing*sinb,
                                     pJ.y - vA*sing + vB*cosg*sinb,
                                     pJ.z + vB*cosb);
                    break;
                }

                default:
                {
                    checkICs(zatm,3);
                    InternalCoord icI = zatm.getIC(0);
                    InternalCoord icJ = zatm.getIC(1);
                    InternalCoord icK = zatm.getIC(2);
                    String type = icK.getType(); 
                    double sign = 0.0;
                    if ("-1".equals(type))
                    {
                        sign = -1.0;
                    }
                    else if ("1".equals(type))
                    {
                        sign = 1.0;
                    }
                    IAtom atmI = mol.getAtom(zatm.getIdRef(0));
                    IAtom atmJ = mol.getAtom(zatm.getIdRef(1));
                    IAtom atmK = mol.getAtom(zatm.getIdRef(2));

                    //TODO del
                    if (debug)
                    {
                        System.out.println("Atm-I: "+atmI);
                        System.out.println("Atm-J: "+atmJ);
                        System.out.println("Atm-K: "+atmK);
                    }
                    Point3d pI = atmI.getPoint3d();
                    Point3d pJ = atmJ.getPoint3d();
                    Point3d pK = atmK.getPoint3d();
                    double rIJ = pI.distance(pJ);
                    Point3d dIJ = new Point3d(pI.x-pJ.x,pI.y-pJ.y,pI.z-pJ.z);
                    dIJ.scale(1.0/rIJ);
                    double rJK = pJ.distance(pK);
                    Point3d dJK = new Point3d(pJ.x-pK.x,pJ.y-pK.y,pJ.z-pK.z);
                    dJK.scale(1.0/rJK);
                    double t = 0.00000010;
                    double sinA = Math.sin(Math.toRadians(icJ.getValue()));
                    double cosA = Math.cos(Math.toRadians(icJ.getValue()));
                    double sinB = Math.sin(Math.toRadians(icK.getValue()));
                    double cosB = Math.cos(Math.toRadians(icK.getValue()));

                    if ("0".equals(type))
                    {
                        //TODO del
                        if (debug)
                        {
                            System.out.println("------ IN TORSION ------");
                            System.out.println("sinA: "+sinA);
                            System.out.println("cosA: "+cosA);
                            System.out.println("sinB: "+sinB);
                            System.out.println("cosB: "+cosB);
                        }

                        // 3rd IC is dihedral angle
                        double dotProdIJIK = dIJ.x*dJK.x 
					   + dIJ.y*dJK.y 
					   + dIJ.z*dJK.z;
                        double compl = Math.sqrt(
                                       Math.max(1.0-Math.pow(dotProdIJIK,2),t));
                        Point3d dt = new Point3d(dIJ.z*dJK.y - dIJ.y*dJK.z,
                                                 dIJ.x*dJK.z - dIJ.z*dJK.x,
                                                 dIJ.y*dJK.x - dIJ.x*dJK.y);
                        dt.scale(1.0/compl);
                        Point3d du = new Point3d(dt.y*dIJ.z - dt.z*dIJ.y,
                                                 dt.z*dIJ.x - dt.x*dIJ.z,
                                                 dt.x*dIJ.y - dt.y*dIJ.x);

                        //TODO del
                        if (debug)
                        {
                            System.out.println("dIJ: "+dIJ.x+" "+dIJ.y+" "+dIJ.z);
                            System.out.println("dJK: "+dJK.x+" "+dJK.y+" "+dJK.z);
                            System.out.println("rIJ: "+rIJ);
                            System.out.println("rJK: "+rJK);
                            System.out.println("dotProdIJIK: "+dotProdIJIK);
                            System.out.println("compl: "+compl);
                            System.out.println("dt: "+dt.x+" "+dt.y+" "+dt.z);
                            System.out.println("du: "+du.x+" "+du.y+" "+du.z);
                        }

                        if (Math.abs(dotProdIJIK) >= 1.0-t)
                        {
                            throw new Throwable("Linearity in definition of "
                                    + "atom (0-based) " + i + ". Need a "
                                    + "linearity-breaking dummy atom bonded to "
                                    + "atom (0-based) " + zatm.getIdRef(0) 
				    + "or an alternative pair of angles.");
                        }

                        pt = new Point3d(pI.x + icI.getValue()*
                                (du.x*sinA*cosB + dt.x*sinA*sinB - dIJ.x*cosA),
                                         pI.y + icI.getValue()*
                                (du.y*sinA*cosB + dt.y*sinA*sinB - dIJ.y*cosA),
                                         pI.z + icI.getValue()*
                                (du.z*sinA*cosB + dt.z*sinA*sinB - dIJ.z*cosA));

                        //TODO del
                        if (debug)
                        {                     
                            System.out.println("bond: "+icI.getValue());
                            System.out.println("pI.x: "+pI.x);
                            System.out.println("pI.y: "+pI.y);
                            System.out.println("pI.z: "+pI.z);
                            System.out.println("--------- END torsion ------ ");
                        }
                    }
                    else
                    {
                        //TODO del
                        if (debug)
                        {       
                            System.out.println("--------- IN ANGLE --------- ");
                        }

                        // 4rd IC is angle
                        double rIK = pI.distance(pK);
                        Point3d dIK= new Point3d(pI.x-pK.x,pI.y-pK.y,pI.z-pK.z);                        dIK.scale(1.0/rIK);

                        Point3d dt = new Point3d(-dIJ.z*dIK.y + dIJ.y*dIK.z,
                                                 -dIJ.x*dIK.z + dIJ.z*dIK.x,
                                                 -dIJ.y*dIK.x + dIJ.x*dIK.y);
                        double dotProdIJIK = -dIJ.x*dIK.x 
					     -dIJ.y*dIK.y 
					     -dIJ.z*dIK.z;
                        double compl = Math.max(1.0-Math.pow(dotProdIJIK,2),t);

                        //TODO del
                        if (debug)
                        {       
                            System.out.println("dIJ: "+dIJ.x+" "+dIJ.y+" "+dIJ.z);
                            System.out.println("dIK: "+dIK.x+" "+dIK.y+" "+dIK.z);
                            System.out.println("rIJ: "+rIJ);
                            System.out.println("rIK: "+rIK);
                            System.out.println("dotProdIJIK: "+dotProdIJIK);
                            System.out.println("compl: "+compl);
                            System.out.println("dt: "+dt.x+" "+dt.y+" "+dt.z);
                        }

                        if (Math.abs(dotProdIJIK) >= 1.0-t)
                        {
                            throw new Throwable("Linearity in definition of "
                                    + "atom (0-based) " + i + ". Need a "
                                    + "linearity-breaking dummy atom bonded to "
                                    + "atom (0-based) " + zatm.getIdRef(0));
                        }

                        double a = (-1.0)*(cosB + dotProdIJIK*cosA)/compl;
                        double b = (cosA + dotProdIJIK*cosB)/compl;
                        double c = (1.0 + a*cosB - b*cosA)/compl;

                        if (debug)
                        {
                            System.out.println("chiral: "+type);
                            System.out.println("a: "+a);
                            System.out.println("b: "+b);
                            System.out.println("c(pre-check): "+c);
			}

                        if (c >= t)
                        {
                            c = Math.sqrt(c)*sign;
                        }
                        else if (c < -t)
                        {
//TODO: decide when tothrow the exception and when to accept a negligible c
// Thorugh exception only if high accuracy is requested
/*
                            throw new Throwable("Generating linearity with "
                                    + "atom (0-based) " + i + ". Need a "
                                    + "linearity-breaking dummy atom bonded to "
                                    + "atom (0-based) " + zatm.getIdRef(0));
*/
                            if (verbosity > 0)
                            {
                                System.out.println("WARNING: negligible c="
				    + c + " for "
                                    + "atom (0-based) " + i + ". Low accuracy "
				    + "is expected. To improve the results add "
				    + "a dummy on atom (0-based) " 
				    + zatm.getIdRef(0) + ", reorder atom list, "
				    + "and build a more healty ZMatrix.");
                            }
                        }
                        else
                        {
                            c = 0.0;
                        }

                        if (debug)
                        {
                            System.out.println("c(post-check): "+c);
                            System.out.println("bond: "+icI.getValue());
                            System.out.println("pI.x: "+pI.x);
                            System.out.println("pI.y: "+pI.y);
                            System.out.println("pI.z: "+pI.z);
                        }

                        pt = new Point3d(pI.x + icI.getValue()*
                                                  (a*dIK.x - b*dIJ.x + c*dt.x),
                                         pI.y + icI.getValue()*
                                                  (a*dIK.y - b*dIJ.y + c*dt.y),
                                         pI.z + icI.getValue()*
                                                  (a*dIK.z - b*dIJ.z + c*dt.z));
                        if (debug)
                        {
                            System.out.println("--------- END angle ------- ");
                        }
                        break;
                    }
                }
            }
            if (debug)
            {
                System.out.println("pt.x: "+pt.x);
                System.out.println("pt.y: "+pt.y);
                System.out.println("pt.z: "+pt.z);
                IOtools.pause();
            }

            IAtom atm = new Atom(el,pt);
            mol.addAtom(atm);

            if (i!=0)
            {
                IAtom atmI = mol.getAtom(zatm.getIdRef(0));
                IBond.Order order = IBond.Order.valueOf("SINGLE");
                IBond.Stereo stereo = IBond.Stereo.NONE;
                boolean revert = false;
                if (null!=oldMol)
                {
                    IBond oldBnd = oldMol.getBond(oldMol.getAtom(i),
                                       oldMol.getAtom(mol.getAtomNumber(atmI)));
                    order = oldBnd.getOrder();
                    stereo = oldBnd.getStereo();
                    if (oldMol.getAtom(i) != oldBnd.getAtom(0))
                    {
                        revert = true;
                    }
                }
                if (revert)
                {
                    IBond bnd = new Bond(atmI,atm,order,stereo);
                    mol.addBond(bnd);
                }
                else
                {
                    IBond bnd = new Bond(atm,atmI,order,stereo);
                    mol.addBond(bnd);
                }
            }
        }

        if (zmat.hasAddedBonds())
        {
            for (int[] bndToAdd : zmat.getPointersToBonded())
            {
                IAtom atm = mol.getAtom(bndToAdd[0]);
                IAtom atmI = mol.getAtom(bndToAdd[1]);
                IBond.Order order = IBond.Order.valueOf("SINGLE");
                IBond.Stereo stereo = IBond.Stereo.NONE;
                boolean revert = false;
                if (null!=oldMol)
                {
                    IBond oldBnd = oldMol.getBond(oldMol.getAtom(bndToAdd[0]),
                                                   oldMol.getAtom(bndToAdd[1]));
                    order = oldBnd.getOrder();
                    stereo = oldBnd.getStereo();
                    if (oldMol.getAtom(bndToAdd[0]) != oldBnd.getAtom(0))
                    {
                        revert = true;
                    }
                }
                if (revert)
                {
                    IBond bnd = new Bond(atmI,atm,order,stereo);
                    mol.addBond(bnd);
                }
                else
                {
                    IBond bnd = new Bond(atm,atmI,order,stereo);
                    mol.addBond(bnd);
                }
            }
        }
        if (zmat.hasBondsToDelete())
        {
            ArrayList<IBond> bondsToDel = new ArrayList<IBond>();
            for (IBond bnd : mol.bonds())
            {
                int ia = mol.getAtomNumber(bnd.getAtom(0));
                int ib = mol.getAtomNumber(bnd.getAtom(1));
    
                for (int[] bndToDel : zmat.getPointersToNonBonded())
                {
                    if ((ia == bndToDel[0] && ib == bndToDel[1]) ||
                        (ia == bndToDel[1] && ib == bndToDel[0]))
                    {
                        bondsToDel.add(bnd);
                        break;
                    }
                }
            }
            for (IBond bnd : bondsToDel)
            {
                mol.removeBond(bnd);
            }
        }
        return mol;
    }

//----------------------------------------------------------------------------

    /**
     * Check that the ZMatrixAtom has the expected number of internal 
     * coordinates
     * @param zatm the atom to check
     * @param nic the expected number of internal coordinates
     */

    private void checkICs(ZMatrixAtom zatm, int nic)
    {
        if (zatm.getICsCount() != nic)
        {
            Terminator.withMsgAndStatus("ERROR! ZMatrixAtom does not contain "
                + "the expected number of internal coordinates. Check "
                + zatm.toZMatrixLine(false,false),-1);            
        }
    }

//----------------------------------------------------------------------------

    /**
     * Choose the first reference atom for defining the internal coordinates of
     * an atom.
     * @param atmC the atom for which we are making the internal coordinates
     * @param mol the molecule
     * @return the index of the chosen atom
     */
 
    private int chooseFirstRefAtom(IAtom atmC, IAtomContainer mol)
    {
        if (useTmpl)
        {
            return tmplZMat.getZAtom(mol.getAtomNumber(atmC)).getIdRef(0);
        }

        List<ZAtomCandidate> candidates = new ArrayList<ZAtomCandidate>();
        for (IAtom nbr : mol.getConnectedAtomsList(atmC))
        {
            if (mol.getAtomNumber(nbr) < mol.getAtomNumber(atmC))
            {
                ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ZAtomCandidateComparator());
        int result = 0;
        try 
        {
            mol.getAtomNumber(candidates.get(0).getAtom());
        }
        catch (Throwable t)
        {
            System.out.println("SourceAtm: " + MolecularUtils.getAtomRef(atmC,
                                              mol)+ " Candidates: "+candidates);
            Terminator.withMsgAndStatus("Cannot choose a reference atom. "
                + "Make sure each atoms is connected by any chain of bonds to "
                + "any other atom in the chemical entity, or reorder the atom "
                + "list as to follow the connectivity.",-1);
        }
        return mol.getAtomNumber(candidates.get(0).getAtom());
    }

//----------------------------------------------------------------------------

    /**
     * Choose the second reference atom for defining the internal coordinates of
     * an atom.
     * @param atmC the atom for which we are making the internal coordinates
     * @param atmI the first reference atom used to define the 1st internal
     * coordinate
     * @param atmJ reference atom that will be defined by this method
     * @param mol the molecule
     * @return the index of the chosen atom
     */

    private int chooseSecondRefAtom(IAtom atmC, IAtom atmI, IAtomContainer mol)
    {
        if (useTmpl)
        {
            return tmplZMat.getZAtom(mol.getAtomNumber(atmC)).getIdRef(1);
        }

        List<ZAtomCandidate> candidates = new ArrayList<ZAtomCandidate>();
        for (IAtom nbr : mol.getConnectedAtomsList(atmI))
        {
            if ((mol.getAtomNumber(nbr) < mol.getAtomNumber(atmC)) 
                && (nbr != atmC))
            {
                ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ZAtomCandidateComparator());
        return mol.getAtomNumber(candidates.get(0).getAtom());
    }

//----------------------------------------------------------------------------

    /**
     * Choose the third reference atom for defining the internal coordinates of
     * an atom.
     * @param atmC the atom for which we are making the internal coordinates
     * @param atmI the first reference atom used to define the 1st internal
     * coordinate
     * @param atmJ the second reference atom used to define the 2nd internal
     * coordinate
     * @param atmK reference atom that will be defined by this method
     * @param mol the molecule
     * @param zmat the z-matrix under construction
     * @return a pair of objects where the first is the integer index of the 
     * chosen atom, and the second
     * a string defining the type of the 3rd internal coordinate
     * (0: torsion, -/+1: angle or improper torsion)
     */

    private Object[] chooseThirdRefAtom(IAtom atmC, IAtom atmI, IAtom atmJ,
                                               IAtomContainer mol, ZMatrix zmat)
    {
        String typK = "0";
        int idC = mol.getAtomNumber(atmC);
        int idI = mol.getAtomNumber(atmI);
        int idJ = mol.getAtomNumber(atmJ);

        if (useTmpl)
        {
            Object[] ob = new Object[2];
            int idK =  tmplZMat.getZAtom(mol.getAtomNumber(atmC)).getIdRef(2);
            typK = tmplZMat.getZAtom(mol.getAtomNumber(
                                                      atmC)).getIC(2).getType();
            IAtom atmK = mol.getAtom(idK);
            // WARNING! The sign of the flag depends on the geometry of mol
            if (!"0".equals(typK))
            {
                double sign = MolecularUtils.calculateTorsionAngle(atmC,
                                                                atmI,atmJ,atmK);
                if (sign > 0.0)
                {
                    typK = "-1";
                }
                else
                {
                    typK = "1";
                }
            }
            ob[0] = idK;
            ob[1] = typK;
            return ob;
        }

        List<ZAtomCandidate> candidates = new ArrayList<ZAtomCandidate>();
        if (zmat.findTorsion(idI,idJ) 
           ||  countDefinedNeighbours(idC,atmJ,mol) == 1)
        {
            typK = "1";
            for (IAtom nbr : mol.getConnectedAtomsList(atmI))
            {
                if (verbosity > 2)
                {
                   System.err.println("  Eval. 3rd (ANG): " + nbr.getSymbol()
                   + mol.getAtomNumber(nbr) + " "
                   + (mol.getAtomNumber(nbr) < idC) + " "
                   + (nbr != atmC) + " "
                   + (nbr != atmJ));
                }
                if ((mol.getAtomNumber(nbr) < idC) && (nbr != atmC) &&
                                                        (nbr != atmJ))
                {
                    double dbcAng = MolecularUtils.calculateBondAngle(nbr,atmI,
                                                                          atmJ);
                    if (dbcAng > 1.0)
                    {
                        ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                        candidates.add(cl);
                    }
                    else
                    {
                        if (verbosity > 2)
                        {
                            System.err.println("Rejected due to linearity with "
                                               + atmJ.getSymbol() + idJ
                                               + " (idK-idI-idJ: " + dbcAng
                                               + ")");
                        }
                    }
                }
            }
        }
        else
        {
            typK = "0";
            for (IAtom nbr : mol.getConnectedAtomsList(atmJ))
            {
                if (verbosity > 2)
                {
                   System.err.println("  Eval. 3rd (TOR): " + nbr.getSymbol()
                   + mol.getAtomNumber(nbr) + " "
                   + (mol.getAtomNumber(nbr) < idC) + " "
                   + (nbr != atmC) + " "
                   + (nbr != atmI));
                }
                if ((mol.getAtomNumber(nbr) < idC) && (nbr != atmC) &&
                                                        (nbr != atmI))
                {
                    ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                    candidates.add(cl);
                }
            }
        }
        Collections.sort(candidates, new ZAtomCandidateComparator());
        if (candidates.size() == 0)
        {
            String msg = "Cannot find a candidate that does not form a linear "
                         + "(or close-to-linear) angle. Please consider the "
                         + "use of dummy atoms in proximity "
                         + "of atom " + MolecularUtils.getAtomRef(atmC,mol);
            Terminator.withMsgAndStatus(msg,-1);
        }

        IAtom atmK = candidates.get(0).getAtom();

        if ("1".equals(typK))
        {
            double sign = MolecularUtils.calculateTorsionAngle(atmC,atmI,
                                                                     atmJ,atmK);
            if (sign > 0.0)
            {
                typK = "-1";
            }
        }

	if (this.onlyTors)
	{
	    typK = "0";
	}

        // build return object
        Object[] pair = new Object[2];
        pair[0] = mol.getAtomNumber(atmK);
        pair[1] = typK;

        return pair;
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of atoms that are connected to a given atom (i.e., a)
     * and are already defined in the zmatrix.
     * @param i the index of the current atom. Atoms with an index above this
     * have not been defined yet in the zmatrix.
     * @param a the atom to which the neighbors are connected
     * @param mol the molecule
     * @return the number of neighbors with index lower than i
     */

    private int countDefinedNeighbours(int i, IAtom a, IAtomContainer mol)
    {
        int tot = 0;
        for (IAtom nbr : mol.getConnectedAtomsList(a))
        {
            if (mol.getAtomNumber(nbr) < i)
                tot++;
        }
        return tot;
    }

//------------------------------------------------------------------------------

    /**
     * Private class used to collect and manage the reference to an object IAtom
     * and the number of connected neighbors.
     */

    private class ZAtomCandidate
    {
        // The IAtom object
        private IAtom iatm;

        // The number of connected atoms
        private int numConnections;

        // Flag indicating this atom as a dummy atom
        private boolean isDummy;

    //--------------------------------------------------------------------------

        public ZAtomCandidate(IAtom iatm, int numConnections)
        {
            this.iatm = iatm;
            this.numConnections = numConnections;
            if (AtomUtils.isElement(iatm.getSymbol()))
            {
                this.isDummy = false;
            }
            else
            {
                this.isDummy = true;
            }
        }

    //--------------------------------------------------------------------------

        public int getConnections()
        {
            return this.numConnections;
        }
    
    //--------------------------------------------------------------------------
    
        public boolean isDummy()
        {
            return this.isDummy;
        }
    
    //--------------------------------------------------------------------------
    
        public IAtom getAtom()
        {
            return this.iatm;
        }
    
    //--------------------------------------------------------------------------

    }

//------------------------------------------------------------------------------

    /**
     * Private comparator for private class ZAtomCandidate
     */

    private class ZAtomCandidateComparator implements Comparator<ZAtomCandidate>
    {
    
        @Override
        public int compare(ZAtomCandidate a, ZAtomCandidate b)
        {
            final int FIRST = 1;
            final int EQUAL = 0;
            final int LAST = -1;
    
            // Get the connection number
            int cnnA = a.getConnections();
            int cnnB = b.getConnections();
    
            //Get the masses
            int massA;
            int massB;
            if (!a.isDummy() && !b.isDummy())
            {
                massA = a.getAtom().getMassNumber();
                massB = b.getAtom().getMassNumber();
            }
            else
            {
                // For dummy atoms set fictitious mass and connectivity
                if (a.isDummy())
                {
                    massA = 1;
                    if (cnnA == 1)
                    {
                        cnnA = 100;
                    }
                }
                if (b.isDummy())
                {
                    massB = 1;
                    if (cnnB == 1)
                    {
                        cnnB = 100;
                    }
                }
            }
    
            //Decide on priority
            if (cnnA == cnnB)
            {
                if (a.isDummy())
                {
                    massA = 1;
                }
                else
                {
                    massA = a.getAtom().getMassNumber();
                }
                if (b.isDummy())
                {
                    massB = 1;
                }
                else
                {
                    massB = b.getAtom().getMassNumber();
                }
    
                if (massA == massB)
                {
                    return EQUAL;
                }
                else
                {
                    if (massA < massB)
                    {
                        return FIRST;
                    }
                    else
                    {
                        return LAST;
                    }
                }
            }
            else
            {
                if (cnnA < cnnB)
                {
                    return FIRST;
                }
                else
                {
                    return LAST;
                }
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Return a unique (within this object) name of a distance-type internal
     * coordinate 
     */

    private String getUnqDistName()
    {
        String s = DISTROOT + distCounter.getAndIncrement();
        return s;
    }

//-----------------------------------------------------------------------------

    /**
     * Return a unique (within this object) name of a ang-type internal
     * coordinate
     */

    private String getUnqAngName()
    {
        String s = ANGROOT + angCounter.getAndIncrement();
        return s;
    }

//-----------------------------------------------------------------------------

    /**
     * Return a unique (within this object) name of a torsion-type internal
     * coordinate
     */

    private String getUnqTorName()
    {
        String s = TORROOT + torCounter.getAndIncrement();
        return s;
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of internal coordinates defined in this ZMatrixHandler
     * @return the list of internal coordinates defined in this handler
     */

    public ArrayList<InternalCoord> getIntCoords()
    {
        return intCoords;
    }

//------------------------------------------------------------------------------

}

