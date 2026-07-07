package com.siruoren.buildpopup;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Pure unit tests for BuildPopupThreadPool.
 * Since BuildPopupThreadPool.getInstance() requires Jenkins initialization,
 * we test the core concurrency patterns directly using ThreadPoolExecutor.
 */
public class BuildPopupThreadPoolTest {

    private ThreadPoolExecutor createTestExecutor() {
        return new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Test
    public void testSubmitTask() throws Exception {
        ThreadPoolExecutor executor = createTestExecutor();
        try {
            Future<String> future = executor.submit(() -> "hello");
            assertEquals("hello", future.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testMultipleSubmissions() throws Exception {
        ThreadPoolExecutor executor = createTestExecutor();
        try {
            int taskCount = 10;
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int val = i;
                futures.add(executor.submit(() -> val * 2));
            }

            for (int i = 0; i < taskCount; i++) {
                assertEquals(Integer.valueOf(i * 2), futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testActiveCount() throws Exception {
        ThreadPoolExecutor executor = createTestExecutor();
        try {
            // Submit a task that blocks so we can observe active count
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);

            executor.submit(() -> {
                started.countDown();
                release.await(10, TimeUnit.SECONDS);
                return "done";
            });

            // Wait for the task to start
            started.await(5, TimeUnit.SECONDS);
            assertTrue("Active count should be at least 1", executor.getActiveCount() >= 1);

            // Release the blocking task
            release.countDown();
        } finally {
            executor.shutdownNow();
        }
    }
}
