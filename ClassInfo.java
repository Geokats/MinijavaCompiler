import java.util.*;

class ClassInfo{
  public String name;
  public ClassInfo parentClass;

  public LinkedHashMap<String,String> variables;
  public LinkedHashMap<String,MethodInfo> methods;

  public ClassInfo(String name){
    this.name = new String(name);
    this.parentClass = null;
    this.variables = new LinkedHashMap<String,String>();
    this.methods = new LinkedHashMap<String,MethodInfo>();
  }

  public ClassInfo(String name, ClassInfo parent){
    this.name = new String(name);
    this.parentClass = parent;
    this.variables = new LinkedHashMap<String,String>();
    this.methods = new LinkedHashMap<String,MethodInfo>();
  }

  public void printOffsets(){
    int varOffset = 0;
    int methOffset = 0;

    if(this.parentClass != null){
      varOffset = parentClass.getVarOffset();
      methOffset = parentClass.getMethOffset();
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

    Set meths = this.methods.keySet();
    Iterator methIter = meths.iterator();

    while(methIter.hasNext()){
      String name = (String) methIter.next();
      System.out.println(this.name + "." + name + " : " + methOffset);
      methOffset += 8;
    }
    System.out.println();
  }

  public int getVarOffset(){
    int offset = 0;

    if(this.parentClass != null){
      offset += parentClass.getVarOffset();
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

  public int getMethOffset(){
    int offset = 0;

    if(this.parentClass != null){
      offset += parentClass.getMethOffset();
    }

    Set meths = this.methods.keySet();
    offset += 8 * meths.size();

    return offset;
  }

  public boolean methodOverloads(String method){
    MethodInfo info = this.methods.get(method);
    LinkedList parameterTypes = info.parameterTypes;

    ClassInfo parent = this.parentClass;
    while(parent != null){
      if(parent.methods.containsKey(method)){
        MethodInfo curInfo = parent.methods.get(method);
        LinkedList curParameterTypes = curInfo.parameterTypes;

        if(!info.type.equals(curInfo.type)){
          return true;
        }

        if(parameterTypes.size() != curParameterTypes.size()){
          return true;
        }

        for(int i = 0; i < parameterTypes.size(); i++){
          if(!parameterTypes.get(i).equals(curParameterTypes.get(i))){
            return true;
          }
        }
      }

      parent = parent.parentClass;
    }

    return false;
  }

  public boolean hasField(String id){
    if(this.variables.containsKey(id)){
      return true;
    }
    else if(parentClass != null){
      return this.parentClass.hasField(id);
    }

    return false;
  }

  public boolean hasMethod(String id){
    if(this.methods.containsKey(id)){
      return true;
    }
    else if(parentClass != null){
      return this.parentClass.hasMethod(id);
    }

    return false;
  }

  public MethodInfo getMethod(String id){
    if(this.methods.containsKey(id)){
      return this.methods.get(id);
    }
    else if(parentClass != null){
      return this.parentClass.getMethod(id);
    }

    return null;
  }

  public String getFieldType(String id){
    if(this.variables.containsKey(id)){
      return this.variables.get(id);
    }
    else{
      return this.parentClass.getFieldType(id);
    }
  }

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
}
