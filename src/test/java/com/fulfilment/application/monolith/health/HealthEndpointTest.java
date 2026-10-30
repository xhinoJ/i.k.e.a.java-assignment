package com.fulfilment.application.monolith.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HealthEndpointTest {

  @Test
  void reportsOverallHealth() {
    given().when().get("/q/health").then().statusCode(200).body("status", is("UP"));
  }

  @Test
  void reportsLiveness() {
    given().when().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
  }

  @Test
  void reportsReadiness() {
    given().when().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
  }
}
