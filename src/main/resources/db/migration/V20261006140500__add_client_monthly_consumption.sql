ALTER TABLE clients
    ADD COLUMN monthly_consumption NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN consumption_month DATE;

ALTER TABLE clients
    ADD CONSTRAINT ck_clients_monthly_consumption_non_negative
        CHECK (monthly_consumption >= 0),
    ADD CONSTRAINT ck_clients_consumption_month_first_day
        CHECK (
            consumption_month IS NULL
            OR EXTRACT(DAY FROM consumption_month) = 1
        );