package autocompchem.smarts;

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


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for SMARTS class
 * 
 * @author Marco Foscato
 */

public class SMARTSTest 
{
	
//------------------------------------------------------------------------------
	
	@Test
	public void testIsSingleAtomSMARTS() throws Exception
	{
		assertFalse(SMARTS.isSingleAtomSMARTS(""));
		assertFalse(SMARTS.isSingleAtomSMARTS("CC"));
		assertFalse(SMARTS.isSingleAtomSMARTS("[#6]~O"));
		
		// Old practice is not valid anymore
		assertFalse(SMARTS.isSingleAtomSMARTS("C"));
		
		assertTrue(SMARTS.isSingleAtomSMARTS("[#1]"));
		assertTrue(SMARTS.isSingleAtomSMARTS(" [#1] "));
		assertTrue(SMARTS.isSingleAtomSMARTS("[$([#6]~[#44])]"));
	}
    
//------------------------------------------------------------------------------

}
