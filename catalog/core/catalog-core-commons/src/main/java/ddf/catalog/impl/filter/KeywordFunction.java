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
package ddf.catalog.impl.filter;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

public class KeywordFunction extends CustomFunctionImpl {

  private static final FilterFactory FF = new FilterFactoryImpl();

  private static final int NUM_PARAMETERS = 3;

  private static final String FUNCTION_NAME_STRING = "keyword";

  public static final FunctionName FUNCTION_NAME =
      functionName(
          FUNCTION_NAME_STRING,
          "result:Boolean",
          "property:String",
          "location:String",
          "name:String");

  public KeywordFunction(List<Expression> parameters, Literal fallback) {
    notNull(parameters, "Parameters are required");
    isTrue(
        parameters.size() == NUM_PARAMETERS,
        String.format("Keyword expression requires at least %s parameters", NUM_PARAMETERS));

    this.setName(FUNCTION_NAME_STRING);
    this.setParameters(parameters);
    this.setFallbackValue(fallback);
    this.functionName = FUNCTION_NAME;
  }

  @Override
  public Filter retrieveProxyFilter(Filter filter) {
    if (filter instanceof PropertyIsEqualTo) {
      return ((FilterFactoryImpl) FF)
          .intersects(
              this.getParameters().get(0),
              this.getParameters().get(1),
              ((PropertyIsEqualTo) filter).getMatchAction());
    }
    return filter;
  }
}
