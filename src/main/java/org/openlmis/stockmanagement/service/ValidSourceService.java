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

package org.openlmis.stockmanagement.service;

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_SOURCE_ASSIGNMENT_NOT_FOUND;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_SOURCE_NOT_FOUND;
import static org.slf4j.ext.XLoggerFactory.getXLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openlmis.stockmanagement.domain.sourcedestination.ValidSourceAssignment;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidSourcesCache;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.ValidSourceAssignmentRepository;
import org.openlmis.stockmanagement.repository.ValidSourcesCacheRepository;
import org.slf4j.ext.XLogger;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ValidSourceService extends SourceDestinationBaseService {

  private static final XLogger XLOGGER = getXLogger(ValidSourceService.class);

  @Autowired
  private ValidSourceAssignmentRepository validSourceRepository;
  @Autowired
  private ValidSourcesCacheRepository validSourcesCacheRepository;

  //  /**
  //   * Find valid destinations page by program ID and facility ID.
  //   *
  //   * @param programId      program ID
  //   * @param facilityId facility ID
  //   * @param pageable pagination and sorting parameters
  //   * @return page of valid destination assignment DTOs
  //   */
  //  public Page<ValidSourceDestinationDto> findSources(UUID programId,
  //                                                     UUID facilityId, Pageable pageable) {
  //    XLOGGER.entry();
  //    Profiler profiler = new Profiler("FIND_SOURCE_ASSIGNMENTS");
  //    profiler.setLogger(XLOGGER);
  //
  //    Page<ValidSourceDestinationDto> sourceAssignments =
  //            findAssignments(programId, facilityId, validSourceRepository, profiler, pageable);
  //    profiler.stop().log();
  //    XLOGGER.exit();
  //    return sourceAssignments;
  //  }

  /**
   * Find valid destinations page by program ID and facility ID.
   *
   * @param programId      program ID
   * @param facilityId facility ID
   * @param pageable pagination and sorting parameters
   * @return page of valid destination assignment DTOs
   */
  public Page<ValidSourceDestinationDto> findSources(
          UUID programId, UUID facilityId, Pageable pageable) {
    XLOGGER.entry();
    Profiler profiler = new Profiler("FIND_SOURCE_ASSIGNMENTS");
    profiler.setLogger(XLOGGER);

    Page<ValidSourceDestinationDto> sourceAssignments =
            getValidSourceDestinationDtoPage(programId, facilityId, pageable, profiler);
    profiler.stop().log();
    XLOGGER.exit();
    return sourceAssignments;
  }

  private Page<ValidSourceDestinationDto> getValidSourceDestinationDtoPage(
          UUID programId, UUID facilityId, Pageable pageable, Profiler profiler) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      Optional<ValidSourcesCache> sourcesCache = validSourcesCacheRepository
              .findByProgramIdAndFacilityId(programId, facilityId);
      if (sourcesCache.isPresent()) {
        List<ValidSourceDestinationDto> listOfValidSourceDestination =
                objectMapper.readValue(sourcesCache.get().getValidSources(),
                        new TypeReference<List<ValidSourceDestinationDto>>() {
                        });
        return new PageImpl<>(listOfValidSourceDestination,
                pageable, listOfValidSourceDestination.size());
      }
      Page<ValidSourceDestinationDto> resultPage = findAssignments(
              programId, facilityId, validSourceRepository, profiler,
              PageRequest.of(0, Integer.MAX_VALUE));

      if (!validSourcesCacheRepository
              .existsByProgramIdAndFacilityId(programId, facilityId)) {
        String jsonString = objectMapper.writeValueAsString(resultPage.getContent());
        ValidSourcesCache newValidSource = new ValidSourcesCache();
        newValidSource.setFacilityId(facilityId);
        newValidSource.setProgramId(programId);
        newValidSource.setValidSources(jsonString);
        validSourcesCacheRepository.save(newValidSource);
      }
      return new PageImpl<>(resultPage.getContent(), pageable, resultPage.getTotalElements());
    } catch (JsonProcessingException jsonProcessingException) {
      throw new ResourceNotFoundException("JSON processing exception");
    }
  }

  /**
   * Find valid sources List by program ID and facility ID.
   *
   * @return List valid source destination assignments
   */
  public List<ValidSourceAssignment> getValidSourceAssignmentsList(
          UUID programId, UUID facilityId, Profiler profiler) {
    Page<ValidSourceDestinationDto> pageOfValidSourceDtos = getValidSourceDestinationDtoPage(
            programId, facilityId, PageRequest.of(0, Integer.MAX_VALUE), profiler);
    return pageOfValidSourceDtos.stream()
                    .forEach(dto -> {
                      ValidSourceAssignment source = new ValidSourceAssignment();
                      source.setId(dto.getId());
                      source.setNode(dto.getNode());
                      source.setProgramId(dto.getProgramId());
                      source.setFacilityTypeId(dto.getFacilityTypeId());
                    }).collect(Collectors.toList());
  }

  /**
   * Assign a source to a program and facility type.
   *
   * @param assignment assignment JPA model
   * @return a valid source destination dto
   */
  public ValidSourceDestinationDto assignSource(ValidSourceAssignment assignment) {
    assignment.setId(null);
    return doAssign(assignment, ERROR_SOURCE_NOT_FOUND, validSourceRepository);
  }
  
  /**
   * Find existing source.
   * @param assignmentId source assignment Id
   * @return assigmnet dto 
   * @throws ValidationMessageException when assignment was not found
   */
  public ValidSourceDestinationDto findById(UUID assignmentId) {
    return findById(assignmentId, validSourceRepository,
        ERROR_SOURCE_ASSIGNMENT_NOT_FOUND);
  }

  /**
   * Find existing source assignment.
   *
   * @param assignment assignment JPA model
   * @return a valid source destination dto
   */
  public ValidSourceDestinationDto findByProgramFacilitySource(
      ValidSourceAssignment assignment) {
    return findAssignment(assignment, validSourceRepository);
  }

  /**
   * Delete a source assignment by Id.
   *
   * @param assignmentId source assignment Id
   */
  public void deleteSourceAssignmentById(UUID assignmentId) {
    doDelete(assignmentId, validSourceRepository, ERROR_SOURCE_ASSIGNMENT_NOT_FOUND);
  }

}
