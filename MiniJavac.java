import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.*;

class MiniJavac {
  public static void main (String [] args){
    if(args.length < 1){
      System.err.println("Usage: java MiniJavac [file1] [file2] ... [fileN]");
      System.exit(1);
    }

    FileInputStream fis = null;

    for(int i = 0; i < args.length; i++){
      try{
        fis = new FileInputStream(args[i]);
        System.err.println("Checking file " + args[i]);
        Goal root = new MiniJavaParser(fis).Goal();
        // System.err.println("Program parsed successfully.");

        //Create Symbol Table
        LinkedHashMap<String,ClassInfo> symbolTable = new LinkedHashMap<String,ClassInfo>();

        //Send visitor 1 to fill the symbol table and do very basic checks
        Visitor1 v1 = new Visitor1(symbolTable);
        root.accept(v1);

        //Send vistor 2 to do the rest of the checks
        Visitor2 v2 = new Visitor2(symbolTable);
        root.accept(v2);

        //Print offsets
        printOffsets(symbolTable);

        // Send LLVMVisitor to produce IR code
        LLVMVisitor llvm = new LLVMVisitor(symbolTable, "test.ll");
        root.accept(llvm, null);

      }
      catch(ParseException ex){
        System.out.println(ex.getMessage());
      }
      catch(FileNotFoundException ex){
        System.err.println(ex.getMessage());
      }
      catch(Exception ex){
        System.out.println("Semantic Error: " + ex.getMessage());
        // System.out.println(ex.getClass().getName());
        // ex.printStackTrace();
      }
      finally{
        try{
          if(fis != null) fis.close();
        }
        catch(IOException ex){
          System.err.println(ex.getMessage());
        }
      }
    }
  }

  public static void printOffsets(LinkedHashMap<String,ClassInfo> symbolTable){
    Set classes = symbolTable.keySet();
    Iterator iter = classes.iterator();

    while(iter.hasNext()){
      String name = (String) iter.next();
      ClassInfo curClassInfo = symbolTable.get(name);

      curClassInfo.printOffsets();
    }
  }
}
