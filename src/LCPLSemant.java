import java.io.*;

import ro.pub.cs.lcpl.*;

import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.Constructor;

/*
 * Class which represents the entry point in the semantic analysis
 */
public class LCPLSemant {

	public static void main(String[] args) {
		if (args.length != 2)
		{
			System.err.println("Usage: LCPLSemant <filein.yaml> <fileout.yaml>\n");
			System.exit(1);
		}
		try {
			Yaml yaml = new Yaml(new Constructor(Program.class));
			FileInputStream fis = new FileInputStream(args[0]);
			Program p = (Program) yaml.load(fis);
			fis.close();

			/* begin the semantic analysis */
			LCPLSemanticAnalyzer sa = new LCPLSemanticAnalyzer(p);
			try {
				sa.startSemanticAnalysis();
			} catch (LCPLException ex) {
				System.out.println("Error in line " + ex.node.getLineNumber() + " : " + ex.message);
			} 
			Yaml yamlOut = new Yaml();
			PrintStream fos = new PrintStream(new FileOutputStream(args[1]));
			fos.println(yamlOut.dump(p));
			fos.close();
		} catch (IOException ex) {
			System.err.println("File error: " + ex.getMessage());
			System.err.println("===================================================");
		} 
		
	}

	

}
