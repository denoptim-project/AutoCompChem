package autocompchem.molecule.intcoords.zmatrix;

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

/**
 * Storage of predefined constants for ZMatrix-related code
 *
 * @author Marco Foscato
 */

public class ZMatrixConstants
{
    /**
     * Separator for multi-entity, ACC's format ZMatrix file
     */
    public final static String ZMATMOLSEP = "$$$$";
    
    /**
     * The reference of the parameter defining is we use a selector
     * for identifying constant or variable internal coordinates.
     */
	public static final String SELECTORMODE = "SELECTORMODE";
	
	/**
	 * The selector mode used to define constants
	 */
	public static final String SELECTORMODE_CONSTANT = "CONSTANTS";
			
	/**
	 * The selector mode used to define variables
	 */
	public static final String SELECTORMODE_VARIABLES = "VARIABLES";

}
