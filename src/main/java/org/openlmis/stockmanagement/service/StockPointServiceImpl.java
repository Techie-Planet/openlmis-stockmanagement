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

/**
 * This class is in charge of retrieving stock points.
 * For retrieving, it retrieves all stock point records matching the search criteria.
 * Its purpose is for users to view the levels of stock per facility
 */
@Service
public class StockPointServiceImpl implements StockPointService {

  private static final Logger LOGGER = LoggerFactory.getLogger(org.openlmis.stockmanagement.service.StockPointServiceImpl.class);

  @Autowired
  StockPointRepository pointRepository;

  /**
   * Get a list of all stock points.
   * @return list of stock points.
   */
  public List<StockPoint> findStockPoints() {
    LOGGER.info("Finding the stock points");
    return pointRepository.findAll();
  }
}