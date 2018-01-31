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

public class PluginFactory_ArrayEqualsNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.nodes.ArrayEqualsNode
    //       method: equals(java.lang.Object,java.lang.Object,int,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ArrayEqualsNode_equals extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            jdk.vm.ci.meta.JavaKind arg3;
            if (args[3].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.replacements.nodes.ArrayEqualsNode node = new org.graalvm.compiler.replacements.nodes.ArrayEqualsNode(arg0, arg1, arg2, arg3);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private ArrayEqualsNode_equals(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ArrayEqualsNode_equals(injection), org.graalvm.compiler.replacements.nodes.ArrayEqualsNode.class, "equals", java.lang.Object.class, java.lang.Object.class, int.class, jdk.vm.ci.meta.JavaKind.class);
    }
}
