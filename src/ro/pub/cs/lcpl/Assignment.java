package ro.pub.cs.lcpl;


/** Expression of type <i>symbol</i> = <i>e1</i> */
public class Assignment extends Expression {
	/** Left hand side of the assignment.
	 * Take into account the syntax "self.sym = ..." to specify explicitly assignments to attributes.
	 */
	private String symbol;
	private Expression e1;
	
	/** Reference to the variable corresponding to the symbol on the left side of the assignment.
	 * The variable can be:
	 *   <li> an Attribute of the current class
	 *   <li> a FormalParam of the current method. It can be any formal parameter, except for self. 
	 *   <li> a LocalDefinition. The Assignment must be inside the scope of the LocalDefinition.
	 *  */
	private Variable symbolData;
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Expression getE1() {
		return e1;
	}
	public void setE1(Expression e1) {
		this.e1 = e1;
	}
	public Variable getSymbolData() {
		return symbolData;
	}
	public void setSymbolData(Variable symbolData) {
		this.symbolData = symbolData;
	}
	public Assignment(int lineNumber, String symbol, 
			Expression e1) {
		super(lineNumber);
		this.symbol = symbol;
		this.e1 = e1;
	}
	public Assignment() {}
	

}
