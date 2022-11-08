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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.perception.circumstance.Circumstance;
import autocompchem.perception.circumstance.CircumstanceFactory;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.concept.Concept;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.run.Action;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;


/**
 * A situation is a concept in a context. 
 * The context is defined by a satisfied set of circumstances. 
 * A situation, once verified, can trigger a response/impulse.
 *
 * @author Marco Foscato
 */

public class Situation extends Concept
{   
	/**
     * Details describing this situation
     */
    private String description = "no detail";

    /**
     * The context that characterise this situation is defined by a set 
     * of circumstances
     */
     private ArrayList<ICircumstance> context = new ArrayList<ICircumstance>();

    /**
     * Default logical expression for evaluating overall context
     */
    private final String DEFLOGICAL = "none";

    /**
     * Logical expression for evaluating overall context
     */
    private String logicalExpression = DEFLOGICAL;

    /**
     * The impulse or reaction triggered by this situation.
     */
    private Action reaction;

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public Situation()
    {
        super();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type
     * @param conceptType the type of Situation
     */

    public Situation(String conceptType)
    {
        super(conceptType,"noRefName");
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type on situation to construct
     * @param context the context as a list of circumstances
     */

    public Situation(String conceptType, ArrayList<ICircumstance> context)
    {
        super(conceptType,"noRefName");
        this.context = context;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type of situation (i.e., error, warning,...).
     * @param refName the name of the situation (i.e., Error-1.3C).
     * @param context the context as a list of circumstances.
     */

    public Situation(String conceptType, String refName,
    		ArrayList<ICircumstance> context)
    {
        super(conceptType,refName);
        this.context = context;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type of situation (i.e., error, warning,...).
     * @param refName the name of the situation (i.e., Error-1.3C).
     * @param context the context as a list of circumstances.
     * @param reaction the action to be triggered by the perception of this
     * situation.
     */

    public Situation(String conceptType, String refName,
    		ArrayList<ICircumstance> context, Action reaction)
    {
        super(conceptType,refName);
        this.context = context;
        this.reaction = reaction;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a new situation from a file. The file is expected to contain
     * a definition of the Situation object we are constructing. 
     * The format of the definition is detected on the fly by this constructor.
     * @param file the file we read to make this object
     * @throws exception if the file cannot be properly converted or read
     */

    public Situation(File file) throws Exception
    {
        super();
        
    	String format = FileUtils.getFileExtension(file).toLowerCase();
    	
    	// TODO: detect format from file content
    	
		switch (format)
		{
			case SituationConstants.SITUATIONTXTFILEEXT:
				makeFromTxtFile(file);
				break;
				
			default:
				throw new Exception("Unknown format for file '" 
						+ file.getAbsolutePath() + "', which was expected to "
						+ "contain the definition of a known situation.");
		}
	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Creates a situation from a file
     * @param pathname of file to read
     * @return the object
     * @throws exception if the file cannot be properly converted or read
     */
    
    private void makeFromTxtFile(File f) throws Exception
    {
    	    
	    //Read file
	    String fname = f.toString();
	    ArrayList<ArrayList<String>> form = IOtools.readFormattedText(
	    		fname,
	    		SituationConstants.SEPARATOR, //key-value separator
	    		SituationConstants.COMMENTLINE,
	    		SituationConstants.STARTMULTILINE,
	    		SituationConstants.ENDMULTILINE);
	    
	    configure(form,"file "+fname);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Import all configurations from formatted test
     * @param form the formatted text
     */
    
    public void configure(ArrayList<ArrayList<String>> form) throws Exception
    {
    	configure(form,"given form");
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Import all configurations from formatted test
     * @param form the formatted text
     */
    
    private void configure(ArrayList<ArrayList<String>> form, String source) 
    		throws Exception
    {
        boolean refNameFound = false;
        boolean actionFound = false;
        for (int i=0; i<form.size(); i++)
        {
            ArrayList<String> singleBlock = form.get(i);
            String key = singleBlock.get(0);
            String value = singleBlock.get(1);
            value = value.trim();
            switch (key.toUpperCase())
            {
            	case SituationConstants.SITUATIONTYPE:
            		this.setType(value);
            		break;
            		
                case SituationConstants.REFERENCENAMELINE:
                    if (!refNameFound)
                    {
                        refNameFound = true;
                        this.setRefName(value);
                        if (this.getRefName().equals(""))
                        {
                            throw new Exception("Empty '"
                                    + SituationConstants.REFERENCENAMELINE
                                    + "' while defining a Situation from text" 
                                    + " file. Check " + source + ".");
                        }
                    } else {
                        throw new Exception("Multiple '"
                                    + SituationConstants.REFERENCENAMELINE 
                                    + "' while defining a Situation from text"
                                    + " file. Check " + source + ".");
                    }
                    break;
                    
                case SituationConstants.CIRCUMSTANCE:
                	Circumstance circ = CircumstanceFactory.createFromString(
                			value);
                	this.addCircumstance(circ);
                	break;
                	
                case SituationConstants.ACTION:
                    if (!actionFound)
                    {
                        actionFound = true;
                        reaction = new Action(value);
                        
                        if (reaction == null)
                        {
                        	throw new Exception("Coul not read impulse "
                                    + " while defining a Situation from text"
                                    + " file. Check " + source + ".");
                        }
                    } else {
                        throw new Exception("Multiple '"
                                    + SituationConstants.ACTION 
                                    + "' while defining a Situation from text"
                                    + " file. Check " + source + ".");
                    }
                	break;
            } //end of switch
        } //end of loop on array of pairs key:value
        
        
        //Checking requirements
        if (!refNameFound)
        {
        	throw new Exception("No reference name found for situation defined "
        			+ "in " + source);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Append description or additional information as a string.
     * @param txt the description
     */
    public void setDescription(String txt)
    {
        this.description = txt;
    }

//------------------------------------------------------------------------------

    /**
     * Append a circumstance to the context
     * @param circumstance to be added
     */

    public void addCircumstance(ICircumstance circumstance)
    {
        context.add(circumstance);
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of circumstances that identify the context surrounding 
     * the concept of this situation
     * @return the circumstances
     */

    public ArrayList<ICircumstance> getCircumstances()
    {
        return context;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the logical expression for drawing conclusions from the satisfaction
     * of the circumstances.
     */
 
    public void setLogicalExpression(String el)
    {
        this.logicalExpression = el;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this known situation can trigger a reaction.
     * @return <code>true</code> a reaction to this situation is defined.
     */
    public boolean hasReaction()
    {
    	return reaction != null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the expected reaction to this situation, or null if no reaction
     * is known.
     * @return the behaviour in response of the occurrence of this situation
     */
    
    public Action getReaction()
    {
    	return reaction;
    }

//------------------------------------------------------------------------------

    /**
     * Extract all the information channel types involved in the definition of
     * this situation's context
     * @return all relevant information channel types
     */

    public Set<InfoChannelType> getInfoChannelTypes()
    {
        Set<InfoChannelType> allIct = new HashSet<InfoChannelType>();
        for (ICircumstance ic : context)
        {
            allIct.add(ic.getChannelType());
        }
        return allIct;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if all circumstances are satisfied. This method evaluates 
     * the logical expression that puts in relation all the 
     * circumstances and returns the boolean result.
     * @param fingerprint is the list of satisfaction flags one for each 
     * circumstance in the same order of the circumstances.
     * @return the result of the logical expression.
     */

    public boolean isOccurring(ArrayList<Boolean> fingerprint) throws Exception
    {
        boolean res = false;
        if (fingerprint.size() != context.size())
        {
            //TODO move to ERROR
            throw new Exception("ERROR! Number of satisfaction flags ("
                                 + fingerprint.size() + ") "
                                 + "differs from the number of "
                                 + "circumstances (" + context.size() + ")");
        }

        HashMap<String,Object> vars = new HashMap<String,Object>();
        for (int i=0; i<fingerprint.size(); i++)
        {
            vars.put("v" + Integer.toString(i), fingerprint.get(i));
        }
        
        ExpressionFactory expFactory = ExpressionFactory.newInstance();
        ELContext ncc = new ELContext() {
        	VariableMapper vm = new VariableMapper() {
        		@Override
        		public ValueExpression resolveVariable(String varName) 
        		{
        			ValueExpression ve = new ValueExpression() 
        			{
        				Object value;
        				
						/**
						 * Version ID
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public Object getValue(ELContext context) {
							if (vars.containsKey(varName))
							{
								value = vars.get(varName);
							} else {
								throw new ELException("Variable '" + varName
										+ "' cannot be resolved.");
							}
							return value;
						}

						// This should not make sense since this is read-only
						@Override
						public void setValue(ELContext context, Object value) {
							this.value = value;
						}

						@Override
						public boolean isReadOnly(ELContext context) {
							return true;
						}

						@Override
						public Class<?> getType(ELContext context) {
							return value.getClass();
						}

						@Override
						public Class<?> getExpectedType() {
							return Boolean.class;
						}

						@Override
						public String getExpressionString() {
							return null;
						}

						@Override
						public boolean equals(Object obj) {
							return false;
						}

						@Override
						public int hashCode() {
							//Dummy hashcode
							return 0;
						}

						@Override
						public boolean isLiteralText() {
							return false;
						}
        			};
        			return ve;
        		}

        		// Read-only
				@Override
				public ValueExpression setVariable(String variable, 
						ValueExpression expression) 
				{
					return null;
				}
        	};
       
			@Override
			public ELResolver getELResolver() {
				//None
				return null;
			}

			@Override
			public FunctionMapper getFunctionMapper() {
				// None
				return null;
			}

			@Override
			public VariableMapper getVariableMapper() {
				return vm;
			}
		};
		
		if (logicalExpression.equals(DEFLOGICAL))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("${");
            for (int i=0; i<fingerprint.size(); i++)
            {
                sb.append("v" + i );
                if (i<(fingerprint.size()-1))
                {
                    sb.append(" && ");
                }
            }
            sb.append("}");
            logicalExpression = sb.toString();
        }
		
		try
		{
			ValueExpression ve = expFactory.createValueExpression(ncc, 
					logicalExpression, Boolean.class);
			Object value = ve.getValue(ncc);

            if (value != null) 
            {
                String[] words = ((String) value).split("\\s+");
                if (words.length > 1)
                {
                    throw new Exception("Evaluation of Expression "
                            + "'" + logicalExpression + "' "
                            + "returned more than one word (i.e., '" 
                            + value + "'). Check expression.");
                }
                else
                {
                    switch (words[0])
                    {
                        case ("true"):
                            res = true;
                            break;
                        case ("false"):
                            res = false;
                            break;
                        default:
                            throw new Exception("Evaluation of Expression "
                            + "'" + logicalExpression 
                            + "' did not return a Boolean (i.e., '"
                            + value + "'). Check expression.");
                    }
                }
            }
            else
            {
            	//TODO error?
                throw new Exception("Evaluation of Expression Language "
                		+ "returned null instead of boolean. "
                		+ "Check expression.");
            }
        }
        catch (Throwable t)
        {
            //TODO error or warning?
            throw new Exception("ERROR in Expression Language! " 
                    + "Expression '" + logicalExpression + "' triggers " 
                    + t);
        }

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation of this Situation
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Situation [").append(super.toString());
        sb.append("; Context [");
        boolean first = true;
        for (ICircumstance c : context)
        {
            if (first)
            {
                sb.append(c.toString());
                first = false;
            }
            else
            {
                sb.append("; ").append(c.toString());
            }
        }
        sb.append("]; Impusle [");
        if (reaction != null)
        {
        	sb.append(reaction.toString());
        } else
        {
        	sb.append("none");
        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
