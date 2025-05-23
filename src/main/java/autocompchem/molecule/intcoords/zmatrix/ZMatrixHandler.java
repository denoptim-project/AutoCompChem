package autocompchem.molecule.intcoords.zmatrix;

import java.io.File;
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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IIsotope;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


/**
 * Tool to create and handle the Z-Matrix (internal coordinates) of a chemical 
 * entity.
 *
 * @author Marco Foscato
 */ 

public class ZMatrixHandler extends AtomContainerInputProcessor
{   
    /**
     * Additional ZMatrix input
     */
    private ZMatrix zmat2;

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
     * Property used to stamp visited bonds
     */
    private final static String DONEFLAG = "ZMHVISITED"; 

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
     * String defining the task of generating and printing a ZMatrix
     */
    public static final String CONVERTTOZMATNAME = "convertToZMatrix";

    /**
     * Task about  generating and printing a ZMatrix
     */
    public static final Task CONVERTTOZMATTASK;
    static {
    	CONVERTTOZMATTASK = Task.make(CONVERTTOZMATNAME);
    }
    /**
     * String defining the task converting a ZMatrix to connection table
     */
    public static final String CONVERTFROMZMATTASKNAME = 
    		"convertFromZMatrix";

    /**
     * Task about converting a ZMatrix to connection table
     */
    public static final Task CONVERTFROMZMATTASK;
    static {
    	CONVERTFROMZMATTASK = Task.make(CONVERTFROMZMATTASKNAME);
    }
    /**
     * String defining the task of calculating the difference between ZMatrices
     */
    public static final String SUBTRACTZMATRICESTASKNAME = "subtractZMatrices";

    /**
     * Task about calculating the difference between ZMatrices
     */
    public static final Task SUBTRACTZMATRICESTASK;
    static {
    	SUBTRACTZMATRICESTASK = Task.make(SUBTRACTZMATRICESTASKNAME);
    }
    
    /**
     * Parameter requesting exclusive use of torsions as 3rd internal coordinate.
     */
    public static final String TORSIONONLY = "TORSIONONLY";

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public ZMatrixHandler()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(CONVERTTOZMATTASK,
                		CONVERTFROMZMATTASK,
                		SUBTRACTZMATRICESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ZMatrixHandler.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ZMatrixHandler();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        //Get the template ZMatrix
        if (params.contains("TEMPLATEZMAT"))
        {
            this.useTmpl = true;
            File tmplZMatFile = new File(
                      params.getParameter("TEMPLATEZMAT").getValue().toString());
            List<ZMatrix> templates = IOtools.readZMatrixFile(tmplZMatFile);
            if (templates.size()==0)
            {
            	 Terminator.withMsgAndStatus("ERROR! Could not find a template "
            	 		+ "ZMatrix in '" + tmplZMatFile + "'",-1);
            } else if (templates.size()>1)
            {
                logger.warn("Found " + templates.size() 
                	+ " ZMatrix templates, but we'll use only the first one.");
            }
            this.tmplZMat = templates.get(0);
        }

        //Get the template ZMatrix
        if (params.contains(TORSIONONLY))
        {
            this.onlyTors = true;
            if (this.useTmpl)
            {
                Terminator.withMsgAndStatus("ERROR! Inconsistent request to "
                             + "use only torsions AND a template ZMatrix.", -1);
            }
        }
        
        if (params.contains("INFILE2"))
        {
            File inFile2 = new File(
            		params.getParameter("INFILE2").getValue().toString());
            FileUtils.foundAndPermissions(inFile2,true,false,false);
        	if (inFile2.getName().endsWith(".zmat"))
            {
        		List<ZMatrix> lst = IOtools.readZMatrixFile(inFile2);
        		if (lst.size()>1)
                {
                	logger.warn("WARNING! Found " + lst.size() 
                			+ " from INFILE2, but "
                			+ "can use only one atom container. "
                			+ "I'll use the first and ignore the rest.");
                }
        		zmat2 = lst.get(0);
            } else {
                try
                {
                	List<IAtomContainer> lst = IOtools.readMultiMolFiles(
                			inFile2);
            		if (lst.size()>1)
                    {
                    	logger.warn("WARNING! Found " + lst.size() 
                    			+ " from INFILE2, but "
                    			+ "can use only one atom container. "
                    			+ "I'll use the first and ignore the rest.");
                    }
                	IAtomContainer mol = lst.get(0);
                	String molName = MolecularUtils.getNameOrID(mol);
                    zmat2 = makeZMatrix(mol, tmplZMat);
                    zmat2.setTitle(molName);
                } catch (Throwable t) {
                    t.printStackTrace();
                    Terminator.withMsgAndStatus("ERROR! Exception returned "
                        + " while reading " + inFile2, -1);
                }
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Process any molecular structure file as usual, but ignore zmat files
     */
    @Override
    protected void processInputFileParameter(String value)
    {
    	String[] words = value.trim().split("\\s+");
        String pathname = words[0];
        FileUtils.foundAndPermissions(pathname,true,false,false);
    	if (!pathname.endsWith(".zmat"))
        {
    		super.processInputFileParameter(pathname);
        } else {
        	this.inFile = new File(pathname);;
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
    	// Exclude reading of standard molecular structures for tasks that expect 
    	// a ZMatrix as main input
    	if (task.equals(CONVERTFROMZMATTASK)
    			|| task.equals(SUBTRACTZMATRICESTASK))
    	{
    		processZMatrixInput();
    	} else {
    		processInput();
    	}
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(CONVERTTOZMATTASK))
    	{
    		makeZMatrix(iac, i);
    	} else {
    		dealWithTaskMismatch();
        }
    	return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Creates the ZMatrix of the loaded chemical system/s, if any.
     * This method will NOT try to reorder the atom list as to make a proper
     * ZMatrix.
     */
   
    public void makeZMatrix(IAtomContainer iac, int i)
    {
	    String molName = MolecularUtils.getNameOrID(iac);
	    ZMatrix zmat = makeZMatrix(iac, tmplZMat);
	    zmat.setTitle(molName);
	    
	    String msg = StringUtils.mergeListToString(
	    			zmat.toLinesOfText(useTmpl, onlyTors), 
	    			System.getProperty("line.separator"));
	    logger.info(msg);
	    
	    if (outFile!=null)
	    {
	    	outFileAlreadyUsed = true;
	    	IOtools.writeZMatAppend(outFile,zmat,true);
	    }
	    
        if (exposedOutputCollector != null)
        {
        	exposeOutputData(new NamedData(task.ID + "mol-"+i, zmat));
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

    public  ZMatrix makeZMatrix(IAtomContainer iac)
    {
    	return makeZMatrix(iac, tmplZMat);
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates the Z-Matrix and the corresponding atom index map.
     * @return the resulting ZMatrix
     */

    public ZMatrix makeZMatrix(IAtomContainer iac, ZMatrix tmplZMat)
    {
        ZMatrix zmat = new ZMatrix();

//TODO: May need to reorder atoms OR change connectivity according to atm list

        // Ensure consistency with template
        if (tmplZMat!=null && iac.getAtomCount() != tmplZMat.getZAtomCount())
        {
            Terminator.withMsgAndStatus("ZMatrix template (" 
                + tmplZMat.getZAtomCount() + ") and current molecule ("
                + iac.getAtomCount() + ") have different size.",-1); 
        }

        // Fill the Z-matrix
        for (int idC=0; idC<iac.getAtomCount(); idC++)
        {
            logger.trace("Working on atom "+idC);
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
            IAtom atmC = iac.getAtom(idC);

            // First internal coordinate of atmC: distance from atmI
            if (idC>0)
            {
                idI = chooseFirstRefAtom(atmC,iac);
                atmI = iac.getAtom(idI);
                intCrdI = atmC.getPoint3d().distance(atmI.getPoint3d());
                List<IAtom> bondedToAtmC = iac.getConnectedAtomsList(atmC);
                if (bondedToAtmC.contains(atmI))
                {
                	iac.getBond(atmC,atmI).setProperty(DONEFLAG,"T");
                }
                else
                {
                    zmat.addPointerToNonBonded(idC,idI);
                }
                icI = new InternalCoord(getUnqDistName(),intCrdI,
                                new ArrayList<Integer>(Arrays.asList(idC,idI)));
            }

            // define the bond angle
            if (idC>1)
            {
                idJ = chooseSecondRefAtom(atmC,atmI,iac);
                atmJ = iac.getAtom(idJ);
                intCrdJ = MolecularUtils.calculateBondAngle(atmC,atmI,atmJ);
                icJ = new InternalCoord(getUnqAngName(),intCrdJ,
                            new ArrayList<Integer>(Arrays.asList(idC,idI,idJ)));
            }

            // decide on dihedral or second angle and get value
            ArrayList<Integer> arrK = new ArrayList<Integer>();
            if (idC>2)
            {
                Object[] pair = chooseThirdRefAtom(atmC,atmI,atmJ,iac,zmat);
                idK = (int) pair[0];
                atmK = iac.getAtom(idK);
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
            }

            String zatmName = AtomUtils.getSymbolOrLabel(atmC);
            
            // build object and store it
            ZMatrixAtom zatm = new ZMatrixAtom(zatmName, idI, idJ, idK,
                                                                 icI, icJ, icK);
            zmat.addZMatrixAtom(zatm);
        }

        // Add bonds not visited
        for (IBond b : iac.bonds())
        {
            if (b.getProperty(DONEFLAG) == null)
            {
                zmat.addPointerToBonded(iac.indexOf(b.getAtom(0)),
                		iac.indexOf(b.getAtom(1)));
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
                if (Math.abs(change)<0.0000001)
                {
                    continue;
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
        }
        
        String msg = "Modified ZMatrix: " + NL;
        List<String> txt = modZMat.toLinesOfText(false,false);
        for (int i=0; i<txt.size(); i++)
        {
           msg = msg + "  Line-" + i + ": " + txt.get(i);
        }
        logger.info(msg);
        
        return modZMat;
    } 
    
//------------------------------------------------------------------------------

    private void processZMatrixInput()
    {
    	//TODO: enable input from feed instead of file
        if (null != inFile)
        {
            List<ZMatrix> zmats = IOtools.readZMatrixFile(inFile);
	        boolean breakAfterThis = false;
            for (int i=0; i<zmats.size(); i++)
            {
            	if (chosenGeomIdx!=null)
	            {
            		if (i==chosenGeomIdx)
            		{
		        		breakAfterThis = true;
            		} else {
            			continue;
            		}
	            }
            	ZMatrix zmat = zmats.get(i);
      			logger.info("# " + zmat.getTitle());
            	processOneZMatrix(zmat, i);
            	if (breakAfterThis)
            		break;
            }
        } else {
            Terminator.withMsgAndStatus("ERROR! Expecting an input ZMatrix "
            		+ "file. Please use '" + WorkerConstants.PARINFILE 
            		+ "' keyord to provide a pathname to your ZMatrix input.",
            		-1);
        }
    }
    
//------------------------------------------------------------------------------
    
    private void processOneZMatrix(ZMatrix zmat, int i)
    {
    	if (task.equals(CONVERTFROMZMATTASK)) 
    	{
    		try
            {
                IAtomContainer iac = convertZMatrixToIAC(zmat);
                iac.setProperty(CDKConstants.TITLE,zmat.getTitle());

                if (outFile != null)
                {
                	IOtools.writeSDFAppend(outFile, iac, true);
                }
                
                logger.debug(zmat.toLinesOfText(false, onlyTors));
                
                if (exposedOutputCollector != null)
                {
                	exposeOutputData(new NamedData(task.ID + "mol-"+i, iac));
                }
            }
            catch (Throwable t)
            {
                Terminator.withMsgAndStatus("ERROR! Exception while "
                + "converting ZMatrix " + i + ". You might need dummy "
                + "atoms to define an healthier ZMatrix. Cause of the "
                + "exception: " + t.getMessage(), -1);
            }
    	} else if (task.equals(SUBTRACTZMATRICESTASK)) {
            ZMatrix zmatRes = subtractZMatrices(zmat, zmat2);

            if (outFile!=null)
            {
            	IOtools.writeZMatAppend(outFile, zmatRes, true);
            }
            
            logger.debug(zmatRes.toLinesOfText(false, onlyTors));
            
            if (exposedOutputCollector != null)
            {
            	exposeOutputData(new NamedData("zmat-"+i, zmatRes));
            }
        }
    }
    
//------------------------------------------------------------------------------

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
        IAtomContainer mol = new AtomContainer();
        for (int i=0; i<zmat.getZAtomCount(); i++)
        {
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
                    }
                    else
                    {
                        // 4rd IC is angle
                        double rIK = pI.distance(pK);
                        Point3d dIK= new Point3d(pI.x-pK.x,pI.y-pK.y,pI.z-pK.z);
                        dIK.scale(1.0/rIK);

                        Point3d dt = new Point3d(-dIJ.z*dIK.y + dIJ.y*dIK.z,
                                                 -dIJ.x*dIK.z + dIJ.z*dIK.x,
                                                 -dIJ.y*dIK.x + dIJ.x*dIK.y);
                        double dotProdIJIK = -dIJ.x*dIK.x 
                                             -dIJ.y*dIK.y 
                                             -dIJ.z*dIK.z;
                        double compl = Math.max(1.0-Math.pow(dotProdIJIK,2),t);

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
                        
                        if (c >= t)
                        {
                            c = Math.sqrt(c)*sign;
                        }
                        else if (c < -t)
                        {
//TODO: decide when to throw the exception and when to accept a negligible c
// Throw exception only if high accuracy is requested
/*
                            throw new Throwable("Generating linearity with "
                                    + "atom (0-based) " + i + ". Need a "
                                    + "linearity-breaking dummy atom bonded to "
                                    + "atom (0-based) " + zatm.getIdRef(0));
*/
                        
                            logger.warn("WARNING: negligible c="
                                + c + " for "
                                + "atom (0-based) " + i + ". Low accuracy "
                                + "is expected. To improve the results add "
                                + "a dummy on atom (0-based) " 
                                + zatm.getIdRef(0) + ", reorder atom list, "
                                + "and build a more healty ZMatrix.");
                        }
                        else
                        {
                            c = 0.0;
                        }

                        pt = new Point3d(pI.x + icI.getValue()*
                                                  (a*dIK.x - b*dIJ.x + c*dt.x),
                                         pI.y + icI.getValue()*
                                                  (a*dIK.y - b*dIJ.y + c*dt.y),
                                         pI.z + icI.getValue()*
                                                  (a*dIK.z - b*dIJ.z + c*dt.z));
                        break;
                    }
                }
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
                                       oldMol.getAtom(mol.indexOf(atmI)));
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
                int ia = mol.indexOf(bnd.getAtom(0));
                int ib = mol.indexOf(bnd.getAtom(1));
    
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
            return tmplZMat.getZAtom(mol.indexOf(atmC)).getIdRef(0);
        }

        List<ZAtomCandidate> candidates = new ArrayList<ZAtomCandidate>();
        for (IAtom nbr : mol.getConnectedAtomsList(atmC))
        {
            if (mol.indexOf(nbr) < mol.indexOf(atmC))
            {
                ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ZAtomCandidateComparator());
        int result = 0;
        try 
        {
            mol.indexOf(candidates.get(0).getAtom());
        }
        catch (Throwable t)
        {
            Terminator.withMsgAndStatus("Cannot choose a reference atom. "
                + "Make sure each atoms is connected by any chain of bonds to "
                + "any other atom in the chemical entity, or reorder the atom "
                + "list as to follow the connectivity. Candidate for atom " 
                + MolecularUtils.getAtomRef(atmC, mol) + ": " + candidates,-1);
        }
        return mol.indexOf(candidates.get(0).getAtom());
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
            return tmplZMat.getZAtom(mol.indexOf(atmC)).getIdRef(1);
        }

        List<ZAtomCandidate> candidates = new ArrayList<ZAtomCandidate>();
        for (IAtom nbr : mol.getConnectedAtomsList(atmI))
        {
            if ((mol.indexOf(nbr) < mol.indexOf(atmC)) 
                && (nbr != atmC))
            {
                ZAtomCandidate cl = new ZAtomCandidate(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ZAtomCandidateComparator());
        return mol.indexOf(candidates.get(0).getAtom());
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
        int idC = mol.indexOf(atmC);
        int idI = mol.indexOf(atmI);
        int idJ = mol.indexOf(atmJ);

        if (useTmpl)
        {
            Object[] ob = new Object[2];
            int idK =  tmplZMat.getZAtom(mol.indexOf(atmC)).getIdRef(2);
            typK = tmplZMat.getZAtom(mol.indexOf(
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
        if (zmat.usesTorsion(idI,idJ) 
           ||  countDefinedNeighbours(idC,atmJ,mol) == 1)
        {
            typK = "1";
            for (IAtom nbr : mol.getConnectedAtomsList(atmI))
            {
            	String msg = "Eval. 3rd (ANG): " + nbr.getSymbol()
                   + mol.indexOf(nbr) + " "
                   + (mol.indexOf(nbr) < idC) + " "
                   + (nbr != atmC) + " "
                   + (nbr != atmJ);
            	logger.trace(msg);
                if ((mol.indexOf(nbr) < idC) && (nbr != atmC) &&
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
                        logger.trace("Rejected due to linearity with "
                                               + atmJ.getSymbol() + idJ
                                               + " (idK-idI-idJ: " + dbcAng
                                               + ")");
                    }
                }
            }
        }
        else
        {
            typK = "0";
            for (IAtom nbr : mol.getConnectedAtomsList(atmJ))
            {
            	String msg = "Eval. 3rd (TOR): " + nbr.getSymbol()
                   + mol.indexOf(nbr) + " "
                   + (mol.indexOf(nbr) < idC) + " "
                   + (nbr != atmC) + " "
                   + (nbr != atmI);
                logger.trace(msg);
                
                if ((mol.indexOf(nbr) < idC) && (nbr != atmC) &&
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
        pair[0] = mol.indexOf(atmK);
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
            if (mol.indexOf(nbr) < i)
                tot++;
        }
        return tot;
    }

//------------------------------------------------------------------------------

    /**
     * Private class used to collect and manage the reference to an object IAtom
     * and the number of connected neighbors.
     */
    
    //TODO: consider replacing with SeedAtom. The difference is minimal!

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
            this.isDummy = !AtomUtils.isElement(iatm.getSymbol());
        }
        
    //--------------------------------------------------------------------------

        /**
         * Returns the mass number
         */
        public Integer getMassNumber()
        {
        	Integer a = null;

    		try {
    			if (Isotopes.getInstance().isElement(iatm.getSymbol()))
    	        {
    				IIsotope i = Isotopes.getInstance().getMajorIsotope(
    						iatm.getSymbol());
    	            a = i.getMassNumber();
    	        }
    		} catch (Throwable e) {
    			// nothing really
    		}
    		
        	return a;
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
                massA = a.getMassNumber();
                massB = b.getMassNumber();
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
                    massA = a.getMassNumber();
                }
                if (b.isDummy())
                {
                    massB = 1;
                }
                else
                {
                    massB = b.getMassNumber();
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
     * coordinate.
     */

    private String getUnqTorName()
    {
        String s = TORROOT + torCounter.getAndIncrement();
        return s;
    }

//------------------------------------------------------------------------------

}

