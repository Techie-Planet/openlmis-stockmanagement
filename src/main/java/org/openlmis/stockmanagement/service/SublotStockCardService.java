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



import static java.time.ZonedDateTime.now;
import static org.openlmis.stockmanagement.domain.card.StockCard.createStockCardFrom;
import static org.openlmis.stockmanagement.domain.card.StockCardLineItem.createLineItemFrom;
import static org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity.identityOf;
import static org.openlmis.stockmanagement.domain.reason.ReasonCategory.SUBLOT;
import static org.openlmis.stockmanagement.domain.reason.ReasonType.CREDIT;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCard;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCardLineItem;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.SublotRepository;
import org.openlmis.stockmanagement.repository.SublotStockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.StockCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SublotStockCardService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SublotStockCardService.class);

  @Autowired
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;
  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;
  @Autowired
  private SublotRepository sublotRepository;
  @Autowired
  private StockCardService stockCardService;
  @Autowired
  private LotReferenceDataService lotReferenceDataService;
  @Autowired
  private SublotStockCardRepository sublotStockCardRepository;
  @Autowired
  private StockCardRepository stockCardRepository;




  /**
   * Generate sublot stock cards based on event, and persist them.
   *
   * @param stockEventDto the origin event.
   * @param savedEventId  saved event id.
   */
    @Transactional
    void saveFromEvent(
            StockEventDto stockEventDto,
            UUID savedEventId,
            List<StockCard> stockCardsToUpdate,
            ZonedDateTime processedDate
    ) {
      // check sublot reason, save sublots from event, create sublot stockcard
      // sublots SOH recalculate SOH
      // send all the cardsToUpdate into sublotStockCardService. it also contains all the line items (and sublotId in extradata)
      // if reason is sublot reason, create new sublotstockcard,
      // else, just check for sublot id in extra data, and find the sublotstockcard, and recalculate the CSOH for that card
      // find sublot cards by parentStockCard, and sublotid, or create new

      StockCardLineItemReason creditReason =
              stockCardLineItemReasonRepository.findByReasonTypeAndReasonCategory(CREDIT, SUBLOT);

      for (StockEventLineItemDto lineItem : stockEventDto.getLineItems()) {
        StockCardLineItemReason reason =
                stockEventDto.getContext().findEventReason(lineItem.getReasonId());
        if (reason != null &&
                (reason.isSublotReasonCategory() && reason.isDebitReasonType())) {

          // create a sublot credit
          // create sublot
          StockCard stockCard =
                  findCard(stockEventDto, lineItem, stockCardsToUpdate);

          StockCardLineItem stockCardLineItem =
                  createLineItemFrom(
                          stockEventDto, lineItem, stockCard, savedEventId, processedDate);
          stockCardLineItem.setReason(creditReason);
          stockCard.getLineItems().add(stockCardLineItem);
          // create new sublotStockcard line item
          SublotStockCardLineItem sublotStockCardLineItem = new SublotStockCardLineItem();
          sublotStockCardLineItem.setStockCardLineItem(stockCardLineItem);
          sublotStockCardLineItem.setSublotStockCardLineItemExtraData(lineItem.getExtraData());

          // add the updated stock card back into the list
          stockCardsToUpdate.stream()
                  .filter(card -> card.getId() == stockCard.getId())
                  .findFirst()
                  .ifPresent(foundStockCard -> {
                    int index = stockCardsToUpdate.indexOf(foundStockCard);
                    stockCardsToUpdate.set(index, stockCard);
                  });

          LotDto lotDto = lotReferenceDataService.findOne(stockCard.getLotId());

          Sublot newSublot = new Sublot();
          newSublot.setSublotCode(generateSublotCode(stockCard.getFacilityId(), lotDto));
          newSublot.setFacilityId(stockCard.getFacilityID());
          newSublot.setLot(lotDto);
          Sublot savedSublot = sublotRepository.save(newSublot);

          //return


          //create new sublotStockCard
          SublotStockCard sublotStockCard = new SublotStockCard();
          sublotStockCard.setSublot(savedSublot);
          sublotStockCard.setStockCard(stockCard);
          sublotStockCard.setOriginEventId(savedEventId);
          sublotStockCard.getLineItems().add(sublotStockCardLineItem);
          // recalculate cSOH for sublot stockcard

          sublotRepository.save(sublotStockCard);
          stockCardService.checkSublotDebitReasonAndUsePreviousStatus(stockCard, lineItem, stockEventDto);

          stockCardRepository.saveAll(cardsToUpdate);
          stockCardRepository.flush();





        }

      }
        LOGGER.debug("Stock cards and line items saved");
    }
  private StockCard findCard(StockEventDto eventDto, StockEventLineItemDto eventLineItem,
                                     List<StockCard> cardsToUpdate) {
    OrderableLotIdentity identity = identityOf(eventLineItem);
    System.out.println("finding card");
    StockCard card = eventDto.getContext().findCard(identity);

    if (null == card) {
      System.out.println("from cardsToUpdate");
      card = cardsToUpdate
              .stream()
              .filter(elem -> identityOf(elem).equals(identity))
              .findFirst()
              .orElse(null);
    }

    System.out.println("found card");
    System.out.println(card.toString());

    return card;
  }

  private String generateSublotCode(UUID facilityId, LotDto lotDto) {
      FacilityDto facilityDto = facilityReferenceDataService.findOne(facilityId);
      List<Sublot> sublotsInFacility =
              sublotRepository.findByFacilityIdAndLotId(facilityId, lotDto.getId());
    int numberOfSublots = sublotsInFacility.size();
    String formattedNumber = String.format("%02d", numberOfSublots + 1);
    String sublotCode = "" + facilityDto.getCode()
            + "/" + lotDto.getLotCode() + "/SUB0" + formattedNumber;
    System.out.println(sublotCode);
    return sublotCode;

  }
}
