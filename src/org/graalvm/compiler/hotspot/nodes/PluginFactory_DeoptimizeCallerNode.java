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

public class PluginFactory_DeoptimizeCallerNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.DeoptimizeCallerNode
    //       method: deopt(jdk.vm.ci.meta.DeoptimizationAction,jdk.vm.ci.meta.DeoptimizationReason)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class DeoptimizeCallerNode_deopt extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            jdk.vm.ci.meta.DeoptimizationAction arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(jdk.vm.ci.meta.DeoptimizationAction.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.vm.ci.meta.DeoptimizationReason arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(jdk.vm.ci.meta.DeoptimizationReason.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.nodes.DeoptimizeCallerNode node = new org.graalvm.compiler.hotspot.nodes.DeoptimizeCallerNode(arg0, arg1);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private DeoptimizeCallerNode_deopt(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new DeoptimizeCallerNode_deopt(injection), org.graalvm.compiler.hotspot.nodes.DeoptimizeCallerNode.class, "deopt", jdk.vm.ci.meta.DeoptimizationAction.class, jdk.vm.ci.meta.DeoptimizationReason.class);
    }
}
