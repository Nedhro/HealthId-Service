package org.sharedhealth.healthId.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.cassandraunit.spring.CassandraUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.sharedhealth.healthId.web.Model.MciHealthId;
import org.sharedhealth.healthId.web.config.EnvironmentMock;
import org.sharedhealth.healthId.web.exception.Forbidden;
import org.sharedhealth.healthId.web.launch.WebMvcConfig;
import org.sharedhealth.healthId.web.repository.HealthIdRepository;
import org.sharedhealth.healthId.web.utils.TestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.Date;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@WebAppConfiguration
@ContextConfiguration(initializers = EnvironmentMock.class, classes = WebMvcConfig.class)
@TestPropertySource(properties = {"HEALTH_ID_REPLENISH_INITIAL_DELAY = 0",
        "HEALTH_ID_REPLENISH_DELAY = 1",
        "HEALTH_ID_BLOCK_SIZE = 1",
        "HEALTH_ID_BLOCK_SIZE_THRESHOLD=1"})
@CassandraUnit
public class BaseControllerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);
    @Autowired
    private HealthIdRepository healthIdRepository;
    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("HealthIdCassandraTemplate")
    protected CassandraOperations cassandraOps;

    @Autowired
    protected Filter springSecurityFilterChain;

    protected String validClientId;
    protected String validEmail;
    protected String validAccessToken;
    protected MockMvc mockMvc;
    protected ObjectMapper mapper = new ObjectMapper();
    public static final String API_END_POINT_FOR_PATIENT = "/patients";
    public static final String API_END_POINT_FOR_MERGE_REQUEST = "/mergerequest";
    public static final String API_END_POINT_FOR_LOCATION = "/locations";
    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";

    protected final String facilityClientId = "18548";
    protected final String facilityEmail = "facility@gmail.com";
    protected final String facilityAccessToken = "40214a6c-e27c-4223-981c-1f837be90f02";

    protected final String mciApproverClientId = "18555";
    protected final String mciApproverEmail = "mciapprover@gmail.com";
    protected final String mciApproverAccessToken = "40214a6c-e27c-4223-981c-1f837be90f06";


    protected void setUpMockMvcBuilder() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Before
    public void setupBase() throws Exception {
        healthIdRepository.resetLastReservedHealthId();
        createHealthIds();
    }

    private void createHealthIds() {
        for (int i = 0; i < numberOfHealthIdsNeeded(); i++) {
            healthIdRepository.saveMciHealthId(new MciHealthId(String.valueOf(new Date().getTime() + i)));
        }
    }

    protected int numberOfHealthIdsNeeded() {
        return 10;
    }

    @After
    public void teardownBase() {
        healthIdRepository.resetLastReservedHealthId();
        TestUtil.truncateAllColumnFamilies(cassandraOps);
    }

    protected BaseMatcher<Object> isForbidden() {
        return new BaseMatcher<Object>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof Forbidden;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue("Forbidden");
            }
        };
    }
}
