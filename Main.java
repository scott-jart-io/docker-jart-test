package io.jart.test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import com.ea.async.Async;

import io.jart.async.AsyncLoop;
import io.jart.async.AsyncRunnable;
import io.jart.net.Ip4AddrAndPort;
import io.jart.net.TcpConnection;
import io.jart.net.TcpContext;
import io.jart.netmap.bridge.inet.SimpleInetBridgeTask;
import io.jart.util.HelpingWorkerThread;
import io.jart.util.RoundRobinExecutor;
import io.jart.util.Misc;

class Main {
	static {
		// make ea-async happy
		Async.init();
	}
	
	// simple test connection!
	private static class TestTcpConnection extends TcpConnection {
		public TestTcpConnection(TcpContext tcpContext, int mss, SimpleInetBridgeTask.Context sibtContext, int startSeqNum) {
			super(tcpContext, mss, sibtContext.eventQueue, sibtContext.msgRelay, startSeqNum, sibtContext.exec, null);
		}
	
		@Override
		protected CompletableFuture<Void> connectionRun() {
			System.err.println("connected");
			
			// run a simple echo loop
			byte[] lineBuf = new byte[2048];
			
			Async.await(AsyncLoop.doWhile(()->{
				int read = Async.await(getReader().read(lineBuf, (byte)'\n'));
				
				if(read <= 0)
					return AsyncLoop.cfFalse;
				Async.await(getWriter().write(Arrays.copyOfRange(lineBuf, 0, read)));
				return AsyncLoop.cfTrue;
			}, executor()));
			System.err.println("disconnected");
			return AsyncLoop.cfVoid;
		}
	}
	
	// asynchronous main function
	private static CompletableFuture<Void> asyncMain(String[] args) {
		try {
			// command line args: [device [tcp mss [thread count]]]
			String devName = (args.length > 0) ? args[0] : "em0";
			int mss = (args.length > 1) ? Integer.parseInt(args[1]) : 1484;
			int threadCount = (args.length > 2) ? Integer.parseInt(args[2]) : 4;
			// create worker thread team and wrap with RoundRobinExecutor
			HelpingWorkerThread[] workerThreads = HelpingWorkerThread.createTeam(threadCount);
			Executor exec = new RoundRobinExecutor(workerThreads);
			// create the bridge task
			SimpleInetBridgeTask sibt = new SimpleInetBridgeTask(devName, exec);
			CompletableFuture<Void> sibtCf = sibt.run();
			// wait for either failure or SimpleInetBridgeTask.Context to become available
			CompletableFuture<Object> sibtCtxOrExit = CompletableFuture.anyOf(sibtCf, sibt.context);
			SimpleInetBridgeTask.Context sibtContext = (SimpleInetBridgeTask.Context)Async.await(sibtCtxOrExit);
			// use to produce random sequence numbers
			SecureRandom secRand = new SecureRandom();
			// listen addr/port
			Ip4AddrAndPort addrPortPair = new Ip4AddrAndPort(Misc.guessIp4Addr(devName), 7);
			
			// add the listener
			System.out.println("listening on ip4 addr: 0x" + Integer.toHexString(addrPortPair.addr) + " / port " + addrPortPair.port);
			sibtContext.ip4TcpListen(addrPortPair,
					(TcpContext tcpContext)->new TestTcpConnection(tcpContext, mss, sibtContext, secRand.nextInt()));
			
			// we're done when the SimpleInetBridgeTask is done
			return sibtCf;
		} catch (Throwable th) {
			return CompletableFuture.failedFuture(th);
		}
	}

	public static void main(String[] args) {
		// start async main
		AsyncRunnable asyncMain = ()->asyncMain(args);
		ForkJoinTask<Void> mainTask = asyncMain.asForkJoinTask();
		ForkJoinPool.commonPool().execute(mainTask);

		// wait for async main to complete
		mainTask.join();
	}
}
