package autocompchem.wiro.chem.crest;

import java.io.File;

/*
 *   Copyright (C) 2026  Marco Foscato
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.wiro.chem.xtb.XTBInputWriter;
import autocompchem.worker.Task;

/**
 * Writes input files for software CREST.
 *
 * @author Marco Foscato
 */

public class CRESTInputWriter extends XTBInputWriter
{
    /**
     * String defining the task of preparing input files for CREST
     */
    public static final String PREPAREINPUTCRESTTASKNAME = "prepareInputCREST";

    /**
     * Task about preparing input files for CREST
     */
    public static final Task PREPAREINPUTCRESTTASK;
    static {
    	PREPAREINPUTCRESTTASK = Task.make(PREPAREINPUTCRESTTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTCRESTTASK)));
    }
   
//------------------------------------------------------------------------------

}
