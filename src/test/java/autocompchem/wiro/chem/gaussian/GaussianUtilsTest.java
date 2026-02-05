package autocompchem.wiro.chem.gaussian;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.utils.NumberUtils;


/**
 * Unit Test for utilities and tools for dealing with Gaussian.
 * 
 * @author Marco Foscato
 */

public class GaussianUtilsTest 
{

    private final String NL = System.getProperty("line.separator");
    
//-----------------------------------------------------------------------------

	@Test
    public void testParseNormalModesFromLog() throws Exception
    {
		String test = 
				  " Dipole        =-2.41563817D-01 1.22636581D-01 1.84136552D+00" + NL
				+ " Polarizability= 6.22260946D+02 5.99621748D+01 6.02945363D+02" + NL
				+ "                 4.64826633D+00 3.56155080D+00 5.40770926D+02" + NL
				+ " Full mass-weighted force constant matrix:" + NL
				+ " Low frequencies --- -256.3814  -15.1134   -7.1589   -0.0018   -0.0016    0.0006" + NL
				+ " Low frequencies ---   10.4065   19.8343   24.2792" + NL
				+ " ******    1 imaginary frequencies (negative Signs) ******" + NL
				+ " Diagonal vibrational polarizability:" + NL
				+ "      165.9890349     127.9696443     170.5772403" + NL
				+ " Harmonic frequencies (cm**-1), IR intensities (KM/Mole), Raman scattering" + NL
				+ " activities (A**4/AMU), depolarization ratios for plane and unpolarized" + NL
				+ " incident light, reduced masses (AMU), force constants (mDyne/A)," + NL
				+ " and normal coordinates:" + NL
				+ "                      1                      2                      3" + NL
				+ "                      A                      A                      A" + NL
				+ " Frequencies --   -256.3730                12.9017                20.9815" + NL
				+ " Red. masses --      7.6147                 3.1183                 1.1391" + NL
				+ " Frc consts  --      0.2949                 0.0003                 0.0003" + NL
				+ " IR Inten    --     10.6294                 0.3753                 0.5210" + NL
				+ "  Atom  AN      X      Y      Z        X      Y      Z        X      Y      Z" + NL
				+ "     1   6     1.00   2.00   3.00     4.02   5.07   6.02     7.01   8.01   9.01" + NL
				+ "     2   6     0.10   0.20   0.30     0.40  -0.50  -0.60     0.70  -0.80   0.90" + NL
				+ "     3   6    -0.00   0.02   0.02     0.05  -0.02  -0.02     0.02   0.00  -0.00" + NL
				+ "     4   6    -0.00   0.02   0.02     0.02   0.02  -0.01     0.01   0.00  -0.01" + NL
				+ "     5   6     0.00   0.01   0.01    -0.01   0.00   0.00     0.00   0.00  -0.00" + NL
				+ "                      4                      5                      6" + NL
				+ "                      A                      A                      A" + NL
				+ " Frequencies --     29.2212                32.0881                39.3774" + NL
				+ " Red. masses --      1.1410                 3.1462                 3.8216" + NL
				+ " Frc consts  --      0.0006                 0.0019                 0.0035" + NL
				+ " IR Inten    --      0.5612                 0.1492                 0.9615" + NL
				+ "  Atom  AN      X      Y      Z        X      Y      Z        X      Y      Z" + NL
				+ "     1   6    -1.00  -2.00  -3.00    -4.02  -5.07  -6.02    -7.01  -8.01  -9.01"  + NL
				+ "     2   6    -0.02   0.01  -0.00     0.05   0.05   0.11    -0.04  -0.03  -0.06" + NL
				+ "     3   6    -0.02   0.00   0.00     0.05   0.04   0.06    -0.05  -0.02  -0.01" + NL
				+ "     4   6    -0.01  -0.01   0.00     0.03   0.02  -0.01    -0.03  -0.01   0.02" + NL
				+ "     5   6     0.00  -0.00  -0.00     0.01   0.00  -0.02    -0.02  -0.01  -0.00" + NL
				+ "                      7                      8" + NL
				+ "                      A                      A" + NL
				+ " Frequencies --     47.4236                59.1848" + NL
				+ " Red. masses --      4.4726                 3.37741" + NL
				+ " Frc consts  --      0.0059                 0.0070" + NL
				+ " IR Inten    --      1.7801                 6.7533" + NL
				+ "  Atom  AN      X      Y      Z        X      Y      Z" + NL
				+ "     1   6     0.02  -0.08   0.02    -0.01  -0.01  -0.00" + NL
				+ "     2   6     0.05  -0.10  -0.04    -0.00  -0.01  -0.01" + NL
				+ "     3   6     0.05  -0.06  -0.05    -0.00   0.00  -0.02" + NL
				+ "     4   6     0.02  -0.02  -0.01    -0.01   0.02  -0.01" + NL
				+ "     5   6     1.11   2.22   3.33     4.44   5.55   6.66" + NL
				+ "" + NL
				+ " -------------------" + NL
				+ " - Thermochemistry -" + NL
				+ " -------------------" + NL
				+ " Temperature   298.150 Kelvin.  Pressure   1.00000 Atm." + NL
				+ " Atom     1 has atomic number  6 and mass  12.00000" + NL;
		Reader inputString = new StringReader(test);
		BufferedReader reader = new BufferedReader(inputString);
		
		NormalModeSet nms = new NormalModeSet();
		String line = "";
		while ((line = reader.readLine()) != null)
        {
			if (line.matches(GaussianConstants.OUTFREQHEADER + ".*"))
        	{
				nms = GaussianUtils.parseNormalModesFromLog(reader);
				break;
        	}
        }
		
		assertEquals(8, nms.size());
		assertTrue(nms.get(0).isImaginary());
		assertFalse(nms.get(1).isImaginary());
		assertFalse(nms.get(7).isImaginary());
		assertTrue(NumberUtils.closeEnough(-256.3730, nms.get(0).getFrequency()));
		assertTrue(NumberUtils.closeEnough(12.9017, nms.get(1).getFrequency()));
		assertTrue(NumberUtils.closeEnough(59.1848, nms.get(7).getFrequency()));
		assertTrue(NumberUtils.closeEnough(1.0, nms.get(0).getComponent(0).x));
		assertTrue(NumberUtils.closeEnough(2.0, nms.get(0).getComponent(0).y));
		assertTrue(NumberUtils.closeEnough(3.0, nms.get(0).getComponent(0).z));
		assertTrue(NumberUtils.closeEnough(7.01, nms.get(2).getComponent(0).x));
		assertTrue(NumberUtils.closeEnough(8.01, nms.get(2).getComponent(0).y));
		assertTrue(NumberUtils.closeEnough(9.01, nms.get(2).getComponent(0).z));
		assertTrue(NumberUtils.closeEnough(4.44, nms.get(7).getComponent(4).x));
		assertTrue(NumberUtils.closeEnough(5.55, nms.get(7).getComponent(4).y));
		assertTrue(NumberUtils.closeEnough(6.66, nms.get(7).getComponent(4).z));
    }

//------------------------------------------------------------------------------

}
