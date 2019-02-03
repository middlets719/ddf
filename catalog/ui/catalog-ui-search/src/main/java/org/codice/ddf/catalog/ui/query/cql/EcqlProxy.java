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
package org.codice.ddf.catalog.ui.query.cql;

import ddf.catalog.impl.filter.CustomFunctionImpl;
import java.util.function.Predicate;
import org.geotools.filter.IsEqualsToImpl;
import org.opengis.filter.Filter;

/**
 * Enables us to do dynamic filter conversions of custom ECQL if and only if we have a registered
 * function that matches the input cql.
 */
public class EcqlProxy {
  private static Predicate<Filter> isCustomFilterFunction =
      convertedFilter ->
          convertedFilter instanceof IsEqualsToImpl
              && ((IsEqualsToImpl) convertedFilter).getExpression1() instanceof CustomFunctionImpl;

  public static Filter toProxyFilter(Filter convertedFilter) {
    if (isCustomFilterFunction.test(convertedFilter)) {
      Filter proxyFilter =
          ((CustomFunctionImpl) ((IsEqualsToImpl) convertedFilter).getExpression1())
              .retrieveProxyFilter(convertedFilter);
      return proxyFilter != null ? proxyFilter : convertedFilter;
    }
    return convertedFilter;
  }
}
