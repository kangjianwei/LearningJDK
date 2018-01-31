// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.nodes.debug;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_DynamicCounterNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.nodes.debug.DynamicCounterNode
    //       method: counter(java.lang.String,java.lang.String,long,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class DynamicCounterNode_counter extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            java.lang.String arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(java.lang.String.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            java.lang.String arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(java.lang.String.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg2 = args[2];
            boolean arg3;
            if (args[3].isConstant()) {
                arg3 = args[3].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.debug.DynamicCounterNode node = new org.graalvm.compiler.nodes.debug.DynamicCounterNode(arg0, arg1, arg2, arg3);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private DynamicCounterNode_counter(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new DynamicCounterNode_counter(injection), org.graalvm.compiler.nodes.debug.DynamicCounterNode.class, "counter", java.lang.String.class, java.lang.String.class, long.class, boolean.class);
    }
}
