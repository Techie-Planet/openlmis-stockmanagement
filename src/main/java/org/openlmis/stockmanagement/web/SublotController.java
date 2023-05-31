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

import java.util.List;
import java.util.UUID;

import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.repository.SublotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping('/api')
public class SublotController {
    @Autowired
    private SublotRepository sublotRepository;

    @RequestMapping(value = "sublots", method = RequestMethod.GET)
    public ResponseEntity<List<Sublot>> getSublotsByFacilityIdAndLotId(
            @RequestParam(name = "facilityId", required = false) UUID facilityId,
            @RequestParam(name = "lotId", required = false) UUID lotId
    ) {
        return new ResponseEntity<>(sublotRepository.findByFacilityIdAndLotId(facilityId, lotId), OK);

    }
}
