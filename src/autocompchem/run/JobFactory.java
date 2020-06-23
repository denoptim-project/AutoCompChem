package autocompchem.run;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import autocompchem.files.FilesAnalyzer;
import autocompchem.parameters.ParameterConstants;
import autocompchem.parameters.ParameterStorage;
import autocompchem.text.TextBlock;


/**
 * Factory building jobs
 * 
 * @author Marco Foscato
 */

public class JobFactory
{

//------------------------------------------------------------------------------

    /**
     * Build a Job from from an existing definition stored in a file.
     * Currently supported file formats: 
     * <ul>
     * <li><i>jobDetails</i> format.</li>
     * </ul>
     * @param pathName the pathname of the file
     * @return the collection of parameters
     */

    public static Job buildFromFile(String pathName)
    {
        ArrayList<TextBlock> blocks = FilesAnalyzer.extractTextBlocks(pathName,
                                        ParameterConstants.STARTJOB, //delimiter
                                        ParameterConstants.ENDJOB, //delimiter
                                        false,  //don't take only first
                                        false); //don't include delimiters

        // Unless there is only one set of parameters the outernmost job serves
        // as a container of the possibly nested structure of sub-jobs.
        Job job = new Job();
        if (blocks.size() == 1)
        {
            job = createJob(blocks.get(0));
        }
        else
        {
            for (TextBlock tb : blocks)
            {
                Job subJob = createJob(tb);
                job.addStep(subJob);
            }
        }
        return job;
    }

//------------------------------------------------------------------------------

    /**
     * Create a job from the text block of the job's parameters. Handles
     * nested text blocks creating nested jobs of any deeplness.
     * @param tb the outernmost text block that may incude nested blocks
     * @return the job, possibly including nested sub-jobs
     */ 

    public static Job createJob(TextBlock tb)
    {
        ParameterStorage oneJobParams = new ParameterStorage();
        oneJobParams.importParameters(tb);

        Job job = new Job();
        //TODO: choose job class based on parameters
        job.setParameters(oneJobParams);
        if (tb.getNestedBlocks().size() > 0)
        {
            for (TextBlock intTb : tb.getNestedBlocks())
            {
                // Recursive exploration of nested structure of TextBlocks
                Job subJob = createJob(intTb);
                job.addStep(subJob);
            }
        }

        return job;
    }

//------------------------------------------------------------------------------

}
