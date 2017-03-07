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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_ORGANIZATION_NAME_MISSING;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import org.openlmis.stockmanagement.domain.movement.Organization;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.OrganizationRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.utils.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api")
public class OrganizationController {

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private PermissionService permissionService;

  private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationController.class);

  /**
   * Create a new organization.
   *
   * @param organization organization object bound to request body
   * @return created organization.
   */
  @RequestMapping(value = "organizations", method = RequestMethod.POST)
  public ResponseEntity<Organization> createOrganization(@RequestBody Organization organization) {
    LOGGER.debug("Try to create a new organization.");
    permissionService.canManageOrganizations();
    organization.setId(null);
    checkIsValidRequest(organization);
    if (isDuplicateOrganization(organization)) {
      return new ResponseEntity<>(organization, OK);
    }
    return new ResponseEntity<>(organizationRepository.save(organization), CREATED);
  }

  /**
   * Retrieve all organizations.
   *
   * @return list of organizations.
   */
  @RequestMapping(value = "organizations", method = RequestMethod.GET)
  public ResponseEntity<List<Organization>> getAllOrganizations() {
    permissionService.canManageOrganizations();
    return new ResponseEntity<>(organizationRepository.findAll(), OK);
  }

  /**
   * Update an existing organization.
   *
   * @param id           update organization ID
   * @param organization object bound to request body
   * @return updated organization
   */
  @RequestMapping(value = "organizations/{id}", method = RequestMethod.PUT)
  public ResponseEntity<Organization> updateOrganization(
      @PathVariable("id") UUID id, @RequestBody Organization organization) {
    permissionService.canManageOrganizations();
    LOGGER.debug("Try to update organization with id: ", id.toString());
    checkUpdateOrganizationExists(id);
    organization.setId(id);
    if (isDuplicateOrganization(organization)) {
      throw new ValidationMessageException(new Message("key"));
    }
    return new ResponseEntity<>(organizationRepository.save(organization), OK);
  }

  private void checkUpdateOrganizationExists(UUID id) {
    if (organizationRepository.findOne(id) == null) {
      throw new ValidationMessageException(new Message("key"));
    }
  }

  private boolean isDuplicateOrganization(@RequestBody Organization organization) {
    return organizationRepository.findByName(organization.getName()) != null;
  }

  private void checkIsValidRequest(@RequestBody Organization organization) {
    if (organization.getName() == null) {
      throw new ValidationMessageException(new Message(ERROR_ORGANIZATION_NAME_MISSING));
    }
  }
}
