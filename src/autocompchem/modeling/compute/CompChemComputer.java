package autocompchem.modeling.compute;

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

import java.util.List;
import java.util.ArrayList;

import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.constants.ACCConstants;

/**
 * Toolbox to compute quantities deriving from computational chemistry jobs.
 * 
 * @author Marco Foscato
 */


public class CompChemComputer
{
    /**
     * Pathname to the file containing the molecules to analyze
     */
    private String inFile;

    /**
     * Pathname to the file where to write the output
     */
    private String outFile;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty CompChemComputer
     */

    public CompChemComputer()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Compute vibrational entropy correction to free energy. This method used
     * high precision values for physical constants such as Plank and
     * Boltzmann's constants. It is known that NWChem uses low precision
     * constants (4 digits in the latest version available in 2018) which
     * explains the sensible, yet acceptable numerical deviations.
     * @param freqs list of frequencies (after projection of translations and
     * rotations of the center of mass) in cm<sup>-1</sup>
     * @param temp the temperature in Kelvin
     * @return the correction in [cal/(K*mol))]
     */

    public static double vibrationalEntropyCorr(ArrayList<Double> freqs,
                                                                    double temp)
    {
	return vibrationalEntropyCorr(freqs,temp,0.0,0.0,0.0,0);
    }

//------------------------------------------------------------------------------

    /**
     * Compute vibrational entropy correction to free energy. This method used
     * high precision values for physical constants such as Plank and 
     * Boltzmann's constants. It is known that NWChem uses low precision 
     * constants (4 digits in the latest version available in 2018) which 
     * explains the sensible, yet acceptable numerical deviations.
     * @param freqs list of frequencies (after projection of translations and 
     * rotations of the center of mass) in cm<sup>-1</sup>
     * @param temp the temperature in Kelvin
     * @param qhThrsh threshold for quasi-harmonic approximation in 
     * cm<sup>-1</sup> (see J. Phys. Chem. B, 2011, 115 (49), pp 14556–14562).
     *  Use 0.0 to
     * exclude application of quasi-harmonic approximation.
     * @param imThrsh threshold for converting imaginary modes  
     * to real modes. If a mode has frequency 'i*v' lower than i*(imThrsh) it 
     * will be threated as if its frequency was 'v'.
     * @param ignThrsh threshold for ignore modes: if the modulus of the
     * frequency in cm<sup>-1</sup> is below this value the mode is ignored.
     * This is meant for handling list of modes that include rotations and
     * translations and report them with close-to-zero frequencies.
     * @param verbosity the verbosity level
     * @return the correction in [cal/(K*mol))]
     */

    public static double vibrationalEntropyCorr(ArrayList<Double> freqs, 
                                                double temp, 
						double qhThrsh, 
						double imThrsh, 
						double ignThrsh, 
						int verbosity)
    {
        double vibS = 0.0d;
        double cR = ACCConstants.GASR 
                    / ACCConstants.JOULEPERMOLETOCALPERMOL; 
                    // cR units are [J/(K*mol)] / [J/cal] = [cal/(K*mol))]
        double hck = ACCConstants.PLANKSK 
                     * ACCConstants.SPEEDOFLIGHT 
                     / ACCConstants.BOLTZMANNSK; 
                     //hck units are  [J*s]*[cm/s]/[J/K]=cm/K

	String w = "VibS:";
        for (Double freq : freqs)
        {
            // imaginary modes
            if (freq < 0.0d)
	    {
		if (Math.abs(freq) < imThrsh)
                {
		    if (verbosity > 1)
		    {
			System.out.println(w + " changing sing to "
					   + "imaginary mode "+freq);
		    }
		    freq = -freq;
		}
		else
		{
                    if (verbosity > 1)
                    {
                        System.out.println(w + " ignoring imaginary mode "
								        + freq);
		    }
                    continue;
		}
            }

            // get rid of translations and rotations
            if (Math.abs(freq) < ignThrsh)
            {
                if (verbosity > 1)
                {
                    System.out.println(w + " ignoring frequency "+freq);
		}
                continue;
            }

            // scale to threshold value
            if (freq < qhThrsh)
            {
                if (verbosity > 1)
                {
                    System.out.println(w + " scaling frequency " + freq 
							    + " to " + qhThrsh);
                }
                freq = qhThrsh;  
            }

            // calculate contribution for this mode
            freq = hck * freq;  //[cm/K] * [1/cm] = 1/K
            double vs = cR*((freq/temp)/(Math.exp(freq/temp) - 1.0) 
                            - Math.log(1.0 - Math.exp(-1.0*freq/temp)));
            vibS = vibS + vs;
        }
        
        return vibS;  // units are [cal/(K*mol))]
    }

//------------------------------------------------------------------------------
}
