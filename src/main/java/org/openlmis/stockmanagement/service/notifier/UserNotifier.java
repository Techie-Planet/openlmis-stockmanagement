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

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_NEAR_EXPIRY_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_NEAR_EXPIRY_SUBJECT;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.referencedata.UserReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

@Component
public class UserNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(UserNotifier.class);

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserReferenceDataService userReferenceDataService;


    /**
     * Notify users with a certain message.
     *
     * @param params message params to construct message
     */
    @Async
    public void notifyUser(UserDto userDto, NotificationMessageParams params) {
        Profiler profiler = new Profiler("NOTIFY_STOCK_EDITORS");
        profiler.setLogger(XLOGGER);

        Map<String, String> valuesMap = params.getSubstitutionMap();
        StrSubstitutor sub = new StrSubstitutor(valuesMap);

        profiler.start("NOTIFY_RECIPIENTS");
        XLOGGER.debug("Recipient username = {}", userDto.getUsername());
        notificationService.notify(userDto,
                sub.replace(params.getMessageSubject()), sub.replace(params.getMessageContent()));

        profiler.stop().log();
        XLOGGER.exit();
    }
}
