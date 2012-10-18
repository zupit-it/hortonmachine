/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gears.libs.modules;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

/**
 * A node in the flow environment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FlowNode {

    public final int row;
    public final int col;
    public final double elevation;
    private RandomIter elevationIter;
    private int cols;
    private int rows;
    private double xRes;
    private double yRes;
    private boolean isOutlet = false;

    /**
     * @param elevationIter the raster iter.
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     * @param xRes the x resolution of the raster.
     * @param yRes the y resolution of the raster.
     * @param col the col of the current {@link FlowNode node}.
     * @param row the row of the current {@link FlowNode node}.
     */
    public FlowNode( RandomIter elevationIter, int cols, int rows, double xRes, double yRes, int col, int row ) {
        this.elevationIter = elevationIter;
        this.cols = cols;
        this.rows = rows;
        this.xRes = xRes;
        this.yRes = yRes;
        this.col = col;
        this.row = row;

        if (isInRaster(col, row)) {
            elevation = elevationIter.getSampleDouble(col, row, 0);
        } else {
            elevation = JGTConstants.doubleNovalue;
        }
    }

    @Override
    public String toString() {
        return "FlowNode [\n\trow=" + row + ", \n\tcol=" + col + ", \n\televation=" + elevation + "\n]";
    }

    /**
     * Checks if the node is valid.
     * 
     * <p>A node is valid if</p>
     * <ul>
     *  <li>it is placed inside the raster bounds</li>
     *  <li>its elevation value is not novalue</li>
     * </ul>
     * 
     * @return <code>true</code> if the node is valid.
     */
    public boolean isValid() {
        if (JGTConstants.isNovalue(elevation)) {
            return false;
        } else if (!isInRaster(col, row)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return <code>true</code> if this node can't flow anywhere following the steepest path downstream. 
     */
    public boolean isOutlet() {
        return isOutlet;
    }

    /**
     * Get next downstream {@link FlowNode node} following the steepest path.
     * 
     * @return the next downstream node or <code>null</code> if it is an outlet.
     */
    public FlowNode goDownstreamSP() {
        Direction[] orderedDirs = Direction.getOrderedDirs();
        double maxSlope = Double.NEGATIVE_INFINITY;
        FlowNode nextNode = null;
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (isInRaster(newCol, newRow)) {
                FlowNode node = new FlowNode(elevationIter, cols, rows, xRes, yRes, newCol, newRow);
                if (node.isValid()) {
                    double slopeTo = getSlopeTo(node);
                    if (slopeTo > 0 && slopeTo > maxSlope) {
                        nextNode = node;
                        maxSlope = slopeTo;
                    }
                }
            }
        }
        if (nextNode == null) {
            isOutlet = true;
        }
        return nextNode;
    }

    /**
     * Get next upstream {@link FlowNode node}, based on least cost.
     * 
     * @return the next least cost, upstream node.
     */
    public FlowNode goLeastCostUpstream() {

        return null;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes}, starting from the most eastern.
     * 
     * Note that the list contains all 8 directions, but some might be null, if outside a boundary 
     * 
     * @return the nodes surrounding the current node. 
     */
    public List<FlowNode> getSurroundingNodes() {
        List<FlowNode> nodes = new ArrayList<FlowNode>();
        Direction[] orderedDirs = Direction.getOrderedDirs();
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (isInRaster(newCol, newRow)) {
                FlowNode node = new FlowNode(elevationIter, cols, rows, xRes, yRes, newCol, newRow);
                if (node.isValid()) {
                    nodes.add(node);
                } else {
                    nodes.add(null);
                }
            } else {
                nodes.add(null);
            }
        }
        return nodes;
    }
    /**
     * Gets all surrounding {@link FlowNode nodes} that <b>DO</b> flow into this node by steepest path rule.
     * 
     * @return the nodes that flow into this node.
     */
    public List<FlowNode> getEnteringNodesSP() {
        List<FlowNode> nodes = new ArrayList<FlowNode>();
        List<FlowNode> surroundingNodes = getSurroundingNodes();
        for( FlowNode flowNode : surroundingNodes ) {
            if (flowNode != null) {
                FlowNode downstream = flowNode.goDownstreamSP();
                if (downstream.isValid() && this.equals(downstream)) {
                    nodes.add(flowNode);
                }
            }
        }
        return nodes;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes} that do <b>NOT</b> flow into this node by steepest path rule.
     * 
     * @return the nodes that flow into this node.
     */
    public List<FlowNode> getNonEnteringNodesSP() {
        List<FlowNode> nodes = new ArrayList<FlowNode>();
        List<FlowNode> surroundingNodes = getSurroundingNodes();
        for( FlowNode flowNode : surroundingNodes ) {
            if (flowNode != null) {
                FlowNode downstream = flowNode.goDownstreamSP();
                if (!downstream.isValid() || !this.equals(downstream)) {
                    nodes.add(flowNode);
                }
            }
        }
        return nodes;
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the slope from the current to the supplied point. 
     * 
     * @param node the node to which to calculate the slope to.
     * @return the slope.
     */
    public double getSlopeTo( FlowNode node ) {
        double slope = (elevation - node.elevation)
                / (sqrt(pow((node.col - col) * xRes, 2.0) + pow((node.row - row) * yRes, 2.0)));
        return slope;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        long temp;
        temp = Double.doubleToLongBits(elevation);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + row;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowNode other = (FlowNode) obj;
        if (col != other.col || row != other.row)
            return false;
        if (Double.doubleToLongBits(elevation) != Double.doubleToLongBits(other.elevation))
            return false;
        return true;
    }

}
