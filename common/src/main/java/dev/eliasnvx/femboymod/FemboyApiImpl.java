package dev.eliasnvx.femboymod;

import dev.eliasnvx.femboymod.api.FemboyApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

final class FemboyApiImpl implements FemboyApi {

    private static final String API_PROPERTIES = "/META-INF/femboymod-api.properties";

    private final String apiVersion = readApiVersion();

    @Override
    public String apiVersion() {
        return apiVersion;
    }

    private static String readApiVersion() {
        try (InputStream in = FemboyApi.class.getResourceAsStream(API_PROPERTIES)) {
            if (in == null) {
                throw new IllegalStateException(API_PROPERTIES + " is missing from the femboymod jar");
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("api_version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
