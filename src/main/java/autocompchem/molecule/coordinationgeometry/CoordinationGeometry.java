package autocompchem.molecule.coordinationgeometry;

import java.util.ArrayList;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;

import autocompchem.atom.AtomUtils;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;

/**
 *   Object representing a particular geometry of points surrounding a
 *   central point. This object represents the concept of coordination
 *   geometry well known by chemists that studied the Valence
 *   Shell Electron Pair Repulsion theory (VSEPR).
 *   In general, we consider the number of points (P_i) surrounding the 
 *   central one (P_c) and the angles between vectors P_c-P_i.
 */

public class CoordinationGeometry
{
    //ID or Name
    private String name;

    //Coordination umber
    private int cn;

    //set of points
    private List<Point3d> ligands;

    //Angles A-centralAtom-B as meny as needed (deg)
    private double[][] angles;

//------------------------------------------------------------------------------
    public CoordinationGeometry()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct a new CoordinationGeometry from the list of atom surrounding
     * the central atom. This method assumes that the list of atoms 
     * does correspond to connected atoms, thus there is no check of the 
     * connectivity at this stage.
     * @param name the name of the coordination gemetry
     * @param centralAtm the atom to which this geometry is the description of
     * the coordination geometry
     * @param atms the list of surrounding atoms
     */

    public CoordinationGeometry(String name, IAtom centralAtm, List<IAtom> atms)
    {
        this.name = name;

        //Determine coordination number
        int cnloc = atms.size();
        if (cnloc < 2)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot define a coordination "
                + "geometry with less than two ligands!",-1);
        }
        this.cn = cnloc;

        //Make ligands in space
        ligands = new ArrayList<Point3d>();
        Point3d pCnt = AtomUtils.getCoords3d(centralAtm);
        for (IAtom atml : atms)
        {
            Point3d pLig = AtomUtils.getCoords3d(atml);
            Point3d pLigNorm = new Point3d(pLig.x - pCnt.x ,
                                           pLig.y - pCnt.y ,
                                           pLig.z - pCnt.z);
            ligands.add(pLigNorm);
        }

        //Calculate angles
        angles = new double[cn][cn];
        for (int i=0; i<cn; i++)
        {
            angles[i][i] = 0.0;
            for (int j=i+1; j<cnloc; j++)
            {
                double ang = MolecularUtils.calculateBondAngle(atms.get(i), 
                                                centralAtm, atms.get(j));
                ang = Math.abs(ang);
                angles[i][j] = ang;
                angles[j][i] = ang;
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Set the name of this geometry
     * @param name name to be used
     */

    public void setName(String name)
    {
        this.name = name;
    }

//------------------------------------------------------------------------------

    /**
     * Return the name of this geometry
     * @return the name of this geometry
     */

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Return the connection number of the central atom
     * @return the connection number of this geometry
     */

    public int getConnectionNumber()
    {
        return cn;
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of <code>Point3d</code> representing the position of the 
     * ligands
     * @return the list of 3D points representing the position of the ligands
     */

    public List<Point3d> getLigands()
    {
        return ligands;
    }

//------------------------------------------------------------------------------

    /**
     * Return the matrix of angles characterising this coordination geometry
     * @return the matric of angles (deg)
     */

    public double[][] getAngles()
    {
        return angles;
    }

//------------------------------------------------------------------------------

    /**
     * Return the angle corresponding to the given pair of atom indices
     * @param i the first atom index 
     * @param j the second atom index
     * @return the angle (deg)
     */

    public double getAngle(int i, int j)
    {
        return angles[i][j];
    }

//------------------------------------------------------------------------------

    /**
     * Return a tring representing this object
     * @return a string representation of this object
     */

    public String toString()
    {
        String s = name + "_" + cn + "_";
        for (int i=0; i<cn; i++)
            for (int j=0; j<cn; j++)
                s = s + "," + angles[i][j];

        return s;
    }

//------------------------------------------------------------------------------
}
