package autocompchem.utils;

import java.util.ArrayList;

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

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

/**
 * Toolbox for general 3D space
 * 
 * @author Marco Foscato
 */

public class ThreeDimensionalSpaceUtils
{

//------------------------------------------------------------------------------

    /**
     * Generate a vector that is perpedicular to the given one.
     * No control over which perpendicular direction will be chosen.
     * @param dir input direction
     * @return a normal direction
     */
    public static Vector3d getNormalDirection(Vector3d dir)
    {
        Vector3d normalDir = new Vector3d();

        Vector3d dirX = new Vector3d(1.0, 0.0, 0.0);
        Vector3d dirY = new Vector3d(0.0, 1.0, 0.0);
        Vector3d dirZ = new Vector3d(0.0, 0.0, 1.0);
        List<Vector3d> candidates = new ArrayList<Vector3d>();
        candidates.add(dirX);
        candidates.add(dirY);
        candidates.add(dirZ);

        // Check for the lucky case... one of the candidates IS the solution
        List<Double> dotProds = new ArrayList<Double>();
        boolean found = false;
        double max = 0.0;
        for (int i=0; i<candidates.size(); i++)
        {
            double res = dir.dot(candidates.get(i));
            double absRes = Math.abs(res);
            if (absRes > max)
                max = absRes;

            if (res == 0.0)
            {
                normalDir = candidates.get(i);
                found = true;
                break;
            } else {
                dotProds.add(absRes);
            }
        }

        // So, since you are not that lucky use the cross-product to get a
        // normal direction using the most divergent of the previous candidates
        if (!found)
        {
            int mostDivergent = dotProds.indexOf(max);
            normalDir.cross(dir,candidates.get(mostDivergent));
            normalDir.normalize();
        }

        return normalDir;
    }

//------------------------------------------------------------------------------

    /**
     * Rotate a vector according to a given rotation axis and angle.
     * @param v original vector to be rotated
     * @param axis rotation axis
     * @param ang rotation angle
     * @return the rotated vector as a new object
     */

    public static Vector3d rotatedVectorWAxisAngle(Vector3d v, Vector3d axis, 
								     double ang)
    {
	Vector3d vRot = new Vector3d(v.x, v.y, v.z);
        axis.normalize();
        double rad = Math.toRadians(ang);
        AxisAngle4d aa = new AxisAngle4d(axis.x,axis.y,axis.z,rad);
        Matrix3d rotMatrix = new Matrix3d();
        rotMatrix.set(aa);
        rotMatrix.transform(vRot);
	return vRot;
    }

//------------------------------------------------------------------------------

}

