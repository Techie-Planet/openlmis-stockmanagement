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

package org.openlmis.stockmanagement.service.notifier;

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_MULTIPLE_STOCK_ISSUE_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_STOCK_ISSUE_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_STOCK_ISSUE_SUBJECT;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.SupervisoryNodeDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.notifier.BaseNotifier;
import org.openlmis.stockmanagement.service.notifier.StockCardNotifier;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisingUsersReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.UserReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class IssueNotifier extends BaseNotifier {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(IssueNotifier.class);

  @Autowired
  private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Autowired
  private NotificationService notificationService;
  @Autowired
  private LotReferenceDataService lotReferenceDataService;

  @Autowired
  private MessageService messageService;

  @Autowired
  private StockCardNotifier stockCardNotifier;

  @Autowired
  private NodeRepository nodeRepository;
  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Value("${email.urlToStockReceive}")
  private String urlToStockReceive;

  /**
   * Notify users with a certain right that line items have been issued to their facility.
   *
   * @param stockCard StockCard for a product
   * @param rightId right UUID
   */
  public void notifyStockEditors(StockCard stockCard,
                                 StockEventDto event,
                                 StockEventLineItemDto eventLine,
                                 UUID rightId) {
    NotificationMessageParams params = new NotificationMessageParams(
            getMessage(NOTIFICATION_STOCK_ISSUE_SUBJECT),
            getMessage(NOTIFICATION_STOCK_ISSUE_CONTENT),
            constructSubstitutionMap(stockCard, event));
    notifyStockRecipients(stockCard, eventLine, rightId, params);
  }

  Map<String, String> constructSubstitutionMap(StockCard stockCard, StockEventDto event) {
    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("facilityName", getFacilityName(event.getFacilityId()));
    valuesMap.put("orderableName", getOrderableName(stockCard.getOrderableId()));
    valuesMap.put("orderableNameLotInformation",
            getOrderableNameLotInformation(valuesMap.get("orderableName"), stockCard.getLotId()));
    valuesMap.put("programName", getProgramName(stockCard.getProgramId()));
    valuesMap.put("urlToStockReceive", urlToStockReceive);
    valuesMap.put("issueId", event.getDocumentNumber());
    return valuesMap;
  }

  /**
   * Notify receiving editors of issue event.
   *
   * @param stockCard StockCard for a product
   * @param eventLine the event line item
   * @param rightId right UUID
   * @param params message params to construct message
   */
  @Async
  private void notifyStockRecipients(StockCard stockCard,
                                     StockEventLineItemDto eventLine, UUID rightId,
                                     NotificationMessageParams params) {
    Profiler profiler = new Profiler("NOTIFY_STOCK_RECIPIENTS");
    profiler.setLogger(XLOGGER);

    profiler.start("GET_EDITORS");

    UUID receivingFacilityId = nodeRepository
            .findById(eventLine.getDestinationId()).get().getReferenceId();
    Collection<UserDto> recipients = getEditors(
            stockCard.getProgramId(), receivingFacilityId, rightId);

    Map<String, String> valuesMap = params.getSubstitutionMap();
    StrSubstitutor sub = new StrSubstitutor(valuesMap);

    profiler.start("NOTIFY_RECIPIENTS");
    for (UserDto recipient : recipients) {
      if (receivingFacilityId.equals(recipient.getHomeFacilityId())) {
        valuesMap.put("username", recipient.getUsername());
        XLOGGER.debug("Recipient username = {}", recipient.getUsername());
        notificationService.notify(recipient,
                sub.replace(params.getMessageSubject()), sub.replace(params.getMessageContent()));
      }
    }

    profiler.stop().log();
  }

  private Collection<UserDto> getEditors(UUID programId, UUID facilityId, UUID rightId) {

    SupervisoryNodeDto supervisoryNode = supervisoryNodeReferenceDataService
            .findSupervisoryNode(programId, facilityId);

    if (supervisoryNode == null) {
      throw new IllegalArgumentException(
              String.format("There is no supervisory node for program %s and facility %s",
                      programId, facilityId));
    }

    XLOGGER.debug("Supervisory node ID = {}", supervisoryNode.getId());

    List<UserDto> supervisingUsers = Optional
            .ofNullable(supervisoryNode)
            .map(node -> supervisingUsersReferenceDataService
                    .findAll(node.getId(), rightId, programId))
            .orElse(Collections.emptyList());

    List<UserDto> homeUsers = userReferenceDataService
            .findByRight(rightId, programId, null);
    Set<UserDto> users = new HashSet<>(supervisingUsers);
    users.addAll(homeUsers);

    return users;
  }

  private String getOrderableNameLotInformation(String orderableName, UUID lotId) {
    if (lotId != null) {
      LotDto lot = lotReferenceDataService.findOne(lotId);
      return orderableName + " " + lot.getLotCode();
    }
    return orderableName;
  }

  private String getMessage(String key) {
    return messageService
            .localize(new org.openlmis.stockmanagement.util.Message(key))
            .getMessage();
  }
}
