package autocompchem.molecule.stereochemistry;

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

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

import autocompchem.atom.AtomUtils;

/**
 * Object representing a point-like ligand surrounding a central atom.
 * The first atom (or dummy) used to connect the central atom is used to 
 * identify this ligand.
 */


public class LigandInSpace
{

    /**
     * ID or Name of the ligand
     */
    private String name;

    /**
     * Atom used to bind the ligand to the coordination center
     */
    private IAtom atm;

    /**
     * Atom at the center of the coordination system
     */
    private IAtom cnt;

    /** 
     * Vector center-ligand
     */
    private Vector3d clVec;

    /**
     * Normalized vector center-ligand
     */
    private Vector3d clVecNorm;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty LigandInSpace
     */

    public LigandInSpace()
    {
        this.name = "noname";
        this.atm = new Atom();
        this.cnt = new Atom();
        this.clVec = new Vector3d(0.0, 0.0, 0.0);
        this.clVecNorm = new Vector3d(0.0, 0.0, 0.0);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor specifying name and providing atoms involved
     * @param name the name of the LigandInSpace
     * @param atm the atom representing the ligand. It's the coordinating atom.
     * @param cnt the central atom
     */

    public LigandInSpace(String name, IAtom atm, IAtom cnt)
    {
        this.name = name;
        this.atm = atm;
        this.cnt = cnt;

        //calculate vector center-ligand atom
        Point3d pLig = AtomUtils.getCoords3d(atm);
        Point3d pCnt = AtomUtils.getCoords3d(cnt);
        this.clVec = new Vector3d(pCnt.x - pLig.x, 
                                  pCnt.y - pLig.y,
                                  pCnt.z - pLig.z);
        this.clVecNorm = new Vector3d(pCnt.x - pLig.x,
                                  pCnt.y - pLig.y,
                                  pCnt.z - pLig.z);
        this.clVecNorm.normalize();
    }

//------------------------------------------------------------------------------

    /**
     * Return the name of the ligand
     * @return the ID or name of this ligand
     */

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Return the coordinating atom used to identify this ligand
     * @return the atom used to coordinate this ligant to the center
     */

    public IAtom getLigandAtom()
    {
        return atm;
    }

//------------------------------------------------------------------------------

    /**
     * Return the central atom, which is the atom to which the coordinating atom
     * is connected
     * @return returns the atom at the center of the coordination center
     */

    public IAtom getCenter()
    {
        return cnt;
    }

//------------------------------------------------------------------------------
    
    /**
     * Return the vector [central-atom]-to-[coordinating-atom]
     * @return the vector that goes from the center to the coordinating atom
     */

    public Vector3d getVector()
    {
        return clVec;
    }

//------------------------------------------------------------------------------
    
    /**
     * Return the normalized vector [central-atom]-to-[coordinating-atom]
     * @return the normalized vector that goes from the center to the 
     * coordinating atom
     */

    public Vector3d getVectorNormalized()
    {
        return clVecNorm;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the string representation of the ligand
     * @return the string representation of this ligand
     */

    public String toString()
    {
        String s = name + "_" + atm + "_" + cnt;
        return s;
    }

//------------------------------------------------------------------------------

    /**
     * Calculates the distance from this ligand and another one
     * @param otherLig the other ligand
     * @return the distance between this ligand (at the coordinating atom)
     * and the other ligand (at the coordinating atom)
     */

    public double distanceFrom(LigandInSpace otherLig)
    {
        double dist = -1.0;
        Point3d thisPt = new Point3d(clVec.x, clVec.y, clVec.z);
        Vector3d otherVec = otherLig.getVector();
        Point3d otherPt = new Point3d(otherVec.x, otherVec.y, otherVec.z);

        dist = thisPt.distance(otherPt);

        return dist;
    }

//------------------------------------------------------------------------------
    
    /**
     * Calculates the distance from the head of the normalized vector
     * pointing towards this ligand and the head of the normalized vector
     * pointing towards another ligand
     * @param otherLig the other ligand
     * @return the distance between this and nother ligand, both taken at the
     * head at the end of the normalized vector.
     */

    public double distanceFromDirection(LigandInSpace otherLig)
    {
        double dist = -1.0;
        Point3d thisPt = new Point3d(clVecNorm.x, clVecNorm.y, clVecNorm.z);
        Vector3d otherVec = otherLig.getVectorNormalized();
        Point3d otherPt = new Point3d(otherVec.x, otherVec.y, otherVec.z);

        dist = thisPt.distance(otherPt);

        return dist;
    }

//------------------------------------------------------------------------------

}
