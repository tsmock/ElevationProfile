// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.grid;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.jcs3.JCS;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileController;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.elevation.ElevationHelper;
import org.openstreetmap.josm.plugins.elevation.HgtReader;
import org.openstreetmap.josm.plugins.elevation.IVertexRenderer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Elevation grid display layer
 * @author Oliver Wieland &lt;oliver.wieland@online.de&gt;
 *
 */
public class ElevationGridLayer extends Layer implements TileLoaderListener, MouseListener {
    private static final int ELE_ZOOM_LEVEL = 13;
    private final IVertexRenderer vertexRenderer;
    private final MemoryTileCache tileCache;
    protected TileSource tileSource;
    protected ElevationGridTileLoader tileLoader;
    protected TileController tileController;

    private ILatLon clickLocation;

    private Bounds lastBounds;
    private TileSet tileSet;

    public ElevationGridLayer(String name) {
        super(name);
        HgtReader.clearCache();
        MainApplication.getMap().mapView.addMouseListener(this);

        setOpacity(0.8);
        setBackgroundLayer(true);
        vertexRenderer = new SimpleVertexRenderer();

        tileCache = new MemoryTileCache();
        tileCache.setCacheSize(500);
        tileSource = new ElevationGridTileSource(name);
        tileLoader = new ElevationGridTileLoader(this, JCS.getInstance("elevationgridlayer"), new TileJobOptions(20, 20, null, 3600));
        tileController = new ElevationGridTileController(tileSource, tileCache, this, tileLoader);
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        boolean needsNewTileSet = tileSet == null || (lastBounds == null || !lastBounds.equals(box));

        if (needsNewTileSet) {
            tileSet = new TileSet(box.getMin(), box.getMax(), ELE_ZOOM_LEVEL); // we use a vector format with constant zoom level
            lastBounds = box;
        }

        if (tileSet.insane()) {
            myDrawString(g, tr("zoom in to load any tiles"), 120, 120);
            return;
        } else if (tileSet.tooLarge()) {
            myDrawString(g, tr("zoom in to load more tiles"), 120, 120);
            return;
        } else if (tileSet.tooSmall()) {
            myDrawString(g, tr("increase zoom level to see more detail"), 120, 120);
            return;
        }

        for (int x = tileSet.x0; x <= tileSet.x1; x++) {
            for (int y = tileSet.y0; y <= tileSet.y1; y++) {
                Tile t = tileController.getTile(x, y, ELE_ZOOM_LEVEL);

                if (t != null && t.isLoaded() && t instanceof ElevationGridTile) {
                    ((ElevationGridTile) t).paintTile(g, mv, vertexRenderer);
                } else {
                    // give some consolation...
                    Point topLeft = mv.getPoint(CoordinateConversion.coorToLL(tileSource.tileXYToLatLon(x, y, ELE_ZOOM_LEVEL)));
                    t.paint(g, topLeft.x, topLeft.y);
                }
            }
        }
        // Paint the current point area
        ElevationHelper.getBounds(this.clickLocation).ifPresent(bounds -> {
            final BBox bbox = bounds.toBBox();
            final Point upperLeft = mv.getPoint(bbox.getTopLeft());
            final Point bottomRight = mv.getPoint(bbox.getBottomRight());
            final Rectangle rectangle = new Rectangle(upperLeft.x, upperLeft.y,
                    bottomRight.x - upperLeft.x, bottomRight.y - upperLeft.y);
            g.setColor(Color.RED);
            g.draw(rectangle);
        });
    }

    @Override
    public String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        // TODO Auto-generated method stub

    }

    @Override
    public Action[] getMenuEntries() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void tileLoadingFinished(Tile tile, boolean success) {
        try {
            invalidate();
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "elevation");
    }

    @Override
    public void mergeFrom(Layer from) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isMergable(Layer other) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getInfoComponent() {
        // TODO Auto-generated method stub
        return null;
    }


    // Stolen from TMSLayer...
    void myDrawString(Graphics g, String text, int x, int y) {
        Color oldColor = g.getColor();
        g.setColor(Color.black);
        g.drawString(text, x+1, y+1);
        g.setColor(oldColor);
        g.drawString(text, x, y);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            this.clickLocation = MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY());
            final double elevation = ElevationHelper.getSrtmElevation(clickLocation);
            Notification notification = new Notification("Elevation is: " + elevation);
            notification.setDuration(Notification.TIME_SHORT);
            GuiHelper.runInEDT(notification::show);
            GuiHelper.runInEDT(this::invalidate);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    private class TileSet {
        int x0, x1, y0, y1;
        int tileMax = -1;

        /**
         * Create a TileSet by known LatLon bbox without layer shift correction
         */
        TileSet(LatLon topLeft, LatLon botRight, int zoom) {
            if (zoom == 0)
                return;

            TileXY p0 = tileSource.latLonToTileXY(topLeft.lat(), topLeft.lon(), zoom);
            TileXY p1 = tileSource.latLonToTileXY(botRight.lat(), botRight.lon(), zoom);

            x0 = p0.getXIndex();
            y0 = p0.getYIndex();
            x1 = p1.getXIndex();
            y1 = p1.getYIndex();
            if (x0 > x1) {
                int tmp = x0;
                x0 = x1;
                x1 = tmp;
            }
            if (y0 > y1) {
                int tmp = y0;
                y0 = y1;
                y1 = tmp;
            }
            tileMax = (int) Math.pow(2.0, zoom);
            if (x0 < 0) {
                x0 = 0;
            }
            if (y0 < 0) {
                y0 = 0;
            }
            if (x1 > tileMax) {
                x1 = tileMax;
            }
            if (y1 > tileMax) {
                y1 = tileMax;
            }
        }

        int size() {
            int x_span = x1 - x0 + 1;
            int y_span = y1 - y0 + 1;
            return x_span * y_span;
        }

        @Override
        public String toString() {
            return "TileSet [x0=" + x0 + ", x1=" + x1 + ", y0=" + y0 + ", y1="
                    + y1 + ", size()=" + size() + ", tilesSpanned()="
                    + tilesSpanned() + "]";
        }

        double tilesSpanned() {
            return Math.sqrt(1.0 * this.size());
        }

        boolean tooSmall() {
            return this.tilesSpanned() < 1;
        }

        boolean tooLarge() {
            return this.tilesSpanned() > 50;
        }

        boolean insane() {
            return this.tilesSpanned() > 200;
        }
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        HgtReader.clearCache();
        MainApplication.getMap().mapView.removeMouseListener(this);
    }
}
