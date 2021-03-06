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

package org.curioswitch.cafemap.server.places;

import static com.google.common.base.Preconditions.checkArgument;
import static org.curioswitch.database.cafemapdb.tables.Place.PLACE;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dagger.BindsInstance;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import org.curioswitch.cafemap.api.GetPlaceRequest;
import org.curioswitch.cafemap.api.GetPlaceResponse;
import org.curioswitch.common.server.framework.database.ForDatabase;
import org.curioswitch.common.server.framework.grpc.Unvalidated;
import org.curioswitch.database.cafemapdb.tables.pojos.Place;
import org.jooq.DSLContext;
import org.jooq.types.ULong;

@ProducerModule
public abstract class GetPlaceGraph {

  @ProductionSubcomponent(modules = GetPlaceGraph.class)
  public interface Component {
    ListenableFuture<GetPlaceResponse> execute();

    @ProductionSubcomponent.Builder
    interface Builder {
      Builder setRequest(@BindsInstance @Unvalidated GetPlaceRequest request);

      Component build();
    }
  }

  @Produces
  static GetPlaceRequest validateRequest(@Unvalidated GetPlaceRequest request) {
    checkArgument(!request.getId().isEmpty(), "id is required");
    return request;
  }

  @Produces
  static ListenableFuture<Place> fetchPlace(
      GetPlaceRequest request,
      DSLContext cafemapDb,
      @ForDatabase ListeningExecutorService dbExecutor) {
    return dbExecutor.submit(
        () ->
            cafemapDb
                .selectFrom(PLACE)
                .where(PLACE.ID.eq(ULong.valueOf(request.getId())))
                .fetchOneInto(Place.class));
  }

  @Produces
  static GetPlaceResponse response(Place place) {
    return GetPlaceResponse.newBuilder().setPlace(PlaceUtil.convertPlace(place)).build();
  }

  private GetPlaceGraph() {}
}
