package com.fran.dev.potjera.potjerawebscraper.builders;

import com.fran.dev.potjera.potjerawebscraper.models.CsvRow;
import com.fran.dev.potjera.potjerawebscraper.models.ParsedEpisode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class ZipBuilder {

    private final ObjectMapper objectMapper;
    private final CsvBuilder csvBuilder;

    public byte[] buildZip(List<ParsedEpisode> episodes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);

        List<CsvRow> csvRows = new ArrayList<>();

        for (ParsedEpisode ep : episodes) {
            String fileName = "ep-" + ep.episodeNumber() + ".json";

            // JSON entry
            zipOut.putNextEntry(new ZipEntry(fileName));
            zipOut.write(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(ep.data()));
            zipOut.closeEntry();

            var quick = ep.data().quickFireQuestions();
            var board = ep.data().multipleChoiceQuestions();

            csvRows.add(new CsvRow(
                    ep.link(),
                    fileName,
                    quick.count(),
                    board.count(),
                    quick.items().size(),
                    board.items().size()
            ));
        }

        // CSV entry
        zipOut.putNextEntry(new ZipEntry("summary.csv"));
        zipOut.write(csvBuilder.build(csvRows));
        zipOut.closeEntry();

        zipOut.close();
        return baos.toByteArray();
    }
}

