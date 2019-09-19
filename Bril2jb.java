import org.objectweb.asm.*;
import java.io.FileOutputStream;
import static org.objectweb.asm.Opcodes.*;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "bril2jb", version = "bril2jb 0.1.0", mixinStandardHelpOptions = true,
        description = "A tool to translate bril code to Java Bytecode")
public class Bril2jb implements Callable<Integer> {
    /*@Parameters(index = "0", description = "The source code file.")
    File inputFile;*/

    @Parameters(index = "0", description = "The output path")
    Path outputPath;

/*    @ArgGroup(exclusive = true, multiplicity = "1")
    FuncRequest funcRequest;

    static class FuncRequest {
        @Option(names = {"-t", "--typechecker"}, required = true, description = "Information flow typecheck: constraints as log")
        boolean typecheck;
        @Option(names = {"-p", "--parser"}, required = true, description = "Parse: ast json as log")
        boolean parse;
        @Option(names = {"-l", "--lexer"}, required = true, description = "Tokenize")
        boolean tokenize;
    }*/

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Bril2jb()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        File outputFile = outputPath.resolve("HelloGen.class").toFile();

        ClassWriter cw = new ClassWriter(0);

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "HelloGen", null, "java/lang/Object", null);

        //default constructor
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0); //load the first local variable: this
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        //main method
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"); //put System.out to operand stack
            mv.visitLdcInsn("Hello"); //load const "Hello" from const_pool, and put onto the operand stack
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        //save bytecode into disk
        FileOutputStream out = new FileOutputStream(outputFile);
        out.write(cw.toByteArray());
        out.close();
        return 0;
    }
/*        if (funcRequest.typecheck) {
            String outputFileName;
            File outputFile;
            if (outputFileNames == null) {
                outputFile = File.createTempFile("cons", "tmp");
                outputFileName = outputFile.getAbsolutePath();
                outputFile.deleteOnExit();
            } else {
                outputFileName = outputFileNames[0];
                outputFile = new File(outputFileName);
            }
            TypeChecker.typecheck(inputFile, outputFile);
            String classDirectoryPath = new File(Wyvern.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            String[] sherrlocResult = Utils.runSherrloc(classDirectoryPath, outputFileName);
            if (sherrlocResult[sherrlocResult.length - 1].contains(Utils.SHERRLOC_PASS_INDICATOR)) {
                System.out.println(Utils.TYPECHECK_PASS_MSG);
            } else {
                System.out.println(Utils.TYPECHECK_ERROR_MSG);
                for (int i = 0; i < sherrlocResult.length; ++i)
                    if (sherrlocResult[i].contains(Utils.SHERRLOC_ERROR_INDICATOR)) {
                        for (int j = i; j < sherrlocResult.length; ++j) {
                            System.out.println(sherrlocResult[j]);
                        }
                        break;
                    }
            }
        } else if (funcRequest.parse) {
            File astOutputFile = outputFileNames == null ? null : new File(outputFileNames[0]);
            Parser.parse(inputFile, astOutputFile);
        } else if (funcRequest.tokenize) {
            LexerTest.tokenize(inputFile);
        } else {
            logger.error("No funcRequest specified, this should never happen!");
            //System.out.println("No funcRequest specified, this should never happen!");
        }

        logger.trace("wyvern finishes");
        return 0;
    }*/

}