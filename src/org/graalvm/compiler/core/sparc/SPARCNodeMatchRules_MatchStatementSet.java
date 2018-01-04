// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: SPARCNodeMatchRules.java
package org.graalvm.compiler.core.sparc;

import java.util.*;
import org.graalvm.compiler.core.match.*;
import org.graalvm.compiler.core.gen.NodeMatchRules;
import org.graalvm.compiler.graph.Position;
import org.graalvm.compiler.nodes.memory.*;
import org.graalvm.compiler.nodes.*;
import org.graalvm.compiler.nodes.calc.*;
import org.graalvm.compiler.nodes.java.*;

public class SPARCNodeMatchRules_MatchStatementSet implements MatchStatementSet {

    private static final String[] signExtend_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_signExtend implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_signExtend();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((SPARCNodeMatchRules) nodeMatchRules).signExtend((SignExtendNode) args[0], (Access) args[1]);
        }
        @Override
        public String getName() {
             return "signExtend";
        }
    }

    private static final String[] ifCompareLogicCas_arguments = new String[] {"root", "compare", "value", "cas"};
    private static final class MatchGenerator_ifCompareLogicCas implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_ifCompareLogicCas();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((SPARCNodeMatchRules) nodeMatchRules).ifCompareLogicCas((IfNode) args[0], (CompareNode) args[1], (ValueNode) args[2], (LogicCompareAndSwapNode) args[3]);
        }
        @Override
        public String getName() {
             return "ifCompareLogicCas";
        }
    }

    private static final String[] zeroExtend_arguments = new String[] {"root", "access"};
    private static final class MatchGenerator_zeroExtend implements MatchGenerator {
        static MatchGenerator instance = new MatchGenerator_zeroExtend();
        @Override
        public ComplexMatchResult match(NodeMatchRules nodeMatchRules, Object...args) {
            return ((SPARCNodeMatchRules) nodeMatchRules).zeroExtend((ZeroExtendNode) args[0], (Access) args[1]);
        }
        @Override
        public String getName() {
             return "zeroExtend";
        }
    }

    @Override
    public Class<? extends NodeMatchRules> forClass() {
        return SPARCNodeMatchRules.class;
    }

    @Override
    public List<MatchStatement> statements() {
        // Checkstyle: stop 
        Position[] ZeroExtendNode_positions = MatchRuleRegistry.findPositions(ZeroExtendNode.TYPE, new String[]{"value"});
        Position[] FloatEqualsNode_positions = MatchRuleRegistry.findPositions(FloatEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] PointerEqualsNode_positions = MatchRuleRegistry.findPositions(PointerEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] IfNode_positions = MatchRuleRegistry.findPositions(IfNode.TYPE, new String[]{"condition"});
        Position[] ObjectEqualsNode_positions = MatchRuleRegistry.findPositions(ObjectEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] IntegerEqualsNode_positions = MatchRuleRegistry.findPositions(IntegerEqualsNode.TYPE, new String[]{"x", "y"});
        Position[] SignExtendNode_positions = MatchRuleRegistry.findPositions(SignExtendNode.TYPE, new String[]{"value"});

        List<MatchStatement> statements = Collections.unmodifiableList(Arrays.asList(
            new MatchStatement("signExtend", new MatchPattern(SignExtendNode.class, null, new MatchPattern(ReadNode.class, "access", true), SignExtendNode_positions, true), MatchGenerator_signExtend.instance, signExtend_arguments),
            new MatchStatement("signExtend", new MatchPattern(SignExtendNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), SignExtendNode_positions, true), MatchGenerator_signExtend.instance, signExtend_arguments),
            new MatchStatement("zeroExtend", new MatchPattern(ZeroExtendNode.class, null, new MatchPattern(ReadNode.class, "access", true), ZeroExtendNode_positions, true), MatchGenerator_zeroExtend.instance, zeroExtend_arguments),
            new MatchStatement("zeroExtend", new MatchPattern(ZeroExtendNode.class, null, new MatchPattern(FloatingReadNode.class, "access", true), ZeroExtendNode_positions, true), MatchGenerator_zeroExtend.instance, zeroExtend_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(ObjectEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), ObjectEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(PointerEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), PointerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(FloatEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), FloatEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern("value", false), new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments),
            new MatchStatement("ifCompareLogicCas", new MatchPattern(IfNode.class, null, new MatchPattern(IntegerEqualsNode.class, "compare", new MatchPattern(LogicCompareAndSwapNode.class, "cas", true), new MatchPattern("value", false), IntegerEqualsNode_positions, true), IfNode_positions, true), MatchGenerator_ifCompareLogicCas.instance, ifCompareLogicCas_arguments)
        ));
        // Checkstyle: resume
        return statements;
    }

}
