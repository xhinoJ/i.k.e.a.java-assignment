package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FulfilmentUnitEndpointTest {

  private static final String PATH = "fulfilment-unit";

  @Test
  void createsAndRetrievesAssociation() {
    long id = createAssociation(1, 1, 1);

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("id", is((int) id))
        .body("storeId", is(1))
        .body("productId", is(1))
        .body("warehouseId", is(1));

    given().when().get(PATH).then().statusCode(200).body("id", hasItem((int) id));
  }

  @Test
  void returnsNotFoundForUnknownAssociation() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void deletesAssociation() {
    long id = createAssociation(1, 2, 2);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  void rejectsDuplicateAssociation() {
    createAssociation(3, 3, 1);

    given()
        .contentType(ContentType.JSON)
        .body(body(3, 3, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsUnknownStore() {
    given()
        .contentType(ContentType.JSON)
        .body(body(999, 1, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsUnknownProduct() {
    given()
        .contentType(ContentType.JSON)
        .body(body(1, 999, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsUnknownWarehouse() {
    given()
        .contentType(ContentType.JSON)
        .body(body(1, 1, 999))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsMissingStoreId() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"productId\":1,\"warehouseId\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsMissingProductId() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"storeId\":1,\"warehouseId\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsMissingWarehouseId() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"storeId\":1,\"productId\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsThirdWarehouseForProductInStore() {
    createAssociation(2, 2, 1);
    createAssociation(2, 2, 2);

    given()
        .contentType(ContentType.JSON)
        .body(body(2, 2, 3))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  private long createAssociation(long storeId, long productId, long warehouseId) {
    return given()
        .contentType(ContentType.JSON)
        .body(body(storeId, productId, warehouseId))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("id", not(nullValue()))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private String body(long storeId, long productId, long warehouseId) {
    return "{\"storeId\":"
        + storeId
        + ",\"productId\":"
        + productId
        + ",\"warehouseId\":"
        + warehouseId
        + "}";
  }
}
