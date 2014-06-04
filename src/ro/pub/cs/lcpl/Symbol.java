package ro.pub.cs.lcpl;

/** A symbol - local variable or class attribute */
public class Symbol extends Expression {
	/** The name of the symbol being evaluated.
	 * Take into account the syntax "self.sym" to specify explicitly class attributes.
	 */
	private String name;
	
	/** Reference to the variable corresponding to the symbol.
	 * The variable can be:
	 *   <li> an Attribute of the current class
	 *   <li> a FormalParam of the current method. It can be any formal parameter, except for self. 
	 *   <li> a LocalDefinition. The Assignment must be inside the scope of the LocalDefinition.
	 *  */
	private Variable variable;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}

	public Symbol(int lineNumber, String name) {
		super(lineNumber);
		this.name = name;
	}
	public Symbol() {}

	
}
