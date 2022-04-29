package com.sparklicorn.bucket.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Performs some shuffle benchmarking, providing statistical output and
 * a graphic representing the spread of permutations.
 *
 * args:
 * [0]: numShuffles: Number of rounds of shuffling to do. Each round is 9! shuffles. Defaults to 1000.
 */
public class ShufflerTester {

    private static final String SHUFFLE_FILE_NAME = "shuffle-spread.txt";
    private static final char[] SRC = "123456789".toCharArray();

    public static void main(String[] args) {
        // Load shuffle file, or use blank map.
        Map<String,Integer> shuffles = new HashMap<>();
        try {
            readShuffleFile(shuffles, SHUFFLE_FILE_NAME);
            analyzeShuffles(shuffles);
            System.out.println();
        } catch (FileNotFoundException e1) {
            // do nothing, using blank map
        }

        long numShuffles;
        try {
            numShuffles = Long.valueOf(args[0]);
        } catch (IndexOutOfBoundsException | NullPointerException | NumberFormatException ex) {
            numShuffles = 1000L;
        }
        numShuffles *= Counting.factorial((long)SRC.length);

        long startTime = System.currentTimeMillis();
        recordShuffles(shuffles, numShuffles, Runtime.getRuntime().availableProcessors());
        long endTime = System.currentTimeMillis();
        System.out.printf("Finished generating %d shuffles in %d ms.%n", numShuffles, (endTime - startTime));
        System.out.println();

        try {
            writeShuffleFile(shuffles, SHUFFLE_FILE_NAME);
        } catch (IOException e1) {
            e1.printStackTrace(System.out);
            System.out.println("Did not write shuffles to file.");
        }

        System.out.println();
        analyzeShuffles(shuffles);
        System.out.println();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle("Shuffle Tester");

        int[] dimensions = Math.shortestDimensionsRect(Counting.factorial(SRC.length));
        int h = dimensions[0];
        int w = dimensions[1];
        float maxStdDev = 0.10f;

        List<Integer> vals = new ArrayList<>(shuffles.values());
        float[] colorVals = new float[vals.size()];
        double mean = Stats.mean(vals);
        double stddev = Stats.stddev(vals);

        int i = 0;
        float maxStdDevDiff = 0f;
        for (int v : vals) {
            float stddevsDiff = java.lang.Math.abs((float)((v - mean) / stddev));
            if (stddevsDiff > maxStdDevDiff) {
                maxStdDevDiff = stddevsDiff;
            }
            float pts = stddevsDiff * maxStdDev;
            if (pts > 1f) {
                System.out.printf("Over val: %d%nstddevsDiff: %f%npts: %f%n", v, stddevsDiff, pts);
            }
            colorVals[i++] = (pts > 0f) ?
                java.lang.Math.min(pts, 1f) : // should be for blue
                java.lang.Math.max(pts, -1f); // red
        }

        System.out.printf("max std devs diff: %f%nColor value: %f%n", maxStdDevDiff, Math.max(colorVals));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int cell = 0;
                for (float color : colorVals) {
                    int row = cell / w;
                    int col = cell % w;

                    g.setColor(new Color(
                        color,// (color < 0f) ? -color : 1f,
                        color,// 0f,
                        color//(color > 0f) ? color : 1f
                    ));
                    g.drawLine(col, row, col, row);

                    cell++;
                }
            }
        };

        panel.setPreferredSize(new Dimension(w, h));
        panel.setBackground(Color.WHITE);

        frame.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyPressed(KeyEvent e) {}
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
        });
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);

        System.out.println("Starting visual spread. Press ESCAPE to exit.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Integer> readShuffleFile(Map<String,Integer> permsCount, String filename)
        throws FileNotFoundException
    {
        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);

        System.out.print("Reading from file: \"" + filename + "\"... ");
        Gson gson = new Gson();
        Type typeOfT = new TypeToken<Map<String,Integer>>(){}.getType();

        Map<String,Integer> countFromFile = (Map<String,Integer>)gson.fromJson(br, typeOfT);
        System.out.println("Done.");
        System.out.printf("Mapping data... ");
        for (Entry<String,Integer> e : countFromFile.entrySet()) {
            permsCount.put(
                e.getKey(),
                permsCount.getOrDefault(e.getKey(), 0) + e.getValue()
            );
        }
        System.out.println("Done.");

        return permsCount;
    }

    private static void writeShuffleFile(Map<String,Integer> permsCount, String filename)
        throws IOException
    {
        FileWriter fw = new FileWriter(filename);
        System.out.print("Writing to file: \"" + filename + "\"... ");
        Gson gson = new Gson();
        gson.toJson(permsCount, fw);
        System.out.println("Done.");
    }

    private static void recordShuffles(Map<String,Integer> permsCount, long numShuffles, int numThreads) {
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, workQueue);
        final long numShufflesPerThread = numShuffles / numThreads;
        final long leftOverNumShuffles = numShuffles % numThreads;
        final Future<?>[] futures = new Future[numThreads];

        @SuppressWarnings("unchecked")
        final Map<String,Integer>[] threadPermsCounts = new HashMap[numThreads];
        for (int i = 0; i < threadPermsCounts.length; i++) {
            threadPermsCounts[i] = new HashMap<String,Integer>();
        }

        System.out.println("Generating Shuffles...");
        System.out.println("Waiting on threads...");

        pool.prestartAllCoreThreads();
        for (int i = 0; i < numThreads; i++) {
            final int THREAD_INDEX = i;
            futures[THREAD_INDEX] = pool.submit(
                () -> getShuffles(threadPermsCounts[THREAD_INDEX], numShufflesPerThread)
            );
        }

        if (leftOverNumShuffles > 0L) {
            getShuffles(permsCount, leftOverNumShuffles);
        }

        waitForAllFutures(futures, (ex) -> {
            ex.printStackTrace();
        });
        pool.shutdown();
        System.out.println();
        System.out.println("Done!");

        // Now all threads are done and results can be combined.
        System.out.print("Combining maps from threads... ");
        for (Map<String,Integer> threadPerms : threadPermsCounts) {
            for (Entry<String,Integer> e : threadPerms.entrySet()) {
                permsCount.put(
                    e.getKey(),
                    permsCount.getOrDefault(e.getKey(), 0) + e.getValue()
                );
            }
        }
        System.out.println("Done!");
    }

    // Records the given number of shuffles into the given map.
    private static void getShuffles(Map<String,Integer> shuffles, long numShuffles) {
        Shuffler snuffles = new Shuffler();
        char[] dest = new char[SRC.length];

        for (long j = 0; j < numShuffles; j++) {
            System.arraycopy(SRC, 0, dest, 0, SRC.length);
            snuffles.shuffle(dest);
            String perm = new String(dest);
            Integer count = shuffles.getOrDefault(perm, 0);
            shuffles.put(perm, ++count);

            if (j % (numShuffles/100) == 0L) {
                System.out.print('.');
            }
        }
    }

    // Waits for all given Futures to resolve.
    // If any throw an exception, errorFunc will be invoked if it has been provided.
    private static void waitForAllFutures(Future<?>[] futures, Consumer<Exception> errorFunc) {
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception ex) {
                if (errorFunc != null) {
                    errorFunc.accept(ex);
                }
            }
        }
    }

    // Prints stats about the shuffle data.
    private static void analyzeShuffles(Map<String,Integer> shuffles) {
        System.out.println("=== SHUFFLE DATA ANALYSIS ===");
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        long total = 0L;
        for (Entry<String, Integer> e : shuffles.entrySet()) {
            if (e.getValue() < min) {
                min = e.getValue();
            }

            if (e.getValue() > max) {
                max = e.getValue();
            }

            total += e.getValue();
        }

        List<Integer> vals = new ArrayList<>(shuffles.values());
        double mean = Stats.mean(vals);
        double stddev = Stats.stddev(vals);

        System.out.printf(
            "%nNum Perms: %d%nNum Shuffles: %d%nMinimum: %d%nMaximum:%d%nStd Dev: %f%nMean: %f%n",
            vals.size(), total, min, max, stddev, mean
        );
    }
}
