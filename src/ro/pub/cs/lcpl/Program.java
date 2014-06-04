package ro.pub.cs.lcpl;
import java.util.List;

/** A complete LCPL program
 * <i>class</i> ... 
 */
public class Program extends TreeNode {
	/** List of all classes in the program.
	 * Add the built-in classes below to this list: 
	 * Program.objectType, Program.stringType, Program.ioType. */
	private List<LCPLClass> classes;
	
	/** Create a new IntType object representing the type of all Int expressions in the program. */
	private IntType intType;
	
	/** Create a new NoType object representing the type of all expressions in the program that do not return a value, such as 'while'. */
	private NoType noType;

	/** Create a new NullType object representing the type of all expressions in the program that are evaluated to the constant 'void'. */
	private NullType nullType;
	
	/** Create a new LCPLClass object representing the Object class */
	private LCPLClass objectType;

	/** Create a new LCPLClass object representing the String class */
	private LCPLClass stringType;
	
	/** Create a new LCPLClass object representing the IO class */
	private LCPLClass ioType;

	public List<LCPLClass> getClasses() {
		return classes;
	}

	public void setClasses(List<LCPLClass> classes) {
		this.classes = classes;
	}

	public IntType getIntType() {
		return intType;
	}

	public void setIntType(IntType intType) {
		this.intType = intType;
	}

	public NoType getNoType() {
		return noType;
	}

	public void setNoType(NoType noType) {
		this.noType = noType;
	}

	public NullType getNullType() {
		return nullType;
	}

	public void setNullType(NullType nullType) {
		this.nullType = nullType;
	}

	public LCPLClass getObjectType() {
		return objectType;
	}

	public void setObjectType(LCPLClass objectType) {
		this.objectType = objectType;
	}

	public LCPLClass getStringType() {
		return stringType;
	}

	public void setStringType(LCPLClass stringType) {
		this.stringType = stringType;
	}

	public LCPLClass getIoType() {
		return ioType;
	}

	public void setIoType(LCPLClass ioType) {
		this.ioType = ioType;
	}

	public Program(int lineNumber, List<LCPLClass> classes) {
		super(lineNumber);
		this.classes = classes;
	}
	public Program () {}
	


}
