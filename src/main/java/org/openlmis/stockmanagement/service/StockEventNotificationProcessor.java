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
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity;
import org.openlmis.stockmanagement.domain.reason.ReasonCategory;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.service.notifier.IssueNotifier;
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
   * From the stock event, check each line item's stock card and see
   * if stock on hand has gone to zero.
   * If so, send a notification to all of that stock card's editors.
   * 
   * @param eventDto the stock event to process
   */
  public void callAllNotifications(StockEventDto eventDto) {
    RightDto right = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
    eventDto
        .getLineItems()
        .forEach(line -> callNotifications(eventDto, line, right.getId()));
    // here, instead of calling issue notifications one after another,
    // send the whole event to issue notifiier, new method to be created in
    // issueNotifier to check, sort and send the emails. Time complexity is O(n2)
  }

  private void callNotifications(StockEventDto event, StockEventLineItemDto eventLine,
      UUID rightId) {
    XLOGGER.entry(event, eventLine);
    Profiler profiler = new Profiler("CALL_NOTIFICATION_FOR_LINE_ITEM");
    profiler.setLogger(XLOGGER);

    profiler.start("COPY_STOCK_CARD");
    OrderableLotIdentity identity = OrderableLotIdentity.identityOf(eventLine);
    StockCard stockCard = event.getContext().findCard(identity);

    if (stockCard.getStockOnHand() == 0) {
      stockoutNotifier.notifyStockEditors(stockCard, rightId);
    }
    // add issue notification here
    if (eventLine.getReasonId() != null) {
      StockCardLineItemReason reason = event.getContext()
              .findEventReason(eventLine.getReasonId());
      if (reason.isDebitReasonType() && reason.getReasonCategory()
              == ReasonCategory.TRANSFER) {
        // send issue notification
        issueNotifier.notifyStockEditors(stockCard, event, eventLine, rightId);
      }
    }

    profiler.stop().log();
    XLOGGER.exit();
  }

}
