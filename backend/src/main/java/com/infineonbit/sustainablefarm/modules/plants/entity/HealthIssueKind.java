package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Nature of a {@link HealthIssueReference}: an animal pest, a disease, or a
 * problem the catalogue does not name.
 *
 * <p>Stored as text with a CHECK constraint, which {@code ddl-auto=update} does
 * not widen later. The set is closed: a new issue is a new catalogue row, not a
 * new kind.
 */
public enum HealthIssueKind {
    PEST,
    DISEASE,
    OTHER
}
