/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.olingo.odata2.api.uri.expression;

/**
 * Enumerations for all supported methods of the ODATA expression parser
 * for ODATA version 2.0 (with some restrictions).
 *
 */
public enum MethodOperator {
  ENDSWITH("endswith"),
  INDEXOF("indexof"),
  STARTSWITH("startswith"),
  TOLOWER("tolower"),
  TOUPPER("toupper"),
  TRIM("trim"),
  SUBSTRING("substring"),
  SUBSTRINGOF("substringof"),
  CONCAT("concat"),
  LENGTH("length"),
  YEAR("year"),
  MONTH("month"),
  DAY("day"),
  HOUR("hour"),
  MINUTE("minute"),
  SECOND("second"),
  ROUND("round"),
  FLOOR("floor"),
  CEILING("ceiling"),
  HAS("has"),
  HASANY("hasany"),
  BY_GROUPID("bygroupid");

  private String syntax;
  private String stringRespresentation;

  private MethodOperator(final String syntax) {
    this.syntax = syntax;
    stringRespresentation = syntax;
  }

  /**
   * @return Operators name for usage in in text
   */
  @Override
  public String toString() {
    return stringRespresentation;
  }

  /**
   * @return URI literal of the unary operator as used in the URL.
   */
  public String toUriLiteral() {
    return syntax;
  }

}
