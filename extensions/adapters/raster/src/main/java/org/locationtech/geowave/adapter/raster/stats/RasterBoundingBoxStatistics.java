/**
 * Copyright (c) 2013-2019 Contributors to the Eclipse Foundation
 *
 * <p> See the NOTICE file distributed with this work for additional information regarding copyright
 * ownership. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Apache License, Version 2.0 which accompanies this distribution and is
 * available at http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package org.locationtech.geowave.adapter.raster.stats;

import org.geotools.geometry.GeneralEnvelope;
import org.locationtech.geowave.adapter.raster.FitToIndexGridCoverage;
import org.locationtech.geowave.core.geotime.store.statistics.BoundingBoxDataStatistics;
import org.locationtech.geowave.core.store.adapter.statistics.BaseStatisticsQueryBuilder;
import org.locationtech.geowave.core.store.adapter.statistics.BaseStatisticsType;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridCoverage;

public class RasterBoundingBoxStatistics extends
    BoundingBoxDataStatistics<GridCoverage, BaseStatisticsQueryBuilder<Envelope>> {
  public static final BaseStatisticsType<Envelope> STATS_TYPE =
      new BaseStatisticsType<>("BOUNDING_BOX");

  public RasterBoundingBoxStatistics() {
    this(null);
  }

  public RasterBoundingBoxStatistics(final Short internalAdapterId) {
    super(internalAdapterId, STATS_TYPE);
  }

  @Override
  protected Envelope getEnvelope(final GridCoverage entry) {
    final org.opengis.geometry.Envelope indexedEnvelope = entry.getEnvelope();
    final org.opengis.geometry.Envelope originalEnvelope;
    if (entry instanceof FitToIndexGridCoverage) {
      originalEnvelope = ((FitToIndexGridCoverage) entry).getOriginalEnvelope();
    } else {
      originalEnvelope = null;
    }
    // we don't want to accumulate the envelope outside of the original if
    // it is fit to the index, so compute the intersection with the original
    // envelope
    final org.opengis.geometry.Envelope resultingEnvelope =
        getIntersection(originalEnvelope, indexedEnvelope);
    if (resultingEnvelope != null) {
      return new Envelope(
          resultingEnvelope.getMinimum(0),
          resultingEnvelope.getMaximum(0),
          resultingEnvelope.getMinimum(1),
          resultingEnvelope.getMaximum(1));
    }
    return null;
  }

  private static org.opengis.geometry.Envelope getIntersection(
      final org.opengis.geometry.Envelope originalEnvelope,
      final org.opengis.geometry.Envelope indexedEnvelope) {
    if (originalEnvelope == null) {
      return indexedEnvelope;
    }
    if (indexedEnvelope == null) {
      return originalEnvelope;
    }
    final int dimensions = originalEnvelope.getDimension();
    final double[] minDP = new double[dimensions];
    final double[] maxDP = new double[dimensions];
    for (int d = 0; d < dimensions; d++) {
      // to perform the intersection of the original envelope and the
      // indexed envelope, use the max of the mins per dimension and the
      // min of the maxes
      minDP[d] = Math.max(originalEnvelope.getMinimum(d), indexedEnvelope.getMinimum(d));
      maxDP[d] = Math.min(originalEnvelope.getMaximum(d), indexedEnvelope.getMaximum(d));
    }
    return new GeneralEnvelope(minDP, maxDP);
  }
}
