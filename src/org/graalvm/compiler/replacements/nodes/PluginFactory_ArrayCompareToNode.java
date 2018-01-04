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

public class PluginFactory_ArrayCompareToNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.nodes.ArrayCompareToNode
    //       method: compareTo(java.lang.Object,java.lang.Object,int,int,jdk.vm.ci.meta.JavaKind,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ArrayCompareToNode_compareTo extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            ValueNode arg3 = args[3];
            jdk.vm.ci.meta.JavaKind arg4;
            if (args[4].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[4].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.vm.ci.meta.JavaKind arg5;
            if (args[5].isConstant()) {
                arg5 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[5].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.replacements.nodes.ArrayCompareToNode node = new org.graalvm.compiler.replacements.nodes.ArrayCompareToNode(arg0, arg1, arg2, arg3, arg4, arg5);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private ArrayCompareToNode_compareTo(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ArrayCompareToNode_compareTo(injection), org.graalvm.compiler.replacements.nodes.ArrayCompareToNode.class, "compareTo", java.lang.Object.class, java.lang.Object.class, int.class, int.class, jdk.vm.ci.meta.JavaKind.class, jdk.vm.ci.meta.JavaKind.class);
    }
}
