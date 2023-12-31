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

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.domain.event.StockEventLineItem;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockEventLineItemRepository;
import org.openlmis.stockmanagement.service.HomeFacilityPermissionService;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.service.StockEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Controller used to create stock event.
 */
@Controller
@Transactional
@RequestMapping("/api")
public class StockEventsController extends BaseController {
  private static final Logger LOGGER = LoggerFactory.getLogger(StockEventsController.class);

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private HomeFacilityPermissionService homeFacilityPermissionService;

  @Autowired
  private StockEventProcessor stockEventProcessor;

  @Autowired
  private StockEventLineItemRepository stockEventLineItemRepository;

  /**
   * Create stock event.
   *
   * @param eventDto a stock event bound to request body.
   * @return created stock event's ID.
   */
  @RequestMapping(value = "stockEvents", method = POST)
  public ResponseEntity<UUID> createStockEvent(@RequestBody StockEventDto eventDto) {
    LOGGER.debug("Try to create a stock event");

    Profiler profiler = getProfiler("CREATE_STOCK_EVENT", eventDto);

    checkPermission(eventDto, profiler.startNested("CHECK_PERMISSION"));

    profiler.start("PROCESS");
    UUID createdEventId = stockEventProcessor.process(eventDto);

    profiler.start("CREATE_RESPONSE");
    ResponseEntity<UUID> response = new ResponseEntity<>(createdEventId, CREATED);

    return stopProfiler(profiler, response);
  }


  /**
   * Initialize stock event.
   * THIS METHOD IS USED TO INITIALIZE STOCK FOR ALL FACILITIES AND PRODUCTS
   * DURING UAT.
   * IT SHOULD NOT BE USED IN PRODUCTION AND SHOULD BE MADE UNAVAILABLE AFTER USE.
   * THE NAIN DIFFERENCE FROM THE ACTIVE POST ENDPOINT IS THAT THIS METHOD DOES NOT
   * CHECK FOR PERMISSIONS WHILE THE ACTIVE ONE DOES
   *
   * @param eventDto a stock event bound to request body.
   * @return created stock event's ID.
   */
  @RequestMapping(value = "stockEvents/initialize", method = POST)
  public ResponseEntity<UUID> initializeStocks(@RequestBody StockEventDto eventDto) {
    LOGGER.debug("Try to create a stock event");

    Profiler profiler = getProfiler("CREATE_STOCK_EVENT", eventDto);

    profiler.start("PROCESS");
    UUID createdEventId = stockEventProcessor.process(eventDto);

    profiler.start("CREATE_RESPONSE");
    ResponseEntity<UUID> response = new ResponseEntity<>(createdEventId, CREATED);

    return stopProfiler(profiler, response);
  }

  private void checkPermission(StockEventDto eventDto, Profiler profiler) {
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext().getAuthentication();

    if (!authentication.isClientOnly()) {
      UUID programId = eventDto.getProgramId();
      UUID facilityId = eventDto.getFacilityId();

      profiler.start("CHECK_PROGRAM_SUPPORTED_BY_HOME_FACILITY");
      homeFacilityPermissionService.checkProgramSupported(programId);

      if (eventDto.isPhysicalInventory()) {
        profiler.start("CAN_EDIT_PHYSICAL_INVENTORY");
        permissionService.canEditPhysicalInventory(programId, facilityId);
      } else {
        //we check STOCK_ADJUST permission for both adjustment and issue/receive
        //this may change in the future
        profiler.start("CAN_ADJUST_STOCK");
        permissionService.canAdjustStock(programId, facilityId);
      }
    }
  }

  /**
   * Fetch Stock Line Item Issued to Facility.
   *
   * @param programId UUID of Program.
   * @param facilityId UUID of Facility.
   * @param documentNumber String of documentNumber.
   * @return stockEventLineItem.
   */
  @RequestMapping(value = "issuedStockItems", method = GET)
  public ResponseEntity<List<StockEventLineItemDto>> getStockLineItemIssuedToFacility(
      @RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "documentNumber") String documentNumber) {
    List<StockEventLineItem> result = stockEventLineItemRepository
                                                .getAllLineItemIssuedToFacility(
                                                    programId,
                                                    facilityId,
                                                    documentNumber
      );
    List<StockEventLineItemDto> stockEventLineItemDtos = result
        .stream()
        .map(StockEventLineItem::toEventLineItemDto)
        .collect(Collectors.toList());
    return new ResponseEntity<>(stockEventLineItemDtos, OK);
  }

  /**
   * Fetch Stock Line Item ISSUE ID Issued to Facility.
   *
   * @param programId UUID of Program.
   * @param facilityId UUID of Facility.
   * @return string.
   */
  @RequestMapping(value = "issuedStockItemsNumber", method = GET)
  public ResponseEntity<List<String>> getStockLineItemIssuedToFacilityNumber(
      @RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId) {
    List<String> result = stockEventLineItemRepository
                                                .getAllLineItemIssuedToFacilityNumber(
                                                    programId,
                                                    facilityId
      );
    // List<String> result = new ArrayList<>();
    return new ResponseEntity<>(result, OK);
  }

}
