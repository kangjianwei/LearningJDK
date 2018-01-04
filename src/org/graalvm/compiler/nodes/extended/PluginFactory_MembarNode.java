// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_MembarNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.nodes.extended.MembarNode
    //       method: memoryBarrier(int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class MembarNode_memoryBarrier_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int arg0;
            if (args[0].isConstant()) {
                arg0 = args[0].asJavaConstant().asInt();
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.MembarNode node = new org.graalvm.compiler.nodes.extended.MembarNode(arg0);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.nodes.extended.MembarNode
    //       method: memoryBarrier(int,jdk.internal.vm.compiler.word.LocationIdentity)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class MembarNode_memoryBarrier_int_LocationIdentity extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int arg0;
            if (args[0].isConstant()) {
                arg0 = args[0].asJavaConstant().asInt();
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.MembarNode node = new org.graalvm.compiler.nodes.extended.MembarNode(arg0, arg1);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private MembarNode_memoryBarrier_int_LocationIdentity(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new MembarNode_memoryBarrier_int(), org.graalvm.compiler.nodes.extended.MembarNode.class, "memoryBarrier", int.class);
        plugins.register(new MembarNode_memoryBarrier_int_LocationIdentity(injection), org.graalvm.compiler.nodes.extended.MembarNode.class, "memoryBarrier", int.class, jdk.internal.vm.compiler.word.LocationIdentity.class);
    }
}
