package com.metal_pony.bucket.util;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

	private static final long CORE_THREAD_MAX_IDLE_TIME = 25L;
	private static final TimeUnit CORE_THREAD_MAX_IDLE_TIME_UNIT = TimeUnit.MILLISECONDS;

	private static int coreThreads;
	private static int maxThreads;
    private static ThreadPoolExecutor pool;
	private static BlockingQueue<Runnable> workQueue;

	private static synchronized void init() {
		if (pool != null) {
			return;
		}

		int numProcessors = Runtime.getRuntime().availableProcessors();

		coreThreads = numProcessors <= 4 ? 2 : numProcessors / 4;
		maxThreads = numProcessors / 2;
		workQueue = new LinkedBlockingQueue<Runnable>();
		pool = new ThreadPoolExecutor(
			coreThreads,
			maxThreads,
			CORE_THREAD_MAX_IDLE_TIME,
			CORE_THREAD_MAX_IDLE_TIME_UNIT,
			workQueue
		);
	}

	public static synchronized void setCoreThreads(int coreThreads) {
		if (coreThreads < 1) {
			throw new IllegalArgumentException(String.format("coreThreads (%d) too low", coreThreads));
		}
		init();
		pool.setCorePoolSize(coreThreads);
	}

	public static synchronized void setMaxThreads(int maxThreads) {
		if (maxThreads < 1) {
			throw new IllegalArgumentException(String.format("maxThreads (%d) too low", maxThreads));
		}
		init();
		pool.setMaximumPoolSize(maxThreads);
	}

	public static synchronized void useMaxThreads() {
		init();
		pool.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() - 1);
		pool.setCorePoolSize(Runtime.getRuntime().availableProcessors() - 1);
		pool.prestartAllCoreThreads();
	}

	public static synchronized void setSizeAndStart(int poolSize) {
		init();
		pool.setMaximumPoolSize(poolSize);
		pool.setCorePoolSize(poolSize);
		pool.prestartAllCoreThreads();
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
