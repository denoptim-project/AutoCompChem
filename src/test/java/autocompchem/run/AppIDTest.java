package autocompchem.run;

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

import org.junit.jupiter.api.Test;


public class AppIDTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testDefaultRunnability() throws Exception
    {
    	assertTrue(AppID.ACC.isRunnableByACC());
    	assertTrue(AppID.SHELL.isRunnableByACC());
    	assertFalse(AppID.UNDEFINED.isRunnableByACC());
    	assertFalse(AppID.GAUSSIAN.isRunnableByACC());
    }

//------------------------------------------------------------------------------

}
