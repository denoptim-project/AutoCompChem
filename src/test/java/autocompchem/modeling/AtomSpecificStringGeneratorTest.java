package autocompchem.modeling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

public class AtomSpecificStringGeneratorTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testConvertTupleToAtomSpecString() throws Exception
    {
    	// These should not be put in the  string
    	Set<String> valuelessAttributes = new HashSet<String>();
    	valuelessAttributes.add("key1");
    	valuelessAttributes.add("key2");
		Map<String, String> valuedAttributes = new HashMap<String, String>();
		valuedAttributes.put("valA", "1.23");
		valuedAttributes.put("valB", "ABC");
    	// But these are special values attributes
		valuedAttributes.put(AtomTupleConstants.KEYPREFIX, "pre pre : ");
		valuedAttributes.put(AtomTupleConstants.KEYSUFFIX, "after : ");
		
    	AnnotatedAtomTuple tuple = new AnnotatedAtomTuple(
    			new ArrayList<Integer>(Arrays.asList(2, 6, 0, 10)),
    			new ArrayList<String>(Arrays.asList("a", "b", "c", "d")),
    			valuelessAttributes, 
    			valuedAttributes, 
  				null, 11);
    	
    	AnnotatedAtomTuple tupleB = new AnnotatedAtomTuple(
    			new ArrayList<Integer>(Arrays.asList(10)),
    			new ArrayList<String>(Arrays.asList("C2")),
    			valuelessAttributes, 
    			valuedAttributes, 
  				null, 11);

    	AnnotatedAtomTuple tupleC = new AnnotatedAtomTuple(
    			new ArrayList<Integer>(Arrays.asList(10)),
    			null,
    			valuelessAttributes, 
    			valuedAttributes, 
  				null, 11);
    	
    	AtomSpecificStringGenerator assg = new AtomSpecificStringGenerator();
    	
    	assertEquals("pre pre : abcdafter : ", 
    			assg.convertTupleToAtomSpecString(tuple));
    	assertEquals("pre pre : C2after : ", 
    			assg.convertTupleToAtomSpecString(tupleB));
    	assertEquals("pre pre : 10after : ", 
    			assg.convertTupleToAtomSpecString(tupleC));
    	ParameterStorage ps = new ParameterStorage();
        ps.setParameter(WorkerConstants.PARTASK,
        		AtomSpecificStringGenerator.GETATOMSPECIFICSTRINGTASK.ID);
        ps.setParameter("idSEPARATOR","@");
        ps.setParameter("fieldSEPARATOR","_");
        
        assg = (AtomSpecificStringGenerator) WorkerFactory.createWorker(ps, null);
        
    	assertEquals("pre pre : _a@b@c@d_after : ", 
    			assg.convertTupleToAtomSpecString(tuple));
    	assertEquals("pre pre : _C2_after : ", 
    			assg.convertTupleToAtomSpecString(tupleB));
    	assertEquals("pre pre : _10_after : ", 
    			assg.convertTupleToAtomSpecString(tupleC));
    	
    }
    
//------------------------------------------------------------------------------

}
