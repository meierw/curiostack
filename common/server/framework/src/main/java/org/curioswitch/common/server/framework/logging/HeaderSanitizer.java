/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.curioswitch.common.server.framework.logging;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpHeadersBuilder;
import com.linecorp.armeria.common.RequestContext;
import io.netty.util.AsciiString;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

final class HeaderSanitizer implements BiFunction<RequestContext, HttpHeaders, HttpHeaders> {

  private final Set<Consumer<HttpHeadersBuilder>> customSanitizers;
  private final Set<AsciiString> blacklistedHeaders;

  HeaderSanitizer(
      Set<Consumer<HttpHeadersBuilder>> customSanitizers, List<String> blacklistedHeaders) {
    this.customSanitizers = customSanitizers;
    this.blacklistedHeaders =
        blacklistedHeaders.stream().map(AsciiString::of).collect(toImmutableSet());
  }

  @Override
  public HttpHeaders apply(RequestContext ctx, HttpHeaders entries) {
    HttpHeadersBuilder builder = entries.toBuilder();
    blacklistedHeaders.forEach(builder::remove);
    customSanitizers.forEach(c -> c.accept(builder));
    return builder.build();
  }
}
