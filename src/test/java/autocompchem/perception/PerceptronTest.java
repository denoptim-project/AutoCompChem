package autocompchem.perception;

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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import autocompchem.perception.circumstance.CountTextMatches;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.EnvironmentAsSource;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.ShortTextAsSource;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;


/**
 * Unit Test for the perceptron. 
 * 
 * @author Marco Foscato
 */

public class PerceptronTest 
{
    private final String NL = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetTxtMatchesFromReader() throws Exception
    {
    	ArrayList<String> a = new ArrayList<String>();
        a.add("line0: Text to analyze");
        a.add("line1: array contains QRY@1 and QRY@2...");
        a.add("line2: more lnes with QRY@1, but this time with QRY@3");
        a.add("line3: QRY@4 in text");

        ShortTextAsSource ic = new ShortTextAsSource(a);
        ic.setType(InfoChannelType.OUTPUTFILE);
        
        TxtQuery tq1 = new TxtQuery(".*QRY@1.*", null, null);
        TxtQuery tq2 = new TxtQuery(".*QRY@2.*", null, null);
        TxtQuery tq3 = new TxtQuery(".*QRY@3.*", null, null);
        TxtQuery tq4 = new TxtQuery(".*QRY@4.*", null, null);
        TxtQuery tq5 = new TxtQuery(".*NOMATCH.*", null, null);
        List<TxtQuery> queries = new ArrayList<TxtQuery>(Arrays.asList(
        		tq1, tq2, tq3, tq4, tq5));
        
    	Map<TxtQuery,List<String>> map = Perceptron.getTxtMatchesFromICReader(
    			queries, ic, 0);
    	
    	assertEquals(queries.size(), map.size());
    	assertEquals(2, map.get(tq1).size());
    	assertEquals(1, map.get(tq2).size());
    	assertEquals(1, map.get(tq3).size());
    	assertEquals(1, map.get(tq4).size());
    	assertEquals(0, map.get(tq5).size());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testSourceStreams() throws Exception
    {
        ArrayList<String> a = new ArrayList<String>();
        a.add("Text in array");
        a.add("array contains blabla...");
        a.add("more blabla");
        a.add("PATH in array");

        ShortTextAsSource st = new ShortTextAsSource(a);
        st.setType(InfoChannelType.OUTPUTFILE);

        EnvironmentAsSource env = new EnvironmentAsSource();
        env.setType(InfoChannelType.ENVIRONMENT);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st);
        icb.addChannel(env);

        String sInFile = "PATH and blabla in file";
        InfoChannel icFile;
        try 
        {
            String fileName = "/tmp/tmpTxtFileForJUnitTestingOfPerceptron";
            FileWriter writer = new FileWriter(fileName);
            writer.write(sInFile + NL);
            writer.close();
            icFile = new FileAsSource(fileName);
            icFile.setType(InfoChannelType.OUTPUTFILE);
            icb.addChannel(icFile);
        }
        catch (Throwable t)
        {
            System.err.println(NL + NL +  "WARNING! Unable to create tmp file "
                        + "for testing perception. I'll avoid tmp files." + NL);
        }

        Situation sit1 = new Situation("");
        sit1.addCircumstance(new MatchText(".*blabla.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText(".*PATH.*bin.*",
                                       InfoChannelType.ENVIRONMENT));
        sit1.addCircumstance(new MatchText(".*PATH.*",
                                       InfoChannelType.ANY));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        Perceptron prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness");
        assertEquals(1,prc.getOccurringSituations().size(),
                                              "Number of occurring situations");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                         "Occurring situation");

        Situation sit2 = new Situation("");
        sit2.addCircumstance(new MatchText(".*not-in-out.*",true,
                                       InfoChannelType.OUTPUTFILE));
        sit2.addCircumstance(new MatchText(".*not-in-env.*",true,
                                       InfoChannelType.ENVIRONMENT));
        sit2.addCircumstance(new MatchText(".*not-in-any.*",true,
                                       InfoChannelType.ANY));

        sitsBase1.addSituation(sit2);

        prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(false,prc.isAware(),"Perception awareness (2)");
        assertEquals(2,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (2)");
    }

//------------------------------------------------------------------------------

    @Test
    public void testSimplePerception() throws Exception
    {
        ShortTextAsSource st = new ShortTextAsSource("I'm aware!");
        st.setType(InfoChannelType.OUTPUTFILE);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st);

        Situation sit1 = new Situation("Source exists and contains text");
        sit1.addCircumstance(new MatchText("I'm aware!",
                                       InfoChannelType.OUTPUTFILE));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);        

        Perceptron  prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness");
        assertEquals(1,prc.getOccurringSituations().size(),
                                              "Number of occurring situations");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                         "Occurring situation");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNegationOfTextMatches() throws Exception
    {
        ShortTextAsSource st = new ShortTextAsSource("text in source");
        st.setType(InfoChannelType.OUTPUTFILE);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st);

        Situation sit1 = new Situation("Source exists and text !matched");
        sit1.addCircumstance(new MatchText("text in source",true,
                                       InfoChannelType.OUTPUTFILE));

        Situation sit2 = new Situation("Source exist and text matched");
        sit2.addCircumstance(new MatchText("text in source",false,
                                       InfoChannelType.OUTPUTFILE));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);
        sitsBase1.addSituation(sit2);

        Perceptron prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness");
        assertEquals(1,prc.getOccurringSituations().size(),
                                              "Number of occurring situations");
        assertEquals(sit2,prc.getOccurringSituations().get(0),
                                                         "Occurring situation");

        // Negation must not be matched by missing source
        Situation sit3 = new Situation("Source !exist");
        sit3.addCircumstance(new MatchText("text in source",true,
                                       InfoChannelType.LOGFEED));

        SituationBase sitsBase2 = new SituationBase();
        sitsBase2.addSituation(sit3);
        prc = new Perceptron(sitsBase2,icb);
        prc.perceive();

        assertEquals(false,prc.isAware(),"Awareness when sources lack");
        assertEquals(0,prc.getOccurringSituations().size(),
                              "Number of occurring situations if sources lack");
        assertIterableEquals(new ArrayList<Situation>(),
                                                  prc.getOccurringSituations(),
                           "Empty list of occurring situation if sources lack");
    }

//------------------------------------------------------------------------------

    @Test
    public void testManyConditionsSameSource() throws Exception
    {
        ArrayList<String> a = new ArrayList<String>();
        a.add("text in source");
        a.add("more and more text");
        a.add("@end");

        ShortTextAsSource st = new ShortTextAsSource(a);
        st.setType(InfoChannelType.OUTPUTFILE);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st);
   
        Situation sit1 = new Situation("Multiple circumstances, same source");
        sit1.addCircumstance(new MatchText("text in source",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText("more.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText("text.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText("@end",
                                       InfoChannelType.OUTPUTFILE));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        Perceptron prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        //prc.printScores();
        //icb.printInfoChannels();

        assertEquals(true,prc.isAware(),"Perception awareness");
        assertEquals(1,prc.getOccurringSituations().size(),
                                              "Number of occurring situations");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                         "Occurring situation");
    }

//------------------------------------------------------------------------------

    @Test
    public void testMultiConditionMultiSource() throws Exception
    {
        ShortTextAsSource st1 = new ShortTextAsSource("text in source");
        st1.setType(InfoChannelType.INPUTFILE);

        ShortTextAsSource st2 = new ShortTextAsSource("more %%% and more text");
        st2.setType(InfoChannelType.OUTPUTFILE);

        ShortTextAsSource st3 = new ShortTextAsSource("@end");
        st3.setType(InfoChannelType.LOGFEED);

        ShortTextAsSource st4 = new ShortTextAsSource("text in another file");
        st4.setType(InfoChannelType.OUTPUTFILE);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st1);
        icb.addChannel(st2);
        icb.addChannel(st3);
        icb.addChannel(st4);

        Situation sit1 = new Situation("Multiple circumstances and sourcs");
        sit1.addCircumstance(new MatchText("text in source",
                                       InfoChannelType.INPUTFILE));
        sit1.addCircumstance(new MatchText("more.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText(".*text",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText(".*another.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText(".*text.*",
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new MatchText(".*&.*",true,
                                       InfoChannelType.ANY));
        sit1.addCircumstance(new MatchText("@end",
                                       InfoChannelType.LOGFEED));
        sit1.addCircumstance(new MatchText("text",true,
                                       InfoChannelType.LOGFEED));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        Perceptron prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness");
        assertEquals(1,prc.getOccurringSituations().size(),
                                              "Number of occurring situations");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                         "Occurring situation");
    }

//------------------------------------------------------------------------------

    @Test
    public void testMultiSituation() throws Exception
    {
        ShortTextAsSource st1 = new ShortTextAsSource("text in source");
        st1.setType(InfoChannelType.OUTPUTFILE);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st1);

        Situation sit1 = new Situation("S1");
        sit1.addCircumstance(new MatchText("text in source",
                                       InfoChannelType.OUTPUTFILE));
        Situation sit2 = new Situation("S2");
        sit2.addCircumstance(new MatchText("text in source",
                                       InfoChannelType.OUTPUTFILE));
        Situation sit3 = new Situation("S3");
        sit3.addCircumstance(new MatchText("text in source",
                                       InfoChannelType.OUTPUTFILE));

        SituationBase sitsBase1 = new SituationBase();

        Perceptron  prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(false,prc.isAware(),"Perception awareness (1)");
        assertEquals(0,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (1)");
        assertIterableEquals(new ArrayList<Situation>(),
                                                  prc.getOccurringSituations(),
                                           "Empty list of occurring situation");

        sitsBase1.addSituation(sit1);
        prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness (2)");
        assertEquals(1,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (2)");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                     "Occurring situation (2)");

        sitsBase1.addSituation(sit2);
        prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(false,prc.isAware(),"Perception awareness (3)");
        assertEquals(2,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (3)");

        sitsBase1.addSituation(sit3);
        prc = new Perceptron(sitsBase1,icb);
        prc.perceive();

        assertEquals(false,prc.isAware(),"Perception awareness (4)");
        assertEquals(3,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (4)");
    }

//------------------------------------------------------------------------------

    @Test
    public void testCountTextMatches() throws Exception
    {
        //NB: CountTextMatches counts LINES
        ShortTextAsSource st1 = new ShortTextAsSource(
               new ArrayList<String>(Arrays.asList("here asd","asd asd here")));
        st1.setType(InfoChannelType.OUTPUTFILE);

        ShortTextAsSource st2 = new ShortTextAsSource(
               new ArrayList<String>(Arrays.asList("_here asd","more here  ff",
                       "_here asd","more here  ff","no match","@end sd here")));
        st2.setType(InfoChannelType.INPUTFILE);

        ShortTextAsSource st3 = new ShortTextAsSource(
               new ArrayList<String>(Arrays.asList("_here asd","more here  ff",
                                                   "no match","@end sd here")));
        st3.setType(InfoChannelType.LOGFEED);

        ShortTextAsSource st4 = new ShortTextAsSource(
               new ArrayList<String>(Arrays.asList("_ asd","more here  ff",
                                                   "no match","@end sd here")));
        st4.setType(InfoChannelType.JOBDETAILS);

        InfoChannelBase icb = new InfoChannelBase();
        icb.addChannel(st1);
        icb.addChannel(st2);
        icb.addChannel(st3);
        icb.addChannel(st4);

        Situation sit1 = new Situation("Count matches");
        sit1.addCircumstance(new CountTextMatches(".*here.*", 2,
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 5, true, //MIN
                                                    InfoChannelType.INPUTFILE));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3, false, //MAX
                                                    InfoChannelType.LOGFEED));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 2,2, //RANGE 
                                                   InfoChannelType.JOBDETAILS));

        SituationBase sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        Perceptron prc = new Perceptron(sitsBase1,icb);
        //prc.setVerbosity(3);
        prc.perceive();

        assertEquals(true,prc.isAware(),"Perception awareness (1)");
        assertEquals(1,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (1)");
        assertEquals(sit1,prc.getOccurringSituations().get(0),
                                                     "Occurring situation (1)");

        sit1 = new Situation("Count matches");
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3,
                                       InfoChannelType.OUTPUTFILE));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 6, true, //MIN
                                                    InfoChannelType.INPUTFILE));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 1, false, //MAX
                                                    InfoChannelType.LOGFEED));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3,4, //RANGE
                                                   InfoChannelType.JOBDETAILS));

        sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        prc = new Perceptron(sitsBase1,icb);
        //prc.setVerbosity(3);
        prc.perceive();
        //prc.printScores();

        assertEquals(false,prc.isAware(),"Perception awareness (2)");
        assertEquals(0,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (2)");

        // Test with negation in exact match an range
        sit1 = new Situation("Count matches");
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3,
                                              InfoChannelType.OUTPUTFILE,true));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 5, true, //MIN
                                                    InfoChannelType.INPUTFILE));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3, false, //MAX
                                                    InfoChannelType.LOGFEED));
        sit1.addCircumstance(new CountTextMatches(".*here.*", 3,4, //RANGE
                                              InfoChannelType.JOBDETAILS,true));

        sitsBase1 = new SituationBase();
        sitsBase1.addSituation(sit1);

        prc = new Perceptron(sitsBase1,icb);
        //prc.setVerbosity(3);
        prc.perceive();
        //prc.printScores();

        assertEquals(true,prc.isAware(),"Perception awareness (3)");
        assertEquals(1,prc.getOccurringSituations().size(),
                                          "Number of occurring situations (3)");
    }

//------------------------------------------------------------------------------

}
