package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.metal_pony.bucket.util.Stats;

public class TestStats {

    private static record TestFixture (
        int[] values,
        double mean,
        double variance,
        double stddev
    ) {}

    private static final double DELTA = 1.0e-10;

    private static TestFixture[] fixtures;

    @BeforeAll
    public static void beforeAll() {
        fixtures = new TestFixture[] {
            new TestFixture(new int[]{0}, 0.0, 0.0, 0.0),
            new TestFixture(new int[]{0,1,2,3,4,5}, 15.0/6.0, 17.5/6.0, java.lang.Math.sqrt(17.5/6.0)),
            new TestFixture(new int[]{-5,-4,-3,-2,-1,0,1,2,3,4,5}, 0.0, 10.0, java.lang.Math.sqrt(10.0)),
            new TestFixture(new int[]{-1,-2,-3,-4,-5}, -3.0, 2.0, java.lang.Math.sqrt(2.0)),
            new TestFixture(new int[]{77,77,77}, 77.0, 0.0, 0.0)
        };
    }

    @Test
    public void testMeanCollection_whenCollectionIsEmpty_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(Collections.emptyList()));
    }

    @Test
    public void testMean_whenArrIsEmpty_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new float[]{}));
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new long[]{}));
        assertThrows(IllegalArgumentException.class, () -> Stats.mean(new double[]{}));
    }

    @Test
    public void testWithList() {
        for (TestFixture fixture : fixtures) {
            List<Integer> list = new ArrayList<>();
            for (int i : fixture.values) {
                list.add(i);
            }
            double actualMean = Stats.mean(list);
            double actualVariance = Stats.variance(list);
            double actualStddev = Stats.stddev(list);

            assertEquals(fixture.mean, actualMean, DELTA, String.format(
                "input: %s, expected mean: %f, got: %f",
                list.toString(), fixture.mean, actualMean
            ));
            assertEquals(fixture.variance, actualVariance, DELTA, String.format(
                "input: %s, expected variance: %f, got: %f",
                list.toString(), fixture.variance, actualVariance
            ));
            assertEquals(fixture.stddev, actualStddev, DELTA, String.format(
                "input: %s, expected stddev: %f, got: %f",
                list.toString(), fixture.stddev, actualStddev
            ));
        }
    }

    @Test
    public void testWithInts() {
        for (TestFixture fixture : fixtures) {
            double actualMean = Stats.mean(fixture.values);
            double actualVariance = Stats.variance(fixture.values);
            double actualStddev = Stats.stddev(fixture.values);

            assertEquals(fixture.mean, actualMean, DELTA, String.format(
                "input: %s, expected mean: %f, got: %f",
                Arrays.toString(fixture.values), fixture.mean, actualMean
            ));
            assertEquals(fixture.variance, actualVariance, DELTA, String.format(
                "input: %s, expected variance: %f, got: %f",
                Arrays.toString(fixture.values), fixture.variance, actualVariance
            ));
            assertEquals(fixture.stddev, actualStddev, DELTA, String.format(
                "input: %s, expected stddev: %f, got: %f",
                Arrays.toString(fixture.values), fixture.stddev, actualStddev
            ));
        }
    }

    @Test
    public void testWithFloats() {
        for (TestFixture fixture : fixtures) {
            float[] values = new float[fixture.values.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = (float)fixture.values[i];
            }

            double actualMean = Stats.mean(values);
            double actualVariance = Stats.variance(values);
            double actualStddev = Stats.stddev(values);

            assertEquals(fixture.mean, actualMean, DELTA, String.format(
                "input: %s, expected mean: %f, got: %f",
                Arrays.toString(values), fixture.mean, actualMean
            ));
            assertEquals(fixture.variance, actualVariance, DELTA, String.format(
                "input: %s, expected variance: %f, got: %f",
                Arrays.toString(values), fixture.variance, actualVariance
            ));
            assertEquals(fixture.stddev, actualStddev, DELTA, String.format(
                "input: %s, expected stddev: %f, got: %f",
                Arrays.toString(values), fixture.stddev, actualStddev
            ));
        }
    }

    @Test
    public void testWithLongs() {
        for (TestFixture fixture : fixtures) {
            long[] values = new long[fixture.values.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = (long)fixture.values[i];
            }

            double actualMean = Stats.mean(values);
            double actualVariance = Stats.variance(values);
            double actualStddev = Stats.stddev(values);

            assertEquals(fixture.mean, actualMean, DELTA, String.format(
                "input: %s, expected mean: %f, got: %f",
                Arrays.toString(values), fixture.mean, actualMean
            ));
            assertEquals(fixture.variance, actualVariance, DELTA, String.format(
                "input: %s, expected variance: %f, got: %f",
                Arrays.toString(values), fixture.variance, actualVariance
            ));
            assertEquals(fixture.stddev, actualStddev, DELTA, String.format(
                "input: %s, expected stddev: %f, got: %f",
                Arrays.toString(values), fixture.stddev, actualStddev
            ));
        }
    }

    @Test
    public void testWithDoubles() {
        for (TestFixture fixture : fixtures) {
            double[] values = new double[fixture.values.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = (double)fixture.values[i];
            }

            double actualMean = Stats.mean(values);
            double actualVariance = Stats.variance(values);
            double actualStddev = Stats.stddev(values);

            assertEquals(fixture.mean, actualMean, DELTA, String.format(
                "input: %s, expected mean: %f, got: %f",
                Arrays.toString(values), fixture.mean, actualMean
            ));
            assertEquals(fixture.variance, actualVariance, DELTA, String.format(
                "input: %s, expected variance: %f, got: %f",
                Arrays.toString(values), fixture.variance, actualVariance
            ));
            assertEquals(fixture.stddev, actualStddev, DELTA, String.format(
                "input: %s, expected stddev: %f, got: %f",
                Arrays.toString(values), fixture.stddev, actualStddev
            ));
        }
    }
}
