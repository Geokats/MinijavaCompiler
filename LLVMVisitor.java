import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.*;
import java.io.*;

public class LLVMVisitor extends GJDepthFirst<String,String>{
  private HashMap<String, ClassInfo> symbolTable;
  private HashMap<String, String> localTable;
  private Stack<LinkedList<String>> exprStack;
  private String curClass;
  private int registerCount;
  private int flagCount;
  private PrintWriter output;

  public LLVMVisitor(HashMap<String, ClassInfo> symbolTable, String outFileName) throws FileNotFoundException{
    this.symbolTable = symbolTable;
    this.registerCount = 0;
    this.flagCount = 0;
    this.output = new PrintWriter(outFileName);
  }

  private void emit(String str){
    this.output.println(str);
  }

  private String getRegister(){
    this.registerCount += 1;
    return "%_" + registerCount;
  }

  private String getFlag(){
    this.registerCount += 1;
    return "flag_" + flagCount;
  }

  private void emitHeader(){
    emit("declare i8* @calloc(i32, i32)");
    emit("declare i32 @printf(i8*, ...)");
    emit("declare void @exit(i32)");
    emit("");
    emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
    emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
    emit("define void @print_int(i32 %i) {");
    emit("%_str = bitcast [4 x i8]* @_cint to i8*");
    emit("call i32 (i8*, ...) @printf(i8* %_str, i32 %i)");
    emit("ret void");
    emit("}");
    emit("");
    emit("define void @throw_oob() {");
    emit("%_str = bitcast [15 x i8]* @_cOOB to i8*");
    emit("call i32 (i8*, ...) @printf(i8* %_str)");
    emit("call void @exit(i32 1)");
    emit("ret void");
    emit("}");
    emit("");

  }

  private void emitVtables(){
    Set classes = symbolTable.keySet();
    Iterator iter = classes.iterator();

    //Main class vtable
    String name = (String) iter.next();
    emit("@." + name + "_vtable = global [1 x i8*] [i8* bitcast (i32 ()* @main to i8*)]");

    while(iter.hasNext()){
      name = (String) iter.next();
      ClassInfo curClassInfo = symbolTable.get(name);
      emitVtable(curClassInfo);
    }

    emit("");
    emit("");
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
      switch(method.getType()){
        case "int":
          vt = vt.concat("i32 ");
          break;
        case "boolean":
          vt = vt.concat("i1 ");
          break;
        default:
          vt = vt.concat("i8* ");
      }

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

    vt = vt.concat("]");

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

    n.f14.accept(this, argu); //declarations

    emit("define i32 @main() {");

    n.f15.accept(this, argu); //Statements
    emit("}");
    emit("");

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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
    n.f7.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  * f2 -> ";"
  */
  public String visit(VarDeclaration n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
    n.f7.accept(this, argu);
    n.f8.accept(this, argu);
    n.f9.accept(this, argu);
    n.f10.accept(this, argu);
    n.f11.accept(this, argu);
    n.f12.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
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
    n.f0.accept(this, argu);
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
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> "boolean"
  */
  public String visit(BooleanType n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "int"
  */
  public String visit(IntegerType n, String argu) throws Exception {
    return n.f0.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
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
    String r1 = n.f0.accept(this, argu);
    String r2 = n.f2.accept(this, argu);
    String r0 = getRegister();

    emit(r0 + " = and i32 " + r1 + ", " + r2);

    return r0;
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

    emit("blah");

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

    emit(r0 + " = add i32 " + r1 + ", " + r2);

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

    emit(r0 + " = sub i32 " + r1 + ", " + r2);

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

    emit(r0 + " = mul i32 " + r1 + ", " + r2);

    return r0;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "["
  * f2 -> PrimaryExpression()
  * f3 -> "]"
  */
  public String visit(ArrayLookup n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> "length"
  */
  public String visit(ArrayLength n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
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
  public String visit(MessageSend n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> Expression()
  * f1 -> ExpressionTail()
  */
  public String visit(ExpressionList n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
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
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
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
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> <INTEGER_LITERAL>
  */
  public String visit(IntegerLiteral n, String argu) throws Exception {
    String r = getRegister();
    emit(r + " = " + n.f0.toString());
    return r;
  }

  /**
  * f0 -> "true"
  */
  public String visit(TrueLiteral n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "false"
  */
  public String visit(FalseLiteral n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> <IDENTIFIER>
  */
  public String visit(Identifier n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "this"
  */
  public String visit(ThisExpression n, String argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
  * f0 -> "new"
  * f1 -> "int"
  * f2 -> "["
  * f3 -> Expression()
  * f4 -> "]"
  */
  public String visit(ArrayAllocationExpression n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> "new"
  * f1 -> Identifier()
  * f2 -> "("
  * f3 -> ")"
  */
  public String visit(AllocationExpression n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> "!"
  * f1 -> PrimaryExpression()
  */
  public String visit(NotExpression n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return _ret;
  }

  /**
  * f0 -> "("
  * f1 -> Expression()
  * f2 -> ")"
  */
  public String visit(BracketExpression n, String argu) throws Exception {
    String _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    return _ret;
  }

}
