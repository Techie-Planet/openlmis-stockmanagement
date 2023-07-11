CREATE TABLE IF NOT EXISTS sublots (
    id UUID PRIMARY KEY,
    lotid UUID NOT NULL,
    sublotcode VARCHAR(255) NOT NULL,
    facilityid UUID NOT NULL
    );

CREATE TABLE IF NOT EXISTS sublot_stock_cards (
    id UUID PRIMARY KEY,
    sublotid UUID NOT NULL,
    stockcardid UUID NOT NULL,
    origineventid UUID NOT NULL,
    sublotstockonhand INTEGER
    );

CREATE TABLE IF NOT EXISTS sublot_stock_card_line_items (
    id UUID PRIMARY KEY,
    stockcardlineitemid UUID,
    sublotstockcardlineitemextradata JSONB,
    sublotstockcardid UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS sublot_calculated_stocks_on_hand (
    id UUID PRIMARY KEY,
    sublotstockcardid UUID NOT NULL,
    sublotstockonhand INTEGER NOT NULL,
    occurreddate DATE NOT NULL,
    processeddate TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS useby_statuses (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    useexpirydate BOOLEAN NOT NULL
);

ALTER TABLE sublot_stock_cards ADD FOREIGN KEY (sublotid) REFERENCES sublots (id);
ALTER TABLE sublot_stock_cards ADD FOREIGN KEY (stockcardid) REFERENCES stock_cards (id);
ALTER TABLE sublot_stock_card_line_items ADD FOREIGN KEY (stockcardlineitemid) REFERENCES stock_card_line_items (id);
ALTER TABLE sublot_stock_card_line_items ADD FOREIGN KEY (sublotstockcardid) REFERENCES sublot_stock_cards (id);
ALTER TABLE sublot_calculated_stocks_on_hand ADD FOREIGN KEY (sublotstockcardid) REFERENCES sublot_stock_cards (id);
