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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableList;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.filter.impl.PropertyNameImpl;
import java.util.ArrayList;
import java.util.List;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;

public class KeywordFunctionTest {

  private static final FilterFactory FF = new FilterFactoryImpl();

  private static final PropertyName SOURCE_PROPERTY_NAME = new PropertyNameImpl("keyword");

  private static final String ANY_GEO = "anyGeo";

  private static final String WKT_LOCATION =
      "POLYGON((33.9634 9.4643,33.975 8.6846,33.8255 8.3792,33.2948 8.3546,32.9542 7.785,33.5683 7.7133,34.0751 7.226,34.2503 6.8261,34.707 6.5942,35.298 5.506,33.39 3.79,32.6864 3.7923,31.8815 3.5583,31.2456 3.7819,30.8339 3.5092,29.9535 4.1737,29.716 4.6008,28.429 4.2872,27.98 4.4084,27.2134 5.551,26.4659 5.9467,26.2134 6.5466,25.1241 7.5001,25.1149 7.8251,23.887 8.6197,24.5374 8.9175,25.0696 10.2738,25.7906 10.4111,26.4773 9.5527,26.752 9.4669,27.1125 9.6386,27.8336 9.6042,27.9709 9.3982,28.9666 9.3982,29.0009 9.6042,29.516 9.7931,29.619 10.0849,29.9966 10.2909,30.8378 9.7072,31.3529 9.8102,32.4001 11.0806,32.3142 11.6815,32.0739 11.9733,32.6748 12.0248,32.7434 12.248,33.2069 12.1793,33.0868 11.4411,33.2069 10.7201,33.722 10.3253,33.825 9.4841,33.9634 9.4643))";

  private static final String KEYWORD_STRING = "South Sudan";

  private static final Literal SOURCE_LITERAL =
      new LiteralExpressionImpl("(" + ANY_GEO + ", " + WKT_LOCATION + ", " + KEYWORD_STRING + ")");

  private static final Expression EXPRESSION1 = FF.literal("anyGeo");

  private static final Expression EXPRESSION2 = FF.literal(WKT_LOCATION);

  private static final Expression EXPRESSION3 = FF.literal("South Sudan");

  private static final Filter EQUAL_FILTER =
      new PropertyIsEqualToLiteral(SOURCE_PROPERTY_NAME, SOURCE_LITERAL);

  private static final Intersects INTERSECTS_FILTER =
      ((FilterFactoryImpl) FF).intersects(EXPRESSION1, EXPRESSION2, null);

  private static final Within WITHIN_FILTER =
      ((FilterFactoryImpl) FF).within(EXPRESSION1, EXPRESSION2);

  private List<Expression> exprs =
      new ImmutableList.Builder<Expression>()
          .add(EXPRESSION1)
          .add(EXPRESSION2)
          .add(EXPRESSION3)
          .build();

  @Test(expected = NullPointerException.class)
  public void testVerifyKeywordFunctionCannotBeCalledWithNull() {
    new KeywordFunction(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testVerifyKeywordFunctionWithWrongParameter() {
    List<Expression> invalidExprs = new ArrayList<>();
    invalidExprs.add(Expression.NIL);
    invalidExprs.add(Expression.NIL);
    new KeywordFunction(invalidExprs, null);
  }

  @Test
  public void testVerifyKeywordFunction() {
    KeywordFunction func = new KeywordFunction(exprs, null);
    assertThat(func.getName(), is(KeywordFunction.FUNCTION_NAME.getName()));
  }

  @Test
  public void testKeywordFilterProxy() {
    KeywordFunction func = new KeywordFunction(exprs, null);
    assertThat(func.retrieveProxyFilter(EQUAL_FILTER), is(INTERSECTS_FILTER));
  }

  @Test
  public void testFilterProxyReturnOriginal() {
    KeywordFunction func = new KeywordFunction(exprs, null);
    assertThat(func.retrieveProxyFilter(WITHIN_FILTER), is(WITHIN_FILTER));
  }
}
