package com.sun.tools.jdi;

import com.sun.jdi.*;
import java.util.*;


/**
 * Java(tm) Debug Wire Protocol
 */
class JDWP {

    static class VirtualMachine {
        static final int COMMAND_SET = 1;
        private VirtualMachine() {}  // hide constructor

        /**
         * Returns the JDWP version implemented by the target VM. 
         * The version string format is implementation dependent. 
         */
        static class Version {
            static final int COMMAND = 1;

            static Version process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Version"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static Version waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Version(vm, ps);
            }


            /**
             * Text information on the VM version
             */
            final String description;

            /**
             * Major JDWP Version number
             */
            final int jdwpMajor;

            /**
             * Minor JDWP Version number
             */
            final int jdwpMinor;

            /**
             * Target VM JRE version, as in the java.version property
             */
            final String vmVersion;

            /**
             * Target VM name, as in the java.vm.name property
             */
            final String vmName;

            private Version(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Version"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                description = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "description(String): " + description);
                }
                jdwpMajor = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "jdwpMajor(int): " + jdwpMajor);
                }
                jdwpMinor = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "jdwpMinor(int): " + jdwpMinor);
                }
                vmVersion = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "vmVersion(String): " + vmVersion);
                }
                vmName = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "vmName(String): " + vmName);
                }
            }
        }

        /**
         * Returns reference types for all the classes loaded by the target VM 
         * which match the given signature. 
         * Multple reference types will be returned if two or more class 
         * loaders have loaded a class of the same name. 
         * The search is confined to loaded classes only; no attempt is made 
         * to load a class of the given signature. 
         */
        static class ClassesBySignature {
            static final int COMMAND = 2;

            static ClassesBySignature process(VirtualMachineImpl vm, 
                                String signature)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, signature);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                String signature) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ClassesBySignature"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 signature(String): " + signature);
                }
                ps.writeString(signature);
                ps.send();
                return ps;
            }

            static ClassesBySignature waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassesBySignature(vm, ps);
            }

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                final byte refTypeTag;

                /**
                 * Matching loaded reference type
                 */
                final long typeID;

                /**
                 * The current class 
                 * <a href="#JDWP_ClassStatus">status.</a> 
                 */
                final int status;

                private ClassInfo(VirtualMachineImpl vm, PacketStream ps) {
                    refTypeTag = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "refTypeTag(byte): " + refTypeTag);
                    }
                    typeID = ps.readClassRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "typeID(long): " + "ref="+typeID);
                    }
                    status = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "status(int): " + status);
                    }
                }
            }


            /**
             * Number of reference types that follow.
             */
            final ClassInfo[] classes;

            private ClassesBySignature(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ClassesBySignature"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classes(ClassInfo[]): " + "");
                }
                int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classes[i](ClassInfo): " + "");
                    }
                    classes[i] = new ClassInfo(vm, ps);
                }
            }
        }

        /**
         * Returns reference types for all classes currently loaded by the 
         * target VM.
         */
        static class AllClasses {
            static final int COMMAND = 3;

            static AllClasses process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static AllClasses waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new AllClasses(vm, ps);
            }

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                final byte refTypeTag;

                /**
                 * Loaded reference type
                 */
                final long typeID;

                /**
                 * The JNI signature of the loaded reference type
                 */
                final String signature;

                /**
                 * The current class 
                 * <a href="#JDWP_ClassStatus">status.</a> 
                 */
                final int status;

                private ClassInfo(VirtualMachineImpl vm, PacketStream ps) {
                    refTypeTag = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "refTypeTag(byte): " + refTypeTag);
                    }
                    typeID = ps.readClassRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "typeID(long): " + "ref="+typeID);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    status = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "status(int): " + status);
                    }
                }
            }


            /**
             * Number of reference types that follow.
             */
            final ClassInfo[] classes;

            private AllClasses(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classes(ClassInfo[]): " + "");
                }
                int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classes[i](ClassInfo): " + "");
                    }
                    classes[i] = new ClassInfo(vm, ps);
                }
            }
        }

        /**
         * Returns all threads currently running in the target VM . 
         * The returned list contains threads created through 
         * java.lang.Thread, all native threads attached to 
         * the target VM through JNI, and system threads created 
         * by the target VM. Threads that have not yet been started 
         * and threads that have completed their execution are not 
         * included in the returned list. 
         */
        static class AllThreads {
            static final int COMMAND = 4;

            static AllThreads process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllThreads"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static AllThreads waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new AllThreads(vm, ps);
            }


            /**
             * Number of threads that follow.
             */
            final ThreadReferenceImpl[] threads;

            private AllThreads(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllThreads"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "threads(ThreadReferenceImpl[]): " + "");
                }
                int threadsCount = ps.readInt();
                threads = new ThreadReferenceImpl[threadsCount];
                for (int i = 0; i < threadsCount; i++) {;
                    threads[i] = ps.readThreadReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "threads[i](ThreadReferenceImpl): " + (threads[i]==null?"NULL":"ref="+threads[i].ref()));
                    }
                }
            }
        }

        /**
         * Returns all thread groups that do not have a parent. This command 
         * may be used as the first step in building a tree (or trees) of the 
         * existing thread groups.
         */
        static class TopLevelThreadGroups {
            static final int COMMAND = 5;

            static TopLevelThreadGroups process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.TopLevelThreadGroups"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static TopLevelThreadGroups waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new TopLevelThreadGroups(vm, ps);
            }


            /**
             * Number of thread groups that follow.
             */
            final ThreadGroupReferenceImpl[] groups;

            private TopLevelThreadGroups(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.TopLevelThreadGroups"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "groups(ThreadGroupReferenceImpl[]): " + "");
                }
                int groupsCount = ps.readInt();
                groups = new ThreadGroupReferenceImpl[groupsCount];
                for (int i = 0; i < groupsCount; i++) {;
                    groups[i] = ps.readThreadGroupReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "groups[i](ThreadGroupReferenceImpl): " + (groups[i]==null?"NULL":"ref="+groups[i].ref()));
                    }
                }
            }
        }

        /**
         * Invalidates this virtual machine mirror. 
         * The communication channel to the target VM is closed, and 
         * the target VM prepares to accept another subsequent connection 
         * from this debugger or another debugger, including the 
         * following tasks: 
         * <ul>
         * <li>All event requests are cancelled. 
         * <li>All threads suspended by the thread-level 
         * <a href="#JDWP_ThreadReference_Resume">resume</a> command 
         * or the VM-level 
         * <a href="#JDWP_VirtualMachine_Resume">resume</a> command 
         * are resumed as many times as necessary for them to run. 
         * <li>Garbage collection is re-enabled in all cases where it was 
         * <a href="#JDWP_ObjectReference_DisableCollection">disabled</a> 
         * </ul>
         * Any current method invocations executing in the target VM 
         * are continued after the disconnection. Upon completion of any such 
         * method invocation, the invoking thread continues from the 
         * location where it was originally stopped. 
         * <p>
         * Resources originating in  
         * this VirtualMachine (ObjectReferences, ReferenceTypes, etc.) 
         * will become invalid. 
         */
        static class Dispose {
            static final int COMMAND = 6;

            static Dispose process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Dispose"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static Dispose waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Dispose(vm, ps);
            }


            private Dispose(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Dispose"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Returns the sizes of variably-sized data types in the target VM.
         * The returned values indicate the number of bytes used by the 
         * identifiers in command and reply packets.
         */
        static class IDSizes {
            static final int COMMAND = 7;

            static IDSizes process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.IDSizes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static IDSizes waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new IDSizes(vm, ps);
            }


            /**
             * fieldID size in bytes 
             */
            final int fieldIDSize;

            /**
             * methodID size in bytes 
             */
            final int methodIDSize;

            /**
             * objectID size in bytes 
             */
            final int objectIDSize;

            /**
             * referenceTypeID size in bytes 
             */
            final int referenceTypeIDSize;

            /**
             * frameID size in bytes 
             */
            final int frameIDSize;

            private IDSizes(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.IDSizes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                fieldIDSize = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "fieldIDSize(int): " + fieldIDSize);
                }
                methodIDSize = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "methodIDSize(int): " + methodIDSize);
                }
                objectIDSize = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "objectIDSize(int): " + objectIDSize);
                }
                referenceTypeIDSize = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "referenceTypeIDSize(int): " + referenceTypeIDSize);
                }
                frameIDSize = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "frameIDSize(int): " + frameIDSize);
                }
            }
        }

        /**
         * Suspends the execution of the application running in the target 
         * VM. All Java threads currently running will be suspended. 
         * <p>
         * Unlike java.lang.Thread.suspend, 
         * suspends of both the virtual machine and individual threads are 
         * counted. Before a thread will run again, it must be resumed through 
         * the <a href="#JDWP_VirtualMachine_Resume">VM-level resume</a> command 
         * or the <a href="#JDWP_ThreadReference_Resume">thread-level resume</a> command 
         * the same number of times it has been suspended. 
         */
        static class Suspend {
            static final int COMMAND = 8;

            static Suspend process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Suspend"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static Suspend waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Suspend(vm, ps);
            }


            private Suspend(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Suspend"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Resumes execution of the application after the suspend 
         * command or an event has stopped it. 
         * Suspensions of the Virtual Machine and individual threads are 
         * counted. If a particular thread is suspended n times, it must 
         * resumed n times before it will continue. 
         */
        static class Resume {
            static final int COMMAND = 9;

            static Resume process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Resume"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static Resume waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Resume(vm, ps);
            }


            private Resume(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Resume"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Terminates the target VM with the given exit code. 
         * On some platforms, the exit code might be truncated, for 
         * example, to the low order 8 bits. 
         * All ids previously returned from the target VM become invalid. 
         * Threads running in the VM are abruptly terminated. 
         * A thread death exception is not thrown and 
         * finally blocks are not run.
         */
        static class Exit {
            static final int COMMAND = 10;

            static Exit process(VirtualMachineImpl vm, 
                                int exitCode)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, exitCode);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                int exitCode) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Exit"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 exitCode(int): " + exitCode);
                }
                ps.writeInt(exitCode);
                ps.send();
                return ps;
            }

            static Exit waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Exit(vm, ps);
            }


            private Exit(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Exit"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Creates a new string object in the target VM and returns 
         * its id. 
         */
        static class CreateString {
            static final int COMMAND = 11;

            static CreateString process(VirtualMachineImpl vm, 
                                String utf)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, utf);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                String utf) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.CreateString"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 utf(String): " + utf);
                }
                ps.writeString(utf);
                ps.send();
                return ps;
            }

            static CreateString waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new CreateString(vm, ps);
            }


            /**
             * Created string (instance of java.lang.String) 
             */
            final StringReferenceImpl stringObject;

            private CreateString(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.CreateString"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                stringObject = ps.readStringReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "stringObject(StringReferenceImpl): " + (stringObject==null?"NULL":"ref="+stringObject.ref()));
                }
            }
        }

        /**
         * Retrieve this VM's capabilities. The capabilities are returned 
         * as booleans, each indicating the presence or absence of a 
         * capability. The commands associated with each capability will 
         * return the NOT_IMPLEMENTED error if the cabability is not 
         * available.
         */
        static class Capabilities {
            static final int COMMAND = 12;

            static Capabilities process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Capabilities"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static Capabilities waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Capabilities(vm, ps);
            }


            /**
             * Can the VM watch field modification, and therefore 
             * can it send the Modification Watchpoint Event?
             */
            final boolean canWatchFieldModification;

            /**
             * Can the VM watch field access, and therefore 
             * can it send the Access Watchpoint Event?
             */
            final boolean canWatchFieldAccess;

            /**
             * Can the VM get the bytecodes of a given method? 
             */
            final boolean canGetBytecodes;

            /**
             * Can the VM determine whether a field or method is 
             * synthetic? (that is, can the VM determine if the 
             * method or the field was invented by the compiler?) 
             */
            final boolean canGetSyntheticAttribute;

            /**
             * Can the VM get the owned monitors infornation for 
             * a thread?
             */
            final boolean canGetOwnedMonitorInfo;

            /**
             * Can the VM get the current contended monitor of a thread?
             */
            final boolean canGetCurrentContendedMonitor;

            /**
             * Can the VM get the monitor information for a given object? 
             */
            final boolean canGetMonitorInfo;

            private Capabilities(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.Capabilities"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                canWatchFieldModification = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canWatchFieldModification(boolean): " + canWatchFieldModification);
                }
                canWatchFieldAccess = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canWatchFieldAccess(boolean): " + canWatchFieldAccess);
                }
                canGetBytecodes = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetBytecodes(boolean): " + canGetBytecodes);
                }
                canGetSyntheticAttribute = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetSyntheticAttribute(boolean): " + canGetSyntheticAttribute);
                }
                canGetOwnedMonitorInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetOwnedMonitorInfo(boolean): " + canGetOwnedMonitorInfo);
                }
                canGetCurrentContendedMonitor = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetCurrentContendedMonitor(boolean): " + canGetCurrentContendedMonitor);
                }
                canGetMonitorInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetMonitorInfo(boolean): " + canGetMonitorInfo);
                }
            }
        }

        /**
         * Retrieve the classpath and bootclasspath of the target VM. 
         * If the classpath is not defined, returns an empty list. If the 
         * bootclasspath is not defined returns an empty list.
         */
        static class ClassPaths {
            static final int COMMAND = 13;

            static ClassPaths process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ClassPaths"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static ClassPaths waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassPaths(vm, ps);
            }


            /**
             * Base directory used to resolve relative 
             * paths in either of the following lists.
             */
            final String baseDir;

            /**
             * Number of paths in classpath.
             */
            final String[] classpaths;

            /**
             * Number of paths in bootclasspath.
             */
            final String[] bootclasspaths;

            private ClassPaths(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ClassPaths"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                baseDir = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "baseDir(String): " + baseDir);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classpaths(String[]): " + "");
                }
                int classpathsCount = ps.readInt();
                classpaths = new String[classpathsCount];
                for (int i = 0; i < classpathsCount; i++) {;
                    classpaths[i] = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classpaths[i](String): " + classpaths[i]);
                    }
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "bootclasspaths(String[]): " + "");
                }
                int bootclasspathsCount = ps.readInt();
                bootclasspaths = new String[bootclasspathsCount];
                for (int i = 0; i < bootclasspathsCount; i++) {;
                    bootclasspaths[i] = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "bootclasspaths[i](String): " + bootclasspaths[i]);
                    }
                }
            }
        }

        /**
         * Releases a list of object IDs. For each object in the list, the 
         * following applies. 
         * The count of references held by the back-end (the reference 
         * count) will be decremented by refCnt. 
         * If thereafter the reference count is less than 
         * or equal to zero, the ID is freed. 
         * Any back-end resources associated with the freed ID may 
         * be freed, and if garbage collection was 
         * disabled for the object, it will be re-enabled. 
         * The sender of this command 
         * promises that no further commands will be sent 
         * referencing a freed ID.
         * <p>
         * Use of this command is not required. If it is not sent, 
         * resources associated with each ID will be freed by the back-end 
         * at some time after the corresponding object is garbage collected. 
         * It is most useful to use this command to reduce the load on the 
         * back-end if a very large number of 
         * objects has been retrieved from the back-end (a large array, 
         * for example) but may not be garbage collected any time soon. 
         * <p>
         * IDs may be re-used by the back-end after they 
         * have been freed with this command.
         * This description assumes reference counting, 
         * a back-end may use any implementation which operates 
         * equivalently. 
         */
        static class DisposeObjects {
            static final int COMMAND = 14;

            static class Request {

                /**
                 * The object ID
                 */
                final ObjectReferenceImpl object;

                /**
                 * The number of times this object ID has been 
                 * part of a packet received from the back-end. 
                 * An accurate count prevents the object ID 
                 * from being freed on the back-end if 
                 * it is part of an incoming packet, not yet 
                 * handled by the front-end.
                 */
                final int refCnt;

                Request(ObjectReferenceImpl object, int refCnt) {
                    this.object = object;
                    this.refCnt = refCnt;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                    }
                    ps.writeObjectRef(object.ref());
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     refCnt(int): " + refCnt);
                    }
                    ps.writeInt(refCnt);
                }
            }

            static DisposeObjects process(VirtualMachineImpl vm, 
                                Request[] requests)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, requests);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                Request[] requests) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.DisposeObjects"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 requests(Request[]): " + "");
                }
                ps.writeInt(requests.length);
                for (int i = 0; i < requests.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     requests[i](Request): " + "");
                    }
                    requests[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static DisposeObjects waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new DisposeObjects(vm, ps);
            }


            private DisposeObjects(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.DisposeObjects"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Tells the target VM to stop sending events. Events are not discarded; 
         * they are held until a subsequent ReleaseEvents command is sent. 
         * This command is useful to control the number of events sent 
         * to the debugger VM in situations where very large numbers of events 
         * are generated. 
         * While events are held by the debugger back-end, application 
         * execution may be frozen by the debugger back-end to prevent 
         * buffer overflows on the back end.
         * Responses to commands are never held and are not affected by this
         * command. If events are already being held, this command is 
         * ignored.
         */
        static class HoldEvents {
            static final int COMMAND = 15;

            static HoldEvents process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.HoldEvents"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static HoldEvents waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new HoldEvents(vm, ps);
            }


            private HoldEvents(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.HoldEvents"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Tells the target VM to continue sending events. This command is 
         * used to restore normal activity after a HoldEvents command. If 
         * there is no current HoldEvents command in effect, this command is 
         * ignored.
         */
        static class ReleaseEvents {
            static final int COMMAND = 16;

            static ReleaseEvents process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ReleaseEvents"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static ReleaseEvents waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ReleaseEvents(vm, ps);
            }


            private ReleaseEvents(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.ReleaseEvents"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Retrieve all of this VM's capabilities. The capabilities are returned 
         * as booleans, each indicating the presence or absence of a 
         * capability. The commands associated with each capability will 
         * return the NOT_IMPLEMENTED error if the cabability is not 
         * available.
         * Since JDWP version 1.4.
         */
        static class CapabilitiesNew {
            static final int COMMAND = 17;

            static CapabilitiesNew process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.CapabilitiesNew"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static CapabilitiesNew waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new CapabilitiesNew(vm, ps);
            }


            /**
             * Can the VM watch field modification, and therefore 
             * can it send the Modification Watchpoint Event?
             */
            final boolean canWatchFieldModification;

            /**
             * Can the VM watch field access, and therefore 
             * can it send the Access Watchpoint Event?
             */
            final boolean canWatchFieldAccess;

            /**
             * Can the VM get the bytecodes of a given method? 
             */
            final boolean canGetBytecodes;

            /**
             * Can the VM determine whether a field or method is 
             * synthetic? (that is, can the VM determine if the 
             * method or the field was invented by the compiler?) 
             */
            final boolean canGetSyntheticAttribute;

            /**
             * Can the VM get the owned monitors infornation for 
             * a thread?
             */
            final boolean canGetOwnedMonitorInfo;

            /**
             * Can the VM get the current contended monitor of a thread?
             */
            final boolean canGetCurrentContendedMonitor;

            /**
             * Can the VM get the monitor information for a given object? 
             */
            final boolean canGetMonitorInfo;

            /**
             * Can the VM redefine classes?
             */
            final boolean canRedefineClasses;

            /**
             * Can the VM add methods when redefining 
             * classes?
             */
            final boolean canAddMethod;

            /**
             * Can the VM redefine classes 
             * in ways that are normally restricted?
             */
            final boolean canUnrestrictedlyRedefineClasses;

            /**
             * Can the VM pop stack frames?
             */
            final boolean canPopFrames;

            /**
             * Can the VM filter events by specific object?
             */
            final boolean canUseInstanceFilters;

            /**
             * Can the VM get the source debug extension?
             */
            final boolean canGetSourceDebugExtension;

            /**
             * Can the VM request VM death events?
             */
            final boolean canRequestVMDeathEvent;

            /**
             * Can the VM set a default stratum?
             */
            final boolean canSetDefaultStratum;

            /**
             * Can the VM return instances, counts of instances of classes 
             * and referring objects?
             */
            final boolean canGetInstanceInfo;

            /**
             * Can the VM request monitor events?
             */
            final boolean canRequestMonitorEvents;

            /**
             * Can the VM get monitors with frame depth info?
             */
            final boolean canGetMonitorFrameInfo;

            /**
             * Can the VM filter class prepare events by source name?
             */
            final boolean canUseSourceNameFilters;

            /**
             * Can the VM return the constant pool information?
             */
            final boolean canGetConstantPool;

            /**
             * Can the VM force early return from a method?
             */
            final boolean canForceEarlyReturn;

            /**
             * Reserved for future capability
             */
            final boolean reserved22;

            /**
             * Reserved for future capability
             */
            final boolean reserved23;

            /**
             * Reserved for future capability
             */
            final boolean reserved24;

            /**
             * Reserved for future capability
             */
            final boolean reserved25;

            /**
             * Reserved for future capability
             */
            final boolean reserved26;

            /**
             * Reserved for future capability
             */
            final boolean reserved27;

            /**
             * Reserved for future capability
             */
            final boolean reserved28;

            /**
             * Reserved for future capability
             */
            final boolean reserved29;

            /**
             * Reserved for future capability
             */
            final boolean reserved30;

            /**
             * Reserved for future capability
             */
            final boolean reserved31;

            /**
             * Reserved for future capability
             */
            final boolean reserved32;

            private CapabilitiesNew(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.CapabilitiesNew"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                canWatchFieldModification = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canWatchFieldModification(boolean): " + canWatchFieldModification);
                }
                canWatchFieldAccess = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canWatchFieldAccess(boolean): " + canWatchFieldAccess);
                }
                canGetBytecodes = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetBytecodes(boolean): " + canGetBytecodes);
                }
                canGetSyntheticAttribute = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetSyntheticAttribute(boolean): " + canGetSyntheticAttribute);
                }
                canGetOwnedMonitorInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetOwnedMonitorInfo(boolean): " + canGetOwnedMonitorInfo);
                }
                canGetCurrentContendedMonitor = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetCurrentContendedMonitor(boolean): " + canGetCurrentContendedMonitor);
                }
                canGetMonitorInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetMonitorInfo(boolean): " + canGetMonitorInfo);
                }
                canRedefineClasses = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canRedefineClasses(boolean): " + canRedefineClasses);
                }
                canAddMethod = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canAddMethod(boolean): " + canAddMethod);
                }
                canUnrestrictedlyRedefineClasses = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canUnrestrictedlyRedefineClasses(boolean): " + canUnrestrictedlyRedefineClasses);
                }
                canPopFrames = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canPopFrames(boolean): " + canPopFrames);
                }
                canUseInstanceFilters = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canUseInstanceFilters(boolean): " + canUseInstanceFilters);
                }
                canGetSourceDebugExtension = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetSourceDebugExtension(boolean): " + canGetSourceDebugExtension);
                }
                canRequestVMDeathEvent = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canRequestVMDeathEvent(boolean): " + canRequestVMDeathEvent);
                }
                canSetDefaultStratum = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canSetDefaultStratum(boolean): " + canSetDefaultStratum);
                }
                canGetInstanceInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetInstanceInfo(boolean): " + canGetInstanceInfo);
                }
                canRequestMonitorEvents = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canRequestMonitorEvents(boolean): " + canRequestMonitorEvents);
                }
                canGetMonitorFrameInfo = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetMonitorFrameInfo(boolean): " + canGetMonitorFrameInfo);
                }
                canUseSourceNameFilters = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canUseSourceNameFilters(boolean): " + canUseSourceNameFilters);
                }
                canGetConstantPool = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canGetConstantPool(boolean): " + canGetConstantPool);
                }
                canForceEarlyReturn = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "canForceEarlyReturn(boolean): " + canForceEarlyReturn);
                }
                reserved22 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved22(boolean): " + reserved22);
                }
                reserved23 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved23(boolean): " + reserved23);
                }
                reserved24 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved24(boolean): " + reserved24);
                }
                reserved25 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved25(boolean): " + reserved25);
                }
                reserved26 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved26(boolean): " + reserved26);
                }
                reserved27 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved27(boolean): " + reserved27);
                }
                reserved28 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved28(boolean): " + reserved28);
                }
                reserved29 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved29(boolean): " + reserved29);
                }
                reserved30 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved30(boolean): " + reserved30);
                }
                reserved31 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved31(boolean): " + reserved31);
                }
                reserved32 = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "reserved32(boolean): " + reserved32);
                }
            }
        }

        /**
         * Installs new class definitions. 
         * If there are active stack frames in methods of the redefined classes in the 
         * target VM then those active frames continue to run the bytecodes of the 
         * original method. These methods are considered obsolete - see 
         * <a href="#JDWP_Method_IsObsolete">IsObsolete</a>. The methods in the 
         * redefined classes will be used for new invokes in the target VM. 
         * The original method ID refers to the redefined method. 
         * All breakpoints in the redefined classes are cleared.
         * If resetting of stack frames is desired, the 
         * <a href="#JDWP_StackFrame_PopFrames">PopFrames</a> command can be used 
         * to pop frames with obsolete methods.
         * <p>
         * Unless the canUnrestrictedlyRedefineClasses capability is present the following 
         * redefinitions are restricted: 
         * <ul>
         * <li>changing the schema (the fields)</li>
         * <li>changing the hierarchy (superclasses, interfaces)</li>
         * <li>deleting a method</li>
         * <li>changing class modifiers</li>
         * <li>changing method modifiers</li>
         * <li>changing the <code>NestHost</code> or <code>NestMembers</code> class attributes</li>
         * </ul>
         * <p>
         * Requires canRedefineClasses capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
         * In addition to the canRedefineClasses capability, the target VM must 
         * have the canAddMethod capability to add methods when redefining classes, 
         * or the canUnrestrictedlyRedefineClasses capability to redefine classes in ways 
         * that are normally restricted.
         */
        static class RedefineClasses {
            static final int COMMAND = 18;

            static class ClassDef {

                /**
                 * The reference type.
                 */
                final ReferenceTypeImpl refType;

                /**
                 * Number of bytes defining class (below)
                 */
                final byte[] classfile;

                ClassDef(ReferenceTypeImpl refType, byte[] classfile) {
                    this.refType = refType;
                    this.classfile = classfile;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                    }
                    ps.writeClassRef(refType.ref());
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     classfile(byte[]): " + "");
                    }
                    ps.writeInt(classfile.length);
                    for (int i = 0; i < classfile.length; i++) {;
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         classfile[i](byte): " + classfile[i]);
                        }
                        ps.writeByte(classfile[i]);
                    }
                }
            }

            static RedefineClasses process(VirtualMachineImpl vm, 
                                ClassDef[] classes)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, classes);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassDef[] classes) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.RedefineClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 classes(ClassDef[]): " + "");
                }
                ps.writeInt(classes.length);
                for (int i = 0; i < classes.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     classes[i](ClassDef): " + "");
                    }
                    classes[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static RedefineClasses waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new RedefineClasses(vm, ps);
            }


            private RedefineClasses(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.RedefineClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Set the default stratum. Requires canSetDefaultStratum capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SetDefaultStratum {
            static final int COMMAND = 19;

            static SetDefaultStratum process(VirtualMachineImpl vm, 
                                String stratumID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, stratumID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                String stratumID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.SetDefaultStratum"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 stratumID(String): " + stratumID);
                }
                ps.writeString(stratumID);
                ps.send();
                return ps;
            }

            static SetDefaultStratum waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SetDefaultStratum(vm, ps);
            }


            private SetDefaultStratum(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.SetDefaultStratum"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Returns reference types for all classes currently loaded by the 
         * target VM.  
         * Both the JNI signature and the generic signature are 
         * returned for each class.  
         * Generic signatures are described in the signature attribute 
         * section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Since JDWP version 1.5.
         */
        static class AllClassesWithGeneric {
            static final int COMMAND = 20;

            static AllClassesWithGeneric process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllClassesWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static AllClassesWithGeneric waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new AllClassesWithGeneric(vm, ps);
            }

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                final byte refTypeTag;

                /**
                 * Loaded reference type
                 */
                final long typeID;

                /**
                 * The JNI signature of the loaded reference type.
                 */
                final String signature;

                /**
                 * The generic signature of the loaded reference type 
                 * or an empty string if there is none.
                 */
                final String genericSignature;

                /**
                 * The current class 
                 * <a href="#JDWP_ClassStatus">status.</a> 
                 */
                final int status;

                private ClassInfo(VirtualMachineImpl vm, PacketStream ps) {
                    refTypeTag = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "refTypeTag(byte): " + refTypeTag);
                    }
                    typeID = ps.readClassRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "typeID(long): " + "ref="+typeID);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    genericSignature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "genericSignature(String): " + genericSignature);
                    }
                    status = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "status(int): " + status);
                    }
                }
            }


            /**
             * Number of reference types that follow.
             */
            final ClassInfo[] classes;

            private AllClassesWithGeneric(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllClassesWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classes(ClassInfo[]): " + "");
                }
                int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classes[i](ClassInfo): " + "");
                    }
                    classes[i] = new ClassInfo(vm, ps);
                }
            }
        }

        /**
         * Returns the number of instances of each reference type in the input list. 
         * Only instances that are reachable for the purposes of 
         * garbage collection are counted.  If a reference type is invalid, 
         * eg. it has been unloaded, zero is returned for its instance count.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class InstanceCounts {
            static final int COMMAND = 21;

            static InstanceCounts process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl[] refTypesCount)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refTypesCount);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl[] refTypesCount) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.InstanceCounts"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refTypesCount(ReferenceTypeImpl[]): " + "");
                }
                ps.writeInt(refTypesCount.length);
                for (int i = 0; i < refTypesCount.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     refTypesCount[i](ReferenceTypeImpl): " + (refTypesCount[i]==null?"NULL":"ref="+refTypesCount[i].ref()));
                    }
                    ps.writeClassRef(refTypesCount[i].ref());
                }
                ps.send();
                return ps;
            }

            static InstanceCounts waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new InstanceCounts(vm, ps);
            }


            /**
             * The number of counts that follow.
             */
            final long[] counts;

            private InstanceCounts(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.InstanceCounts"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "counts(long[]): " + "");
                }
                int countsCount = ps.readInt();
                counts = new long[countsCount];
                for (int i = 0; i < countsCount; i++) {;
                    counts[i] = ps.readLong();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "counts[i](long): " + counts[i]);
                    }
                }
            }
        }

        /**
         * Returns all modules in the target VM.
         * <p>Since JDWP version 9.
         */
        static class AllModules {
            static final int COMMAND = 22;

            static AllModules process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllModules"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static AllModules waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new AllModules(vm, ps);
            }


            /**
             * The number of the modules that follow.
             */
            final ModuleReferenceImpl[] modules;

            private AllModules(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.VirtualMachine.AllModules"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "modules(ModuleReferenceImpl[]): " + "");
                }
                int modulesCount = ps.readInt();
                modules = new ModuleReferenceImpl[modulesCount];
                for (int i = 0; i < modulesCount; i++) {;
                    modules[i] = ps.readModule();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "modules[i](ModuleReferenceImpl): " + (modules[i]==null?"NULL":"ref="+modules[i].ref()));
                    }
                }
            }
        }
    }

    static class ReferenceType {
        static final int COMMAND_SET = 2;
        private ReferenceType() {}  // hide constructor

        /**
         * Returns the JNI signature of a reference type. 
         * JNI signature formats are described in the 
         * <a href="http://java.sun.com/products/jdk/1.2/docs/guide/jni/index.html">Java Native Inteface Specification</a>
         * <p>
         * For primitive classes 
         * the returned signature is the signature of the corresponding primitive 
         * type; for example, "I" is returned as the signature of the class 
         * represented by java.lang.Integer.TYPE.
         */
        static class Signature {
            static final int COMMAND = 1;

            static Signature process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Signature"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Signature waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Signature(vm, ps);
            }


            /**
             * The JNI signature for the reference type.
             */
            final String signature;

            private Signature(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Signature"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                signature = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "signature(String): " + signature);
                }
            }
        }

        /**
         * Returns the instance of java.lang.ClassLoader which loaded 
         * a given reference type. If the reference type was loaded by the 
         * system class loader, the returned object ID is null.
         */
        static class ClassLoader {
            static final int COMMAND = 2;

            static ClassLoader process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassLoader"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static ClassLoader waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassLoader(vm, ps);
            }


            /**
             * The class loader for the reference type. 
             */
            final ClassLoaderReferenceImpl classLoader;

            private ClassLoader(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassLoader"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                classLoader = ps.readClassLoaderReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classLoader(ClassLoaderReferenceImpl): " + (classLoader==null?"NULL":"ref="+classLoader.ref()));
                }
            }
        }

        /**
         * Returns the modifiers (also known as access flags) for a reference type. 
         * The returned bit mask contains information on the declaration 
         * of the reference type. If the reference type is an array or 
         * a primitive class (for example, java.lang.Integer.TYPE), the 
         * value of the returned bit mask is undefined.
         */
        static class Modifiers {
            static final int COMMAND = 3;

            static Modifiers process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Modifiers"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Modifiers waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Modifiers(vm, ps);
            }


            /**
             * Modifier bits as defined in Chapter 4 of 
             * <cite>The Java&trade; Virtual Machine Specification</cite>
             */
            final int modBits;

            private Modifiers(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Modifiers"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                modBits = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "modBits(int): " + modBits);
                }
            }
        }

        /**
         * Returns information for each field in a reference type. 
         * Inherited fields are not included. 
         * The field list will include any synthetic fields created 
         * by the compiler. 
         * Fields are returned in the order they occur in the class file.
         */
        static class Fields {
            static final int COMMAND = 4;

            static Fields process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Fields"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Fields waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Fields(vm, ps);
            }

            static class FieldInfo {

                /**
                 * Field ID.
                 */
                final long fieldID;

                /**
                 * Name of field.
                 */
                final String name;

                /**
                 * JNI Signature of field.
                 */
                final String signature;

                /**
                 * The modifier bit flags (also known as access flags) 
                 * which provide additional information on the  
                 * field declaration. Individual flag values are 
                 * defined in Chapter 4 of 
                 * <cite>The Java&trade; Virtual Machine Specification</cite>. 
                 * In addition, The <code>0xf0000000</code> bit identifies 
                 * the field as synthetic, if the synthetic attribute 
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                final int modBits;

                private FieldInfo(VirtualMachineImpl vm, PacketStream ps) {
                    fieldID = ps.readFieldRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "fieldID(long): " + fieldID);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    modBits = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "modBits(int): " + modBits);
                    }
                }
            }


            /**
             * Number of declared fields.
             */
            final FieldInfo[] declared;

            private Fields(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Fields"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "declared(FieldInfo[]): " + "");
                }
                int declaredCount = ps.readInt();
                declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "declared[i](FieldInfo): " + "");
                    }
                    declared[i] = new FieldInfo(vm, ps);
                }
            }
        }

        /**
         * Returns information for each method in a reference type. 
         * Inherited methods are not included. The list of methods will 
         * include constructors (identified with the name "&lt;init&gt;"), 
         * the initialization method (identified with the name "&lt;clinit&gt;") 
         * if present, and any synthetic methods created by the compiler. 
         * Methods are returned in the order they occur in the class file.
         */
        static class Methods {
            static final int COMMAND = 5;

            static Methods process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Methods"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Methods waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Methods(vm, ps);
            }

            static class MethodInfo {

                /**
                 * Method ID.
                 */
                final long methodID;

                /**
                 * Name of method.
                 */
                final String name;

                /**
                 * JNI signature of method.
                 */
                final String signature;

                /**
                 * The modifier bit flags (also known as access flags) 
                 * which provide additional information on the  
                 * method declaration. Individual flag values are 
                 * defined in Chapter 4 of 
                 * <cite>The Java&trade; Virtual Machine Specification</cite>. 
                 * In addition, The <code>0xf0000000</code> bit identifies 
                 * the method as synthetic, if the synthetic attribute 
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                final int modBits;

                private MethodInfo(VirtualMachineImpl vm, PacketStream ps) {
                    methodID = ps.readMethodRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "methodID(long): " + methodID);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    modBits = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "modBits(int): " + modBits);
                    }
                }
            }


            /**
             * Number of declared methods.
             */
            final MethodInfo[] declared;

            private Methods(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Methods"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "declared(MethodInfo[]): " + "");
                }
                int declaredCount = ps.readInt();
                declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "declared[i](MethodInfo): " + "");
                    }
                    declared[i] = new MethodInfo(vm, ps);
                }
            }
        }

        /**
         * Returns the value of one or more static fields of the 
         * reference type. Each field must be member of the reference type 
         * or one of its superclasses, superinterfaces, or implemented interfaces. 
         * Access control is not enforced; for example, the values of private 
         * fields can be obtained.
         */
        static class GetValues {
            static final int COMMAND = 6;

            static class Field {

                /**
                 * A field to get
                 */
                final long fieldID;

                Field(long fieldID) {
                    this.fieldID = fieldID;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fieldID(long): " + fieldID);
                    }
                    ps.writeFieldRef(fieldID);
                }
            }

            static GetValues process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                Field[] fields)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, fields);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                Field[] fields) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 fields(Field[]): " + "");
                }
                ps.writeInt(fields.length);
                for (int i = 0; i < fields.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fields[i](Field): " + "");
                    }
                    fields[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static GetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new GetValues(vm, ps);
            }


            /**
             * The number of values returned, always equal to fields, 
             * the number of values to get.
             */
            final ValueImpl[] values;

            private GetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "values(ValueImpl[]): " + "");
                }
                int valuesCount = ps.readInt();
                values = new ValueImpl[valuesCount];
                for (int i = 0; i < valuesCount; i++) {;
                    values[i] = ps.readValue();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "values[i](ValueImpl): " + values[i]);
                    }
                }
            }
        }

        /**
         * Returns the name of source file in which a reference type was 
         * declared. 
         */
        static class SourceFile {
            static final int COMMAND = 7;

            static SourceFile process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SourceFile"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static SourceFile waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SourceFile(vm, ps);
            }


            /**
             * The source file name. No path information 
             * for the file is included
             */
            final String sourceFile;

            private SourceFile(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SourceFile"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                sourceFile = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "sourceFile(String): " + sourceFile);
                }
            }
        }

        /**
         * Returns the classes and interfaces directly nested within this type.
         * Types further nested within those types are not included. 
         */
        static class NestedTypes {
            static final int COMMAND = 8;

            static NestedTypes process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.NestedTypes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static NestedTypes waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new NestedTypes(vm, ps);
            }

            static class TypeInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                final byte refTypeTag;

                /**
                 * The nested class or interface ID.
                 */
                final long typeID;

                private TypeInfo(VirtualMachineImpl vm, PacketStream ps) {
                    refTypeTag = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "refTypeTag(byte): " + refTypeTag);
                    }
                    typeID = ps.readClassRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "typeID(long): " + "ref="+typeID);
                    }
                }
            }


            /**
             * The number of nested classes and interfaces
             */
            final TypeInfo[] classes;

            private NestedTypes(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.NestedTypes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classes(TypeInfo[]): " + "");
                }
                int classesCount = ps.readInt();
                classes = new TypeInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classes[i](TypeInfo): " + "");
                    }
                    classes[i] = new TypeInfo(vm, ps);
                }
            }
        }

        /**
         * Returns the current status of the reference type. The status 
         * indicates the extent to which the reference type has been 
         * initialized, as described in section 2.1.6 of 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * If the class is linked the PREPARED and VERIFIED bits in the returned status bits 
         * will be set. If the class is initialized the INITIALIZED bit in the returned 
         * status bits will be set. If an error occured during initialization then the 
         * ERROR bit in the returned status bits will be set. 
         * The returned status bits are undefined for array types and for 
         * primitive classes (such as java.lang.Integer.TYPE). 
         */
        static class Status {
            static final int COMMAND = 9;

            static Status process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Status"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Status waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Status(vm, ps);
            }


            /**
             * <a href="#JDWP_ClassStatus">Status</a> bits:
             * See <a href="#JDWP_ClassStatus">JDWP.ClassStatus</a>
             */
            final int status;

            private Status(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Status"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                status = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "status(int): " + status);
                }
            }
        }

        /**
         * Returns the interfaces declared as implemented by this class. 
         * Interfaces indirectly implemented (extended by the implemented 
         * interface or implemented by a superclass) are not included.
         */
        static class Interfaces {
            static final int COMMAND = 10;

            static Interfaces process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Interfaces"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Interfaces waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Interfaces(vm, ps);
            }


            /**
             * The number of implemented interfaces
             */
            final InterfaceTypeImpl[] interfaces;

            private Interfaces(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Interfaces"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "interfaces(InterfaceTypeImpl[]): " + "");
                }
                int interfacesCount = ps.readInt();
                interfaces = new InterfaceTypeImpl[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {;
                    interfaces[i] = vm.interfaceType(ps.readClassRef());
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "interfaces[i](InterfaceTypeImpl): " + (interfaces[i]==null?"NULL":"ref="+interfaces[i].ref()));
                    }
                }
            }
        }

        /**
         * Returns the class object corresponding to this type. 
         */
        static class ClassObject {
            static final int COMMAND = 11;

            static ClassObject process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassObject"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static ClassObject waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassObject(vm, ps);
            }


            /**
             * class object.
             */
            final ClassObjectReferenceImpl classObject;

            private ClassObject(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassObject"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                classObject = ps.readClassObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classObject(ClassObjectReferenceImpl): " + (classObject==null?"NULL":"ref="+classObject.ref()));
                }
            }
        }

        /**
         * Returns the value of the SourceDebugExtension attribute. 
         * Since JDWP version 1.4. Requires canGetSourceDebugExtension capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SourceDebugExtension {
            static final int COMMAND = 12;

            static SourceDebugExtension process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SourceDebugExtension"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static SourceDebugExtension waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SourceDebugExtension(vm, ps);
            }


            /**
             * extension attribute
             */
            final String extension;

            private SourceDebugExtension(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SourceDebugExtension"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                extension = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "extension(String): " + extension);
                }
            }
        }

        /**
         * Returns the JNI signature of a reference type along with the 
         * generic signature if there is one.  
         * Generic signatures are described in the signature attribute 
         * section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Since JDWP version 1.5.
         */
        static class SignatureWithGeneric {
            static final int COMMAND = 13;

            static SignatureWithGeneric process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SignatureWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static SignatureWithGeneric waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SignatureWithGeneric(vm, ps);
            }


            /**
             * The JNI signature for the reference type.
             */
            final String signature;

            /**
             * The generic signature for the reference type or an empty 
             * string if there is none.
             */
            final String genericSignature;

            private SignatureWithGeneric(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.SignatureWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                signature = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "signature(String): " + signature);
                }
                genericSignature = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "genericSignature(String): " + genericSignature);
                }
            }
        }

        /**
         * Returns information, including the generic signature if any, 
         * for each field in a reference type. 
         * Inherited fields are not included. 
         * The field list will include any synthetic fields created 
         * by the compiler. 
         * Fields are returned in the order they occur in the class file.  
         * Generic signatures are described in the signature attribute 
         * section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Since JDWP version 1.5.
         */
        static class FieldsWithGeneric {
            static final int COMMAND = 14;

            static FieldsWithGeneric process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.FieldsWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static FieldsWithGeneric waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new FieldsWithGeneric(vm, ps);
            }

            static class FieldInfo {

                /**
                 * Field ID.
                 */
                final long fieldID;

                /**
                 * The name of the field.
                 */
                final String name;

                /**
                 * The JNI signature of the field.
                 */
                final String signature;

                /**
                 * The generic signature of the 
                 * field, or an empty string if there is none.
                 */
                final String genericSignature;

                /**
                 * The modifier bit flags (also known as access flags) 
                 * which provide additional information on the  
                 * field declaration. Individual flag values are 
                 * defined in Chapter 4 of 
                 * <cite>The Java&trade; Virtual Machine Specification</cite>. 
                 * In addition, The <code>0xf0000000</code> bit identifies 
                 * the field as synthetic, if the synthetic attribute 
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                final int modBits;

                private FieldInfo(VirtualMachineImpl vm, PacketStream ps) {
                    fieldID = ps.readFieldRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "fieldID(long): " + fieldID);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    genericSignature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "genericSignature(String): " + genericSignature);
                    }
                    modBits = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "modBits(int): " + modBits);
                    }
                }
            }


            /**
             * Number of declared fields.
             */
            final FieldInfo[] declared;

            private FieldsWithGeneric(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.FieldsWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "declared(FieldInfo[]): " + "");
                }
                int declaredCount = ps.readInt();
                declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "declared[i](FieldInfo): " + "");
                    }
                    declared[i] = new FieldInfo(vm, ps);
                }
            }
        }

        /**
         * Returns information, including the generic signature if any, 
         * for each method in a reference type. 
         * Inherited methodss are not included. The list of methods will 
         * include constructors (identified with the name "&lt;init&gt;"), 
         * the initialization method (identified with the name "&lt;clinit&gt;") 
         * if present, and any synthetic methods created by the compiler. 
         * Methods are returned in the order they occur in the class file.  
         * Generic signatures are described in the signature attribute 
         * section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Since JDWP version 1.5.
         */
        static class MethodsWithGeneric {
            static final int COMMAND = 15;

            static MethodsWithGeneric process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.MethodsWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static MethodsWithGeneric waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new MethodsWithGeneric(vm, ps);
            }

            static class MethodInfo {

                /**
                 * Method ID.
                 */
                final long methodID;

                /**
                 * The name of the method.
                 */
                final String name;

                /**
                 * The JNI signature of the method.
                 */
                final String signature;

                /**
                 * The generic signature of the method, or 
                 * an empty string if there is none.
                 */
                final String genericSignature;

                /**
                 * The modifier bit flags (also known as access flags) 
                 * which provide additional information on the  
                 * method declaration. Individual flag values are 
                 * defined in Chapter 4 of 
                 * <cite>The Java&trade; Virtual Machine Specification</cite>. 
                 * In addition, The <code>0xf0000000</code> bit identifies 
                 * the method as synthetic, if the synthetic attribute 
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                final int modBits;

                private MethodInfo(VirtualMachineImpl vm, PacketStream ps) {
                    methodID = ps.readMethodRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "methodID(long): " + methodID);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    genericSignature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "genericSignature(String): " + genericSignature);
                    }
                    modBits = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "modBits(int): " + modBits);
                    }
                }
            }


            /**
             * Number of declared methods.
             */
            final MethodInfo[] declared;

            private MethodsWithGeneric(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.MethodsWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "declared(MethodInfo[]): " + "");
                }
                int declaredCount = ps.readInt();
                declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "declared[i](MethodInfo): " + "");
                    }
                    declared[i] = new MethodInfo(vm, ps);
                }
            }
        }

        /**
         * Returns instances of this reference type. 
         * Only instances that are reachable for the purposes of 
         * garbage collection are returned. 
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class Instances {
            static final int COMMAND = 16;

            static Instances process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                int maxInstances)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, maxInstances);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                int maxInstances) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Instances"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 maxInstances(int): " + maxInstances);
                }
                ps.writeInt(maxInstances);
                ps.send();
                return ps;
            }

            static Instances waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Instances(vm, ps);
            }


            /**
             * The number of instances that follow.
             */
            final ObjectReferenceImpl[] instances;

            private Instances(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Instances"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "instances(ObjectReferenceImpl[]): " + "");
                }
                int instancesCount = ps.readInt();
                instances = new ObjectReferenceImpl[instancesCount];
                for (int i = 0; i < instancesCount; i++) {;
                    instances[i] = ps.readTaggedObjectReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "instances[i](ObjectReferenceImpl): " + (instances[i]==null?"NULL":"ref="+instances[i].ref()));
                    }
                }
            }
        }

        /**
         * Returns the class file major and minor version numbers, as defined in the class 
         * file format of the Java Virtual Machine specification. 
         * <p>Since JDWP version 1.6. 
         */
        static class ClassFileVersion {
            static final int COMMAND = 17;

            static ClassFileVersion process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassFileVersion"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static ClassFileVersion waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassFileVersion(vm, ps);
            }


            /**
             * Major version number
             */
            final int majorVersion;

            /**
             * Minor version number
             */
            final int minorVersion;

            private ClassFileVersion(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ClassFileVersion"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                majorVersion = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "majorVersion(int): " + majorVersion);
                }
                minorVersion = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "minorVersion(int): " + minorVersion);
                }
            }
        }

        /**
         * Return the raw bytes of the constant pool in the format of the 
         * constant_pool item of the Class File Format in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * <p>Since JDWP version 1.6. Requires canGetConstantPool capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         * 
         */
        static class ConstantPool {
            static final int COMMAND = 18;

            static ConstantPool process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ConstantPool"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static ConstantPool waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ConstantPool(vm, ps);
            }


            /**
             * Total number of constant pool entries plus one. This 
             * corresponds to the constant_pool_count item of the 
             * Class File Format in 
             * <cite>The Java&trade; Virtual Machine Specification</cite>. 
             */
            final int count;

            final byte[] bytes;

            private ConstantPool(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.ConstantPool"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                count = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "count(int): " + count);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "bytes(byte[]): " + "");
                }
                int bytesCount = ps.readInt();
                bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {;
                    bytes[i] = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "bytes[i](byte): " + bytes[i]);
                    }
                }
            }
        }

        /**
         * Returns the module that this reference type belongs to.
         * <p>Since JDWP version 9.
         */
        static class Module {
            static final int COMMAND = 19;

            static Module process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Module"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                ps.send();
                return ps;
            }

            static Module waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Module(vm, ps);
            }


            /**
             * The module this reference type belongs to.
             */
            final ModuleReferenceImpl module;

            private Module(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ReferenceType.Module"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                module = ps.readModule();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "module(ModuleReferenceImpl): " + (module==null?"NULL":"ref="+module.ref()));
                }
            }
        }
    }

    static class ClassType {
        static final int COMMAND_SET = 3;
        private ClassType() {}  // hide constructor

        /**
         * Returns the immediate superclass of a class.
         */
        static class Superclass {
            static final int COMMAND = 1;

            static Superclass process(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, clazz);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassType.Superclass"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(ClassTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                ps.send();
                return ps;
            }

            static Superclass waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Superclass(vm, ps);
            }


            /**
             * The superclass (null if the class ID for java.lang.Object is specified).
             */
            final ClassTypeImpl superclass;

            private Superclass(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassType.Superclass"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                superclass = vm.classType(ps.readClassRef());
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "superclass(ClassTypeImpl): " + (superclass==null?"NULL":"ref="+superclass.ref()));
                }
            }
        }

        /**
         * Sets the value of one or more static fields. 
         * Each field must be member of the class type 
         * or one of its superclasses, superinterfaces, or implemented interfaces. 
         * Access control is not enforced; for example, the values of private 
         * fields can be set. Final fields cannot be set.
         * For primitive values, the value's type must match the 
         * field's type exactly. For object values, there must exist a 
         * widening reference conversion from the value's type to the
         * field's type and the field's type must be loaded. 
         */
        static class SetValues {
            static final int COMMAND = 2;

            /**
             * A Field/Value pair.
             */
            static class FieldValue {

                /**
                 * Field to set.
                 */
                final long fieldID;

                /**
                 * Value to put in the field.
                 */
                final ValueImpl value;

                FieldValue(long fieldID, ValueImpl value) {
                    this.fieldID = fieldID;
                    this.value = value;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fieldID(long): " + fieldID);
                    }
                    ps.writeFieldRef(fieldID);
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     value(ValueImpl): " + value);
                    }
                    ps.writeUntaggedValue(value);
                }
            }

            static SetValues process(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                FieldValue[] values)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, clazz, values);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                FieldValue[] values) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassType.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(ClassTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 values(FieldValue[]): " + "");
                }
                ps.writeInt(values.length);
                for (int i = 0; i < values.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     values[i](FieldValue): " + "");
                    }
                    values[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static SetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SetValues(vm, ps);
            }


            private SetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassType.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Invokes a static method. 
         * The method must be member of the class type 
         * or one of its superclasses. 
         * Access control is not enforced; for example, private 
         * methods can be invoked.
         * <p>
         * The method invocation will occur in the specified thread. 
         * Method invocation can occur only if the specified thread 
         * has been suspended by an event. 
         * Method invocation is not supported 
         * when the target VM has been suspended by the front-end. 
         * <p>
         * The specified method is invoked with the arguments in the specified 
         * argument list. 
         * The method invocation is synchronous; the reply packet is not 
         * sent until the invoked method returns in the target VM. 
         * The return value (possibly the void value) is 
         * included in the reply packet. 
         * If the invoked method throws an exception, the 
         * exception object ID is set in the reply packet; otherwise, the 
         * exception object ID is null. 
         * <p>
         * For primitive arguments, the argument value's type must match the 
         * argument's type exactly. For object arguments, there must exist a 
         * widening reference conversion from the argument value's type to the 
         * argument's type and the argument's type must be loaded. 
         * <p>
         * By default, all threads in the target VM are resumed while 
         * the method is being invoked if they were previously 
         * suspended by an event or by command. 
         * This is done to prevent the deadlocks 
         * that will occur if any of the threads own monitors 
         * that will be needed by the invoked method. It is possible that 
         * breakpoints or other events might occur during the invocation. 
         * Note, however, that this implicit resume acts exactly like 
         * the ThreadReference resume command, so if the thread's suspend 
         * count is greater than 1, it will remain in a suspended state 
         * during the invocation. By default, when the invocation completes, 
         * all threads in the target VM are suspended, regardless their state 
         * before the invocation. 
         * <p>
         * The resumption of other threads during the invoke can be prevented 
         * by specifying the INVOKE_SINGLE_THREADED 
         * bit flag in the <code>options</code> field; however, 
         * there is no protection against or recovery from the deadlocks 
         * described above, so this option should be used with great caution. 
         * Only the specified thread will be resumed (as described for all 
         * threads above). Upon completion of a single threaded invoke, the invoking thread 
         * will be suspended once again. Note that any threads started during 
         * the single threaded invocation will not be suspended when the 
         * invocation completes. 
         * <p>
         * If the target VM is disconnected during the invoke (for example, through 
         * the VirtualMachine dispose command) the method invocation continues. 
         */
        static class InvokeMethod {
            static final int COMMAND = 3;

            static InvokeMethod process(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, clazz, thread, methodID, arguments, options);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassType.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(ClassTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arguments(ValueImpl[]): " + "");
                }
                ps.writeInt(arguments.length);
                for (int i = 0; i < arguments.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     arguments[i](ValueImpl): " + arguments[i]);
                    }
                    ps.writeValue(arguments[i]);
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 options(int): " + options);
                }
                ps.writeInt(options);
                ps.send();
                return ps;
            }

            static InvokeMethod waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new InvokeMethod(vm, ps);
            }


            /**
             * The returned value.
             */
            final ValueImpl returnValue;

            /**
             * The thrown exception.
             */
            final ObjectReferenceImpl exception;

            private InvokeMethod(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassType.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                returnValue = ps.readValue();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "returnValue(ValueImpl): " + returnValue);
                }
                exception = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "exception(ObjectReferenceImpl): " + (exception==null?"NULL":"ref="+exception.ref()));
                }
            }
        }

        /**
         * Creates a new object of this type, invoking the specified 
         * constructor. The constructor method ID must be a member of 
         * the class type.
         * <p>
         * Instance creation will occur in the specified thread. 
         * Instance creation can occur only if the specified thread 
         * has been suspended by an event. 
         * Method invocation is not supported 
         * when the target VM has been suspended by the front-end. 
         * <p>
         * The specified constructor is invoked with the arguments in the specified 
         * argument list. 
         * The constructor invocation is synchronous; the reply packet is not 
         * sent until the invoked method returns in the target VM. 
         * The return value (possibly the void value) is 
         * included in the reply packet. 
         * If the constructor throws an exception, the 
         * exception object ID is set in the reply packet; otherwise, the 
         * exception object ID is null. 
         * <p>
         * For primitive arguments, the argument value's type must match the 
         * argument's type exactly. For object arguments, there must exist a 
         * widening reference conversion from the argument value's type to the 
         * argument's type and the argument's type must be loaded. 
         * <p>
         * By default, all threads in the target VM are resumed while 
         * the method is being invoked if they were previously 
         * suspended by an event or by command. 
         * This is done to prevent the deadlocks 
         * that will occur if any of the threads own monitors 
         * that will be needed by the invoked method. It is possible that 
         * breakpoints or other events might occur during the invocation. 
         * Note, however, that this implicit resume acts exactly like 
         * the ThreadReference resume command, so if the thread's suspend 
         * count is greater than 1, it will remain in a suspended state 
         * during the invocation. By default, when the invocation completes, 
         * all threads in the target VM are suspended, regardless their state 
         * before the invocation. 
         * <p>
         * The resumption of other threads during the invoke can be prevented 
         * by specifying the INVOKE_SINGLE_THREADED 
         * bit flag in the <code>options</code> field; however, 
         * there is no protection against or recovery from the deadlocks 
         * described above, so this option should be used with great caution. 
         * Only the specified thread will be resumed (as described for all 
         * threads above). Upon completion of a single threaded invoke, the invoking thread 
         * will be suspended once again. Note that any threads started during 
         * the single threaded invocation will not be suspended when the 
         * invocation completes. 
         * <p>
         * If the target VM is disconnected during the invoke (for example, through 
         * the VirtualMachine dispose command) the method invocation continues. 
         */
        static class NewInstance {
            static final int COMMAND = 4;

            static NewInstance process(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, clazz, thread, methodID, arguments, options);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassType.NewInstance"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(ClassTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arguments(ValueImpl[]): " + "");
                }
                ps.writeInt(arguments.length);
                for (int i = 0; i < arguments.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     arguments[i](ValueImpl): " + arguments[i]);
                    }
                    ps.writeValue(arguments[i]);
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 options(int): " + options);
                }
                ps.writeInt(options);
                ps.send();
                return ps;
            }

            static NewInstance waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new NewInstance(vm, ps);
            }


            /**
             * The newly created object, or null 
             * if the constructor threw an exception.
             */
            final ObjectReferenceImpl newObject;

            /**
             * The thrown exception, if any; otherwise, null.
             */
            final ObjectReferenceImpl exception;

            private NewInstance(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassType.NewInstance"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                newObject = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "newObject(ObjectReferenceImpl): " + (newObject==null?"NULL":"ref="+newObject.ref()));
                }
                exception = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "exception(ObjectReferenceImpl): " + (exception==null?"NULL":"ref="+exception.ref()));
                }
            }
        }
    }

    static class ArrayType {
        static final int COMMAND_SET = 4;
        private ArrayType() {}  // hide constructor

        /**
         * Creates a new array object of this type with a given length.
         */
        static class NewInstance {
            static final int COMMAND = 1;

            static NewInstance process(VirtualMachineImpl vm, 
                                ArrayTypeImpl arrType, 
                                int length)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, arrType, length);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ArrayTypeImpl arrType, 
                                int length) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ArrayType.NewInstance"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arrType(ArrayTypeImpl): " + (arrType==null?"NULL":"ref="+arrType.ref()));
                }
                ps.writeClassRef(arrType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 length(int): " + length);
                }
                ps.writeInt(length);
                ps.send();
                return ps;
            }

            static NewInstance waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new NewInstance(vm, ps);
            }


            /**
             * The newly created array object. 
             */
            final ObjectReferenceImpl newArray;

            private NewInstance(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ArrayType.NewInstance"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                newArray = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "newArray(ObjectReferenceImpl): " + (newArray==null?"NULL":"ref="+newArray.ref()));
                }
            }
        }
    }

    static class InterfaceType {
        static final int COMMAND_SET = 5;
        private InterfaceType() {}  // hide constructor

        /**
         * Invokes a static method. 
         * The method must not be a static initializer. 
         * The method must be a member of the interface type. 
         * <p>Since JDWP version 1.8 
         * <p>
         * The method invocation will occur in the specified thread. 
         * Method invocation can occur only if the specified thread 
         * has been suspended by an event. 
         * Method invocation is not supported 
         * when the target VM has been suspended by the front-end. 
         * <p>
         * The specified method is invoked with the arguments in the specified 
         * argument list. 
         * The method invocation is synchronous; the reply packet is not 
         * sent until the invoked method returns in the target VM. 
         * The return value (possibly the void value) is 
         * included in the reply packet. 
         * If the invoked method throws an exception, the 
         * exception object ID is set in the reply packet; otherwise, the 
         * exception object ID is null. 
         * <p>
         * For primitive arguments, the argument value's type must match the 
         * argument's type exactly. For object arguments, there must exist a 
         * widening reference conversion from the argument value's type to the 
         * argument's type and the argument's type must be loaded. 
         * <p>
         * By default, all threads in the target VM are resumed while 
         * the method is being invoked if they were previously 
         * suspended by an event or by a command. 
         * This is done to prevent the deadlocks 
         * that will occur if any of the threads own monitors 
         * that will be needed by the invoked method. It is possible that 
         * breakpoints or other events might occur during the invocation. 
         * Note, however, that this implicit resume acts exactly like 
         * the ThreadReference resume command, so if the thread's suspend 
         * count is greater than 1, it will remain in a suspended state 
         * during the invocation. By default, when the invocation completes, 
         * all threads in the target VM are suspended, regardless their state 
         * before the invocation. 
         * <p>
         * The resumption of other threads during the invoke can be prevented 
         * by specifying the INVOKE_SINGLE_THREADED 
         * bit flag in the <code>options</code> field; however, 
         * there is no protection against or recovery from the deadlocks 
         * described above, so this option should be used with great caution. 
         * Only the specified thread will be resumed (as described for all 
         * threads above). Upon completion of a single threaded invoke, the invoking thread 
         * will be suspended once again. Note that any threads started during 
         * the single threaded invocation will not be suspended when the 
         * invocation completes. 
         * <p>
         * If the target VM is disconnected during the invoke (for example, through 
         * the VirtualMachine dispose command) the method invocation continues. 
         */
        static class InvokeMethod {
            static final int COMMAND = 1;

            static InvokeMethod process(VirtualMachineImpl vm, 
                                InterfaceTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, clazz, thread, methodID, arguments, options);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                InterfaceTypeImpl clazz, 
                                ThreadReferenceImpl thread, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.InterfaceType.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(InterfaceTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arguments(ValueImpl[]): " + "");
                }
                ps.writeInt(arguments.length);
                for (int i = 0; i < arguments.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     arguments[i](ValueImpl): " + arguments[i]);
                    }
                    ps.writeValue(arguments[i]);
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 options(int): " + options);
                }
                ps.writeInt(options);
                ps.send();
                return ps;
            }

            static InvokeMethod waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new InvokeMethod(vm, ps);
            }


            /**
             * The returned value.
             */
            final ValueImpl returnValue;

            /**
             * The thrown exception.
             */
            final ObjectReferenceImpl exception;

            private InvokeMethod(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.InterfaceType.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                returnValue = ps.readValue();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "returnValue(ValueImpl): " + returnValue);
                }
                exception = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "exception(ObjectReferenceImpl): " + (exception==null?"NULL":"ref="+exception.ref()));
                }
            }
        }
    }

    static class Method {
        static final int COMMAND_SET = 6;
        private Method() {}  // hide constructor

        /**
         * Returns line number information for the method, if present. 
         * The line table maps source line numbers to the initial code index 
         * of the line. The line table 
         * is ordered by code index (from lowest to highest). The line number 
         * information is constant unless a new class definition is installed 
         * using <a href="#JDWP_VirtualMachine_RedefineClasses">RedefineClasses</a>.
         */
        static class LineTable {
            static final int COMMAND = 1;

            static LineTable process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, methodID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.Method.LineTable"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                ps.send();
                return ps;
            }

            static LineTable waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new LineTable(vm, ps);
            }

            static class LineInfo {

                /**
                 * Initial code index of the line, 
                 * start &lt;= lineCodeIndex &lt; end
                 */
                final long lineCodeIndex;

                /**
                 * Line number.
                 */
                final int lineNumber;

                private LineInfo(VirtualMachineImpl vm, PacketStream ps) {
                    lineCodeIndex = ps.readLong();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "lineCodeIndex(long): " + lineCodeIndex);
                    }
                    lineNumber = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "lineNumber(int): " + lineNumber);
                    }
                }
            }


            /**
             * Lowest valid code index for the method, >=0, or -1 if the method is native 
             */
            final long start;

            /**
             * Highest valid code index for the method, >=0, or -1 if the method is native
             */
            final long end;

            /**
             * The number of entries in the line table for this method.
             */
            final LineInfo[] lines;

            private LineTable(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Method.LineTable"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                start = ps.readLong();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "start(long): " + start);
                }
                end = ps.readLong();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "end(long): " + end);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "lines(LineInfo[]): " + "");
                }
                int linesCount = ps.readInt();
                lines = new LineInfo[linesCount];
                for (int i = 0; i < linesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "lines[i](LineInfo): " + "");
                    }
                    lines[i] = new LineInfo(vm, ps);
                }
            }
        }

        /**
         * Returns variable information for the method. The variable table 
         * includes arguments and locals declared within the method. For 
         * instance methods, the "this" reference is included in the 
         * table. Also, synthetic variables may be present. 
         */
        static class VariableTable {
            static final int COMMAND = 2;

            static VariableTable process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, methodID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.Method.VariableTable"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                ps.send();
                return ps;
            }

            static VariableTable waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new VariableTable(vm, ps);
            }

            /**
             * Information about the variable.
             */
            static class SlotInfo {

                /**
                 * First code index at which the variable is visible (unsigned). 
                 * Used in conjunction with <code>length</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> &lt;= current frame code index &lt; <code>codeIndex + length</code> 
                 */
                final long codeIndex;

                /**
                 * The variable's name.
                 */
                final String name;

                /**
                 * The variable type's JNI signature.
                 */
                final String signature;

                /**
                 * Unsigned value used in conjunction with <code>codeIndex</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> &lt;= current frame code index &lt; <code>code index + length</code> 
                 */
                final int length;

                /**
                 * The local variable's index in its frame
                 */
                final int slot;

                private SlotInfo(VirtualMachineImpl vm, PacketStream ps) {
                    codeIndex = ps.readLong();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "codeIndex(long): " + codeIndex);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    length = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "length(int): " + length);
                    }
                    slot = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "slot(int): " + slot);
                    }
                }
            }


            /**
             * The number of words in the frame used by arguments. 
             * Eight-byte arguments use two words; all others use one. 
             */
            final int argCnt;

            /**
             * The number of variables.
             */
            final SlotInfo[] slots;

            private VariableTable(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Method.VariableTable"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                argCnt = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "argCnt(int): " + argCnt);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "slots(SlotInfo[]): " + "");
                }
                int slotsCount = ps.readInt();
                slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "slots[i](SlotInfo): " + "");
                    }
                    slots[i] = new SlotInfo(vm, ps);
                }
            }
        }

        /**
         * Retrieve the method's bytecodes as defined in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Requires canGetBytecodes capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class Bytecodes {
            static final int COMMAND = 3;

            static Bytecodes process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, methodID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.Method.Bytecodes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                ps.send();
                return ps;
            }

            static Bytecodes waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Bytecodes(vm, ps);
            }


            final byte[] bytes;

            private Bytecodes(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Method.Bytecodes"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "bytes(byte[]): " + "");
                }
                int bytesCount = ps.readInt();
                bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {;
                    bytes[i] = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "bytes[i](byte): " + bytes[i]);
                    }
                }
            }
        }

        /**
         * Determine if this method is obsolete. A method is obsolete if it has been replaced 
         * by a non-equivalent method using the 
         * <a href="#JDWP_VirtualMachine_RedefineClasses">RedefineClasses</a> command. 
         * The original and redefined methods are considered equivalent if their bytecodes are 
         * the same except for indices into the constant pool and the referenced constants are 
         * equal.
         */
        static class IsObsolete {
            static final int COMMAND = 4;

            static IsObsolete process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, methodID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.Method.IsObsolete"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                ps.send();
                return ps;
            }

            static IsObsolete waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new IsObsolete(vm, ps);
            }


            /**
             * true if this method has been replaced
             * by a non-equivalent method using
             * the RedefineClasses command.
             */
            final boolean isObsolete;

            private IsObsolete(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Method.IsObsolete"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                isObsolete = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "isObsolete(boolean): " + isObsolete);
                }
            }
        }

        /**
         * Returns variable information for the method, including 
         * generic signatures for the variables. The variable table 
         * includes arguments and locals declared within the method. For 
         * instance methods, the "this" reference is included in the 
         * table. Also, synthetic variables may be present. 
         * Generic signatures are described in the signature attribute 
         * section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>. 
         * Since JDWP version 1.5.
         */
        static class VariableTableWithGeneric {
            static final int COMMAND = 5;

            static VariableTableWithGeneric process(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, refType, methodID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ReferenceTypeImpl refType, 
                                long methodID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.Method.VariableTableWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 refType(ReferenceTypeImpl): " + (refType==null?"NULL":"ref="+refType.ref()));
                }
                ps.writeClassRef(refType.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                ps.send();
                return ps;
            }

            static VariableTableWithGeneric waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new VariableTableWithGeneric(vm, ps);
            }

            /**
             * Information about the variable.
             */
            static class SlotInfo {

                /**
                 * First code index at which the variable is visible (unsigned). 
                 * Used in conjunction with <code>length</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> &lt;= current frame code index &lt; <code>codeIndex + length</code> 
                 */
                final long codeIndex;

                /**
                 * The variable's name.
                 */
                final String name;

                /**
                 * The variable type's JNI signature.
                 */
                final String signature;

                /**
                 * The variable type's generic 
                 * signature or an empty string if there is none.
                 */
                final String genericSignature;

                /**
                 * Unsigned value used in conjunction with <code>codeIndex</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> &lt;= current frame code index &lt; <code>code index + length</code> 
                 */
                final int length;

                /**
                 * The local variable's index in its frame
                 */
                final int slot;

                private SlotInfo(VirtualMachineImpl vm, PacketStream ps) {
                    codeIndex = ps.readLong();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "codeIndex(long): " + codeIndex);
                    }
                    name = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "name(String): " + name);
                    }
                    signature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "signature(String): " + signature);
                    }
                    genericSignature = ps.readString();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "genericSignature(String): " + genericSignature);
                    }
                    length = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "length(int): " + length);
                    }
                    slot = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "slot(int): " + slot);
                    }
                }
            }


            /**
             * The number of words in the frame used by arguments. 
             * Eight-byte arguments use two words; all others use one. 
             */
            final int argCnt;

            /**
             * The number of variables.
             */
            final SlotInfo[] slots;

            private VariableTableWithGeneric(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Method.VariableTableWithGeneric"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                argCnt = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "argCnt(int): " + argCnt);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "slots(SlotInfo[]): " + "");
                }
                int slotsCount = ps.readInt();
                slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "slots[i](SlotInfo): " + "");
                    }
                    slots[i] = new SlotInfo(vm, ps);
                }
            }
        }
    }

    static class Field {
        static final int COMMAND_SET = 8;
        private Field() {}  // hide constructor
    }

    static class ObjectReference {
        static final int COMMAND_SET = 9;
        private ObjectReference() {}  // hide constructor

        /**
         * Returns the runtime type of the object. 
         * The runtime type will be a class or an array. 
         */
        static class ReferenceType {
            static final int COMMAND = 1;

            static ReferenceType process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.ReferenceType"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                ps.send();
                return ps;
            }

            static ReferenceType waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ReferenceType(vm, ps);
            }


            /**
             * <a href="#JDWP_TypeTag">Kind</a> 
             * of following reference type. 
             */
            final byte refTypeTag;

            /**
             * The runtime reference type.
             */
            final long typeID;

            private ReferenceType(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.ReferenceType"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                refTypeTag = ps.readByte();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "refTypeTag(byte): " + refTypeTag);
                }
                typeID = ps.readClassRef();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "typeID(long): " + "ref="+typeID);
                }
            }
        }

        /**
         * Returns the value of one or more instance fields. 
         * Each field must be member of the object's type 
         * or one of its superclasses, superinterfaces, or implemented interfaces. 
         * Access control is not enforced; for example, the values of private 
         * fields can be obtained.
         */
        static class GetValues {
            static final int COMMAND = 2;

            static class Field {

                /**
                 * Field to get.
                 */
                final long fieldID;

                Field(long fieldID) {
                    this.fieldID = fieldID;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fieldID(long): " + fieldID);
                    }
                    ps.writeFieldRef(fieldID);
                }
            }

            static GetValues process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                Field[] fields)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object, fields);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                Field[] fields) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 fields(Field[]): " + "");
                }
                ps.writeInt(fields.length);
                for (int i = 0; i < fields.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fields[i](Field): " + "");
                    }
                    fields[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static GetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new GetValues(vm, ps);
            }


            /**
             * The number of values returned, always equal to 'fields', 
             * the number of values to get. Field values are ordered 
             * in the reply in the same order as corresponding fieldIDs 
             * in the command.
             */
            final ValueImpl[] values;

            private GetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "values(ValueImpl[]): " + "");
                }
                int valuesCount = ps.readInt();
                values = new ValueImpl[valuesCount];
                for (int i = 0; i < valuesCount; i++) {;
                    values[i] = ps.readValue();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "values[i](ValueImpl): " + values[i]);
                    }
                }
            }
        }

        /**
         * Sets the value of one or more instance fields. 
         * Each field must be member of the object's type 
         * or one of its superclasses, superinterfaces, or implemented interfaces. 
         * Access control is not enforced; for example, the values of private 
         * fields can be set. 
         * For primitive values, the value's type must match the 
         * field's type exactly. For object values, there must be a 
         * widening reference conversion from the value's type to the
         * field's type and the field's type must be loaded. 
         */
        static class SetValues {
            static final int COMMAND = 3;

            /**
             * A Field/Value pair.
             */
            static class FieldValue {

                /**
                 * Field to set.
                 */
                final long fieldID;

                /**
                 * Value to put in the field.
                 */
                final ValueImpl value;

                FieldValue(long fieldID, ValueImpl value) {
                    this.fieldID = fieldID;
                    this.value = value;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     fieldID(long): " + fieldID);
                    }
                    ps.writeFieldRef(fieldID);
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     value(ValueImpl): " + value);
                    }
                    ps.writeUntaggedValue(value);
                }
            }

            static SetValues process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                FieldValue[] values)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object, values);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                FieldValue[] values) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 values(FieldValue[]): " + "");
                }
                ps.writeInt(values.length);
                for (int i = 0; i < values.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     values[i](FieldValue): " + "");
                    }
                    values[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static SetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SetValues(vm, ps);
            }


            private SetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Returns monitor information for an object. All threads int the VM must 
         * be suspended.
         * Requires canGetMonitorInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class MonitorInfo {
            static final int COMMAND = 5;

            static MonitorInfo process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.MonitorInfo"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                ps.send();
                return ps;
            }

            static MonitorInfo waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new MonitorInfo(vm, ps);
            }


            /**
             * The monitor owner, or null if it is not currently owned.
             */
            final ThreadReferenceImpl owner;

            /**
             * The number of times the monitor has been entered.
             */
            final int entryCount;

            /**
             * The number of threads that are waiting for the monitor 
             * 0 if there is no current owner
             */
            final ThreadReferenceImpl[] waiters;

            private MonitorInfo(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.MonitorInfo"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                owner = ps.readThreadReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "owner(ThreadReferenceImpl): " + (owner==null?"NULL":"ref="+owner.ref()));
                }
                entryCount = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "entryCount(int): " + entryCount);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "waiters(ThreadReferenceImpl[]): " + "");
                }
                int waitersCount = ps.readInt();
                waiters = new ThreadReferenceImpl[waitersCount];
                for (int i = 0; i < waitersCount; i++) {;
                    waiters[i] = ps.readThreadReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "waiters[i](ThreadReferenceImpl): " + (waiters[i]==null?"NULL":"ref="+waiters[i].ref()));
                    }
                }
            }
        }

        /**
         * Invokes a instance method. 
         * The method must be member of the object's type 
         * or one of its superclasses, superinterfaces, or implemented interfaces. 
         * Access control is not enforced; for example, private 
         * methods can be invoked.
         * <p>
         * The method invocation will occur in the specified thread. 
         * Method invocation can occur only if the specified thread 
         * has been suspended by an event. 
         * Method invocation is not supported 
         * when the target VM has been suspended by the front-end. 
         * <p>
         * The specified method is invoked with the arguments in the specified 
         * argument list. 
         * The method invocation is synchronous; the reply packet is not 
         * sent until the invoked method returns in the target VM. 
         * The return value (possibly the void value) is 
         * included in the reply packet. 
         * If the invoked method throws an exception, the 
         * exception object ID is set in the reply packet; otherwise, the 
         * exception object ID is null. 
         * <p>
         * For primitive arguments, the argument value's type must match the 
         * argument's type exactly. For object arguments, there must be a 
         * widening reference conversion from the argument value's type to the 
         * argument's type and the argument's type must be loaded. 
         * <p>
         * By default, all threads in the target VM are resumed while 
         * the method is being invoked if they were previously 
         * suspended by an event or by a command. 
         * This is done to prevent the deadlocks 
         * that will occur if any of the threads own monitors 
         * that will be needed by the invoked method. It is possible that 
         * breakpoints or other events might occur during the invocation. 
         * Note, however, that this implicit resume acts exactly like 
         * the ThreadReference resume command, so if the thread's suspend 
         * count is greater than 1, it will remain in a suspended state 
         * during the invocation. By default, when the invocation completes, 
         * all threads in the target VM are suspended, regardless their state 
         * before the invocation. 
         * <p>
         * The resumption of other threads during the invoke can be prevented 
         * by specifying the INVOKE_SINGLE_THREADED 
         * bit flag in the <code>options</code> field; however, 
         * there is no protection against or recovery from the deadlocks 
         * described above, so this option should be used with great caution. 
         * Only the specified thread will be resumed (as described for all 
         * threads above). Upon completion of a single threaded invoke, the invoking thread 
         * will be suspended once again. Note that any threads started during 
         * the single threaded invocation will not be suspended when the 
         * invocation completes. 
         * <p>
         * If the target VM is disconnected during the invoke (for example, through 
         * the VirtualMachine dispose command) the method invocation continues. 
         */
        static class InvokeMethod {
            static final int COMMAND = 6;

            static InvokeMethod process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                ThreadReferenceImpl thread, 
                                ClassTypeImpl clazz, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object, thread, clazz, methodID, arguments, options);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                ThreadReferenceImpl thread, 
                                ClassTypeImpl clazz, 
                                long methodID, 
                                ValueImpl[] arguments, 
                                int options) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 clazz(ClassTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                }
                ps.writeClassRef(clazz.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 methodID(long): " + methodID);
                }
                ps.writeMethodRef(methodID);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arguments(ValueImpl[]): " + "");
                }
                ps.writeInt(arguments.length);
                for (int i = 0; i < arguments.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     arguments[i](ValueImpl): " + arguments[i]);
                    }
                    ps.writeValue(arguments[i]);
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 options(int): " + options);
                }
                ps.writeInt(options);
                ps.send();
                return ps;
            }

            static InvokeMethod waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new InvokeMethod(vm, ps);
            }


            /**
             * The returned value, or null if an exception is thrown.
             */
            final ValueImpl returnValue;

            /**
             * The thrown exception, if any.
             */
            final ObjectReferenceImpl exception;

            private InvokeMethod(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.InvokeMethod"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                returnValue = ps.readValue();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "returnValue(ValueImpl): " + returnValue);
                }
                exception = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "exception(ObjectReferenceImpl): " + (exception==null?"NULL":"ref="+exception.ref()));
                }
            }
        }

        /**
         * Prevents garbage collection for the given object. By 
         * default all objects in back-end replies may be 
         * collected at any time the target VM is running. A call to 
         * this command guarantees that the object will not be 
         * collected. The 
         * <a href="#JDWP_ObjectReference_EnableCollection">EnableCollection</a> 
         * command can be used to 
         * allow collection once again. 
         * <p>
         * Note that while the target VM is suspended, no garbage 
         * collection will occur because all threads are suspended. 
         * The typical examination of variables, fields, and arrays 
         * during the suspension is safe without explicitly disabling 
         * garbage collection. 
         * <p>
         * This method should be used sparingly, as it alters the 
         * pattern of garbage collection in the target VM and, 
         * consequently, may result in application behavior under the 
         * debugger that differs from its non-debugged behavior. 
         */
        static class DisableCollection {
            static final int COMMAND = 7;

            static DisableCollection process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.DisableCollection"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                ps.send();
                return ps;
            }

            static DisableCollection waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new DisableCollection(vm, ps);
            }


            private DisableCollection(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.DisableCollection"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Permits garbage collection for this object. By default all 
         * objects returned by JDWP may become unreachable in the target VM, 
         * and hence may be garbage collected. A call to this command is 
         * necessary only if garbage collection was previously disabled with 
         * the <a href="#JDWP_ObjectReference_DisableCollection">DisableCollection</a> 
         * command.
         */
        static class EnableCollection {
            static final int COMMAND = 8;

            static EnableCollection process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.EnableCollection"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                ps.send();
                return ps;
            }

            static EnableCollection waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new EnableCollection(vm, ps);
            }


            private EnableCollection(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.EnableCollection"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Determines whether an object has been garbage collected in the 
         * target VM. 
         */
        static class IsCollected {
            static final int COMMAND = 9;

            static IsCollected process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.IsCollected"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                ps.send();
                return ps;
            }

            static IsCollected waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new IsCollected(vm, ps);
            }


            /**
             * true if the object has been collected; false otherwise
             */
            final boolean isCollected;

            private IsCollected(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.IsCollected"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                isCollected = ps.readBoolean();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "isCollected(boolean): " + isCollected);
                }
            }
        }

        /**
         * Returns objects that directly reference this object.  
         * Only objects that are reachable for the purposes 
         * of garbage collection are returned. 
         * Note that an object can also be referenced in other ways, 
         * such as from a local variable in a stack frame, or from a JNI global 
         * reference.  Such non-object referrers are not returned by this command. 
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class ReferringObjects {
            static final int COMMAND = 10;

            static ReferringObjects process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                int maxReferrers)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, object, maxReferrers);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl object, 
                                int maxReferrers) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.ReferringObjects"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                }
                ps.writeObjectRef(object.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 maxReferrers(int): " + maxReferrers);
                }
                ps.writeInt(maxReferrers);
                ps.send();
                return ps;
            }

            static ReferringObjects waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ReferringObjects(vm, ps);
            }


            /**
             * The number of objects that follow.
             */
            final ObjectReferenceImpl[] referringObjects;

            private ReferringObjects(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ObjectReference.ReferringObjects"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "referringObjects(ObjectReferenceImpl[]): " + "");
                }
                int referringObjectsCount = ps.readInt();
                referringObjects = new ObjectReferenceImpl[referringObjectsCount];
                for (int i = 0; i < referringObjectsCount; i++) {;
                    referringObjects[i] = ps.readTaggedObjectReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "referringObjects[i](ObjectReferenceImpl): " + (referringObjects[i]==null?"NULL":"ref="+referringObjects[i].ref()));
                    }
                }
            }
        }
    }

    static class StringReference {
        static final int COMMAND_SET = 10;
        private StringReference() {}  // hide constructor

        /**
         * Returns the characters contained in the string. 
         */
        static class Value {
            static final int COMMAND = 1;

            static Value process(VirtualMachineImpl vm, 
                                ObjectReferenceImpl stringObject)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, stringObject);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ObjectReferenceImpl stringObject) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.StringReference.Value"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 stringObject(ObjectReferenceImpl): " + (stringObject==null?"NULL":"ref="+stringObject.ref()));
                }
                ps.writeObjectRef(stringObject.ref());
                ps.send();
                return ps;
            }

            static Value waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Value(vm, ps);
            }


            /**
             * UTF-8 representation of the string value.
             */
            final String stringValue;

            private Value(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.StringReference.Value"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                stringValue = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "stringValue(String): " + stringValue);
                }
            }
        }
    }

    static class ThreadReference {
        static final int COMMAND_SET = 11;
        private ThreadReference() {}  // hide constructor

        /**
         * Returns the thread name. 
         */
        static class Name {
            static final int COMMAND = 1;

            static Name process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static Name waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Name(vm, ps);
            }


            /**
             * The thread name.
             */
            final String threadName;

            private Name(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                threadName = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "threadName(String): " + threadName);
                }
            }
        }

        /**
         * Suspends the thread. 
         * <p>
         * Unlike java.lang.Thread.suspend(), suspends of both 
         * the virtual machine and individual threads are counted. Before 
         * a thread will run again, it must be resumed the same number 
         * of times it has been suspended. 
         * <p>
         * Suspending single threads with command has the same 
         * dangers java.lang.Thread.suspend(). If the suspended 
         * thread holds a monitor needed by another running thread, 
         * deadlock is possible in the target VM (at least until the 
         * suspended thread is resumed again). 
         * <p>
         * The suspended thread is guaranteed to remain suspended until 
         * resumed through one of the JDI resume methods mentioned above; 
         * the application in the target VM cannot resume the suspended thread 
         * through {@link java.lang.Thread#resume}. 
         * <p>
         * Note that this doesn't change the status of the thread (see the 
         * <a href="#JDWP_ThreadReference_Status">ThreadStatus</a> command.) 
         * For example, if it was 
         * Running, it will still appear running to other threads. 
         */
        static class Suspend {
            static final int COMMAND = 2;

            static Suspend process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Suspend"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static Suspend waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Suspend(vm, ps);
            }


            private Suspend(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Suspend"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Resumes the execution of a given thread. If this thread was 
         * not previously suspended by the front-end, 
         * calling this command has no effect. 
         * Otherwise, the count of pending suspends on this thread is 
         * decremented. If it is decremented to 0, the thread will 
         * continue to execute. 
         */
        static class Resume {
            static final int COMMAND = 3;

            static Resume process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Resume"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static Resume waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Resume(vm, ps);
            }


            private Resume(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Resume"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Returns the current status of a thread. The thread status 
         * reply indicates the thread status the last time it was running. 
         * the suspend status provides information on the thread's 
         * suspension, if any.
         */
        static class Status {
            static final int COMMAND = 4;

            static Status process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Status"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static Status waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Status(vm, ps);
            }


            /**
             * One of the thread status codes 
             * See <a href="#JDWP_ThreadStatus">JDWP.ThreadStatus</a>
             */
            final int threadStatus;

            /**
             * One of the suspend status codes 
             * See <a href="#JDWP_SuspendStatus">JDWP.SuspendStatus</a>
             */
            final int suspendStatus;

            private Status(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Status"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                threadStatus = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "threadStatus(int): " + threadStatus);
                }
                suspendStatus = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "suspendStatus(int): " + suspendStatus);
                }
            }
        }

        /**
         * Returns the thread group that contains a given thread. 
         */
        static class ThreadGroup {
            static final int COMMAND = 5;

            static ThreadGroup process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.ThreadGroup"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static ThreadGroup waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ThreadGroup(vm, ps);
            }


            /**
             * The thread group of this thread. 
             */
            final ThreadGroupReferenceImpl group;

            private ThreadGroup(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.ThreadGroup"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                group = ps.readThreadGroupReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "group(ThreadGroupReferenceImpl): " + (group==null?"NULL":"ref="+group.ref()));
                }
            }
        }

        /**
         * Returns the current call stack of a suspended thread. 
         * The sequence of frames starts with 
         * the currently executing frame, followed by its caller, 
         * and so on. The thread must be suspended, and the returned 
         * frameID is valid only while the thread is suspended. 
         */
        static class Frames {
            static final int COMMAND = 6;

            static Frames process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                int startFrame, 
                                int length)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, startFrame, length);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                int startFrame, 
                                int length) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Frames"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 startFrame(int): " + startFrame);
                }
                ps.writeInt(startFrame);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 length(int): " + length);
                }
                ps.writeInt(length);
                ps.send();
                return ps;
            }

            static Frames waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Frames(vm, ps);
            }

            static class Frame {

                /**
                 * The ID of this frame. 
                 */
                final long frameID;

                /**
                 * The current location of this frame
                 */
                final Location location;

                private Frame(VirtualMachineImpl vm, PacketStream ps) {
                    frameID = ps.readFrameRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "frameID(long): " + frameID);
                    }
                    location = ps.readLocation();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "location(Location): " + location);
                    }
                }
            }


            /**
             * The number of frames retreived
             */
            final Frame[] frames;

            private Frames(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Frames"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "frames(Frame[]): " + "");
                }
                int framesCount = ps.readInt();
                frames = new Frame[framesCount];
                for (int i = 0; i < framesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "frames[i](Frame): " + "");
                    }
                    frames[i] = new Frame(vm, ps);
                }
            }
        }

        /**
         * Returns the count of frames on this thread's stack. 
         * The thread must be suspended, and the returned 
         * count is valid only while the thread is suspended. 
         * Returns JDWP.Error.errorThreadNotSuspended if not suspended. 
         */
        static class FrameCount {
            static final int COMMAND = 7;

            static FrameCount process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.FrameCount"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static FrameCount waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new FrameCount(vm, ps);
            }


            /**
             * The count of frames on this thread's stack. 
             */
            final int frameCount;

            private FrameCount(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.FrameCount"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                frameCount = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "frameCount(int): " + frameCount);
                }
            }
        }

        /**
         * Returns the objects whose monitors have been entered by this thread. 
         * The thread must be suspended, and the returned information is 
         * relevant only while the thread is suspended. 
         * Requires canGetOwnedMonitorInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class OwnedMonitors {
            static final int COMMAND = 8;

            static OwnedMonitors process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.OwnedMonitors"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static OwnedMonitors waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new OwnedMonitors(vm, ps);
            }


            /**
             * The number of owned monitors
             */
            final ObjectReferenceImpl[] owned;

            private OwnedMonitors(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.OwnedMonitors"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "owned(ObjectReferenceImpl[]): " + "");
                }
                int ownedCount = ps.readInt();
                owned = new ObjectReferenceImpl[ownedCount];
                for (int i = 0; i < ownedCount; i++) {;
                    owned[i] = ps.readTaggedObjectReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "owned[i](ObjectReferenceImpl): " + (owned[i]==null?"NULL":"ref="+owned[i].ref()));
                    }
                }
            }
        }

        /**
         * Returns the object, if any, for which this thread is waiting. The 
         * thread may be waiting to enter a monitor, or it may be waiting, via 
         * the java.lang.Object.wait method, for another thread to invoke the 
         * notify method. 
         * The thread must be suspended, and the returned information is 
         * relevant only while the thread is suspended. 
         * Requires canGetCurrentContendedMonitor capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class CurrentContendedMonitor {
            static final int COMMAND = 9;

            static CurrentContendedMonitor process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.CurrentContendedMonitor"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static CurrentContendedMonitor waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new CurrentContendedMonitor(vm, ps);
            }


            /**
             * The contended monitor, or null if 
             * there is no current contended monitor. 
             */
            final ObjectReferenceImpl monitor;

            private CurrentContendedMonitor(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.CurrentContendedMonitor"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                monitor = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "monitor(ObjectReferenceImpl): " + (monitor==null?"NULL":"ref="+monitor.ref()));
                }
            }
        }

        /**
         * Stops the thread with an asynchronous exception. 
         */
        static class Stop {
            static final int COMMAND = 10;

            static Stop process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                ObjectReferenceImpl throwable)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, throwable);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                ObjectReferenceImpl throwable) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Stop"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 throwable(ObjectReferenceImpl): " + (throwable==null?"NULL":"ref="+throwable.ref()));
                }
                ps.writeObjectRef(throwable.ref());
                ps.send();
                return ps;
            }

            static Stop waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Stop(vm, ps);
            }


            private Stop(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Stop"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Interrupt the thread, as if done by java.lang.Thread.interrupt 
         */
        static class Interrupt {
            static final int COMMAND = 11;

            static Interrupt process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Interrupt"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static Interrupt waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Interrupt(vm, ps);
            }


            private Interrupt(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.Interrupt"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Get the suspend count for this thread. The suspend count is the  
         * number of times the thread has been suspended through the 
         * thread-level or VM-level suspend commands without a corresponding resume 
         */
        static class SuspendCount {
            static final int COMMAND = 12;

            static SuspendCount process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.SuspendCount"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static SuspendCount waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SuspendCount(vm, ps);
            }


            /**
             * The number of outstanding suspends of this thread. 
             */
            final int suspendCount;

            private SuspendCount(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.SuspendCount"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                suspendCount = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "suspendCount(int): " + suspendCount);
                }
            }
        }

        /**
         * Returns monitor objects owned by the thread, along with stack depth at which 
         * the monitor was acquired. Returns stack depth of -1  if 
         * the implementation cannot determine the stack depth 
         * (e.g., for monitors acquired by JNI MonitorEnter).
         * The thread must be suspended, and the returned information is 
         * relevant only while the thread is suspended. 
         * Requires canGetMonitorFrameInfo capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
         * <p>Since JDWP version 1.6. 
         */
        static class OwnedMonitorsStackDepthInfo {
            static final int COMMAND = 13;

            static OwnedMonitorsStackDepthInfo process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.OwnedMonitorsStackDepthInfo"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                ps.send();
                return ps;
            }

            static OwnedMonitorsStackDepthInfo waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new OwnedMonitorsStackDepthInfo(vm, ps);
            }

            static class monitor {

                /**
                 * An owned monitor
                 */
                final ObjectReferenceImpl monitor;

                /**
                 * Stack depth location where monitor was acquired
                 */
                final int stack_depth;

                private monitor(VirtualMachineImpl vm, PacketStream ps) {
                    monitor = ps.readTaggedObjectReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "monitor(ObjectReferenceImpl): " + (monitor==null?"NULL":"ref="+monitor.ref()));
                    }
                    stack_depth = ps.readInt();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "stack_depth(int): " + stack_depth);
                    }
                }
            }


            /**
             * The number of owned monitors
             */
            final monitor[] owned;

            private OwnedMonitorsStackDepthInfo(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.OwnedMonitorsStackDepthInfo"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "owned(monitor[]): " + "");
                }
                int ownedCount = ps.readInt();
                owned = new monitor[ownedCount];
                for (int i = 0; i < ownedCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "owned[i](monitor): " + "");
                    }
                    owned[i] = new monitor(vm, ps);
                }
            }
        }

        /**
         * Force a method to return before it reaches a return 
         * statement.  
         * <p>
         * The method which will return early is referred to as the 
         * called method. The called method is the current method (as 
         * defined by the Frames section in 
         * <cite>The Java&trade; Virtual Machine Specification</cite>) 
         * for the specified thread at the time this command 
         * is received. 
         * <p>
         * The specified thread must be suspended. 
         * The return occurs when execution of Java programming 
         * language code is resumed on this thread. Between sending this 
         * command and resumption of thread execution, the 
         * state of the stack is undefined. 
         * <p>
         * No further instructions are executed in the called 
         * method. Specifically, finally blocks are not executed. Note: 
         * this can cause inconsistent states in the application. 
         * <p>
         * A lock acquired by calling the called method (if it is a 
         * synchronized method) and locks acquired by entering 
         * synchronized blocks within the called method are 
         * released. Note: this does not apply to JNI locks or 
         * java.util.concurrent.locks locks. 
         * <p>
         * Events, such as MethodExit, are generated as they would be in 
         * a normal return. 
         * <p>
         * The called method must be a non-native Java programming 
         * language method. Forcing return on a thread with only one 
         * frame on the stack causes the thread to exit when resumed. 
         * <p>
         * For void methods, the value must be a void value. 
         * For methods that return primitive values, the value's type must 
         * match the return type exactly.  For object values, there must be a 
         * widening reference conversion from the value's type to the 
         * return type type and the return type must be loaded. 
         * <p>
         * Since JDWP version 1.6. Requires canForceEarlyReturn capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class ForceEarlyReturn {
            static final int COMMAND = 14;

            static ForceEarlyReturn process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                ValueImpl value)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, value);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                ValueImpl value) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.ForceEarlyReturn"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 value(ValueImpl): " + value);
                }
                ps.writeValue(value);
                ps.send();
                return ps;
            }

            static ForceEarlyReturn waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ForceEarlyReturn(vm, ps);
            }


            private ForceEarlyReturn(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadReference.ForceEarlyReturn"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }
    }

    static class ThreadGroupReference {
        static final int COMMAND_SET = 12;
        private ThreadGroupReference() {}  // hide constructor

        /**
         * Returns the thread group name. 
         */
        static class Name {
            static final int COMMAND = 1;

            static Name process(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, group);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 group(ThreadGroupReferenceImpl): " + (group==null?"NULL":"ref="+group.ref()));
                }
                ps.writeObjectRef(group.ref());
                ps.send();
                return ps;
            }

            static Name waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Name(vm, ps);
            }


            /**
             * The thread group's name.
             */
            final String groupName;

            private Name(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                groupName = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "groupName(String): " + groupName);
                }
            }
        }

        /**
         * Returns the thread group, if any, which contains a given thread group. 
         */
        static class Parent {
            static final int COMMAND = 2;

            static Parent process(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, group);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Parent"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 group(ThreadGroupReferenceImpl): " + (group==null?"NULL":"ref="+group.ref()));
                }
                ps.writeObjectRef(group.ref());
                ps.send();
                return ps;
            }

            static Parent waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Parent(vm, ps);
            }


            /**
             * The parent thread group object, or 
             * null if the given thread group 
             * is a top-level thread group
             */
            final ThreadGroupReferenceImpl parentGroup;

            private Parent(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Parent"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                parentGroup = ps.readThreadGroupReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "parentGroup(ThreadGroupReferenceImpl): " + (parentGroup==null?"NULL":"ref="+parentGroup.ref()));
                }
            }
        }

        /**
         * Returns the live threads and active thread groups directly contained 
         * in this thread group. Threads and thread groups in child 
         * thread groups are not included. 
         * A thread is alive if it has been started and has not yet been stopped. 
         * See <a href=../../../api/java/lang/ThreadGroup.html>java.lang.ThreadGroup </a>
         * for information about active ThreadGroups.
         */
        static class Children {
            static final int COMMAND = 3;

            static Children process(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, group);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadGroupReferenceImpl group) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Children"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 group(ThreadGroupReferenceImpl): " + (group==null?"NULL":"ref="+group.ref()));
                }
                ps.writeObjectRef(group.ref());
                ps.send();
                return ps;
            }

            static Children waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Children(vm, ps);
            }


            /**
             * The number of live child threads. 
             */
            final ThreadReferenceImpl[] childThreads;

            /**
             * The number of active child thread groups. 
             */
            final ThreadGroupReferenceImpl[] childGroups;

            private Children(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ThreadGroupReference.Children"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "childThreads(ThreadReferenceImpl[]): " + "");
                }
                int childThreadsCount = ps.readInt();
                childThreads = new ThreadReferenceImpl[childThreadsCount];
                for (int i = 0; i < childThreadsCount; i++) {;
                    childThreads[i] = ps.readThreadReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "childThreads[i](ThreadReferenceImpl): " + (childThreads[i]==null?"NULL":"ref="+childThreads[i].ref()));
                    }
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "childGroups(ThreadGroupReferenceImpl[]): " + "");
                }
                int childGroupsCount = ps.readInt();
                childGroups = new ThreadGroupReferenceImpl[childGroupsCount];
                for (int i = 0; i < childGroupsCount; i++) {;
                    childGroups[i] = ps.readThreadGroupReference();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "childGroups[i](ThreadGroupReferenceImpl): " + (childGroups[i]==null?"NULL":"ref="+childGroups[i].ref()));
                    }
                }
            }
        }
    }

    static class ArrayReference {
        static final int COMMAND_SET = 13;
        private ArrayReference() {}  // hide constructor

        /**
         * Returns the number of components in a given array. 
         */
        static class Length {
            static final int COMMAND = 1;

            static Length process(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, arrayObject);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.Length"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arrayObject(ArrayReferenceImpl): " + (arrayObject==null?"NULL":"ref="+arrayObject.ref()));
                }
                ps.writeObjectRef(arrayObject.ref());
                ps.send();
                return ps;
            }

            static Length waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Length(vm, ps);
            }


            /**
             * The length of the array.
             */
            final int arrayLength;

            private Length(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.Length"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                arrayLength = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "arrayLength(int): " + arrayLength);
                }
            }
        }

        /**
         * Returns a range of array components. The specified range must 
         * be within the bounds of the array. 
         */
        static class GetValues {
            static final int COMMAND = 2;

            static GetValues process(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject, 
                                int firstIndex, 
                                int length)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, arrayObject, firstIndex, length);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject, 
                                int firstIndex, 
                                int length) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arrayObject(ArrayReferenceImpl): " + (arrayObject==null?"NULL":"ref="+arrayObject.ref()));
                }
                ps.writeObjectRef(arrayObject.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 firstIndex(int): " + firstIndex);
                }
                ps.writeInt(firstIndex);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 length(int): " + length);
                }
                ps.writeInt(length);
                ps.send();
                return ps;
            }

            static GetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new GetValues(vm, ps);
            }


            /**
             * The retrieved values. If the values 
             * are objects, they are tagged-values; 
             * otherwise, they are untagged-values
             */
            final List<?> values;

            private GetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                values = ps.readArrayRegion();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "values(List<?>): " + values);
                }
            }
        }

        /**
         * Sets a range of array components. The specified range must 
         * be within the bounds of the array. 
         * For primitive values, each value's type must match the 
         * array component type exactly. For object values, there must be a 
         * widening reference conversion from the value's type to the
         * array component type and the array component type must be loaded. 
         */
        static class SetValues {
            static final int COMMAND = 3;

            static SetValues process(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject, 
                                int firstIndex, 
                                ValueImpl[] values)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, arrayObject, firstIndex, values);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ArrayReferenceImpl arrayObject, 
                                int firstIndex, 
                                ValueImpl[] values) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 arrayObject(ArrayReferenceImpl): " + (arrayObject==null?"NULL":"ref="+arrayObject.ref()));
                }
                ps.writeObjectRef(arrayObject.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 firstIndex(int): " + firstIndex);
                }
                ps.writeInt(firstIndex);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 values(ValueImpl[]): " + "");
                }
                ps.writeInt(values.length);
                for (int i = 0; i < values.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     values[i](ValueImpl): " + values[i]);
                    }
                    ps.writeUntaggedValue(values[i]);
                }
                ps.send();
                return ps;
            }

            static SetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SetValues(vm, ps);
            }


            private SetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ArrayReference.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }
    }

    static class ClassLoaderReference {
        static final int COMMAND_SET = 14;
        private ClassLoaderReference() {}  // hide constructor

        /**
         * Returns a list of all classes which this class loader has 
         * been requested to load. This class loader is considered to be 
         * an <i>initiating</i> class loader for each class in the returned 
         * list. The list contains each 
         * reference type defined by this loader and any types for which 
         * loading was delegated by this class loader to another class loader. 
         * <p>
         * The visible class list has useful properties with respect to 
         * the type namespace. A particular type name will occur at most 
         * once in the list. Each field or variable declared with that 
         * type name in a class defined by 
         * this class loader must be resolved to that single type. 
         * <p>
         * No ordering of the returned list is guaranteed. 
         */
        static class VisibleClasses {
            static final int COMMAND = 1;

            static VisibleClasses process(VirtualMachineImpl vm, 
                                ClassLoaderReferenceImpl classLoaderObject)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, classLoaderObject);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassLoaderReferenceImpl classLoaderObject) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassLoaderReference.VisibleClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 classLoaderObject(ClassLoaderReferenceImpl): " + (classLoaderObject==null?"NULL":"ref="+classLoaderObject.ref()));
                }
                ps.writeObjectRef(classLoaderObject.ref());
                ps.send();
                return ps;
            }

            static VisibleClasses waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new VisibleClasses(vm, ps);
            }

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                final byte refTypeTag;

                /**
                 * A class visible to this class loader.
                 */
                final long typeID;

                private ClassInfo(VirtualMachineImpl vm, PacketStream ps) {
                    refTypeTag = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "refTypeTag(byte): " + refTypeTag);
                    }
                    typeID = ps.readClassRef();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "typeID(long): " + "ref="+typeID);
                    }
                }
            }


            /**
             * The number of visible classes. 
             */
            final ClassInfo[] classes;

            private VisibleClasses(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassLoaderReference.VisibleClasses"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classes(ClassInfo[]): " + "");
                }
                int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "classes[i](ClassInfo): " + "");
                    }
                    classes[i] = new ClassInfo(vm, ps);
                }
            }
        }
    }

    static class EventRequest {
        static final int COMMAND_SET = 15;
        private EventRequest() {}  // hide constructor

        /**
         * Set an event request. When the event described by this request 
         * occurs, an <a href="#JDWP_Event">event</a> is sent from the 
         * target VM. If an event occurs that has not been requested then it is not sent 
         * from the target VM. The two exceptions to this are the VM Start Event and 
         * the VM Death Event which are automatically generated events - see 
         * <a href="#JDWP_Event_Composite">Composite Command</a> for further details.
         */
        static class Set {
            static final int COMMAND = 1;

            static class Modifier {
                abstract static class ModifierCommon {
                    abstract void write(PacketStream ps);
                }

                /**
                 * Modifier kind
                 */
                final byte modKind;
                ModifierCommon aModifierCommon;

                Modifier(byte modKind, ModifierCommon aModifierCommon) {
                    this.modKind = modKind;
                    this. aModifierCommon = aModifierCommon;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     modKind(byte): " + modKind);
                    }
                    ps.writeByte(modKind);
                     aModifierCommon.write(ps);
                }

                /**
                 * Limit the requested event to be reported at most once after a 
                 * given number of occurrences.  The event is not reported 
                 * the first <code>count - 1</code> times this filter is reached. 
                 * To request a one-off event, call this method with a count of 1. 
                 * <p>
                 * Once the count reaches 0, any subsequent filters in this request 
                 * are applied. If none of those filters cause the event to be 
                 * suppressed, the event is reported. Otherwise, the event is not 
                 * reported. In either case subsequent events are never reported for 
                 * this request. 
                 * This modifier can be used with any event kind.
                 */
                static class Count extends ModifierCommon {
                    static final byte ALT_ID = 1;
                    static Modifier create(int count) {
                        return new Modifier(ALT_ID, new Count(count));
                    }

                    /**
                     * Count before event. One for one-off.
                     */
                    final int count;

                    Count(int count) {
                        this.count = count;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         count(int): " + count);
                        }
                        ps.writeInt(count);
                    }
                }

                /**
                 * Conditional on expression
                 */
                static class Conditional extends ModifierCommon {
                    static final byte ALT_ID = 2;
                    static Modifier create(int exprID) {
                        return new Modifier(ALT_ID, new Conditional(exprID));
                    }

                    /**
                     * For the future
                     */
                    final int exprID;

                    Conditional(int exprID) {
                        this.exprID = exprID;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         exprID(int): " + exprID);
                        }
                        ps.writeInt(exprID);
                    }
                }

                /**
                 * Restricts reported events to 
                 * those in the given thread. 
                 * This modifier can be used with any event kind 
                 * except for class unload. 
                 */
                static class ThreadOnly extends ModifierCommon {
                    static final byte ALT_ID = 3;
                    static Modifier create(ThreadReferenceImpl thread) {
                        return new Modifier(ALT_ID, new ThreadOnly(thread));
                    }

                    /**
                     * Required thread
                     */
                    final ThreadReferenceImpl thread;

                    ThreadOnly(ThreadReferenceImpl thread) {
                        this.thread = thread;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        ps.writeObjectRef(thread.ref());
                    }
                }

                /**
                 * For class prepare events, restricts the events 
                 * generated by this request to be the 
                 * preparation of the given reference type and any subtypes. 
                 * For monitor wait and waited events, restricts the events 
                 * generated by this request to those whose monitor object 
                 * is of the given reference type or any of its subtypes. 
                 * For other events, restricts the events generated 
                 * by this request to those 
                 * whose location is in the given reference type or any of its subtypes. 
                 * An event will be generated for any location in a reference type that can 
                 * be safely cast to the given reference type. 
                 * This modifier can be used with any event kind except 
                 * class unload, thread start, and thread end. 
                 */
                static class ClassOnly extends ModifierCommon {
                    static final byte ALT_ID = 4;
                    static Modifier create(ReferenceTypeImpl clazz) {
                        return new Modifier(ALT_ID, new ClassOnly(clazz));
                    }

                    /**
                     * Required class
                     */
                    final ReferenceTypeImpl clazz;

                    ClassOnly(ReferenceTypeImpl clazz) {
                        this.clazz = clazz;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         clazz(ReferenceTypeImpl): " + (clazz==null?"NULL":"ref="+clazz.ref()));
                        }
                        ps.writeClassRef(clazz.ref());
                    }
                }

                /**
                 * Restricts reported events to those for classes whose name 
                 * matches the given restricted regular expression. 
                 * For class prepare events, the prepared class name 
                 * is matched. For class unload events, the 
                 * unloaded class name is matched. For monitor wait 
                 * and waited events, the name of the class of the 
                 * monitor object is matched. For other events, 
                 * the class name of the event's location is matched. 
                 * This modifier can be used with any event kind except 
                 * thread start and thread end. 
                 */
                static class ClassMatch extends ModifierCommon {
                    static final byte ALT_ID = 5;
                    static Modifier create(String classPattern) {
                        return new Modifier(ALT_ID, new ClassMatch(classPattern));
                    }

                    /**
                     * Required class pattern. 
                     * Matches are limited to exact matches of the 
                     * given class pattern and matches of patterns that 
                     * begin or end with '*'; for example, 
                     * "*.Foo" or "java.*". 
                     */
                    final String classPattern;

                    ClassMatch(String classPattern) {
                        this.classPattern = classPattern;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         classPattern(String): " + classPattern);
                        }
                        ps.writeString(classPattern);
                    }
                }

                /**
                 * Restricts reported events to those for classes whose name 
                 * does not match the given restricted regular expression. 
                 * For class prepare events, the prepared class name 
                 * is matched. For class unload events, the 
                 * unloaded class name is matched. For monitor wait and 
                 * waited events, the name of the class of the monitor 
                 * object is matched. For other events, 
                 * the class name of the event's location is matched. 
                 * This modifier can be used with any event kind except 
                 * thread start and thread end. 
                 */
                static class ClassExclude extends ModifierCommon {
                    static final byte ALT_ID = 6;
                    static Modifier create(String classPattern) {
                        return new Modifier(ALT_ID, new ClassExclude(classPattern));
                    }

                    /**
                     * Disallowed class pattern. 
                     * Matches are limited to exact matches of the 
                     * given class pattern and matches of patterns that 
                     * begin or end with '*'; for example, 
                     * "*.Foo" or "java.*". 
                     */
                    final String classPattern;

                    ClassExclude(String classPattern) {
                        this.classPattern = classPattern;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         classPattern(String): " + classPattern);
                        }
                        ps.writeString(classPattern);
                    }
                }

                /**
                 * Restricts reported events to those that occur at 
                 * the given location. 
                 * This modifier can be used with 
                 * breakpoint, field access, field modification, 
                 * step, and exception event kinds. 
                 */
                static class LocationOnly extends ModifierCommon {
                    static final byte ALT_ID = 7;
                    static Modifier create(Location loc) {
                        return new Modifier(ALT_ID, new LocationOnly(loc));
                    }

                    /**
                     * Required location
                     */
                    final Location loc;

                    LocationOnly(Location loc) {
                        this.loc = loc;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         loc(Location): " + loc);
                        }
                        ps.writeLocation(loc);
                    }
                }

                /**
                 * Restricts reported exceptions by their class and 
                 * whether they are caught or uncaught. 
                 * This modifier can be used with 
                 * exception event kinds only. 
                 */
                static class ExceptionOnly extends ModifierCommon {
                    static final byte ALT_ID = 8;
                    static Modifier create(ReferenceTypeImpl exceptionOrNull, boolean caught, boolean uncaught) {
                        return new Modifier(ALT_ID, new ExceptionOnly(exceptionOrNull, caught, uncaught));
                    }

                    /**
                     * Exception to report. Null (0) means report 
                     * exceptions of all types. 
                     * A non-null type restricts the reported exception 
                     * events to exceptions of the given type or 
                     * any of its subtypes. 
                     */
                    final ReferenceTypeImpl exceptionOrNull;

                    /**
                     * Report caught exceptions
                     */
                    final boolean caught;

                    /**
                     * Report uncaught exceptions. 
                     * Note that it 
                     * is not always possible to determine whether an 
                     * exception is caught or uncaught at the time it is 
                     * thrown. See the exception event catch location under 
                     * <a href="#JDWP_Event_Composite">composite events</a> 
                     * for more information. 
                     */
                    final boolean uncaught;

                    ExceptionOnly(ReferenceTypeImpl exceptionOrNull, boolean caught, boolean uncaught) {
                        this.exceptionOrNull = exceptionOrNull;
                        this.caught = caught;
                        this.uncaught = uncaught;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         exceptionOrNull(ReferenceTypeImpl): " + (exceptionOrNull==null?"NULL":"ref="+exceptionOrNull.ref()));
                        }
                        ps.writeClassRef(exceptionOrNull.ref());
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         caught(boolean): " + caught);
                        }
                        ps.writeBoolean(caught);
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         uncaught(boolean): " + uncaught);
                        }
                        ps.writeBoolean(uncaught);
                    }
                }

                /**
                 * Restricts reported events to those that occur for 
                 * a given field. 
                 * This modifier can be used with 
                 * field access and field modification event kinds only. 
                 */
                static class FieldOnly extends ModifierCommon {
                    static final byte ALT_ID = 9;
                    static Modifier create(ReferenceTypeImpl declaring, long fieldID) {
                        return new Modifier(ALT_ID, new FieldOnly(declaring, fieldID));
                    }

                    /**
                     * Type in which field is declared.
                     */
                    final ReferenceTypeImpl declaring;

                    /**
                     * Required field
                     */
                    final long fieldID;

                    FieldOnly(ReferenceTypeImpl declaring, long fieldID) {
                        this.declaring = declaring;
                        this.fieldID = fieldID;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         declaring(ReferenceTypeImpl): " + (declaring==null?"NULL":"ref="+declaring.ref()));
                        }
                        ps.writeClassRef(declaring.ref());
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         fieldID(long): " + fieldID);
                        }
                        ps.writeFieldRef(fieldID);
                    }
                }

                /**
                 * Restricts reported step events 
                 * to those which satisfy 
                 * depth and size constraints. 
                 * This modifier can be used with 
                 * step event kinds only. 
                 */
                static class Step extends ModifierCommon {
                    static final byte ALT_ID = 10;
                    static Modifier create(ThreadReferenceImpl thread, int size, int depth) {
                        return new Modifier(ALT_ID, new Step(thread, size, depth));
                    }

                    /**
                     * Thread in which to step
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * size of each step. 
                     * See <a href="#JDWP_StepSize">JDWP.StepSize</a>
                     */
                    final int size;

                    /**
                     * relative call stack limit. 
                     * See <a href="#JDWP_StepDepth">JDWP.StepDepth</a>
                     */
                    final int depth;

                    Step(ThreadReferenceImpl thread, int size, int depth) {
                        this.thread = thread;
                        this.size = size;
                        this.depth = depth;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        ps.writeObjectRef(thread.ref());
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         size(int): " + size);
                        }
                        ps.writeInt(size);
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         depth(int): " + depth);
                        }
                        ps.writeInt(depth);
                    }
                }

                /**
                 * Restricts reported events to those whose 
                 * active 'this' object is the given object. 
                 * Match value is the null object for static methods. 
                 * This modifier can be used with any event kind 
                 * except class prepare, class unload, thread start, 
                 * and thread end. Introduced in JDWP version 1.4.
                 */
                static class InstanceOnly extends ModifierCommon {
                    static final byte ALT_ID = 11;
                    static Modifier create(ObjectReferenceImpl instance) {
                        return new Modifier(ALT_ID, new InstanceOnly(instance));
                    }

                    /**
                     * Required 'this' object
                     */
                    final ObjectReferenceImpl instance;

                    InstanceOnly(ObjectReferenceImpl instance) {
                        this.instance = instance;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         instance(ObjectReferenceImpl): " + (instance==null?"NULL":"ref="+instance.ref()));
                        }
                        ps.writeObjectRef(instance.ref());
                    }
                }

                /**
                 * Restricts reported class prepare events to those 
                 * for reference types which have a source name 
                 * which matches the given restricted regular expression. 
                 * The source names are determined by the reference type's 
                 * <a href="#JDWP_ReferenceType_SourceDebugExtension"> 
                 * SourceDebugExtension</a>. 
                 * This modifier can only be used with class prepare 
                 * events. 
                 * Since JDWP version 1.6. Requires the canUseSourceNameFilters 
                 * capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 */
                static class SourceNameMatch extends ModifierCommon {
                    static final byte ALT_ID = 12;
                    static Modifier create(String sourceNamePattern) {
                        return new Modifier(ALT_ID, new SourceNameMatch(sourceNamePattern));
                    }

                    /**
                     * Required source name pattern. 
                     * Matches are limited to exact matches of the 
                     * given pattern and matches of patterns that 
                     * begin or end with '*'; for example, 
                     * "*.Foo" or "java.*". 
                     */
                    final String sourceNamePattern;

                    SourceNameMatch(String sourceNamePattern) {
                        this.sourceNamePattern = sourceNamePattern;
                    }

                    void write(PacketStream ps) {
                        if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                            ps.vm.printTrace("Sending:                         sourceNamePattern(String): " + sourceNamePattern);
                        }
                        ps.writeString(sourceNamePattern);
                    }
                }
            }

            static Set process(VirtualMachineImpl vm, 
                                byte eventKind, 
                                byte suspendPolicy, 
                                Modifier[] modifiers)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, eventKind, suspendPolicy, modifiers);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                byte eventKind, 
                                byte suspendPolicy, 
                                Modifier[] modifiers) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.EventRequest.Set"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 eventKind(byte): " + eventKind);
                }
                ps.writeByte(eventKind);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 suspendPolicy(byte): " + suspendPolicy);
                }
                ps.writeByte(suspendPolicy);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 modifiers(Modifier[]): " + "");
                }
                ps.writeInt(modifiers.length);
                for (int i = 0; i < modifiers.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     modifiers[i](Modifier): " + "");
                    }
                    modifiers[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static Set waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Set(vm, ps);
            }


            /**
             * ID of created request
             */
            final int requestID;

            private Set(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.EventRequest.Set"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                requestID = ps.readInt();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "requestID(int): " + requestID);
                }
            }
        }

        /**
         * Clear an event request. See <a href="#JDWP_EventKind">JDWP.EventKind</a> 
         * for a complete list of events that can be cleared. Only the event request matching 
         * the specified event kind and requestID is cleared. If there isn't a matching event 
         * request the command is a no-op and does not result in an error. Automatically 
         * generated events do not have a corresponding event request and may not be cleared 
         * using this command.
         */
        static class Clear {
            static final int COMMAND = 2;

            static Clear process(VirtualMachineImpl vm, 
                                byte eventKind, 
                                int requestID)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, eventKind, requestID);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                byte eventKind, 
                                int requestID) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.EventRequest.Clear"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 eventKind(byte): " + eventKind);
                }
                ps.writeByte(eventKind);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 requestID(int): " + requestID);
                }
                ps.writeInt(requestID);
                ps.send();
                return ps;
            }

            static Clear waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Clear(vm, ps);
            }


            private Clear(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.EventRequest.Clear"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Removes all set breakpoints, a no-op if there are no breakpoints set.
         */
        static class ClearAllBreakpoints {
            static final int COMMAND = 3;

            static ClearAllBreakpoints process(VirtualMachineImpl vm)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.EventRequest.ClearAllBreakpoints"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                ps.send();
                return ps;
            }

            static ClearAllBreakpoints waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClearAllBreakpoints(vm, ps);
            }


            private ClearAllBreakpoints(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.EventRequest.ClearAllBreakpoints"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }
    }

    static class StackFrame {
        static final int COMMAND_SET = 16;
        private StackFrame() {}  // hide constructor

        /**
         * Returns the value of one or more local variables in a 
         * given frame. Each variable must be visible at the frame's code index. 
         * Even if local variable information is not available, values can 
         * be retrieved if the front-end is able to 
         * determine the correct local variable index. (Typically, this 
         * index can be determined for method arguments from the method 
         * signature without access to the local variable table information.) 
         */
        static class GetValues {
            static final int COMMAND = 1;

            static class SlotInfo {

                /**
                 * The local variable's index in the frame. 
                 */
                final int slot;

                /**
                 * A <a href="#JDWP_Tag">tag</a> 
                 * identifying the type of the variable 
                 */
                final byte sigbyte;

                SlotInfo(int slot, byte sigbyte) {
                    this.slot = slot;
                    this.sigbyte = sigbyte;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     slot(int): " + slot);
                    }
                    ps.writeInt(slot);
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     sigbyte(byte): " + sigbyte);
                    }
                    ps.writeByte(sigbyte);
                }
            }

            static GetValues process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame, 
                                SlotInfo[] slots)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, frame, slots);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame, 
                                SlotInfo[] slots) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.StackFrame.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 frame(long): " + frame);
                }
                ps.writeFrameRef(frame);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 slots(SlotInfo[]): " + "");
                }
                ps.writeInt(slots.length);
                for (int i = 0; i < slots.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     slots[i](SlotInfo): " + "");
                    }
                    slots[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static GetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new GetValues(vm, ps);
            }


            /**
             * The number of values retrieved, always equal to slots, 
             * the number of values to get.
             */
            final ValueImpl[] values;

            private GetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.StackFrame.GetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "values(ValueImpl[]): " + "");
                }
                int valuesCount = ps.readInt();
                values = new ValueImpl[valuesCount];
                for (int i = 0; i < valuesCount; i++) {;
                    values[i] = ps.readValue();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "values[i](ValueImpl): " + values[i]);
                    }
                }
            }
        }

        /**
         * Sets the value of one or more local variables. 
         * Each variable must be visible at the current frame code index. 
         * For primitive values, the value's type must match the 
         * variable's type exactly. For object values, there must be a 
         * widening reference conversion from the value's type to the
         * variable's type and the variable's type must be loaded. 
         * <p>
         * Even if local variable information is not available, values can 
         * be set, if the front-end is able to 
         * determine the correct local variable index. (Typically, this
         * index can be determined for method arguments from the method 
         * signature without access to the local variable table information.) 
         */
        static class SetValues {
            static final int COMMAND = 2;

            static class SlotInfo {

                /**
                 * The slot ID. 
                 */
                final int slot;

                /**
                 * The value to set. 
                 */
                final ValueImpl slotValue;

                SlotInfo(int slot, ValueImpl slotValue) {
                    this.slot = slot;
                    this.slotValue = slotValue;
                }

                private void write(PacketStream ps) {
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     slot(int): " + slot);
                    }
                    ps.writeInt(slot);
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     slotValue(ValueImpl): " + slotValue);
                    }
                    ps.writeValue(slotValue);
                }
            }

            static SetValues process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame, 
                                SlotInfo[] slotValues)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, frame, slotValues);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame, 
                                SlotInfo[] slotValues) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.StackFrame.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 frame(long): " + frame);
                }
                ps.writeFrameRef(frame);
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 slotValues(SlotInfo[]): " + "");
                }
                ps.writeInt(slotValues.length);
                for (int i = 0; i < slotValues.length; i++) {;
                    if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                        ps.vm.printTrace("Sending:                     slotValues[i](SlotInfo): " + "");
                    }
                    slotValues[i].write(ps);
                }
                ps.send();
                return ps;
            }

            static SetValues waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new SetValues(vm, ps);
            }


            private SetValues(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.StackFrame.SetValues"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }

        /**
         * Returns the value of the 'this' reference for this frame. 
         * If the frame's method is static or native, the reply 
         * will contain the null object reference. 
         */
        static class ThisObject {
            static final int COMMAND = 3;

            static ThisObject process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, frame);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.StackFrame.ThisObject"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 frame(long): " + frame);
                }
                ps.writeFrameRef(frame);
                ps.send();
                return ps;
            }

            static ThisObject waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ThisObject(vm, ps);
            }


            /**
             * The 'this' object for this frame. 
             */
            final ObjectReferenceImpl objectThis;

            private ThisObject(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.StackFrame.ThisObject"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                objectThis = ps.readTaggedObjectReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "objectThis(ObjectReferenceImpl): " + (objectThis==null?"NULL":"ref="+objectThis.ref()));
                }
            }
        }

        /**
         * Pop the top-most stack frames of the thread stack, up to, and including 'frame'. 
         * The thread must be suspended to perform this command. 
         * The top-most stack frames are discarded and the stack frame previous to 'frame' 
         * becomes the current frame. The operand stack is restored -- the argument values 
         * are added back and if the invoke was not <code>invokestatic</code>, 
         * <code>objectref</code> is added back as well. The Java virtual machine 
         * program counter is restored to the opcode of the invoke instruction.
         * <p>
         * Since JDWP version 1.4. Requires canPopFrames capability - see 
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class PopFrames {
            static final int COMMAND = 4;

            static PopFrames process(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, thread, frame);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ThreadReferenceImpl thread, 
                                long frame) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.StackFrame.PopFrames"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                }
                ps.writeObjectRef(thread.ref());
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 frame(long): " + frame);
                }
                ps.writeFrameRef(frame);
                ps.send();
                return ps;
            }

            static PopFrames waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new PopFrames(vm, ps);
            }


            private PopFrames(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.StackFrame.PopFrames"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
            }
        }
    }

    static class ClassObjectReference {
        static final int COMMAND_SET = 17;
        private ClassObjectReference() {}  // hide constructor

        /**
         * Returns the reference type reflected by this class object.
         */
        static class ReflectedType {
            static final int COMMAND = 1;

            static ReflectedType process(VirtualMachineImpl vm, 
                                ClassObjectReferenceImpl classObject)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, classObject);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ClassObjectReferenceImpl classObject) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ClassObjectReference.ReflectedType"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 classObject(ClassObjectReferenceImpl): " + (classObject==null?"NULL":"ref="+classObject.ref()));
                }
                ps.writeObjectRef(classObject.ref());
                ps.send();
                return ps;
            }

            static ReflectedType waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ReflectedType(vm, ps);
            }


            /**
             * <a href="#JDWP_TypeTag">Kind</a> 
             * of following reference type. 
             */
            final byte refTypeTag;

            /**
             * reflected reference type
             */
            final long typeID;

            private ReflectedType(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ClassObjectReference.ReflectedType"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                refTypeTag = ps.readByte();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "refTypeTag(byte): " + refTypeTag);
                }
                typeID = ps.readClassRef();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "typeID(long): " + "ref="+typeID);
                }
            }
        }
    }

    static class ModuleReference {
        static final int COMMAND_SET = 18;
        private ModuleReference() {}  // hide constructor

        /**
         * Returns the name of this module.
         * <p>Since JDWP version 9.
         */
        static class Name {
            static final int COMMAND = 1;

            static Name process(VirtualMachineImpl vm, 
                                ModuleReferenceImpl module)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, module);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ModuleReferenceImpl module) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ModuleReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 module(ModuleReferenceImpl): " + (module==null?"NULL":"ref="+module.ref()));
                }
                ps.writeModuleRef(module.ref());
                ps.send();
                return ps;
            }

            static Name waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new Name(vm, ps);
            }


            /**
             * The module's name.
             */
            final String name;

            private Name(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ModuleReference.Name"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                name = ps.readString();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "name(String): " + name);
                }
            }
        }

        /**
         * Returns the class loader of this module.
         * <p>Since JDWP version 9.
         */
        static class ClassLoader {
            static final int COMMAND = 2;

            static ClassLoader process(VirtualMachineImpl vm, 
                                ModuleReferenceImpl module)
                                    throws JDWPException {
                PacketStream ps = enqueueCommand(vm, module);
                return waitForReply(vm, ps);
            }

            static PacketStream enqueueCommand(VirtualMachineImpl vm, 
                                ModuleReferenceImpl module) {
                PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);
                if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    vm.printTrace("Sending Command(id=" + ps.pkt.id + ") JDWP.ModuleReference.ClassLoader"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:""));
                }
                if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {
                    ps.vm.printTrace("Sending:                 module(ModuleReferenceImpl): " + (module==null?"NULL":"ref="+module.ref()));
                }
                ps.writeModuleRef(module.ref());
                ps.send();
                return ps;
            }

            static ClassLoader waitForReply(VirtualMachineImpl vm, PacketStream ps)
                                    throws JDWPException {
                ps.waitForReply();
                return new ClassLoader(vm, ps);
            }


            /**
             * The module's class loader.
             */
            final ClassLoaderReferenceImpl classLoader;

            private ClassLoader(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.ModuleReference.ClassLoader"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                classLoader = ps.readClassLoaderReference();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "classLoader(ClassLoaderReferenceImpl): " + (classLoader==null?"NULL":"ref="+classLoader.ref()));
                }
            }
        }
    }

    static class Event {
        static final int COMMAND_SET = 64;
        private Event() {}  // hide constructor

        /**
         * Several events may occur at a given time in the target VM. 
         * For example, there may be more than one breakpoint request 
         * for a given location 
         * or you might single step to the same location as a 
         * breakpoint request.  These events are delivered 
         * together as a composite event.  For uniformity, a 
         * composite event is always used 
         * to deliver events, even if there is only one event to report. 
         * <P>
         * The events that are grouped in a composite event are restricted in the 
         * following ways: 
         * <UL>
         * <LI>Only with other thread start events for the same thread:
         *     <UL>
         *     <LI>Thread Start Event
         *     </UL>
         * <LI>Only with other thread death events for the same thread:
         *     <UL>
         *     <LI>Thread Death Event
         *     </UL>
         * <LI>Only with other class prepare events for the same class:
         *     <UL>
         *     <LI>Class Prepare Event
         *     </UL>
         * <LI>Only with other class unload events for the same class:
         *     <UL>
         *     <LI>Class Unload Event
         *     </UL>
         * <LI>Only with other access watchpoint events for the same field access:
         *     <UL>
         *     <LI>Access Watchpoint Event
         *     </UL>
         * <LI>Only with other modification watchpoint events for the same field 
         * modification:
         *     <UL>
         *     <LI>Modification Watchpoint Event
         *     </UL>
         * <LI>Only with other Monitor contended enter events for the same monitor object: 
         *     <UL>
         *     <LI>Monitor Contended Enter Event
         *     </UL>
         * <LI>Only with other Monitor contended entered events for the same monitor object: 
         *     <UL>
         *     <LI>Monitor Contended Entered Event
         *     </UL>
         * <LI>Only with other Monitor wait events for the same monitor object: 
         *     <UL>
         *     <LI>Monitor Wait Event
         *     </UL>
         * <LI>Only with other Monitor waited events for the same monitor object: 
         *     <UL>
         *     <LI>Monitor Waited Event
         *     </UL>
         * <LI>Only with other ExceptionEvents for the same exception occurrance:
         *     <UL>
         *     <LI>ExceptionEvent
         *     </UL>
         * <LI>Only with other members of this group, at the same location 
         * and in the same thread: 
         *     <UL>
         *     <LI>Breakpoint Event
         *     <LI>Step Event
         *     <LI>Method Entry Event
         *     <LI>Method Exit Event
         *     </UL>
         * </UL>
         * <P>
         * The VM Start Event and VM Death Event are automatically generated events. 
         * This means they do not need to be requested using the 
         * <a href="#JDWP_EventRequest_Set">EventRequest.Set</a> command. 
         * The VM Start event signals the completion of VM initialization. The VM Death 
         * event signals the termination of the VM.
         * If there is a debugger connected at the time when an automatically generated 
         * event occurs it is sent from the target VM. Automatically generated events may 
         * also be requested using the EventRequest.Set command and thus multiple events 
         * of the same event kind will be sent from the target VM when an event occurs.
         * Automatically generated events are sent with the requestID field 
         * in the Event Data set to 0. The value of the suspendPolicy field in the 
         * Event Data depends on the event. For the automatically generated VM Start 
         * Event the value of suspendPolicy is not defined and is therefore implementation 
         * or configuration specific. In the Sun implementation, for example, the 
         * suspendPolicy is specified as an option to the JDWP agent at launch-time.
         * The automatically generated VM Death Event will have the suspendPolicy set to 
         * NONE.
         */
        static class Composite {
            static final int COMMAND = 100;

            static class Events {
                abstract static class EventsCommon {
                    abstract byte eventKind();
                }

                /**
                 * Event kind selector
                 */
                final byte eventKind;
                EventsCommon aEventsCommon;

                Events(VirtualMachineImpl vm, PacketStream ps) {
                    eventKind = ps.readByte();
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "eventKind(byte): " + eventKind);
                    }
                    switch (eventKind) {
                        case JDWP.EventKind.VM_START:
                             aEventsCommon = new VMStart(vm, ps);
                            break;
                        case JDWP.EventKind.SINGLE_STEP:
                             aEventsCommon = new SingleStep(vm, ps);
                            break;
                        case JDWP.EventKind.BREAKPOINT:
                             aEventsCommon = new Breakpoint(vm, ps);
                            break;
                        case JDWP.EventKind.METHOD_ENTRY:
                             aEventsCommon = new MethodEntry(vm, ps);
                            break;
                        case JDWP.EventKind.METHOD_EXIT:
                             aEventsCommon = new MethodExit(vm, ps);
                            break;
                        case JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                             aEventsCommon = new MethodExitWithReturnValue(vm, ps);
                            break;
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTER:
                             aEventsCommon = new MonitorContendedEnter(vm, ps);
                            break;
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTERED:
                             aEventsCommon = new MonitorContendedEntered(vm, ps);
                            break;
                        case JDWP.EventKind.MONITOR_WAIT:
                             aEventsCommon = new MonitorWait(vm, ps);
                            break;
                        case JDWP.EventKind.MONITOR_WAITED:
                             aEventsCommon = new MonitorWaited(vm, ps);
                            break;
                        case JDWP.EventKind.EXCEPTION:
                             aEventsCommon = new Exception(vm, ps);
                            break;
                        case JDWP.EventKind.THREAD_START:
                             aEventsCommon = new ThreadStart(vm, ps);
                            break;
                        case JDWP.EventKind.THREAD_DEATH:
                             aEventsCommon = new ThreadDeath(vm, ps);
                            break;
                        case JDWP.EventKind.CLASS_PREPARE:
                             aEventsCommon = new ClassPrepare(vm, ps);
                            break;
                        case JDWP.EventKind.CLASS_UNLOAD:
                             aEventsCommon = new ClassUnload(vm, ps);
                            break;
                        case JDWP.EventKind.FIELD_ACCESS:
                             aEventsCommon = new FieldAccess(vm, ps);
                            break;
                        case JDWP.EventKind.FIELD_MODIFICATION:
                             aEventsCommon = new FieldModification(vm, ps);
                            break;
                        case JDWP.EventKind.VM_DEATH:
                             aEventsCommon = new VMDeath(vm, ps);
                            break;
                    }
                }

                /**
                 * Notification of initialization of a target VM.  This event is 
                 * received before the main thread is started and before any 
                 * application code has been executed. Before this event occurs 
                 * a significant amount of system code has executed and a number 
                 * of system classes have been loaded. 
                 * This event is always generated by the target VM, even 
                 * if not explicitly requested.
                 */
                static class VMStart extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.VM_START;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event (or 0 if this 
                     * event is automatically generated.
                     */
                    final int requestID;

                    /**
                     * Initial thread
                     */
                    final ThreadReferenceImpl thread;

                    VMStart(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                    }
                }

                /**
                 * Notification of step completion in the target VM. The step event 
                 * is generated before the code at its location is executed. 
                 */
                static class SingleStep extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.SINGLE_STEP;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Stepped thread
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location stepped to
                     */
                    final Location location;

                    SingleStep(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a breakpoint in the target VM. The breakpoint event 
                 * is generated before the code at its location is executed. 
                 */
                static class Breakpoint extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.BREAKPOINT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which hit breakpoint
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location hit
                     */
                    final Location location;

                    Breakpoint(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a method invocation in the target VM. This event 
                 * is generated before any code in the invoked method has executed. 
                 * Method entry events are generated for both native and non-native 
                 * methods. 
                 * <P>
                 * In some VMs method entry events can occur for a particular thread 
                 * before its thread start event occurs if methods are called 
                 * as part of the thread's initialization. 
                 */
                static class MethodEntry extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_ENTRY;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which entered method
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * The initial executable location in the method.
                     */
                    final Location location;

                    MethodEntry(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a method return in the target VM. This event 
                 * is generated after all code in the method has executed, but the 
                 * location of this event is the last executed location in the method. 
                 * Method exit events are generated for both native and non-native 
                 * methods. Method exit events are not generated if the method terminates 
                 * with a thrown exception. 
                 */
                static class MethodExit extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_EXIT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which exited method
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location of exit
                     */
                    final Location location;

                    MethodExit(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a method return in the target VM. This event 
                 * is generated after all code in the method has executed, but the 
                 * location of this event is the last executed location in the method. 
                 * Method exit events are generated for both native and non-native 
                 * methods. Method exit events are not generated if the method terminates 
                 * with a thrown exception. <p>Since JDWP version 1.6. 
                 */
                static class MethodExitWithReturnValue extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which exited method
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location of exit
                     */
                    final Location location;

                    /**
                     * Value that will be returned by the method
                     */
                    final ValueImpl value;

                    MethodExitWithReturnValue(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        value = ps.readValue();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "value(ValueImpl): " + value);
                        }
                    }
                }

                /**
                 * Notification that a thread in the target VM is attempting 
                 * to enter a monitor that is already acquired by another thread. 
                 * Requires canRequestMonitorEvents capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
                 * <p>Since JDWP version 1.6. 
                 */
                static class MonitorContendedEnter extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_CONTENDED_ENTER;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which is trying to enter the monitor
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    final ObjectReferenceImpl object;

                    /**
                     * Location of contended monitor enter
                     */
                    final Location location;

                    MonitorContendedEnter(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a thread in the target VM is entering a monitor 
                 * after waiting for it to be released by another thread. 
                 * Requires canRequestMonitorEvents capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
                 * <p>Since JDWP version 1.6. 
                 */
                static class MonitorContendedEntered extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_CONTENDED_ENTERED;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which entered monitor
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    final ObjectReferenceImpl object;

                    /**
                     * Location of contended monitor enter
                     */
                    final Location location;

                    MonitorContendedEntered(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                    }
                }

                /**
                 * Notification of a thread about to wait on a monitor object. 
                 * Requires canRequestMonitorEvents capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
                 * <p>Since JDWP version 1.6. 
                 */
                static class MonitorWait extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_WAIT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which is about to wait
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    final ObjectReferenceImpl object;

                    /**
                     * Location at which the wait will occur
                     */
                    final Location location;

                    /**
                     * Thread wait time in milliseconds
                     */
                    final long timeout;

                    MonitorWait(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        timeout = ps.readLong();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "timeout(long): " + timeout);
                        }
                    }
                }

                /**
                 * Notification that a thread in the target VM has finished waiting on 
                 * Requires canRequestMonitorEvents capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>. 
                 * a monitor object. 
                 * <p>Since JDWP version 1.6. 
                 */
                static class MonitorWaited extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_WAITED;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread which waited
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    final ObjectReferenceImpl object;

                    /**
                     * Location at which the wait occured
                     */
                    final Location location;

                    /**
                     * True if timed out
                     */
                    final boolean timed_out;

                    MonitorWaited(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        timed_out = ps.readBoolean();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "timed_out(boolean): " + timed_out);
                        }
                    }
                }

                /**
                 * Notification of an exception in the target VM. 
                 * If the exception is thrown from a non-native method, 
                 * the exception event is generated at the location where the 
                 * exception is thrown. 
                 * If the exception is thrown from a native method, the exception event 
                 * is generated at the first non-native location reached after the exception 
                 * is thrown. 
                 */
                static class Exception extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.EXCEPTION;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Thread with exception
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location of exception throw 
                     * (or first non-native location after throw if thrown from a native method)
                     */
                    final Location location;

                    /**
                     * Thrown exception
                     */
                    final ObjectReferenceImpl exception;

                    /**
                     * Location of catch, or 0 if not caught. An exception 
                     * is considered to be caught if, at the point of the throw, the 
                     * current location is dynamically enclosed in a try statement that 
                     * handles the exception. (See the JVM specification for details). 
                     * If there is such a try statement, the catch location is the 
                     * first location in the appropriate catch clause. 
                     * <p>
                     * If there are native methods in the call stack at the time of the 
                     * exception, there are important restrictions to note about the 
                     * returned catch location. In such cases, 
                     * it is not possible to predict whether an exception will be handled 
                     * by some native method on the call stack. 
                     * Thus, it is possible that exceptions considered uncaught 
                     * here will, in fact, be handled by a native method and not cause 
                     * termination of the target VM. Furthermore, it cannot be assumed that the 
                     * catch location returned here will ever be reached by the throwing 
                     * thread. If there is 
                     * a native frame between the current location and the catch location, 
                     * the exception might be handled and cleared in that native method 
                     * instead. 
                     * <p>
                     * Note that compilers can generate try-catch blocks in some cases 
                     * where they are not explicit in the source code; for example, 
                     * the code generated for <code>synchronized</code> and 
                     * <code>finally</code> blocks can contain implicit try-catch blocks. 
                     * If such an implicitly generated try-catch is 
                     * present on the call stack at the time of the throw, the exception 
                     * will be considered caught even though it appears to be uncaught from 
                     * examination of the source code. 
                     */
                    final Location catchLocation;

                    Exception(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        exception = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "exception(ObjectReferenceImpl): " + (exception==null?"NULL":"ref="+exception.ref()));
                        }
                        catchLocation = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "catchLocation(Location): " + catchLocation);
                        }
                    }
                }

                /**
                 * Notification of a new running thread in the target VM. 
                 * The new thread can be the result of a call to 
                 * <code>java.lang.Thread.start</code> or the result of 
                 * attaching a new thread to the VM though JNI. The 
                 * notification is generated by the new thread some time before 
                 * its execution starts. 
                 * Because of this timing, it is possible to receive other events 
                 * for the thread before this event is received. (Notably, 
                 * Method Entry Events and Method Exit Events might occur 
                 * during thread initialization. 
                 * It is also possible for the 
                 * <a href="#JDWP_VirtualMachine_AllThreads">VirtualMachine AllThreads</a> 
                 * command to return 
                 * a thread before its thread start event is received. 
                 * <p>
                 * Note that this event gives no information 
                 * about the creation of the thread object which may have happened 
                 * much earlier, depending on the VM being debugged. 
                 */
                static class ThreadStart extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.THREAD_START;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Started thread
                     */
                    final ThreadReferenceImpl thread;

                    ThreadStart(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                    }
                }

                /**
                 * Notification of a completed thread in the target VM. The 
                 * notification is generated by the dying thread before it terminates. 
                 * Because of this timing, it is possible 
                 * for {@link VirtualMachine#allThreads} to return this thread 
                 * after this event is received. 
                 * <p>
                 * Note that this event gives no information 
                 * about the lifetime of the thread object. It may or may not be collected 
                 * soon depending on what references exist in the target VM. 
                 */
                static class ThreadDeath extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.THREAD_DEATH;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Ending thread
                     */
                    final ThreadReferenceImpl thread;

                    ThreadDeath(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                    }
                }

                /**
                 * Notification of a class prepare in the target VM. See the JVM 
                 * specification for a definition of class preparation. Class prepare 
                 * events are not generated for primtiive classes (for example, 
                 * java.lang.Integer.TYPE). 
                 */
                static class ClassPrepare extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.CLASS_PREPARE;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Preparing thread. 
                     * In rare cases, this event may occur in a debugger system 
                     * thread within the target VM. Debugger threads take precautions 
                     * to prevent these events, but they cannot be avoided under some 
                     * conditions, especially for some subclasses of 
                     * java.lang.Error. 
                     * If the event was generated by a debugger system thread, the 
                     * value returned by this method is null, and if the requested  
                     * <a href="#JDWP_SuspendPolicy">suspend policy</a> 
                     * for the event was EVENT_THREAD 
                     * all threads will be suspended instead, and the 
                     * composite event's suspend policy will reflect this change. 
                     * <p>
                     * Note that the discussion above does not apply to system threads 
                     * created by the target VM during its normal (non-debug) operation. 
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Kind of reference type. 
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    final byte refTypeTag;

                    /**
                     * Type being prepared
                     */
                    final long typeID;

                    /**
                     * Type signature
                     */
                    final String signature;

                    /**
                     * Status of type. 
                     * See <a href="#JDWP_ClassStatus">JDWP.ClassStatus</a>
                     */
                    final int status;

                    ClassPrepare(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        refTypeTag = ps.readByte();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "refTypeTag(byte): " + refTypeTag);
                        }
                        typeID = ps.readClassRef();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "typeID(long): " + "ref="+typeID);
                        }
                        signature = ps.readString();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "signature(String): " + signature);
                        }
                        status = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "status(int): " + status);
                        }
                    }
                }

                /**
                 * Notification of a class unload in the target VM. 
                 * <p>
                 * There are severe constraints on the debugger back-end during 
                 * garbage collection, so unload information is greatly limited. 
                 */
                static class ClassUnload extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.CLASS_UNLOAD;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Type signature
                     */
                    final String signature;

                    ClassUnload(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        signature = ps.readString();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "signature(String): " + signature);
                        }
                    }
                }

                /**
                 * Notification of a field access in the target VM. 
                 * Field modifications 
                 * are not considered field accesses. 
                 * Requires canWatchFieldAccess capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 */
                static class FieldAccess extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.FIELD_ACCESS;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Accessing thread
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location of access
                     */
                    final Location location;

                    /**
                     * Kind of reference type. 
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    final byte refTypeTag;

                    /**
                     * Type of field
                     */
                    final long typeID;

                    /**
                     * Field being accessed
                     */
                    final long fieldID;

                    /**
                     * Object being accessed (null=0 for statics
                     */
                    final ObjectReferenceImpl object;

                    FieldAccess(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        refTypeTag = ps.readByte();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "refTypeTag(byte): " + refTypeTag);
                        }
                        typeID = ps.readClassRef();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "typeID(long): " + "ref="+typeID);
                        }
                        fieldID = ps.readFieldRef();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "fieldID(long): " + fieldID);
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                    }
                }

                /**
                 * Notification of a field modification in the target VM. 
                 * Requires canWatchFieldModification capability - see 
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 */
                static class FieldModification extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.FIELD_MODIFICATION;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    /**
                     * Modifying thread
                     */
                    final ThreadReferenceImpl thread;

                    /**
                     * Location of modify
                     */
                    final Location location;

                    /**
                     * Kind of reference type. 
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    final byte refTypeTag;

                    /**
                     * Type of field
                     */
                    final long typeID;

                    /**
                     * Field being modified
                     */
                    final long fieldID;

                    /**
                     * Object being modified (null=0 for statics
                     */
                    final ObjectReferenceImpl object;

                    /**
                     * Value to be assigned
                     */
                    final ValueImpl valueToBe;

                    FieldModification(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                        thread = ps.readThreadReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "thread(ThreadReferenceImpl): " + (thread==null?"NULL":"ref="+thread.ref()));
                        }
                        location = ps.readLocation();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "location(Location): " + location);
                        }
                        refTypeTag = ps.readByte();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "refTypeTag(byte): " + refTypeTag);
                        }
                        typeID = ps.readClassRef();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "typeID(long): " + "ref="+typeID);
                        }
                        fieldID = ps.readFieldRef();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "fieldID(long): " + fieldID);
                        }
                        object = ps.readTaggedObjectReference();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "object(ObjectReferenceImpl): " + (object==null?"NULL":"ref="+object.ref()));
                        }
                        valueToBe = ps.readValue();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "valueToBe(ValueImpl): " + valueToBe);
                        }
                    }
                }

                static class VMDeath extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.VM_DEATH;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    final int requestID;

                    VMDeath(VirtualMachineImpl vm, PacketStream ps) {
                        requestID = ps.readInt();
                        if (vm.traceReceives) {
                            vm.printReceiveTrace(6, "requestID(int): " + requestID);
                        }
                    }
                }
            }


            /**
             * Which threads where suspended by this composite event?
             */
            final byte suspendPolicy;

            /**
             * Events in set.
             */
            final Events[] events;

            Composite(VirtualMachineImpl vm, PacketStream ps) {
                if (vm.traceReceives) {
                    vm.printTrace("Receiving Command(id=" + ps.pkt.id + ") JDWP.Event.Composite"+(ps.pkt.flags!=0?", FLAGS=" + ps.pkt.flags:"")+(ps.pkt.errorCode!=0?", ERROR CODE=" + ps.pkt.errorCode:""));
                }
                suspendPolicy = ps.readByte();
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "suspendPolicy(byte): " + suspendPolicy);
                }
                if (vm.traceReceives) {
                    vm.printReceiveTrace(4, "events(Events[]): " + "");
                }
                int eventsCount = ps.readInt();
                events = new Events[eventsCount];
                for (int i = 0; i < eventsCount; i++) {;
                    if (vm.traceReceives) {
                        vm.printReceiveTrace(5, "events[i](Events): " + "");
                    }
                    events[i] = new Events(vm, ps);
                }
            }
        }
    }

    static class Error {
        static final int NONE = 0;
        static final int INVALID_THREAD = 10;
        static final int INVALID_THREAD_GROUP = 11;
        static final int INVALID_PRIORITY = 12;
        static final int THREAD_NOT_SUSPENDED = 13;
        static final int THREAD_SUSPENDED = 14;
        static final int THREAD_NOT_ALIVE = 15;
        static final int INVALID_OBJECT = 20;
        static final int INVALID_CLASS = 21;
        static final int CLASS_NOT_PREPARED = 22;
        static final int INVALID_METHODID = 23;
        static final int INVALID_LOCATION = 24;
        static final int INVALID_FIELDID = 25;
        static final int INVALID_FRAMEID = 30;
        static final int NO_MORE_FRAMES = 31;
        static final int OPAQUE_FRAME = 32;
        static final int NOT_CURRENT_FRAME = 33;
        static final int TYPE_MISMATCH = 34;
        static final int INVALID_SLOT = 35;
        static final int DUPLICATE = 40;
        static final int NOT_FOUND = 41;
        static final int INVALID_MODULE = 42;
        static final int INVALID_MONITOR = 50;
        static final int NOT_MONITOR_OWNER = 51;
        static final int INTERRUPT = 52;
        static final int INVALID_CLASS_FORMAT = 60;
        static final int CIRCULAR_CLASS_DEFINITION = 61;
        static final int FAILS_VERIFICATION = 62;
        static final int ADD_METHOD_NOT_IMPLEMENTED = 63;
        static final int SCHEMA_CHANGE_NOT_IMPLEMENTED = 64;
        static final int INVALID_TYPESTATE = 65;
        static final int HIERARCHY_CHANGE_NOT_IMPLEMENTED = 66;
        static final int DELETE_METHOD_NOT_IMPLEMENTED = 67;
        static final int UNSUPPORTED_VERSION = 68;
        static final int NAMES_DONT_MATCH = 69;
        static final int CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 70;
        static final int METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;
        static final int CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED = 72;
        static final int NOT_IMPLEMENTED = 99;
        static final int NULL_POINTER = 100;
        static final int ABSENT_INFORMATION = 101;
        static final int INVALID_EVENT_TYPE = 102;
        static final int ILLEGAL_ARGUMENT = 103;
        static final int OUT_OF_MEMORY = 110;
        static final int ACCESS_DENIED = 111;
        static final int VM_DEAD = 112;
        static final int INTERNAL = 113;
        static final int UNATTACHED_THREAD = 115;
        static final int INVALID_TAG = 500;
        static final int ALREADY_INVOKING = 502;
        static final int INVALID_INDEX = 503;
        static final int INVALID_LENGTH = 504;
        static final int INVALID_STRING = 506;
        static final int INVALID_CLASS_LOADER = 507;
        static final int INVALID_ARRAY = 508;
        static final int TRANSPORT_LOAD = 509;
        static final int TRANSPORT_INIT = 510;
        static final int NATIVE_METHOD = 511;
        static final int INVALID_COUNT = 512;
    }

    static class EventKind {
        static final int SINGLE_STEP = 1;
        static final int BREAKPOINT = 2;
        static final int FRAME_POP = 3;
        static final int EXCEPTION = 4;
        static final int USER_DEFINED = 5;
        static final int THREAD_START = 6;
        static final int THREAD_DEATH = 7;
        static final int THREAD_END = 7;
        static final int CLASS_PREPARE = 8;
        static final int CLASS_UNLOAD = 9;
        static final int CLASS_LOAD = 10;
        static final int FIELD_ACCESS = 20;
        static final int FIELD_MODIFICATION = 21;
        static final int EXCEPTION_CATCH = 30;
        static final int METHOD_ENTRY = 40;
        static final int METHOD_EXIT = 41;
        static final int METHOD_EXIT_WITH_RETURN_VALUE = 42;
        static final int MONITOR_CONTENDED_ENTER = 43;
        static final int MONITOR_CONTENDED_ENTERED = 44;
        static final int MONITOR_WAIT = 45;
        static final int MONITOR_WAITED = 46;
        static final int VM_START = 90;
        static final int VM_INIT = 90;
        static final int VM_DEATH = 99;
        static final int VM_DISCONNECTED = 100;
    }

    static class ThreadStatus {
        static final int ZOMBIE = 0;
        static final int RUNNING = 1;
        static final int SLEEPING = 2;
        static final int MONITOR = 3;
        static final int WAIT = 4;
    }

    static class SuspendStatus {
        static final int SUSPEND_STATUS_SUSPENDED = 0x1;
    }

    static class ClassStatus {
        static final int VERIFIED = 1;
        static final int PREPARED = 2;
        static final int INITIALIZED = 4;
        static final int ERROR = 8;
    }

    static class TypeTag {
        static final int CLASS = 1;
        static final int INTERFACE = 2;
        static final int ARRAY = 3;
    }

    static class Tag {
        static final int ARRAY = 91;
        static final int BYTE = 66;
        static final int CHAR = 67;
        static final int OBJECT = 76;
        static final int FLOAT = 70;
        static final int DOUBLE = 68;
        static final int INT = 73;
        static final int LONG = 74;
        static final int SHORT = 83;
        static final int VOID = 86;
        static final int BOOLEAN = 90;
        static final int STRING = 115;
        static final int THREAD = 116;
        static final int THREAD_GROUP = 103;
        static final int CLASS_LOADER = 108;
        static final int CLASS_OBJECT = 99;
    }

    static class StepDepth {
        static final int INTO = 0;
        static final int OVER = 1;
        static final int OUT = 2;
    }

    static class StepSize {
        static final int MIN = 0;
        static final int LINE = 1;
    }

    static class SuspendPolicy {
        static final int NONE = 0;
        static final int EVENT_THREAD = 1;
        static final int ALL = 2;
    }

    /**
     * The invoke options are a combination of zero or more of the following bit flags:
     */
    static class InvokeOptions {
        static final int INVOKE_SINGLE_THREADED = 0x01;
        static final int INVOKE_NONVIRTUAL = 0x02;
    }
}
