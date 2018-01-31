// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_VMErrorNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.VMErrorNode
    //       method: vmError(java.lang.String,long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class VMErrorNode_vmError extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            java.lang.String arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(java.lang.String.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg1 = args[1];
            org.graalvm.compiler.hotspot.nodes.VMErrorNode node = new org.graalvm.compiler.hotspot.nodes.VMErrorNode(arg0, arg1);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private VMErrorNode_vmError(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new VMErrorNode_vmError(injection), org.graalvm.compiler.hotspot.nodes.VMErrorNode.class, "vmError", java.lang.String.class, long.class);
    }
}
