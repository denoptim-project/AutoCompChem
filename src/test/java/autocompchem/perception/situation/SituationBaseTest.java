package autocompchem.perception.situation;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import autocompchem.io.ACCJson;
import autocompchem.perception.SCPair;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.circumstance.CountTextMatches;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;

/**
 * Unit Test
 * 
 * @author Marco Foscato
 */

public class SituationBaseTest 
{
	public static SituationBase getTestSituationBase()
	{
		SituationBase sb = new SituationBase();
		sb.addSituation(new Situation("Sit", "A", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2", 
								InfoChannelType.INPUTFILE),
						new MatchText("txtQuery2", 
								InfoChannelType.LOGFEED),
						new MatchText("txtQuery3", true, 
								InfoChannelType.ANY),
						new CountTextMatches("counterA", 10, 
								InfoChannelType.LOGFEED),
						new CountTextMatches("counterB", 10, 
								InfoChannelType.OUTPUTFILE))), 
				new Action(ActionType.SKIP, ActionObject.FOCUSJOB)));

		sb.addSituation(new Situation("Sit", "B", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1B",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2", 
								InfoChannelType.OUTPUTFILE),
						new MatchText("txtQuery2", 
								InfoChannelType.LOGFEED),
						new MatchText("txtQuery3", true, 
								InfoChannelType.ANY))), 
				new Action(ActionType.STOP, ActionObject.PARALLELJOB)));
		
		sb.addSituation(new Situation("Sit", "C", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2", 
								InfoChannelType.ANY),
						new MatchText("txtQuery3C", true, 
								InfoChannelType.ANY))), 
				new Action(ActionType.STOP, ActionObject.PARALLELJOB)));
		
		return sb;
	}
	
//-----------------------------------------------------------------------------

	@Test
	public void testGetAllTxTQueriesForICT() throws Exception
	{
		SituationBase sb =getTestSituationBase();
		Set<TxtQuery> tqs = sb.getAllTxTQueriesForICT(
				InfoChannelType.INPUTFILE, false);
		assertEquals(1,tqs.size());
		boolean found = false;
		for (TxtQuery tq : tqs)
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.s==sb.getSituation(0) &&
						sc.c == sb.getSituation(0).getCircumstances().get(1))
					found = true;
			}
		}
		assertTrue(found);
		
		tqs = sb.getAllTxTQueriesForICT(InfoChannelType.LOGFEED, false);
		assertEquals(2,tqs.size());
		Set<String> expectedStrings = new HashSet<String>();
		expectedStrings.add("txtQuery2");
		expectedStrings.add("counterA");
		Set<String> foundStrings = new HashSet<String>();
		for (TxtQuery tq : tqs)
			foundStrings.add(tq.query);
		assertEquals(expectedStrings, foundStrings);
		
		tqs = sb.getAllTxTQueriesForICT(InfoChannelType.INPUTFILE, true);
		assertEquals(3,tqs.size());
		expectedStrings = new HashSet<String>();
		expectedStrings.add("txtQuery2");
		expectedStrings.add("txtQuery3");
		expectedStrings.add("txtQuery3C");
		foundStrings = new HashSet<String>();
		for (TxtQuery tq : tqs)
			foundStrings.add(tq.query);
		assertEquals(expectedStrings, foundStrings);
	}
    
//-----------------------------------------------------------------------------
	
	@Test
	public void testAddAndIndexingSituations() throws Exception
	{
		SituationBase sb = new SituationBase();
		
		ICircumstance c1 = new MatchText("q1", InfoChannelType.INPUTFILE);
		ICircumstance c2 = new MatchText("q2", InfoChannelType.LOGFEED);
		ICircumstance c3 = new MatchText("q2", InfoChannelType.ANY);
		ICircumstance c4 = new MatchText("q2", InfoChannelType.INPUTFILE);
		ICircumstance c5 = new CountTextMatches("q2", 22, InfoChannelType.ANY);
		ICircumstance c6 = new CountTextMatches("q2", 33, InfoChannelType.OUTPUTFILE);
		ICircumstance c7 = new CountTextMatches("q2", 1, 2, InfoChannelType.ANY);
		ICircumstance c8 = new MatchText("q3", InfoChannelType.ANY);
		
		sb.addSituation(new Situation("Sit", "A", new ArrayList<ICircumstance>(
				Arrays.asList(c1,c2,c3,c4,c5,c6,c7,c8))));
		
		assertEquals(1, sb.getSituationCount());
		
		Set<String> expectedQrys = new HashSet<String>(Arrays.asList(
				"q1","q2","q3"));
		Map<String,TxtQuery> byStr = sb.getAllTxTQueriesByQuery();
		
		assertEquals(expectedQrys, byStr.keySet());
		assertEquals(1, byStr.get("q1").sources.size());
		assertTrue(c1 == byStr.get("q1").sources.get(0).c);
		assertEquals(6, byStr.get("q2").sources.size());
		assertTrue(c2 == byStr.get("q2").sources.get(0).c);
		assertTrue(c3 == byStr.get("q2").sources.get(1).c);
		assertTrue(c4 == byStr.get("q2").sources.get(2).c);
		assertTrue(c5 == byStr.get("q2").sources.get(3).c);
		assertTrue(c6 == byStr.get("q2").sources.get(4).c);
		assertTrue(c7 == byStr.get("q2").sources.get(5).c);
		assertEquals(1, byStr.get("q3").sources.size());
		assertTrue(c8 == byStr.get("q3").sources.get(0).c);
		
		Set<InfoChannelType> expectedICTs = new HashSet<InfoChannelType>(
				Arrays.asList(InfoChannelType.INPUTFILE, 
						InfoChannelType.LOGFEED, InfoChannelType.ANY,
						InfoChannelType.OUTPUTFILE));
		Map<InfoChannelType,Set<TxtQuery>> byICT = sb.getAllTxTQueriesByICT();
	
		assertEquals(expectedICTs, byICT.keySet());
		assertEquals(2, byICT.get(InfoChannelType.INPUTFILE).size()); // q1 q2
		assertEquals(1, byICT.get(InfoChannelType.LOGFEED).size()); // q2
		assertEquals(2, byICT.get(InfoChannelType.ANY).size()); // q2 and q3
		assertEquals(1, byICT.get(InfoChannelType.OUTPUTFILE).size()); // q2
		boolean foundC1 = false;
		boolean foundC2 = false;
		boolean foundC3 = false;
		boolean foundC4 = false;
		boolean foundC5 = false;
		boolean foundC6 = false;
		boolean foundC7 = false;
		boolean foundC8 = false;
		
		for (TxtQuery tq : byICT.get(InfoChannelType.INPUTFILE))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c1)
					foundC1=true;
				if (sc.c==c4)
					foundC4=true;
			}
		}
		assertTrue(foundC1);
		assertTrue(foundC4);
		
		for (TxtQuery tq : byICT.get(InfoChannelType.LOGFEED))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c2)
					foundC2=true;
			}
		}
		assertTrue(foundC2);

		for (TxtQuery tq : byICT.get(InfoChannelType.ANY))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c3)
					foundC3=true;
				if (sc.c==c5)
					foundC5=true;
				if (sc.c==c7)
					foundC7=true;
				if (sc.c==c8)
					foundC8=true;
			}
		}
		assertTrue(foundC3);
		assertTrue(foundC5);
		assertTrue(foundC7);
		assertTrue(foundC8);
		
		for (TxtQuery tq : byICT.get(InfoChannelType.OUTPUTFILE))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c6)
					foundC6=true;
			}
		}
		assertTrue(foundC6);
		
		// Adding of none
		sb.addSituation(new Situation());
		assertEquals(2, sb.getSituationCount());
		byStr = sb.getAllTxTQueriesByQuery();
		assertEquals(expectedQrys, byStr.keySet());
		assertEquals(1, byStr.get("q1").sources.size());
		assertEquals(6, byStr.get("q2").sources.size());
		assertEquals(1, byStr.get("q3").sources.size());
		byICT = sb.getAllTxTQueriesByICT();
		assertEquals(expectedICTs, byICT.keySet());
		assertEquals(2, byICT.get(InfoChannelType.INPUTFILE).size()); // q1 q2
		assertEquals(1, byICT.get(InfoChannelType.LOGFEED).size()); // q2
		assertEquals(2, byICT.get(InfoChannelType.ANY).size()); // q2 and q3
		assertEquals(1, byICT.get(InfoChannelType.OUTPUTFILE).size()); // q2
		
		// Adding of non-text
		ICircumstance cNonTxt = new ICircumstance() {
			public boolean scoreToDecision(double dScore) {
				return false;
			}
			public InfoChannelType getChannelType() {
				return null;
			}
			@Override
			public TreeMap<String, JsonElement> getJsonMembers(
					JsonSerializationContext context) {
				return null;
			}
		};
		sb.addSituation(new Situation("Sit", "A", new ArrayList<ICircumstance>(
				Arrays.asList(cNonTxt))));
		assertEquals(3, sb.getSituationCount());
		byStr = sb.getAllTxTQueriesByQuery();
		assertEquals(expectedQrys, byStr.keySet());
		assertEquals(1, byStr.get("q1").sources.size());
		assertEquals(6, byStr.get("q2").sources.size());
		assertEquals(1, byStr.get("q3").sources.size());
		byICT = sb.getAllTxTQueriesByICT();
		assertEquals(expectedICTs, byICT.keySet());
		assertEquals(2, byICT.get(InfoChannelType.INPUTFILE).size()); // q1 q2
		assertEquals(1, byICT.get(InfoChannelType.LOGFEED).size()); // q2
		assertEquals(2, byICT.get(InfoChannelType.ANY).size()); // q2 q3
		assertEquals(1, byICT.get(InfoChannelType.OUTPUTFILE).size()); // q2
		
		// Further adding of text-based circumstance
		ICircumstance c10 = new CountTextMatches("q2", 33, InfoChannelType.OUTPUTFILE);
		ICircumstance c11 = new CountTextMatches("q4", 1, 2, InfoChannelType.ANY);
		ICircumstance c12 = new MatchText("q3", InfoChannelType.JOBDETAILS);
		sb.addSituation(new Situation("Sit", "C", new ArrayList<ICircumstance>(
				Arrays.asList(c10, c11, c12))));
		assertEquals(4, sb.getSituationCount());
		byStr = sb.getAllTxTQueriesByQuery();
		expectedQrys.add("q4");
		assertEquals(expectedQrys, byStr.keySet());
		assertEquals(1, byStr.get("q1").sources.size());
		assertEquals(7, byStr.get("q2").sources.size());
		assertEquals(2, byStr.get("q3").sources.size());
		assertEquals(1, byStr.get("q4").sources.size());
		byICT = sb.getAllTxTQueriesByICT();
		expectedICTs.add(InfoChannelType.JOBDETAILS);
		assertEquals(expectedICTs, byICT.keySet());
		assertEquals(2, byICT.get(InfoChannelType.INPUTFILE).size()); // q1 q2
		assertEquals(1, byICT.get(InfoChannelType.LOGFEED).size()); // q2
		assertEquals(3, byICT.get(InfoChannelType.ANY).size()); // q2 q3 q4
		assertEquals(1, byICT.get(InfoChannelType.OUTPUTFILE).size()); // q2
		assertEquals(1, byICT.get(InfoChannelType.JOBDETAILS).size()); // q2
		
		boolean foundC10 = false;
		boolean foundC11 = false;
		boolean foundC12 = false;
		
		for (TxtQuery tq : byICT.get(InfoChannelType.OUTPUTFILE))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c10)
					foundC10=true;
			}
		}
		assertTrue(foundC10);

		for (TxtQuery tq : byICT.get(InfoChannelType.ANY))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c11)
					foundC11=true;
			}
		}
		assertTrue(foundC11);

		for (TxtQuery tq : byICT.get(InfoChannelType.JOBDETAILS))
		{
			for (SCPair sc : tq.sources)
			{
				if (sc.c==c12)
					foundC12=true;
			}
		}
		assertTrue(foundC12);
	}
	
//-----------------------------------------------------------------------------

	@Test
	public void testGetRelevantSituations() throws Exception
	{
		SituationBase sb = new SituationBase();
		Situation s1 = new Situation("Sit", "A", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2", 
								InfoChannelType.INPUTFILE),
						new MatchText("txtQuery2", 
								InfoChannelType.LOGFEED),
						new MatchText("txtQuery3", true, 
								InfoChannelType.ANY),
						new CountTextMatches("counterA", 10, 
								InfoChannelType.LOGFEED),
						new CountTextMatches("counterB", 10, 
								InfoChannelType.OUTPUTFILE))), 
				new Action(ActionType.SKIP, ActionObject.FOCUSJOB));

		Situation s2 = new Situation("Sit", "B", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1B",
								InfoChannelType.NOTDEFINED),
						new MatchText("txtQuery2", 
								InfoChannelType.OUTPUTFILE),
						new MatchText("txtQuery2", 
								InfoChannelType.LOGFEED),
						new MatchText("txtQuery3", true, 
								InfoChannelType.LOGFEED))), 
				new Action(ActionType.STOP, ActionObject.PARALLELJOB));
		
		Situation s3 = new Situation("Sit", "C", 
				new ArrayList<ICircumstance>(Arrays.asList(
						new MatchText("txtQuery1", InfoChannelType.JOBDETAILS),
						new MatchText("txtQuery2", 
								InfoChannelType.INPUTFILE),
						new MatchText("txtQuery3C", true, 
								InfoChannelType.INPUTFILE))), 
				new Action(ActionType.STOP, ActionObject.PARALLELJOB));

		sb.addSituation(s1);
		sb.addSituation(s2);
		sb.addSituation(s3);
		List<Situation> sits = sb.getRelevantSituations(InfoChannelType.OUTPUTFILE);
		assertEquals(2, sits.size());
		assertTrue(sits.contains(s1));
		assertTrue(sits.contains(s2));
		assertFalse(sits.contains(s3));
		
		InfoChannelBase icb = new InfoChannelBase();
		icb.addChannel(new FileAsSource("some/file1.txt", 
				InfoChannelType.JOBDETAILS));
		icb.addChannel(new FileAsSource("some/file2.txt", 
				InfoChannelType.INPUTFILE));
		
		sits = sb.getRelevantSituations(icb);
		assertEquals(2, sits.size());
		assertTrue(sits.contains(s1));
		assertTrue(sits.contains(s3));
		assertFalse(sits.contains(s2));
	}
    
//------------------------------------------------------------------------------
    
    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	SituationBase original = getTestSituationBase();
         
         Gson writer = ACCJson.getWriter();
         Gson reader = ACCJson.getReader();
          
         String json = writer.toJson(original);
          
         SituationBase fromJson = reader.fromJson(json, SituationBase.class);
         assertEquals(original, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	SituationBase a1 = getTestSituationBase();
    	SituationBase a2 = getTestSituationBase();

        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        assertTrue(a1.equals(a1));
        assertFalse(a1.equals(null));

        a2 = getTestSituationBase();
        a2.addSituation(SituationTest.getTestSituation()); 
        assertFalse(a1.equals(a2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	SituationBase s1 = getTestSituationBase();
    	SituationBase cl1 = s1.clone();
		assertTrue(s1.equals(cl1));
		assertFalse(s1 == cl1);
		
        s1.addSituation(SituationTest.getTestSituation()); 
		assertFalse(s1.equals(cl1));
    }

//------------------------------------------------------------------------------

}
