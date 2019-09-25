package Bril;

import java.util.ArrayList;

public class EffectOperation extends Operation {
    public String op;
    public ArrayList<String> args;

    public EffectOperation(String opName, ArrayList<String> args) {
        this.op = opName;
        this.args = args;
    }

    public String display() {
        String argsString = "[";
        for (String arg : args)
            argsString += arg + ", ";
        argsString = (argsString.length() > 1 ? argsString.substring(0, argsString.length() - 2) : argsString) + "]";
        return op + "::" + argsString;
    }
}
