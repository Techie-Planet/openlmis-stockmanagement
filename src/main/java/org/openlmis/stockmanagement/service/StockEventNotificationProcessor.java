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

import static org.openlmis.stockmanagement.service.PermissionService.STOCK_ADJUST;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.service.notifier.IssueNotifier;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.service.notifier.StockoutNotifier;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service that helps the StockEventProcessor to determine notification.
 */
@Service
public class StockEventNotificationProcessor {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
      StockEventNotificationProcessor.class);

  @Autowired
  private StockoutNotifier stockoutNotifier;
  @Autowired
  private IssueNotifier issueNotifier;

  @Autowired
  private RightReferenceDataService rightReferenceDataService;

  /**
   * From the stock event, check each line item's stock card and see if stock on hand has gone to
   * zero. If so, send a notification to all of that stock card's editors.
   * 
   * @param eventDto the stock event to process
   */
  public void callAllNotifications(StockEventDto eventDto) {
    RightDto right = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
    eventDto
        .getLineItems()
        .forEach(line -> callNotifications(eventDto, line, right.getId()));
  }

  private void callNotifications(StockEventDto event, StockEventLineItemDto eventLine,
      UUID rightId) {
    XLOGGER.entry(event, eventLine);
    Profiler profiler = new Profiler("CALL_NOTIFICATION_FOR_LINE_ITEM");
    profiler.setLogger(XLOGGER);

    profiler.start("COPY_STOCK_CARD");
    OrderableLotIdentity identity = OrderableLotIdentity.identityOf(eventLine);
    StockCard stockCard = event.getContext().findCard(identity);

    //if (stockCard.getStockOnHand() == 0) {
    if (stockCard.getStockOnHand() >= 0) { //testing
      stockoutNotifier.notifyStockEditors(stockCard, rightId);
    }

    profiler.stop().log();
    XLOGGER.exit();
  }
  private void callIssueNotifications(StockEventDto event, Map.Entry<FacilityDto,
      List<StockEventLineItemDto>> eventLine, UUID rightId) {
    XLOGGER.entry(event, eventLine);
    Profiler profiler = new Profiler("CALL_ISSUE_NOTIFICATION_FOR_LINE_ITEM");
    profiler.setLogger(XLOGGER);

    profiler.start("COPY_STOCK_CARD");
    OrderableLotIdentity identity = OrderableLotIdentity.identityOf(eventLine.getValue().get(0));
    StockCard stockCard = event.getContext().findCard(identity);

    issueNotifier.notifyStockEditors(stockCard, rightId,
      eventLine.getValue().size(), event.getFacilityId());

    profiler.stop().log();
    XLOGGER.exit();
  }
  // TODO this is a notifier class... create a notification method for issue here

  public void notifyIssue(StockEventDto eventDto, Map<StockEventLineItemDto,
      FacilityDto> stockEventLineItems) {
    RightDto right = rightReferenceDataService.findRight(STOCK_ADJUST);

    Map<FacilityDto, List<StockEventLineItemDto>> numberInEachFacility = new HashMap<>();
    // this block checks if different stock event line items are going to the same facility
    // and collects them into a list
    for (Map.Entry<StockEventLineItemDto, FacilityDto> eachEntry: stockEventLineItems.entrySet()) {
      if(numberInEachFacility.get(eachEntry.getValue()) == null){
        List<StockEventLineItemDto> list = new ArrayList<>();
        list.add(eachEntry.getKey());
        numberInEachFacility.put(eachEntry.getValue(), list);
      }else{
        List<StockEventLineItemDto> list = numberInEachFacility.get(eachEntry.getValue());
        list.add(eachEntry.getKey());
        numberInEachFacility.put(eachEntry.getValue(), list);
      }
    }
    for (Map.Entry<FacilityDto, List<StockEventLineItemDto>> entry
            : numberInEachFacility.entrySet()) {
      callIssueNotifications(eventDto, entry, right.getId());
    }
  }
}
