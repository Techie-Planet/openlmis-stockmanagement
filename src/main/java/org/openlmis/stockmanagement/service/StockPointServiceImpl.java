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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.MapUtils;
import org.openlmis.stockmanagement.domain.stockpoint.StockPoint;
import org.openlmis.stockmanagement.repository.StockPointRepository;
import org.openlmis.stockmanagement.service.abstracts.StockPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 * This class is in charge of retrieving stock points.
 * For retrieving, it retrieves all stock point records matching the search criteria.
 * Its purpose is for users to view the levels of stock per facility
 */
@Service
public class StockPointServiceImpl implements StockPointService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
          org.openlmis.stockmanagement.service.StockPointServiceImpl.class);

  static final String PROGRAM_ID = "programId";
  static final String FACILITY_TYPE_ID = "facilityTypeId";
  static final String FACILITY_ID = "facilityId";

  @Autowired
  StockPointRepository pointRepository;

  /**
   * Get a list of all stock points.
   * Search Parameters - Program Id, Facility type Id, Facility Id
   * @return list of stock points.
   */
  public List<StockPoint> findStockPoints(MultiValueMap<String, String> parameters) {
    LOGGER.info("Finding the stock points");
    System.out.println(parameters.toSingleValueMap().toString());
    Map<String, String> filtersMap = parameters.toSingleValueMap();

    if (!MapUtils.isEmpty(parameters)) {
      System.out.println("MapUtils NOT empty " + filtersMap.get(FACILITY_TYPE_ID));

      UUID programId = this.getId(PROGRAM_ID, parameters);
      UUID facilityTypeId = this.getId(FACILITY_TYPE_ID, parameters);
      UUID facilityId = this.getId(FACILITY_ID, parameters);

      System.out.printf("ProgramId: %s, facilityTypeId: %s, facilityId: %s",
              null != programId ? programId.toString() : "",
              null != facilityTypeId ? facilityTypeId.toString() : "",
              null != facilityId ? facilityId.toString() : "");

      LOGGER.info("Passed Null Checks");

      if (null != facilityId && null != facilityTypeId) {
        return pointRepository.findByFacilityTypeIdAndFacilityId(facilityTypeId, facilityId);
      } else if (null != facilityTypeId) {
        System.out.println("Inside facilityTypeId");
        return pointRepository.findByFacilityTypeId(facilityTypeId);
      } else if (null != facilityId) {
        return pointRepository.findByFacilityId(facilityId);
      }
    }

    return pointRepository.findAll();
  }

  private UUID getId(String paramName, MultiValueMap<String, String> parameters) {
    return null != parameters.getFirst(paramName)
            ? UUID.fromString(parameters.getFirst(paramName))
            : null;
  }
}