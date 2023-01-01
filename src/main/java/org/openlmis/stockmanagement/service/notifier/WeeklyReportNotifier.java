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

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_EMAIL_WEEKLY_REPORT_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_EMAIL_WEEKLY_REPORT_SUBJECT;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.service.notifier.UserNotifier;
import org.openlmis.stockmanagement.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.UserReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class WeeklyReportNotifier {
  @Autowired
  private MessageService messageService;
  @Autowired
  private ProgramReferenceDataService programReferenceDataService;
  @Autowired
  private UserReferenceDataService userReferenceDataService;
  @Autowired
  private RightReferenceDataService rightReferenceDataService;
  @Autowired
  private UserNotifier userNotifier;


  @Value("${time.zoneId}")
  private String timeZoneId;
  @Value("${publicUrl}")
  private String publicUrl;
  private LocalDate weeklyReportDate;
  private final String sohUrl = "stockmanagement/stockCardSummaries";
  private final String stockAdjust = "STOCK_ADJUST";
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(WeeklyReportNotifier.class);

  /**
   * Send a weekly email reminder to all facility editors to
   * turn in weekly reports.
   */
  @Scheduled(cron = "${notifications.weeklyReport.cron}", zone = "${time.zoneId}")
  public void weeklyReportNotifier() {
    System.out.println("start weekly report");
    try {
      Profiler profiler = new Profiler("SEND_WEEKLY_REPORT_NOTIFICATION");
      profiler.setLogger(XLOGGER);
      weeklyReportDate = LocalDate.now(ZoneId.of(timeZoneId));
      XLOGGER.debug("Weekly report date = {}", weeklyReportDate);
      System.out.println("get program ids");

      XLOGGER.debug("Getting all program Ids");
      Collection<UUID> programIds = programReferenceDataService
              .findPrograms(new HashMap<String, Object>()).stream()
              .map(each -> each.getId()).collect(Collectors.toList());
      System.out.println(programIds);
      XLOGGER.debug("Getting user rights");
      RightDto right = rightReferenceDataService.findRight(stockAdjust);
      UUID rightId = right.getId();
      XLOGGER.debug("Getting all users");
      Set<UserDto> allUsers = new HashSet<>();
      for (UUID programId : programIds) {
        Collection<UserDto> userDtos = new ArrayList<>();
        //userDtos = Arrays.asList(userReferenceDataService.getUsers(programId, rightId).getResult());
        userDtos = userReferenceDataService.getUsers(programId, rightId);
        Set<UserDto> usersInProgram = new HashSet<>(userDtos);
        allUsers.addAll(usersInProgram);
      }

      sendBulkNotifications(profiler, allUsers);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Method to send bulk notifications.
   */
  public void sendBulkNotifications(Profiler profiler, Collection<UserDto> userDtos) {
    XLOGGER.debug("SENDING_TO_USERS");
    userDtos.forEach(user -> {
      NotificationMessageParams params = new NotificationMessageParams(
              getMessage(NOTIFICATION_EMAIL_WEEKLY_REPORT_SUBJECT),
              getMessage(NOTIFICATION_EMAIL_WEEKLY_REPORT_CONTENT),
              constructSubstitutionMap(user));
      userNotifier.notifyUser(user, params);
    });
    profiler.stop().log();
  }

  Map<String, String> constructSubstitutionMap(UserDto user) {
    Map<String, String> messageParams = new HashMap<>();
    messageParams.put("username", user.getUsername());
    messageParams.put("date", String.valueOf(weeklyReportDate));
    messageParams.put("urlToSOH", publicUrl + sohUrl);

    return messageParams;
  }

  private String getMessage(String key) {
    return messageService
        .localize(new Message(key))
        .getMessage();
  }
}
