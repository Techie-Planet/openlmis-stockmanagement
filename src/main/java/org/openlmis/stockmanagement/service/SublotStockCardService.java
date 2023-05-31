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
import static org.openlmis.stockmanagement.domain.card.StockCardLineItem.createLineItemFrom;
import static org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity.identityOf;
import static org.openlmis.stockmanagement.domain.reason.ReasonCategory.PHYSICAL_INVENTORY;
import static org.openlmis.stockmanagement.domain.reason.ReasonCategory.SUBLOT;
import static org.openlmis.stockmanagement.domain.reason.ReasonType.CREDIT;
import static org.openlmis.stockmanagement.dto.SublotStockCardDto.createFrom;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.domain.sourcedestination.Organization;
import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.domain.sublot.SublotCalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCard;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCardLineItem;
import org.openlmis.stockmanagement.dto.SublotStockCardLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.SublotStockCardDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.repository.OrganizationRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.SublotCalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.SublotRepository;
import org.openlmis.stockmanagement.repository.SublotStockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.SublotStockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SublotStockCardService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SublotStockCardService.class);
  private static final String PHYSICAL_INVENTORY_REASON_PREFIX =
          "stockmanagement.reason.physicalInventory.";

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
  @Autowired
  private SublotCalculatedStockOnHandRepository sublotCalculatedStockOnHandRepository;
  @Autowired
  private SublotCalculatedStockOnHandService sublotCalculatedStockOnHandService;
  @Autowired
  private SublotStockCardLineItemRepository sublotStockCardLineItemRepository;
  @Autowired
  private OrganizationRepository organizationRepository;
  @Autowired
  private MessageService messageService;


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
      /* Get the sublot credit reason.
       For every stockEventLineitemDto, if it is a debit reason,
       get it's stock card from the list of stock cards.
       Create a new stockCardLineItem with the credit reason,
       create a new sublotStockCard, and its line item, from the credit lineitem
       if reason is sublot reason, create new sublotstockcard,
       else, just check for sublot id in extra data, and find the sublotstockcard, and recalculate the CSOH for that card
       find sublot cards by parentStockCard, and sublotid, or create new*/

      StockCardLineItemReason creditReason =
              stockCardLineItemReasonRepository.findByReasonTypeAndReasonCategory(
                      CREDIT, SUBLOT
              );

      for (StockEventLineItemDto lineItem : stockEventDto.getLineItems()) {
        StockCardLineItemReason reason =
                stockEventDto.getContext().findEventReason(lineItem.getReasonId());
        if (reason != null &&
                (reason.isSublotReasonCategory() && reason.isDebitReasonType())) {

          LOGGER.info("find stock card, sublot");
          StockCard stockCard = findCard(stockEventDto, lineItem, stockCardsToUpdate);

          LOGGER.info("create credit line item");
          StockCardLineItem stockCardLineItem =
                  createLineItemFrom(
                          stockEventDto, lineItem, stockCard, savedEventId, processedDate);
          stockCardLineItem.setReason(creditReason);
          stockCardLineItem.setProcessedDate(ZonedDateTime.now());

          LOGGER.info("create sublot stock card line item");
          SublotStockCardLineItem sublotStockCardLineItem = new SublotStockCardLineItem();
          sublotStockCardLineItem.setStockCardLineItem(stockCardLineItem);
          sublotStockCardLineItem.setSublotStockCardLineItemExtraData(lineItem.getExtraData());

          LOGGER.info("create and save a new sublot from this lot");
          LotDto lotDto = lotReferenceDataService.findOne(stockCard.getLotId());
          Sublot newSublot = new Sublot();
          newSublot.setSublotCode(generateSublotCode(stockCard.getFacilityId(), lotDto));
          newSublot.setFacilityId(stockCard.getFacilityId());
          newSublot.setLotId(lotDto.getId());
          Sublot savedSublot = sublotRepository.save(newSublot);

          LOGGER.info("create new sublot stockcard");
          SublotStockCard sublotStockCard = new SublotStockCard();
          sublotStockCard.setSublot(savedSublot);
          sublotStockCard.setStockCard(stockCard);
          sublotStockCard.setOriginEventId(savedEventId);

          LOGGER.info("saving sublotstockcard and line items");
          SublotStockCard savedSublotStockCard = sublotStockCardRepository.save(sublotStockCard);
          sublotStockCardLineItem.setSublotStockCard(savedSublotStockCard);
          sublotStockCardLineItemRepository.save(sublotStockCardLineItem);

          LOGGER.info("use old extraData");
          stockCardService.checkSublotDebitReasonAndUsePreviousStatus(
                  stockCard, lineItem, stockEventDto);

          LOGGER.info("save the new sublot stockCard calculated SOH");
          SublotCalculatedStockOnHand sublotCalculatedStockOnHand = new SublotCalculatedStockOnHand();
          sublotCalculatedStockOnHand.setSublotStockCard(savedSublotStockCard);
          sublotCalculatedStockOnHand.setSublotStockOnHand(lineItem.getQuantity());
          sublotCalculatedStockOnHand.setOccurredDate(lineItem.getOccurredDate());
          sublotCalculatedStockOnHand.setProcessedDate(stockCardLineItem.getProcessedDate());
          sublotCalculatedStockOnHandRepository.save(sublotCalculatedStockOnHand);

          LOGGER.info("set updated card back into list");
          stockCardsToUpdate.stream()
                  .filter(card -> card.getId() == stockCard.getId())
                  .findFirst()
                  .ifPresent(foundStockCard -> {
                    int index = stockCardsToUpdate.indexOf(foundStockCard);
                    stockCardsToUpdate.set(index, stockCard);
                  });

        }
      }
        LOGGER.info("Sublot Stock cards and line items saved");
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

    return card;
  }

  public SublotStockCardDto findSublotStockCard(String sublotCode) {
      LOGGER.info("finding sublot stock card");
    Optional<SublotStockCard> sublotStockCard = sublotStockCardRepository
            .findBySublotSublotCode(sublotCode);
    if (sublotStockCard.isPresent()) {
      SublotStockCard foundsublotStockCard = sublotStockCard.get();
      calculateSublotLineItemsSOH(foundsublotStockCard);
      sublotCalculatedStockOnHandService.fetchStockOnHand(
              foundsublotStockCard, LocalDate.now());
      sublotCalculatedStockOnHandService.recalculateStockOnHand(foundsublotStockCard);
      SublotStockCardDto sublotStockCardDto = createFrom(foundsublotStockCard);
      assignSourceDestinationReasonNameForLineItems(sublotStockCardDto.getSublotLineItems());

      return sublotStockCardDto;
    }
    throw new RuntimeException("Sublot Stock card not found");

  }

  private String generateSublotCode(UUID facilityId, LotDto lotDto) {
    FacilityDto facilityDto = facilityReferenceDataService.findOne(facilityId);
    List<String> sublotsInFacility = sublotRepository
            .findByFacilityIdAndLotId(facilityId, lotDto.getId())
            .stream()
            .map(Sublot::getSublotCode)
            .collect(Collectors.toList());

    Set<String> existingSublotCodes = new HashSet<>(sublotsInFacility);

    int numberOfSublots = sublotsInFacility.size();
    String sublotCode;

    do {
      int incrementedNumber = numberOfSublots + 1;
      String formattedNumber = String.format("%02d", incrementedNumber);
      sublotCode = facilityDto.getCode() + "/" + lotDto.getLotCode() + "/SUB0" + formattedNumber;

      if (!existingSublotCodes.contains(sublotCode)) {
        break;
      }

      numberOfSublots++;
    } while (true);

    System.out.println(sublotCode);
    return sublotCode;
  }

  private void calculateSublotLineItemsSOH(SublotStockCard sublotStockCard) {
    List<SublotStockCardLineItem> lineItems = sublotStockCard.getLineItems();
    if (lineItems != null && !lineItems.isEmpty()) {
      AtomicInteger soh = new AtomicInteger();
      lineItems.forEach(sublotStockCardLineItem -> {
        StockCardLineItem stockCardLineItem = sublotStockCardLineItem.getStockCardLineItem();
        int newSoh = sublotCalculatedStockOnHandService
                .calculateStockOnHand(stockCardLineItem, soh.get());
        soh.set(newSoh);
      });
    }

  }

  private void assignSourceDestinationReasonNameForLineItems(
          List<SublotStockCardLineItemDto> sublotStockCardLineItemDtos) {
    sublotStockCardLineItemDtos.forEach(lineItemDto -> {
      StockCardLineItem lineItem = lineItemDto.getStockCardLineItemDto().getLineItem();
      assignReasonName(lineItem);
      lineItemDto.getStockCardLineItemDto()
              .setSource(getFromRefDataOrConvertOrg(lineItem.getSource()));
      lineItemDto.getStockCardLineItemDto()
              .setDestination(getFromRefDataOrConvertOrg(lineItem.getDestination()));
    });
  }

  private void assignReasonName(StockCardLineItem lineItem) {
    lineItem.setReasonFreeText(lineItem.getReason().getName());
  }

  private FacilityDto getFromRefDataOrConvertOrg(Node node) {
    if (node == null) {
      return null;
    }

    if (node.isRefDataFacility()) {
      LOGGER.debug("Calling ref data to retrieve facility info for line item");
      return facilityReferenceDataService.findOne(node.getReferenceId());
    } else {
      Organization org = organizationRepository.findById(node.getReferenceId()).orElse(null);
      if (null != org) {
        return FacilityDto.createFrom(org);
      } else {
        LOGGER.warn("Could not find any organization matching node id {}", node.getReferenceId());
        return FacilityDto.createFrom(new Organization());
      }
    }
  }
}
