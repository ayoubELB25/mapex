package dev.ayoubelb25.mapex.service;

public interface ModelClient {
    String generate(String model, String prompt) throws Exception;
}