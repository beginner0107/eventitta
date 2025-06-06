package com.eventitta.event.service;

import com.eventitta.event.client.NationalFestivalClient;
import com.eventitta.event.dto.NationalFestivalResponse;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NationalFestivalImportService extends PageBasedFestivalImportService<NationalFestivalResponse.FestivalItem> {

    private final NationalFestivalClient nationalClient;

    public NationalFestivalImportService(NationalFestivalClient nationalClient,
                                         FestivalToEventMapper<NationalFestivalResponse.FestivalItem> mapper,
                                         FestivalRepository repository,
                                         FestivalUpsertWorker upsertWorker,
                                         @Value("${festival.national.import.source}") String source,
                                         @Value("${festival.national.import.page-size}") int pageSize) {
        super(mapper, repository, upsertWorker, source, pageSize);
        this.nationalClient = nationalClient;
    }

    @Override
    protected PageResult<NationalFestivalResponse.FestivalItem> fetchPageAndCount(int pageNo, String dateParam) {
        var body = nationalClient.fetchPage(pageNo, pageSize);
        if (body == null) {
            throw new RuntimeException("NationalFestivalImportService: API 응답이 null이었습니다.");
        }
        List<NationalFestivalResponse.FestivalItem> items = body.getItems();
        int totalCount = body.getTotalCount();
        return new PageResult<>(items, totalCount);
    }

    public void importAll() {
        importAll(null);
    }
}
