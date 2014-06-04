package ro.pub.cs.lcpl;


/** new <i>type</i> */
public class NewObject extends Expression {
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public NewObject(int lineNumber, String type) {
		super(lineNumber);
		this.type = type;
	}
	public NewObject() {}

	

}
