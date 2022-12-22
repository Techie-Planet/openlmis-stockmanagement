package main.java.org.openlmis.stockmanagement.service.notifier;

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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.openlmis.stockmanagement.service.notifier.StockCardNotifier;
//import org.openlmis.stockmanagement.service.notifier.NotificationMessageParams;

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_MULTIPLE_STOCK_ISSUE_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_STOCK_ISSUE_SUBJECT;

@Component
public class IssueNotifier {
  @Autowired
  private LotReferenceDataService lotReferenceDataService;

  @Autowired
  private MessageService messageService;

  @Autowired
  private StockCardNotifier stockCardNotifier;

  @Value("${email.urlToInitiateRequisition}")
  private String urlToInitiateRequisition;

  /**
   * Notify users with a certain right that line items have been issued to their facility.
   *
   * @param stockCard StockCard for a product
   * @param rightId right UUID
   */
  public void notifyStockEditors(StockCard stockCard, UUID rightId, Integer numberOfEventItems, UUID issuingFacilityId) {
    // if numberOfItems is 1, send the email with the product name and all,
    // if more than 1, send as number of products currently issued by the facility
    String content = numberOfEventItems > 1 ? NOTIFICATION_MULTIPLE_STOCK_ISSUE_CONTENT
            : NOTIFICATION_STOCK_ISSUE_CONTENT;

    NotificationMessageParams params = new NotificationMessageParams(
            getMessage(NOTIFICATION_STOCK_ISSUE_SUBJECT),
            getMessage(content),
            constructSubstitutionMap(stockCard, numberOfEventItems, issuingFacilityId));
    stockCardNotifier.notifyStockEditors(stockCard, rightId, params);
  }

  Map<String, String> constructSubstitutionMap(StockCard stockCard, Integer numberOfEventItems,
                                               UUID issuingFacilityId) {
    Map<String, String> valuesMap = new HashMap<>();
    //  valuesMap.put("facilityName", stockCardNotifier.getFacilityName(stockCard.getFacilityId()));
    valuesMap.put("facilityName", stockCardNotifier.getFacilityName(issuingFacilityId));
    valuesMap.put("orderableName", stockCardNotifier.getOrderableName(stockCard.getOrderableId()));
    valuesMap.put("orderableNameLotInformation",
            getOrderableNameLotInformation(valuesMap.get("orderableName"), stockCard.getLotId()));
    valuesMap.put("programName", stockCardNotifier.getProgramName(stockCard.getProgramId()));
    valuesMap.put("number", String.valueOf(numberOfEventItems));
    valuesMap.put("urlToViewBinCard", stockCardNotifier.getUrlToViewBinCard(stockCard.getId()));
    valuesMap.put("urlToInitiateRequisition", getUrlToInitiateRequisition(stockCard));
    valuesMap.put("issueId", "issueId");
    return valuesMap;
  }

  private String getOrderableNameLotInformation(String orderableName, UUID lotId) {
    if (lotId != null) {
      LotDto lot = lotReferenceDataService.findOne(lotId);
      return orderableName + " " + lot.getLotCode();
    }
    return orderableName;
  }

  private long getNumberOfDaysOfStockout(LocalDate stockoutDate) {
    return ChronoUnit.DAYS.between(stockoutDate, LocalDate.now());
  }

  private String getUrlToInitiateRequisition(org.openlmis.stockmanagement.domain.card.StockCard stockCard) {
    return MessageFormat.format(urlToInitiateRequisition,
            stockCard.getFacilityId(), stockCard.getProgramId(), "true", "false");
  }

  private String getMessage(String key) {
    return messageService
            .localize(new org.openlmis.stockmanagement.util.Message(key))
            .getMessage();
  }
}
