/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.export;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * {@code MetricExporter} is the interface that all "push based" metric libraries should use to
 * export metrics to the OpenTelemetry exporters.
 *
 * <p>All OpenTelemetry exporters should allow access to a {@code MetricExporter} instance.
 */
public interface MetricExporter extends Closeable {

  /**
   * A common implementation of {@link #getAggregationTemporality(InstrumentType)} which returns
   * {@link AggregationTemporality#CUMULATIVE} for all instruments.
   */
  static AggregationTemporality alwaysCumulative(InstrumentType unused) {
    return AggregationTemporality.CUMULATIVE;
  }

  /**
   * A common implementation of {@link #getAggregationTemporality(InstrumentType)} which indicates
   * delta preference.
   *
   * <p>{@link AggregationTemporality#DELTA} is returned for {@link InstrumentType#COUNTER}, {@link
   * InstrumentType#OBSERVABLE_COUNTER}, and {@link InstrumentType#HISTOGRAM}. {@link
   * AggregationTemporality#CUMULATIVE} is returned for {@link InstrumentType#UP_DOWN_COUNTER} and
   * {@link InstrumentType#OBSERVABLE_UP_DOWN_COUNTER}.
   */
  static AggregationTemporality deltaPreferred(InstrumentType instrumentType) {
    switch (instrumentType) {
      case UP_DOWN_COUNTER:
      case OBSERVABLE_UP_DOWN_COUNTER:
        return AggregationTemporality.CUMULATIVE;
      case COUNTER:
      case OBSERVABLE_COUNTER:
      case HISTOGRAM:
      default:
        return AggregationTemporality.DELTA;
    }
  }

  /** Return the default aggregation temporality for the {@link InstrumentType}. */
  AggregationTemporality getAggregationTemporality(InstrumentType instrumentType);

  /**
   * Exports the collection of given {@link MetricData}. Note that export operations can be
   * performed simultaneously depending on the type of metric reader being used. However, the caller
   * MUST ensure that only one export can occur at a time.
   *
   * @param metrics the collection of {@link MetricData} to be exported.
   * @return the result of the export, which is often an asynchronous operation.
   */
  CompletableResultCode export(Collection<MetricData> metrics);

  /**
   * Exports the collection of {@link MetricData} that have not yet been exported. Note that flush
   * operations can be performed simultaneously depending on the type of metric reader being used.
   * However, the caller MUST ensure that only one export can occur at a time.
   *
   * @return the result of the flush, which is often an asynchronous operation.
   */
  CompletableResultCode flush();

  /**
   * Called when the associated IntervalMetricReader is shutdown.
   *
   * @return a {@link CompletableResultCode} which is completed when shutdown completes.
   */
  CompletableResultCode shutdown();

  /** Closes this {@link MetricExporter}, releasing any resources. */
  @Override
  default void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }
}
