import visitor.GJNoArguDepthFirst;
import syntaxtree.*;
import java.util.*;

public class Visitor1 extends GJNoArguDepthFirst<String> {
  private LinkedHashMap<String, ClassInfo> symbolTable;
  private String curClass;
  private String curMethod;
  private MethodInfo curMethodInfo;

  public Visitor1(LinkedHashMap<String, ClassInfo> symbolTable){
    this.symbolTable = symbolTable;
    this.curClass = null;
    this.curMethod = null;
    this.curMethodInfo = null;
  }

  //
  // User-generated visitor methods below
  //

  /**
  * f0 -> MainClass()
  * f1 -> ( TypeDeclaration() )*
  * f2 -> <EOF>
  */
  public String visit(Goal n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> "class"
  * f1 -> Identifier()
  * f2 -> "{"
  * f3 -> "public"
  * f4 -> "static"
  * f5 -> "void"
  * f6 -> "main"
  * f7 -> "("
  * f8 -> "String"
  * f9 -> "["
  * f10 -> "]"
  * f11 -> Identifier()
  * f12 -> ")"
  * f13 -> "{"
  * f14 -> ( VarDeclaration() )*
  * f15 -> ( Statement() )*
  * f16 -> "}"
  * f17 -> "}"
  */
  public String visit(MainClass n) throws Exception {
    String _ret=null;

    String className = new String(n.f1.accept(this));
    String argsName = new String(n.f11.accept(this));

    if(symbolTable.containsKey(className)){
      throw new Exception("Class " + className + " is defined multiple times");
    }

    //Add class to symbol table
    ClassInfo curClassInfo = new ClassInfo(className);
    symbolTable.put(className, curClassInfo);

    //Add main method to class
    MethodInfo mainMethod = new MethodInfo("main", "void", curClassInfo);
    mainMethod.addParameter("String[]");
    curClassInfo.addMethod(mainMethod);


    return _ret;
  }

  /**
  * f0 -> ClassDeclaration()
  *       | ClassExtendsDeclaration()
  */
  public String visit(TypeDeclaration n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "class"
  * f1 -> Identifier()
  * f2 -> "{"
  * f3 -> ( VarDeclaration() )*
  * f4 -> ( MethodDeclaration() )*
  * f5 -> "}"
  */
  public String visit(ClassDeclaration n) throws Exception {
    String _ret=null;

    String name = new String(n.f1.accept(this));

    if(symbolTable.containsKey(name)){
      throw new Exception("Class " + name + " is defined multiple times");
    }

    //Add class to symbol table
    ClassInfo curClassInfo = new ClassInfo(name);
    symbolTable.put(name, curClassInfo);

    //This info will be used to add vars and methods to the class
    curClass = name;
    curMethod = null;

    n.f3.accept(this); //Variables declaration
    n.f4.accept(this); //Methods declaration
    return _ret;
  }

  /**
  * f0 -> "class"
  * f1 -> Identifier()
  * f2 -> "extends"
  * f3 -> Identifier()
  * f4 -> "{"
  * f5 -> ( VarDeclaration() )*
  * f6 -> ( MethodDeclaration() )*
  * f7 -> "}"
  */
  public String visit(ClassExtendsDeclaration n) throws Exception {
    String _ret=null;

    String name = new String(n.f1.accept(this));
    String parentName = new String(n.f3.accept(this));

    if(symbolTable.containsKey(name)){
      throw new Exception("Class " + name + " is defined multiple times");
    }

    if(!symbolTable.containsKey(parentName)){
      throw new Exception("Class " + name + " is defined before the class it extends");
    }

    //Add class to symbol table
    ClassInfo curClassInfo = new ClassInfo(name, symbolTable.get(parentName));
    symbolTable.put(name, curClassInfo);

    curClass = name;
    curMethod = null;

    n.f5.accept(this); //Var Declaration
    n.f6.accept(this); //Method Declaration

    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  * f2 -> ";"
  */
  public String visit(VarDeclaration n) throws Exception {
    String _ret=null;

    String type = new String(n.f0.accept(this));
    String name = new String(n.f1.accept(this));

    ClassInfo curClassInfo = symbolTable.get(curClass);

    if(curMethod == null){
      //The variable being declared is a field of the class
      if(curClassInfo.hasVarDef(name)){
        throw new Exception("Class " + curClass + " has more than one variables named " + name);
      }

      curClassInfo.addVar(name, type);
    }
    else{
      //The variable being declared belongs to a method
    }

    return _ret;
  }

  /**
  * f0 -> "public"
  * f1 -> Type()
  * f2 -> Identifier()
  * f3 -> "("
  * f4 -> ( FormalParameterList() )?
  * f5 -> ")"
  * f6 -> "{"
  * f7 -> ( VarDeclaration() )*
  * f8 -> ( Statement() )*
  * f9 -> "return"
  * f10 -> Expression()
  * f11 -> ";"
  * f12 -> "}"
  */
  public String visit(MethodDeclaration n) throws Exception {
    String _ret=null;

    String type = new String(n.f1.accept(this));
    String name = new String(n.f2.accept(this));

    ClassInfo curClassInfo = symbolTable.get(curClass);

    if(curClassInfo.hasMethodDef(name)){
      throw new Exception("Class " + curClass + " has more than one methods named " + name);
    }

    curMethod = name;
    // MethodInfo curMethodInfo = new MethodInfo(name, type, curClassInfo);
    curMethodInfo = new MethodInfo(name, type, curClassInfo);

    n.f4.accept(this); //Parameter list

    if(curClassInfo.methodOverloads(curMethodInfo)){
      throw new Exception("Overloading done by method " + name + " is not allowed");
    }

    curClassInfo.addMethod(curMethodInfo);

    return _ret;
  }

  /**
  * f0 -> FormalParameter()
  * f1 -> FormalParameterTail()
  */
  public String visit(FormalParameterList n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  */
  public String visit(FormalParameter n) throws Exception {
    String _ret=null;

    // ClassInfo curClassInfo = symbolTable.get(curClass);
    // MethodInfo curMethodInfo = curClassInfo.getMethod(curMethod);

    String type = new String(n.f0.accept(this));

    curMethodInfo.addParameter(type);

    return _ret;
  }

  /**
  * f0 -> ( FormalParameterTerm() )*
  */
  public String visit(FormalParameterTail n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> ","
  * f1 -> FormalParameter()
  */
  public String visit(FormalParameterTerm n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> ArrayType()
  *       | BooleanType()
  *       | IntegerType()
  *       | Identifier()
  */
  public String visit(Type n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "int"
  * f1 -> "["
  * f2 -> "]"
  */
  public String visit(ArrayType n) throws Exception {
    String _ret = new String("int[]");
    return _ret;
  }

  /**
  * f0 -> "boolean"
  */
  public String visit(BooleanType n) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> "int"
  */
  public String visit(IntegerType n) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> Block()
  *       | AssignmentStatement()
  *       | ArrayAssignmentStatement()
  *       | IfStatement()
  *       | WhileStatement()
  *       | PrintStatement()
  */
  public String visit(Statement n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "{"
  * f1 -> ( Statement() )*
  * f2 -> "}"
  */
  public String visit(Block n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> Identifier()
  * f1 -> "="
  * f2 -> Expression()
  * f3 -> ";"
  */
  public String visit(AssignmentStatement n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    return _ret;
  }

  /**
  * f0 -> Identifier()
  * f1 -> "["
  * f2 -> Expression()
  * f3 -> "]"
  * f4 -> "="
  * f5 -> Expression()
  * f6 -> ";"
  */
  public String visit(ArrayAssignmentStatement n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    n.f5.accept(this);
    n.f6.accept(this);
    return _ret;
  }

  /**
  * f0 -> "if"
  * f1 -> "("
  * f2 -> Expression()
  * f3 -> ")"
  * f4 -> Statement()
  * f5 -> "else"
  * f6 -> Statement()
  */
  public String visit(IfStatement n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    n.f5.accept(this);
    n.f6.accept(this);
    return _ret;
  }

  /**
  * f0 -> "while"
  * f1 -> "("
  * f2 -> Expression()
  * f3 -> ")"
  * f4 -> Statement()
  */
  public String visit(WhileStatement n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    return _ret;
  }

  /**
  * f0 -> "System.out.println"
  * f1 -> "("
  * f2 -> Expression()
  * f3 -> ")"
  * f4 -> ";"
  */
  public String visit(PrintStatement n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    return _ret;
  }

  /**
  * f0 -> AndExpression()
  *       | CompareExpression()
  *       | PlusExpression()
  *       | MinusExpression()
  *       | TimesExpression()
  *       | ArrayLookup()
  *       | ArrayLength()
  *       | MessageSend()
  *       | PrimaryExpression()
  */
  public String visit(Expression n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "&&"
  * f2 -> PrimaryExpression()
  */
  public String visit(AndExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "<"
  * f2 -> PrimaryExpression()
  */
  public String visit(CompareExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "+"
  * f2 -> PrimaryExpression()
  */
  public String visit(PlusExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "-"
  * f2 -> PrimaryExpression()
  */
  public String visit(MinusExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "*"
  * f2 -> PrimaryExpression()
  */
  public String visit(TimesExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "["
  * f2 -> PrimaryExpression()
  * f3 -> "]"
  */
  public String visit(ArrayLookup n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> "length"
  */
  public String visit(ArrayLength n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> Identifier()
  * f3 -> "("
  * f4 -> ( ExpressionList() )?
  * f5 -> ")"
  */
  public String visit(MessageSend n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    n.f5.accept(this);
    return _ret;
  }

  /**
  * f0 -> Expression()
  * f1 -> ExpressionTail()
  */
  public String visit(ExpressionList n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> ( ExpressionTerm() )*
  */
  public String visit(ExpressionTail n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> ","
  * f1 -> Expression()
  */
  public String visit(ExpressionTerm n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> IntegerLiteral()
  *       | TrueLiteral()
  *       | FalseLiteral()
  *       | Identifier()
  *       | ThisExpression()
  *       | ArrayAllocationExpression()
  *       | AllocationExpression()
  *       | NotExpression()
  *       | BracketExpression()
  */
  public String visit(PrimaryExpression n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> <INTEGER_LITERAL>
  */
  public String visit(IntegerLiteral n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "true"
  */
  public String visit(TrueLiteral n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "false"
  */
  public String visit(FalseLiteral n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> <IDENTIFIER>
  */
  public String visit(Identifier n) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> "this"
  */
  public String visit(ThisExpression n) throws Exception {
    return n.f0.accept(this);
  }

  /**
  * f0 -> "new"
  * f1 -> "int"
  * f2 -> "["
  * f3 -> Expression()
  * f4 -> "]"
  */
  public String visit(ArrayAllocationExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    n.f4.accept(this);
    return _ret;
  }

  /**
  * f0 -> "new"
  * f1 -> Identifier()
  * f2 -> "("
  * f3 -> ")"
  */
  public String visit(AllocationExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    n.f3.accept(this);
    return _ret;
  }

  /**
  * f0 -> "!"
  * f1 -> PrimaryExpression()
  */
  public String visit(NotExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    return _ret;
  }

  /**
  * f0 -> "("
  * f1 -> Expression()
  * f2 -> ")"
  */
  public String visit(BracketExpression n) throws Exception {
    String _ret=null;
    n.f0.accept(this);
    n.f1.accept(this);
    n.f2.accept(this);
    return _ret;
  }

}
