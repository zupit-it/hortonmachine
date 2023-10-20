package org.hortonmachine.gears.io.wcs.wcs111.models;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.wcs.ICoverageSummary;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.w3c.dom.Node;

public class CoverageSummary implements ICoverageSummary {
    /**
     * List of coverage summaries to be filled by the visitor.
     */
    private List<ICoverageSummary> coverageSummaries;

    private String title;
    private String abstract_;
    private List<String> keywords = new ArrayList<>();
    private String coverageId;
    private ReferencedEnvelope wgs84BoundingBox;

    public CoverageSummary(List<ICoverageSummary> coverageSummaries) {
        this.coverageSummaries = coverageSummaries;
    }

    public boolean checkElementName(String name) {
        if (name.equals("wcs:CoverageSummary") || name.endsWith(":CoverageSummary"))
            return true;
        return false;
    }

    public void visit(Node node) {
        CoverageSummary cs = new CoverageSummary(null);
        cs.title = XmlHelper.findFirstTextInChildren(node, "label");
        cs.abstract_ = XmlHelper.findFirstTextInChildren(node, "description");

        Node keywordsNode = XmlHelper.findNode(node, "keywords");
        cs.keywords = XmlHelper.findAllTextsInChildren(keywordsNode, "keyword");
        cs.coverageId = XmlHelper.findFirstTextInChildren(node, "name");

        Node bboxNode = XmlHelper.findNode(node, "WGS84BoundingBox");

        if (bboxNode != null) {
            String lowerCorner = XmlHelper.findFirstTextInChildren(bboxNode, "lowercorner");
            String upperCorner = XmlHelper.findFirstTextInChildren(bboxNode, "uppercorner");
            cs.wgs84BoundingBox = new ReferencedEnvelope(
                    Double.parseDouble(lowerCorner.split(" ")[0]),
                    Double.parseDouble(upperCorner.split(" ")[0]),
                    Double.parseDouble(lowerCorner.split(" ")[1]),
                    Double.parseDouble(upperCorner.split(" ")[1]),
                    DefaultGeographicCRS.WGS84);
        }
        coverageSummaries.add(cs);
    }

    public String toString() {
        String s = "";
        s += "title: " + title + "\n";
        s += "\tabstract: " + abstract_ + "\n";
        s += "\tkeywords: \n";
        if (keywords != null && keywords.size() > 0)
            for (String k : keywords) {
                s += "\t\t" + k + "\n";
            }
        s += "\tcoverageId: " + coverageId + "\n";
        s += "\twgs84BoundingBox: " + wgs84BoundingBox + "\n";
        return s;
    }

    @Override
    public ReferencedEnvelope getBoundingBox() {
        return wgs84BoundingBox;
    }

    @Override
    public ReferencedEnvelope getWgs84BoundingBox() {
        return wgs84BoundingBox;
    }

    @Override
    public String getCoverageId() {
        return coverageId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getAbstract() {
        return abstract_;
    }
}
