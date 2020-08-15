package autocompchem.io;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.io.FileInputStream;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import autocompchem.molecule.MolecularUtils;

/**
 * Extension of the IteratingSDFReader that allows to read molecules from an 
 * SDF file one by one, without storing them into memory. Suitable for huge
 * SDF file.
 * In addition this tool returns a pre-treated IAtomContainer where properties
 * line molecule title are already defined.
 * 
 * @author Marco Foscato
 */


public class SDFIterator extends IteratingSDFReader
{
    //Counter for molecules
    private int nnn;

    //Separator to extract molecule name
    private String separator;

//------------------------------------------------------------------------------

    /**
     * Constructs a new SDFIterator specifying the file to read
     * @param inFile the file to read
     * @throws Throwable in case of exceptions
     */

    public SDFIterator(String inFile) throws Throwable
    {
        super(new FileInputStream(inFile), 
                                        DefaultChemObjectBuilder.getInstance());
        this.nnn = 0;
        this.separator = "\\s+";
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new SDFIterator specifying the file to read and the
     * separator to be used to extract the first portion of molecule's name
     * @param inFile name of input file
     * @param separator string used as separator
     * @throws Throwable in case of exceptions
     */

    public SDFIterator(String inFile, String separator) throws Throwable
    {
        super(new FileInputStream(inFile), 
                                        DefaultChemObjectBuilder.getInstance());
        this.nnn = 0;
        this.separator = separator;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns true next IAtomContainer
     * @return the next container
     */

    public IAtomContainer next()
    {
        //Get Molecule
        IAtomContainer mol = super.next();

        //Update counter
        nnn++;

        //Get existing name, if any
        String molName = MolecularUtils.getNameOrID(mol);

        //Fix name
        if (molName.equals("noname"))
        {
            molName = "MOL_" + Integer.toString(nnn);
            mol.setProperty("cdk:Title",Integer.toString(nnn));
        } else {
            String[] p = molName.split(separator);
            molName = p[0];
            mol.setProperty("cdk:Title",molName);
        }

        //Return the pre-treated molecule
        return mol;
    }

//------------------------------------------------------------------------------
}
