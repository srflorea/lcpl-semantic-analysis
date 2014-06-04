import java.util.*;

import ro.pub.cs.lcpl.*;

/*
 * Class which where are created the special types of classes: Object, IO, String, where are made the setups for the classes
 * and for every class uses LCPLFutureWalker objects to set up the attributes and the methods
 */
public class LCPLSemanticAnalyzer {
	
	private Program p;
	
	/*
	 * Saves the attributes from all the classes.
	 * Map<className, Map<attributeName, AttributeObject>>
	 */
	private Map<String, Map<String, Variable>> attributeSymbols;
	/*
	 * Saves all the classes.
	 * Map<className, classObject>
	 */
	private Map<String, LCPLClass> classes;
	
	public LCPLSemanticAnalyzer(Program p) {
		this.p 			 = p;
		attributeSymbols = new LinkedHashMap<String, Map<String, Variable>>();
		classes		 	 = new LinkedHashMap<String, LCPLClass>();
	}
	
	/*
	 * Method where the semantic analysis begins.
	 */
	public void startSemanticAnalysis() throws LCPLException {
		setAllObjects();
		
		verifyClassesNames();
		/* verify if the program contains the Main class */
		if(!classes.containsKey(LCPLConstants.CLASSMAIN)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.classNotFoundMessage(LCPLConstants.CLASSMAIN);
			throw new LCPLException(message, p);
		}
		
		List<LCPLClass> classesList = p.getClasses();
		classesList.add(p.getObjectType());
		classesList.add(p.getIoType());
		classesList.add(p.getStringType());
			
		walkThroughClasses();
		
		/* verify if the main class contains a main method */
		LCPLClass mainClass = classes.get(LCPLConstants.CLASSMAIN);
			
		if(!containsMainClass(mainClass)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.methodNotFoundMessage(LCPLConstants.METHODMAIN, LCPLConstants.CLASSMAIN);
			throw new LCPLException(message, mainClass);
		}
	}
	
	/*
	 * Method that verifies if a class is already defined.
	 */
	private void verifyClassesNames() throws LCPLException {
		for(LCPLClass lcplClass : p.getClasses()) {
			if(classes.containsKey(lcplClass.getName())) {
				LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
				String message = eb.classAlreadyExistsMessage(lcplClass.getName());
				throw new LCPLException(message, lcplClass);
			}
			classes.put(lcplClass.getName(), lcplClass);
		}
	}
	
	/*
	 * Method that iterates through the classes list and sets them up.
	 */
	private void walkThroughClasses() throws LCPLException {
		/* first set class properties, attributes and methods signatures */
		for(LCPLClass lcplClass : p.getClasses()) {
			String name = lcplClass.getName();
			if(name.equals(p.getIoType().getName()) 
					|| name.equals(p.getObjectType().getName()) 
					|| name.equals(p.getStringType().getName())) {
				continue;
			}
			
			setClassProperties(lcplClass);
			
			LCPLFuturesWalker fw = new LCPLFuturesWalker(p, lcplClass, attributeSymbols);
			fw.makeSuperficialWalking();
		}
		
		/* verify parent loop */
		for(LCPLClass lcplClass : p.getClasses()) {
			if(parentLoop(lcplClass, lcplClass.getParentData())) {
				LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
				String message = eb.classRecursivelyInheritsItself(lcplClass.getName());
				throw new LCPLException(message, lcplClass);
			}
		}
		
		/* then set the methods */
		for(LCPLClass lcplClass : p.getClasses()) {
			String name = lcplClass.getName();
			if(name.equals(p.getIoType().getName()) 
					|| name.equals(p.getObjectType().getName()) 
					|| name.equals(p.getStringType().getName())) {
				continue;
			}
			LCPLFuturesWalker fw = new LCPLFuturesWalker(p, lcplClass, attributeSymbols);
			fw.walkthroughFutures();
		}
		
	}
	
	/*
	 * Method that sets up the properties for a class.
	 */
	private void setClassProperties(LCPLClass lcplClass) throws LCPLException {
		
		Map<String, Variable> classAttributes = new LinkedHashMap<String, Variable>();
		attributeSymbols.put(lcplClass.getName(), classAttributes);

		LCPLClass parentClass = null;
		/* if doesn't inherit any class, than its parent will be Object */
		if(lcplClass.getParent() == null) {
			parentClass = p.getObjectType();
		}
		else if(lcplClass.getParent().equals(p.getIoType().getName())) {
			parentClass = p.getIoType();
		}
		/* else, search for the class */
		else {
			for(LCPLClass possibleParent : p.getClasses()) {
				if(lcplClass.getParent().equals(possibleParent.getName())) {
					parentClass = possibleParent;
					break;
				}
			}
		}
		
		/* if the parent class doesn't exist */
		if(parentClass == null) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.classNotFoundMessage(lcplClass.getParent());
			throw new LCPLException(message, lcplClass);
		}
		
		if(parentClass.getName().equals(p.getStringType().getName())) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.aClassCanNotInheritAString();
			throw new LCPLException(message, lcplClass);
		}
		
		lcplClass.setParent(parentClass.getName());
		lcplClass.setParentData(parentClass);
	}
	
	/*
	 * Method that sets up the special classes from lcpl language.
	 */
	private void setAllObjects() {
		p.setIntType(new IntType());
		p.setNoType(new NoType());
		p.setNullType(new NullType());
		
		LCPLClass object = new LCPLClass(LCPLConstants.ZERO, LCPLConstants.OBJECT, null, new ArrayList<Feature>());
		p.setObjectType(object);
		
		LCPLClass string = new LCPLClass(LCPLConstants.ZERO, LCPLConstants.STRING, p.getObjectType().getName(), new ArrayList<Feature>());
		string.setParentData(p.getObjectType());
		p.setStringType(string);
		
		LCPLClass io = new LCPLClass(LCPLConstants.ZERO, LCPLConstants.IO, p.getObjectType().getName(), new ArrayList<Feature>());
		io.setParentData(p.getObjectType());
		p.setIoType(io);
		
		setStringClassFeatures();
		setIOClassFeatures();
		setObjectClassFeatures();
	}
	
	/*
	 * Method that sets up the Object class futures.
	 */
	private void setObjectClassFeatures() {		
		FormalParam self; 
		
		/* abort */
		Feature abort = new Method(LCPLConstants.ZERO, LCPLConstants.ABORT, new ArrayList<FormalParam>(), LCPLConstants.VOID, null);
		((Method)abort).setParent(p.getObjectType());
		((Method)abort).setReturnTypeData(p.getNoType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getObjectType().getName());
		self.setVariableType(p.getObjectType());
		((Method)abort).setSelf(self);
		
		p.getObjectType().getFeatures().add(abort);
		
		/* typeName */
		Feature typeName = new Method(LCPLConstants.ZERO, LCPLConstants.TYPENAME, new ArrayList<FormalParam>(), LCPLConstants.STRING, null);
		((Method)typeName).setParent(p.getObjectType());
		((Method)typeName).setReturnTypeData(p.getStringType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getObjectType().getName());
		self.setVariableType(p.getObjectType());
		((Method)typeName).setSelf(self);
		
		p.getObjectType().getFeatures().add(typeName);
		
		/* copy */
		Feature copy = new Method(LCPLConstants.ZERO, LCPLConstants.COPY, new ArrayList<FormalParam>(), LCPLConstants.OBJECT, null);
		((Method)copy).setParent(p.getObjectType());
		((Method)copy).setReturnTypeData(p.getObjectType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getObjectType().getName());
		self.setVariableType(p.getObjectType());
		((Method)copy).setSelf(self);

		p.getObjectType().getFeatures().add(copy);
	}
	/*
	 * Method that sets up the String class futures.
	 */
	private void setStringClassFeatures() {
		FormalParam self;
		
		/* length */ 
		Feature length = new Method(LCPLConstants.ZERO, LCPLConstants.LENGTH, new ArrayList<FormalParam>(), p.getIntType().getName(), null);
		((Method)length).setParent(p.getStringType());
		((Method)length).setReturnTypeData(p.getIntType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getStringType().getName());
		self.setVariableType(p.getStringType());
		((Method)length).setSelf(self);
		
		p.getStringType().getFeatures().add(length);

		/* toInt */
		Feature toInt = new Method(LCPLConstants.ZERO, LCPLConstants.TOINT, new ArrayList<FormalParam>(), p.getIntType().getName(), null);
		((Method)toInt).setParent(p.getStringType());
		((Method)toInt).setReturnTypeData(p.getIntType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getStringType().getName());
		self.setVariableType(p.getStringType());
		((Method)toInt).setSelf(self);
		
		p.getStringType().getFeatures().add(toInt);
	}
	
	/*
	 *  Methods that set up the IO class futures 
	 */
	private void setIOClassFeatures() {
		FormalParam self;
		
		/* out */
		FormalParam outParam = new FormalParam(LCPLConstants.MSG, p.getStringType().getName());
		outParam.setVariableType(p.getStringType());
		List<FormalParam> outParams = new ArrayList<FormalParam>();
		outParams.add(outParam);
		
		Feature out = new Method(LCPLConstants.ZERO, LCPLConstants.OUT, outParams, p.getIoType().getName(), null);
		((Method)out).setParent(p.getIoType());
		((Method)out).setReturnTypeData(p.getIoType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getIoType().getName());
		self.setVariableType(p.getIoType());
		((Method)out).setSelf(self);
		
		p.getIoType().getFeatures().add(out);
		
		/* in */	
		Feature in = new Method(LCPLConstants.ZERO, LCPLConstants.IN, new ArrayList<FormalParam>(), p.getStringType().getName(), null);
		((Method)in).setParent(p.getIoType());
		((Method)in).setReturnTypeData(p.getStringType());
		
		self = new FormalParam(LCPLConstants.SELF, p.getIoType().getName());
		self.setVariableType(p.getIoType());
		((Method)in).setSelf(self);
	
		p.getIoType().getFeatures().add(in);
	}
	
	/*
	 *  Method that verifies if a Main class contains the main method. 
	 */
	private boolean containsMainClass(LCPLClass lcplClass) {
		if(lcplClass.getName().equals(LCPLConstants.OBJECT)) {
			return false;
		}
		for(Feature feature : lcplClass.getFeatures()) {
			if(feature instanceof Method) {
				if(((Method)feature).getName().equals(LCPLConstants.METHODMAIN)) {
					return true;
				}
			}
		}
		
		return containsMainClass(lcplClass.getParentData());
	}
	/*
	 * Method that verifies of a class inherits itself through a inheriting loop.
	 */
	private boolean parentLoop(LCPLClass lcplClass, LCPLClass parentClass) {
		if(parentClass == null) {
			return false;
		}
		
		if(lcplClass.getName().equals(parentClass.getName())) {
			return true;
		}
		
		return parentLoop(lcplClass, parentClass.getParentData());
	}
	
}
