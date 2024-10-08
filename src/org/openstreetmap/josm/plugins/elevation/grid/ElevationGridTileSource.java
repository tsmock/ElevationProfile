// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.grid;

import java.io.IOException;

import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;

/**
 * Tile source class for the tile loader
 * @author Olli
 */
public class ElevationGridTileSource extends TMSTileSource {
    public ElevationGridTileSource(String name) {
        super(new TileSourceInfo(name, "", "eg"));
    }

    @Override
    public String getName() {
        return "eg";
    }

    @Override
    public String getExtension() {
        return "";
    }

    /**
     * @throws IOException when subclass cannot return the tile URL
     */
    @Override
    public String getTilePath(int zoom, int tilex, int tiley) throws IOException {
        return "/" + zoom + "/" + tilex + "/" + tiley + "." + getExtension();
    }

    @Override
    public String getBaseUrl() {
        return "";
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) throws IOException {
        return this.getBaseUrl() + getTilePath(zoom, tilex, tiley);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int getTileSize() {
        // TODO
        return 256;
    }

}
