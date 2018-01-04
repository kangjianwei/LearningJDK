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

public class PluginFactory_HotSpotReplacementsUtil implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: ageMaskInPlace(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_ageMaskInPlace extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.ageMaskInPlace(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_ageMaskInPlace(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayClassElementOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayClassElementOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayClassElementOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_arrayClassElementOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayIndexScale(jdk.vm.ci.meta.MetaAccessProvider,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayIndexScale extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            jdk.vm.ci.meta.MetaAccessProvider arg0 = b.getMetaAccess();
            jdk.vm.ci.meta.JavaKind arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayIndexScale(arg0, arg1);
            JavaConstant constant = JavaConstant.forInt(result);
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

        private HotSpotReplacementsUtil_arrayIndexScale(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayKlassComponentMirrorOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayKlassComponentMirrorOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayKlassComponentMirrorOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_arrayKlassComponentMirrorOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayKlassOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayKlassOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayKlassOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_arrayKlassOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayLengthOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayLengthOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayLengthOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_arrayLengthOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: arrayPrototypeMarkWord(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_arrayPrototypeMarkWord extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayPrototypeMarkWord(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_arrayPrototypeMarkWord(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: biasedLockMaskInPlace(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_biasedLockMaskInPlace extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.biasedLockMaskInPlace(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_biasedLockMaskInPlace(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: biasedLockPattern(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_biasedLockPattern extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.biasedLockPattern(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_biasedLockPattern(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: cardTableShift(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_cardTableShift extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.cardTableShift(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_cardTableShift(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: cardTableStart(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_cardTableStart extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.cardTableStart(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_cardTableStart(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: config(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_config extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.config(arg0);
            JavaConstant constant = snippetReflection.forObject(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Object, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_config(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: dirtyCardValue(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_dirtyCardValue extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            byte result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.dirtyCardValue(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_dirtyCardValue(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: epochMaskInPlace(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_epochMaskInPlace extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.epochMaskInPlace(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_epochMaskInPlace(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1CardQueueBufferOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1CardQueueBufferOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1CardQueueBufferOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1CardQueueBufferOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1CardQueueIndexOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1CardQueueIndexOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1CardQueueIndexOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1CardQueueIndexOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1SATBQueueBufferOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1SATBQueueBufferOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1SATBQueueBufferOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1SATBQueueBufferOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1SATBQueueIndexOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1SATBQueueIndexOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1SATBQueueIndexOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1SATBQueueIndexOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1SATBQueueMarkingOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1SATBQueueMarkingOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1SATBQueueMarkingOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1SATBQueueMarkingOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: g1YoungCardValue(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_g1YoungCardValue extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            byte result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.g1YoungCardValue(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_g1YoungCardValue(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: gcTotalCollectionsAddress(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_gcTotalCollectionsAddress extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.gcTotalCollectionsAddress(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_gcTotalCollectionsAddress(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: getArrayBaseOffset(jdk.vm.ci.meta.MetaAccessProvider,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_getArrayBaseOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            jdk.vm.ci.meta.MetaAccessProvider arg0 = b.getMetaAccess();
            jdk.vm.ci.meta.JavaKind arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.getArrayBaseOffset(arg0, arg1);
            JavaConstant constant = JavaConstant.forInt(result);
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

        private HotSpotReplacementsUtil_getArrayBaseOffset(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: getConfig(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_getConfig extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.getConfig(arg0);
            JavaConstant constant = snippetReflection.forObject(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Object, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_getConfig(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: getWordKind()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_getWordKind extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            jdk.vm.ci.meta.JavaKind result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.getWordKind();
            JavaConstant constant = snippetReflection.forObject(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Object, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private HotSpotReplacementsUtil_getWordKind(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: heapEndAddress(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_heapEndAddress extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.heapEndAddress(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_heapEndAddress(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: heapTopAddress(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_heapTopAddress extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.heapTopAddress(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_heapTopAddress(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: heapWordSize(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_heapWordSize extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.heapWordSize(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_heapWordSize(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: hubOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_hubOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.hubOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_hubOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: identityHashCode(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class HotSpotReplacementsUtil_identityHashCode extends GeneratedInvocationPlugin {

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

        private HotSpotReplacementsUtil_identityHashCode(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(int.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: identityHashCodeShift(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_identityHashCodeShift extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.identityHashCodeShift(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_identityHashCodeShift(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: inlineContiguousAllocationSupported(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_inlineContiguousAllocationSupported extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.inlineContiguousAllocationSupported(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_inlineContiguousAllocationSupported(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: instanceHeaderSize(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_instanceHeaderSize extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.instanceHeaderSize(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_instanceHeaderSize(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: instanceKlassInitStateOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_instanceKlassInitStateOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.instanceKlassInitStateOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_instanceKlassInitStateOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: instanceKlassStateFullyInitialized(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_instanceKlassStateFullyInitialized extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.instanceKlassStateFullyInitialized(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_instanceKlassStateFullyInitialized(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: jvmAccWrittenFlags(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_jvmAccWrittenFlags extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.jvmAccWrittenFlags(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_jvmAccWrittenFlags(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: klassAccessFlagsOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_klassAccessFlagsOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassAccessFlagsOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_klassAccessFlagsOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: klassLayoutHelperOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_klassLayoutHelperOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassLayoutHelperOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_klassLayoutHelperOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: klassModifierFlagsOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_klassModifierFlagsOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassModifierFlagsOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_klassModifierFlagsOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: klassOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_klassOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_klassOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: klassSuperKlassOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_klassSuperKlassOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassSuperKlassOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_klassSuperKlassOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperElementTypeMask(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperElementTypeMask extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperElementTypeMask(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperElementTypeMask(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperElementTypePrimitiveInPlace(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperElementTypePrimitiveInPlace extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperElementTypePrimitiveInPlace(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperElementTypePrimitiveInPlace(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperElementTypeShift(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperElementTypeShift extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperElementTypeShift(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperElementTypeShift(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperHeaderSizeMask(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperHeaderSizeMask extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperHeaderSizeMask(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperHeaderSizeMask(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperHeaderSizeShift(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperHeaderSizeShift extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperHeaderSizeShift(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperHeaderSizeShift(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperLog2ElementSizeMask(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperLog2ElementSizeMask extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperLog2ElementSizeMask(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperLog2ElementSizeMask(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: layoutHelperLog2ElementSizeShift(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_layoutHelperLog2ElementSizeShift extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.layoutHelperLog2ElementSizeShift(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_layoutHelperLog2ElementSizeShift(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: loadHubIntrinsic(java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HotSpotReplacementsUtil_loadHubIntrinsic extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.nodes.spi.StampProvider arg0 = b.getStampProvider();
            ValueNode arg1 = args[0];
            org.graalvm.compiler.nodes.extended.LoadHubNode node = new org.graalvm.compiler.nodes.extended.LoadHubNode(arg0, arg1);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: loadKlassFromObjectIntrinsic(java.lang.Object,long,jdk.internal.vm.compiler.word.LocationIdentity,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HotSpotReplacementsUtil_loadKlassFromObjectIntrinsic extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            ValueNode arg1 = args[0];
            ValueNode arg2 = args[1];
            jdk.internal.vm.compiler.word.LocationIdentity arg3;
            if (args[2].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[2].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.vm.ci.meta.JavaKind arg4;
            if (args[3].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.RawLoadNode node = new org.graalvm.compiler.nodes.extended.RawLoadNode(arg0, arg1, arg2, arg3, arg4);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private HotSpotReplacementsUtil_loadKlassFromObjectIntrinsic(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(org.graalvm.compiler.hotspot.word.KlassPointer.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: loadWordFromObjectIntrinsic(java.lang.Object,long,jdk.internal.vm.compiler.word.LocationIdentity,jdk.vm.ci.meta.JavaKind)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HotSpotReplacementsUtil_loadWordFromObjectIntrinsic extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            ValueNode arg1 = args[0];
            ValueNode arg2 = args[1];
            jdk.internal.vm.compiler.word.LocationIdentity arg3;
            if (args[2].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[2].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.vm.ci.meta.JavaKind arg4;
            if (args[3].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.RawLoadNode node = new org.graalvm.compiler.nodes.extended.RawLoadNode(arg0, arg1, arg2, arg3, arg4);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private HotSpotReplacementsUtil_loadWordFromObjectIntrinsic(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(org.graalvm.compiler.word.Word.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: lockDisplacedMarkOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_lockDisplacedMarkOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.lockDisplacedMarkOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_lockDisplacedMarkOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: log2WordSize()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_log2WordSize extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.log2WordSize();
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: logOfHeapRegionGrainBytes(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_logOfHeapRegionGrainBytes extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.logOfHeapRegionGrainBytes(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_logOfHeapRegionGrainBytes(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: markOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_markOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.markOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_markOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: metaspaceArrayBaseOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_metaspaceArrayBaseOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.metaspaceArrayBaseOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_metaspaceArrayBaseOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: metaspaceArrayLengthOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_metaspaceArrayLengthOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.metaspaceArrayLengthOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_metaspaceArrayLengthOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: monitorMask(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_monitorMask extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.monitorMask(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_monitorMask(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: objectMonitorCxqOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_objectMonitorCxqOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorCxqOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_objectMonitorCxqOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: objectMonitorEntryListOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_objectMonitorEntryListOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorEntryListOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_objectMonitorEntryListOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: objectMonitorOwnerOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_objectMonitorOwnerOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorOwnerOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_objectMonitorOwnerOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: objectMonitorRecursionsOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_objectMonitorRecursionsOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorRecursionsOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_objectMonitorRecursionsOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: objectResultOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_objectResultOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectResultOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_objectResultOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: osThreadInterruptedOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_osThreadInterruptedOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.osThreadInterruptedOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_osThreadInterruptedOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: osThreadOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_osThreadOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.osThreadOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_osThreadOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: pageSize()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_pageSize extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.pageSize();
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: prototypeMarkWordOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_prototypeMarkWordOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.prototypeMarkWordOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_prototypeMarkWordOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: readLayoutHelper(org.graalvm.compiler.hotspot.word.KlassPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class HotSpotReplacementsUtil_readLayoutHelper extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            ValueNode arg1 = args[0];
            return org.graalvm.compiler.hotspot.replacements.KlassLayoutHelperNode.intrinsify(b, targetMethod, arg0, arg1);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_readLayoutHelper(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: referentOffset()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_referentOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.referentOffset();
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: registerAsWord(jdk.vm.ci.code.Register,boolean,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HotSpotReplacementsUtil_registerAsWord extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            jdk.vm.ci.code.Register arg1;
            if (args[0].isConstant()) {
                arg1 = snippetReflection.asObject(jdk.vm.ci.code.Register.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg2;
            if (args[1].isConstant()) {
                arg2 = args[1].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg3;
            if (args[2].isConstant()) {
                arg3 = args[2].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.replacements.nodes.ReadRegisterNode node = new org.graalvm.compiler.replacements.nodes.ReadRegisterNode(arg0, arg1, arg2, arg3);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private HotSpotReplacementsUtil_registerAsWord(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(org.graalvm.compiler.word.Word.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: secondarySuperCacheOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_secondarySuperCacheOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.secondarySuperCacheOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_secondarySuperCacheOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: secondarySupersOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_secondarySupersOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.secondarySupersOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_secondarySupersOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: superCheckOffsetOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_superCheckOffsetOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.superCheckOffsetOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_superCheckOffsetOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadAllocatedBytesOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadAllocatedBytesOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadAllocatedBytesOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadAllocatedBytesOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadExceptionOopOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadExceptionOopOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadExceptionOopOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadExceptionOopOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadExceptionPcOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadExceptionPcOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadExceptionPcOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadExceptionPcOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadObjectOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadObjectOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadObjectOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadObjectOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadPendingDeoptimizationOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadPendingDeoptimizationOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadPendingDeoptimizationOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadPendingDeoptimizationOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadPendingExceptionOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadPendingExceptionOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadPendingExceptionOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadPendingExceptionOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadTlabEndOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadTlabEndOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadTlabEndOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadTlabEndOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadTlabSizeOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadTlabSizeOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadTlabSizeOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadTlabSizeOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadTlabStartOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadTlabStartOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadTlabStartOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadTlabStartOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: threadTlabTopOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_threadTlabTopOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadTlabTopOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_threadTlabTopOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabAlignmentReserveInHeapWords(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabAlignmentReserveInHeapWords extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabAlignmentReserveInHeapWords(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabAlignmentReserveInHeapWords(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabFastRefillWasteOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabFastRefillWasteOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabFastRefillWasteOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabFastRefillWasteOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabIntArrayMarkWord(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabIntArrayMarkWord extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            long result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabIntArrayMarkWord(arg0);
            JavaConstant constant = JavaConstant.forLong(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Long, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabIntArrayMarkWord(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabNumberOfRefillsOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabNumberOfRefillsOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabNumberOfRefillsOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabNumberOfRefillsOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabRefillWasteIncrement(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabRefillWasteIncrement extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabRefillWasteIncrement(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabRefillWasteIncrement(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabRefillWasteLimitOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabRefillWasteLimitOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabRefillWasteLimitOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabRefillWasteLimitOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabSlowAllocationsOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabSlowAllocationsOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabSlowAllocationsOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabSlowAllocationsOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: tlabStats(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_tlabStats extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.tlabStats(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_tlabStats(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: uninitializedIdentityHashCodeValue(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_uninitializedIdentityHashCodeValue extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.uninitializedIdentityHashCodeValue(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_uninitializedIdentityHashCodeValue(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: unlockedMask(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_unlockedMask extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.unlockedMask(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_unlockedMask(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useBiasedLocking(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useBiasedLocking extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useBiasedLocking(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useBiasedLocking(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useCMSIncrementalMode(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useCMSIncrementalMode extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useCMSIncrementalMode(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useCMSIncrementalMode(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useCompressedOops(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useCompressedOops extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useCompressedOops(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useCompressedOops(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useDeferredInitBarriers(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useDeferredInitBarriers extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useDeferredInitBarriers(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useDeferredInitBarriers(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useFastTLABRefill(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useFastTLABRefill extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useFastTLABRefill(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useFastTLABRefill(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useG1GC(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useG1GC extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useG1GC(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useG1GC(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: useTLAB(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_useTLAB extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useTLAB(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_useTLAB(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: verifiedEntryPointOffset(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_verifiedEntryPointOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.verifiedEntryPointOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_verifiedEntryPointOffset(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: verifyOopStub(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class HotSpotReplacementsUtil_verifyOopStub extends GeneratedInvocationPlugin {

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

        private HotSpotReplacementsUtil_verifyOopStub(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(java.lang.Object.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: verifyOops(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_verifyOops extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.hotspot.GraalHotSpotVMConfig arg0 = injectedGraalHotSpotVMConfig;
            boolean result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.verifyOops(arg0);
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

        private final org.graalvm.compiler.hotspot.GraalHotSpotVMConfig injectedGraalHotSpotVMConfig;

        private HotSpotReplacementsUtil_verifyOops(InjectionProvider injection) {
            this.injectedGraalHotSpotVMConfig = injection.getInjectedArgument(org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: wordSize()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class HotSpotReplacementsUtil_wordSize extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int result = org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.wordSize();
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil
    //       method: writeRegisterAsWord(jdk.vm.ci.code.Register,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HotSpotReplacementsUtil_writeRegisterAsWord extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            jdk.vm.ci.code.Register arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(jdk.vm.ci.code.Register.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.nodes.WriteRegisterNode node = new org.graalvm.compiler.replacements.nodes.WriteRegisterNode(arg0, arg1);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private HotSpotReplacementsUtil_writeRegisterAsWord(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new HotSpotReplacementsUtil_ageMaskInPlace(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "ageMaskInPlace", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_arrayClassElementOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayClassElementOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_arrayIndexScale(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayIndexScale", jdk.vm.ci.meta.MetaAccessProvider.class, jdk.vm.ci.meta.JavaKind.class);
        plugins.register(new HotSpotReplacementsUtil_arrayKlassComponentMirrorOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayKlassComponentMirrorOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_arrayKlassOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayKlassOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_arrayLengthOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayLengthOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_arrayPrototypeMarkWord(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "arrayPrototypeMarkWord", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_biasedLockMaskInPlace(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "biasedLockMaskInPlace", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_biasedLockPattern(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "biasedLockPattern", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_cardTableShift(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "cardTableShift", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_cardTableStart(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "cardTableStart", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_config(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "config", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_dirtyCardValue(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "dirtyCardValue", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_epochMaskInPlace(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "epochMaskInPlace", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1CardQueueBufferOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1CardQueueBufferOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1CardQueueIndexOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1CardQueueIndexOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1SATBQueueBufferOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1SATBQueueBufferOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1SATBQueueIndexOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1SATBQueueIndexOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1SATBQueueMarkingOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1SATBQueueMarkingOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_g1YoungCardValue(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "g1YoungCardValue", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_gcTotalCollectionsAddress(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "gcTotalCollectionsAddress", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_getArrayBaseOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "getArrayBaseOffset", jdk.vm.ci.meta.MetaAccessProvider.class, jdk.vm.ci.meta.JavaKind.class);
        plugins.register(new HotSpotReplacementsUtil_getConfig(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "getConfig", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_getWordKind(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "getWordKind");
        plugins.register(new HotSpotReplacementsUtil_heapEndAddress(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "heapEndAddress", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_heapTopAddress(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "heapTopAddress", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_heapWordSize(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "heapWordSize", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_hubOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "hubOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_identityHashCode(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "identityHashCode", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, java.lang.Object.class);
        plugins.register(new HotSpotReplacementsUtil_identityHashCodeShift(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "identityHashCodeShift", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_inlineContiguousAllocationSupported(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "inlineContiguousAllocationSupported", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_instanceHeaderSize(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "instanceHeaderSize", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_instanceKlassInitStateOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "instanceKlassInitStateOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_instanceKlassStateFullyInitialized(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "instanceKlassStateFullyInitialized", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_jvmAccWrittenFlags(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "jvmAccWrittenFlags", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_klassAccessFlagsOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "klassAccessFlagsOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_klassLayoutHelperOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "klassLayoutHelperOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_klassModifierFlagsOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "klassModifierFlagsOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_klassOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "klassOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_klassSuperKlassOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "klassSuperKlassOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperElementTypeMask(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperElementTypeMask", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperElementTypePrimitiveInPlace(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperElementTypePrimitiveInPlace", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperElementTypeShift(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperElementTypeShift", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperHeaderSizeMask(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperHeaderSizeMask", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperHeaderSizeShift(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperHeaderSizeShift", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperLog2ElementSizeMask(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperLog2ElementSizeMask", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_layoutHelperLog2ElementSizeShift(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "layoutHelperLog2ElementSizeShift", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_loadHubIntrinsic(), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "loadHubIntrinsic", java.lang.Object.class);
        plugins.register(new HotSpotReplacementsUtil_loadKlassFromObjectIntrinsic(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "loadKlassFromObjectIntrinsic", java.lang.Object.class, long.class, jdk.internal.vm.compiler.word.LocationIdentity.class, jdk.vm.ci.meta.JavaKind.class);
        plugins.register(new HotSpotReplacementsUtil_loadWordFromObjectIntrinsic(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "loadWordFromObjectIntrinsic", java.lang.Object.class, long.class, jdk.internal.vm.compiler.word.LocationIdentity.class, jdk.vm.ci.meta.JavaKind.class);
        plugins.register(new HotSpotReplacementsUtil_lockDisplacedMarkOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "lockDisplacedMarkOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_log2WordSize(), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "log2WordSize");
        plugins.register(new HotSpotReplacementsUtil_logOfHeapRegionGrainBytes(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "logOfHeapRegionGrainBytes", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_markOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "markOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_metaspaceArrayBaseOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "metaspaceArrayBaseOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_metaspaceArrayLengthOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "metaspaceArrayLengthOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_monitorMask(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "monitorMask", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_objectMonitorCxqOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "objectMonitorCxqOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_objectMonitorEntryListOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "objectMonitorEntryListOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_objectMonitorOwnerOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "objectMonitorOwnerOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_objectMonitorRecursionsOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "objectMonitorRecursionsOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_objectResultOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "objectResultOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_osThreadInterruptedOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "osThreadInterruptedOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_osThreadOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "osThreadOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_pageSize(), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "pageSize");
        plugins.register(new HotSpotReplacementsUtil_prototypeMarkWordOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "prototypeMarkWordOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_readLayoutHelper(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "readLayoutHelper", org.graalvm.compiler.hotspot.word.KlassPointer.class);
        plugins.register(new HotSpotReplacementsUtil_referentOffset(), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "referentOffset");
        plugins.register(new HotSpotReplacementsUtil_registerAsWord(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "registerAsWord", jdk.vm.ci.code.Register.class, boolean.class, boolean.class);
        plugins.register(new HotSpotReplacementsUtil_secondarySuperCacheOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "secondarySuperCacheOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_secondarySupersOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "secondarySupersOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_superCheckOffsetOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "superCheckOffsetOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadAllocatedBytesOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadAllocatedBytesOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadExceptionOopOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadExceptionOopOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadExceptionPcOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadExceptionPcOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadObjectOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadObjectOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadPendingDeoptimizationOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadPendingDeoptimizationOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadPendingExceptionOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadPendingExceptionOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadTlabEndOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadTlabEndOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadTlabSizeOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadTlabSizeOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadTlabStartOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadTlabStartOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_threadTlabTopOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "threadTlabTopOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabAlignmentReserveInHeapWords(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabAlignmentReserveInHeapWords", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabFastRefillWasteOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabFastRefillWasteOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabIntArrayMarkWord(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabIntArrayMarkWord", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabNumberOfRefillsOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabNumberOfRefillsOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabRefillWasteIncrement(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabRefillWasteIncrement", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabRefillWasteLimitOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabRefillWasteLimitOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabSlowAllocationsOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabSlowAllocationsOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_tlabStats(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "tlabStats", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_uninitializedIdentityHashCodeValue(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "uninitializedIdentityHashCodeValue", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_unlockedMask(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "unlockedMask", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useBiasedLocking(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useBiasedLocking", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useCMSIncrementalMode(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useCMSIncrementalMode", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useCompressedOops(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useCompressedOops", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useDeferredInitBarriers(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useDeferredInitBarriers", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useFastTLABRefill(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useFastTLABRefill", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useG1GC(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useG1GC", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_useTLAB(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "useTLAB", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_verifiedEntryPointOffset(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "verifiedEntryPointOffset", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_verifyOopStub(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "verifyOopStub", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, java.lang.Object.class);
        plugins.register(new HotSpotReplacementsUtil_verifyOops(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "verifyOops", org.graalvm.compiler.hotspot.GraalHotSpotVMConfig.class);
        plugins.register(new HotSpotReplacementsUtil_wordSize(), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "wordSize");
        plugins.register(new HotSpotReplacementsUtil_writeRegisterAsWord(injection), org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.class, "writeRegisterAsWord", jdk.vm.ci.code.Register.class, org.graalvm.compiler.word.Word.class);
    }
}
