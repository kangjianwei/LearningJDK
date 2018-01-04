// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: AMD64NodeMatchRules.java
package org.graalvm.compiler.core.amd64;

import java.util.*;
import org.graalvm.compiler.core.match.*;
import org.graalvm.compiler.core.gen.NodeMatchRules;
import org.graalvm.compiler.graph.Position;
import org.graalvm.compiler.nodes.memory.*;
import org.graalvm.compiler.nodes.*;
import org.graalvm.compiler.nodes.calc.*;
import org.graalvm.compiler.nodes.java.*;

public class AMD64NodeMatchRules_MatchStatementSet implements MatchStatementSet {

    private static final String[] ifCompareValueCas_arguments = new String[] {"root", "compare", "value", "cas"};
    private static final class MatchGenerator_ifCompareValueCas implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_ifCompareValueCas();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).ifCompareValueCas((IfNode) args[0], (CompareNode) args[1], (ValueNode) args[2], (ValueCompareAndSwapNode) args[3]);
        }
        @Override
        public String getName() {
             return "ifCompareValueCas";
        }
    }

    private static final String[] writeNarrow_arguments = new String[] {"root", "narrow"};
    private static final class MatchGenerator_writeNarrow implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_writeNarrow();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).writeNarrow((WriteNode) args[0], (NarrowNode) args[1]);
        }
        @Override
        public String getName() {
             return "writeNarrow";
        }
    }

    private static final String[] rotateRightVariable_arguments = new String[] {"value", "delta", "shiftAmount"};
    private static final class MatchGenerator_rotateRightVariable implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_rotateRightVariable();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).rotateRightVariable((ValueNode) args[0], (ConstantNode) args[1], (ValueNode) args[2]);
        }
        @Override
        public String getName() {
             return "rotateRightVariable";
        }
    }

    private static final String[] ifCompareMemory_arguments = new String[] {"root", "compare", "value", "access"};
    private static final class MatchGenerator_ifCompareMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_ifCompareMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).ifCompareMemory((IfNode) args[0], (CompareNode) args[1], (ValueNode) args[2], (LIRLowerableAccess) args[3]);
        }
        @Override
        public String getName() {
             return "ifCompareMemory";
        }
    }

    private static final String[] zeroExtend_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_zeroExtend implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_zeroExtend();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).zeroExtend((ZeroExtendNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "zeroExtend";
        }
    }

    private static final String[] signExtendNarrowRead_arguments = new String[] {"root", "narrow", "access"};
    private static final class MatchGenerator_signExtendNarrowRead implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_signExtendNarrowRead();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).signExtendNarrowRead((SignExtendNode) args[0], (NarrowNode) args[1], (LIRLowerableAccess) args[2]);
        }
        @Override
        public String getName() {
             return "signExtendNarrowRead";
        }
    }

    private static final String[] rotateLeftConstant_arguments = new String[] {"lshift", "rshift"};
    private static final class MatchGenerator_rotateLeftConstant implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_rotateLeftConstant();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).rotateLeftConstant((LeftShiftNode) args[0], (UnsignedRightShiftNode) args[1]);
        }
        @Override
        public String getName() {
             return "rotateLeftConstant";
        }
    }

    private static final String[] subMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_subMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_subMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).subMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "subMemory";
        }
    }

    private static final String[] mulMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_mulMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_mulMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).mulMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "mulMemory";
        }
    }

    private static final String[] reinterpret_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_reinterpret implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_reinterpret();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).reinterpret((ReinterpretNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "reinterpret";
        }
    }

    private static final String[] signExtend_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_signExtend implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_signExtend();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).signExtend((SignExtendNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "signExtend";
        }
    }

    private static final String[] orMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_orMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_orMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).orMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "orMemory";
        }
    }

    private static final String[] floatConvert_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_floatConvert implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_floatConvert();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).floatConvert((FloatConvertNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "floatConvert";
        }
    }

    private static final String[] narrowRead_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_narrowRead implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_narrowRead();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).narrowRead((NarrowNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "narrowRead";
        }
    }

    private static final String[] rotateLeftVariable_arguments = new String[] {"value", "shiftAmount", "delta"};
    private static final class MatchGenerator_rotateLeftVariable implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_rotateLeftVariable();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).rotateLeftVariable((ValueNode) args[0], (ValueNode) args[1], (ConstantNode) args[2]);
        }
        @Override
        public String getName() {
             return "rotateLeftVariable";
        }
    }

    private static final String[] addMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_addMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_addMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).addMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "addMemory";
        }
    }

    private static final String[] xorMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_xorMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_xorMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).xorMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "xorMemory";
        }
    }

    private static final String[] integerTestBranchMemory_arguments = new String[] {"root", "access", "value"};
    private static final class MatchGenerator_integerTestBranchMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_integerTestBranchMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).integerTestBranchMemory((IfNode) args[0], (LIRLowerableAccess) args[1], (ValueNode) args[2]);
        }
        @Override
        public String getName() {
             return "integerTestBranchMemory";
        }
    }

    private static final String[] ifCompareLogicCas_arguments = new String[] {"root", "compare", "value", "cas"};
    private static final class MatchGenerator_ifCompareLogicCas implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_ifCompareLogicCas();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).ifCompareLogicCas((IfNode) args[0], (CompareNode) args[1], (ValueNode) args[2], (LogicCompareAndSwapNode) args[3]);
        }
        @Override
        public String getName() {
             return "ifCompareLogicCas";
        }
    }

    private static final String[] ifLogicCas_arguments = new String[] {"root", "compare", "value", "access"};
    private static final class MatchGenerator_ifLogicCas implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_ifLogicCas();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).ifLogicCas((IfNode) args[0], (CompareNode) args[1], (ValueNode) args[2], (LIRLowerableAccess) args[3]);
        }
        @Override
        public String getName() {
             return "ifLogicCas";
        }
    }

    private static final String[] writeReinterpret_arguments = new String[] {"root", "reinterpret"};
    private static final class MatchGenerator_writeReinterpret implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_writeReinterpret();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).writeReinterpret((WriteNode) args[0], (ReinterpretNode) args[1]);
        }
        @Override
        public String getName() {
             return "writeReinterpret";
        }
    }

    private static final String[] andMemory_arguments = new String[] {"value", "access"};
    private static final class MatchGenerator_andMemory implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_andMemory();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((AMD64NodeMatchRules) nodeMatchRules).andMemory((ValueNode) args[0], (LIRLowerableAccess) args[1]);
        }
        @Override
        public String getName() {
             return "andMemory";
        }
    }

    @Override
    public Class<? extends NodeMatchRules> forClass() {
        return AMD64NodeMatchRules.class;
    }

    @Override
    public List<MatchStatement> statements() {
        // Checkstyle: stop 
        Position[] OrNode_positions = MatchRuleRegistry.findPositions(OrNode.TYPE, new String[]{"x", "y"});
        Position[] ZeroExtendNode_positions = MatchRuleRegistry.findPositions(ZeroExtendNode.TYPE, new String[]{"value"});
        Position[] PointerEqualsNode_positions = MatchRuleRegistry.findPositions(PointerEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] IfNode_positions = MatchRuleRegistry.findPositions(IfNode.TYPE, new String[]{"condition"});
        Position[] MulNode_positions = MatchRuleRegistry.findPositions(MulNode.TYPE, new String[]{"x", "y"});
        Position[] AddNode_positions = MatchRuleRegistry.findPositions(AddNode.TYPE, new String[]{"x", "y"});
        Position[] AndNode_positions = MatchRuleRegistry.findPositions(AndNode.TYPE, new String[]{"x", "y"});
        Position[] ReinterpretNode_positions = MatchRuleRegistry.findPositions(ReinterpretNode.TYPE, new String[]{"value"});
        Position[] LeftShiftNode_positions = MatchRuleRegistry.findPositions(LeftShiftNode.TYPE, new String[]{"x", "y"});
        Position[] WriteNode_positions = MatchRuleRegistry.findPositions(WriteNode.TYPE, new String[]{"address", "value"});
        Position[] IntegerLessThanNode_positions = MatchRuleRegistry.findPositions(IntegerLessThanNode.TYPE, new String[]{"x", "y"});
        Position[] IntegerBelowNode_positions = MatchRuleRegistry.findPositions(IntegerBelowNode.TYPE, new String[]{"x", "y"});
        Position[] FloatEqualsNode_positions = MatchRuleRegistry.findPositions(FloatEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] FloatConvertNode_positions = MatchRuleRegistry.findPositions(FloatConvertNode.TYPE, new String[]{"value"});
        Position[] FloatLessThanNode_positions = MatchRuleRegistry.findPositions(FloatLessThanNode.TYPE, new String[]{"x", "y"});
        Position[] ObjectEqualsNode_positions = MatchRuleRegistry.findPositions(ObjectEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] IntegerEqualsNode_positions = MatchRuleRegistry.findPositions(IntegerEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] UnsignedRightShiftNode_positions = MatchRuleRegistry.findPositions(UnsignedRightShiftNode.TYPE, new String[]{"x", "y"});
        Position[] SignExtendNode_positions = MatchRuleRegistry.findPositions(SignExtendNode.TYPE, new String[]{"value"});
        Position[] IntegerTestNode_positions = MatchRuleRegistry.findPositions(IntegerTestNode.TYPE, new String[]{"x", "y"});
        Position[] NarrowNode_positions = MatchRuleRegistry.findPositions(NarrowNode.TYPE, new String[]{"value"});
        Position[] SubNode_positions = MatchRuleRegistry.findPositions(SubNode.TYPE, new String[]{"x", "y"});
        Position[] XorNode_positions = MatchRuleRegistry.findPositions(XorNode.TYPE, new String[]{"x", "y"});

        List<MatchStatement> statements = Collections.unmodifiableList(Arrays.asList(
            new MatchStatement("ifLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifLogicCas.instance, ifLogicCas_arguments),
            new MatchStatement("ifLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifLogicCas.instance, ifLogicCas_arguments),
            new MatchStatement("rotateLeftConstant", new MatchPattern(OrNode.class, null, new MatchPattern(LeftShiftNode.class, "lshift", new MatchPattern("value", false), new MatchPattern(ConstantNode.class, null, false), LeftShiftNode_positions, true), new MatchPattern(UnsignedRightShiftNode.class, "rshift", new MatchPattern("value", false), new MatchPattern(ConstantNode.class, null, false), UnsignedRightShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateLeftConstant.instance, rotateLeftConstant_arguments),
            new MatchStatement("rotateLeftConstant", new MatchPattern(OrNode.class, null, new MatchPattern(UnsignedRightShiftNode.class, "rshift", new MatchPattern("value", false), new MatchPattern(ConstantNode.class, null, false), UnsignedRightShiftNode_positions, true), new MatchPattern(LeftShiftNode.class, "lshift", new MatchPattern("value", false), new MatchPattern(ConstantNode.class, null, false), LeftShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateLeftConstant.instance, rotateLeftConstant_arguments),
            new MatchStatement("rotateRightVariable", new MatchPattern(OrNode.class, null, new MatchPattern(LeftShiftNode.class, null, new MatchPattern("value", false), new MatchPattern(SubNode.class, null, new MatchPattern(ConstantNode.class, "delta", false), new MatchPattern("shiftAmount", false), SubNode_positions, true), LeftShiftNode_positions, true), new MatchPattern(UnsignedRightShiftNode.class, null, new MatchPattern("value", false), new MatchPattern("shiftAmount", false), UnsignedRightShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateRightVariable.instance, rotateRightVariable_arguments),
            new MatchStatement("rotateRightVariable", new MatchPattern(OrNode.class, null, new MatchPattern(UnsignedRightShiftNode.class, null, new MatchPattern("value", false), new MatchPattern("shiftAmount", false), UnsignedRightShiftNode_positions, true), new MatchPattern(LeftShiftNode.class, null, new MatchPattern("value", false), new MatchPattern(SubNode.class, null, new MatchPattern(ConstantNode.class, "delta", false), new MatchPattern("shiftAmount", false), SubNode_positions, true), LeftShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateRightVariable.instance, rotateRightVariable_arguments),
            new MatchStatement("rotateLeftVariable", new MatchPattern(OrNode.class, null, new MatchPattern(LeftShiftNode.class, null, new MatchPattern("value", false), new MatchPattern("shiftAmount", false), LeftShiftNode_positions, true), new MatchPattern(UnsignedRightShiftNode.class, null, new MatchPattern("value", false), new MatchPattern(SubNode.class, null, new MatchPattern(ConstantNode.class, "delta", false), new MatchPattern("shiftAmount", false), SubNode_positions, true), UnsignedRightShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateLeftVariable.instance, rotateLeftVariable_arguments),
            new MatchStatement("rotateLeftVariable", new MatchPattern(OrNode.class, null, new MatchPattern(UnsignedRightShiftNode.class, null, new MatchPattern("value", false), new MatchPattern(SubNode.class, null, new MatchPattern(ConstantNode.class, "delta", false), new MatchPattern("shiftAmount", false), SubNode_positions, true), UnsignedRightShiftNode_positions, true), new MatchPattern(LeftShiftNode.class, null, new MatchPattern("value", false), new MatchPattern("shiftAmount", false), LeftShiftNode_positions, true), OrNode_positions, true), MatchGenerator_rotateLeftVariable.instance, rotateLeftVariable_arguments),
            new MatchStatement("writeNarrow", new MatchPattern(WriteNode.class, null, new MatchPattern("object", false), new MatchPattern(NarrowNode.class, "narrow", true), WriteNode_positions, true), MatchGenerator_writeNarrow.instance, writeNarrow_arguments),
            new MatchStatement("writeReinterpret", new MatchPattern(WriteNode.class, null, new MatchPattern("object", false), new MatchPattern(ReinterpretNode.class, "reinterpret", true), WriteNode_positions, true), MatchGenerator_writeReinterpret.instance, writeReinterpret_arguments),
            new MatchStatement("integerTestBranchMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerTestNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), IntegerTestNode_positions, true), IfNode_positions, true), MatchGenerator_integerTestBranchMemory.instance, integerTestBranchMemory_arguments),
            new MatchStatement("integerTestBranchMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerTestNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), IntegerTestNode_positions, true), IfNode_positions, true), MatchGenerator_integerTestBranchMemory.instance, integerTestBranchMemory_arguments),
            new MatchStatement("integerTestBranchMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerTestNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), IntegerTestNode_positions, true), IfNode_positions, true), MatchGenerator_integerTestBranchMemory.instance, integerTestBranchMemory_arguments),
            new MatchStatement("integerTestBranchMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerTestNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), IntegerTestNode_positions, true), IfNode_positions, true), MatchGenerator_integerTestBranchMemory.instance, integerTestBranchMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerLessThanNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), IntegerLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerLessThanNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), IntegerLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerBelowNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), IntegerBelowNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerBelowNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), IntegerBelowNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerLessThanNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), IntegerLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerLessThanNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), IntegerLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerBelowNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), IntegerBelowNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerBelowNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), IntegerBelowNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatLessThanNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), FloatLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatLessThanNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), FloatLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatLessThanNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), FloatLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(FloatLessThanNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), FloatLessThanNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareMemory", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareMemory.instance, ifCompareMemory_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareValueCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern(ValueCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareValueCas.instance, ifCompareValueCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("addMemory", new MatchPattern(AddNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), AddNode_positions, true), MatchGenerator_addMemory.instance, addMemory_arguments),
            new MatchStatement("addMemory", new MatchPattern(AddNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), AddNode_positions, true), MatchGenerator_addMemory.instance, addMemory_arguments),
            new MatchStatement("addMemory", new MatchPattern(AddNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), AddNode_positions, true), MatchGenerator_addMemory.instance, addMemory_arguments),
            new MatchStatement("addMemory", new MatchPattern(AddNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), AddNode_positions, true), MatchGenerator_addMemory.instance, addMemory_arguments),
            new MatchStatement("subMemory", new MatchPattern(SubNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), SubNode_positions, true), MatchGenerator_subMemory.instance, subMemory_arguments),
            new MatchStatement("subMemory", new MatchPattern(SubNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), SubNode_positions, true), MatchGenerator_subMemory.instance, subMemory_arguments),
            new MatchStatement("mulMemory", new MatchPattern(MulNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), MulNode_positions, true), MatchGenerator_mulMemory.instance, mulMemory_arguments),
            new MatchStatement("mulMemory", new MatchPattern(MulNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), MulNode_positions, true), MatchGenerator_mulMemory.instance, mulMemory_arguments),
            new MatchStatement("mulMemory", new MatchPattern(MulNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), MulNode_positions, true), MatchGenerator_mulMemory.instance, mulMemory_arguments),
            new MatchStatement("mulMemory", new MatchPattern(MulNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), MulNode_positions, true), MatchGenerator_mulMemory.instance, mulMemory_arguments),
            new MatchStatement("andMemory", new MatchPattern(AndNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), AndNode_positions, true), MatchGenerator_andMemory.instance, andMemory_arguments),
            new MatchStatement("andMemory", new MatchPattern(AndNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), AndNode_positions, true), MatchGenerator_andMemory.instance, andMemory_arguments),
            new MatchStatement("andMemory", new MatchPattern(AndNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), AndNode_positions, true), MatchGenerator_andMemory.instance, andMemory_arguments),
            new MatchStatement("andMemory", new MatchPattern(AndNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), AndNode_positions, true), MatchGenerator_andMemory.instance, andMemory_arguments),
            new MatchStatement("orMemory", new MatchPattern(OrNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), OrNode_positions, true), MatchGenerator_orMemory.instance, orMemory_arguments),
            new MatchStatement("orMemory", new MatchPattern(OrNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), OrNode_positions, true), MatchGenerator_orMemory.instance, orMemory_arguments),
            new MatchStatement("orMemory", new MatchPattern(OrNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), OrNode_positions, true), MatchGenerator_orMemory.instance, orMemory_arguments),
            new MatchStatement("orMemory", new MatchPattern(OrNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), OrNode_positions, true), MatchGenerator_orMemory.instance, orMemory_arguments),
            new MatchStatement("xorMemory", new MatchPattern(XorNode.class, null, new MatchPattern("value", false), new MatchPattern(ReadNode.class, "access", true), XorNode_positions, true), MatchGenerator_xorMemory.instance, xorMemory_arguments),
            new MatchStatement("xorMemory", new MatchPattern(XorNode.class, null, new MatchPattern(ReadNode.class, "access", true), new MatchPattern("value", false), XorNode_positions, true), MatchGenerator_xorMemory.instance, xorMemory_arguments),
            new MatchStatement("xorMemory", new MatchPattern(XorNode.class, null, new MatchPattern("value", false), new MatchPattern(FloatingReadNode.class, "access", true), XorNode_positions, true), MatchGenerator_xorMemory.instance, xorMemory_arguments),
            new MatchStatement("xorMemory", new MatchPattern(XorNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), new MatchPattern("value", false), XorNode_positions, true), MatchGenerator_xorMemory.instance, xorMemory_arguments),
            new MatchStatement("signExtend", new MatchPattern(SignExtendNode.class, null, new MatchPattern(ReadNode.class, "access", true), SignExtendNode_positions, true), MatchGenerator_signExtend.instance, signExtend_arguments),
            new MatchStatement("signExtend", new MatchPattern(SignExtendNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), SignExtendNode_positions, true), MatchGenerator_signExtend.instance, signExtend_arguments),
            new MatchStatement("zeroExtend", new MatchPattern(ZeroExtendNode.class, null, new MatchPattern(ReadNode.class, "access", true), ZeroExtendNode_positions, true), MatchGenerator_zeroExtend.instance, zeroExtend_arguments),
            new MatchStatement("zeroExtend", new MatchPattern(ZeroExtendNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), ZeroExtendNode_positions, true), MatchGenerator_zeroExtend.instance, zeroExtend_arguments),
            new MatchStatement("narrowRead", new MatchPattern(NarrowNode.class, null, new MatchPattern(ReadNode.class, "access", true), NarrowNode_positions, true), MatchGenerator_narrowRead.instance, narrowRead_arguments),
            new MatchStatement("narrowRead", new MatchPattern(NarrowNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), NarrowNode_positions, true), MatchGenerator_narrowRead.instance, narrowRead_arguments),
            new MatchStatement("signExtendNarrowRead", new MatchPattern(SignExtendNode.class, null, new MatchPattern(NarrowNode.class, "narrow", new MatchPattern(ReadNode.class, "access", true), NarrowNode_positions, true), SignExtendNode_positions, true), MatchGenerator_signExtendNarrowRead.instance, signExtendNarrowRead_arguments),
            new MatchStatement("signExtendNarrowRead", new MatchPattern(SignExtendNode.class, null, new MatchPattern(NarrowNode.class, "narrow", new MatchPattern(FloatingReadNode.class, "access", true), NarrowNode_positions, true), SignExtendNode_positions, true), MatchGenerator_signExtendNarrowRead.instance, signExtendNarrowRead_arguments),
            new MatchStatement("floatConvert", new MatchPattern(FloatConvertNode.class, null, new MatchPattern(ReadNode.class, "access", true), FloatConvertNode_positions, true), MatchGenerator_floatConvert.instance, floatConvert_arguments),
            new MatchStatement("floatConvert", new MatchPattern(FloatConvertNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), FloatConvertNode_positions, true), MatchGenerator_floatConvert.instance, floatConvert_arguments),
            new MatchStatement("reinterpret", new MatchPattern(ReinterpretNode.class, null, new MatchPattern(ReadNode.class, "access", true), ReinterpretNode_positions, true), MatchGenerator_reinterpret.instance, reinterpret_arguments),
            new MatchStatement("reinterpret", new MatchPattern(ReinterpretNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), ReinterpretNode_positions, true), MatchGenerator_reinterpret.instance, reinterpret_arguments)
        ));
        // Checkstyle: resume
        return statements;
    }

}
