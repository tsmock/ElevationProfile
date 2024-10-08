// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.gpx;

import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Interface for all GPX data visitors. Hopefully this will be part of JOSM some day.
 * @author Oliver Wieland &lt;oliver.wieland@online.de&gt;
 */
public interface IGpxWaypointVisitor {
    /**
     * Visits a way point. This method is called for isolated way points, i. e. way points
     * without an associated route or track.
     * @param wp The way point to visit.
     */
    void visitWayPoint(WayPoint wp);
}
