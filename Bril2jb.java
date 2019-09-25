import Bril.*;
import Bril.Label;
import Bril.Type;
import org.json.JSONArray;
import org.json.JSONObject;
import org.objectweb.asm.*;
import java.io.FileOutputStream;
import Utils.*;


import static javax.lang.model.SourceVersion.isName;
import static org.objectweb.asm.Opcodes.*;
import picocli.CommandLine;
import picocli.CommandLine.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "bril2jb", version = "bril2jb 0.1.0", mixinStandardHelpOptions = true,
        description = "A tool to translate bril code to Java Bytecode")
public class Bril2jb implements Callable<Integer> {
    @Parameters(index = "0", description = "The source code file.")
    File inputFile;

    @Parameters(index = "1", description = "The output class path.")
    Path outputPath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Bril2jb()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        String inputFileName = inputFile.getName();
        if (inputFileName.contains("."))
            inputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
        String className = isName(inputFileName) ? inputFileName : "trans";

        String outputFileName = className + ".class";
        File outputFile = outputPath.resolve(outputFileName).toFile();

        Program program = parseJson(inputFile);

        ClassWriter cw = translate(program, className);

        //save bytecode into disk
        FileOutputStream out = new FileOutputStream(outputFile);
        out.write(cw.toByteArray());
        out.close();

        return 0;
    }

    private Program parseJson(File jsonFile) throws Exception {
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile.getPath())));
        JSONObject input = new JSONObject(jsonString);
        //System.out.println(input.toString(4));
        JSONArray functions = input.getJSONArray("functions");
        JSONObject mainFunc = functions.getJSONObject(0);
        JSONArray instrsJson = mainFunc.getJSONArray("instrs");
        ArrayList<Instruction> instrs = new ArrayList<>();
        for (int i = 0; i < instrsJson.length(); ++i) {
            JSONObject insJson = instrsJson.getJSONObject(i);
            Instruction ins = null;
            if (insJson.has("label")) {
                Label lbl = new Label(insJson.getString("label"));
                ins = lbl;
            } else if (insJson.getString("op").matches("const")) {
                if (insJson.getString("type").equals("int")) {
                    ConstIntOperation cio = new ConstIntOperation(insJson.getString("op"), insJson.getString("dest"), Type.tInt, insJson.getInt("value"));
                    ins = cio;
                }
                else {
                    ConstBoolOperation cbo = new ConstBoolOperation(insJson.getString("op"), insJson.getString("dest"), Type.tBool, insJson.getBoolean("value"));
                    ins = cbo;
                }
            } else {
                JSONArray argsJson = insJson.getJSONArray("args");
                ArrayList<String> args = new ArrayList<>();
                for (int j = 0; j < argsJson.length(); ++j) {
                    args.add(argsJson.getString(j));
                }
                if (insJson.getString("op").matches("add|mul|sub|div|eq|lt|gt|le|ge|not|and|or|id")) {

                    ValueOperation vo = new ValueOperation(insJson.getString("op"), insJson.getString("dest"),
                            insJson.getString("type").equals("int") ? Type.tInt : Type.tBool, args);
                    ins = vo;
                } else if (insJson.getString("op").matches("jmp|br|ret|nop|print")) {

                    EffectOperation eo = new EffectOperation(insJson.getString("op"), args);
                    ins = eo;
                }
            }
            instrs.add(ins);
            //System.out.println(ins.display());
        }
        ArrayList<Function> funcs = new ArrayList<>();
        Function main = new Function("main", instrs);
        funcs.add(main);
        Program program = new Program(funcs);
        return program;
    }

    private ClassWriter translate(Program program, String className) {


        HashMap<String, Type> varType = new HashMap<>();
        HashMap<String, org.objectweb.asm.Label> labelHashMap = new HashMap<>();
        HashMap<String, Integer> varToLocalNo = new HashMap<>();
        preprocess(program, varType, labelHashMap, varToLocalNo);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V10, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
        cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        //default constructor
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0); //load the first local variable: this
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        //main method
        {

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();

            int maxStack = 0;
            for (Instruction ins : program.functions.get(0).instrs) {
                if (ins instanceof ConstBoolOperation) {
                    int value;
                    String dest;
                    ConstBoolOperation cbo = (ConstBoolOperation) ins;
                    value = cbo.value ? 1 : 0;
                    dest = cbo.dest;
                    InsTrans.pushConst(mv, value);
                    int varNo = getOrCreatLocalVar(varToLocalNo, dest);
                    InsTrans.storeVarBool(mv, varNo);
                } else if (ins instanceof ConstIntOperation) {
                    long value;
                    String dest;
                    ConstIntOperation cio = (ConstIntOperation) ins;
                    value = cio.value;
                    dest = cio.dest;
                    InsTrans.pushConst(mv, value);
                    int varNo = getOrCreatLocalVar(varToLocalNo, dest);
                    InsTrans.storeVarLong(mv, varNo);
                } else if (ins instanceof EffectOperation) {
                    EffectOperation eo = (EffectOperation) ins;
                    if (eo.op.equals("jmp")) {
                        String labelName = eo.args.get(0);
                        if (!labelHashMap.containsKey(labelName)) {
                            labelHashMap.put(labelName, new org.objectweb.asm.Label());
                        }
                        org.objectweb.asm.Label label = getOrCreatLabel(labelHashMap, labelName);
                        InsTrans.jmpLabel(mv, label);
                    } else if (eo.op.equals("br")) {
                        String condVarName = eo.args.get(0);
                        String lLabelName = eo.args.get(1);
                        String rLabelName = eo.args.get(2);
                        int varNo = getOrCreatLocalVar(varToLocalNo, condVarName);

                        InsTrans.getVarBool(mv, varNo);
                        org.objectweb.asm.Label lLabel = getOrCreatLabel(labelHashMap, lLabelName);
                        InsTrans.jmpWhenTrue(mv, lLabel);

                        InsTrans.getVarBool(mv, varNo);
                        org.objectweb.asm.Label rLabel = getOrCreatLabel(labelHashMap, rLabelName);
                        InsTrans.jmpWhenFalse(mv, rLabel);
                    } else if (eo.op.equals("ret")) {
                        InsTrans.returnVoid(mv);
                    } else if (eo.op.equals("print")) {
                        ArrayList<String> args = eo.args;
                        //TODO: unify the style

                        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"); //put System.out to operand stack
                        String descriptor = "(", obj = "";
                        for (String varName : args) {
                            Type argType = varType.get(varName);
                            int argVarNo = getOrCreatLocalVar(varToLocalNo, varName);
                            if (argType == Type.tBool) {
                                InsTrans.getVarBool(mv, argVarNo);
                                descriptor += "Z";
                            } else if (argType == Type.tInt) {
                                InsTrans.getVarLong(mv, argVarNo);
                                descriptor += "J";
                            }
                            if (obj.equals(""))
                                obj += "\u0001";
                            else
                                obj += " \u0001";
                        }
                        descriptor += ")";

                        mv.visitInvokeDynamicInsn("makeConcatWithConstants", descriptor + "Ljava/lang/String;",
                                new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/StringConcatFactory",
                                        "makeConcatWithConstants", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false),
                                new Object[]{obj});

                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                    }
                } else if (ins instanceof ValueOperation) {
                    ValueOperation vo = (ValueOperation) ins;
                    if (vo.opName.matches("add|mul|sub|div")) {
                        String lvarName = vo.args.get(0), rvarName = vo.args.get(1);
                        int lvarNo = getOrCreatLocalVar(varToLocalNo, lvarName), rvarNo = getOrCreatLocalVar(varToLocalNo, rvarName);
                        String rstvarName = vo.destName;
                        int rstvarNo = getOrCreatLocalVar(varToLocalNo, rstvarName);

                        InsTrans.getVarLong(mv, lvarNo);
                        InsTrans.getVarLong(mv, rvarNo);
                        if (vo.opName.equals("add")) {
                            InsTrans.addLong(mv);
                        } else if (vo.opName.equals("mul")) {
                            InsTrans.mulLong(mv);
                        } else if (vo.opName.equals("sub")) {
                            InsTrans.subLong(mv);
                        } else {
                            InsTrans.divLong(mv);
                        }
                        InsTrans.storeVarLong(mv, rstvarNo);
                    } else if (vo.opName.matches("eq|lt|gt|le|ge")) {
                        String lvarName = vo.args.get(0), rvarName = vo.args.get(1);
                        int lvarNo = getOrCreatLocalVar(varToLocalNo, lvarName), rvarNo = getOrCreatLocalVar(varToLocalNo, rvarName);
                        String rstvarName = vo.destName;
                        int rstvarNo = getOrCreatLocalVar(varToLocalNo, rstvarName);

                        InsTrans.getVarLong(mv, lvarNo);
                        InsTrans.getVarLong(mv, rvarNo);
                        InsTrans.cmpLong(mv);

                        org.objectweb.asm.Label branch0 = new org.objectweb.asm.Label(), branch1 = new org.objectweb.asm.Label();

                        if (vo.opName.equals("eq")) {
                            InsTrans.ifEq(mv, branch0);
                        } else if (vo.opName.equals("lt")) {
                            InsTrans.ifLt(mv, branch0);
                        } else if (vo.opName.equals("gt")) {
                            InsTrans.ifGt(mv, branch0);
                        } else if (vo.opName.equals("le")) {
                            InsTrans.ifLe(mv, branch0);
                        } else {
                            InsTrans.ifGe(mv, branch0);
                        }

                        InsTrans.pushConst(mv, 0);
                        InsTrans.jmpLabel(mv, branch1);
                        InsTrans.setLabel(mv, branch0);
                        InsTrans.pushConst(mv, 1);
                        InsTrans.setLabel(mv, branch1);
                        InsTrans.storeVarBool(mv, rstvarNo);

                    } else if (vo.opName.matches("not|and|or")) {
                        if (vo.opName.matches("not")) {
                            String varName = vo.args.get(0);
                            int varNo = getOrCreatLocalVar(varToLocalNo, varName);
                            String rstvarName = vo.destName;
                            int rstvarNo = getOrCreatLocalVar(varToLocalNo, rstvarName);

                            org.objectweb.asm.Label branch0 = new org.objectweb.asm.Label(), branch1 = new org.objectweb.asm.Label();

                            InsTrans.getVarBool(mv, varNo);
                            InsTrans.ifEq(mv, branch0);
                            InsTrans.pushConst(mv, 0);
                            InsTrans.jmpLabel(mv, branch1);
                            InsTrans.setLabel(mv, branch0);
                            InsTrans.pushConst(mv, 1);
                            InsTrans.setLabel(mv, branch1);
                            InsTrans.storeVarBool(mv, rstvarNo);

                            InsTrans.storeVarBool(mv, rstvarNo);
                        } else {
                            String lvarName = vo.args.get(0), rvarName = vo.args.get(1);
                            int lvarNo = getOrCreatLocalVar(varToLocalNo, lvarName), rvarNo = getOrCreatLocalVar(varToLocalNo, rvarName);
                            String rstvarName = vo.destName;
                            int rstvarNo = getOrCreatLocalVar(varToLocalNo, rstvarName);

                            InsTrans.getVarBool(mv, lvarNo);
                            InsTrans.getVarBool(mv, rvarNo);

                            if (vo.opName.equals("and")) {
                                InsTrans.andBool(mv);
                            } else if (vo.opName.equals("or")){
                                InsTrans.orBool(mv);
                            }

                            InsTrans.storeVarBool(mv, rstvarNo);
                        }
                    } else if (vo.opName.matches("id")) {
                        String varName = vo.args.get(0);
                        int varNo = getOrCreatLocalVar(varToLocalNo, varName);
                        String rstvarName = vo.destName;
                        int rstvarNo = getOrCreatLocalVar(varToLocalNo, rstvarName);
                        Type type =varType.get(varName);
                        if (type == Type.tBool) {
                            InsTrans.getVarBool(mv, varNo);
                            InsTrans.storeVarBool(mv, rstvarNo);
                        } else if (type == Type.tInt) {
                            InsTrans.getVarLong(mv, varNo);
                            InsTrans.storeVarLong(mv, rstvarNo);
                        }
                    }
                } else if (ins instanceof Label) {
                    String labelName = ((Label) ins).labelName;
                    if (!labelHashMap.containsKey(labelName)) {
                        labelHashMap.put(labelName, new org.objectweb.asm.Label());
                    }
                    org.objectweb.asm.Label label = labelHashMap.get(labelName);
                    InsTrans.setLabel(mv, label);
                }
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);

            mv.visitEnd();
        }
        cw.visitEnd();
        return cw;
    }

    private void preprocess(Program program, HashMap<String, Type> varType, HashMap<String, org.objectweb.asm.Label> labelHashMap, HashMap<String, Integer> varToLocalNo) {
        HashMap<String, HashSet<String>> idLinks =  new HashMap<>();
        for (Instruction ins : program.functions.get(0).instrs) {
            if (ins instanceof ValueOperation) {
                ValueOperation vo = (ValueOperation) ins;
                if (vo.opName.equals("id")) {
                    String lhVarName = vo.destName, rhVarName = vo.args.get(0);
                    if (!idLinks.containsKey(lhVarName)) {
                        idLinks.put(lhVarName, new HashSet<>());
                    }
                    HashSet<String> idLink = idLinks.get(lhVarName);
                    if (!idLink.contains(rhVarName))
                        idLink.add(rhVarName);
                }
            }
        }
        int localVarCounter = 0;
        for (Instruction ins : program.functions.get(0).instrs) {
            if (ins instanceof ConstBoolOperation) {
                String dest;
                ConstBoolOperation cbo = (ConstBoolOperation) ins;
                dest = cbo.dest;
                if (!varType.containsKey(dest)) {
                    varType.put(dest, Type.tBool);
                    varToLocalNo.put(dest, localVarCounter);
                    localVarCounter += 1;
                }
            } else if (ins instanceof ConstIntOperation) {
                String dest;
                ConstIntOperation cio = (ConstIntOperation) ins;
                dest = cio.dest;
                if (!varType.containsKey(dest)) {
                    varType.put(dest, Type.tInt);
                    varToLocalNo.put(dest, localVarCounter);
                    localVarCounter += 2;
                }
            } else if (ins instanceof ValueOperation) {
                ValueOperation vo = (ValueOperation) ins;
                if (vo.opName.matches("add|mul|sub|div")) {
                    String rstvarName = vo.destName;
                    if (!varType.containsKey(rstvarName)) {
                        varType.put(rstvarName, Type.tInt);
                        varToLocalNo.put(rstvarName, localVarCounter);
                        localVarCounter += 2;
                    }
                } else if (vo.opName.matches("eq|lt|gt|le|ge|not|and|or")) {
                    String rstvarName = vo.destName;
                    if (!varType.containsKey(rstvarName)) {
                        varType.put(rstvarName, Type.tBool);
                        varToLocalNo.put(rstvarName, localVarCounter);
                        localVarCounter += 1;
                    }
                }
            } else if (ins instanceof Label) {
                Label lbl = (Label) ins;
                labelHashMap.put(lbl.labelName, new org.objectweb.asm.Label());
            }
        }
        Queue<String> queue = new LinkedList<>();
        for (String varName : varType.keySet()) {
            queue.add(varName);
        }
        while (queue.size() > 0) {
            String cur = queue.remove();
            if (!idLinks.containsKey(cur)) continue;
            Type type = varType.get(cur);
            for (String linkVar : idLinks.get(cur)) {
                if (!varType.containsKey(linkVar)) {
                    varType.put(linkVar, type);
                    queue.add(linkVar);
                    varToLocalNo.put(linkVar, localVarCounter);
                    localVarCounter += type == Type.tInt ? 2 : 1;
                }
            }
        }
    }

    private org.objectweb.asm.Label getOrCreatLabel(HashMap<String, org.objectweb.asm.Label> map, String key) {
        if (!map.containsKey(key)) {
            map.put(key, new org.objectweb.asm.Label());
        }
        return map.get(key);
    }

    private int getOrCreatLocalVar(HashMap<String, Integer> map, String key) {
        return map.get(key);
    }
}