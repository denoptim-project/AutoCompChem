package autocompchem.utils;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Random;

/**
 * Singleton utility for generating pseudo-random numbers and decisions.
 * Supports an optional seed so that the sequence is reproducible when needed.
 *
 * @author Marco Foscato
 */
public final class RandomUtils
{
    /**
     * Singleton instance.
     */
    private static RandomUtils INSTANCE;

    /**
     * Pseudo-random number generator. Replaced when the seed is set.
     */
    private Random random = new Random();

//------------------------------------------------------------------------------

    private RandomUtils()
    {}

//------------------------------------------------------------------------------

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static synchronized RandomUtils getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new RandomUtils();
        return INSTANCE;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the seed for the internal generator. After this call, the sequence
     * of values from {@link #nextInt()}, {@link #nextInt(int)}, {@link #nextDouble()},
     * {@link #nextDouble(double, double)}, and {@link #nextBoolean()} is
     * reproducible for the same seed.
     *
     * @param seed the seed
     */
    public synchronized void setSeed(long seed)
    {
        random = new Random(seed);
    }

//------------------------------------------------------------------------------    

    /**
     * Returns the next pseudo-random integer from the generator.
     *
     * @return the next int value
     */
    public synchronized int nextInt()
    {
        return random.nextInt();
    }

//------------------------------------------------------------------------------

    /**
     * Returns a pseudo-random int in the range [0, bound).
     *
     * @param bound upper bound (exclusive); must be positive
     * @return a random int in [0, bound)
     * @throws IllegalArgumentException if bound is not positive
     */
    public synchronized int nextInt(int bound)
    {
        return random.nextInt(bound);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the next pseudo-random double in the range [0.0, 1.0).
     *
     * @return the next double value
     */
    public synchronized double nextDouble()
    {
        return random.nextDouble();
    }

//------------------------------------------------------------------------------

    /**
     * Returns a pseudo-random double in the range [min, max).
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (exclusive)
     * @return a random double in [min, max)
     * @throws IllegalArgumentException if min &gt;= max
     */
    public synchronized double nextDouble(double min, double max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");
        return min + random.nextDouble() * (max - min);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the next pseudo-random boolean (true or false with equal probability).
     *
     * @return the next boolean value
     */
    public synchronized boolean nextBoolean()
    {
        return random.nextBoolean();
    }

//------------------------------------------------------------------------------
}
