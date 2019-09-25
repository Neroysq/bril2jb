package Bril;

import java.util.ArrayList;

public class Function {
    String name;
    public ArrayList<Instruction> instrs;
    public Function(String name, ArrayList<Instruction> instrs) {
        this.name = name;
        this.instrs = instrs;
    }
}
