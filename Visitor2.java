import visitor.GJNoArguDepthFirst;
import syntaxtree.*;
import java.util.*;

public class Visitor2 extends GJNoArguDepthFirst<String>{
  private HashMap<String, ClassInfo> symbolTable;
  private HashMap<String, String> localTable;
  private Stack<LinkedList<String>> exprStack;
  private String curClass;

  public Visitor2(HashMap<String, ClassInfo> symbolTable){
    this.symbolTable = symbolTable;
    this.localTable = null;
    this.exprStack = new Stack<LinkedList<String>>();
    this.curClass = null;
  }

  //Checks if expr of type2 can be assigned to var of type1
  private boolean assignmentCheck(String type1, String type2){
    if(type1.equals(type2)){
      return true;
    }

    ClassInfo type2Class = symbolTable.get(type2);
    if(type2Class != null && type2Class.isChildOf(type1)){
      return true;
    }

    return false;
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
    String methodName = new String("main");

    curClass = className;

    String argsName = new String(n.f11.accept(this));
    String argsType = new String("String[]");

    localTable = new HashMap<String,String>();
    localTable.put(argsName, argsType);

    n.f14.accept(this); //Var Declaration
    n.f15.accept(this); //Statements
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
    curClass = name;

    //We skip these var declarations (class fields)
    //since visitor1 has already saved them in the symbol table

    n.f4.accept(this); //Method declarations
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
    curClass = name;

    //We skip these var declarations (class fields)
    //since visitor1 has already saved them in the symbol table

    n.f6.accept(this); //Method declarations
    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  * f2 -> ";"
  */
  public String visit(VarDeclaration n) throws Exception {
    String _ret=null;
    //Var declaration can only be reached by a method declaration
    //It will never be called for the declaration of a class' fields

    String type = new String(n.f0.accept(this));
    String name = new String(n.f1.accept(this));

    if(localTable.containsKey(name)){
      throw new Exception("Variable " + name + "is defined multiple times");
    }

    localTable.put(name, type);

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

    localTable = new HashMap<String,String>();

    n.f4.accept(this); //Parameter list
    n.f7.accept(this); //Var declaration
    n.f8.accept(this); //Statements

    String retType = new String(n.f10.accept(this));

    if(!assignmentCheck(type, retType)){
      throw new Exception("In method " + name + ", return type doesn't match method's type");
    }

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

    String type = new String(n.f0.accept(this));
    String name = new String(n.f1.accept(this));

    if(localTable.containsKey(name)){
      throw new Exception("Variable " + name + " is defined multiple times");
    }

    localTable.put(name, type);
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
    n.f1.accept(this);
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

    ClassInfo curClassInfo = symbolTable.get(curClass);

    //Id and expr must be of same type
    String id = new String(n.f0.accept(this));
    String idType = null;
    String exprType = new String(n.f2.accept(this));

    //Find id's type
    if(localTable.containsKey(id)){
      idType = localTable.get(id);
    }
    else if(curClassInfo.hasVar(id)){
      idType = curClassInfo.getVarType(id);
    }
    else{
      throw new Exception("Variable " + id + " is never defined");
    }

    //Compare types
    if(!assignmentCheck(idType, exprType)){
      throw new Exception("Cannot assign type " + exprType + " to " + id + " of type " + idType);
    }
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

    ClassInfo curClassInfo = symbolTable.get(curClass);

    String id = new String(n.f0.accept(this));
    String idType = null;

    //Find id's type
    if(localTable.containsKey(id)){
      idType = localTable.get(id);
    }
    else if(curClassInfo.hasVar(id)){
      idType = curClassInfo.getVarType(id);
    }
    else{
      throw new Exception("Variable " + id + " is never defined");
    }

    String posType = new String(n.f2.accept(this));
    String assignmentType = new String(n.f5.accept(this));

    //Check that all the types are correct
    if(!idType.equals("int[]")){
      throw new Exception("Variable " + id + " is not of type int[]");
    }
    if(!posType.equals("int")){
      throw new Exception("Array index must be of type int");
    }
    if(!assignmentType.equals("int")){
      throw new Exception("Value assigned to array's member must be of type int");
    }

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

    //Check if conditional expr is of type boolean
    String condExpr = new String(n.f2.accept(this));
    if(!condExpr.equals("boolean")){
      throw new Exception("Expression at if statement's condition must be of type boolean");
    }

    n.f4.accept(this); //if statement
    n.f5.accept(this); //else statement

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

    //Check if conditional expr is of type boolean
    String condExpr = new String(n.f2.accept(this));
    if(!condExpr.equals("boolean")){
      throw new Exception("Expression at while statement's condition must be of type boolean");
    }

    n.f4.accept(this); //while statement

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

    //Check if expression is of type int
    String expr = new String(n.f2.accept(this));

    if(!expr.equals("int")){
      throw new Exception("Expressions in print statements must be of type int");
    }
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
    String _ret = new String("boolean");
    //Check if both exprs are of type boolean and return "boolean"
    String type1 = new String(n.f0.accept(this));
    String type2 = new String(n.f2.accept(this));

    if(!type1.equals("boolean") || !type2.equals("boolean")){
      throw new Exception("Expressions of an && expression must be of type boolean");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "<"
  * f2 -> PrimaryExpression()
  */
  public String visit(CompareExpression n) throws Exception {
    String _ret = new String("boolean");
    //Check if both exprs are of type int and return "boolean"
    String type1 = new String(n.f0.accept(this));
    String type2 = new String(n.f2.accept(this));

    if(!type1.equals("int") || !type2.equals("int")){
      throw new Exception("Expressions of a > expression must be of type int");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "+"
  * f2 -> PrimaryExpression()
  */
  public String visit(PlusExpression n) throws Exception {
    String _ret = new String("int");
    //Check if both exprs are of type int and return "int"
    String type1 = new String(n.f0.accept(this));
    String type2 = new String(n.f2.accept(this));

    if(!type1.equals("int") || !type2.equals("int")){
      throw new Exception("Expressions of a + expression must be of type int");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "-"
  * f2 -> PrimaryExpression()
  */
  public String visit(MinusExpression n) throws Exception {
    String _ret = new String("int");
    //Check if both exprs are of type int and return "int"
    String type1 = new String(n.f0.accept(this));
    String type2 = new String(n.f2.accept(this));

    if(!type1.equals("int") || !type2.equals("int")){
      throw new Exception("Expressions of a - expression must be of type int");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "*"
  * f2 -> PrimaryExpression()
  */
  public String visit(TimesExpression n) throws Exception {
    String _ret = new String("int");
    //Check if both exprs are of type int and return "int"
    String type1 = new String(n.f0.accept(this));
    String type2 = new String(n.f2.accept(this));

    if(!type1.equals("int") || !type2.equals("int")){
      throw new Exception("Expressions of a * expression must be of type int");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "["
  * f2 -> PrimaryExpression()
  * f3 -> "]"
  */
  public String visit(ArrayLookup n) throws Exception {
    String _ret = new String("int");
    //Check if exprs are of type int[] and int and return "int"
    String arrayType = new String(n.f0.accept(this));
    String indexType = new String(n.f2.accept(this));

    //Check types
    if(!arrayType.equals("int[]")){
      throw new Exception("In array lookup expression must be of type int[]");
    }
    if(!indexType.equals("int")){
      throw new Exception("In array lookup index expression must be of type int");
    }

    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> "length"
  */
  public String visit(ArrayLength n) throws Exception {
    String _ret = new String("int");
    //Check if expr is of type int[] and return int
    String arrayType = new String(n.f0.accept(this));

    if(!arrayType.equals("int[]")){
      throw new Exception("In array length expression must be of type int[]");
    }

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
    String className = new String(n.f0.accept(this));
    String methodName = new String(n.f2.accept(this));

    ClassInfo curClass = symbolTable.get(className);

    if(!curClass.hasMethod(methodName)){
      throw new Exception("Class " + className + " has no method named " + methodName);
    }

    MethodInfo curMethod = curClass.getMethod(methodName);

    //Create an exprList and push it to the stack to be filled
    LinkedList<String> exprList = new LinkedList<String>();
    exprStack.push(exprList);
    n.f4.accept(this); //Expression list
    exprList = exprStack.pop();

    //Check that the expression list matches the parameter list
    if(exprList.size() != curMethod.parameterCount()){
      throw new Exception("Invalid count of arguments given in call of method " + methodName);
    }

    for(int i = 0; i < exprList.size(); i++){
      //Check if types match
      if(!assignmentCheck(curMethod.getParameter(i), exprList.get(i))){
        throw new Exception("In call of method " + methodName + ", argument " + (i+1) + " is of type " + exprList.get(i) + " instead of " + curMethod.getParameter(i));
      }
    }

    return curMethod.getType();
  }

  /**
  * f0 -> Expression()
  * f1 -> ExpressionTail()
  */
  public String visit(ExpressionList n) throws Exception {
    String _ret=null;

    LinkedList<String> exprList = exprStack.pop();
    String expr = new String(n.f0.accept(this));
    exprList.add(expr);
    exprStack.push(exprList);

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

    LinkedList<String> exprList = exprStack.pop();
    String expr = new String(n.f1.accept(this));
    exprList.add(expr);
    exprStack.push(exprList);

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
    if(n.f0.choice instanceof Identifier){
      String id = new String(n.f0.accept(this));
      String idType = null;

      ClassInfo curClassInfo = symbolTable.get(curClass);

      //Find id's type
      if(localTable.containsKey(id)){
        idType = localTable.get(id);
      }
      else if(curClassInfo.hasVar(id)){
        idType = curClassInfo.getVarType(id);
      }
      else{
        throw new Exception("Variable " + id + " is never defined");
      }

      return idType;
    }
    else{
      return n.f0.accept(this);
    }
  }

  /**
  * f0 -> <INTEGER_LITERAL>
  */
  public String visit(IntegerLiteral n) throws Exception {
    return new String("int");
  }

  /**
  * f0 -> "true"
  */
  public String visit(TrueLiteral n) throws Exception {
    return new String("boolean");
  }

  /**
  * f0 -> "false"
  */
  public String visit(FalseLiteral n) throws Exception {
    return new String("boolean");
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
    //Return the type (name) of the current class
    ClassInfo curClassInfo = symbolTable.get(curClass);
    return curClassInfo.getName();
  }

  /**
  * f0 -> "new"
  * f1 -> "int"
  * f2 -> "["
  * f3 -> Expression()
  * f4 -> "]"
  */
  public String visit(ArrayAllocationExpression n) throws Exception {
    String _ret = new String("int[]");
    //Check if expr is of type int and return "int[]"
    String exprType = new String(n.f3.accept(this));

    if(!exprType.equals("int")){
      throw new Exception("In array allocation array size must be of type int");
    }

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
    //The given identifier must be a class name
    String id = new String(n.f1.accept(this));

    //Check if class has been declared
    if(!symbolTable.containsKey(id)){
      throw new Exception("Type " + id + " is never declared");
    }

    return id;
  }

  /**
  * f0 -> "!"
  * f1 -> PrimaryExpression()
  */
  public String visit(NotExpression n) throws Exception {
    String _ret = new String("boolean");
    //Check if expr is of type boolean and return "boolean"
    String exprType = new String(n.f1.accept(this));

    if(!exprType.equals("boolean")){
      throw new Exception("In ! expression, expression must be of type boolean");
    }
    return _ret;
  }

  /**
  * f0 -> "("
  * f1 -> Expression()
  * f2 -> ")"
  */
  public String visit(BracketExpression n) throws Exception {
    return new String(n.f1.accept(this));
  }

}
