package com.example.payroll.payroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class MainVerticle extends AbstractVerticle {

  /**
     * The logger instance that is used to log.
     */
  private Logger logger = LoggerFactory.getLogger(
    MainVerticle.class.getName());

  /**
   * The service wait time.
   */
  private static final int WAIT_TIME = 2000;


  @Override
  public void start() {

    this.logger.info("Starting MotorService ->");
        try {
            this.startHttpServer(0, event -> {
                if (event.failed()) {
                    logger.error("Server start failed!", event.cause());
                }
            });
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }
        this.logger.info("Starting MotorService <-");
  }

  /**
     * Start http server.
     * @param customport A custom server port.
     * @param handler The result handler.
     */
    protected void startHttpServer(final int customport,
        final Handler<AsyncResult<HttpServer>> handler) {

        this.vertx = Vertx.vertx(new VertxOptions()
            .setMetricsOptions(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions()
                    .setEnabled(true))
                .setEnabled(true)));
  
        Router router = Router.router(this.vertx);
        this.setRoutes(router);

        // Health check
        this.setHealthCheck(router);

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(customport, handler);
    }

    /**
     * Closes all the resources that were opened.
     * @param completionHandler The complention handler.
     */
    public void close(final Handler<AsyncResult<Void>> completionHandler) {
      this.vertx.close(completionHandler);
    }

    /**
     * Sets routes for the http server.
     * @param router The router used to set paths.
     */
    private void setRoutes(final Router router) {
      router.route().handler(CorsHandler.create()
          .allowedMethod(HttpMethod.POST)
          .allowedMethod(HttpMethod.GET)
          .allowedMethod(HttpMethod.OPTIONS)
          .allowedMethod(HttpMethod.PUT)
          .allowedMethod(HttpMethod.DELETE)
          .allowedHeader("Authorization")
          .allowedHeader("Access-Control-Allow-Method")
          .allowedHeader("Access-Control-Allow-Origin")
          .allowedHeader("Access-Control-Allow-Credentials")
          .allowedHeader("Content-Type"));

      // Enable multipart form data parsing for all POST API requests.
      router.route().handler(BodyHandler.create());
      router.post("/saveuser").handler(this::saveUser);
  }

  /**
     * Sets the system health check.
     * @param router The router used to set paths.
     */
    private void setHealthCheck(final Router router) {
        HealthCheckHandler health = HealthCheckHandler.create(this.vertx);
        health.register("ws", WAIT_TIME, f -> f.complete(Status.OK()));
        /*
        health.register("db", WAIT_TIME, f -> {
            if (this.getDbUtils().getDBClient() == null) {
                f.fail("MongoClient (mongoClient) is null!");
            } else {
                this.getDbUtils().getDBClient().find(DB_USERS,
                        new JsonObject().put("_id",
                            UUID.randomUUID().toString()), res -> {
                            if (res.succeeded()) {
                                f.complete(Status.OK());
                            } else {
                                f.fail(res.cause().getMessage());
                            }
                        });
            }
        });
        */

        router.get("/health").handler(health);

        HealthCheckHandler healthz = HealthCheckHandler.create(this.vertx);
        healthz.register("ws", WAIT_TIME,
            f -> f.complete(Status.OK()));

        router.get("/healthz").handler(healthz);
    }

    /**
     * Saves the user.
     * @param rc The routing context.
     */
    private void saveUser(final RoutingContext rc) {
      this.logger.info("saveUser() ->");
      try {
          JsonObject body = rc.body().asJsonObject();
          JsonObject resp = new JsonObject()
            .put("Status", 200)
            .put("Message", "Success")
            .put("Payload", body);

          rc.response().end(resp.encode());
      } catch (final Exception e) {
          logger.error(e.getMessage(), e);
          JsonObject resp = new JsonObject()
            .put("Status", 500)
            .put("Message", e.getMessage());
          rc.response().end(resp.encode());
      }
  }
}
