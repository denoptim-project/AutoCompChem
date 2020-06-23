package autocompchem.perception;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;


/**
 * Unit Test for data structure storing perception scores
 * 
 * @author Marco Foscato
 */

public class ScoreCollectorTest 
{
    @Test
    public void testGettingKeyWithSameSCValues() throws Exception
    {
        ScoreCollector tsc = new ScoreCollector();
        Situation n1 = new Situation("N1");
        MatchText mcA = new MatchText("AA",InfoChannelType.LOGFEED);
        n1.addCircumstance(mcA);
        MatchText mcB = new MatchText("BB",InfoChannelType.LOGFEED);
        n1.addCircumstance(mcB);

        Situation n2 = new Situation("N2");
        n2.addCircumstance(mcA);

        SCPair scp1 = new SCPair(n1,mcA);
        SCPair scp2 = new SCPair(n1,mcB);
        SCPair scp3 = new SCPair(n1,mcA);

        tsc.addScore(scp1,0.111);
        tsc.addScore(scp2,0.222);
        tsc.addScore(scp3,0.333);
        tsc.addScore(n2,mcB,0.555);

        assertEquals(scp1,tsc.keyWithSameSCValues(scp1));
        assertEquals(scp1,tsc.keyWithSameSCValues(new SCPair(n1,mcA)));
        assertEquals(null,tsc.keyWithSameSCValues(new SCPair(n2,mcA)));
    }
}
