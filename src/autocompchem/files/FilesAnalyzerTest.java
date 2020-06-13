package autocompchem.files;

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

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import autocompchem.io.IOtools;


/**
 * Unit Test for text analyzer
 * 
 * @author Marco Foscato
 */

public class FilesAnalyzerTest 
{
    @Test
    public void testCountMatches() throws Exception
    {
	String tmpPathName = "/tmp/__tmp_acc_junit";
	IOtools.writeTXTAppend(tmpPathName,"First line",false);
	IOtools.writeTXTAppend(tmpPathName,"Second line",true);
	IOtools.writeTXTAppend(tmpPathName,"Third line",true);
	
	assertEquals(3,FilesAnalyzer.count(tmpPathName,"line"),"Total matches");
    }

}
