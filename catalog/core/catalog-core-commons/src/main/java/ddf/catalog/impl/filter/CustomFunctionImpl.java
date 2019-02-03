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

import org.geotools.filter.FunctionImpl;
import org.opengis.filter.Filter;

/**
 * Allows us to extend the capability of a standard geotools filter function by baking in the
 * conversion of a given ECQL filter into a filter that can be interpreted to follow the actual CQL
 * specification.
 */
public abstract class CustomFunctionImpl extends FunctionImpl {
  public Filter retrieveProxyFilter(Filter filter) {
    return filter;
  };
}
