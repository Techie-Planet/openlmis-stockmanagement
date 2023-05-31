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

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openlmis.stockmanagement.domain.common.UseByStatus;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.UseByStatusRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_ORGANIZATION_ID_NOT_FOUND;

@Controller
@RequestMapping("/api/useByStatus")
public class UseByStatusController {
    @Autowired
    private UseByStatusRepository useByStatusRepository;

    @Autowired
    private PermissionService permissionService;
    private static final Logger LOGGER = LoggerFactory.getLogger(UseByStatusController.class);

    /**
     * Create a new use-by status. If the ID is specified, ID will be ignored.
     *
     * @param useByStatus object bound to request body
     * @return created use-by status.
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<UseByStatus> createUseByStatus(@RequestBody UseByStatus useByStatus) {
        LOGGER.debug("Try to create a new use-by status.");
        // permissionService.canManageOrganizations();
        useByStatus.setId(null);
        // checkRequiredFieldsExist(organization);
        UseByStatus foundUseByStatus = useByStatusRepository.findByName(useByStatus.getName());
        if (foundUseByStatus != null) {
            return new ResponseEntity<>(foundUseByStatus, OK);
        }
        return new ResponseEntity<>(useByStatusRepository.save(useByStatus), CREATED);
    }

    /**
     * Retrieve all use-by statuses.
     *
     * @return page of use-by statuses.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<UseByStatus>> getAllUseByStatuses(Pageable pageable,
          @RequestParam(name = "name", required = false) String name) {
        // permissionService.canManageOrganizations();

        return new ResponseEntity<>(useByStatusRepository.findAll(pageable), OK);
    }

    /**
     * Update an existing use-by status.
     *
     * @param id           update use-by status ID
     * @param useByStatus object bound to request body
     * @return updated use-by status
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<UseByStatus> updateUseByStatus(
            @PathVariable("id") UUID id, @RequestBody UseByStatus useByStatus) {
        // permissionService.canManageOrganizations();
        LOGGER.debug("Try to update use-by status with id: ", id.toString());
        // checkIsValidUpdateModel(id, organization);
        useByStatus.setId(id);
        return new ResponseEntity<>(useByStatusRepository.save(useByStatus), OK);
    }
}
