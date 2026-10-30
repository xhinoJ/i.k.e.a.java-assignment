package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    // Emulates sending the store to a legacy system by writing a temporary payload file.
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    // Emulates sending the store to a legacy system by writing a temporary payload file.
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      Path tempFile = Files.createTempFile(filePrefix(store), ".txt");
      LOGGER.debugf("Created temporary legacy payload file %s", tempFile);

      String content =
          "Store created. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.writeString(tempFile, content);

      Files.delete(tempFile);
      LOGGER.debugf("Propagated store %s to the legacy system", store.name);
    } catch (Exception e) {
      // The database transaction has already committed at this point; a legacy outage must not
      // turn a successful store change into an error for the caller.
      LOGGER.errorf(e, "Failed to propagate store %s to the legacy system", store.name);
    }
  }

  private String filePrefix(Store store) {
    // Files.createTempFile requires a prefix of at least three characters.
    return store.name == null || store.name.length() < 3 ? "store" : store.name;
  }
}
