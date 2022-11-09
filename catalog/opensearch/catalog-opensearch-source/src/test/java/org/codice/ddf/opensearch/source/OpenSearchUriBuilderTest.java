/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.opensearch.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ddf.catalog.data.Result;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Arrays;
import java.util.Date;
import org.apache.http.client.utils.URIBuilder;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class OpenSearchUriBuilderTest {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final String MAX_RESULTS = "2000";

  private static final String TIMEOUT = "30000";

  private static final String DESCENDING_TEMPORAL_SORT = "date:desc";

  private static final String WKT_GEOMETRY =
      "GEOMETRYCOLLECTION (POINT (-105.2071712 40.0160994), LINESTRING (4 6, 7 10))";

  private URIBuilder uriBuilder;

  @Before
  public void setUp() {
    uriBuilder = mock(URIBuilder.class);
  }

  // {@link OpenSearchParser#populateTemporal(WebClient, TemporalFilter, List)} tests

  @Test
  public void populateTemporal() {
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    Date start = new Date(System.currentTimeMillis() - 10000000);
    Date end = new Date(System.currentTimeMillis());
    TemporalFilter temporal = new TemporalFilter(start, end);

    OpenSearchUriBuilder.populateTemporal(
        uriBuilder,
        temporal,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.DATE_START, fmt.print(start.getTime()));
    assertQueryParameterPopulated(OpenSearchConstants.DATE_END, fmt.print(end.getTime()));
  }

  @Test
  public void populateEmptyTemporal() {
    TemporalFilter temporalFilter = new TemporalFilter(0L);
    temporalFilter.setEndDate(null);
    temporalFilter.setStartDate(null);

    OpenSearchUriBuilder.populateTemporal(
        uriBuilder,
        temporalFilter,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.DATE_START);
    assertQueryParameterPopulated(OpenSearchConstants.DATE_END);
  }

  @Test
  public void populateNullTemporal() {
    OpenSearchUriBuilder.populateTemporal(
        uriBuilder,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertNoQueryParametersPopulated();
  }

  // {@link OpenSearchParser#populateSearchOptions(WebClient, QueryRequest, Subject, List)} tests

  @Test
  public void populateSearchOptions() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_RESULTS, MAX_RESULTS);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterPopulated(OpenSearchConstants.SORT, DESCENDING_TEMPORAL_SORT);
  }

  @Test
  public void populateSearchOptionsSortAscending() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_RESULTS, MAX_RESULTS);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterPopulated(OpenSearchConstants.SORT, "date:asc");
  }

  @Test
  public void populateSearchOptionsSortRelevance() {
    SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_RESULTS, MAX_RESULTS);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterPopulated(OpenSearchConstants.SORT, "relevance:desc");
  }

  @Test
  public void populateSearchOptionsSortRelevanceUnsupported() {
    SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_RESULTS, MAX_RESULTS);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterNotPopulated(OpenSearchConstants.SORT);
  }

  @Test
  public void populateSearchOptionsNullSort() {
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, null, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_RESULTS, MAX_RESULTS);
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterNotPopulated(OpenSearchConstants.SORT);
  }

  @Test
  public void populateSearchOptionsNegativePageSize() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, -1000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        queryRequest,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterPopulated(
        OpenSearchConstants.MAX_RESULTS, OpenSearchUriBuilder.DEFAULT_TOTAL_MAX.toString());
    assertQueryParameterPopulated(OpenSearchConstants.MAX_TIMEOUT, TIMEOUT);
    assertQueryParameterPopulated(OpenSearchConstants.SORT, DESCENDING_TEMPORAL_SORT);
  }

  @Test
  public void populateNullSearchOptions() {
    OpenSearchUriBuilder.populateSearchOptions(
        uriBuilder,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterNotPopulated(OpenSearchConstants.COUNT);
    assertQueryParameterNotPopulated(OpenSearchConstants.MAX_RESULTS);
    assertQueryParameterNotPopulated(OpenSearchConstants.SOURCES);
    assertQueryParameterNotPopulated(OpenSearchConstants.MAX_TIMEOUT);
    assertQueryParameterNotPopulated(OpenSearchUriBuilder.USER_DN);
    assertQueryParameterNotPopulated(OpenSearchUriBuilder.FILTER);
    assertQueryParameterNotPopulated(OpenSearchConstants.SORT);
  }

  // {@link OpenSearchParser#populateSpatial(WebClient, SpatialSearch, List)} tests

  @Test
  public void populateSpatialGeometry() throws ParseException {

    Geometry geometry = new WKTReader().read(WKT_GEOMETRY);
    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        geometry,
        null,
        null,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,geometry,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.GEOMETRY, WKT_GEOMETRY);
  }

  @Test
  public void populateSpatialBoundingBox() {
    final BoundingBox boundingBox = new BoundingBox(170, 50, -150, 60);

    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        null,
        boundingBox,
        null,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.BBOX, "170.0,50.0,-150.0,60.0");
  }

  @Test
  public void populateSpatialPolygon() {
    final Polygon polygon =
        GEOMETRY_FACTORY.createPolygon(
            GEOMETRY_FACTORY.createLinearRing(
                new Coordinate[] {
                  new Coordinate(1, 1),
                  new Coordinate(5, 1),
                  new Coordinate(5, 5),
                  new Coordinate(1, 5),
                  new Coordinate(1, 1)
                }),
            null);

    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        null,
        null,
        polygon,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(
        OpenSearchConstants.POLYGON, "1.0,1.0,1.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0");
  }

  @Test
  public void populateSpatialPointRadius() {
    double lat = 43.25;
    double lon = -123.45;
    double radius = 10000;

    PointRadius pointRadius = new PointRadius(lon, lat, radius);
    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        null,
        null,
        null,
        pointRadius,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.LAT, String.valueOf(lat));
    assertQueryParameterPopulated(OpenSearchConstants.LON, String.valueOf(lon));
    assertQueryParameterPopulated(OpenSearchConstants.RADIUS, String.valueOf(radius));
  }

  @Test
  public void populateNullSpatial() {
    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        null,
        null,
        null,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertNoQueryParametersPopulated();
  }

  @Test
  public void populateMultipleSearchesSpatial() throws ParseException {
    double lat = 43.25;
    double lon = -123.45;
    double radius = 10000;

    final PointRadius pointRadius = new PointRadius(lon, lat, radius);
    final Polygon polygon =
        GEOMETRY_FACTORY.createPolygon(
            GEOMETRY_FACTORY.createLinearRing(
                new Coordinate[] {
                  new Coordinate(1, 1),
                  new Coordinate(5, 1),
                  new Coordinate(5, 5),
                  new Coordinate(1, 5),
                  new Coordinate(1, 1)
                }),
            null);

    final Geometry geometry = new WKTReader().read(WKT_GEOMETRY);

    final BoundingBox boundingBox = new BoundingBox(170, 50, -150, 60);

    OpenSearchUriBuilder.populateSpatial(
        uriBuilder,
        geometry,
        boundingBox,
        polygon,
        pointRadius,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,geometry,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));

    assertQueryParameterPopulated(OpenSearchConstants.GEOMETRY, WKT_GEOMETRY);
    assertQueryParameterPopulated(
        OpenSearchConstants.POLYGON, "1.0,1.0,1.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0");
    assertQueryParameterPopulated(OpenSearchConstants.BBOX, "170.0,50.0,-150.0,60.0");
    assertQueryParameterPopulated(OpenSearchConstants.LAT, String.valueOf(lat));
    assertQueryParameterPopulated(OpenSearchConstants.LON, String.valueOf(lon));
    assertQueryParameterPopulated(OpenSearchConstants.RADIUS, String.valueOf(radius));
  }

  private void assertQueryParameterPopulated(final String queryParameterName) {
    verify(uriBuilder, times(1)).setParameter(eq(queryParameterName), any());
  }

  private void assertQueryParameterPopulated(
      final String queryParameterName, final String queryParameterValue) {
    verify(uriBuilder, times(1)).setParameter(queryParameterName, queryParameterValue);
  }

  private void assertQueryParameterNotPopulated(final String queryParameterName) {
    verify(uriBuilder, never()).setParameter(eq(queryParameterName), any());
  }

  private void assertNoQueryParametersPopulated() {
    verifyNoInteractions(uriBuilder);
  }
}