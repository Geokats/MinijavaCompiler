import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.*;
import java.io.*;

public class LLVMVisitor extends GJDepthFirst<String,String>{
  private HashMap<String, ClassInfo> symbolTable;
  private LinkedHashMap<String, String> localTable;
  private Stack<String> exprStack;

  private ClassInfo curClass;
  private MethodInfo curMethod;
  private String curType;

  private int registerCount;
  private int ifCount;
  private int loopCount;
  private int allocCount;
  private int oobCount;
  private int andCount;

  private int indent;
  private PrintWriter output;

  public LLVMVisitor(HashMap<String, ClassInfo> symbolTable, String outFileName) throws FileNotFoundException{
    this.symbolTable = symbolTable;
    this.localTable = null;
    this.exprStack = new Stack<String>();

    this.curClass = null;
    this.curMethod = null;
    this.curType = null;

    this.registerCount = 0;
    this.ifCount = 0;
    this.loopCount = 0;
    this.allocCount = 0;
    this.oobCount = 0;
    this.andCount = 0;

    this.indent = 0;
    this.output = new PrintWriter(outFileName);
  }




  private void emit(String str){
    for(int i = 0; i < this.indent; i++){
      this.output.print("\t");
    }
    this.output.print(str);
  }

  private String getRegister(){
    this.registerCount += 1;
    return "%_" + registerCount;
  }

  private String getIf(){
    this.ifCount += 1;
    return "if" + ifCount;
  }

  private String getLoop(){
    this.loopCount += 1;
    return "loop" + loopCount;
  }

  private String getAlloc(){
    this.allocCount += 1;
    return "arr_alloc" + allocCount;
  }

  private String getOob(){
    this.oobCount += 1;
    return "oob" + oobCount;
  }

  private String getAnd(){
    this.andCount += 1;
    return "and" + andCount;
  }

  private String convertType(String type){
    switch(type){
      case "int":
        return "i32";
      case "boolean":
        return "i1";
      case "int[]":
        return "i32*";
      default:
        return "i8*";
    }
  }



  private void emitHeader(){
    emit("declare i8* @calloc(i32, i32)\n");
    emit("declare i32 @printf(i8*, ...)\n");
    emit("declare void @exit(i32)\n");
    emit("\n");
    emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
    emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
    emit("define void @print_int(i32 %i) {\n");
    this.indent++;
    emit("%_str = bitcast [4 x i8]* @_cint to i8*\n");
    emit("call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n");
    emit("ret void\n");
    this.indent--;
    emit("}\n");
    emit("\n");
    emit("define void @throw_oob() {\n");
    this.indent++;
    emit("%_str = bitcast [15 x i8]* @_cOOB to i8*\n");
    emit("call i32 (i8*, ...) @printf(i8* %_str)\n");
    emit("call void @exit(i32 1)\n");
    emit("ret void\n");
    this.indent--;
    emit("}\n\n");
  }

  private void emitVtables(){
    Set classes = symbolTable.keySet();
    Iterator iter = classes.iterator();

    //Main class vtable
    String name = (String) iter.next();
    emit("@." + name + "_vtable = global [1 x i8*] [i8* bitcast (i32 ()* @main to i8*)]\n");

    while(iter.hasNext()){
      name = (String) iter.next();
      ClassInfo curClassInfo = symbolTable.get(name);
      emitVtable(curClassInfo);
    }

    emit("\n\n");
  }

  private void emitVtable(ClassInfo cl){
    LinkedHashMap<String, MethodInfo> vtable = cl.getVtable();
    Set methods = vtable.keySet();
    Iterator iter = methods.iterator();

    String vt = new String();

    vt = vt.concat("@." + cl.getName() + "_vtable = global [");
    vt = vt.concat(cl.getMethodCount() + " x i8*] [");


    while(iter.hasNext()){
      MethodInfo method = vtable.get((String) iter.next());

      vt = vt.concat("i8* bitcast (");

      //Return type
      vt = vt.concat(convertType(method.getType()) + " ");

      //Pointer to "this"
      vt = vt.concat("(i8*");

      //Method parameters
      for(int i = 0; i < method.parameterCount(); i++){
        switch(method.getParameter(i)){
          case "int":
          vt = vt.concat(", i32");
            break;
          case "boolean":
            vt = vt.concat(", i1");
            break;
          default:
            vt = vt.concat(", i8*");
        }
      }

      vt = vt.concat(")* ");

      //Method name
      vt = vt.concat("@" + method.getDefClass().getName() +  "." + method.getName());

      vt = vt.concat(" to i8*)");

      if(iter.hasNext()){
        vt = vt.concat(", ");
      }
    }

    vt = vt.concat("]\n");

    emit(vt);
  }

  //
  // User-generated visitor methods below
  //

  /**
  * f0 -> MainClass()
  * f1 -> ( TypeDeclaration() )*
  * f2 -> <EOF>
  */
  public String visit(Goal n, String argu) throws Exception {
    String _ret=null;

    //Emit Vtables
    emitVtables();
    //Emit header
    emitHeader();

    //Visit program
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);

    //Close output file
    output.close();

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
  public String visit(MainClass n, String argu) throws Exception {
    String _ret=null;
    n.f1.accept(this, argu); //Class name

    n.f11.accept(this, argu); //args name

    this.localTable = new LinkedHashMap<String,String>();

    n.f14.accept(this, argu); //declarations

    emit("define i32 @main() {\n");
    this.indent++;

    //Save local variables in the stack
    Set vars = this.localTable.keySet();
    Iterator iter = vars.iterator();
    while(iter.hasNext()){
      String varName = (String) iter.next();
      String varType = convertType(this.localTable.get(varName));
      emit("%" + varName + " = alloca " + varType + "\n");
    }
    emit("\n");

    n.f15.accept(this, argu); //Statements
    emit("ret i32 0\n");
    this.indent--;
    emit("}\n\n");

    return _ret;
  }

  /**
  * f0 -> ClassDeclaration()
  *       | ClassExtendsDeclaration()
  */
  public String visit(TypeDeclaration n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "class"
  * f1 -> Identifier()
  * f2 -> "{"
  * f3 -> ( VarDeclaration() )*
  * f4 -> ( MethodDeclaration() )*
  * f5 -> "}"
  */
  public String visit(ClassDeclaration n, String argu) throws Exception {
    String _ret=null;

    String name = new String(n.f1.accept(this, argu)); //Class Name
    this.curClass = this.symbolTable.get(name);

    n.f4.accept(this, argu); //Method declaraions
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
  public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
    String _ret=null;

    String name = new String(n.f1.accept(this, argu)); //Class Name
    this.curClass = this.symbolTable.get(name);

    n.f6.accept(this, argu); //Method declarations
    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  * f2 -> ";"
  */
  public String visit(VarDeclaration n, String argu) throws Exception {
    //This can only be visited from inside a method
    String _ret=null;

    String type = new String(n.f0.accept(this, argu));
    String name = new String(n.f1.accept(this, argu));

    this.localTable.put(name, type);

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
  public String visit(MethodDeclaration n, String argu) throws Exception {
    String _ret=null;

    String name = new String(n.f2.accept(this, argu)); //Method name

    this.curMethod = this.curClass.getMethod(name);
    this.localTable = new LinkedHashMap<String, String>();

    emit("define " + convertType(this.curMethod.getType()));
    emit(" @" + this.curClass.getName() + "." + this.curMethod.getName());
    emit("(i8* %this");

    n.f4.accept(this, argu); //Parameter list

    emit(") {\n");
    this.indent++;

    n.f7.accept(this, argu); //Var declarations

    //Save local variables in the stack
    Set vars = this.localTable.keySet();
    Iterator iter = vars.iterator();
    int i = 0;
    while(iter.hasNext()){
      String varName = (String) iter.next();
      String varType = convertType(this.localTable.get(varName));

      emit("%" + varName + " = alloca " + varType + "\n");

      if(i < this.curMethod.parameterCount()){
        //If the var is a parameter, also store its value
        emit("store " + varType + " %." + varName + ", " + varType + "* %" + varName + "\n");
      }

      i++;
    }
    emit("\n");

    n.f8.accept(this, argu); //Method's body

    String ret = new String(n.f10.accept(this, argu)); //Return expression
    emit("ret " + convertType(this.curMethod.getType()) + " " + ret + "\n");

    this.indent--;
    emit("}\n\n");
    this.curMethod = null;
    this.localTable = null;
    return _ret;
  }

  /**
  * f0 -> FormalParameter()
  * f1 -> FormalParameterTail()
  */
  public String visit(FormalParameterList n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  */
  public String visit(FormalParameter n, String argu) throws Exception {
    String _ret=null;

    String type = new String(n.f0.accept(this, argu));
    String name = new String(n.f1.accept(this, argu));

    //Save to local table
    this.localTable.put(name, type);

    emit(", " + convertType(type) + " %." + name);

    return _ret;
  }

  /**
  * f0 -> ( FormalParameterTerm() )*
  */
  public String visit(FormalParameterTail n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> ","
  * f1 -> FormalParameter()
  */
  public String visit(FormalParameterTerm n, String argu) throws Exception {
    String _ret=null;
    n.f1.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> ArrayType()
  *       | BooleanType()
  *       | IntegerType()
  *       | Identifier()
  */
  public String visit(Type n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "int"
  * f1 -> "["
  * f2 -> "]"
  */
  public String visit(ArrayType n, String argu) throws Exception {
    return new String("int[]");
  }

  /**
  * f0 -> "boolean"
  */
  public String visit(BooleanType n, String argu) throws Exception {
    return new String("boolean");
  }

  /**
  * f0 -> "int"
  */
  public String visit(IntegerType n, String argu) throws Exception {
    return new String("int");
  }

  /**
  * f0 -> Block()
  *       | AssignmentStatement()
  *       | ArrayAssignmentStatement()
  *       | IfStatement()
  *       | WhileStatement()
  *       | PrintStatement()
  */
  public String visit(Statement n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "{"
  * f1 -> ( Statement() )*
  * f2 -> "}"
  */
  public String visit(Block n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> Identifier()
  * f1 -> "="
  * f2 -> Expression()
  * f3 -> ";"
  */
  public String visit(AssignmentStatement n, String argu) throws Exception {
    String _ret=null;

    String expr = new String(n.f2.accept(this, argu));

    String name = new String(n.f0.accept(this, argu));
    String type;

    if(this.localTable.containsKey(name)){
      type = convertType(this.localTable.get(name));
      emit("store " + type + " " + expr + ", " + type + "* %" + name + "\n");
    }
    else{
      //if it's not in the local table then it belongs to the class
      type = convertType(this.curClass.getVarType(name));
      int offset = this.curClass.getVarOffset(name);
      String r1 = getRegister();
      String r2 = getRegister();

      emit(r1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");
      emit(r2 + " = bitcast i8* " + r1 + " to " + type + "*\n");
      emit("store " + type + " " + expr + ", " + type + "* " + r2 + "\n");
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
  public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
    String _ret=null;

    String rIndex = new String(n.f2.accept(this, argu));
    String rVal = new String(n.f5.accept(this, argu));
    String rArr = getRegister();

    //Load array
    String id = new String(n.f0.accept(this, argu));
    if(this.localTable.containsKey(id)){
      emit(rArr + " = load i32*, i32** %" + id + "\n");
    }
    else if(this.curClass.hasVar(id)){
      String r1 = getRegister();
      emit(r1 + " = getelementptr i8, i8* %this, i32 " + this.curClass.getVarOffset(id) + "\n");
      String r2 = getRegister();
      emit(r2 + " = bitcast i8* " + r1 + " to i32**\n");
      emit(rArr + " = load i32*, i32** " + r2 + "\n");
    }

    //Load size
    String rSize = getRegister();
    emit(rSize + " = load i32, i32* " + rArr + "\n");
    String r1 = getRegister();
    emit(r1 + " = icmp ult i32 " + rIndex + ", " + rSize + "\n");

    String l1 = getOob();
    String l2 = getOob();
    String l3 = getOob();

    emit("br i1 " + r1 + ", label %" + l1 + ", label %" + l2 + "\n");

    emit(l1 + ":\n");
    this.indent++;
    r1 = getRegister();
    emit(r1 + " = add i32 " + rIndex + ", 1\n");
    String r2 = getRegister();
    emit(r2 + " = getelementptr i32, i32* " + rArr + ", i32 " + r1 + "\n");
    emit("store i32 " + rVal + ", i32* " + r2 + "\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l2 + ":\n");
    this.indent++;
    emit("call void @throw_oob()\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l3 + ":\n");



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
  public String visit(IfStatement n, String argu) throws Exception {
    String _ret=null;

    String cond = n.f2.accept(this, argu); //Conditional expression

    String l1 = getIf();
    String l2 = getIf();
    String l3 = getIf();
    emit("br i1 " + cond + ", label %" + l1 + ", label %" + l2 + "\n\n");

    emit(l1 + ":\n");
    this.indent++;
    n.f4.accept(this, argu); //If statement
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l2 + ":\n");
    this.indent++;
    n.f6.accept(this, argu); //Else statement
    emit("br label %" + l3 + "\n");
    this.indent--;
    emit(l3 + ":\n");

    return _ret;
  }

  /**
  * f0 -> "while"
  * f1 -> "("
  * f2 -> Expression()
  * f3 -> ")"
  * f4 -> Statement()
  */
  public String visit(WhileStatement n, String argu) throws Exception {
    String _ret=null;

    String l1 = getLoop();
    String l2 = getLoop();
    String l3 = getLoop();

    emit("br label %" + l1 + "\n");
    emit(l1 + ":\n");
    this.indent++;

    String cond = new String(n.f2.accept(this, argu));
    emit("br i1 " + cond + ", label %" + l2 + ", label %" + l3 + "\n");
    this.indent--;

    emit(l2 + ":\n");
    this.indent++;
    n.f4.accept(this, argu); //statement
    emit("br label %" + l1 + "\n");
    this.indent--;

    emit(l3 + ":\n\n");
    return _ret;
  }

  /**
  * f0 -> "System.out.println"
  * f1 -> "("
  * f2 -> Expression()
  * f3 -> ")"
  * f4 -> ";"
  */
  public String visit(PrintStatement n, String argu) throws Exception {
    String _ret=null;

    String r = n.f2.accept(this, argu);
    emit("call void (i32) @print_int(i32 " + r + ")\n");

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
  public String visit(Expression n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "&&"
  * f2 -> PrimaryExpression()
  */
  public String visit(AndExpression n, String argu) throws Exception {
    String res = getRegister();
    String rt = getRegister();
    String rf = getRegister();

    String l1 = getAnd();
    String l2 = getAnd();
    String l3 = getAnd();

    //Evaluate first part
    String r1 = n.f0.accept(this, argu);
    emit("br i1 " + r1 + ", label %" + l1 + ", label %" + l2 + "\n");

    //If true evaluate second part
    emit(l1 + ":\n");
    this.indent++;
    String r2 = n.f2.accept(this, argu);
    emit(rt + " = add i1 0, " + r2 + "\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    //If false set to 0
    emit(l2 + ":\n");
    this.indent++;
    emit(rf + " = add i1 0, 0\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l3 + ":\n");
    this.indent++;
    emit(res + " = phi i1 [" + rt + ", %"+ l1 + "], [" + rf + ", %" + l2 + "]\n");
    this.indent--;

    emit("\n");

    return res;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "<"
  * f2 -> PrimaryExpression()
  */
  public String visit(CompareExpression n, String argu) throws Exception {
    String r1 = n.f0.accept(this, argu);
    String r2 = n.f2.accept(this, argu);
    String r0 = getRegister();

    emit(r0 + " = icmp slt i32 " + r1 + ", " + r2 + "\n");

    return r0;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "+"
  * f2 -> PrimaryExpression()
  */
  public String visit(PlusExpression n, String argu) throws Exception {
    String r1 = n.f0.accept(this, argu);
    String r2 = n.f2.accept(this, argu);
    String r0 = getRegister();

    emit(r0 + " = add i32 " + r1 + ", " + r2 + "\n");

    return r0;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "-"
  * f2 -> PrimaryExpression()
  */
  public String visit(MinusExpression n, String argu) throws Exception {
    String r1 = n.f0.accept(this, argu);
    String r2 = n.f2.accept(this, argu);
    String r0 = getRegister();

    emit(r0 + " = sub i32 " + r1 + ", " + r2 + "\n");

    return r0;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "*"
  * f2 -> PrimaryExpression()
  */
  public String visit(TimesExpression n, String argu) throws Exception {
    String r1 = n.f0.accept(this, argu);
    String r2 = n.f2.accept(this, argu);
    String r0 = getRegister();

    emit(r0 + " = mul i32 " + r1 + ", " + r2 + "\n");

    return r0;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "["
  * f2 -> PrimaryExpression()
  * f3 -> "]"
  */
  public String visit(ArrayLookup n, String argu) throws Exception {
    String rArr = new String(n.f0.accept(this, argu));
    String rIndex = new String(n.f2.accept(this, argu));

    //Load array's size
    String r1 = getRegister();
    emit(r1 + " = load i32, i32* " + rArr + "\n");
    //Check if index < size
    String r2 = getRegister();
    emit(r2 + " = icmp ult i32 " + rIndex + ", " + r1 + "\n");

    String l1 = getOob();
    String l2 = getOob();
    String l3 = getOob();
    emit("br i1 " + r2 + ", label %" + l1 + ", label %" + l2 + "\n");

    emit(l1 + ":\n");
    this.indent++;
    //Add 1 to the index
    r1 = getRegister();
    emit(r1 + " = add i32 " + rIndex + ", 1\n");
    r2 = getRegister();
    emit(r2 + " = getelementptr i32, i32* " + rArr + ", i32 " + r1 + "\n");
    String res = getRegister();
    emit(res + " = load i32, i32* " + r2 + "\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l2 + ":\n");
    this.indent++;
    emit("call void @throw_oob()\n");
    emit("br label %" + l3 + "\n");
    this.indent--;

    emit(l3 + ":\n");

    return res;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> "length"
  */
  public String visit(ArrayLength n, String argu) throws Exception {
    String rArr = new String(n.f0.accept(this, argu));
    String res = getRegister();
    emit(res + " = load i32, i32* " + rArr + "\n");

    return res;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> Identifier()
  * f3 -> "("
  * f4 -> ( ExpressionList() )?
  * f5 -> ")"
  */
  public String visit(MessageSend n, String argu) throws Exception {
    String rObj = n.f0.accept(this, argu);

    String methodName = new String(n.f2.accept(this, argu));
    ClassInfo cl = this.symbolTable.get(curType);
    MethodInfo meth = cl.getMethod(methodName);

    //Get arguments
    String exprList = new String("i8* " + rObj);
    this.exprStack.push(exprList);
    n.f4.accept(this, argu); //ExprList
    exprList = this.exprStack.pop();
    //Add types to exprs
    String[] exprArr = exprList.split(",");
    exprList = exprArr[0];
    for(int i = 0; i < meth.parameterCount(); i++){
      exprList = exprList.concat(", " + convertType(meth.getParameter(i)) + exprArr[i+1]);
    }

    //Get method
    String r1 = getRegister();
    emit(r1 + " = bitcast i8* " + rObj + " to i8***\n");
    String r2 = getRegister();
    emit(r2 + " = load i8**, i8*** " + r1 + "\n");
    r1 = getRegister();
    emit(r1 + " = getelementptr i8*, i8** " + r2 + ", i32 " + cl.getMethodOffset(methodName) + "\n");
    r2 = getRegister();
    emit(r2 + " = load i8*, i8** " + r1 + "\n");
    String rMeth = getRegister();
    emit(rMeth + " = bitcast i8* " + r2 + " to " + meth.llvmSign() + "*\n");

    //Call method
    emit(";" + methodName + "\n");
    String res = getRegister();
    emit(res + " = call " + convertType(meth.getType()) + " " + rMeth + "(" + exprList + ")\n");

    //Store type maybe another MessageSend will be called on the return val of this ;)
    this.curType = meth.getType();

    return res;
  }

  /**
  * f0 -> Expression()
  * f1 -> ExpressionTail()
  */
  public String visit(ExpressionList n, String argu) throws Exception {
    String _ret=null;

    String r = new String(n.f0.accept(this, argu));
    String exprList = this.exprStack.pop();
    exprList = exprList.concat(", " + r);
    this.exprStack.push(exprList);

    n.f1.accept(this, argu);

    return _ret;
  }

  /**
  * f0 -> ( ExpressionTerm() )*
  */
  public String visit(ExpressionTail n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> ","
  * f1 -> Expression()
  */
  public String visit(ExpressionTerm n, String argu) throws Exception {
    String _ret=null;

    String r = new String(n.f1.accept(this, argu));
    String exprList = this.exprStack.pop();
    exprList = exprList.concat(", " + r);
    this.exprStack.push(exprList);

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
  public String visit(PrimaryExpression n, String argu) throws Exception {
    if(n.f0.choice instanceof Identifier){
      String name = n.f0.accept(this, argu);
      String type;
      String res = getRegister();

      if(this.localTable.containsKey(name)){
        this.curType = this.localTable.get(name);
        type = convertType(this.curType);
        emit(res + " = load " + type + ", " + type + "* %" + name + "\n");
      }
      else if(this.curClass.hasVar(name)){
        this.curType = this.curClass.getVarType(name);
        type = convertType(this.curClass.getVarType(name));
        String r1 = getRegister();
        emit(r1 + " = getelementptr i8, i8* %this, i32 " + this.curClass.getVarOffset(name) + "\n");
        String r2 = getRegister();
        emit(r2 + " = bitcast i8* " + r1 + " to " + type + "*\n");
        emit(res + " = load " + type + ", " + type + "* " + r2 + "\n");
      }

      return res;
    }
    else{
      return n.f0.accept(this, argu);
    }
  }

  /**
  * f0 -> <INTEGER_LITERAL>
  */
  public String visit(IntegerLiteral n, String argu) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> "true"
  */
  public String visit(TrueLiteral n, String argu) throws Exception {
    return new String("1");
  }

  /**
  * f0 -> "false"
  */
  public String visit(FalseLiteral n, String argu) throws Exception {
    return new String("0");
  }

  /**
  * f0 -> <IDENTIFIER>
  */
  public String visit(Identifier n, String argu) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> "this"
  */
  public String visit(ThisExpression n, String argu) throws Exception {
    this.curType = this.curClass.getName();
    return new String("%this");
  }

  /**
  * f0 -> "new"
  * f1 -> "int"
  * f2 -> "["
  * f3 -> Expression()
  * f4 -> "]"
  */
  public String visit(ArrayAllocationExpression n, String argu) throws Exception {
    String size = new String(n.f3.accept(this, argu));
    String r1, r2;
    String l1 = getAlloc();
    String l2 = getAlloc();

    //Check if size is positive
    r1 = getRegister();
    emit(r1 + " = icmp slt i32 " + size + ", 0\n");
    emit("br i1 " + r1 + ", label %" + l1 + ", label %" + l2 + "\n");

    //If size is negative
    emit(l1 + ":\n");
    this.indent++;
    emit("call void @throw_oob()\n");
    emit("br label %" + l2 + "\n");
    this.indent--;

    //If size is positive
    emit(l2 + ":\n");
    this.indent++;
    //add 1 to the size
    r1 = getRegister();
    emit(r1 + " = add i32 " + size + ", 1\n");
    r2 = getRegister();
    emit(r2 + " = call i8* @calloc(i32 4, i32 " + r1 + ")\n");
    r1 = getRegister();
    emit(r1 + " = bitcast i8* " + r2 + " to i32*\n");
    //store the size in the first position
    emit("store i32 " + size + ", i32* " + r1 + "\n");
    this.indent--;

    return r1;
  }

  /**
  * f0 -> "new"
  * f1 -> Identifier()
  * f2 -> "("
  * f3 -> ")"
  */
  public String visit(AllocationExpression n, String argu) throws Exception {
    String className = new String(n.f1.accept(this, argu));
    ClassInfo cl = this.symbolTable.get(className);

    String r1 = getRegister();
    String r2 = getRegister();
    String r3 = getRegister();

    int size = cl.getVarsOffset() + 8;

    emit(r1 + " = call i8* @calloc(i32 1, i32 " + size + ")\n");

    emit(r2 + " = bitcast i8* " + r1 + " to i8***\n");

    emit(r3 + " = getelementptr [" + cl.getMethodCount() + " x i8*], [" + cl.getMethodCount() + " x i8*]* @." + cl.getName() + "_vtable, i32 0, i32 0\n");

    emit("store i8** " + r3 + ", i8*** " + r2 + "\n");

    this.curType = className;
    return r1;
  }

  /**
  * f0 -> "!"
  * f1 -> PrimaryExpression()
  */
  public String visit(NotExpression n, String argu) throws Exception {
    String expr = new String(n.f1.accept(this, argu));
    String r = getRegister();
    emit(r + " = xor i1 " + expr + ", 1\n");
    return r;
  }

  /**
  * f0 -> "("
  * f1 -> Expression()
  * f2 -> ")"
  */
  public String visit(BracketExpression n, String argu) throws Exception {
    return n.f1.accept(this, argu);
  }

}
