package io.aiven.klaw.controller;

import io.aiven.klaw.model.RequestsCountOverview;
import io.aiven.klaw.model.enums.RequestMode;
import io.aiven.klaw.service.RequestStatisticsService;
import io.aiven.klaw.service.UtilControllerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UtilController {

  @Autowired private UtilControllerService utilControllerService;

  @Autowired private RequestStatisticsService requestStatisticsService;

  @RequestMapping(
      value = "/getDashboardStats",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, String>> getDashboardStats() {
    return new ResponseEntity<>(utilControllerService.getDashboardStats(), HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getAuth",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, String>> getAuth() {
    return new ResponseEntity<>(utilControllerService.getAuth(), HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getBasicInfo",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, Object>> getBasicInfo() {
    return new ResponseEntity<>(utilControllerService.getBasicInfo(), HttpStatus.OK);
  }

  @PostMapping(value = "/logout")
  public ResponseEntity<Map<String, String>> logout(
      HttpServletRequest request, HttpServletResponse response) {
    utilControllerService.getLogoutPage(request, response);
    return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
  }

  @RequestMapping(
      value = "/resetMemoryCache/{tenantName}/{entityType}/{operationType}",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, String>> resetMemoryCache(
      @PathVariable String tenantName,
      @PathVariable String entityType,
      @PathVariable String operationType) {
    utilControllerService.resetCache(tenantName, entityType, operationType);
    return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
  }

  @GetMapping("/shutdownContext")
  public void shutdownApp() {
    utilControllerService.shutdownContext();
  }

  /**
   * @param requestMode TO_APPROVE / MY_REQUESTS
   * @return RequestsCountOverview A count of each request entity type, and request status, and
   *     overall count
   */
  /*
     Get counts of all request entity types for different status,operation types
  */
  @Operation(
      summary = "Get counts of all request entity types for different status,operation types",
      responses = {
        @ApiResponse(
            content = @Content(schema = @Schema(implementation = RequestsCountOverview.class)))
      })
  @RequestMapping(
      value = "/requests/statistics",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<RequestsCountOverview> getRequestStatistics(
      @RequestParam("requestMode") RequestMode requestMode) {
    return new ResponseEntity<>(
        requestStatisticsService.getRequestsCountOverview(requestMode), HttpStatus.OK);
  }
}
