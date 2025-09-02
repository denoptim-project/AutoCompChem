package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.JobDetailsAsSource;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.CompChemJobTest;
import autocompchem.wiro.chem.Directive;
import autocompchem.perception.infochannel.FileAsSource;

public class JobDetailsAsSourceTest {
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	JobDetailsAsSource ic = new JobDetailsAsSource(
    			 CompChemJobTest.getTextCompChemJob());
    	ic.setType(InfoChannelType.INPUTFILE);
    	
    	String json = writer.toJson(ic);
    	
    	JobDetailsAsSource fromJson = reader.fromJson(json, 
    			JobDetailsAsSource.class);
    	assertEquals(ic, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	JobDetailsAsSource ic1 = new JobDetailsAsSource(
    			 CompChemJobTest.getTextCompChemJob());
    	ic1.setType(InfoChannelType.JOBDETAILS);

    	JobDetailsAsSource ic2 = new JobDetailsAsSource(
    			 CompChemJobTest.getTextCompChemJob());
    	ic2.setType(InfoChannelType.JOBDETAILS);

      	assertTrue(ic1.equals(ic2));
      	assertTrue(ic2.equals(ic1));
      	assertTrue(ic1.equals(ic1));
      	assertFalse(ic1.equals(null));
      	
      	CompChemJob job = CompChemJobTest.getTextCompChemJob();
      	job.removeDirective(job.getDirective("A"));
      	ic2 = new JobDetailsAsSource(job);
    	ic2.setType(InfoChannelType.JOBDETAILS);
      	assertFalse(ic1.equals(ic2));

      	ic2 = new JobDetailsAsSource(
   			 CompChemJobTest.getTextCompChemJob());
    	ic2.setType(InfoChannelType.OUTPUTFILE);
      	assertFalse(ic1.equals(ic2));
      	
      	ic2 = new JobDetailsAsSource(job);
    	ic2.setType(InfoChannelType.LOGFEED);
    	assertFalse(ic1.equals(ic2));
    }
    
//------------------------------------------------------------------------------

}
