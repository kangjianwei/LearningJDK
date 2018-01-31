// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import org.graalvm.compiler.nodes.ConstantNode;

public class PluginFactory_MonitorSnippets implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: bkpt(java.lang.Object,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class MonitorSnippets_bkpt extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            ValueNode arg3 = args[3];
            org.graalvm.compiler.nodes.BreakpointNode node = new org.graalvm.compiler.nodes.BreakpointNode(arg0, arg1, arg2, arg3);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: doProfile(org.graalvm.compiler.options.OptionValues)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class MonitorSnippets_doProfile extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.options.OptionValues arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(org.graalvm.compiler.options.OptionValues.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean result = org.graalvm.compiler.hotspot.replacements.MonitorSnippets.doProfile(arg0);
            JavaConstant constant = JavaConstant.forInt(result ? 1 : 0);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private MonitorSnippets_doProfile(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: monitorenterStubC(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,java.lang.Object,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class MonitorSnippets_monitorenterStubC extends GeneratedInvocationPlugin {

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
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private MonitorSnippets_monitorenterStubC(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: monitorexitStubC(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,java.lang.Object,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class MonitorSnippets_monitorexitStubC extends GeneratedInvocationPlugin {

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
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private MonitorSnippets_monitorexitStubC(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: useFastInflatedLocking(org.graalvm.compiler.options.OptionValues)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class MonitorSnippets_useFastInflatedLocking extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.options.OptionValues arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(org.graalvm.compiler.options.OptionValues.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean result = org.graalvm.compiler.hotspot.replacements.MonitorSnippets.useFastInflatedLocking(arg0);
            JavaConstant constant = JavaConstant.forInt(result ? 1 : 0);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private MonitorSnippets_useFastInflatedLocking(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.MonitorSnippets
    //       method: verifyBalancedMonitors(org.graalvm.compiler.options.OptionValues)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class MonitorSnippets_verifyBalancedMonitors extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.options.OptionValues arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(org.graalvm.compiler.options.OptionValues.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean result = org.graalvm.compiler.hotspot.replacements.MonitorSnippets.verifyBalancedMonitors(arg0);
            JavaConstant constant = JavaConstant.forInt(result ? 1 : 0);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private MonitorSnippets_verifyBalancedMonitors(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new MonitorSnippets_bkpt(), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "bkpt", java.lang.Object.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class);
        plugins.register(new MonitorSnippets_doProfile(injection), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "doProfile", org.graalvm.compiler.options.OptionValues.class);
        plugins.register(new MonitorSnippets_monitorenterStubC(injection), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "monitorenterStubC", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, java.lang.Object.class, org.graalvm.compiler.word.Word.class);
        plugins.register(new MonitorSnippets_monitorexitStubC(injection), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "monitorexitStubC", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, java.lang.Object.class, org.graalvm.compiler.word.Word.class);
        plugins.register(new MonitorSnippets_useFastInflatedLocking(injection), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "useFastInflatedLocking", org.graalvm.compiler.options.OptionValues.class);
        plugins.register(new MonitorSnippets_verifyBalancedMonitors(injection), org.graalvm.compiler.hotspot.replacements.MonitorSnippets.class, "verifyBalancedMonitors", org.graalvm.compiler.options.OptionValues.class);
    }
}
