package ro.pub.cs.lcpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** Common code for Dispatch and StaticDispatch */
public class BaseDispatch extends Expression {
	private Expression object;
	private String name;
	private List<Expression> arguments;
	
	/** A reference to the method invoked by the dispatch expression.
	 * 
	 * In case of dynamic dispatch, use the type of the <i>object</i> expression to 
	 * identify which method the dispatch refers to.
	 * 
	 * The actual method invoked at runtime could be different due to polymorphism, 
	 * because a derived class can override methods of the base class. 
	 *  */
	private Method method;
	
	public Expression getObject() {
		return object;
	}
	public void setObject(Expression object) {
		this.object = object;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public List<Expression> getArguments() {
		return arguments;
	}
	public void setArguments(List<Expression> arguments) {
		this.arguments = arguments;
	}
	public BaseDispatch() {}
	
}
