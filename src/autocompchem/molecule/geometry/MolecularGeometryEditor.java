package autocompchem.molecule;

/*   
 *   Copyright (C) 2017  Marco Foscato 
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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

import javax.vecmath.Point3d;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.CDKConstants;

import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.files.FilesManager;
import autocompchem.parameters.ParameterStorage;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.molecule.geometry.ComparatorOfGeometries;


/**
 * Tool for editing molecular geometries. 
 * 
 * @author Marco Foscato
 */


public class MolecularGeometryEditor
{
    /**
     * Flag indicating the input is from file
     */
    private boolean inpFromFile = false;

    /**
     * Name of the molecular structure input file
     */
    private String inFile;

    /**
     * Molecular representation of the initial system
     */
    private IAtomContainer inMol;

    /**
     * Flag indicating the output is to be written to file
     */
    private boolean outToFile = false;

    /**
     * Name of the output file
     */
    private String outFile;

    /**
     * Molecular representation of the final systems
     */
    private ArrayList<IAtomContainer> outMols = new ArrayList<IAtomContainer>();

    /**
     * Pathname to SDF file with reference substructure
     */
    private String refFile;

    /**
     * Molecular representation of the reference substructure
     */
    private IAtomContainer refMol;

    /**
     * Cartesian Move
     */
    private ArrayList<Point3d> crtMove = new ArrayList<Point3d>();

    /**
     * ZMatrix Move
     */
    private ZMatrix zmtMove;

    /**
     * Scale factor for Move
     */
    private ArrayList<Double> scaleFactors = new ArrayList<Double>(
                                                            Arrays.asList(1.0));
    /**
     * Flag reporting that all tasks are done
     */
    private boolean allDone = false;

    /**
     * Verbosity level
     */
    private int verbosity = 0;

    /**
     * Parameters given to Constructor
     */
    private ParameterStorage params;


//------------------------------------------------------------------------------

    /**
     * Constructor for a MolecularGeometryEditor with default settings.
     */

    public MolecularGeometryEditor() 
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MolecularGeometryEditor specifying the parameters with a
     * {@link ParameterStorage}. 
     * <ul>
     * <li>
     * <b>INFILE</b> pathname of the SDF file containing the molecular
     * structure.
     * (only SDF files with ONE molecule are acceptable!).
     * </li>
     * <li>
     * <b>OUTFILE</b> pathname of the SDF file where results are to be
     * written.
     * </li>
     * <li>
     * <b>CARTESIANMOVE</b> (optional) pathname to a file defining 
     * the Cartesian components of the geometrical change. The file format
     * consists of a list of translation vectors in Cartesian coordinates, 
     * where each vector is reported as a space-separated  (X Y Z) tupla.
     * The Cartesian move is orientation-dependent. Unless a
     * reference substructure is also provided (see keyword 
     * REFERENCESUBSTRUCTURE), it is assumed that the 1-to-N vectors refer to
     * the 1-to-N atoms in the molecular system provided as input and that the
     * orientation of the molecule is consistent with that of the Cartesian 
     * move. When a reference substructure is provided (see parameter
     * REFERENCESUBSTRUCTURE), the orientation of the Cartesian move is assumed
     * to be consistent with that of the reference substructure.
     * </li>
     * <li>
     * <b>CARTESIANSCALINGFACTORS</b> (optional) one or more scaling factors
     * (real numbers) to be applied to the Cartesian move. The Cartesian
     * move is applies on the initial structure per each given scaling factor.
     * The default is 1.0).
     * <li>
     * <b>REFERENCESUBSTRUCTUREFILE</b> (optional) pathname to SDF file with 
     * the definition of the substructure to which the geometrical change has 
     * to be applied. The atom list is used to assign the Cartesian moves to
     * the atoms of the molecular system given via the INFILE keyword.
     * </li>
     * <li>
     * <b>VERBOSITY</b> (optional) verbosity level.
     * </li>
     * </ul>
     *
     * @param params object <code>ParameterStorage</code> containing all the
     * parameters needed
     */

    public MolecularGeometryEditor(ParameterStorage params) 
    {
        //Define verbosity
        if (params.contains("VERBOSITY"))
        {
            String v = params.getParameter("VERBOSITY").getValue().toString();
            this.verbosity = Integer.parseInt(v);
        }
	this.params = params;

        if (verbosity > 0)
        {
            System.out.println(
                              " Reading parameters in MolecularGeometryEditor");
        }

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            inpFromFile = true;
            this.inFile = params.getParameter("INFILE").getValue().toString();
            FilesManager.foundAndPermissions(this.inFile,true,false,false);
            ArrayList<IAtomContainer> inMols = IOtools.readSDF(inFile);
            if (inMols.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! MoleculeGeometryEditor "
                        + "requires  SDF files with only one structure. "
                        + "Check file " + inFile, -1);
            }
            this.inMol = inMols.get(0);
        }

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            outToFile = true;
            this.outFile = params.getParameter("OUTFILE").getValue().toString();
            FilesManager.mustNotExist(this.outFile);
        }

        // Get the Cartesian move
        if (params.contains("CARTESIANMOVE"))
        {
            String crtFile = 
                     params.getParameter("CARTESIANMOVE").getValue().toString();
            FilesManager.foundAndPermissions(crtFile,true,false,false);
            ArrayList<String> all = IOtools.readTXT(crtFile);
            for (String line : all)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }
                Point3d v = new Point3d();
                try
                {
                    String[] partsB = line.trim().split("\\s+");
                    v = new Point3d(Double.parseDouble(partsB[0]),
                                    Double.parseDouble(partsB[1]),
                                    Double.parseDouble(partsB[2]));
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot convert line '"
                        + line + "' into a 3-tupla of Cartesian coordinates. "
                        + "Check file '" + crtFile+ "'.",-1);
                }
                this.crtMove.add(v);
            }
        }

        // Get sequence of scaling factors
        if (params.contains("CARTESIANSCALINGFACTORS"))
        {
            String line = params.getParameter("CARTESIANSCALINGFACTORS")
                                                         .getValue().toString();
            scaleFactors.clear();
            String[] parts = line.trim().split("\\s+");
            for (int i=0; i<parts.length; i++)
            {
                double d = 1.0;
                try
                {
                    d = Double.parseDouble(parts[i]);
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot understand scale "
                        + "factor '" + parts[i] + "'. Expecting double.",-1);
                }
                scaleFactors.add(d);
            }
        }

        // Get the Cartesian move
        if (params.contains("ZMATRIXMOVE"))
        {
            String zmtFile =
                       params.getParameter("ZMATRIXMOVE").getValue().toString();
            FilesManager.foundAndPermissions(zmtFile,true,false,false);
	    if (IOtools.readZMatrixFile(zmtFile).size() != 1)
	    {
		Terminator.withMsgAndStatus("ERROR! Found multiple ZMatrices "
					     + "in file '" + zmtFile + "'.",-1);
	    }
            this.zmtMove = IOtools.readZMatrixFile(zmtFile).get(0);
        }

        // Get the reference substructure
        if (params.contains("REFERENCESUBSTRUCTUREFILE"))
        {
            this.refFile = params.getParameter("REFERENCESUBSTRUCTUREFILE")
                                                         .getValue().toString();
            FilesManager.foundAndPermissions(this.refFile,true,false,false);
            ArrayList<IAtomContainer> refMols = IOtools.readSDF(this.refFile);
            if (refMols.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! MoleculeGeometryEditor "
                        + "requires SDF files with one reference structure. "
                        + "Check file " + refFile, -1);
            }
            this.refMol = refMols.get(0);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Apply the geometrical modification as defined by the constructor.
     */

    public void applyMove()
    {
        if (zmtMove!=null && zmtMove.getZAtomCount()>0)
        {
                if (verbosity > 0)
                {
                    System.out.println(" Applying ZMatrix move");
                }
		try
		{
                    applyZMatrixMove();
		}
		catch (Throwable t)
		{
		    Terminator.withMsgAndStatus("ERROR! Exception while "
			+ "altering the ZMatrix. You might need dummy "
			+ "atoms to define an healthier ZMatrix. Cause of the "
			+ "exception: " + t.getMessage(), -1);
		}
        }
        else if (crtMove!=null && crtMove.size()>0)
        {
            if (verbosity > 0)
                {
                    System.out.println(" Applying Cartesian move");
                }
                applyCartesianMove();
        }
        else
            {
            Terminator.withMsgAndStatus("ERROR! Choose and provide either "
                    + "a Cartesian or a ZMatrix move.", -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Apply the geometrical modification as defined by the constructor.
     */

    public void applyCartesianMove()
    {
        // Consistency check
        if (inMol == null || inMol.getAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No input molecule defined. "
            + "Please, try using 'INFILE' to import a molecule from file.",-1);
        }
        if (crtMove == null || crtMove.size() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No Cartesian move defined. "
                + "Please, try using 'CARTESIANMOVE' to provide a Cartesian "
                + "move.",-1);
        }

        // Identify the actual Cartesian move, possibly using reference 
        // substructure to identify the atom to be moved
        ArrayList<Point3d> actualMove = new ArrayList<Point3d>();
                IAtomContainer actualMol = inMol;
        for (int i=0; i<actualMol.getAtomCount(); i++)
        {
            actualMove.add(new Point3d());
        }
        if (this.refMol != null && this.refMol.getAtomCount() > 0)
        {
            if (verbosity > 0)
            {
                System.out.println(" Matching reference sub-structure");
            }
            if (this.refMol.getAtomCount() != crtMove.size())
            {
                Terminator.withMsgAndStatus("ERROR! Reference structure (size "
                     + this.refMol.getAtomCount() + ") and Cartesian step ("
                     + "size " + crtMove.size() + ") have different size. ",-1);
            }
            ComparatorOfGeometries mc = new ComparatorOfGeometries(verbosity-1);
            Map<Integer,Integer> refToInAtmMap = mc.getAtomMapping(actualMol,
                                                                        refMol);
            for (Integer refAtId : refToInAtmMap.keySet())
            {
                actualMove.set(refToInAtmMap.get(refAtId),crtMove.get(refAtId));
            }

            //Get the rototranslate actualMol as to make it consistent with the
            // reference's Cartesian move
            actualMol = mc.getFirstMolAligned();
        }
        else
        {
            for (int i=0; i<crtMove.size(); i++)
            {
                actualMove.set(i,crtMove.get(i));
            }
        }
        if (verbosity > 0)
        {
            System.out.println(" Actual Cartesian move: ");
            for (int i=0; i<actualMove.size(); i++)
            {
                Point3d pt = actualMove.get(i);
                System.out.println("  " + i + ": " + pt);
            }
            System.out.println(" Scaling factors: " + scaleFactors);
        }

        // Produce the transformed geometry/ies
        for (int j=0; j<scaleFactors.size(); j++)
        {
            if (verbosity > 0)
            {
                System.out.println(" Generating results for scaling " 
                                            + " factor " + scaleFactors.get(j));
            }

            IAtomContainer outMol = new AtomContainer();
            try
            {
                outMol = (IAtomContainer) actualMol.clone();
            }
            catch (Throwable t)
            {
                Terminator.withMsgAndStatus("ERROR! Cannot clone molecule.",-1);
            }
            for (int i=0; i<outMol.getAtomCount(); i++)
            {
                Point3d newPt3d = new Point3d(outMol.getAtom(i).getPoint3d());
                Point3d newMv = new Point3d(actualMove.get(i));
                newMv.scale(scaleFactors.get(j));
                newPt3d.add(newMv);
                outMol.getAtom(i).setPoint3d(newPt3d);
            }
            String outMolName = MolecularUtils.getNameOrID(inMol);
            outMolName = outMolName + " - moved by " + scaleFactors.get(j) 
                                                        + "x(Cartesian_move)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            outMols.add(outMol);
            if (outToFile)
            {
                IOtools.writeSDFAppend(outFile,outMol,true);
            }
        }
        
        if (!outToFile && verbosity > 0)
        {
            System.out.println(" ");
            System.out.println(" WARNING! No output file produced (use OUTFILE "
                + " to write the results into a new SDF file");
        }
        allDone = true;
    }

//------------------------------------------------------------------------------

    /**
     * Apply the geometrical modification as defined by the constructor.
     * @throws Throwable when there are issues converting the ZMatrix and it 
     * is preferable to redefine the ZMatrix, possibly adding dummy atoms
     */

    public void applyZMatrixMove() throws Throwable
    {
        // Consistency check
        if (inMol == null || inMol.getAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No input molecule defined. "
            + "Please, try using 'INFILE' to import a molecule from file.",-1);
        }
        if (zmtMove == null || zmtMove.getZAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No ZMatrix move defined. "
                + "Please, try using 'ZMATRIXMOVE' to provide a geometrical "
                + "modification as changes of internal coordinates.",-1);
        }

        // Get the ZMatrix of the molecule to work with
        //TODO del
	// ZMatrixHandler zmh = new ZMatrixHandler(inMol,verbosity);
	ZMatrixHandler zmh = new ZMatrixHandler(params);
        ZMatrix inZMatMol = zmh.makeZMatrix();
        if (verbosity > 1)
        {
            System.out.println(" Original ZMatrix: ");
            ArrayList<String> txt = inZMatMol.toLinesOfText(false,false);
            for (int i=0; i<txt.size(); i++)
            {
                System.out.println("  Line-" + i + ": " + txt.get(i));
            }
        }

        // Identify the actual move in terms of ZMatrix
        ZMatrix actualZMatMove = new ZMatrix();
        if (inMol.getAtomCount() != zmtMove.getZAtomCount())
        {
            // TODO
            if (verbosity > -1)
            {
                System.out.println(" TODO: what if only some IC is modified?");
                Terminator.withMsgAndStatus("ERROR! Still in development",-1);
            }
        }
        else
        {
            actualZMatMove = new ZMatrix(zmtMove.toLinesOfText(false,false));
        }
        if (verbosity > 0)
        {
            System.out.println(" Actual ZMatrixMove: ");
            ArrayList<String> txt = actualZMatMove.toLinesOfText(false,false);
            for (int i=0; i<txt.size(); i++)
            {
                System.out.println("  Line-" + i + ": " + txt.get(i));
            }
            System.out.println(" Scaling factors: " + scaleFactors);
        }

        // Produce the transformed geometry/ies
        for (int j=0; j<scaleFactors.size(); j++)
        {
            if (verbosity > 0)
            {
                System.out.println(" Generating results for scaling " 
                                            + " factor " + scaleFactors.get(j));
            }

            // Apply ZMatrixMove
            ZMatrix modZMat = zmh.modifyZMatrix(inZMatMol,actualZMatMove,
                                                           scaleFactors.get(j));

            // Convert to Cartesian coordinates
            IAtomContainer outMol = zmh.convertZMatrixToIAC(modZMat,inMol);

            // Prepare and print output to file
            String outMolName = MolecularUtils.getNameOrID(inMol);
            outMolName = outMolName + " - moved by " + scaleFactors.get(j) 
                                                             + "x(ZMatrixMove)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            outMols.add(outMol);
            if (outToFile)
            {
                IOtools.writeSDFAppend(outFile,outMol,true);
            }
        }
        
        if (!outToFile && verbosity > 0)
        {
            System.out.println(" ");
            System.out.println(" WARNING! No output file produced (use OUTFILE "
                + " to write the results into a new SDF file");
        }
        allDone = true;
    }
 
//------------------------------------------------------------------------------

}
