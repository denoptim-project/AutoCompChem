package autocompchem.utils;

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

import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.VariableMapper;
import jakarta.el.ValueExpression;

/**
 * Singleton class providing centralized access to the Expression Language
 * factory for evaluating expressions throughout AutoCompChem.
 * 
 * @author Marco Foscato
 */

public final class ACCExpressionLanguage
{
	/**
	 * Singleton instance of this class
	 */
	private static ACCExpressionLanguage INSTANCE;
	
	/**
	 * The ExpressionFactory instance used for creating and evaluating
	 * expressions.
	 */
	private ExpressionFactory expressionFactory;

    /**
     * Function mapper that combined functions from Math with decimal formatting 
     * functions.
     */
    private static FunctionMapper functionMapper;
    static {
        functionMapper = new FunctionMapper() {
            @Override
            public Method resolveFunction(String prefix, String localName) {
                try {
                    // Decimal formatting
                    if ("format".equals(localName)) {
                        try {
                            return NumberUtils.class.getMethod("formatNumber", 
                                String.class, Double.class);
                        } catch (NoSuchMethodException e) {
                            return null;
                        }
                    }
                    // Trigonometric functions
                    else if ("sin".equals(localName)) {
                        return Math.class.getMethod("sin", double.class);
                    } else if ("cos".equals(localName)) {
                        return Math.class.getMethod("cos", double.class);
                    } else if ("tan".equals(localName)) {
                        return Math.class.getMethod("tan", double.class);
                    } else if ("asin".equals(localName)) {
                        return Math.class.getMethod("asin", double.class);
                    } else if ("acos".equals(localName)) {
                        return Math.class.getMethod("acos", double.class);
                    } else if ("atan".equals(localName)) {
                        return Math.class.getMethod("atan", double.class);
                    } else if ("atan2".equals(localName)) {
                        return Math.class.getMethod("atan2", double.class, double.class);
                    }
                    // Exponential and logarithmic functions
                    else if ("exp".equals(localName)) {
                        return Math.class.getMethod("exp", double.class);
                    } else if ("log".equals(localName) || "ln".equals(localName)) {
                        return Math.class.getMethod("log", double.class);
                    } else if ("log10".equals(localName)) {
                        return Math.class.getMethod("log10", double.class);
                    }
                    // Power functions
                    else if ("sqrt".equals(localName)) {
                        return Math.class.getMethod("sqrt", double.class);
                    } else if ("pow".equals(localName)) {
                        return Math.class.getMethod("pow", double.class, double.class);
                    }
                    // Hyperbolic functions
                    else if ("sinh".equals(localName)) {
                        return Math.class.getMethod("sinh", double.class);
                    } else if ("cosh".equals(localName)) {
                        return Math.class.getMethod("cosh", double.class);
                    } else if ("tanh".equals(localName)) {
                        return Math.class.getMethod("tanh", double.class);
                    }
                    // Rounding and absolute value
                    else if ("abs".equals(localName)) {
                        return Math.class.getMethod("abs", double.class);
                    } else if ("ceil".equals(localName)) {
                        return Math.class.getMethod("ceil", double.class);
                    } else if ("floor".equals(localName)) {
                        return Math.class.getMethod("floor", double.class);
                    } else if ("round".equals(localName)) {
                        return Math.class.getMethod("round", double.class);
                    }
                    // Min/Max functions
                    else if ("max".equals(localName)) {
                        return Math.class.getMethod("max", double.class, double.class);
                    } else if ("min".equals(localName)) {
                        return Math.class.getMethod("min", double.class, double.class);
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    // If method not found or access denied, return null
                }
                return null;
            }
        };
    }

    /**
     * Variable mapper for no-variable scenarios. Any variable resolution will return null.
     */
    private static VariableMapper variableLessVariableMapper;
    static {
        variableLessVariableMapper = new VariableMapper() {
            @Override
            public ValueExpression resolveVariable(String varName) 
            {
                ValueExpression ve = new ValueExpression() 
                {   
                    /**
                     * Version ID
                     */
                    private static final long serialVersionUID = 1L;

                    @SuppressWarnings("unchecked")
                    @Override
                    public Object getValue(ELContext c) {
                        return null;
                    }

                    @Override
                    public void setValue(ELContext c, Object v) {
                    }

                    @Override
                    public boolean isReadOnly(ELContext c) {
                        return true;
                    }

                    @Override
                    public Class<?> getType(ELContext c) {
                        return Double.class;
                    }

                    @Override
                    public Class<?> getExpectedType() {
                        return Double.class;
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

            @Override
            public ValueExpression setVariable(String variable, 
                    ValueExpression expression) 
            {
                return null;
            }
        };
    }

    /**
     * ELResolver for no-base scenarios. Any base resolution will return null.
     */
    private static ELResolver baseLessELResolver;
    static {
        baseLessELResolver = new ELResolver() {
            @Override
            public Object getValue(ELContext context, Object base, Object property) {
                // Not used for function resolution
                return null;
            }

            @Override
            public Class<?> getType(ELContext context, Object base, Object property) {
                // Not used for function resolution
                return null;
            }

            @Override
            public void setValue(ELContext context, Object base, Object property, Object value) {
                // Read-only
            }

            @Override
            public boolean isReadOnly(ELContext context, Object base, Object property) {
                return true;
            }

            @Override
            public Class<?> getCommonPropertyType(ELContext context, Object base) {
                return null;
            }
        };
    }

    /**
     * Singleton context for no-base and no-variable scenarios.
     */
    private static ELContext variableLessELContext;
    static {
        variableLessELContext = new ELContext() {
            @Override
            public ELResolver getELResolver() {
                return baseLessELResolver;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return functionMapper;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return variableLessVariableMapper;
            }
        };
    }

//------------------------------------------------------------------------------

	/**
	 * Private constructor to prevent instantiation from outside this class.
	 * Initializes the ExpressionFactory.
	 */
	private ACCExpressionLanguage()
	{
		expressionFactory = ExpressionFactory.newInstance();
	}

//------------------------------------------------------------------------------

	/**
	 * Returns the singleton instance of this class.
	 * @return the singleton instance.
	 */
	public synchronized static ACCExpressionLanguage getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new ACCExpressionLanguage();
		return INSTANCE;
	}

//------------------------------------------------------------------------------

	/**
	 * Returns the ExpressionFactory instance.
	 * @return the ExpressionFactory instance.
	 */
	public ExpressionFactory getExpressionFactory()
	{
		return expressionFactory;
	}

//------------------------------------------------------------------------------

    public VariableMapper getVariableLessVariableMapper()
    {
        return variableLessVariableMapper;
    }

//------------------------------------------------------------------------------

    public FunctionMapper getFunctionMapper()
    {
        return functionMapper;
    }

//------------------------------------------------------------------------------

    public ELContext getVariableLessELContext()
    {
        return variableLessELContext;
    }

//------------------------------------------------------------------------------

    public Object processVariableLessExpression(String expr, Class<?> expectedType)
    {
        return expressionFactory.createValueExpression(
            variableLessELContext, expr, expectedType).getValue(
                variableLessELContext);
    }

//------------------------------------------------------------------------------

    /**
     * Processes an expression with variables provided via a map.
     * @param expr the expression string to evaluate
     * @param expectedType the expected return type of the expression
     * @param variables a map of variable names to their values
     * @return the result of evaluating the expression with the provided variables
     */
    public Object processExpressionWithVariables(String expr, Class<?> expectedType,
            Map<String, Object> variables)
    {
        // Create a VariableMapper that resolves variables from the provided map
        VariableMapper variableMapper = new VariableMapper() {
            @Override
            public ValueExpression resolveVariable(String varName) 
            {
                // Check if the variable exists in the map
                if (variables != null && variables.containsKey(varName))
                {
                    Object value = variables.get(varName);
                    ValueExpression ve = new ValueExpression() 
                    {   
                        /**
                         * Version ID
                         */
                        private static final long serialVersionUID = 1L;

                        @SuppressWarnings("unchecked")
                        @Override
                        public Object getValue(ELContext c) {
                            return value;
                        }

                        @Override
                        public void setValue(ELContext c, Object v) {
                            // Read-only
                        }

                        @Override
                        public boolean isReadOnly(ELContext c) {
                            return true;
                        }

                        @Override
                        public Class<?> getType(ELContext c) {
                            return value != null ? value.getClass() : Object.class;
                        }

                        @Override
                        public Class<?> getExpectedType() {
                            return value != null ? value.getClass() : Object.class;
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
                            return value != null ? value.hashCode() : 0;
                        }

                        @Override
                        public boolean isLiteralText() {
                            return false;
                        }
                    };
                    return ve;
                }
                // Variable not found in map, return null
                return null;
            }

            @Override
            public ValueExpression setVariable(String variable, 
                    ValueExpression expression) 
            {
                // Read-only, do not allow setting variables
                return null;
            }
        };

        // Create an ELContext with the variable mapper and function mapper
        ELContext context = new ELContext() {
            @Override
            public ELResolver getELResolver() {
                return baseLessELResolver;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return functionMapper;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return variableMapper;
            }
        };

        // Create and evaluate the expression
        return expressionFactory.createValueExpression(
            context, expr, expectedType).getValue(context);
    }

//------------------------------------------------------------------------------
}
