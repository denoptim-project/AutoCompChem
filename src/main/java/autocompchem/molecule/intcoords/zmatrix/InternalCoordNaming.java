package autocompchem.molecule.intcoords.zmatrix;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for generating systematic names for internal coordinates.
 * Provides both counter-based (sequential) and index-based (deterministic) naming schemes.
 *
 * @author Marco Foscato
 */
public class InternalCoordNaming
{
    /**
     * Root for distance-type IC names
     */
    private static final String DISTROOT = "dst";

    /**
     * Root for angle-type IC names
     */
    private static final String ANGROOT = "ang";

    /**
     * Root for torsion-type IC names
     */
    private static final String TORROOT = "tor";

//------------------------------------------------------------------------------

    /**
     * Generate a counter-based name for a distance-type internal coordinate.
     * @param counter the counter to use for generating unique names
     * @return a unique name like "dst1", "dst2", etc.
     */
    public static String getSequentialDistName(AtomicInteger counter)
    {
        return DISTROOT + counter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Generate a counter-based name for an angle-type internal coordinate.
     * @param counter the counter to use for generating unique names
     * @return a unique name like "ang1", "ang2", etc.
     */
    public static String getSequentialAngName(AtomicInteger counter)
    {
        return ANGROOT + counter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Generate a counter-based name for a torsion-type internal coordinate.
     * @param counter the counter to use for generating unique names
     * @return a unique name like "tor1", "tor2", etc.
     */
    public static String getSequentialTorName(AtomicInteger counter)
    {
        return TORROOT + counter.getAndIncrement();
    }

//------------------------------------------------------------------------------

}
