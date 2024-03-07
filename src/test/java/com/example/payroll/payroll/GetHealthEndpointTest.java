package com.example.payroll.payroll;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class GetHealthEndpointTest {

    private MainVerticle service;

    private int port;

    @Before
    public void before(final TestContext tc) {
        this.service = new MainVerticle();
        this.port = 8081;
        Async async = tc.async();
        this.service.startHttpServer(this.port, event -> {
            if (event.succeeded()) {
                async.complete();
            } else {
                async.complete();
                tc.assertTrue(false, "Error -> "
                    + event.cause().getMessage());
            }
        });
    }

    @After
    public void close(final TestContext tc) {
        Async async = tc.async();
        if (this.service != null) {
            this.service.close(cls -> {
                async.complete();
            });
        }
    }

    @Test
    public void getHealth(final TestContext tc) {
        Async async = tc.async();
        WebClient.create(Vertx.vertx())
            .get(this.port, "localhost", "/health")
            .putHeader("Authorization", UUID.randomUUID().toString())
            .send(h -> {
                async.complete();
                tc.assertTrue(h.succeeded(), "The call failed");
                JsonObject res = h.result().bodyAsJsonObject();
                tc.assertNotNull(res, "The result is null");
                System.out.println(res.encodePrettily());
            });
    }
}
