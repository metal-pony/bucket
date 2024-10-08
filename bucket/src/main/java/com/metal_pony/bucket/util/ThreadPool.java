package com.metal_pony.bucket.util;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

	private static final long DEFAULT_TIMEOUT = 1L;
	private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MINUTES;

	private static int coreThreads;
	private static int maxThreads;
    private static ThreadPoolExecutor pool;
	private static BlockingQueue<Runnable> workQueue;

	private static synchronized void init() {
		if (pool != null) {
			return;
		}

		int numProcessors = Runtime.getRuntime().availableProcessors();

		coreThreads = numProcessors <= 4 ? 1 : numProcessors / 4;
		maxThreads = numProcessors;
		workQueue = new LinkedBlockingQueue<Runnable>();
		pool = new ThreadPoolExecutor(
			coreThreads,
			maxThreads,
			DEFAULT_TIMEOUT,
			DEFAULT_TIMEOUT_UNIT,
			workQueue
		);
	}

	public static synchronized void setKeepAliveTime(long time, TimeUnit unit) {
		init();
		pool.setKeepAliveTime(time, unit);
	}

	public static Future<?> submit(Runnable task) {
		init();
		return pool.submit(task);
	}

	public static <T> Future<T> submit(Callable<T> task) {
		init();
		return pool.submit(task);
	}

	public static <T> Future<T> submit(Runnable task, T result) {
		init();
		return pool.submit(task, result);
	}

	public static void shutdown() {
		init();
		pool.shutdown();
	}

	public static List<Runnable> shutdownNow() {
		init();
		return pool.shutdownNow();
	}
}
