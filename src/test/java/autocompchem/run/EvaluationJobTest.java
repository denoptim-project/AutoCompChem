package autocompchem.run;

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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;
import autocompchem.worker.WorkerFactory2;


/**
 * Unit Test for jobs meant to evaluate other jobs.
 * 
 * @author Marco Foscato
 */

public class EvaluationJobTest 
{

//-----------------------------------------------------------------------------

    @Test
    public void testConstructorWithParams() throws Exception
    {
    	ICircumstance c = new MatchText("Iteration 3",
        		InfoChannelType.LOGFEED);
        Situation s = new Situation("SituationType","TestSituation", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)));
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(s);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource());
    	Job jobToEvaluate = new Job();
    	Job evalJob = new EvaluationJob(jobToEvaluate, sitsDB, icDB);
    	
    	Worker w = WorkerFactory2.createWorker(evalJob);
    	
    	assertTrue(w instanceof JobEvaluator);
    	JobEvaluator je = (JobEvaluator) w;
    	assertTrue(je.getMyJob()==evalJob);
    	assertTrue(je.getSitsDB()==sitsDB);
    	assertTrue(je.getIcDB()==icDB);
    }

//------------------------------------------------------------------------------

}
