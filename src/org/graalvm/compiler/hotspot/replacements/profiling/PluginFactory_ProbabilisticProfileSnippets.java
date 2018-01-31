// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.replacements.profiling;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_ProbabilisticProfileSnippets implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.profiling.ProbabilisticProfileSnippets
    //       method: methodBackedgeEvent(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.hotspot.word.MethodCountersPointer,int,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class ProbabilisticProfileSnippets_methodBackedgeEvent extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg1 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.spi.ForeignCallDescriptor arg2;
            if (args[0].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg3 = args[1];
            ValueNode arg4 = args[2];
            ValueNode arg5 = args[3];
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private ProbabilisticProfileSnippets_methodBackedgeEvent(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.profiling.ProbabilisticProfileSnippets
    //       method: methodInvocationEvent(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.hotspot.word.MethodCountersPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class ProbabilisticProfileSnippets_methodInvocationEvent extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg1 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.spi.ForeignCallDescriptor arg2;
            if (args[0].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg3 = args[1];
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private ProbabilisticProfileSnippets_methodInvocationEvent(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ProbabilisticProfileSnippets_methodBackedgeEvent(injection), org.graalvm.compiler.hotspot.replacements.profiling.ProbabilisticProfileSnippets.class, "methodBackedgeEvent", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.hotspot.word.MethodCountersPointer.class, int.class, int.class);
        plugins.register(new ProbabilisticProfileSnippets_methodInvocationEvent(injection), org.graalvm.compiler.hotspot.replacements.profiling.ProbabilisticProfileSnippets.class, "methodInvocationEvent", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.hotspot.word.MethodCountersPointer.class);
    }
}
