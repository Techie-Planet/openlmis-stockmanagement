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

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_EMAIL_PHYSICAL_INVENTORY_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_EMAIL_PHYSICAL_INVENTORY_SUBJECT;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.Data;
import org.openlmis.stockmanagement.dto.referencedata.FacilityTypeDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.service.referencedata.FacilityTypeReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.UserReferenceDataService;
import org.openlmis.stockmanagement.service.notifier.UserNotifier;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Data
@Component
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PhysicalInventoryNotifier {

  @Autowired
  private MessageService messageService;
  @Autowired
  private UserReferenceDataService userReferenceDataService;
  @Autowired
  private RightReferenceDataService rightReferenceDataService;
  @Autowired
  private FacilityTypeReferenceDataService facilityTypeReferenceDataService;
  @Autowired
  private UserNotifier userNotifier;

  @Value("${time.zoneId}")
  private String timeZoneId;
  @Value("${publicUrl}")
  private String publicUrl;
  private LocalDate currentDate;
  private final String physicalInventoryUrl = "stockmanagement/physicalInventory";
  private final String nationalColdStore = "National Cold Store";
  private final String zonalColdStore = "Zonal Cold Store";
  private final String satelliteColdStore = "Satellite Cold Store";
  private final String stateColdStore = "State Cold Store";
  private final String lgaColdStore = "LGA Cold Store";
  private final String stockAdjust = "STOCK_ADJUST";
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(PhysicalInventoryNotifier.class);

  /**
   * Send a weekly email reminder to all LGA facility editors to
   * update their stock on hand.
   */
  @Scheduled(cron = "${notifications.physicalInventoryWeekly.cron}",
          zone = "${time.zoneId}")
  public void physicalInventoryWeeklyNotifier() {
    Profiler profiler = new Profiler("PHYSICAL_INVENTORY_WEEKLY");
    profiler.setLogger(XLOGGER);
    sendPhysicalInventoryNotification(profiler, lgaColdStore);
  }

  /**
   * Send a monthly email reminder to all state and
   * satellite facility editors to
   * update their stock on hand.
   */
  @Scheduled(cron = "${notifications.physicalInventoryMonthly.cron}", zone = "${time.zoneId}")
  public void physicalInventoryMonthlyNotifier() {
    Profiler profiler = new Profiler("PHYSICAL_INVENTORY_MONTHLY");
    profiler.setLogger(XLOGGER);
    sendPhysicalInventoryNotification(profiler, stateColdStore);
    sendPhysicalInventoryNotification(profiler, satelliteColdStore);
  }

  /**
   * Send a quarterly email reminder to all national cold store editors to
   * update their stock on hand.
   */
  @Scheduled(cron = "${notifications.physicalInventoryQuarterly.cron}", zone = "${time.zoneId}")
  public void physicalInventoryQuarterlyNotifier() {
    Profiler profiler = new Profiler("PHYSICAL_INVENTORY_QUARTERLY");
    profiler.setLogger(XLOGGER);
    sendPhysicalInventoryNotification(profiler, nationalColdStore);
    sendPhysicalInventoryNotification(profiler, zonalColdStore);
  }

  private void sendPhysicalInventoryNotification(Profiler profiler, String facilityTypeName) {
    currentDate = LocalDate.now(ZoneId.of(timeZoneId));
    XLOGGER.debug("Physical inventory report date = {}", currentDate);

    XLOGGER.debug("Getting facility type");

    List<FacilityTypeDto> facilitytype = facilityTypeReferenceDataService
            .getAllFacilityTypes().stream()
            .filter(each -> each.getName().equals(facilityTypeName)).collect(Collectors.toList());
    UUID facilityTypeId = facilitytype.get(0).getId();
    XLOGGER.debug("Getting user rights");
    RightDto right = rightReferenceDataService.findRight(stockAdjust);
    UUID rightId = right.getId();

    XLOGGER.debug("Getting all users");
    Set<UserDto> allUsers = userReferenceDataService
            .getUsersWithRightInFacilityType(rightId, facilityTypeId).stream()
            .collect(Collectors.toSet());
    XLOGGER.debug("Sending emails");
    sendBulkNotifications(profiler, allUsers);
  }

  /**
   * Method to send bulk notifications.
   */
  public void sendBulkNotifications(Profiler profiler, Collection<UserDto> userDtos) {
    XLOGGER.debug("SENDING_TO_USERS");
    userDtos.forEach(user -> {
      NotificationMessageParams params = new NotificationMessageParams(
              getMessage(NOTIFICATION_EMAIL_PHYSICAL_INVENTORY_SUBJECT),
              getMessage(NOTIFICATION_EMAIL_PHYSICAL_INVENTORY_CONTENT),
              constructSubstitutionMap(user));
      userNotifier.notifyUser(user, params);
    });
    profiler.stop().log();
  }

  Map<String, String> constructSubstitutionMap(UserDto user) {
    Map<String, String> messageParams = new HashMap<>();
    messageParams.put("username", user.getUsername());
    messageParams.put("date", String.valueOf(currentDate));
    messageParams.put("urlToPhysicalInventory", publicUrl + physicalInventoryUrl);

    return messageParams;
  }

  private String getMessage(String key) {
    return messageService
            .localize(new org.openlmis.stockmanagement.util.Message(key))
            .getMessage();
  }
}
