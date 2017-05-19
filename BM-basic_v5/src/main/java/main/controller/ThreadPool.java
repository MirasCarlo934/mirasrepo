package main.controller;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool extends ThreadPoolExecutor {

	/*public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		// TODO Auto-generated constructor stub
	}

	public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		// TODO Auto-generated constructor stub
	}

	public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
		// TODO Auto-generated constructor stub
	}*/

	public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, 
				new ArrayBlockingQueue<Runnable>(10), threadFactory, handler);
	}

	/**
	 * Renames thread to include process number
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		ControllerModule cm = (ControllerModule) r;
		int processNumber = cm.getProcessNumber();
		String s = t.getName();
		if(s.contains(":")) {
			s = s.substring(0, s.indexOf(":")) + ":Process" + processNumber;
		} else {
			s = s + ":Process" + processNumber;
		}
		t.setName(s);
	}
}
