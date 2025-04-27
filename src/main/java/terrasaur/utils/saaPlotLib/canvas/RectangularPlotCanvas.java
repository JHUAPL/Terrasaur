package terrasaur.utils.saaPlotLib.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisX;
import terrasaur.utils.saaPlotLib.canvas.axis.AxisY;
import terrasaur.utils.saaPlotLib.config.PlotConfig;
import terrasaur.utils.saaPlotLib.data.Annotation;
import terrasaur.utils.saaPlotLib.data.Annotations;
import terrasaur.utils.saaPlotLib.data.Point4D;
import terrasaur.utils.saaPlotLib.util.Keyword;
import terrasaur.utils.saaPlotLib.util.StringUtils;

/**
 * 0,0 is at top left
 *
 * @author nairah1
 */
public abstract class RectangularPlotCanvas extends PlotCanvas {

    protected AxisX xLowerAxis, xUpperAxis;
    protected AxisY yLeftAxis, yRightAxis;

    public RectangularPlotCanvas(PlotConfig config) {
        super(config);
    }

    /**
     * Add an {@link Annotation} at the desired coordinates
     *
     * @param a annotation
     * @param x data coordinate
     * @param y data coordinate
     */
    public void addAnnotation(Annotation a, double x, double y) {
        Annotations annotations = new Annotations();
        annotations.addAnnotation(a, x, y);
        addAnnotations(annotations);
    }

    /**
     * @param annotations annotations to draw
     */
    public void addAnnotations(Annotations annotations) {

        for (Point4D p : annotations) {
            Annotation a = annotations.getAnnotation(p);

            double pixelX = dataXtoPixel(xLowerAxis, p.getX());
            double pixelY = dataYtoPixel(yLeftAxis, p.getY());

            Graphics2D g = image.createGraphics();
            configureHintsForSubpixelQuality(g);

            g.setColor(a.color());
            g.setFont(a.font());

            addAnnotation(g, a.text(), pixelX, pixelY, a.verticalAlignment(), a.horizontalAlignment());
        }
    }

    protected void drawLowerAxis() {
        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setFont(config.axisFont());
        g.setColor(Color.BLACK);
        g.drawLine(config.leftMargin(), pageHeight - config.bottomMargin(), pageWidth - config.rightMargin(), pageHeight - config.bottomMargin());

        if (xLowerAxis == null) return;
        g.setColor(xLowerAxis.getAxisColor());

        NavigableMap<Double, String> tickLabels = xLowerAxis.getTickLabels();
        NavigableSet<Double> minorTicks = xLowerAxis.getMinorTicks();
        if (minorTicks != null) {
            for (double minorTick : minorTicks) {
                if (!xLowerAxis.getRange().closedContains(minorTick)) continue;

                Path2D.Double path = new Path2D.Double();
                double pixelX = xLowerAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), minorTick);
                double pixelY = pageHeight - config.bottomMargin();
                path.moveTo(pixelX, pixelY);
                pixelY -= config.xMinorTickLength();
                path.lineTo(pixelX, pixelY);

                g.draw(path);
            }
        }

        double labelEdge = config.getBottomPlotEdge();
        for (double majorTick : tickLabels.keySet()) {
            if (!xLowerAxis.getRange().closedContains(majorTick)) continue;

            Path2D.Double path = new Path2D.Double();
            double pixelX = xLowerAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), majorTick);
            double pixelY = pageHeight - config.bottomMargin();
            path.moveTo(pixelX, pixelY);
            pixelY -= config.xMajorTickLength();
            path.lineTo(pixelX, pixelY);

            g.draw(path);

            if (!xLowerAxis.getTitle().isEmpty()) {
                // we want the bounding box to be centered at pixelX, pixelY
                pixelY = config.getBottomPlotEdge();

                Rectangle2D bb = StringUtils.boundingBox(g, tickLabels.get(majorTick));
                double height = Math.max(bb.getHeight() * Math.abs(Math.cos(xLowerAxis.getRotateLabels())), bb.getWidth() * Math.abs(Math.sin(xLowerAxis.getRotateLabels())));
                pixelY += (g.getFontMetrics().getHeight() + height / 2);

                labelEdge = Math.max(labelEdge, pixelY);

                addAnnotation(g, tickLabels.get(majorTick), pixelX, pixelY, xLowerAxis.getRotateLabels());
            }
        }

        if (!xLowerAxis.getTitle().isEmpty()) {
            double pixelX = config.width() / 2. + config.leftMargin();
            double pixelY = labelEdge + g.getFontMetrics().getHeight();

            // we want the bounding box centered at pixelX, pixelY
            Rectangle2D bb = StringUtils.boundingBox(g, xLowerAxis.getTitle());
            double height = Math.max(bb.getHeight() * Math.abs(Math.cos(xLowerAxis.getRotateTitle())), bb.getWidth() * Math.abs(Math.sin(xLowerAxis.getRotateTitle())));
            pixelY += height;

            addAnnotation(g, xLowerAxis.getTitle(), pixelX, pixelY, xLowerAxis.getRotateTitle());
        }
    }

    protected void drawUpperAxis() {
        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setFont(config.axisFont());
        g.setColor(Color.BLACK);
        g.drawLine(config.leftMargin(), config.topMargin(), pageWidth - config.rightMargin(), config.topMargin());

        if (xUpperAxis == null) return;
        g.setColor(xUpperAxis.getAxisColor());

        NavigableMap<Double, String> tickLabels = xUpperAxis.getTickLabels();
        NavigableSet<Double> minorTicks = xUpperAxis.getMinorTicks();
        if (minorTicks != null) {
            for (double minorTick : minorTicks) {
                if (!xUpperAxis.getRange().closedContains(minorTick)) continue;

                Path2D.Double path = new Path2D.Double();
                double pixelX = xUpperAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), minorTick);
                double pixelY = config.topMargin();
                path.moveTo(pixelX, pixelY);
                pixelY += config.xMinorTickLength();
                path.lineTo(pixelX, pixelY);

                g.draw(path);
            }
        }

        double labelEdge = config.getTopPlotEdge();
        for (double majorTick : tickLabels.keySet()) {
            if (!xUpperAxis.getRange().closedContains(majorTick)) continue;

            Path2D.Double path = new Path2D.Double();
            double pixelX = xUpperAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), majorTick);
            double pixelY = config.topMargin();
            path.moveTo(pixelX, pixelY);
            pixelY += config.xMajorTickLength();
            path.lineTo(pixelX, pixelY);

            g.draw(path);

            if (!xUpperAxis.getTitle().isEmpty()) {
                pixelY = config.topMargin();

                Rectangle2D bb = StringUtils.boundingBox(g, tickLabels.get(majorTick));
                double height = Math.max(bb.getHeight() * Math.abs(Math.cos(xUpperAxis.getRotateLabels())), bb.getWidth() * Math.abs(Math.sin(xUpperAxis.getRotateLabels())));
                pixelY -= (g.getFontMetrics().getHeight() + height / 2);

                labelEdge = Math.min(labelEdge, pixelY);

                addAnnotation(g, tickLabels.get(majorTick), pixelX, pixelY, xUpperAxis.getRotateLabels());
            }
        }

        if (!xUpperAxis.getTitle().isEmpty()) {
            double pixelX = config.width() / 2. + config.leftMargin();
            double pixelY = labelEdge - g.getFontMetrics().getHeight();

            // we want the bounding box centered at pixelX, pixelY
            Rectangle2D bb = StringUtils.boundingBox(g, xUpperAxis.getTitle());
            double height = Math.max(bb.getHeight() * Math.abs(Math.cos(xUpperAxis.getRotateTitle())), bb.getWidth() * Math.abs(Math.sin(xUpperAxis.getRotateTitle())));
            pixelY -= height;

            addAnnotation(g, xUpperAxis.getTitle(), pixelX, pixelY, xUpperAxis.getRotateTitle());
        }
    }

    protected void drawLeftAxis() {
        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setFont(config.axisFont());
        g.setColor(Color.BLACK);
        g.drawLine(config.leftMargin(), config.topMargin(), config.leftMargin(), pageHeight - config.bottomMargin());

        if (yLeftAxis == null) return;
        g.setColor(yLeftAxis.getAxisColor());

        NavigableMap<Double, String> tickLabels = yLeftAxis.getTickLabels();
        NavigableSet<Double> minorTicks = yLeftAxis.getMinorTicks();
        if (minorTicks != null) {
            for (double minorTick : minorTicks) {
                if (!yLeftAxis.getRange().closedContains(minorTick)) continue;

                Path2D.Double path = new Path2D.Double();
                double pixelX = config.leftMargin();
                double pixelY = yLeftAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), minorTick);
                path.moveTo(pixelX, pixelY);
                pixelX += config.yMinorTickLength();
                path.lineTo(pixelX, pixelY);

                g.draw(path);
            }
        }

        double labelEdge = config.getLeftPlotEdge();
        for (double majorTick : tickLabels.keySet()) {
            if (!yLeftAxis.getRange().closedContains(majorTick)) continue;

            Path2D.Double path = new Path2D.Double();
            double pixelX = config.leftMargin();
            double pixelY = yLeftAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), majorTick);
            path.moveTo(pixelX, pixelY);
            pixelX += config.yMajorTickLength();
            path.lineTo(pixelX, pixelY);

            g.draw(path);

            if (!yLeftAxis.getTitle().isEmpty()) {
                pixelX = config.leftMargin();

                Rectangle2D bb = StringUtils.boundingBox(g, tickLabels.get(majorTick));
                double width = Math.max(bb.getWidth() * Math.abs(Math.cos(yLeftAxis.getRotateLabels())), bb.getHeight() * Math.abs(Math.sin(yLeftAxis.getRotateLabels())));
                pixelX -= (g.getFontMetrics().getMaxAdvance() + width / 2);

                labelEdge = Math.min(labelEdge, pixelX);

                addAnnotation(g, tickLabels.get(majorTick), pixelX, pixelY, yLeftAxis.getRotateLabels());
            }
        }

        if (!yLeftAxis.getTitle().isEmpty()) {
            double pixelX = labelEdge - g.getFontMetrics().getMaxAdvance();
            double pixelY = config.getTopPlotEdge() + config.height() / 2.;

            // we want the bounding box centered at pixelX, pixelY
            Rectangle2D bb = StringUtils.boundingBox(g, yLeftAxis.getTitle());
            double width = Math.max(bb.getWidth() * Math.abs(Math.cos(yLeftAxis.getRotateTitle())), bb.getHeight() * Math.abs(Math.sin(yLeftAxis.getRotateTitle())));
            pixelX -= width;

            addAnnotation(g, yLeftAxis.getTitle(), pixelX, pixelY, yLeftAxis.getRotateTitle());
        }
    }

    protected void drawRightAxis() {
        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setFont(config.axisFont());
        g.setColor(Color.BLACK);
        g.drawLine(pageWidth - config.rightMargin(), config.topMargin(), pageWidth - config.rightMargin(), pageHeight - config.bottomMargin());

        if (yRightAxis == null) return;
        g.setColor(yRightAxis.getAxisColor());

        NavigableMap<Double, String> tickLabels = yRightAxis.getTickLabels();
        NavigableSet<Double> minorTicks = yRightAxis.getMinorTicks();
        if (minorTicks != null) {
            for (double minorTick : minorTicks) {
                if (!yRightAxis.getRange().closedContains(minorTick)) continue;

                Path2D.Double path = new Path2D.Double();
                double pixelX = pageWidth - config.rightMargin();
                double pixelY = yRightAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), minorTick);
                path.moveTo(pixelX, pixelY);
                pixelX -= config.yMinorTickLength();
                path.lineTo(pixelX, pixelY);

                g.draw(path);
            }
        }

        double labelEdge = config.getRightPlotEdge();
        for (double majorTick : tickLabels.keySet()) {
            if (!yRightAxis.getRange().closedContains(majorTick)) continue;

            Path2D.Double path = new Path2D.Double();
            double pixelX = pageWidth - config.rightMargin();
            double pixelY = yRightAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), majorTick);
            path.moveTo(pixelX, pixelY);
            pixelX -= config.yMajorTickLength();
            path.lineTo(pixelX, pixelY);

            g.draw(path);

            if (!yRightAxis.getTitle().isEmpty()) {
                pixelX = config.getRightPlotEdge();

                Rectangle2D bb = StringUtils.boundingBox(g, tickLabels.get(majorTick));
                double width = Math.max(bb.getWidth() * Math.abs(Math.cos(yRightAxis.getRotateLabels())), bb.getHeight() * Math.abs(Math.sin(yRightAxis.getRotateLabels())));
                pixelX += (g.getFontMetrics().getMaxAdvance() + width / 2);

                labelEdge = Math.max(labelEdge, pixelX);

                addAnnotation(g, tickLabels.get(majorTick), pixelX, pixelY, yRightAxis.getRotateLabels());
            }
        }

        if (!yRightAxis.getTitle().isEmpty()) {
            double pixelX = labelEdge + g.getFontMetrics().getMaxAdvance();
            double pixelY = config.getTopPlotEdge() + config.height() / 2.;

            // we want the bounding box centered at pixelX, pixelY
            Rectangle2D bb = StringUtils.boundingBox(g, yRightAxis.getTitle());
            double width = Math.max(bb.getWidth() * Math.abs(Math.cos(yRightAxis.getRotateTitle())), bb.getHeight() * Math.abs(Math.sin(yRightAxis.getRotateTitle())));
            pixelX += width;

            addAnnotation(g, yRightAxis.getTitle(), pixelX, pixelY, yRightAxis.getRotateTitle());
        }

    }

    /**
     * draw the axes
     */
    public void drawAxes() {
        drawLeftAxis();
        drawLowerAxis();
        drawRightAxis();
        drawUpperAxis();

        drawTitle();
    }

    public void drawTitle() {
        if (!config.title().isEmpty()) {
            Graphics2D g = image.createGraphics();
            configureHintsForSubpixelQuality(g);

            int x = config.leftMargin() + config.width() / 2;

            g.setFont(config.titleFont());
            g.setColor(Color.BLACK);
            addAnnotation(g, config.title(), x, config.topMargin() * 0.2, Keyword.ALIGN_CENTER, Keyword.ALIGN_CENTER);
        }
    }

    /**
     * Set the lower X and left Y axes. The upper and right axes are the same as the lower and left
     * axes without labels.
     *
     * @param xLowerAxis lower axis
     * @param yLeftAxis  left axis
     */
    public void setAxes(AxisX xLowerAxis, AxisY yLeftAxis) {
        AxisX xUpperAxis = new AxisX(xLowerAxis);
        xUpperAxis.setTitle("");
        AxisY yRightAxis = new AxisY(yLeftAxis);
        yRightAxis.setTitle("");
        setAxes(xLowerAxis, yLeftAxis, xUpperAxis, yRightAxis);
    }

    /**
     * Set both X and the left Y axes. The right Y axis is the same as the left without labels.
     *
     * @param xLowerAxis lower axis
     * @param yLeftAxis  left axis
     * @param xUpperAxis upper axis
     */
    public void setAxes(AxisX xLowerAxis, AxisY yLeftAxis, AxisX xUpperAxis) {
        this.xLowerAxis = xLowerAxis;
        this.yLeftAxis = yLeftAxis;
        this.xUpperAxis = xUpperAxis;

        yRightAxis = new AxisY(yLeftAxis);
        yRightAxis.setTitle("");
    }

    /**
     * Set the lower X and both Y axes. The upper X axis is the same as the lower without labels.
     *
     * @param xLowerAxis lower axis
     * @param yLeftAxis  left axis
     * @param yRightAxis right axis
     */
    public void setAxes(AxisX xLowerAxis, AxisY yLeftAxis, AxisY yRightAxis) {
        this.xLowerAxis = xLowerAxis;
        this.yLeftAxis = yLeftAxis;
        this.yRightAxis = yRightAxis;

        xUpperAxis = new AxisX(xLowerAxis);
        xUpperAxis.setTitle("");
    }

    /**
     * set all four axes
     *
     * @param xLowerAxis lower axis
     * @param yLeftAxis  left axis
     * @param xUpperAxis upper axis
     * @param yRightAxis right axis
     */
    public void setAxes(AxisX xLowerAxis, AxisY yLeftAxis, AxisX xUpperAxis, AxisY yRightAxis) {
        this.xLowerAxis = xLowerAxis;
        this.yLeftAxis = yLeftAxis;
        this.xUpperAxis = xUpperAxis;
        this.yRightAxis = yRightAxis;
    }

    /**
     * Draw a grid in the plot area connecting the major tick marks on the lower X and left Y axes.
     */
    public void drawGrid() {
        drawGrid(xLowerAxis, yLeftAxis);
    }

    /**
     * Draw a grid in the plot area connecting the major tick marks on the supplied X and Y axes.
     *
     * @param xAxis X axis
     * @param yAxis Y axis
     */
    public void drawGrid(AxisX xAxis, AxisY yAxis) {
        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setFont(config.axisFont());
        g.setColor(config.gridColor());

        NavigableMap<Double, String> tickLabels = xAxis.getTickLabels();
        for (double majorTick : tickLabels.keySet()) {
            if (!xAxis.getRange().closedContains(majorTick)) continue;
            double pixelX = xAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), majorTick);
            double pixelY0 = yAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), yAxis.getRange().getBegin());
            double pixelY1 = yAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), yAxis.getRange().getEnd());

            Path2D.Double path = new Path2D.Double();
            path.moveTo(pixelX, pixelY0);
            path.lineTo(pixelX, pixelY1);

            g.draw(path);
        }

        tickLabels = yAxis.getTickLabels();
        for (double majorTick : tickLabels.keySet()) {
            if (!yAxis.getRange().closedContains(majorTick)) continue;
            double pixelY = yAxis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), majorTick);
            double pixelX0 = xAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), xAxis.getRange().getBegin());
            double pixelX1 = xAxis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), xAxis.getRange().getEnd());

            Path2D.Double path = new Path2D.Double();
            path.moveTo(pixelX0, pixelY);
            path.lineTo(pixelX1, pixelY);

            g.draw(path);
        }
    }

    /**
     * Shade the X range of the graph
     *
     * @param axis  axis
     * @param range data ranges to shade
     * @param color color to use for shading
     */
    public void shadeRange(AxisX axis, Collection<Interval> range, Color color) {
        for (Interval interval : range)
            shadeRange(axis, interval, color);
    }

    /**
     * Shade the X range of the graph
     *
     * @param axis  axis
     * @param range data range to shade
     * @param color color to use for shading
     */
    public void shadeRange(AxisX axis, Interval range, Color color) {
        double pixelBegin = dataXtoPixel(axis, axis.getRange().clamp(range.getInf()));
        double pixelEnd = dataXtoPixel(axis, axis.getRange().clamp(range.getSup()));

        double xMin = Math.floor(Math.min(pixelBegin, pixelEnd));
        double xWidth = Math.ceil(Math.max(pixelBegin, pixelEnd) - xMin);

        double yMin = config.topMargin();
        double yHeight = pageHeight - config.bottomMargin() - yMin;

        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setColor(color);
        g.fill(new Rectangle2D.Double(xMin, yMin, xWidth, yHeight));
    }

    /**
     * Shade the Y range of the graph
     *
     * @param axis  axis
     * @param range data ranges to shade
     * @param color color to use for shading
     */
    public void shadeRange(AxisY axis, Collection<Interval> range, Color color) {
        for (Interval interval : range)
            shadeRange(axis, interval, color);
    }

    /**
     * Shade the Y range of the graph
     *
     * @param axis  axis
     * @param range data range to shade
     * @param color color to use for shading
     */
    public void shadeRange(AxisY axis, Interval range, Color color) {
        double pixelBegin = dataYtoPixel(axis, axis.getRange().clamp(range.getInf()));
        double pixelEnd = dataYtoPixel(axis, axis.getRange().clamp(range.getSup()));

        double yMin = Math.floor(Math.min(pixelBegin, pixelEnd));
        double yHeight = Math.ceil(Math.max(pixelBegin, pixelEnd) - yMin);

        double xMin = config.getLeftPlotEdge();
        double xWidth = config.getRightPlotEdge() - xMin;

        Graphics2D g = image.createGraphics();
        configureHintsForSubpixelQuality(g);
        g.setColor(color);
        g.fill(new Rectangle2D.Double(xMin, yMin, xWidth, yHeight));
    }

    /**
     * @param axis axis
     * @param x    value
     * @return pixel value along the X axis for the given data value.
     */
    public double dataXtoPixel(AxisX axis, double x) {
        return axis.dataToPixel(config.leftMargin(), pageWidth - config.rightMargin(), x);
    }

    /**
     * @param axis axis
     * @param x    value
     * @return data value along the X axis for the given pixel value.
     */
    public double pixelXtoData(AxisX axis, double x) {
        return axis.pixelToData(config.leftMargin(), pageWidth - config.rightMargin(), x);
    }

    /**
     * @param axis axis
     * @param y    value
     * @return pixel value along the Y axis for the given data value.
     */
    public double dataYtoPixel(AxisY axis, double y) {
        return axis.dataToPixel(pageHeight - config.bottomMargin(), config.topMargin(), y);
    }

    /**
     * @param axis axis
     * @param y    value
     * @return data value along the Y axis for the given pixel value.
     */
    public double pixelYtoData(AxisY axis, double y) {
        return axis.pixelToData(pageHeight - config.bottomMargin(), config.topMargin(), y);
    }

}
