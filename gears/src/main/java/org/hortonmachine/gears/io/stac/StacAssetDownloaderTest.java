package org.hortonmachine.gears.io.stac;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;

public class StacAssetDownloaderTest extends TestVariables {

    @SuppressWarnings({"unchecked"})
    public StacAssetDownloaderTest() throws Exception {

        try (STACClient stacClient = new STACClient(new URL(repoUrl), new MultithreadedHttpClient())) {
            SearchQuery search = new SearchQuery();
            search.setCollections(Arrays.asList(collectionQuery));
            search.setDatetime(dayQuery + "T00:00:00.000000Z/" + dayQuery + "T23:59:59.999999Z");
            search.setIntersects(intersectionGeometry);
            search.setFilter(CQL.toFilter(CQL_FILTER));
            search.setLimit(limit); // limit doesn't seem to work properly

            System.out.println("Search collection with query:");
            System.out.println(search.toString());
            System.out.println("==============================================================================");

            SimpleFeatureCollection fc = stacClient.search(search, STACClient.SearchMode.GET);
            // int size = fc.size();
            // System.out.println("Found " + size + " features matching the query.");
            // System.out.println();

            StacFeatures stacfeatures = getUniqueFeatures(fc);
            processStacFeatures(stacfeatures);
        }

    }

    @SuppressWarnings("unchecked")
    private void processStacFeatures( StacFeatures stacfeatures )
            throws TransformException, FactoryException, IOException, DataSourceException, Exception {
        int size = stacfeatures.getSize();
        if (size > 0) {
            Geometry coveredAreas = stacfeatures.getCoveredAreas();
            Geometry commonArea = coveredAreas.intersection(intersectionGeometry);
            double coveredArea = commonArea.getArea();
            double roiArea = intersectionGeometry.getArea();
            int percentage = (int) Math.round(coveredArea * 100 / roiArea);
            System.out.println("Found " + size + " unique features matching the query.");
            System.out.println("Region of interest is covered by data in amout of " + percentage + "%");
            System.out.println();

            Integer srid = stacfeatures.epsgs.get(0); // TODO check if this is enough
            CoordinateReferenceSystem outputCrs = CrsUtilities.getCrsFromSrid(srid);
            ReferencedEnvelope roiEnvelope = new ReferencedEnvelope(intersectionGeometry.getEnvelopeInternal(),
                    DefaultGeographicCRS.WGS84).transform(outputCrs, true);

            RegionMap regionMap = RegionMap.fromEnvelopeAndGrid(roiEnvelope, downloadCols, downloadRows);
            HMRaster outRaster = null;
            String fileName = null;

            int featureCount = 0;
            System.out.println("Processing features...");
            for( int i = 0; i < size; i++ ) {
                if (featureCount++ % 10 == 0) {
                    System.out.println(featureCount + "/" + size);
                }
                String id = stacfeatures.ids.get(i);
                String ts = stacfeatures.timestamps.get(i);
                int currentSrid = stacfeatures.epsgs.get(i);
                if (srid != currentSrid) {
                    throw new IOException("Epsgs are different");
                }
                Geometry geometry = stacfeatures.geometries.get(i);
                Geometry intersection = geometry.intersection(intersectionGeometry);
                Envelope readEnvelope = intersection.getEnvelopeInternal();

                CoordinateReferenceSystem dataCrs = CrsUtilities.getCrsFromSrid(currentSrid);
                ReferencedEnvelope roiEnv = new ReferencedEnvelope(readEnvelope, DefaultGeographicCRS.WGS84).transform(dataCrs,
                        true);

                GeneralParameterValue[] generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(downloadCols,
                        downloadRows, roiEnv.getMaxY(), roiEnv.getMinY(), roiEnv.getMaxX(), roiEnv.getMinX(),
                        roiEnv.getCoordinateReferenceSystem());

                SimpleFeature feature = stacfeatures.features.get(i);
                Map<String, JsonNode> top = (Map<String, JsonNode>) feature.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
                ObjectNode assets = (ObjectNode) top.get("assets");

                Iterator<JsonNode> assetsIterator = assets.elements();
                while( assetsIterator.hasNext() ) {
                    JsonNode assetNode = assetsIterator.next();
                    JsonNode typeNode = assetNode.get("type");
                    if (typeNode != null) {
                        String type = typeNode.textValue();
                        // we only check cloud optimized datasets here
                        JsonNode titleNode = assetNode.get("title");
                        if (titleNode != null) {
                            String title = titleNode.textValue();

                            if (title.equals(band) && type.toLowerCase().contains("profile=cloud-optimized")) {
                                JsonNode rasterBandNode = assetNode.get("raster:bands");
                                if (rasterBandNode != null && !rasterBandNode.isEmpty()) {
                                    if (outRaster == null) {
                                        // pick novalue of first raster and use that for output
                                        // raster
                                        Iterator<JsonNode> rbIterator = rasterBandNode.elements();
                                        while( rbIterator.hasNext() ) {
                                            JsonNode rbNode = rbIterator.next();
                                            JsonNode noValueNode = rbNode.get("nodata");
                                            if (noValueNode != null) {
                                                double noValue = noValueNode.asDouble();
                                                outRaster = HMRaster.writableFromRegionMap(regionMap, outputCrs, noValue);
                                                break;
                                            }
                                        }
                                    }

                                    String downloadUrl = assetNode.get("href").textValue();
                                    BasicAuthURI cogUri = new BasicAuthURI(downloadUrl, false);
                                    // cogUri.setUser("");
                                    // cogUri.setPassword("");
                                    HttpRangeReader rangeReader = new HttpRangeReader(cogUri.getUri(),
                                            CogImageReadParam.DEFAULT_HEADER_LENGTH);
                                    CogSourceSPIProvider inputProvider = new CogSourceSPIProvider(cogUri, new CogImageReaderSpi(),
                                            new CogImageInputStreamSpi(), rangeReader.getClass().getName());
                                    GeoTiffReader reader = new GeoTiffReader(inputProvider);
                                    GridCoverage2D coverage = reader.read(generalParameter);

                                    outRaster.mapRaster(null, HMRaster.fromGridCoverage(coverage));

                                    int lastSlash = downloadUrl.lastIndexOf('/');
                                    fileName = downloadUrl.substring(lastSlash + 1);
                                    String downloadName = ts + "__" + id + "__" + fileName;
                                    System.out.println(downloadName);

                                    File downloadFolderFile = new File(downloadFolder);
                                    File downloadFile = new File(downloadFolderFile, downloadName);

                                    OmsRasterWriter.writeRaster(downloadFile.getAbsolutePath(), coverage);
                                }
                            }
                        }
                    }

                }

//                    System.out.println(assets);
            }

            GridCoverage2D outCoverage = outRaster.buildCoverage(fileName);
            File downloadFolderFile = new File(downloadFolder);
            File downloadFile = new File(downloadFolderFile, fileName);

            OmsRasterWriter.writeRaster(downloadFile.getAbsolutePath(), outCoverage);
        }
    }

    public static void main( String[] args ) throws Exception {
        new StacAssetDownloaderTest();
    }

}
