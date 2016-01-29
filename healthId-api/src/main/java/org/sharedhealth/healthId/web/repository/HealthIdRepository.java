package org.sharedhealth.healthId.web.repository;

import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.sharedhealth.healthId.web.Model.MciHealthId;
import org.sharedhealth.healthId.web.Model.OrgHealthId;
import org.sharedhealth.healthId.web.exception.HealthIdExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.sharedhealth.healthId.web.repository.RepositoryConstants.*;
import static org.springframework.data.cassandra.core.CassandraTemplate.createInsertQuery;

@Component
public class HealthIdRepository extends BaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(HealthIdRepository.class);
    public static final int BLOCK_SIZE = 10000;
    private String lastTakenHidMarker;

    @Autowired
    public HealthIdRepository(@Qualifier("HealthIdCassandraTemplate") CassandraOperations cassandraOps) {
        super(cassandraOps);
    }

    public MciHealthId saveMciHealthId(MciHealthId mciHealthId) {
        Insert insertQuery = getInsertQuery(mciHealthId);
        cassandraOps.executeAsynchronously(insertQuery.ifNotExists());
        return mciHealthId;
    }

    public MciHealthId saveMciHealthIdSync(MciHealthId mciHealthId) {
        Insert insertQuery = getInsertQuery(mciHealthId);
        cassandraOps.execute(insertQuery.ifNotExists());
        return mciHealthId;
    }

    public OrgHealthId saveOrgHealthId(OrgHealthId orgHealthId) {
        Insert insertQuery = getInsertQuery(orgHealthId);
        cassandraOps.executeAsynchronously(insertQuery);
        return orgHealthId;
    }

    public OrgHealthId saveOrgHealthIdSync(OrgHealthId orgHealthId) {
        Insert insertQuery = getInsertQuery(orgHealthId);
        cassandraOps.execute(insertQuery);
        return orgHealthId;
    }

    public List<MciHealthId> getNextBlock(int blockSize) {
        logger.debug(String.format("Getting next block of size : %d", blockSize));
        Select.Where from = QueryBuilder.select().from(CF_MCI_HEALTH_ID).where();
        if (null != lastTakenHidMarker) {
            from = from.and(QueryBuilder.gt("token(hid)", QueryBuilder.raw(String.format("token('%s')", lastTakenHidMarker))));
        }
        Select nextBlockQuery = from.limit(blockSize);

        List<MciHealthId> MciHealthIds = cassandraOps.select(nextBlockQuery, MciHealthId.class);
        if (MciHealthIds.size() < 1) throw new HealthIdExhaustedException();
        this.lastTakenHidMarker = MciHealthIds.get(MciHealthIds.size() - 1).getHid();
        return MciHealthIds;
    }

    public List<MciHealthId> getNextBlock() {
        return getNextBlock(BLOCK_SIZE);
    }

    public String getLastTakenHidMarker() {
        return lastTakenHidMarker;
    }

    public void resetLastReservedHealthId() {
        lastTakenHidMarker = null;
    }

    public void removedUsedHid(MciHealthId nextMciHealthId) {
        cassandraOps.deleteAsynchronously(nextMciHealthId);
    }

    public void removeUsedHidSync(MciHealthId nextMciHealthId) {
        cassandraOps.delete(nextMciHealthId);
    }

    public MciHealthId getHealthId(String hid) {
        Select selectHealthId = QueryBuilder.select().from(CF_MCI_HEALTH_ID).where(QueryBuilder.eq(HID, hid)).limit(1);
        List<MciHealthId> mciHealthIds = cassandraOps.select(selectHealthId, MciHealthId.class);
        return mciHealthIds.isEmpty() ? null : mciHealthIds.get(0);
    }

    private Insert getInsertQuery(MciHealthId mciHealthId) {
        logger.debug(String.format("Inserting new hid for MCI :%s", mciHealthId.getHid()));
        return createInsertQuery(CF_MCI_HEALTH_ID, mciHealthId, null, cassandraOps.getConverter());
    }

    private Insert getInsertQuery(OrgHealthId orgHealthId) {
        logger.debug(String.format("Inserting new hid for organization %s :%s", orgHealthId.getAllocatedFor(), orgHealthId.getHealthId()));
        return createInsertQuery(CF_ORG_HEALTH_ID, orgHealthId, null, cassandraOps.getConverter());
    }


    public OrgHealthId findOrgHealthId(String healthId) {
        Select selectHealthId = QueryBuilder.select().from(CF_ORG_HEALTH_ID).where(QueryBuilder.eq(HEALTH_ID, healthId)).limit(1);
        List<OrgHealthId> orgHealthIds = cassandraOps.select(selectHealthId, OrgHealthId.class);
        return orgHealthIds.isEmpty() ? null : orgHealthIds.get(0);    }
}
