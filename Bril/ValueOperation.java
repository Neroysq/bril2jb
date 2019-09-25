package Bril;

import java.util.ArrayList;

public class ValueOperation extends Operation {
    public String opName;
    public String destName;
    public Type type;
    public ArrayList<String> args;

    public ValueOperation(String opName, String destName, Type type, ArrayList<String> args) {
        this.opName = opName;
        this.destName = destName;
        this.type = type;
        this.args = args;
    }

    public String display() {
        String argsString = "[";
        for (String arg : args)
            argsString += arg + ", ";
        argsString = argsString.substring(0, argsString.length() - 2) + "]";
        return opName + "::" + "::" + destName + "::" + argsString;
    }
}
