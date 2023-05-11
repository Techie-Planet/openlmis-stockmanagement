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

import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.extension.ExtensionManager;
import org.openlmis.stockmanagement.extension.point.AdjustmentReasonValidator;
import org.openlmis.stockmanagement.extension.point.ExtensionPointId;
import org.openlmis.stockmanagement.extension.point.FreeTextValidator;
import org.openlmis.stockmanagement.extension.point.UnpackKitValidator;
import org.openlmis.stockmanagement.validators.ApprovedOrderableValidator;
import org.openlmis.stockmanagement.validators.LotValidator;
import org.openlmis.stockmanagement.validators.MandatoryFieldsValidator;
import org.openlmis.stockmanagement.validators.OrderableLotDuplicationValidator;
import org.openlmis.stockmanagement.validators.PhysicalInventoryAdjustmentReasonsValidator;
import org.openlmis.stockmanagement.validators.QuantityValidator;
import org.openlmis.stockmanagement.validators.ReasonExistenceValidator;
import org.openlmis.stockmanagement.validators.ReceiveIssueReasonValidator;
import org.openlmis.stockmanagement.validators.SourceDestinationAssignmentValidator;
import org.openlmis.stockmanagement.validators.SourceDestinationGeoLevelAffinityValidator;
import org.openlmis.stockmanagement.validators.StockEventVvmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An aggregator of all stock event validators.
 * All validators will run before any actual DB writing happens.
 * It any one validator detects something wrong, we'll stop processing the stock event.
 */
@Service
public class StockEventValidationsService {

  @Autowired
  private ApprovedOrderableValidator approvedOrderableValidator;

  @Autowired
  private LotValidator lotValidator;

  @Autowired
  private MandatoryFieldsValidator mandatoryFieldsValidator;

  @Autowired
  private OrderableLotDuplicationValidator orderableLotDuplicationValidator;

  @Autowired
  private PhysicalInventoryAdjustmentReasonsValidator physicalInventoryAdjustmentReasonsValidator;

  @Autowired
  private QuantityValidator quantityValidator;

  @Autowired
  private ReasonExistenceValidator existenceValidator;

  @Autowired
  private ReceiveIssueReasonValidator receiveIssueReasonValidator;

  @Autowired
  private SourceDestinationAssignmentValidator destinationAssignmentValidator;

  @Autowired
  private SourceDestinationGeoLevelAffinityValidator destinationGeoLevelAffinityValidator;

  @Autowired
  private StockEventVvmValidator stockEventVvmValidator;

  @Autowired
  private ExtensionManager extensionManager;

  /**
   * Validate stock event with permission service and all validators.
   *
   * @param stockEventDto the event to be validated.
   */
  public void validate(StockEventDto stockEventDto) {
    long startTime, endTime;

    startTime = System.currentTimeMillis();
    approvedOrderableValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("approvedOrderableValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    lotValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("lotValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    mandatoryFieldsValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("mandatoryFieldsValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    orderableLotDuplicationValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("orderableLotDuplicationValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    physicalInventoryAdjustmentReasonsValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("physicalInventoryAdjustmentReasonsValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    quantityValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("quantityValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    existenceValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("existenceValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    receiveIssueReasonValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("receiveIssueReasonValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    destinationAssignmentValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("destinationAssignmentValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    destinationGeoLevelAffinityValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("destinationGeoLevelAffinityValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    stockEventVvmValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("stockEventVvmValidator: " + (endTime - startTime) + "ms");

    AdjustmentReasonValidator adjustmentReasonValidator = extensionManager.getExtension(
        ExtensionPointId.ADJUSTMENT_REASON_POINT_ID, AdjustmentReasonValidator.class);
    FreeTextValidator freeTextValidator = extensionManager.getExtension(
        ExtensionPointId.FREE_TEXT_POINT_ID, FreeTextValidator.class);
    UnpackKitValidator unpackKitValidator = extensionManager.getExtension(
        ExtensionPointId.UNPACK_KIT_POINT_ID, UnpackKitValidator.class);

    startTime = System.currentTimeMillis();
    adjustmentReasonValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("adjustmentReasonValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    freeTextValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("freeTextValidator: " + (endTime - startTime) + "ms");

    startTime = System.currentTimeMillis();
    unpackKitValidator.validate(stockEventDto);
    endTime = System.currentTimeMillis();
    System.out.println("unpackKitValidator: " + (endTime - startTime) + "ms");
  }


}
