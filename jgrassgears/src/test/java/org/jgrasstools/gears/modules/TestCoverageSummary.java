package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.summary.CoverageSummary;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestCoverageSummary extends HMTestCase {
    public void testCoverageSummary() throws Exception {

        double[][] inData = HMTestMaps.extractNet0Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data",
                inData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        CoverageSummary summary = new CoverageSummary();
        summary.pm = pm;
        summary.inMap = inCoverage;
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        double mean = summary.outMean;
        double sdev = summary.outSdev;
        double range = summary.outRange;
        double sum = summary.outSum;
        //        double approxMedian = summary.outApproxmedian;
        
        System.out.println();
        
    }

}
