import java.util.*;

class ClassInfo{
  private String name;
  private ClassInfo parentClass;

  private LinkedHashMap<String,String> variables;
  private LinkedHashMap<String,MethodInfo> vtable;

  public ClassInfo(String name){
    this.name = new String(name);
    this.parentClass = null;
    this.variables = new LinkedHashMap<String,String>();
    this.vtable = new LinkedHashMap<String,MethodInfo>();
  }

  public ClassInfo(String name, ClassInfo parent){
    this.name = new String(name);
    this.parentClass = parent;
    this.variables = new LinkedHashMap<String,String>();
    this.vtable = new LinkedHashMap<String,MethodInfo>(parent.getVtable());
  }



  public String getName(){
    return this.name;
  }

  public ClassInfo getParent(){
    return this.parentClass;
  }

  public LinkedHashMap<String,MethodInfo> getVtable(){
    return this.vtable;
  }

  public int getMethodCount(){
    return vtable.size();
  }

  public MethodInfo getMethod(String name){
    return this.vtable.get(name);
  }

  public int getMethodOffset(String name){
    int offset = 0;
    Set methodSet = this.vtable.keySet();
    Iterator iter = methodSet.iterator();

    while(iter.hasNext()){
      String methodName = (String) iter.next();
      if(methodName.equals(name)){
        return offset;
      }
      else{
        offset += 1;
      }
    }

    return -1;
  }

  public String getVarType(String id){
    if(this.variables.containsKey(id)){
      return this.variables.get(id);
    }
    else{
      return this.parentClass.getVarType(id);
    }
  }

  public int getVarOffset(String id){
    if(this.variables.containsKey(id)){
      int offset = 8; //vtable
      Set varSet = this.variables.keySet();
      Iterator iter = varSet.iterator();

      while(iter.hasNext()){
        String varName = (String) iter.next();

        if(varName.equals(id)){
          return offset;
        }
        else{
          switch(this.variables.get(varName)){
            case "int":
              offset += 4;
              break;
            case "boolean":
              offset += 1;
              break;
            case "int[]":
              offset += 8;
              break;
            default:
              offset += 8;
          }
        }
      }
    }
    else if(this.parentClass != null){
      return this.parentClass.getVarOffset(id);
    }

    return -1;
  }



  public void addVar(String name, String type){
    this.variables.put(name, type);
  }

  public void addMethod(MethodInfo method){
    this.vtable.put(method.getName(), method);
  }



  public boolean hasVar(String id){
    if(this.variables.containsKey(id)){
      return true;
    }
    else if(parentClass != null){
      return this.parentClass.hasVar(id);
    }

    return false;
  }

  //Checks if the variable has been defined in this class
  public boolean hasVarDef(String name){
    return this.variables.containsKey(name);
  }

  public boolean hasMethod(String name){
    if(this.vtable.containsKey(name)){
      return true;
    }
    else{
      return false;
    }
  }

  //Checks if the method has been defined in this class
  public boolean hasMethodDef(String name){
    if(this.hasMethod(name)){
      MethodInfo method = this.getMethod(name);
      if(method.getDefClass().getName() == this.getName()){
        return true;
      }
    }
    return false;
  }

  //Checks if the class has a parent named "parentName"
  public boolean isChildOf(String parentName){
    ClassInfo parent = this.parentClass;

    while(parent != null){
      if(parent.name.equals(parentName)){
        return true;
      }
      parent = parent.parentClass;
    }

    return false;
  }

  //Checks if the method overloads an existing method
  public boolean methodOverloads(MethodInfo method){
    if(this.hasMethod(method.getName())){
      MethodInfo otherMethod = this.getMethod(method.getName());
      //Check return type
      if(!method.getType().equals(otherMethod.getType())){
        return true;
      }
      //Check parameter count
      if(method.parameterCount() != otherMethod.parameterCount()){
        return true;
      }
      //Check parameter types
      for(int i = 0; i < method.parameterCount(); i++){
        if(!method.getParameter(i).equals(otherMethod.getParameter(i))){
          return true;
        }
      }
    }
    return false;
  }



  public void printOffsets(){
    int varOffset = 0;
    int methOffset = 0;

    if(this.parentClass != null){
      varOffset = parentClass.getVarsOffset();
      methOffset = 0;
    }

    System.out.println("-----------Class " + this.name + "-----------");

    //Print variables with their offsets
    System.out.println("---Variables---");

    Set vars = this.variables.keySet();
    Iterator varIter = vars.iterator();

    while(varIter.hasNext()){
      String name = (String) varIter.next();
      System.out.println(this.name + "." + name + " : " + varOffset);
      switch(this.variables.get(name)){
        case "int":
          varOffset += 4;
          break;
        case "boolean":
          varOffset += 1;
          break;
        default:
          varOffset += 8;
      }
    }

    //Print methods with their offsets
    System.out.println("---Methods---");

    Set meths = this.vtable.keySet();
    Iterator methIter = meths.iterator();

    while(methIter.hasNext()){
      String name = (String) methIter.next();
      System.out.println(this.name + "." + name + " : " + methOffset);
      methOffset += 8;
    }
    System.out.println();
  }

  public int getVarsOffset(){
    int offset = 0;

    if(this.parentClass != null){
      offset += parentClass.getVarsOffset();
    }

    Set vars = this.variables.keySet();
    Iterator i = vars.iterator();

    while(i.hasNext()){
      String name = (String) i.next();
      switch(this.variables.get(name)){
        case "int":
          offset += 4;
          break;
        case "boolean":
          offset += 1;
          break;
        default:
          offset += 8;
      }
    }
    return offset;
  }

}
