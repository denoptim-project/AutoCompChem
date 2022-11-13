package autocompchem.modeling.basisset;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;


/**
 * Toolbox for basis sets
 * 
 * @author Marco Foscato
 */

public class BasisSetUtils
{

//------------------------------------------------------------------------------

    /**
     * Reads a Gaussian Basis Set (gbs) file and returns the basis set object. 
     * @param inFile pathname to the gbs file to read
     * @param verbosity amount of log
     * @return the basis set object
     */

    public static BasisSet importBasisSetFromGBSFile(String inFile, 
                                                                  int verbosity)
    {
        String msg = "";
        BasisSet bs = new BasisSet();
        boolean isECPSection = false;
        boolean foundBSSection = false;
        CenterBasisSet cbs;
        if (verbosity > 1)
        {
            System.out.println(" Importing basis set from GBS file " + inFile);
        }
        ArrayList<String> lines = IOtools.readTXT(inFile);
        for (int i=0; i<lines.size(); i++)
        {
            // File is read line-by-line sequentially
            String line = lines.get(i);

            // Ignore comments
            if (line.trim().startsWith("!"))
            {
                continue;
            }
            // Ignore separators if empty basis set section 
            // NB: separators from non-empty basis set section are read inside 
            // the while loop below
            if (line.trim().startsWith("****"))
            {
                foundBSSection = true;
                continue;
            }

            // Ignore empty lines
            if (line.trim().equals(""))
            {
                // ...unless the blank line separates basis set from ECP
                if (foundBSSection)
                {
                    if (!isECPSection && verbosity > 2)
                    {
                        System.out.println("From here reading ECP in "+inFile);
                    }
                    isECPSection = true;
                }
                continue;
            }

            //WARNING: the syntax of the files returned by the basis set
            // exchange website changed so that the initial '****' that
            // could be found before the basis set, is no longer included.
            // Thus both basis set and ecp sections start simply with the
            // line including only element symbol and atom index.
            // The following cannot be assumed anymore.
            /*
            // If is not empty and doesn't start with "****" then is ECP
            if (!foundBSSection)
            {
                isECPSection = true;
            }
            */

            String[] wrds = line.trim().split("\\s+");

            // Parse the basis set section
            if (wrds.length == 2 && wrds[0].matches("\\w+\\.?")
                                       && wrds[1].equals("0") && !isECPSection)
            {
                foundBSSection = true;
                String atmId = wrds[0].toUpperCase();
                cbs = bs.getCenterBasisSetForCenter(atmId);
                if (verbosity > 2)
                {
                    System.out.println("Importing basis set for center '" 
                                                                  + atmId +"'");
                }
                boolean keepReadingCBS = true;
                Shell shell = new Shell();
                while (keepReadingCBS && i<lines.size())
                {
                    i++;
                    line = lines.get(i);

                    if (line.trim().equals("****"))
                    {
                        keepReadingCBS = false;
                        continue;
                    }
                    else if (line.trim().equals(""))
                    {
                        msg = "ERROR! Unexpected empty line in basis set "
                                + "defintition. Check file '" + inFile + "' "
                                + "line nr. " + (i+1) + ".";
                        Terminator.withMsgAndStatus(msg,-1);
                    }

                    wrds = line.trim().split("\\s+");

                    if (wrds.length==3 && wrds[0].matches("\\w+\\.?")
                                               && NumberUtils.isNumber(wrds[2]))
                    {
                        if (shell.getSize() > 0)
                        {
                            cbs.addShell(shell);
                        }
                        if (verbosity > 2)
                        {
                            System.out.println("New "+ wrds[0] + " shell "
                                             + "(scale fact. " + wrds[2] + ")");
                        }
                        shell = new Shell(wrds[0],Double.parseDouble(
                                NumberUtils.formatScientificNotation(wrds[2])));
                    }
                    else if (wrds.length==2 
                                               && NumberUtils.isNumber(wrds[0])
                                               && NumberUtils.isNumber(wrds[1]))
                    {
                        Primitive p = new Primitive();
                        p.setType(shell.getType());
                        p.setExponent(Double.parseDouble(
                                NumberUtils.formatScientificNotation(wrds[0])));
                        p.setExpPrecision(NumberUtils.getPrecision(wrds[0]));
                        p.setCoefficient(Double.parseDouble(
                                NumberUtils.formatScientificNotation(wrds[1])));
                        p.setCoeffPrecision(NumberUtils.getPrecision(wrds[1]));
                        shell.add(p);
                        if (verbosity > 2)
                        {
                            System.out.println("Adding primitive " + p);
                        }
                    } 
                    else if (wrds.length>2 
                            && NumberUtils.isNumber(wrds[0])
                            && NumberUtils.isNumber(wrds[1])
                            && NumberUtils.isNumber(wrds[2]))
					{
					    Primitive p = new Primitive();
					    p.setType(shell.getType());
					    p.setExponent(Double.parseDouble(
					            NumberUtils.formatScientificNotation(wrds[0])));
					    p.setExpPrecision(NumberUtils.getPrecision(wrds[0]));
					    for (int ic=1; ic<wrds.length; ic++)
					    {
					    	p.appendCoefficient(Double.parseDouble(
					    			NumberUtils.formatScientificNotation(
					    					wrds[ic])));
					    	if (ic==1)
					    	{
					    		p.setCoeffPrecision(NumberUtils.getPrecision(
					    				wrds[ic]));
					    	}
					    }
					    shell.add(p);
					    if (verbosity > 2)
					    {
					        System.out.println("Adding primitive " + p);
					    }
					} 
                    else
                    {
                        msg = "ERROR! Unable to understand line '" + line + "' "
                             + "in basis set section of file '" + inFile + "' "
                             + "line " + (i+1) + ". Words:" + wrds.length;
                        if (wrds.length>=1)
                        {
                             msg = msg + ". " + wrds[0]
                             + " isNum: " + NumberUtils.isNumber(wrds[0]);
                        }
                        if (wrds.length>=2)
                        {
                             msg = msg + "; " + wrds[1]
                             + " isNum: " + NumberUtils.isNumber(wrds[1]);
                        }
                        if (wrds.length==3)
                        {
                            msg = msg + "; " + wrds[2] 
                             + " isNum:" + NumberUtils.isNumber(wrds[2]);
                        }

                        Terminator.withMsgAndStatus(msg,-1);
                    }
                }
                if (shell.getSize() > 0)
                {
                     cbs.addShell(shell);
                }
            }
            // Parse the ECP section
            else if (wrds.length == 2 && wrds[0].matches("\\w+\\.?") 
                                        && wrds[1].equals("0") && isECPSection)
            {
                String atmId = wrds[0].toUpperCase();
                cbs = bs.getCenterBasisSetForCenter(atmId);
                cbs.setElement(atmId);
                if (verbosity > 2)
                {
                    System.out.println("Importing ECP for '" + atmId+ "' "
                                       + " element:'" + cbs.getElement()+ "'.");
                }
                boolean keepReadingECP = true;
                boolean addIntECCP = false;
                ECPShell ecps = new ECPShell();
                while (keepReadingECP && i<(lines.size()-1))
                {
                    i++;
                    line = lines.get(i);

                    if (line.trim().equals(""))
                    {
                        keepReadingECP = false;
                        continue;
                    }

                    wrds = line.trim().split("\\s+");

                    // NB: since there is no separator between ECPs of different
                    // centers we must check for the first line of a new
                    // ECP block and update the CenterBasisSet object
                    if (wrds.length == 2 && wrds[0].matches("\\w+\\.?")
                                                         && wrds[1].equals("0"))
                    {
                        if (ecps.getSize() > 0)
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Appending last ECP shell '" 
                                                    + ecps.getType() + "' (A)");
                            }
                            cbs.addECPShell(ecps.clone());
                        }

                        cbs = bs.getCenterBasisSetForCenter(wrds[0]);
                        cbs.setElement(wrds[0]);
                        addIntECCP = false;

                        if (verbosity > 2)
                        {
                            System.out.println("Importing ECP for '" + wrds[0] 
                                                                         + "'");
                        }
                    }
                    else if (wrds.length==3 && wrds[0].matches(".*[a-zA-Z].*")
                                                   && wrds[1].matches("-?\\d+")
                                                   && wrds[2].matches("-?\\d+"))
                    {
                        //Read the max.ang.mom. and number of electrons
                        cbs.setECPType(wrds[0]);
                        cbs.setECPMaxAngMom(Integer.parseInt(wrds[1]));
                        cbs.setElectronsInECP(Integer.parseInt(wrds[2]));
                        if (verbosity > 2)
                        {
                            System.out.println("Setting Max.Ang.Mom. "+wrds[1]);
                            System.out.println("Setting nr. core el. "+wrds[2]);
                        }
                    }
                    else if (wrds.length==2 && wrds[0].matches(".*[a-zA-Z].*")
                                            && wrds[1].matches(".*[a-zA-Z].*"))
                    {
                        if (addIntECCP && ecps.getSize()>0)
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Appending ECP shell '" 
                                                    + ecps.getType() + "' (B)");
                            }
                            cbs.addECPShell(ecps.clone());
                        }
                        String ecpt = wrds[0] + " " + wrds[1];
                        ecps = new ECPShell(ecpt);
                        if (verbosity > 2)
                        {
                            System.out.println("New ECP shell '" + ecpt + "'");
                        }
                        //NB: we skip the line reporting the numebr of functions
                        i++;
                    }
                    // ECP from SDD website use a single word format
                    else if (wrds.length==1 && 
                                             wrds[0].matches(".*[a-zA-Z._-].*"))
                    {
                        if (addIntECCP && ecps.getSize()>0)
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Appending ECP shell '"
                                                    + ecps.getType() + "' (B)");
                            }
                            cbs.addECPShell(ecps.clone());
                        }
                        String ecpt = wrds[0];
                        ecps = new ECPShell(ecpt);
                        if (verbosity > 2)
                        {
                            System.out.println("New ECP shell '" + ecpt + "'");
                        }
                        //NB: we skip the line reporting the numebr of functions
                        i++;
                    }
                    else if (wrds.length==3 && wrds[0].matches("-?\\d+")
                                               && NumberUtils.isNumber(wrds[1])
                                               && NumberUtils.isNumber(wrds[2]))
                    {
                        Primitive p = new Primitive();
                        p.setAngularMomentum(Integer.parseInt(wrds[0]));
                        p.setExponent(Double.parseDouble(
                                NumberUtils.formatScientificNotation(wrds[1])));
                        p.setExpPrecision(NumberUtils.getPrecision(wrds[1]));
                        p.setCoefficient(Double.parseDouble(
                                NumberUtils.formatScientificNotation(wrds[2])));
                        p.setCoeffPrecision(NumberUtils.getPrecision(wrds[2]));
                        ecps.add(p);
                        addIntECCP = true;
                        if (verbosity > 2)
                        {
                            System.out.println("Adding ECP component " + p);
                        }
                    }
                    else
                    {
                        msg = "ERROR! Unable to understand line '" + line + "' "
                             + "in ECP section of file '" + inFile + "' "
                             + "line " + (i+1) + ".";
                        Terminator.withMsgAndStatus(msg,-1);
                    }
                }
                if (ecps.getSize()>0)
                {
                    if (verbosity > 2)
                    {
                        System.out.println("Adding ECP final shell '"
                                                    + ecps.getType() + "' (C)");
                    }

                    cbs.addECPShell(ecps.clone());
                }
            }
            else
            {
                msg = "ERROR! Unable to understand line '" + line + "' in "
                      + "basis set file '" + inFile + "' in line " + (i+1);
                Terminator.withMsgAndStatus(msg,-1);
            }
        }
        
        return bs;
    }

//------------------------------------------------------------------------------

    /**
     * Writes a basis set to an output file according to the given format.
     * Known formats are listed in the {@link BasisSet} documentation.
     * @param bs the basis set to be written
     * @param format the format of the basis set (i.e., the name of the software
     * package meant to read the basis set) 
     * @param out the pathname of the output file
     */

    public static void writeFormattedBS(BasisSet bs, String format, String out)
    {
        IOtools.writeTXTAppend(out,bs.toInputFileString(format),true);
    }

//----------------------------------------------------------------------------- 
 
}
