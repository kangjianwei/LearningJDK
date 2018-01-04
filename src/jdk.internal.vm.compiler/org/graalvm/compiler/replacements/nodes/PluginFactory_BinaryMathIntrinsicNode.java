// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_BinaryMathIntrinsicNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode
    //       method: compute(double,double,org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class BinaryMathIntrinsicNode_compute extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation arg2;
            if (args[2].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation.class, args[2].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode node = new org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode(arg0, arg1, arg2);
            b.addPush(JavaKind.Double, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private BinaryMathIntrinsicNode_compute(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new BinaryMathIntrinsicNode_compute(injection), org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.class, "compute", double.class, double.class, org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation.class);
    }
}
