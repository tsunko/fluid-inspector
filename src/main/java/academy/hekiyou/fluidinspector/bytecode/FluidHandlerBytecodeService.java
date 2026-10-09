package academy.hekiyou.fluidinspector.bytecode;

import academy.hekiyou.fluidinspector.FluidInspectorMod;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.EnumSet;
import java.util.Map;

/**
 * EXTREMELY DANGEROUS WAY TO DO THIS - BUT THE ONLY WAY TO PROPERLY CAPTURE ALL FILL/DRAIN!
 */
public class FluidHandlerBytecodeService implements ILaunchPluginService {

    private static final Logger LOGGER = LogManager.getLogger(FluidHandlerBytecodeService.class);
    private static final EnumSet<Phase> ALL_BEFORE = EnumSet.of(Phase.BEFORE);

    private static final String GET_CAPABILITIES_DESC =
            "(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;";

    private static final String FILL_DESC = "(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I";
    private static final String DRAIN_STACK_DESC = "(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;";
    private static final String DRAIN_INT_DESC = "(ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;";

    private static final String INSN_OWNER = "academy/hekiyou/fluidinspector/IFluidHandlerHooks";

    @Override
    public String name() {
        return FluidInspectorMod.MOD_ID;
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        String className = classType.getClassName();

        // don't intercept ourselves!
        if (className.startsWith("academy.hekiyou.fluidinspector")) {
            return EnumSet.noneOf(Phase.class);
        }

        return ALL_BEFORE;
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        // HERE BE DRAGONS
        boolean wasModified = false;

        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            // don't try to overwrite an interface
            return false;
        }

        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
                // don't try to write to functions that are abstract
                continue;
            }

            // check for fill/drain to hook those methods
            if (method.name.equals("fill") && method.desc.equals(FILL_DESC)) {
                method.instructions.insert(makeInsnBeginFill()); // insert our beginFill() call

                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.IRETURN) {
                        method.instructions.insertBefore(insn, makeInsnEndFill());
                    }
                }

                method.maxStack += 3;
                wasModified = true;
            } else if (method.name.equals("drain")) {
                if (method.desc.equals(DRAIN_STACK_DESC)) {
                    method.instructions.insert(makeInsnBeginDrainStack());

                    for (AbstractInsnNode insn : method.instructions.toArray()) {
                        if (insn.getOpcode() == Opcodes.ARETURN) {
                            method.instructions.insertBefore(insn, makeInsnEndDrainStack());
                        }
                    }

                    method.maxStack += 3;
                    wasModified = true;
                } else if (method.desc.equals(DRAIN_INT_DESC)) {
                    method.instructions.insert(makeInsnBeginDrainInt());

                    for (AbstractInsnNode insn : method.instructions.toArray()) {
                        if (insn.getOpcode() == Opcodes.ARETURN) {
                            method.instructions.insertBefore(insn, makeInsnEndDrainInt());
                        }
                    }

                    method.maxStack += 3;
                    wasModified = true;
                }
            }

            int sideSlot = -1;
            int capabilitySlot = -1;
            int providerSlot = -1;
            boolean allocatedSlots = false;

            // check if we call getCapability ever
            for (AbstractInsnNode node : method.instructions.toArray()) {
                if (node instanceof MethodInsnNode methodCall) {
                    // need to filter by name because Mekanism has a function named "getCapabilityUnchecked" that will
                    // end up leading to recursive calls between getCapabilityUnchecked and getCapability
                    if (methodCall.name.equals("getCapability") && methodCall.desc.equals(GET_CAPABILITIES_DESC)) {
                        if (!allocatedSlots) {
                            sideSlot = method.maxLocals++;
                            capabilitySlot = method.maxLocals++;
                            providerSlot = method.maxLocals++;
                            allocatedSlots = true;
                        }

                        InsnList callLog = new InsnList();

                        // pop the 3 elements off the stack and put them into each slot for temporary keeping
                        callLog.add(new VarInsnNode(Opcodes.ASTORE, sideSlot));
                        callLog.add(new VarInsnNode(Opcodes.ASTORE, capabilitySlot));
                        callLog.add(new VarInsnNode(Opcodes.ASTORE, providerSlot));

                        // now push them back onto the stack to setup our call
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, providerSlot));
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, capabilitySlot));
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, sideSlot));

                        // call logging function
                        callLog.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "academy/hekiyou/fluidinspector/GetCapabilityHook",
                                "getCapabilityHook",
                                "(Ljava/lang/Object;Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)V"
                        ));

                        // restore old call stack
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, providerSlot));
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, capabilitySlot));
                        callLog.add(new VarInsnNode(Opcodes.ALOAD, sideSlot));

                        // inject right before the getCapability
                        method.maxStack += 3;
                        method.instructions.insertBefore(methodCall, callLog);
                        wasModified = true;
                    }
                }
            }
        }

        if (wasModified) {
            LOGGER.info("Instrumented {}", classNode.name);
        }

        return wasModified;
    }

    private InsnList makeInsnBeginFill() {
        // int fill(FluidStack resource, IFluidHandler.FluidAction action)
        InsnList begin = new InsnList();
        begin.add(new VarInsnNode(Opcodes.ALOAD, 0)); // load this (IFluidHandler impl) onto stack
        begin.add(new VarInsnNode(Opcodes.ALOAD, 1)); // load resource onto stack (again)
        begin.add(new VarInsnNode(Opcodes.ALOAD, 2)); // load action onto stack (again)
        begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "beginFill",
                "(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));
        return begin;
    }

    public static InsnList makeInsnEndFill() {
        InsnList end = new InsnList();
        end.add(new InsnNode(Opcodes.DUP));
        end.add(new VarInsnNode(Opcodes.ALOAD, 0));
        end.add(new VarInsnNode(Opcodes.ALOAD, 1));
        end.add(new VarInsnNode(Opcodes.ALOAD, 2));
        end.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "endFill",
                "(ILjava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));
        return end;
    }

    private InsnList makeInsnBeginDrainStack() {
        // FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action)
        InsnList begin = new InsnList();
        begin.add(new VarInsnNode(Opcodes.ALOAD, 0));
        begin.add(new VarInsnNode(Opcodes.ALOAD, 1));
        begin.add(new VarInsnNode(Opcodes.ALOAD, 2));
        begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "beginDrainStack",
                "(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));
        return begin;
    }

    private InsnList makeInsnEndDrainStack() {
        InsnList end = new InsnList();
        end.add(new InsnNode(Opcodes.DUP));
        end.add(new VarInsnNode(Opcodes.ALOAD, 0));
        end.add(new VarInsnNode(Opcodes.ALOAD, 1));
        end.add(new VarInsnNode(Opcodes.ALOAD, 2));
        end.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "endDrainStack",
                "(Lnet/minecraftforge/fluids/FluidStack;Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));

        return end;
    }

    private InsnList makeInsnBeginDrainInt() {
        // FluidStack drain(int maxDrain, IFluidHandler.FluidAction action)
        InsnList begin = new InsnList();
        begin.add(new VarInsnNode(Opcodes.ALOAD, 0));
        begin.add(new VarInsnNode(Opcodes.ILOAD, 1));
        begin.add(new VarInsnNode(Opcodes.ALOAD, 2));
        begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "beginDrainInt",
                "(Ljava/lang/Object;ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));
        return begin;
    }

    private InsnList makeInsnEndDrainInt() {
        InsnList end = new InsnList();
        end.add(new InsnNode(Opcodes.DUP));
        end.add(new VarInsnNode(Opcodes.ALOAD, 0));
        end.add(new VarInsnNode(Opcodes.ILOAD, 1));
        end.add(new VarInsnNode(Opcodes.ALOAD, 2));
        end.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                INSN_OWNER,
                "endDrainInt",
                "(Lnet/minecraftforge/fluids/FluidStack;Ljava/lang/Object;ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)V"));
        return end;
    }

    public static void inject() {
        try {
            LaunchPluginHandler handler = (LaunchPluginHandler) ReflectionUtil.getFieldValue(Launcher.class.getDeclaredField("launchPlugins"), Launcher.INSTANCE);
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>)ReflectionUtil.getFieldValue(LaunchPluginHandler.class.getDeclaredField("plugins"), handler);
            FluidHandlerBytecodeService transformer = new FluidHandlerBytecodeService();
            plugins.put(transformer.name(), transformer);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

}
