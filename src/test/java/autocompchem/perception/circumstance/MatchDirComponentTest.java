package autocompchem.perception.circumstance;


import static org.junit.jupiter.api.Assertions.assertEquals;
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

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.JobDetailsAsSource;
import autocompchem.perception.infochannel.ShortTextAsSource;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveComponentType;
import autocompchem.wiro.chem.Keyword;


public class MatchDirComponentTest 
{
//------------------------------------------------------------------------------

    public MatchDirComponent makeTestMatchDirComponent() throws Exception
    {
    	DirComponentAddress addrs_a = new DirComponentAddress();
    	addrs_a.addStep("A", DirectiveComponentType.DIRECTIVE);
    	addrs_a.addStep("AA", DirectiveComponentType.DIRECTIVE);
    	addrs_a.addStep("kA", DirectiveComponentType.KEYWORD);
    	MatchDirComponent mdc = new MatchDirComponent(addrs_a, "My Value", 1, 
    			true); 
    	return mdc;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	MatchDirComponent a = makeTestMatchDirComponent();
    	MatchDirComponent b = makeTestMatchDirComponent();

    	assertTrue(a.equals(b));
    	assertTrue(b.equals(a));
    	assertTrue(a.equals(a));
    	assertFalse(a.equals(null));
    	
    	DirComponentAddress addrs_c = new DirComponentAddress();
    	addrs_c.addStep("A", DirectiveComponentType.DIRECTIVE);
    	addrs_c.addStep("AB", DirectiveComponentType.DIRECTIVE);
    	addrs_c.addStep("kA", DirectiveComponentType.KEYWORD);
    	
    	DirComponentAddress addrs_b = a.address.clone();
    	String value_b = a.value;
    	int step_b = a.stepId;
    	boolean neg_b = a.negation;
    	
    	b = new MatchDirComponent(addrs_c, value_b, step_b, neg_b);
    	assertFalse(a.equals(b));

    	b = new MatchDirComponent(addrs_b, value_b, step_b+2, neg_b);
    	assertFalse(a.equals(b));

    	b = new MatchDirComponent(addrs_b, "different", step_b, neg_b);
    	assertFalse(a.equals(b));
    	
    	b = new MatchDirComponent(addrs_b, value_b, step_b, !neg_b);
    	assertFalse(a.equals(b));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	MatchDirComponent original = makeTestMatchDirComponent();
    	
    	String json = writer.toJson(original);
    	
    	ICircumstance fromJson = reader.fromJson(json, ICircumstance.class);
    	assertEquals(original, fromJson);
    	
    	MatchDirComponent fromJson2 = reader.fromJson(json, 
    			MatchDirComponent.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCalculateScore() throws Exception
    {
    	// Make consistent directive structure
    	Directive dA = new Directive("A");
    	Directive dAA = new Directive("AA");
    	Keyword kA = new Keyword("kA", false, "My value");
    	dAA.addKeyword(kA);
    	dA.addSubDirective(dAA);
    	CompChemJob job = new CompChemJob();
    	job.addDirective(dA);
    	
    	DirComponentAddress addrs_a = new DirComponentAddress();
    	addrs_a.addStep("A", DirectiveComponentType.DIRECTIVE);
    	addrs_a.addStep("AA", DirectiveComponentType.DIRECTIVE);
    	addrs_a.addStep("kA", DirectiveComponentType.KEYWORD);
    	MatchDirComponent mdc = new MatchDirComponent(addrs_a, "My value");
    	MatchDirComponent mdc_noVal = new MatchDirComponent(addrs_a);
    	
    	double trsh = 0.000001;
    	
    	// Score from unrelated input is 0.0
    	ArrayList<String> lst = new ArrayList<String>();
    	lst.add("bla 0");
    	lst.add("-1");
    	InfoChannel ic = new ShortTextAsSource(lst);
    	assertTrue(trsh > Math.abs(0.0 - mdc.calculateScore(ic)));
    	assertTrue(trsh > Math.abs(0.0 - mdc_noVal.calculateScore(ic)));
    	
    	// Matches address and value
    	ic = new JobDetailsAsSource(job);
    	assertTrue(trsh > Math.abs(1.0 - mdc.calculateScore(ic)));
    	assertTrue(trsh > Math.abs(1.0 - mdc_noVal.calculateScore(ic)));
    	
    	// Matches address but not value
    	MatchDirComponent mdc_b = new MatchDirComponent(addrs_a, "Other Value");
    	MatchDirComponent mdc_b_noVal = new MatchDirComponent(addrs_a);
    	assertTrue(trsh > Math.abs(0.0 - mdc_b.calculateScore(ic)));
    	assertTrue(trsh > Math.abs(1.0 - mdc_b_noVal.calculateScore(ic)));
    	
    	// Matches address but not value (negated)
    	mdc_b = new MatchDirComponent(addrs_a, "Other Value", true);
    	mdc_b_noVal = new MatchDirComponent(addrs_a, true);
    	assertTrue(trsh > Math.abs(1.0 - mdc_b.calculateScore(ic)));
    	assertTrue(trsh > Math.abs(0.0 - mdc_b_noVal.calculateScore(ic)));
    	
    	// Matches value but not address
    	DirComponentAddress addrs_b = new DirComponentAddress();
    	addrs_b.addStep("A", DirectiveComponentType.DIRECTIVE);
    	addrs_b.addStep("kA", DirectiveComponentType.KEYWORD);
    	MatchDirComponent mdc_c = new MatchDirComponent(addrs_b, "My Value");
    	MatchDirComponent mdc_c_noVal = new MatchDirComponent(addrs_b);
    	assertTrue(trsh > Math.abs(0.0 - mdc_c.calculateScore(ic)));
    	assertTrue(trsh > Math.abs(0.0 - mdc_c_noVal.calculateScore(ic)));
    }
    
//------------------------------------------------------------------------------

}
