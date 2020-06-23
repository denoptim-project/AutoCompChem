package autocompchem.perception.situation;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import autocompchem.perception.circumstance.Circumstance;

/**
 * Unit Test for Situation class
 * 
 * @author Marco Foscato
 */

public class SituationTest 
{

    @Test
    public void testIsOccurring() throws Exception
    {
        Situation sit = new Situation();
        sit.addCircumstance(new Circumstance());
        sit.addCircumstance(new Circumstance());
        sit.addCircumstance(new Circumstance());
        sit.addCircumstance(new Circumstance());
        sit.addCircumstance(new Circumstance());

        ArrayList<Boolean> fingerprint0 = new ArrayList<Boolean>();
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);

        sit.setLogicalExpression("none");
        assertEquals(true,sit.isOccurring(fingerprint0),"lack of expression");
        
        ArrayList<Boolean> fingerprint = new ArrayList<Boolean>();
        fingerprint.add(true);
        fingerprint.add(false);
        fingerprint.add(true);
        fingerprint.add(true);
        fingerprint.add(true);

        sit.setLogicalExpression("${v0}");
        assertEquals(true,sit.isOccurring(fingerprint),"single true");

        sit.setLogicalExpression("${v1}");
        assertEquals(false,sit.isOccurring(fingerprint),"single false");

        sit.setLogicalExpression("${v0 && !v1}");
        assertEquals(true,sit.isOccurring(fingerprint),".AND. and .NOT.");

        sit.setLogicalExpression("${v0 && !v1 && v2 && v3 && v4}");
        assertEquals(true,sit.isOccurring(fingerprint),"five .AND.");

        sit.setLogicalExpression("${v0 || v1}");
        assertEquals(true,sit.isOccurring(fingerprint),".OR.");

        sit.setLogicalExpression("${v0 && (4 > 2)}");
        assertEquals(true,sit.isOccurring(fingerprint),
                                                "mixing numerical and boolean");

    }

//------------------------------------------------------------------------------

}
