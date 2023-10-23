package autocompchem.chemsoftware.gaussian;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Terminator;

/**
 * Tools that are useful when dealing with Gaussian.
 */
public class GaussianUtils 
{
	/**
	 * Parses the list of normal modes from the formatted text found in 
	 * Gaussian's log. We expect to start reading from the
	 * @param buffRead
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static NormalModeSet parseNormalModesFromLog(BufferedReader buffRead) 
			throws NumberFormatException, IOException
	{
		NormalModeSet listNormModes = new NormalModeSet();
		List<NormalMode> modes = new ArrayList<NormalMode>();
		int skipped = 0;
		String line = "";
		while ((line = buffRead.readLine()) != null)
        {
			skipped++;
			if (skipped>4)
				break;
        }
		skipped = 0;
		while ((line = buffRead.readLine()) != null)
        {
			if (line.isBlank())
    		{
    			// Done reading modes
    			break;
    		}
			String[] p = line.trim().split("\\s+");
			if (line.matches(".*" + GaussianConstants.OUTPROJFREQ 
					+ ".*"))
        	{
				skipped = 0;
    			modes = new ArrayList<NormalMode>();
    			switch (p.length)
        		{
        		case 3:
            		modes.add(new NormalMode());
        			break;
        		case 4:
            		modes.add(new NormalMode());
            		modes.add(new NormalMode());
        			break;
        		case 5:
            		modes.add(new NormalMode());
            		modes.add(new NormalMode());
            		modes.add(new NormalMode());
        			break;
        		default:
        			Terminator.withMsgAndStatus("ERROR! "
        					+ "Cannot "
        					+ "read normal mode from "
        					+ "line '" + line + "'.",-1);
        		}

				// Read frequency values
                for (int j=2; j<(p.length); j++)
                {
                	double freq = Double.parseDouble(p[j]);
                	modes.get(j-2).setFrequency(freq);
                	if (freq < 0)
                		modes.get(j-2).setImaginary(true);
                }
                listNormModes.addAll(modes);
    			continue;
    		} else if (skipped==4) {
        		int numModes = (p.length - 2) / 3;
        		// Parse one line of normal mode components
        		for (int iMode=0; iMode<numModes; iMode++)
        		{
        			for (int iCoord=0; iCoord<3; iCoord++)
        			{	
        				modes.get(iMode).setComponent(
        						//from 1-based to 0-based atm ID
        						Integer.parseInt(p[0])-1,
        						iCoord, // X or Y or Z
        						Double.parseDouble(p[
        						    2+(iMode*3)+iCoord]));
        			}
        		}
        	} else {
        		skipped++;
        	}
        }
		return listNormModes;
	}
	
//------------------------------------------------------------------------------

}
