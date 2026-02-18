package autocompchem.molecule.coordinationgeometry;

import java.util.ArrayList;
import java.util.Collection;

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
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Collections2;


/**
 * Toolbox for determination and analysis of the geometry of atoms in terms of 
 * the reciprocal positioning of connected neighbours.
 */


public class CoordinationGeometryUtils
{

//------------------------------------------------------------------------------

    /**
     * Compare two coordination geometries and calculate Mean Angle Difference
     * @param cgA the first geometry
     * @param cgB the second geometry
     * @param logger logging tool
     * @return the Mean Angle Difference between the two geometries
     */

    public static double calculateMeanAngleDifference(CoordinationGeometry cgA, 
                                        CoordinationGeometry cgB, 
                                        Logger logger)
    {
        //Check CN
        int cnA = cgA.getConnectionNumber();
        int cnB = cgB.getConnectionNumber();
        if (cnA != cnB)
        {
            String msg = "'calculateMeanAngleDifference' says that "
                         + "Coordination geometries have different CN!";
            throw new IllegalArgumentException(msg);
        }

        //Generate list of indexes
        List<Integer> listIdA = new ArrayList<Integer>();
        List<Integer> listIdB = new ArrayList<Integer>();
        for (int i=0; i<cnA; i++)
        {
            listIdA.add(i);
            listIdB.add(i);
        }

        //Search over all permutations / possible atom mapping
        Collection<List<Integer>> perms = Collections2.permutations(listIdB);
        List<Integer> bestPerm = new ArrayList<Integer>();
        double lowestMad = Double.MAX_VALUE;
        for (List<Integer> permB : perms)
        {
            double locMad = 0.0;            
            for (int posA=0; posA<cnA; posA++)
            {
                for (int posB=posA+1; posB<cnA; posB++)
                {
                    locMad = locMad + Math.abs(
                              cgA.getAngle(listIdA.get(posA),listIdA.get(posB))
                              - cgB.getAngle(permB.get(posA),permB.get(posB)));

                }

            }
            locMad = locMad / ((cnA * (cnA -1)) / 2);
            if (locMad < lowestMad)
            {
                lowestMad = locMad;
                bestPerm = permB;
            }            
        }
        
        logger.debug("Atoms mapping returning lowest MAD is: " 
                                + "molA:" + listIdA + " molB:" + bestPerm 
                                + " LowestMAD: " + lowestMad);

        return lowestMad;
    }
    
//------------------------------------------------------------------------------

    /**
     * Print a matrix representing the all-vs-all comparison of all the
     * coordination geometries contained in the list. The comparison is given by
     * the Mean Angle Difference between each pair of CoordinationGeometry.
     * @param listGeometries list of geometries to compare.
     * @param logger the logging tool.
     */

    public static void printAllVsAllMAD(
    		List<CoordinationGeometry> listGeometries, Logger logger)
    {
        String head = "                ";
        String NL = System.getProperty("line.separator");
        ArrayList<String> lines = new ArrayList<String>();
        int cn = 0;
        for (int i=0; i<listGeometries.size(); i++)
        {
            CoordinationGeometry cgI = listGeometries.get(i);
            cn = cgI.getConnectionNumber();
            head = head + cgI.getName();
            for (int ii=0; ii<(16-cgI.getName().length()); ii++)
                 head = head + " ";
/*
                            if (cgI.getName().length() > 6)
                                head = head + cgI.getName() + "\t";
                            else
                                head = head + cgI.getName() + "\t";
                        
*/
            String line = " " + cgI.getName();
            for (int ii=0; ii<(16-cgI.getName().length()); ii++)
                 line = line + " ";

            for (int j=0; j<listGeometries.size(); j++)
            {
                 CoordinationGeometry cgJ = listGeometries.get(j);
                 double mad = calculateMeanAngleDifference(cgI, cgJ, logger);
                 String madStr = String.format(Locale.ENGLISH,"%.3f", mad);
                 line = line + madStr;
                 for (int ii=0; ii<(16-madStr.length()); ii++)
                      line = line + " ";
            }
            lines.add(line);
        }

        String msg = "Comparison between reference geometries (CN=" + cn + ")";
        msg = msg + NL + head + NL;
        for (String s: lines)
        	msg = msg + s + NL;
        logger.info(msg);
    }

//------------------------------------------------------------------------------

}
