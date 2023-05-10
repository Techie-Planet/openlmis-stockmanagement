/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.stockmanagement.web;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidDestinationAssignment;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidDestinationsCache;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidSourceAssignment;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidSourcesCache;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.repository.ValidDestinationsCacheRepository;
import org.openlmis.stockmanagement.repository.ValidSourcesCacheRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.service.ValidDestinationService;
import org.openlmis.stockmanagement.service.ValidSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ValidSourceDestinationController {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ValidSourceDestinationController.class);

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private ValidSourceService validSourceService;

  @Autowired
  private ValidDestinationService validDestinationService;
  @Autowired
  private ValidDestinationsCacheRepository validDestinationsCacheRepository;
  @Autowired
  private ValidSourcesCacheRepository validSourcesCacheRepository;

  // /**
  //  * Get a page with list of valid destinations.
  //  *
  //  * @param parameters filtering parameters.
  //  * @param pageable valid sources pagination parameters
  //  * @return found valid destinations
  //  */
  // @GetMapping(value = "/validDestinations")
  // public Page<ValidSourceDestinationDto> getValidDestinations(
  //         @RequestParam MultiValueMap<String, String> parameters, Pageable pageable) {
  //     ValidSourceDestinationSearchParams params = new ValidSourceDestinationSearchParams(parameters);

  //     LOGGER.info(format("Try to find valid destinations with program %s and facility %s",
  //             params.getProgramId(), params.getFacilityId()));
  //     return validDestinationService.findDestinations(
  //             params.getProgramId(), params.getFacilityId(), pageable);
  // }

  /**
   * Get a page with list of valid destinations.
   *
   * @param parameters filtering parameters.
   * @param pageable valid sources pagination parameters
   * @return found valid destinations
   */
  @GetMapping(value = "/validDestinations")
  public Page<ValidSourceDestinationDto> getValidDestinations(
      @RequestParam MultiValueMap<String, String> parameters,
      Pageable pageable) throws IOException {
    ValidSourceDestinationSearchParams params = new ValidSourceDestinationSearchParams(parameters);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ObjectMapper objectMapper = new ObjectMapper();

    Optional<ValidDestinationsCache> destinationsCache = validDestinationsCacheRepository
            .findFirst1ByProgramIdAndFacilityId(params.getProgramId(), params.getFacilityId());
    if (destinationsCache.isPresent()) {
      List<ValidSourceDestinationDto> listOfValidSourceDestination =
              objectMapper.readValue(destinationsCache.get().getValidDestinations(),
                      new TypeReference<List<ValidSourceDestinationDto>>() {});
      return new PageImpl<>(listOfValidSourceDestination,
              pageable, listOfValidSourceDestination.size());
    //      return ResponseEntity.ok().headers(headers).body(destinationsCache.get()
    //              .getValidDestinations().toString());
    }
    Page<ValidSourceDestinationDto> resultPage = validDestinationService
            .findDestinations(params.getProgramId(), params.getFacilityId(),
                    PageRequest.of(0, Integer.MAX_VALUE));


    String jsonString = objectMapper.writeValueAsString(resultPage.getContent());

    if (!validDestinationsCacheRepository
            .existsByProgramIdAndFacilityId(params.getProgramId(), params.getFacilityId())) {
        ValidDestinationsCache newValidDestination = new ValidDestinationsCache();
        newValidDestination.setFacilityId(params.getFacilityId());
        newValidDestination.setProgramId(params.getProgramId());
        newValidDestination.setValidDestinations(jsonString);
        validDestinationsCacheRepository.save(newValidDestination);
    }

    return new PageImpl<>(resultPage.getContent(), pageable, resultPage.getTotalElements());
  }

  /**
   * Assign a destination to a program and facility type.
   * If valid destination assignment ID is specified, ID will be ignored.
   *
   * @return the assigned destination and program and facility type.
   */
  @PostMapping(value = "/validDestinations")
  public ResponseEntity<ValidSourceDestinationDto> assignDestination(
      @RequestBody ValidDestinationAssignment assignment) {
    LOGGER.debug("Try to assign destinations");
    permissionService.canManageStockDestinations();

    ValidSourceDestinationDto destinationDto = validDestinationService
        .findByProgramFacilityDestination(assignment);
    if (destinationDto != null) {
      return new ResponseEntity<>(destinationDto, OK);
    }

    return new ResponseEntity<>(
        validDestinationService.assignDestination(assignment), CREATED);
  }

  //  /**
  //   * Get a page with list of valid sources.
  //   *
  //   * @param parameters filtering parameters.
  //   * @param pageable valid sources pagination parameters
  //   * @return found valid sources
  //   */
  //  @GetMapping(value = "/validSources")
  //  public Page<ValidSourceDestinationDto> getValidSources(
  //      @RequestParam MultiValueMap<String, String> parameters, Pageable pageable) {
  //    ValidSourceDestinationSearchParams params = new ValidSourceDestinationSearchParams(parameters);
  //
  //    LOGGER.debug(format("Try to find valid sources with program %s and facility %s",
  //        params.getProgramId(), params.getFacilityId()));
  //    return validSourceService.findSources(
  //        params.getProgramId(), params.getFacilityId(), pageable);
  //    // return new PageImpl<>(Collections.emptyList(), pageable, 0);
  //  }

  /**
   * Get a page with list of valid sources.
   *
   * @param parameters filtering parameters.
   * @param pageable valid sources pagination parameters
   * @return found valid sources
   */
  @GetMapping(value = "/validSources")
  public ResponseEntity<?> getValidSources(
      @RequestParam MultiValueMap<String, String> parameters,
      Pageable pageable) throws IOException {
    ValidSourceDestinationSearchParams params = new ValidSourceDestinationSearchParams(parameters);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Optional<ValidSourcesCache> sourcesCache = validSourcesCacheRepository
            .findFirst1ByProgramIdAndFacilityId(params.getProgramId(), params.getFacilityId());
    if (sourcesCache.isPresent()) {
      return ResponseEntity.ok().headers(headers).body(sourcesCache.get()
              .getValidSources().toString());
    }
    Page<ValidSourceDestinationDto> resultPage = validSourceService
            .findSources(params.getProgramId(), params.getFacilityId(), pageable);

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString = objectMapper.writeValueAsString(resultPage);

      if (!validSourcesCacheRepository
              .existsByProgramIdAndFacilityId(params.getProgramId(), params.getFacilityId())) {
          ValidSourcesCache newValidSource = new ValidSourcesCache();
          newValidSource.setFacilityId(params.getFacilityId());
          newValidSource.setProgramId(params.getProgramId());
          newValidSource.setValidSources(jsonString);
          validSourcesCacheRepository.save(newValidSource);
      }

    return ResponseEntity.ok().body(resultPage);
    // return new PageImpl<>(Collections.emptyList(), pageable, 0);
  }

  /**
   * Assign a source to program and facility type.
   * If valid source assignment ID is specified, ID will be ignored.
   *
   * @return the assigned source and program and facility type.
   */
  @PostMapping(value = "/validSources")
  public ResponseEntity<ValidSourceDestinationDto> assignSource(
      @RequestBody ValidSourceAssignment assignment) {
    LOGGER.debug("Try to assign source");
    permissionService.canManageStockSources();

    ValidSourceDestinationDto foundAssignmentDto = validSourceService
        .findByProgramFacilitySource(assignment);
    if (foundAssignmentDto != null) {
      return new ResponseEntity<>(foundAssignmentDto, OK);
    }
    return new ResponseEntity<>(
        validSourceService.assignSource(assignment), CREATED);
  }

  /**
   * Remove a valid source assignment of a program and facility type combination.
   *
   * @param assignmentId source assignment ID
   */
  @DeleteMapping(value = "/validSources/{id}")
  @ResponseStatus(NO_CONTENT)
  public void removeValidSourceAssignment(@PathVariable("id") UUID assignmentId) {
    LOGGER.debug(format("Try to remove source assignment %s.", assignmentId));
    permissionService.canManageStockSources();
    validSourceService.deleteSourceAssignmentById(assignmentId);
  }

  /**
   * Remove a valid destination assignment of a program and facility type combination.
   *
   * @param assignmentId destination assignment ID
   */
  @DeleteMapping(value = "/validDestinations/{id}")
  @ResponseStatus(NO_CONTENT)
  public void removeValidDestinationAssignment(@PathVariable("id") UUID assignmentId) {
    LOGGER.debug(format("Try to remove destination assignment %s.", assignmentId));
    permissionService.canManageStockDestinations();
    validDestinationService.deleteDestinationAssignmentById(assignmentId);
  }


  /**
   * Fetch a source or destination facility from a node.
   *
   * @param nodeId UUID of Program.
   * @return Node.
   */
  @RequestMapping(value = "/validDestinations/getSourceDestinationFacilityFromNode", method = GET)
  public ResponseEntity<Node> getSourceDestinationFacilityFromNode(
          @RequestParam(value = "nodeId") UUID nodeId) {
    return new ResponseEntity<>(
            validDestinationService
            .getReferenceIdFromNodeId(nodeId), OK);
  }
}
