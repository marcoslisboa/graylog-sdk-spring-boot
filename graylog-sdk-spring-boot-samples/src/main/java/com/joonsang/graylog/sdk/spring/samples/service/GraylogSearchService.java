package com.joonsang.graylog.sdk.spring.samples.service;

import com.joonsang.graylog.GraylogQuery;
import com.joonsang.graylog.sdk.spring.samples.domain.GraylogMessage;
import com.joonsang.graylog.sdk.spring.samples.domain.Histograms;
import com.joonsang.graylog.sdk.spring.samples.domain.TwoStatistics;
import com.joonsang.graylog.sdk.spring.starter.GraylogSearch;
import com.joonsang.graylog.sdk.spring.starter.domain.Histogram;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GraylogSearchService {

    private final String GRAYLOG_STREAM_ID;

    private final GraylogSearch graylogSearch;

    public GraylogSearchService(
        @Value("${graylog.streamId}") String graylogStreamId,
        GraylogSearch graylogSearch
    ) {

        this.GRAYLOG_STREAM_ID = graylogStreamId;
        this.graylogSearch = graylogSearch;
    }

    public GraylogMessage getMessage(
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime,
        GraylogQuery query
    ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {

        @SuppressWarnings("unchecked")
        List<GraylogMessage> messages = (List<GraylogMessage>) graylogSearch.getMessages(
            GRAYLOG_STREAM_ID,
            fromDateTime,
            toDateTime,
            query.build(),
            GraylogMessage.class
        );

        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(0);
    }

    public TwoStatistics getTwoStats(
        String field,
        LocalDateTime firstDateTime,
        LocalDateTime secondDateTime,
        GraylogQuery query
    ) throws IOException {

        LocalDateTime toDateTime = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS);

        return TwoStatistics.builder()
            .first(
                graylogSearch.getStatistics(
                    GRAYLOG_STREAM_ID,
                    field,
                    firstDateTime,
                    toDateTime,
                    query.build()
                )
            )
            .second(
                graylogSearch.getStatistics(
                    GRAYLOG_STREAM_ID,
                    field,
                    secondDateTime,
                    toDateTime,
                    query.build()
                )
            )
            .build();
    }

    public Histograms getProcessTimeHistograms(
        String interval,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime,
        GraylogQuery query
    ) throws IOException {

        Histogram all = graylogSearch.getHistogram(
            GRAYLOG_STREAM_ID,
            interval,
            fromDateTime,
            toDateTime,
            GraylogQuery.builder(query)
                .and().field("process_time", ">=", 0)
                .build()
        );

        Histogram first = graylogSearch.getHistogram(
            GRAYLOG_STREAM_ID,
            interval,
            fromDateTime,
            toDateTime,
            GraylogQuery.builder(query)
                .and().range("process_time", "[", 0, 500, "]")
                .build()
        );

        Histogram second = graylogSearch.getHistogram(
            GRAYLOG_STREAM_ID,
            interval,
            fromDateTime,
            toDateTime,
            GraylogQuery.builder(query)
                .and().field("process_time", ">", 500)
                .build()
        );

        return Histograms.builder()
            .labels(List.of("All", "0-500", "500-"))
            .histograms(List.of(all, first, second))
            .build();
    }
}
