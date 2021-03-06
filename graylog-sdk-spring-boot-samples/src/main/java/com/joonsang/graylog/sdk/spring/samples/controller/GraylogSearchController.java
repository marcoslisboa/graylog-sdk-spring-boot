package com.joonsang.graylog.sdk.spring.samples.controller;

import com.joonsang.graylog.GraylogQuery;
import com.joonsang.graylog.sdk.spring.samples.domain.FieldHistograms;
import com.joonsang.graylog.sdk.spring.samples.domain.GraylogMessage;
import com.joonsang.graylog.sdk.spring.samples.domain.Histograms;
import com.joonsang.graylog.sdk.spring.samples.domain.TwoStatistics;
import com.joonsang.graylog.sdk.spring.samples.service.GraylogSearchService;
import com.joonsang.graylog.sdk.spring.starter.domain.Terms;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping({"/v1/search"})
public class GraylogSearchController {

    private final GraylogSearchService graylogSearchService;

    public GraylogSearchController(GraylogSearchService graylogSearchService) {
        this.graylogSearchService = graylogSearchService;
    }

    /**
     * Message.
     * Get message by API request ID.
     */
    @GetMapping({"/messages/{requestId}"})
    public ResponseEntity<?> getMessageByRequestId(
        @PathVariable("requestId") String requestId
    ) throws IOException, ReflectiveOperationException {

        LocalDateTime fromDateTime = LocalDateTime.now().minusMonths(3);
        LocalDateTime toDateTime = LocalDateTime.now();

        GraylogMessage message = graylogSearchService.getMessage(
            fromDateTime,
            toDateTime,
            GraylogQuery.builder()
                .field("request_id", requestId)
        );

        if (message == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * Statistics.
     * Generates two statistics by sources' process times are between 0ms and 500ms.
     * One has the stats from today to yesterday, the another has last 7 days stats.
     */
    @GetMapping({"/statistics/comparisons/period/sources"})
    public ResponseEntity<?> getCompareSourceProcessTimeStats() throws IOException {
        LocalDateTime firstDateTime = LocalDateTime.now().minusDays(1);
        LocalDateTime secondDateTime = LocalDateTime.now().minusDays(7);

        TwoStatistics twoStats = graylogSearchService.getTwoStats(
            "source",
            firstDateTime,
            secondDateTime,
            GraylogQuery.builder()
                .field("message", "API_REQUEST_FINISHED")
                .and().range("process_time", "[", 0, 500, "]")
        );

        return new ResponseEntity<>(twoStats, HttpStatus.OK);
    }

    /**
     * Histogram.
     * Generates multiple datetime histograms with labels by process time
     */
    @GetMapping({"/histograms/process-times"})
    public ResponseEntity<?> getProcessTimeHistograms(
        @RequestParam(value = "interval") String interval,
        @RequestParam(value = "from") String from,
        @RequestParam(value = "to") String to
    ) throws IOException {

        LocalDateTime fromDateTime = LocalDateTime.parse(from, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime toDateTime = LocalDateTime.parse(to, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Histograms histograms = graylogSearchService.getProcessTimeHistograms(
            interval,
            fromDateTime,
            toDateTime,
            GraylogQuery.builder()
                .field("message", "API_REQUEST_FINISHED")
        );

        return new ResponseEntity<>(histograms, HttpStatus.OK);
    }

    /**
     * Field Histogram.
     * Generates multiple field value histograms with labels by top requested sources' process times
     */
    @GetMapping({"/field-histograms/process-times/sources"})
    public ResponseEntity<?> getSourceProcessTimeFieldHistograms(
        @RequestParam(value = "size") int size,
        @RequestParam(value = "interval") String interval,
        @RequestParam(value = "from") String from,
        @RequestParam(value = "to") String to
    ) throws IOException {

        LocalDateTime fromDateTime = LocalDateTime.parse(from, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime toDateTime = LocalDateTime.parse(to, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        FieldHistograms fieldHistograms = graylogSearchService.getProcessTimeFieldHistogramsByTopSources(
            size,
            interval,
            fromDateTime,
            toDateTime,
            GraylogQuery.builder()
                .field("message", "API_REQUEST_FINISHED")
        );

        return new ResponseEntity<>(fieldHistograms, HttpStatus.OK);
    }

    /**
     * Terms.
     * Get request path usage ranking
     */
    @GetMapping({"/terms/request-paths"})
    public ResponseEntity<?> getRequestPathRanking(
        @RequestParam(value = "size") int size,
        @RequestParam(value = "from") String from,
        @RequestParam(value = "to") String to,
        @RequestParam(value = "top_values_only", defaultValue = "false") boolean topValuesOnly,
        @RequestParam(value = "order", defaultValue = "desc") String order
    ) throws IOException {

        LocalDateTime fromDateTime = LocalDateTime.parse(from, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime toDateTime = LocalDateTime.parse(to, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Terms terms = graylogSearchService.getUsageRanking(
            "request_path",
            "",
            size,
            fromDateTime,
            toDateTime,
            order.equals("asc"),
            topValuesOnly,
            GraylogQuery.builder()
                .field("message", "API_REQUEST_FINISHED")
        );

        return new ResponseEntity<>(terms, HttpStatus.OK);
    }
}
