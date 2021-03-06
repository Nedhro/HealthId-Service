package org.sharedhealth.healthId.web.Model;

import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.util.UUID;

import static org.sharedhealth.healthId.web.repository.RepositoryConstants.*;
import static org.springframework.cassandra.core.PrimaryKeyType.PARTITIONED;


@Table(value = CF_ORG_HEALTH_ID)
public class OrgHealthId {

    @PrimaryKeyColumn(name = HEALTH_ID, ordinal = 0, type = PARTITIONED)
    private String healthId;

    @Column(GENERATED_AT)
    private UUID generatedAt;

    @Column(ALLOCATED_FOR)
    private String allocatedFor;

    @Column(USED_AT)
    private UUID usedAt;

    @Column(IS_USED)
    private boolean isUsed;

    public OrgHealthId() {
    }

    public OrgHealthId(String healthId, String allocatedFor, UUID generatedAt) {
        this(healthId, allocatedFor, generatedAt, Boolean.FALSE, null);
    }

    public OrgHealthId(String healthId, String allocatedFor, UUID generatedAt, Boolean isUsed, UUID usedAt) {
        this.healthId = healthId;
        this.allocatedFor = allocatedFor;
        this.generatedAt = generatedAt;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getAllocatedFor() {
        return allocatedFor;
    }

    public UUID getUsedAt() {
        return usedAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void markUsed(UUID usedAt) {
        this.isUsed = Boolean.TRUE;
        this.usedAt = usedAt;
    }

    public void setUsedAt(UUID uuid) {
        this.usedAt = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrgHealthId that = (OrgHealthId) o;

        if (isUsed != that.isUsed) return false;
        if (healthId != null ? !healthId.equals(that.healthId) : that.healthId != null) return false;
        if (generatedAt != null ? !generatedAt.equals(that.generatedAt) : that.generatedAt != null) return false;
        if (allocatedFor != null ? !allocatedFor.equals(that.allocatedFor) : that.allocatedFor != null) return false;
        return !(usedAt != null ? !usedAt.equals(that.usedAt) : that.usedAt != null);

    }

    @Override
    public int hashCode() {
        int result = healthId != null ? healthId.hashCode() : 0;
        result = 31 * result + (generatedAt != null ? generatedAt.hashCode() : 0);
        result = 31 * result + (allocatedFor != null ? allocatedFor.hashCode() : 0);
        result = 31 * result + (usedAt != null ? usedAt.hashCode() : 0);
        result = 31 * result + (isUsed ? 1 : 0);
        return result;
    }
}

