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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionType;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader.TransactionMessageBodyReader;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer.CswRecordCollectionMessageBodyWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswServlet.class);

  private final CswEndpoint endpoint;

  private final CswXmlParser parser;

  private final TransactionMessageBodyReader transactionReader;

  private final CswRecordCollectionMessageBodyWriter recordCollectionWriter;

  private final CswExceptionMapper exceptionMapper;

  public CswServlet(
      CswEndpoint cswEndpoint,
      CswXmlParser parser,
      TransactionMessageBodyReader transactionReader,
      CswRecordCollectionMessageBodyWriter recordCollectionWriter,
      CswExceptionMapper exceptionMapper) {
    this.endpoint = cswEndpoint;
    this.parser = parser;
    this.transactionReader = transactionReader;
    this.recordCollectionWriter = recordCollectionWriter;
    this.exceptionMapper = exceptionMapper;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      String service = req.getParameter("service");
      if (service == null) {
        throw new CswException(
            "Missing service value", CswConstants.MISSING_PARAMETER_VALUE, "service");
      }
      if (!"csw".equalsIgnoreCase(service)) {
        throw new CswException(
            "Unknown service (" + service + ")", CswConstants.INVALID_PARAMETER_VALUE, "service");
      }

      String version = req.getParameter("version");
      if (version != null && !version.contains("2.0.2")) {
        throw new CswException(
            "Version(s) ("
                + version
                + ") is not supported, we currently support version "
                + CswConstants.VERSION_2_0_2,
            CswConstants.VERSION_NEGOTIATION_FAILED,
            null);
      }

      String request = req.getParameter("request");
      if ("getcapabilities".equalsIgnoreCase(request)) {
        GetCapabilitiesRequest getCapabilitiesRequest = new GetCapabilitiesRequest();
        getCapabilitiesRequest.setAcceptVersions(version);
        getCapabilitiesRequest.setSections(req.getParameter("sections"));
        getCapabilitiesRequest.setUpdateSequence(req.getParameter("updateSequence"));
        getCapabilitiesRequest.setAcceptFormats(req.getParameter("acceptFormats"));

        CapabilitiesType getCapabilitiesResponse = endpoint.getCapabilities(getCapabilitiesRequest);

        JAXBElement<CapabilitiesType> jaxbElement =
            new ObjectFactory().createCapabilities(getCapabilitiesResponse);
        parser.marshal(jaxbElement, resp.getOutputStream());
      } else if ("describerecord".equalsIgnoreCase(request)) {
        DescribeRecordRequest describeRecordRequest = new DescribeRecordRequest();
        describeRecordRequest.setVersion(version);
        describeRecordRequest.setNamespace(req.getParameter("namespace"));
        describeRecordRequest.setTypeName(req.getParameter("typeName"));
        describeRecordRequest.setOutputFormat(req.getParameter("outputFormat"));
        describeRecordRequest.setSchemaLanguage(req.getParameter("schemaLanguage"));

        DescribeRecordResponseType describeRecordResponse =
            endpoint.describeRecord(describeRecordRequest);

        JAXBElement<DescribeRecordResponseType> jaxbElement =
            new ObjectFactory().createDescribeRecordResponse(describeRecordResponse);
        parser.marshal(jaxbElement, resp.getOutputStream());
      } else if ("getrecords".equalsIgnoreCase(request)) {
        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
        getRecordsRequest.setVersion(version);
        getRecordsRequest.setRequestId(req.getParameter("requestId"));
        getRecordsRequest.setNamespace(req.getParameter("namespace"));
        getRecordsRequest.setResultType(req.getParameter("resultType"));
        getRecordsRequest.setOutputFormat(req.getParameter("outputFormat"));
        getRecordsRequest.setOutputSchema(req.getParameter("outputSchema"));
        if (req.getParameter("startPosition") != null) {
          getRecordsRequest.setStartPosition(new BigInteger(req.getParameter("startPosition")));
        }
        if (req.getParameter("maxRecords") != null) {
          getRecordsRequest.setMaxRecords(new BigInteger(req.getParameter("maxRecords")));
        }
        getRecordsRequest.setTypeNames(req.getParameter("typeNames"));
        getRecordsRequest.setElementName(req.getParameter("elementName"));
        getRecordsRequest.setElementSetName(req.getParameter("elementSetName"));
        getRecordsRequest.setConstraintLanguage(req.getParameter("constraintLanguage"));
        getRecordsRequest.setConstraint(req.getParameter("constraint"));
        getRecordsRequest.setSortBy(req.getParameter("sortBy"));
        if (req.getParameter("distributedSearch") != null) {
          getRecordsRequest.setDistributedSearch(
              Boolean.getBoolean(req.getParameter("distributedSearch")));
        }
        if (req.getParameter("hopCount") != null) {
          getRecordsRequest.setHopCount(new BigInteger(req.getParameter("hopCount")));
        }
        getRecordsRequest.setResponseHandler(req.getParameter("responseHandler"));

        CswRecordCollection getRecordsResponse = endpoint.getRecords(getRecordsRequest);

        recordCollectionWriter.writeTo(getRecordsResponse, resp);
      } else if ("getrecordbyid".equalsIgnoreCase(request)) {
        GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
        getRecordByIdRequest.setId(req.getParameter("id"));
        getRecordByIdRequest.setOutputFormat(req.getParameter("outputFormat"));
        getRecordByIdRequest.setOutputSchema(req.getParameter("outputSchema"));
        getRecordByIdRequest.setElementSetName(req.getParameter("elementSetName"));

        CswRecordCollection getRecordByIdResponse =
            endpoint.getRecordById(getRecordByIdRequest, req.getHeader(CswConstants.RANGE_HEADER));

        recordCollectionWriter.writeTo(getRecordByIdResponse, resp);
      } else {
        throw new CswException(
            "Unknown request (" + request + ") for service (" + service + ")",
            CswConstants.INVALID_PARAMETER_VALUE,
            "request");
      }
    } catch (CswException | ParserException | CatalogTransformerException | RuntimeException e) {
      exceptionMapper.sendExceptionReport(e, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try (TemporaryFileBackedOutputStream requestStream = new TemporaryFileBackedOutputStream()) {
      req.getInputStream().transferTo(requestStream);
      requestStream.flush();
      JAXBElement request;
      try (InputStream jaxbStream = requestStream.asByteSource().openBufferedStream()) {
        request = parser.unmarshal(JAXBElement.class, jaxbStream);
      }
      if (GetCapabilitiesType.class.equals(request.getDeclaredType())) {
        GetCapabilitiesType getCapabilitiesRequest = (GetCapabilitiesType) request.getValue();

        CapabilitiesType getCapabilitiesResponse = endpoint.getCapabilities(getCapabilitiesRequest);

        JAXBElement<CapabilitiesType> jaxbElement =
            new ObjectFactory().createCapabilities(getCapabilitiesResponse);
        parser.marshal(jaxbElement, resp.getOutputStream());
      } else if (DescribeRecordType.class.equals(request.getDeclaredType())) {
        DescribeRecordType describeRecordRequest = (DescribeRecordType) request.getValue();

        DescribeRecordResponseType describeRecordResponse =
            endpoint.describeRecord(describeRecordRequest);

        JAXBElement<DescribeRecordResponseType> jaxbElement =
            new ObjectFactory().createDescribeRecordResponse(describeRecordResponse);
        parser.marshal(jaxbElement, resp.getOutputStream());
      } else if (GetRecordsType.class.equals(request.getDeclaredType())) {
        GetRecordsType getRecordsRequest = (GetRecordsType) request.getValue();

        CswRecordCollection getRecordsResponse = endpoint.getRecords(getRecordsRequest);

        recordCollectionWriter.writeTo(getRecordsResponse, resp);
      } else if (GetRecordByIdType.class.equals(request.getDeclaredType())) {
        GetRecordByIdType getRecordByIdRequest = (GetRecordByIdType) request.getValue();

        CswRecordCollection getRecordByIdResponse =
            endpoint.getRecordById(getRecordByIdRequest, req.getHeader(CswConstants.RANGE_HEADER));

        recordCollectionWriter.writeTo(getRecordByIdResponse, resp);
      } else if (TransactionType.class.equals(request.getDeclaredType())) {
        CswTransactionRequest cswTransactionRequest;
        try (InputStream xstreamStream = requestStream.asByteSource().openBufferedStream()) {
          cswTransactionRequest = transactionReader.readFrom(xstreamStream);
        }

        TransactionResponseType transactionResponse = endpoint.transaction(cswTransactionRequest);

        JAXBElement<TransactionResponseType> jaxbElement =
            new ObjectFactory().createTransactionResponse(transactionResponse);
        parser.marshal(jaxbElement, resp.getOutputStream());
      } else {
        throw new CswException(
            "Unknown request type for csw service",
            CswConstants.INVALID_PARAMETER_VALUE,
            "request");
      }
    } catch (CswException | ParserException | CatalogTransformerException | RuntimeException e) {
      exceptionMapper.sendExceptionReport(e, resp);
    }
  }
}