-- Reconciliation for databases created before Flyway versioning.
-- All statements are idempotent so they are safe on a fresh schema too.

alter table if exists booking
    alter column time_slot_id drop not null;

alter table if exists booking
    add column if not exists total_amount numeric(12,2) not null default 0;

alter table if exists booking
    alter column total_amount drop default;

alter table if exists registration
    alter column time_slot_id drop not null;

alter table if exists registration
    add column if not exists event_id bigint;

alter table if exists registration
    add column if not exists is_prospect boolean not null default false;

alter table if exists registration
    alter column is_prospect drop default;

alter table if exists registration
    add column if not exists visit_purpose varchar(20) not null default 'TOURISM';

alter table if exists registration
    alter column visit_purpose drop default;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'uk_registration_event_visitor') then
        alter table registration
            add constraint uk_registration_event_visitor unique (event_id, visitor_id);
    end if;
end $$;
