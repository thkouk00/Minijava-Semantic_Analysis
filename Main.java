import java.io.*;
import java.util.*;
import visitor.*;
import syntaxtree.*;

class Main {
    public static void main (String [] args){
    	if(args.length < 1){
		    System.err.println("Usage: java [MainClassName] [file1] [file2] ... [fileN]");
		    System.exit(1);
		}
		int count = 0;
		FileInputStream fis = null;
		while(count < args.length){
			try{
			    fis = new FileInputStream(args[count]);
			    MiniJavaParser parser = new MiniJavaParser(fis);
			    System.err.println("Program parsed successfully.");
			    fillSTVisitor stVisitor = new fillSTVisitor();
			    Goal root = parser.Goal();
			    root.accept(stVisitor);

			    TCVisitor checkVisitor = new TCVisitor(stVisitor.symtable);
			    root.accept(checkVisitor, null);
			    System.err.println("Program typechecked successfully.");

			    System.out.println("**** "+args[count]+" ****\n");
			    stVisitor.symtable.printOffsets();
		
			}
			catch(ParseException ex){
			    System.out.println(ex.getMessage());
			}
			catch(FileNotFoundException ex){
			    System.err.println(ex.getMessage());
			}
			catch (Exception e) {
				System.err.println("\n****************************************");
				System.err.println("ERROR IN TYPE CHECKING -> INPUT FILE "+(count+1)+" : "+args[count]);
				System.err.println(e.getMessage());
				System.err.println("****************************************\n");
			}
			finally{
			    try{
					if(fis != null) fis.close();
			    }
			    catch(IOException ex){
				System.err.println(ex.getMessage());
			    }
			}
			count++;
		}
    }
}
