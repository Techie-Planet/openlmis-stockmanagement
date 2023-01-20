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

package org.openlmis.stockmanagement.service.referencedata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openlmis.stockmanagement.dto.referencedata.ResultDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.ServiceResponse;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class UserReferenceDataService extends BaseReferenceDataService<UserDto> {

  @Override
  protected String getUrl() {
    return "/api/users/";
  }

  @Override
  protected Class<UserDto> getResultClass() {
    return UserDto.class;
  }

  @Override
  protected Class<UserDto[]> getArrayResultClass() {
    return UserDto[].class;
  }

  public Collection<UserDto> findUsers(Map<String, Object> parameters) {
    return findAll("search", parameters);
  }

  /**
   * This method retrieves a user with given name.
   *
   * @param name the name of user.
   * @return UserDto containing user's data, or null if such user was not found.
   */
  public UserDto findUser(String name) {
    Map<String, Object> payload = Collections.singletonMap("username", name);

    Page<UserDto> users = getPage("search", Collections.emptyMap(), payload);
    return users.getContent().isEmpty() ? null : users.getContent().get(0);
  }

  /**
   * Check if user has a right with certain criteria.
   *
   * @param user     id of user to check for right
   * @param right    right to check
   * @param program  program to check (for supervision rights, can be {@code null})
   * @param facility facility to check (for supervision rights, can be {@code null})
   * @return {@link ResultDto} of true or false depending on if user has the right.
   */
  public ResultDto<Boolean> hasRight(UUID user, UUID right, UUID program, UUID facility,
                                     UUID warehouse) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("rightId", right)
        .set("programId", program)
        .set("facilityId", facility)
        .set("warehouseId", warehouse);

    return getResult(user + "/hasRight", parameters, Boolean.class);
  }

  public ServiceResponse<List<String>> getPermissionStrings(UUID user, String etag) {
    return tryFindAll(user + "/permissionStrings", String[].class, etag);
  }

  /**
   * Find users with particular right in program.
   *
   * @param rightId    right to check
   * @param programId  program to check (for supervision rights, can be {@code null})
   * @return {@link ResultDto} of true or false depending on if user has the right.
   */
  public Collection<UserDto> getUsers(UUID programId, UUID rightId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("rightId", rightId);
    parameters.put("programId", programId);
    return findAll("rightSearch", parameters);
  }

  /**
   * Method to get all users with a right in facility type.
   * @param facilityTypeId     id of facility type to check
   * @param rightId    right to check
   * @return an instance of {@link ResultDto} with results.
   */
  public Collection<UserDto> getUsersWithRightInFacilityType(UUID rightId, UUID facilityTypeId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("rightId", rightId);
    parameters.put("facilityTypeId", facilityTypeId);
    return findAll("rightFacilityTypeSearch", parameters);
  }

  /**
   * Retrieves all users that have supervised role with the given right and program and supervisory
   * node. If supervisory node is not provided, the method will return all users that have
   * supervised role for home facility with the given right and program.
   *
   * @param rightId UUID of supervised right
   * @param programId UUID of program
   * @param supervisoryNodeId UUID of supervisory node. Can be null.
   * @return a list of users that match parameters.
   */
  public List<UserDto> findByRight(UUID rightId, UUID programId, UUID supervisoryNodeId) {
    RequestParameters parameters = RequestParameters
            .init()
            .set("rightId", rightId)
            .set("programId", programId)
            .set("supervisoryNodeId", supervisoryNodeId);

    return findAll("rightSearch", parameters);
  }
}
