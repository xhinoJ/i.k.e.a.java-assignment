package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  void listsActiveWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }

  @Test
  void getsWarehouseById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body("id", is("1"))
        .body("businessUnitCode", is("MWH.001"))
        .body("location", is("ZWOLLE-001"));
  }

  @Test
  void returnsNotFoundForUnknownWarehouseId() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void returnsNotFoundForNonNumericWarehouseId() {
    given().when().get(PATH + "/not-a-number").then().statusCode(404);
  }

  @Test
  void createsWarehouse() {
    String id = createWarehouse("MWH.CREATE-1", "AMSTERDAM-002", 50, 10);

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("id", is(id))
        .body("businessUnitCode", is("MWH.CREATE-1"))
        .body("location", is("AMSTERDAM-002"))
        .body("capacity", is(50))
        .body("stock", is(10));

    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.CREATE-1"));
  }

  @Test
  void rejectsDuplicateBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.001", "AMSTERDAM-002", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsUnknownLocation() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.INVALID-LOCATION", "UNKNOWN-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsCreationWhenLocationAlreadyHasMaximumNumberOfWarehouses() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.MAX-WAREHOUSES", "TILBURG-001", 5, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsCreationWhenLocationCapacityWouldBeExceeded() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.MAX-CAPACITY", "AMSTERDAM-001", 60, 10))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsStockGreaterThanCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.BAD-STOCK", "AMSTERDAM-002", 10, 20))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsNonPositiveCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.BAD-CAPACITY", "AMSTERDAM-002", 0, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsMissingLocation() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":\"MWH.NO-LOCATION\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void archivesWarehouse() {
    String id = createWarehouse("MWH.ARCHIVE-1", "ZWOLLE-002", 20, 5);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.ARCHIVE-1")));
  }

  @Test
  void returnsNotFoundWhenArchivingUnknownWarehouse() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void replacesWarehouse() {
    String oldId = createWarehouse("MWH.REPLACE-1", "EINDHOVEN-001", 30, 10);

    String newId =
        given()
            .contentType(ContentType.JSON)
            .body(body("MWH.REPLACE-1", "EINDHOVEN-001", 40, 10))
            .when()
            .post(PATH + "/MWH.REPLACE-1/replacement")
            .then()
            .statusCode(200)
            .body("businessUnitCode", is("MWH.REPLACE-1"))
            .body("capacity", is(40))
            .body("stock", is(10))
            .extract()
            .jsonPath()
            .getString("id");

    // The previous warehouse is archived and the replacement gets a new id.
    given().when().get(PATH + "/" + oldId).then().statusCode(404);
    given()
        .when()
        .get(PATH + "/" + newId)
        .then()
        .statusCode(200)
        .body("id", is(newId))
        .body("businessUnitCode", is("MWH.REPLACE-1"));
  }

  @Test
  void returnsNotFoundWhenReplacingUnknownBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.UNKNOWN", "AMSTERDAM-002", 20, 5))
        .when()
        .post(PATH + "/MWH.UNKNOWN/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsReplacementCapacityThatCannotAccommodateStock() {
    createWarehouse("MWH.REPLACE-CAPACITY", "HELMOND-001", 20, 10);

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REPLACE-CAPACITY", "HELMOND-001", 8, 8))
        .when()
        .post(PATH + "/MWH.REPLACE-CAPACITY/replacement")
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsReplacementWithMismatchedStock() {
    createWarehouse("MWH.REPLACE-STOCK", "VETSBY-001", 20, 10);

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REPLACE-STOCK", "VETSBY-001", 30, 5))
        .when()
        .post(PATH + "/MWH.REPLACE-STOCK/replacement")
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsReplacementWithUnknownLocation() {
    createWarehouse("MWH.REPLACE-LOCATION", "AMSTERDAM-001", 10, 5);

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REPLACE-LOCATION", "UNKNOWN-001", 10, 5))
        .when()
        .post(PATH + "/MWH.REPLACE-LOCATION/replacement")
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsReplacementWithMismatchedBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.OTHER", "ZWOLLE-001", 20, 10))
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(422);
  }

  private String createWarehouse(String code, String location, int capacity, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body(body(code, location, capacity, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("businessUnitCode", is(code))
        .body("id", not(nullValue()))
        .extract()
        .jsonPath()
        .getString("id");
  }

  private String body(String code, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + code
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }
}
