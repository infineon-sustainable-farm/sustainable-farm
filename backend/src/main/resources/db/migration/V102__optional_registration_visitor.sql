-- A registration no longer requires an attached visitor (admin/event registrations
-- may be recorded without creating a Visitor record first).
alter table if exists registration
    alter column visitor_id drop not null;
