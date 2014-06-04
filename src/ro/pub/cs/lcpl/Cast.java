package ro.pub.cs.lcpl;

/** An explicit cast
 * { <i>type</i> <i>expression</i> }
 */
public class Cast extends Expression {
	private String type;
	private Expression e1;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Expression getE1() {
		return e1;
	}
	public void setE1(Expression e1) {
		this.e1 = e1;
	}
	public Cast(int lineNumber, String type, Expression e1) {
		super(lineNumber);
		this.type = type;
		this.e1 = e1;
	}
	public Cast() {}
	

}
