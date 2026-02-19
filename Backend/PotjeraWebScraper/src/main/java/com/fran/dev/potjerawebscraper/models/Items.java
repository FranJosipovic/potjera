package com.fran.dev.potjerawebscraper.models;

import java.util.List;

public record Items<T>(
        List<T> items,
        Integer count
) {}
