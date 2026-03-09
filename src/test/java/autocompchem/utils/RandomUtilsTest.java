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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link RandomUtils}.
 *
 * @author Marco Foscato
 */
public class RandomUtilsTest
{

//------------------------------------------------------------------------------

    @Test
    public void testGetInstance()
    {
        RandomUtils a = RandomUtils.getInstance();
        RandomUtils b = RandomUtils.getInstance();
        assertSame(a, b, "getInstance() should return the same singleton");
    }

//------------------------------------------------------------------------------

    @Test
    public void testSetSeedReproducibility()
    {
        RandomUtils ru = RandomUtils.getInstance();
        long seed = 12345L;

        ru.setSeed(seed);
        int i1 = ru.nextInt();
        double d1 = ru.nextDouble();
        boolean b1 = ru.nextBoolean();

        ru.setSeed(seed);
        assertEquals(i1, ru.nextInt(), "nextInt() should repeat after same seed");
        assertEquals(d1, ru.nextDouble(), 0.0, "nextDouble() should repeat after same seed");
        assertEquals(b1, ru.nextBoolean(), "nextBoolean() should repeat after same seed");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNextInt()
    {
        RandomUtils ru = RandomUtils.getInstance();
        ru.setSeed(999L);
        int a = ru.nextInt();
        ru.setSeed(999L);
        assertEquals(a, ru.nextInt(), "nextInt() should be reproducible with seed");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNextIntBound()
    {
        RandomUtils ru = RandomUtils.getInstance();
        ru.setSeed(42L);
        int bound = 10;
        int first = ru.nextInt(bound);
        for (int i = 0; i < 99; i++)
        {
            int v = ru.nextInt(bound);
            assertTrue(v >= 0 && v < bound, "nextInt(bound) should be in [0, " + bound + ")");
        }
        ru.setSeed(42L);
        assertEquals(first, ru.nextInt(bound), "nextInt(bound) should repeat after same seed");
    }

    @Test
    public void testNextIntBoundInvalid()
    {
        RandomUtils ru = RandomUtils.getInstance();
        assertThrows(IllegalArgumentException.class, () -> ru.nextInt(0),
                "nextInt(0) should throw");
        assertThrows(IllegalArgumentException.class, () -> ru.nextInt(-1),
                "nextInt(negative) should throw");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNextDouble()
    {
        RandomUtils ru = RandomUtils.getInstance();
        ru.setSeed(77L);
        double first = ru.nextDouble();
        for (int i = 0; i < 49; i++)
        {
            double v = ru.nextDouble();
            assertTrue(v >= 0.0 && v < 1.0, "nextDouble() should be in [0.0, 1.0)");
        }
        ru.setSeed(77L);
        assertEquals(first, ru.nextDouble(), 0.0, "nextDouble() should repeat after same seed");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNextDoubleMinMax()
    {
        RandomUtils ru = RandomUtils.getInstance();
        ru.setSeed(111L);
        double min = 2.5;
        double max = 7.5;
        double first = ru.nextDouble(min, max);
        for (int i = 0; i < 49; i++)
        {
            double v = ru.nextDouble(min, max);
            assertTrue(v >= min && v < max, "nextDouble(min,max) should be in [" + min + ", " + max + ")");
        }
        ru.setSeed(111L);
        assertEquals(first, ru.nextDouble(min, max), 0.0,
                "nextDouble(min,max) should repeat after same seed");
    }

    @Test
    public void testNextDoubleMinMaxInvalid()
    {
        RandomUtils ru = RandomUtils.getInstance();
        assertThrows(IllegalArgumentException.class, () -> ru.nextDouble(1.0, 0.0),
                "nextDouble(min,max) with min > max should throw");
        assertThrows(IllegalArgumentException.class, () -> ru.nextDouble(1.0, 1.0),
                "nextDouble(min,max) with min == max should throw");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNextBoolean()
    {
        RandomUtils ru = RandomUtils.getInstance();
        ru.setSeed(333L);
        boolean a = ru.nextBoolean();
        boolean b = ru.nextBoolean();
        ru.setSeed(333L);
        assertEquals(a, ru.nextBoolean(), "nextBoolean() should repeat after same seed");
        assertEquals(b, ru.nextBoolean(), "nextBoolean() sequence reproducible");
    }

//------------------------------------------------------------------------------
}
