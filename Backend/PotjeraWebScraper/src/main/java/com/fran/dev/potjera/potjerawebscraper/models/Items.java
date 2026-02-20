package com.fran.dev.potjera.potjerawebscraper.models;

import java.util.List;

public record Items<T>(
        List<T> items,
        Integer count
) {}
