package autocompchem.chemsoftware.gaussian;

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

import java.util.ArrayList;
import java.util.Locale;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;

/**
 * Object representing "Molecular Specification Section" section 
 * of Gaussian input files
 * 
 * @author Marco Foscato
 */

public class GaussianMolSpecification
{
    /**
     * Charge
     */
    private int charge = 0;

    /**
     * Spin
     */
    private int spinMlt = 1;

    /**
     * Atom coordinates (and possibly options) as string
     */
    private ArrayList<String> molSpec = new ArrayList<String>();


//------------------------------------------------------------------------------

    /**
     * Constructs an empty Molecular Specification objecte
     */

    public GaussianMolSpecification()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an GaussianMolSpecification object from a list of
     * formatted lines (i.e., autocompchem's JobDetails format).
     * Only charge and spin multiplicity can be defined from job details files.
     * @param lines the block of lines to parse
     */

    public GaussianMolSpecification(ArrayList<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i).trim().toUpperCase();
            if (!line.startsWith(GaussianConstants.KEYMOLSEC))
            {
                continue;
            }

            line = line.substring(GaussianConstants.KEYMOLSEC.length());
            if (line.startsWith(GaussianConstants.CHARGEKEY))
            {
                try
                {
                    charge = Integer.parseInt(line.substring(
                                                          line.indexOf("=")+1));
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to convert '"
                        + line + "' into an integer charge.",-1);
                }
            }
            else if (line.startsWith(GaussianConstants.SPINMLTKEY))
            {
                try
                {
                    spinMlt = Integer.parseInt(line.substring(
                                                          line.indexOf("=")+1));
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to convert '"
                        + line + "' into an integer charge.",-1);
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a Molecular Specification section from the IAtomContainer and 
     * specified charge and spin multiplicity.
     * <br>
     * WARNING! Only Cartesian coords are used (for the moment).
     * <br>
     * @param mol the molecular definition of the system
     * @param charge the charge of the system
     * @param spinMlt the spin multiplicity
     */
     
    public GaussianMolSpecification(IAtomContainer mol, int charge, int spinMlt)
    {
        setChargeSpin(charge, spinMlt);

        for (IAtom a : mol.atoms())
        {
            String line = "";
            String symbol = a.getSymbol();
            Point3d p3d = MolecularUtils.getCoords3d(a);
            String x = String.format(Locale.ENGLISH,"%17.12f",p3d.x);
            String y = String.format(Locale.ENGLISH,"%17.12f",p3d.y);
            String z = String.format(Locale.ENGLISH,"%17.12f",p3d.z);
            String spacer = "     ";
            line = symbol + spacer + x + spacer + y + spacer + z;
            addPart(line);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Append a line in this MolSpecification
     * @param string line to be added at the end of the existing list
     */

    public void addPart(String string)
    {
        molSpec.add(string);
    }

//------------------------------------------------------------------------------

    /**
     * Return the required line
     * @param lineNum line number in this section (charge and spin line is not
     * included)
     * @return the required line
     */

    public String getLine(int lineNum)
    {
        return molSpec.get(lineNum);
    }

//------------------------------------------------------------------------------

    /**
     * Modify a line in this MolSpecification
     * @param lineNum number of the line to be modified
     * @param newString new line
     */

    public void modifyLine(int lineNum, String newString)
    {
        molSpec.set(lineNum,newString);
    }

//------------------------------------------------------------------------------

    /**
     * Get the charge
     * @return the charge
     */

    public int getCharge()
    {
        return charge;
    }

//------------------------------------------------------------------------------

    /**
     * Get spin multiplicity
     * @return the spin multiplicity
     */

    public int getSpinMultiplicity()
    {
        return spinMlt;
    }

//------------------------------------------------------------------------------

    /**
     * Set charge and spin
     * @param charge new value for charge
     * @param spin new value for spin multiplicity
     */

    public void setChargeSpin(int charge, int spin)
    {
        this.charge = charge;
        this.spinMlt = spin;
    }

//------------------------------------------------------------------------------

    /**
     * Set charge
     * @param charge new value for charge
     */

    public void setCharge(int charge)
    {
        this.charge = charge;
    }

//------------------------------------------------------------------------------

    /**
     * Set spin multiplicity
     * @param spinMult new value for spin multiplicity
     */

    public void setSpinMultiplicity(int spinMult)
    {
        this.spinMlt = spinMult;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array string without new line characters
     * Suitable to print this Molecular Specification Section.
     * @return the lines of text ready to print a Gaussian input file
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();

        String str = "";
        str = charge + " " + spinMlt;
        lines.add(str);        
        for (int i=0; i<molSpec.size(); i++)
        {
            lines.add(molSpec.get(i));
        }

        //This section is black line terminated
        lines.add("");

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of lines with the part of the infomation contained in this
     * object that is allowed in a job details text file.
     * The format corresponds to the format of autocompchem's JobDetail formatted
     * text file and can be used as input in the constructor of this object.
     * @return the lines of text ready to printa jobDetail file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(GaussianConstants.KEYMOLSEC + GaussianConstants.CHARGEKEY 
                                                                + "=" + charge);
        lines.add(GaussianConstants.KEYMOLSEC + GaussianConstants.SPINMLTKEY
                                                               + "=" + spinMlt);
        return lines;
    }

//------------------------------------------------------------------------------

}
