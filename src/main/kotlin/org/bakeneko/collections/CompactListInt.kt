package org.bakeneko.collections

import org.bakeneko.collections.CompactListIntLoader.findClass
import org.bakeneko.collections.CompactListIntLoader.loadCompactList
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

/**
 * [CompactListIntLoader] overrides the [findClass] method to load the generated [CompactList] implementation.
 *
 * To get the generated [CompactListInt] class, use the [loadCompactList] method.
 * */
object CompactListIntLoader : ClassLoader() {
    private const val expectedName = "org.bakeneko.collections.CompactListInt"

    private val classGenerated = AtomicBoolean(false)
    private val loaderLock = ReentrantLock()
    private val clazz: AtomicReference<Class<*>?> = AtomicReference(null)

    /**
     * [loadCompactList] returns the generated [CompactList] implementation specializing in storing [Int] elements.
     * */
    fun loadCompactList(): Class<*> {
        return findClass(expectedName)
    }

    override fun findClass(name: String?): Class<*> {
        if (name != expectedName) {
            throw IllegalArgumentException("CompactListIntLoader only supports loading org.bakeneko.collections.CompactListInt")
        }

        if (!classGenerated.get()) {
            loaderLock.lock()
            try {
                if (!classGenerated.get()) {
                    val bytecode = generateCompactListInt()
                    classGenerated.set(true)
                    clazz.set(defineClass(expectedName, bytecode, 0, bytecode.size))
                }
            } finally {
                loaderLock.unlock()
            }
        }
        return clazz.get()!!
    }
}

/**
 * [generateCompactListInt] generates the byte code of an implementation of [CompactList], which is a dynamic
 * array storing primitive [Int] values.
 * The generated implementation is more compact than a [CompactListGeneric] that stores [Int] values, since
 * the [Int]s are not stored in boxed form.
 * */
private fun generateCompactListInt(): ByteArray {
    val classWriter = ClassWriter(0)

    classWriter.initializeClass()
    classWriter.generateFields()
    classWriter.generateConstructor()
    classWriter.generateGetSizeMethod()
    classWriter.generateAddMethod() // The 'add' method handles primitive values
    classWriter.generateAddMethodBridge() // Bridge method handles boxed int values and delegates to 'add'
    classWriter.generateGetMethod() // Returns a primitive value
    classWriter.generateGetMethodBridge() // Returns a boxed value
    classWriter.generateGrowArrayMethod()

    classWriter.visitEnd()

    return classWriter.toByteArray()
}

private fun ClassWriter.initializeClass() {
    // Initialize class
    visit(
        Opcodes.V1_8,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
        "org/bakeneko/collections/CompactListInt",
        "Ljava/lang/Object;Lorg/bakeneko/collections/CompactList<Ljava/lang/Integer;>;",
        "java/lang/Object",
        arrayOf("org/bakeneko/collections/CompactList")
    )
    // There is no source file, so just call with 'null' to follow protocol
    visitSource(null, null)
}

private fun ClassWriter.generateFields() {
    // Initialize class fields
    visitField(Opcodes.ACC_PRIVATE, "lastElementIndex", "I", null, null)
        .visitEnd()
    visitField(Opcodes.ACC_PRIVATE, "elements", "[I", null, null)
        .visitEnd()
}

private fun ClassWriter.generateConstructor() {
    val methodVisitor = visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null)
    methodVisitor.visitCode() // Mark the start of method implementation

    val constructorStartLabel = Label()
    methodVisitor.visitLabel(constructorStartLabel)
    methodVisitor.visitLineNumber(3, constructorStartLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

    val initLastElementIndexLabel = Label()
    methodVisitor.visitLabel(initLastElementIndexLabel)
    methodVisitor.visitLineNumber(4, initLastElementIndexLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
    methodVisitor.visitInsn(Opcodes.ICONST_M1) // Put -1 onto the operand stack
    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, "org/bakeneko/collections/CompactListInt", "lastElementIndex", "I")

    val verifyInitialCapacityLabel = Label()
    methodVisitor.visitLabel(verifyInitialCapacityLabel)
    methodVisitor.visitLineNumber(11, verifyInitialCapacityLabel)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1) // Load local variable 'initialCapacity'
    methodVisitor.visitInsn(Opcodes.ICONST_1)

    val ifStatementLabel = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, ifStatementLabel)

    val invalidInitialCapacityLabel = Label()
    methodVisitor.visitLabel(invalidInitialCapacityLabel)
    methodVisitor.visitLineNumber(12, invalidInitialCapacityLabel)
    methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException")
    methodVisitor.visitInsn(Opcodes.DUP)
    methodVisitor.visitLdcInsn("Invalid initialCapacity: expected to be in [1, Integer.MAX_VALUE].")
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        "java/lang/IllegalArgumentException",
        "<init>",
        "(Ljava/lang/String;)V",
        false
    )
    methodVisitor.visitInsn(Opcodes.ATHROW)
    methodVisitor.visitLabel(ifStatementLabel)

    // End of if statement

    // Initialize array
    methodVisitor.visitLineNumber(14, ifStatementLabel)
    methodVisitor.visitFrame( // ASM frame data
        Opcodes.F_FULL,
        2,
        arrayOf<Any>("org/bakeneko/collections/CompactListInt", Opcodes.INTEGER),
        0,
        arrayOf<Any>()
    )
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")

    val returnStatementLabel = Label()
    methodVisitor.visitLabel(returnStatementLabel)
    methodVisitor.visitLineNumber(3, returnStatementLabel)
    methodVisitor.visitInsn(Opcodes.RETURN)

    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        constructorStartLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitLocalVariable("initialCapacity", "I", null, constructorStartLabel, localVariablesLabel, 1)
    methodVisitor.visitMaxs(3, 2)
    methodVisitor.visitEnd()
}

private fun ClassWriter.generateGetSizeMethod() {
    val methodVisitor = visitMethod(Opcodes.ACC_PUBLIC, "getSize", "()I", null, null)
    methodVisitor.visitCode()

    val functionBodyLabel = Label()
    methodVisitor.visitLabel(functionBodyLabel)
    methodVisitor.visitLineNumber(8, functionBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(
        Opcodes.GETFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )
    methodVisitor.visitInsn(Opcodes.ICONST_1)
    methodVisitor.visitInsn(Opcodes.IADD)
    methodVisitor.visitInsn(Opcodes.IRETURN)

    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        functionBodyLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitMaxs(2, 1)
    methodVisitor.visitEnd()

}

private fun ClassWriter.generateAddMethod() {
    val methodVisitor = visitMethod(Opcodes.ACC_PUBLIC, "add", "(I)V", null, null)
    methodVisitor.visitCode()

    val functionBodyLabel = Label()
    methodVisitor.visitLabel(functionBodyLabel)
    methodVisitor.visitLineNumber(18, functionBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "org/bakeneko/collections/CompactListInt",
        "getSize",
        "()I",
        false
    )

    val ifStatementLabel = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, ifStatementLabel)

    val ifBodyLabel = Label()
    methodVisitor.visitLabel(ifBodyLabel)
    methodVisitor.visitLineNumber(19, ifBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        "org/bakeneko/collections/CompactListInt",
        "growArray",
        "()V",
        false
    )
    methodVisitor.visitLabel(ifStatementLabel)
    methodVisitor.visitLineNumber(22, ifStatementLabel)
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(
        Opcodes.GETFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )
    methodVisitor.visitVarInsn(Opcodes.ISTORE, 2)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 2)
    methodVisitor.visitInsn(Opcodes.ICONST_1)
    methodVisitor.visitInsn(Opcodes.IADD)
    methodVisitor.visitFieldInsn(
        Opcodes.PUTFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )

    val saveValueLabel = Label()
    methodVisitor.visitLabel(saveValueLabel)
    methodVisitor.visitLineNumber(23, saveValueLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(
        Opcodes.GETFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitInsn(Opcodes.IASTORE)

    val returnStatementLabel = Label()
    methodVisitor.visitLabel(returnStatementLabel)
    methodVisitor.visitLineNumber(24, returnStatementLabel)
    methodVisitor.visitInsn(Opcodes.RETURN)

    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        functionBodyLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitLocalVariable("value", "I", null, functionBodyLabel, localVariablesLabel, 1)
    methodVisitor.visitMaxs(3, 3)
    methodVisitor.visitEnd()
}

private fun ClassWriter.generateAddMethodBridge() {
    // handle boxed values
    val methodVisitor = visitMethod(
        Opcodes.ACC_PUBLIC or Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC,
        "add",
        "(Ljava/lang/Object;)V",
        null,
        null
    )
    methodVisitor.visitCode()
    val functionBodyLabel = Label()
    methodVisitor.visitLabel(functionBodyLabel)
    methodVisitor.visitLineNumber(3, functionBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1)
    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number")
    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false)
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "org/bakeneko/collections/CompactListInt",
        "add",
        "(I)V",
        false
    )
    methodVisitor.visitInsn(Opcodes.RETURN)
    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        functionBodyLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitLocalVariable("value", "Ljava/lang/Object;", null, functionBodyLabel, localVariablesLabel, 1)
    methodVisitor.visitMaxs(2, 2)
    methodVisitor.visitEnd()
}

private fun ClassWriter.generateGetMethod() {
    val methodVisitor = visitMethod(Opcodes.ACC_PUBLIC, "get", "(I)Ljava/lang/Integer;", null, null)
    methodVisitor.visitCode()

    val functionBodyLabel = Label()
    methodVisitor.visitLabel(functionBodyLabel)
    methodVisitor.visitLineNumber(28, functionBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)

    val ifConditionLabel = Label()
    methodVisitor.visitJumpInsn(Opcodes.IFLT, ifConditionLabel)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(
        Opcodes.GETFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )
    val ifBodyLabel = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, ifBodyLabel)
    methodVisitor.visitLabel(ifConditionLabel)
    methodVisitor.visitLineNumber(29, ifConditionLabel)
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
    methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/IndexOutOfBoundsException")
    methodVisitor.visitInsn(Opcodes.DUP)
    methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
    methodVisitor.visitInsn(Opcodes.DUP)
    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
    methodVisitor.visitLdcInsn("Invalid index: expected [0, ")
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
        false
    )
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(
        Opcodes.GETFIELD,
        "org/bakeneko/collections/CompactListInt",
        "lastElementIndex",
        "I"
    )
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(I)Ljava/lang/StringBuilder;",
        false
    )
    methodVisitor.visitLdcInsn("].")
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
        false
    )
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "toString",
        "()Ljava/lang/String;",
        false
    )
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        "java/lang/IndexOutOfBoundsException",
        "<init>",
        "(Ljava/lang/String;)V",
        false
    )
    methodVisitor.visitInsn(Opcodes.ATHROW)
    methodVisitor.visitLabel(ifBodyLabel)
    methodVisitor.visitLineNumber(32, ifBodyLabel)
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitInsn(Opcodes.IALOAD)
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        "java/lang/Integer",
        "valueOf",
        "(I)Ljava/lang/Integer;",
        false
    )
    methodVisitor.visitInsn(Opcodes.ARETURN)

    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        functionBodyLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitLocalVariable("index", "I", null, functionBodyLabel, localVariablesLabel, 1)
    methodVisitor.visitMaxs(4, 2)
    methodVisitor.visitEnd()
}

private fun ClassWriter.generateGetMethodBridge() {
    val methodVisitor = visitMethod(
        Opcodes.ACC_PUBLIC or Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC,
        "get",
        "(I)Ljava/lang/Object;",
        null,
        null
    )
    methodVisitor.visitCode()
    val functionBodyLabel = Label()
    methodVisitor.visitLabel(functionBodyLabel)
    methodVisitor.visitLineNumber(3, functionBodyLabel)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "org/bakeneko/collections/CompactListInt",
        "get",
        "(I)Ljava/lang/Integer;",
        false
    )
    methodVisitor.visitInsn(Opcodes.ARETURN)
    val localVariablesLabel = Label()
    methodVisitor.visitLabel(localVariablesLabel)
    methodVisitor.visitLocalVariable(
        "this",
        "Lorg/bakeneko/collections/CompactListInt;",
        null,
        functionBodyLabel,
        localVariablesLabel,
        0
    )
    methodVisitor.visitLocalVariable("index", "I", null, functionBodyLabel, localVariablesLabel, 1)
    methodVisitor.visitMaxs(2, 2)
    methodVisitor.visitEnd()
}

private fun ClassWriter.generateGrowArrayMethod() {
    val methodVisitor = visitMethod(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, "growArray", "()V", null, null)
    methodVisitor.visitCode()

    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(36, label0)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)
    methodVisitor.visitLdcInsn(2147483647)

    val label1 = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, label1)

    val label2 = Label()
    methodVisitor.visitLabel(label2)
    methodVisitor.visitLineNumber(37, label2)
    methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException")
    methodVisitor.visitInsn(Opcodes.DUP)
    methodVisitor.visitLdcInsn("CompactList reached its maximum size and cannot grow any more.")
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        "java/lang/IllegalStateException",
        "<init>",
        "(Ljava/lang/String;)V",
        false
    )
    methodVisitor.visitInsn(Opcodes.ATHROW)
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLineNumber(40, label1)
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)
    methodVisitor.visitInsn(Opcodes.ICONST_2)
    methodVisitor.visitInsn(Opcodes.IMUL)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)

    val label3 = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, label3)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)
    methodVisitor.visitInsn(Opcodes.ICONST_2)
    methodVisitor.visitInsn(Opcodes.IMUL)

    val label4 = Label()
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label4)
    methodVisitor.visitLabel(label3)
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
    methodVisitor.visitLdcInsn(2147483647)
    methodVisitor.visitLabel(label4)
    methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf<Any>(Opcodes.INTEGER))
    methodVisitor.visitVarInsn(Opcodes.ISTORE, 1)

    val label5 = Label()
    methodVisitor.visitLabel(label5)
    methodVisitor.visitLineNumber(42, label5)
    methodVisitor.visitInsn(Opcodes.ICONST_0)
    methodVisitor.visitVarInsn(Opcodes.ISTORE, 3)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
    methodVisitor.visitVarInsn(Opcodes.ASTORE, 4)

    val label6 = Label()
    methodVisitor.visitLabel(label6)
    methodVisitor.visitFrame(
        Opcodes.F_FULL,
        5,
        arrayOf<Any>(
            "org/bakeneko/collections/CompactListInt",
            Opcodes.INTEGER,
            Opcodes.TOP,
            Opcodes.INTEGER,
            "[I"
        ),
        0,
        arrayOf<Any>()
    )
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 3)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)

    val label7 = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, label7)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 3)
    methodVisitor.visitVarInsn(Opcodes.ISTORE, 5)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 4)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 5)
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 5)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)

    val label8 = Label()
    methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, label8)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 5)
    methodVisitor.visitInsn(Opcodes.IALOAD)

    val label9 = Label()
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label9)
    methodVisitor.visitLabel(label8)
    methodVisitor.visitFrame(
        Opcodes.F_FULL,
        6,
        arrayOf<Any>(
            "org/bakeneko/collections/CompactListInt",
            Opcodes.INTEGER,
            Opcodes.TOP,
            Opcodes.INTEGER,
            "[I",
            Opcodes.INTEGER
        ),
        2,
        arrayOf<Any>("[I", Opcodes.INTEGER)
    )
    methodVisitor.visitInsn(Opcodes.ICONST_0)
    methodVisitor.visitLabel(label9)
    methodVisitor.visitFrame(
        Opcodes.F_FULL,
        6,
        arrayOf<Any>(
            "org/bakeneko/collections/CompactListInt",
            Opcodes.INTEGER,
            Opcodes.TOP,
            Opcodes.INTEGER,
            "[I",
            Opcodes.INTEGER
        ),
        3,
        arrayOf<Any>("[I", Opcodes.INTEGER, Opcodes.INTEGER)
    )
    methodVisitor.visitInsn(Opcodes.IASTORE)
    methodVisitor.visitIincInsn(3, 1)
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label6)
    methodVisitor.visitLabel(label7)
    methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 4)
    methodVisitor.visitVarInsn(Opcodes.ASTORE, 2)

    val label10 = Label()
    methodVisitor.visitLabel(label10)
    methodVisitor.visitLineNumber(43, label10)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 2)
    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, "org/bakeneko/collections/CompactListInt", "elements", "[I")

    val label11 = Label()
    methodVisitor.visitLabel(label11)
    methodVisitor.visitLineNumber(44, label11)
    methodVisitor.visitInsn(Opcodes.RETURN)

    val label12 = Label()
    methodVisitor.visitLabel(label12)
    methodVisitor.visitLocalVariable("newSize", "I", null, label5, label12, 1)
    methodVisitor.visitLocalVariable("newElements", "[I", null, label10, label12, 2)
    methodVisitor.visitLocalVariable("this", "Lorg/bakeneko/collections/CompactListInt;", null, label0, label12, 0)
    methodVisitor.visitMaxs(4, 6)
    methodVisitor.visitEnd()
}