package com.fran.dev.potjerawebscraper.builders;

import com.fran.dev.potjerawebscraper.models.CsvRow;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvBuilder {

    public byte[] build(List<CsvRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Link,json name,count of quickfire questions,count of boardQuestions,total quickfire,total board questions\n");

        for (CsvRow row : rows) {
            sb.append(String.join(",",
                    row.link(),
                    row.jsonPath(),
                    String.valueOf(row.quickFireCount()),
                    String.valueOf(row.boardCount()),
                    String.valueOf(row.totalQuickFire()),
                    String.valueOf(row.totalBoard())
            )).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}

