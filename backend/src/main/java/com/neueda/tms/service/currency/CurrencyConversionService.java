package com.neueda.tms.service.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes transaction amounts to USD for currency-agnostic rule evaluation.
 *
 * Transactions can arrive in many different currencies, but monitoring rule
 * thresholds (e.g. HIGH_AMOUNT, NEW_CUSTOMER_HIGH_AMOUNT) are configured in a
 * single reference currency (USD). This service loads a static, offline
 * currency -> USD rate table (classpath resource {@code exchange-rates.json})
 * once at startup and exposes {@link #toUsd(BigDecimal, String)} so rule
 * evaluators can convert an amount purely in-memory before comparing it
 * against a threshold.
 *
 * IMPORTANT: conversion results are used ONLY for rule evaluation. Nothing in
 * this service ever mutates a {@code Transaction} entity or writes back to
 * the database — transactions are always persisted/displayed in their
 * original currency and amount.
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final String RATES_FILE = "exchange-rates.json";

    private final Map<String, BigDecimal> ratesToUsd = new HashMap<>();

    @PostConstruct
    public void loadRates() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(RATES_FILE)) {
            if (is == null) {
                log.error("Currency exchange rate file '{}' not found on classpath; " +
                        "all amounts will be treated as 1:1 with USD.", RATES_FILE);
                return;
            }

            Map<String, Object> raw = mapper.readValue(is, Map.class);
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String currencyCode = entry.getKey();
                // Skip metadata keys (e.g. "_comment") that aren't real currency rates/
                if (currencyCode.startsWith("_")) {
                    continue;
                }
                try {
                    ratesToUsd.put(currencyCode.toUpperCase(), new BigDecimal(entry.getValue().toString()));
                } catch (NumberFormatException e) {
                    log.warn("Skipping invalid exchange rate entry '{}': {}", currencyCode, entry.getValue());
                }
            }
            log.info("Loaded {} currency exchange rates from {}", ratesToUsd.size(), RATES_FILE);
        } catch (IOException e) {
            log.error("Failed to load currency exchange rates from {}: {}", RATES_FILE, e.getMessage());
        }
    }

    /**
     * Converts {@code amount} from {@code currencyCode} into its USD equivalent
     * using the static rate table. Read-only calculation — never persisted.
     *
     * @param amount       the amount in its original currency
     * @param currencyCode the ISO currency code the amount is denominated in
     * @return the USD-equivalent amount, rounded to 2 decimal places. Falls
     *         back to a 1:1 rate (with a warning logged) if the currency code
     *         is unknown, so rule evaluation never fails due to missing rates.
     */
    public BigDecimal toUsd(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currencyCode == null || currencyCode.isBlank() || "USD".equalsIgnoreCase(currencyCode)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rate = ratesToUsd.get(currencyCode.toUpperCase());
        if (rate == null) {
            log.warn("No exchange rate configured for currency '{}'; treating as 1:1 with USD for rule evaluation.",
                    currencyCode);
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
