package autocompchem.modeling.compute;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.constants.ACCConstants;

/**
 * Tools to compute quantities deriving from computational chemistry jobs.
 * 
 * @author Marco Foscato
 */

public class CompChemComputer
{

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty CompChemComputer
     */

    public CompChemComputer()
    {}

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
     * @return the correction in [J/(K*mol))]
     */

    public static double vibrationalEntropyCorr(List<Double> freqs, double temp)
    {
        return vibrationalEntropyCorr(freqs, temp, 0.0, 0.0, 0.0001, null);
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
     * cm<sup>-1</sup> (see J. Phys. Chem. B, 2011, 115 (49), pp 14556-14562).
     *  Use 0.0 to
     * exclude application of quasi-harmonic approximation.
     * @param imThrsh threshold in cm<sup>-1</sup> for converting imaginary 
     * modes  
     * to real modes. If a mode has frequency 'i*v' lower than i*(imThrsh) it 
     * will be treated as if its frequency was 'v'.
     * @param ignThrsh threshold for ignore modes: if the modulus of the
     * frequency in cm<sup>-1</sup> is below this value the mode is ignored.
     * This is meant for handling list of modes that include rotations and
     * translations and report them with close-to-zero frequencies.
     * @param logger tool for logging.
     * @return the correction in [J/(K*mol))]
     */

    public static double vibrationalEntropyCorr(List<Double> freqs, 
                                                double temp, 
                                                double qhThrsh, 
                                                double imThrsh, 
                                                double ignThrsh, 
                                                Logger logger)
    {
        double vibS = 0.0d;
        double cR = ACCConstants.GASR; // cR units are [J/(K*mol)]
        double hck = ACCConstants.PLANKSK 
                     * ACCConstants.SPEEDOFLIGHT 
                     / ACCConstants.BOLTZMANNSK; 
                     //hck units are  [J*s]*[cm/s]/[J/K]=cm/K
        
        if (logger==null)
        	logger = LogManager.getLogger();

        String w = "VibS:";
        for (Double freqOrig : freqs)
        {
        	double freq = freqOrig;
        	if (freqOrig < 0.0d && Math.abs(freqOrig) < imThrsh)
            {
        		logger.debug(w + " changing sign to imaginary "
                    		+ "frequency i" + Math.abs(freqOrig));
        		freq = Math.abs(freqOrig);
            }
        	
            if (freq < 0.0d)
            {
            	logger.debug(w + " ignoring imaginary frequency " 
                    		+ "i" + Math.abs(freq));
                continue;
            }
        	// Typically here we get rid of translations and rotations
            if (freq < ignThrsh)
            {
            	logger.debug(w + " ignoring frequency " + freq);
                continue;
            }
            
            // scale to threshold value
            if (freq < qhThrsh)
            {
            	logger.debug(w + " scaling frequency " + freq 
                    		+ " to " + qhThrsh);
                freq = qhThrsh;  
            }
            
            // calculate contribution for this mode
            freq = hck * freq;  //[cm/K] * [1/cm] = 1/K
            double vs = cR*((freq/temp)/(Math.exp(freq/temp) - 1.0) 
                            - Math.log(1.0 - Math.exp(-1.0*freq/temp)));
            vibS = vibS + vs; // units are [J/(K*mol))]
        }
        
        return vibS;  // units are [J/(K*mol))]
    }

//------------------------------------------------------------------------------
}
